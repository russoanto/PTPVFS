package shared.fs;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

class SymlinkNode extends Node implements Serializable{
    String targetPath;

    public SymlinkNode(String name, String targetPath) {
        super(name);
        this.targetPath = targetPath;
    }

    boolean isDirectory() { return false; }
    boolean isSymlink() { return true; }

    String readLink() {
        return targetPath;
    }
}
