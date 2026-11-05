package src;

import java.util.HashMap;
import java.util.Map;

/**
 * Узел виртуальной файловой системы (файл или каталог).
 */
public class VfsNode {
    private final String name;
    private final boolean isDirectory;
    private final Map<String, VfsNode> children;
    private byte[] content;

    public VfsNode(String name, boolean isDirectory) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.children = isDirectory ? new HashMap<>() : null;
        this.content = isDirectory ? null : new byte[0];
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public Map<String, VfsNode> getChildren() {
        return children;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void addChild(VfsNode child) {
        if (isDirectory && children != null) {
            children.put(child.getName(), child);
        }
    }
}
