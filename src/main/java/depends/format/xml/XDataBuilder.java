package depends.format.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import depends.format.FileAttributes;
import depends.format.matrix.DependencyMatrix;
import depends.format.matrix.DependencyPair;
import depends.format.matrix.DependencyValue;

public class XDataBuilder {

    public XDepObject build2(DependencyMatrix matrix,FileAttributes configure) {
        ArrayList<String> files = matrix.getNodes();
        Map<Integer, Map<Integer, Map<String, Integer>>> finalRes = matrix.getRelations();

        XFiles xFiles = new XFiles();
        xFiles.setFiles(files);

        ArrayList<XCell> xCellList = buildCellList(finalRes);

        XCells xCells = new XCells();
        xCells.setCells(xCellList);

        XDepObject xDepObject = new XDepObject();
        xDepObject.setName(configure.getAttributeName());
        xDepObject.setSchemaVersion(configure.getSchemaVersion());
        xDepObject.setVariables(xFiles);
        xDepObject.setCells(xCells);

        return xDepObject;
    }

    public XDepObject build(DependencyMatrix matrix,FileAttributes configure) {
        ArrayList<String> files = matrix.getNodes();
        Collection<DependencyPair> dependencyPairs = matrix.getDependencyPairs();

        XFiles xFiles = new XFiles();
        xFiles.setFiles(files);

        ArrayList<XCell> xCellList = buildCellList(dependencyPairs);

        XCells xCells = new XCells();
        xCells.setCells(xCellList);

        XDepObject xDepObject = new XDepObject();
        xDepObject.setName(configure.getAttributeName());
        xDepObject.setSchemaVersion(configure.getSchemaVersion());
        xDepObject.setVariables(xFiles);
        xDepObject.setCells(xCells);

        return xDepObject;
    }


    private ArrayList<XCell> buildCellList(Collection<DependencyPair> dependencyPairs) {
    	ArrayList<XCell> cellList = new ArrayList<XCell>();
        for (DependencyPair pair : dependencyPairs) {
                ArrayList<XDepend> xDepends = buildDependList(pair.getDependencies());
                XCell xCell = new XCell();
                xCell.setSrc(pair.getFrom());
                xCell.setDest(pair.getTo());
                xCell.setDepends(xDepends);
                cellList.add(xCell);
        } 
        return cellList;
	}

	private ArrayList<XDepend> buildDependList(Collection<DependencyValue> dependencies) {
		ArrayList<XDepend> dependList = new ArrayList<XDepend>();

        for (DependencyValue dependency : dependencies) {
            XDepend xDepend = new XDepend();
            xDepend.setWeight(dependency.getWeight());
            xDepend.setName(dependency.getType());
            dependList.add(xDepend);
        } 
        return dependList;
	}

	private ArrayList<XCell> buildCellList(Map<Integer, Map<Integer, Map<String, Integer>>> finalRes) {
        ArrayList<XCell> cellList = new ArrayList<XCell>();
        for (Map.Entry<Integer, Map<Integer, Map<String, Integer>>> entry1 : finalRes.entrySet()) {
            int src = entry1.getKey();

            Map<Integer, Map<String, Integer>> values1 = entry1.getValue();
            for (Map.Entry<Integer, Map<String, Integer>> entry2 : values1.entrySet()) {
                int dst = entry2.getKey();

                Map<String, Integer> values2 = entry2.getValue();
                ArrayList<XDepend> xDepends = buildDependList(values2);
                XCell xCell = new XCell();
                xCell.setSrc(src);
                xCell.setDest(dst);
                xCell.setDepends(xDepends);
                cellList.add(xCell);
            }
        } //end for
        return cellList;
    }



    private ArrayList<XDepend> buildDependList(Map<String, Integer> values2) {
        ArrayList<XDepend> dependList = new ArrayList<XDepend>();

        for (Map.Entry<String, Integer> entry3 : values2.entrySet()) {
            String depType = entry3.getKey();
            float weight = (float) entry3.getValue();
            XDepend xDepend = new XDepend();
            xDepend.setWeight(weight);
            xDepend.setName(depType);
            dependList.add(xDepend);
        } //end for
        return dependList;
    }

}
