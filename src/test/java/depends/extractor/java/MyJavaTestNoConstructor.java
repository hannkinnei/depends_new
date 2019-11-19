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

public class MyJavaTestNoConstructor extends JavaParserTest{
	@Before
	public void setUp() {
		super.init();
	}
	
	@Test
	public void test_no_constructor() throws IOException {
		String[] srcs = new String[] {
	    		"./src/test/resources/java-code-examples/MyJavaTest/test_no_constructor/A.java",
	    		"./src/test/resources/java-code-examples/MyJavaTest/test_no_constructor/B.java"
	    	    };
	    
	    for (String src:srcs) {
		    FileParser parser = createParser(src);
		    parser.parse();
	    }
	    inferer.resolveAllBindings();
	    TypeEntity type = (TypeEntity)(entityRepo.getEntity("test_no_constructor.B"));
	    List<Relation> relations = type.getRelations();
	    for(Relation relation : relations) {
	    	System.out.println(relation.getType() + " " + relation.getEntity());
	    }
	    assertTrue(assertRelation(type, DependencyType.CONTAIN, "test_no_constructor.A")
	    		&& assertRelation(type, DependencyType.INHERIT, "test_no_constructor.A"));
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
