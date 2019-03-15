package depends.extractor.python;

import java.util.ArrayList;
import java.util.List;

import depends.entity.repo.EntityRepo;
import depends.relations.Inferer;

public abstract class PythonParserTest {

	protected EntityRepo repo;
	private PythonProcessor p;
	protected Inferer inferer;

	public void init() {
		List<String> includeDir = new ArrayList<>();
		includeDir.add("./src/test/resources/python-code-examples/");
		this.p = new PythonProcessor();
		p.includeDirs = includeDir.toArray(new String[] {});
		
		this.repo = p.getEntityRepo();
		this.inferer = p.inferer;
    }
	
	public PythonFileParser createParser(String src) {
		return (PythonFileParser) p.createFileParser(src);
	}
}
