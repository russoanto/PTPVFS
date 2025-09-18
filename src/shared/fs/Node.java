package shared.fs;
import java.util.*;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

abstract class Node implements Serializable{
    private static final long serialVersionUID = 1L;
    String name;
    DirectoryNode parent; // to be implemented
    long createdAt;
    long modifiedAt;
    boolean isOpen = false;

    public Node(String name){
	this.name = name;
	this.createdAt = System.currentTimeMillis();
	this.modifiedAt = this.createdAt;
    }
    abstract boolean isDirectory();
    abstract boolean isSymlink();
}
