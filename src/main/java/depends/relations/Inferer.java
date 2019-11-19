package depends.relations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import depends.entity.ContainerEntity;
import depends.entity.Entity;
import depends.entity.FileEntity;
import depends.entity.MultiDeclareEntities;
import depends.entity.TypeEntity;
import depends.entity.VarEntity;
import depends.entity.repo.BuiltInType;
import depends.entity.repo.EntityRepo;
import depends.entity.repo.NullBuiltInType;
import depends.importtypes.Import;

public class Inferer {
	private static final Logger logger = LoggerFactory.getLogger(Inferer.class);

	static final public TypeEntity buildInType = new TypeEntity("built-in", null, -1);
	static final public TypeEntity externalType = new TypeEntity("external", null, -1);
	static final public TypeEntity genericParameterType = new TypeEntity("T", null, -1);
	private BuiltInType buildInTypeManager = new NullBuiltInType();
	private ImportLookupStrategy importLookupStrategy;
	private HashSet<String> unsolvedSymbols;
	private EntityRepo repo;

	public Inferer(EntityRepo repo, ImportLookupStrategy importLookupStrategy, BuiltInType buildInTypeManager) {
		this.repo = repo;
		this.importLookupStrategy = importLookupStrategy;
		this.buildInTypeManager = buildInTypeManager;
		unsolvedSymbols= new HashSet<>();
	}

	/**
	 * Resolve all bindings
	 * - Firstly, we resolve all types from there names.
	 * - Secondly, we resolve all expressions (expression will use type infomation of previous step
	 */
	public  Set<String> resolveAllBindings() {
		resolveTypes();
		resolveExpressoins(); 
		System.out.println("Dependency analaysing....");
		new RelationCounter(repo.getEntities()).computeRelations();
		System.out.println("Dependency done....");
		return unsolvedSymbols;		
	}

	private void resolveTypes() {
		for (Entity entity:repo.getEntities()) {
			if (!(entity instanceof FileEntity)) continue;
			entity.inferEntities(this);
		}
	}
	private void resolveExpressoins() {
		for (Entity entity:repo.getEntities()) {
			if ((entity instanceof ContainerEntity))
				((ContainerEntity)entity).resolveExpressions(this);
		}
	}
	
	/**
	 * For types start with the prefix, it will be treated as built-in type
	 * For example, java.io.* in Java, or __ in C/C++
	 * @param prefix
	 * @return
	 */
	public boolean isBuiltInTypePrefix(String prefix) {
		return buildInTypeManager.isBuiltInTypePrefix(prefix);
	}
	
	/**
	 * Different languages have different strategy on how to compute the imported types
	 * and the imported files.
	 * For example, in C/C++, both imported types (using namespace, using <type>) and imported files exists. 
	 * while in java, only 'import class/function, or import wildcard class.* package.* exists. 
	 */
	public List<Entity> getImportedRelationEntities(List<Import> importedNames) {
		return importLookupStrategy.getImportedRelationEntities(importedNames, repo);
	}

	public List<Entity> getImportedTypes(List<Import> importedNames) {
		return importLookupStrategy.getImportedTypes(importedNames, repo);
	}

	public List<Entity> getImportedFiles(List<Import> importedNames) {
		return importLookupStrategy.getImportedFiles(importedNames, repo);
	}

	/**
	 * By given raw name, to infer the type of the name
	 * for example
	 *   if it is a class, the class is the type
	 *   if it is a function, the return type is the type
	 *   if it is a variable, type of variable is the type 
	 * @param fromEntity
	 * @param rawName
	 * @return
	 */
	public TypeEntity inferTypeFromName(Entity fromEntity, String rawName) {
		Entity data = resolveName(fromEntity, rawName, true);
		if (data == null)
			return null;
		return data.getType();
	}

	/**
	 * By given raw name, to infer the entity of the name
	 * @param fromEntity
	 * @param rawName
	 * @param searchImport
	 * @return
	 */
	public Entity resolveName(Entity fromEntity, String rawName, boolean searchImport) {
		Entity entity = resolveNameInternal(fromEntity,rawName,searchImport);
		if (logger.isDebugEnabled()) {
			logger.debug("resolve name " + rawName + " from " + fromEntity.getQualifiedName() +" ==> "
						+ (entity==null?"null":entity.getQualifiedName()));
		}
		return entity;
	}

	private Entity resolveNameInternal(Entity fromEntity, String rawName, boolean searchImport) {
		if (rawName == null)
			return null;
		if (buildInTypeManager.isBuiltInType(rawName)) {
			return buildInType;
		}
		if (buildInTypeManager.isBuiltInTypePrefix(rawName)) {
			return buildInType;
		}
		// qualified name will first try global name directly
		if (rawName.contains(".")) {
			if (rawName.startsWith(".")) rawName = rawName.substring(1);
			if (repo.getEntity(rawName) != null)
				return repo.getEntity(rawName);
		}
		// first we lookup the first symbol
		String[] names = rawName.split("\\.");
		if (names.length == 0)
			return null;
		Entity type = lookupEntity(fromEntity, names[0], searchImport);
		if (type == null)
			return null;
		if (names.length == 1) {
			return type;
		}
		// then find the subsequent symbols
		return findEntitySince(type, names, 1);
	}
	
	private Entity lookupEntity(Entity fromEntity, String name, boolean searcImport) {
		if (name.equals("this") || name.equals("class")) {
			TypeEntity entityType = (TypeEntity) (fromEntity.getAncestorOfType(TypeEntity.class));
			return entityType;
		} else if (name.equals("super")) {
			TypeEntity parent = (TypeEntity) (fromEntity.getAncestorOfType(TypeEntity.class));
			if (parent != null) {
				TypeEntity parentType = parent.getInheritedType();
				if (parentType!=null) 
					return parentType;
			}
		}

		Entity inferData = findEntityUnderSamePackage(fromEntity, name);
		if(fromEntity instanceof VarEntity) {
			System.out.println(fromEntity + " " + inferData);
		}
		if (inferData != null)
			return inferData;
		if (searcImport)
			inferData = lookupTypeInImported((FileEntity)(fromEntity.getAncestorOfType(FileEntity.class)), name);
		return inferData;
	}
	/**
	 * To lookup entity in case of a.b.c from a;
	 * @param precendenceEntity
	 * @param names
	 * @param nameIndex
	 * @return
	 */
	private Entity findEntitySince(Entity precendenceEntity, String[] names, int nameIndex) {
		if (nameIndex >= names.length) {
			return precendenceEntity;
		}
		//If it is not an entity with types (not a type, var, function), fall back to itself
		if (precendenceEntity.getType()==null) 
			return precendenceEntity;
		
		for (Entity child : precendenceEntity.getType().getChildren()) {
			if (child.getRawName().equals(names[nameIndex])) {
				return findEntitySince(child, names, nameIndex + 1);
			}
		}
		return null;
	}

	private Entity lookupTypeInImported(FileEntity fileEntity, String name) {
		if (fileEntity == null)
			return null;
		Entity type = importLookupStrategy.lookupImportedType(name, fileEntity, repo,this);
		if (type != null)
			return type;
		return externalType;
	}


	/**
	 * In Java/C++ etc, the same package names should take priority of resolving.
	 * the entity lookup is implemented recursively.
	 * @param fromEntity
	 * @param name
	 * @return
	 */
	private Entity findEntityUnderSamePackage(Entity fromEntity, String name) {
		while (true) {
			Entity entity = tryToFindEntityWithName(fromEntity, name);
			if (entity != null)
				return entity;
			entity = findEntityInChild(fromEntity,name);
			if (entity!=null) return entity;
			
			if (fromEntity instanceof TypeEntity) {
				TypeEntity type = (TypeEntity)fromEntity;
				while(true) {
					if (type.getInheritedTypes().size()==0) break;
					for (TypeEntity child:type.getInheritedTypes()) {
						entity = findEntityInChild(child,name);
						if (entity!=null) return entity;
						type = child;
					}
				}
				while(true) {
					if (type.getImplementedTypes().size()==0) break;
					for (TypeEntity child:type.getImplementedTypes()) {
						entity = findEntityInChild(child,name);
						if (entity!=null) return entity;
						type = child;
					}
				}
			}
			
			for (Entity child : fromEntity.getChildren()) {
				if (child instanceof FileEntity) {
					for (Entity classUnderFile : child.getChildren()) {
						entity = tryToFindEntityWithName(classUnderFile, name);
						if (entity != null)
							return entity;
					}
				}
			}
			fromEntity = fromEntity.getParent();
			if (fromEntity == null)
				break;
		}
		return null;
	}
	
	
	private Entity findEntityInChild(Entity fromEntity,String name) {
		Entity entity =null;
		for (Entity child : fromEntity.getChildren()) {
			entity = tryToFindEntityWithName(child, name);
			if (entity != null)
				return entity;
		}
		return entity;
	}
	
	/**
	 * Only used by findEntityUnderSamePackage
	 * @param fromEntity
	 * @param name
	 * @return
	 */
	private Entity tryToFindEntityWithName(Entity fromEntity, String name) {
		if (!fromEntity.getRawName().equals(name))
			return null;
		if (fromEntity instanceof MultiDeclareEntities) {
			for (Entity declaredEntitiy : ((MultiDeclareEntities) fromEntity).getEntities()) {
				if (declaredEntitiy.getRawName().equals(name) && declaredEntitiy instanceof TypeEntity) {
					return declaredEntitiy;
				}
			}
		}
		return fromEntity;
	}
}
