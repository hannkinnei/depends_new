package depends.entity.repo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import depends.entity.Entity;
import depends.entity.GenericName;
import depends.entity.MultiDeclareEntities;

public class InMemoryEntityRepo extends SimpleIdGenerator implements EntityRepo {

	private HashMap<String, Entity> allEntieisByName;
	private HashMap<Integer, Entity> allEntitiesById;
	private List<Entity> allEntitiesByOrder;

	public InMemoryEntityRepo() {
		allEntieisByName = new HashMap<>();
		allEntitiesById = new HashMap<>();
		allEntitiesByOrder = new LinkedList<>();
	}

	@Override
	public Entity getEntity(String entityName) {
		return allEntieisByName.get(entityName);
	}

	@Override
	public Entity getEntity(Integer entityId) {
		return allEntitiesById.get(entityId);
	}

	@Override
	public void add(Entity entity) {
		allEntitiesByOrder.add(entity);
		allEntitiesById.put(entity.getId(), entity);
		String name = entity.getRawName().uniqName();
		if (entity.getQualifiedName() != null && !(entity.getQualifiedName().isEmpty())) {
			name = entity.getQualifiedName();
		}
		if (allEntieisByName.containsKey(name)) {
			Entity existedEntity = allEntieisByName.get(name);
			if (existedEntity instanceof MultiDeclareEntities) {
				((MultiDeclareEntities) existedEntity).add(entity);
			} else {
				MultiDeclareEntities eMultiDeclare = new MultiDeclareEntities(existedEntity, this.generateId());
				eMultiDeclare.add(entity);
				allEntieisByName.put(name, eMultiDeclare);
			}
		} else {
			allEntieisByName.put(name, entity);
		}
		if (entity.getParent() != null)
			Entity.setParent(entity, entity.getParent());
	}

	@Override
	public Iterator<Entity> entityIterator() {
		return allEntitiesByOrder.iterator();
	}

	
	@Override
	public void update(Entity entity) {
	}

	@Override
	public Entity getEntity(GenericName rawName) {
		return this.getEntity(rawName.uniqName());
	}

	@Override
	public Collection<Entity> getEntities() {
		return allEntitiesByOrder;
	}

}
