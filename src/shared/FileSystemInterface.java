package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface FileSystemInterface extends Remote {
    // === FS di base (usati dai client esterni)
    boolean mkdir(String path) throws RemoteException;
    boolean mknod(String path) throws RemoteException;
    boolean symlink(String target, String linkPath) throws RemoteException;
    boolean write(String path, byte[] content) throws RemoteException;
    byte[] read(String path) throws RemoteException;
    boolean rename(String oldPath, String newPath) throws RemoteException;
    List<String> readdir(String path) throws RemoteException;
    Map<String, Object> getattr(String path) throws RemoteException;
    String locate(String file) throws RemoteException;

    // === Versioni con visited (usate tra peer)
    boolean writeWithVisited(String path, byte[] content, List<String> visited) throws RemoteException;
    byte[] readWithVisited(String path, List<String> visited) throws RemoteException;
    boolean renameWithVisited(String oldPath, String newPath, List<String> visited) throws RemoteException;
    List<String> readdirWithVisited(String path, List<String> visited) throws RemoteException;
    Map<String, Object> getattrWithVisited(String path, List<String> visited) throws RemoteException;
    String locateWithVisited(String file, List<String> visited) throws RemoteException;
    boolean mkdirWithVisited(String path , List<String> visited) throws RemoteException;
    boolean mknodWithVisited(String path, List<String> visited) throws RemoteException;
    // === Membership
    void addNeighbor(String name, String host, int port) throws RemoteException;
    void removeNeighbor(String name) throws RemoteException;
    List<String> getNeighbors() throws RemoteException;

    // === Utils
    List<String> listAllPaths() throws RemoteException;
    boolean pathExistsWithVisited(String path, java.util.List<String> visited) throws java.rmi.RemoteException;

}
