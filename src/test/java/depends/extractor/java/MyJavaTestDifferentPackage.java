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

public class MyJavaTestDifferentPackage extends JavaParserTest{
	@Before
	public void setUp() {
		super.init();
	}
	@Test
	public void test() throws IOException {
		String[] srcs = new String[] {
	    		"./src/test/resources/java-code-examples/MyJavaTest/test_different_package/test_a/A.java",
	    		"./src/test/resources/java-code-examples/MyJavaTest/test_different_package/test_b/B.java"
	    	    };
	    
	    for (String src:srcs) {
		    FileParser parser = createParser(src);
		    parser.parse();
	    }
	    inferer.resolveAllBindings();
	    TypeEntity type = (TypeEntity)(entityRepo.getEntity("test_different_package.test_b.B"));
	    List<Relation> relations = type.getRelations();
	    for(Relation relation : relations) {
	    	System.out.println(relation.getType() + " " + relation.getEntity());
	    }
	    assertTrue(assertRelation(type, DependencyType.CONTAIN, "test_different_package.test_a.A")
	    		&& assertRelation(type, DependencyType.INHERIT, "test_different_package.test_a.A"));
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
