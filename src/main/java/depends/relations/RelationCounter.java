/*
MIT License

Copyright (c) 2018-2019 Gang ZHANG

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package depends.relations;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import depends.deptypes.DependencyType;
import depends.entity.ContainerEntity;
import depends.entity.Entity;
import depends.entity.Expression;
import depends.entity.FileEntity;
import depends.entity.FunctionEntity;
import depends.entity.FunctionEntityImpl;
import depends.entity.FunctionEntityProto;
import depends.entity.MultiDeclareEntities;
import depends.entity.TypeEntity;
import depends.entity.VarEntity;
import depends.entity.repo.EntityRepo;

public class RelationCounter {

	private Iterator<Entity> iterator;
	private Inferer inferer;
	private EntityRepo repo;
	private boolean callAsImpl;

	public RelationCounter(Iterator<Entity> iterator, Inferer inferer, EntityRepo repo, boolean callAsImpl) {
		this.iterator = iterator;
		this.inferer = inferer;
		this.repo = repo;
		this.callAsImpl = callAsImpl;
	}
	
	public void computeRelations() {
		while(iterator.hasNext()) {
			Entity entity= iterator.next();
			if (!entity.inScope()) continue;
			if (entity instanceof FileEntity) {
				computeImports((FileEntity)entity);
			}
			else if (entity instanceof FunctionEntity) {
				computeFunctionRelations((FunctionEntity)entity);
			}
			else if (entity instanceof TypeEntity) {
				computeTypeRelations((TypeEntity)entity);
			}
			if (entity instanceof ContainerEntity) {
				computeContainerRelations((ContainerEntity)entity);
			}
		}
	}

	

	private void computeContainerRelations(ContainerEntity entity) {
		entity.reloadExpression(repo);
		entity.resolveExpressions(inferer);
		for (VarEntity var:entity.getVars()) {
			if (var.getType()!=null)
				entity.addRelation(new Relation(DependencyType.CONTAIN,var.getType()));
			for (Entity type:var.getResolvedTypeParameters()) {
				var.addRelation(new Relation(DependencyType.PARAMETER,type));
			}
		}
		for (Entity type:entity.getResolvedAnnotations()) {
			entity.addRelation(new Relation(DependencyType.ANNOTATION,type));
		}
		for (Entity type:entity.getResolvedTypeParameters()) {
			entity.addRelation(new Relation(DependencyType.USE,type));
		}
		for (ContainerEntity mixin:entity.getResolvedMixins()) {
			entity.addRelation(new Relation(DependencyType.MIXIN,mixin));
		}
		
		for (Expression expression:entity.expressionList()){
			if (expression.isStatement) {
				continue;
			}
			Entity referredEntity = expression.getReferredEntity();
			addRelationFromExpression(entity, expression, referredEntity);
		}
		

		entity.clearExpressions();
	}

	private void addRelationFromExpression(ContainerEntity entity, Expression expression, Entity referredEntity) {
		
		if (referredEntity==null) {
			return;
		}

		if (referredEntity instanceof MultiDeclareEntities) {
			for (ContainerEntity e:((MultiDeclareEntities)referredEntity).getEntities()) {
				addRelationFromExpression(entity,expression,e);
			}
			return;
		}
		boolean matched = false;
		if (expression.isCall) {
			if (callAsImpl && referredEntity instanceof FunctionEntityProto) {
				Entity multiDeclare = repo.getEntity(referredEntity.getQualifiedName());
				if (multiDeclare instanceof MultiDeclareEntities) {
					MultiDeclareEntities m = (MultiDeclareEntities)multiDeclare;
					List<ContainerEntity> entities = m.getEntities().stream().filter(item->(item instanceof FunctionEntityImpl))
					.collect(Collectors.toList());
					for (Entity e:entities) {
						entity.addRelation(new Relation(DependencyType.CALL,e));
						matched = true;
					}
				}
			}
			if (!matched) {
				entity.addRelation(new Relation(DependencyType.CALL,referredEntity));
				matched = true;
			}

		}
		if (expression.isCreate) {
			entity.addRelation(new Relation(DependencyType.CREATE,referredEntity));
			matched = true;
		}
		if (expression.isThrow) {
			entity.addRelation(new Relation(DependencyType.THROW,referredEntity));
			matched = true;
		}
		if (expression.isCast) { 
			entity.addRelation(new Relation(DependencyType.CAST,referredEntity));
			matched = true;
		}
		if (!matched)  {
			entity.addRelation(new Relation(DependencyType.USE,referredEntity));
		}
	}

	private void computeTypeRelations(TypeEntity type) {
		for (TypeEntity superType:type.getInheritedTypes()) {
			type.addRelation(new Relation(DependencyType.INHERIT,superType));
		}
		for (TypeEntity interfaceType:type.getImplementedTypes()) {
			type.addRelation(new Relation(DependencyType.IMPLEMENT,interfaceType));
		}
	}

	private void computeFunctionRelations(FunctionEntity func) {
		for (Entity returnType:func.getReturnTypes()) {
			func.addRelation(new Relation(DependencyType.RETURN,returnType.getActualReferTo()));
		}
		for (VarEntity parameter:func.getParameters()) {
			if (parameter.getType()!=null) 
				func.addRelation(new Relation(DependencyType.PARAMETER,parameter.getActualReferTo()));
		}
		for (Entity throwType:func.getThrowTypes()) {
			func.addRelation(new Relation(DependencyType.THROW,throwType));
		}
		for (Entity type:func.getResolvedTypeParameters()) {
			func.addRelation(new Relation(DependencyType.PARAMETER,type));
		}
		if (func instanceof FunctionEntityImpl) {
			FunctionEntityImpl funcImpl = (FunctionEntityImpl)func;
			if(funcImpl.getImplemented()!=null) {
				func.addRelation(new Relation(DependencyType.IMPLEMENT,funcImpl.getImplemented()));
			}
		}
	}

	private void computeImports(FileEntity file) {
		Collection<Entity> imports = file.getImportedRelationEntities();
		if (imports==null) return;
		for (Entity imported:imports) {
			if (imported instanceof FileEntity)
			{
				if (((FileEntity)imported).isInProjectScope())
					file.addRelation(new Relation(DependencyType.IMPORT,imported));
			}else {
				file.addRelation(new Relation(DependencyType.IMPORT,imported));
			}
		}
	}

}
