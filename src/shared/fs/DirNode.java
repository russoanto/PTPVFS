package shared.fs;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

class DirectoryNode extends Node implements Serializable{
    Map<String, Node> children = new HashMap<>();

    public DirectoryNode(String name){
	super(name);
    }

    boolean isDirectory() { return true; }
    boolean isSymlink() { return false; }

    boolean isEmpty() {
	return children.isEmpty();
    }


}
