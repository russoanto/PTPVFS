package shared.fs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;                  
import java.nio.file.Paths;                
import java.nio.file.StandardCopyOption;   
import java.nio.file.StandardOpenOption;   
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ConcurrentHashMap;

public class FileSystem {
    private DirectoryNode root;

    // Write-through
    private Path mountedRoot;                  // root reale montata
    private final boolean writeThrough = true;

    // Lock per path (già presente per read/write)
    private final Map<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    public FileSystem() {
        root = new DirectoryNode("/");
        root.parent = null;
    }

    private ReentrantReadWriteLock getLock(String path) {
        return locks.computeIfAbsent(path, p -> new ReentrantReadWriteLock());
    }

    private String[] tokenize(String path) {
        return Arrays.stream(path.split("/")).filter(p -> !p.isEmpty()).toArray(String[]::new);
    }

    private Node resolve(String path, boolean resolveSymlink) {
        if (path.equals("/")) return root;
        String[] parts = tokenize(path);
        Node curr = root;

        for (String part : parts) {
            if (!(curr instanceof DirectoryNode)) return null;
            curr = ((DirectoryNode) curr).children.get(part);
            if (curr == null) return null;
            if (resolveSymlink && curr instanceof SymlinkNode) {
                curr = resolve(((SymlinkNode) curr).targetPath, true);
            }
        }
        return curr;
    }

    // === Helper: path reale dalla root montata, con protezione traversal ===
    private Path realPath(String vpath) {
        if (mountedRoot == null) {
            throw new IllegalStateException("FileSystem non montato su una root reale");
        }
        if (vpath == null || vpath.isEmpty() || "/".equals(vpath)) return mountedRoot;

        String[] parts = tokenize(vpath);
        Path p = mountedRoot;
        for (String part : parts) p = p.resolve(part);
        p = p.normalize();

        if (!p.startsWith(mountedRoot)) {
            throw new SecurityException("Path traversal fuori dalla root montata: " + vpath);
        }
        return p;
    }

    public boolean mkdir(String path) {
        return createNode(path, "dir", null);
    }

    public boolean mknod(String path) {
        return createNode(path, "file", null);
    }

    public boolean symlink(String target, String linkPath) {
        return createNode(linkPath, "symlink", target);
    }

    public boolean link(String existingPath, String newPath) {
        Node target = resolve(existingPath, false);
        if (!(target instanceof FileNode)) return false;

        String[] parts = tokenize(newPath);
        String name = parts[parts.length - 1];
        DirectoryNode parent = (DirectoryNode) resolveParent(newPath);

        if (parent != null && !parent.children.containsKey(name)) {
            parent.children.put(name, target);
            ((FileNode) target).hardLinks.add((FileNode) target);
            // NB: hard link su disco reale non implementato (dipende da FS/OS)
            return true;
        }
        return false;
    }

    public boolean rename(String oldPath, String newPath) {
        Node node = resolve(oldPath, false);
        DirectoryNode oldParent = (DirectoryNode) resolveParent(oldPath);
        DirectoryNode newParent = (DirectoryNode) resolveParent(newPath);
        if (node == null || oldParent == null || newParent == null) return false;

        String oldName = tokenize(oldPath)[tokenize(oldPath).length - 1];
        String newName = tokenize(newPath)[tokenize(newPath).length - 1];

        // write-through su disco
        if (writeThrough && mountedRoot != null) {
            try {
                Path oldReal = realPath(oldPath);
                Path newReal = realPath(newPath);
                if (newReal.getParent() != null) Files.createDirectories(newReal.getParent());
                Files.move(oldReal, newReal,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                System.err.println("rename write-through failed: " + e.getMessage());
                return false;
            }
        }

        // aggiornamento in memoria
        oldParent.children.remove(oldName);
        node.name = newName;
        newParent.children.put(newName, node);
        node.parent = newParent;
        return true;
    }

    public boolean rmdir(String path) {
        Node node = resolve(path, false);
        if (!(node instanceof DirectoryNode) || !((DirectoryNode) node).isEmpty()) return false;
        return removeNode(path);
    }

    public boolean createNode(String path, String type, String symlinkTarget) {
        ReentrantReadWriteLock lock = getLock(path);
        lock.writeLock().lock();
        String[] parts = tokenize(path);
        String name = parts[parts.length - 1];
        DirectoryNode parent = (DirectoryNode) resolveParent(path);
        try{
            if (parent == null || parent.children.containsKey(name)) return false;

            Node node;
            switch (type) {
            case "file":
                node = new FileNode(name);
                break;
            case "dir":
                node = new DirectoryNode(name);
                break;
            case "symlink":
                node = new SymlinkNode(name, symlinkTarget);
                break;
            default:
                return false;
            }

            node.parent = parent;
            parent.children.put(name, node);

            // write-through su disco
            if (writeThrough && mountedRoot != null) {
                try {
                    Path rp = realPath(path);
                    if (node instanceof DirectoryNode) {
                        Files.createDirectories(rp);
                    } else if (node instanceof FileNode) {
                        if (rp.getParent() != null) Files.createDirectories(rp.getParent());
                        if (!Files.exists(rp)) Files.createFile(rp);
                    } else if (node instanceof SymlinkNode) {
                        if (rp.getParent() != null) Files.createDirectories(rp.getParent());
                        try {
                            Path target = Paths.get(((SymlinkNode) node).readLink());
                            Files.createSymbolicLink(rp, target);
                        } catch (UnsupportedOperationException ex) {
                            System.err.println("Symlink non supportato o richiede privilegi: " + ex.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.err.println("createNode write-through failed for " + path + ": " + e.getMessage());
                    // opzionale: rollback in-mem
                }
            }

            return true;
        } finally{
            lock.writeLock().unlock();
        }
    }

    public DirectoryNode resolveParent(String path) {
        String[] parts = tokenize(path);
        if (parts.length == 0) return null;
        String parentPath = "/" + String.join("/", Arrays.copyOf(parts, parts.length - 1));
        Node parent = resolve(parentPath, false);
        return (parent instanceof DirectoryNode) ? (DirectoryNode) parent : null;
    }

    public boolean open(String path) {
        Node node = resolve(path, true);
        if (node == null || node.isOpen) return false;
        node.isOpen = true;
        return true;
    }

    public boolean close(String path) {
        Node node = resolve(path, true);
        if (node == null || !node.isOpen) return false;
        node.isOpen = false;
        return true;
    }

    public Node lookup(String path) {
        return resolve(path, true);
    }

    public List<String> readdir(String path) {
        Node node = resolve(path, false);
        if (!(node instanceof DirectoryNode)) return null;
        return new ArrayList<>(((DirectoryNode) node).children.keySet());
    }

    public String readlink(String path) {
        Node node = resolve(path, false);
        return (node instanceof SymlinkNode) ? ((SymlinkNode) node).readLink() : null;
    }

    public Map<String, Object> getattr(String path) {
        Node node = resolve(path, true);
        if (node == null) return null;
        Map<String, Object> attr = new HashMap<>();
        attr.put("name", node.name);
        attr.put("type", node.getClass().getSimpleName());
        attr.put("createdAt", node.createdAt);
        attr.put("modifiedAt", node.modifiedAt);
        return attr;
    }

    public boolean setattr(String path, String attr, Object value) {
        Node node = resolve(path, true);
        if (node == null) return false;
        if ("name".equals(attr) && value instanceof String) {
            node.name = (String) value;
            return true;
        }
        return false;
    }

    public byte[] read(String path) {
        ReentrantReadWriteLock lock = getLock(path);
        lock.readLock().lock();
        try {
            Node node = resolve(path, true);
            return (node instanceof FileNode) ? ((FileNode) node).read() : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean write(String path, byte[] content) {
        ReentrantReadWriteLock lock = getLock(path);
        lock.writeLock().lock();
        try {
            Node node = resolve(path, true);
            if (!(node instanceof FileNode)) return false;

            // in-mem
            ((FileNode) node).write(content);

            // write-through
            if (writeThrough && mountedRoot != null) {
                try {
                    Path rp = realPath(path);
                    if (rp.getParent() != null) Files.createDirectories(rp.getParent());
                    Files.write(
                        rp,
                        content,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                    );
                } catch (IOException e) {
                    System.err.println("write-through failed for " + path + ": " + e.getMessage());
                    return false;
                }
            }
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean removeNode(String path) {
        String[] parts = tokenize(path);
        String name = parts[parts.length - 1];
        DirectoryNode parent = (DirectoryNode) resolveParent(path);
        if (parent == null) return false;

        Node victim = parent.children.get(name);
        if (victim == null) return false;

        // ⬇️ write-through: elimina su disco prima
        if (writeThrough && mountedRoot != null) {
            try {
                Path rp = realPath(path);
                if (victim instanceof DirectoryNode) {
                    // directory: ci aspettiamo sia vuota (già verificato in rmdir)
                    Files.delete(rp);
                } else {
                    Files.deleteIfExists(rp);
                }
            } catch (IOException e) {
                System.err.println("delete write-through failed for " + path + ": " + e.getMessage());
                return false;
            }
        }

        // aggiornamento in memoria
        parent.children.remove(name);
        return true;
    }

    public static FileSystem mount(String rootPath) {
        File rootFile = new File(rootPath);
        if (!rootFile.exists() || !rootFile.isDirectory()) {
            throw new IllegalArgumentException("Invalid root directory: " + rootPath);
        }

        DirectoryNode rootNode = buildTreeFromDisk(rootFile, null);
        rootNode.name = "/"; // normalizziamo il nome della radice

        FileSystem fs = new FileSystem();
        fs.root = rootNode; // siamo nella stessa classe: ok
        fs.mountedRoot = rootFile.toPath().toAbsolutePath().normalize();
        return fs;
    }

    private static DirectoryNode buildTreeFromDisk(File dir, DirectoryNode parent) {
        DirectoryNode dirNode = new DirectoryNode(dir.getName());
        dirNode.parent = parent;

        File[] entries = dir.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                try {
                    Node childNode;
                    if (Files.isSymbolicLink(entry.toPath())) {
                        String target = Files.readSymbolicLink(entry.toPath()).toString();
                        SymlinkNode symNode = new SymlinkNode(entry.getName(), target);
                        symNode.parent = dirNode;
                        childNode = symNode;
                    } else if (entry.isDirectory()) {
                        childNode = buildTreeFromDisk(entry, dirNode);
                    } else if (entry.isFile()) {
                        FileNode fileNode = new FileNode(entry.getName());
                        fileNode.parent = dirNode;
                        fileNode.data = Files.readAllBytes(entry.toPath());
                        childNode = fileNode;
                    } else {
                        continue; // altri tipi ignorati
                    }
                    dirNode.children.put(childNode.name, childNode);
                } catch (IOException e) {
                    System.err.println("Errore leggendo " + entry.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }

        return dirNode;
    }

    public void tree() {
        printTree(this.root, "");
    }

    private void printTree(Node node, String indent) {
        System.out.println(indent + node.getClass().getSimpleName() + ": " + node.name);

        if (node instanceof SymlinkNode) {
            System.out.println(indent + "  → symbolic link to: " + ((SymlinkNode) node).readLink());
        } else if (node instanceof DirectoryNode) {
            for (Node child : ((DirectoryNode) node).children.values()) {
                printTree(child, indent + "  ");
            }
        }
    }
}

