package shared.fs;
import java.util.Set;
import java.util.HashSet;
import java.io.Serializable;

class FileNode extends Node implements Serializable{
    byte[] data;

    Set<FileNode> hardLinks = new HashSet<>();

    public FileNode(String name){
	super(name);
	this.data = new byte[0];
	hardLinks.add(this);
    }

    boolean isDirectory(){ return false; }
    boolean isSymlink() { return false; }

    void write(byte [] content){
	this.data = content;
	this.modifiedAt = System.currentTimeMillis();
    }

    byte [] read() {
	return this.data;
    }
}
