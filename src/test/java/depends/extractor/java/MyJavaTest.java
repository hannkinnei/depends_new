package depends.extractor.java;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import depends.deptypes.DependencyType;
import depends.entity.Entity;
import depends.entity.TypeEntity;
import depends.extractor.FileParser;
import depends.relations.Relation;

public class MyJavaTest extends JavaParserTest{
	@Before
	public void setUp() {
		super.init();
	}
	
	@Test
	public void test_wildcard_import_should_be_lookedup() throws IOException {
		String[] srcs = new String[] {
	    		"./src/test/resources/java-code-examples/MyJavaTest/test/A.java",
	    		"./src/test/resources/java-code-examples/MyJavaTest/test/B.java"
	    	    };
	    
	    for (String src:srcs) {
		    FileParser parser = createParser(src);
		    parser.parse();
	    }
	    inferer.resolveAllBindings();
	    entityRepo.getEntities().forEach(entity -> {
	    	System.out.println(entity);
	    	entity.getRelations().forEach(relation -> {
	    		System.out.println("- " + relation.getType() + " " + relation.getEntity());
	    	});
	    });
	    TypeEntity type = (TypeEntity)(entityRepo.getEntity("test.B"));
	    List<Relation> relations = type.getRelations();
	    for(Relation relation : relations) {
	    	System.out.println(relation.getType() + " " + relation.getEntity());
	    }
	    assertTrue(assertRelation(type, DependencyType.CONTAIN, "test.A")
	    		&& assertRelation(type, DependencyType.INHERIT, "test.A"));
	}
	
	protected boolean assertRelation(Entity inEntity, String dependencyType, String dependedEntityFullName) {
		for (Relation r:inEntity.getRelations()) {
			if (r.getType().equals(dependencyType)) {
				if (r.getEntity().getQualifiedName().equals(dependedEntityFullName)) {
					return true;
				}
			}
		}
		return false;
	}
}
