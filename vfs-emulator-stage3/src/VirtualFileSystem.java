package src;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;

/**
 * Виртуальная файловая система, работающая исключительно в оперативной памяти.
 */
public class VirtualFileSystem {
    private VfsNode root;
    private final Deque<String> pathStack;

    public VirtualFileSystem() {
        this.root = new VfsNode("/", true);
        this.pathStack = new ArrayDeque<>();
    }

    public void loadFromXml(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Файл VFS не найден: " + filePath);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        doc.getDocumentElement().normalize();

        Element rootElement = doc.getDocumentElement();
        if (!"vfs".equals(rootElement.getNodeName())) {
            throw new IllegalArgumentException("Корневой элемент XML должен быть <vfs>");
        }

        VfsNode newRoot = new VfsNode("/", true);
        parseChildren(rootElement, newRoot);

        this.root = newRoot;
        this.pathStack.clear();
    }

    private void parseChildren(Element parentElement, VfsNode parentNode) {
        NodeList nodeList = parentElement.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element element = (Element) node;
            String tagName = element.getTagName();
            String name = element.getAttribute("name");

            if (name.isEmpty()) {
                continue;
            }

            if ("dir".equals(tagName)) {
                VfsNode dirNode = new VfsNode(name, true);
                parseChildren(element, dirNode);
                parentNode.addChild(dirNode);
            } else if ("file".equals(tagName)) {
                VfsNode fileNode = new VfsNode(name, false);
                String encoding = element.getAttribute("encoding");
                String rawText = element.getTextContent() != null ? element.getTextContent().trim() : "";

                if ("base64".equalsIgnoreCase(encoding)) {
                    try {
                        fileNode.setContent(Base64.getDecoder().decode(rawText));
                    } catch (IllegalArgumentException e) {
                        fileNode.setContent(rawText.getBytes(StandardCharsets.UTF_8));
                    }
                } else {
                    fileNode.setContent(rawText.getBytes(StandardCharsets.UTF_8));
                }
                parentNode.addChild(fileNode);
            }
        }
    }

    public String getCurrentPath() {
        if (pathStack.isEmpty()) {
            return "/";
        }
        return "/" + String.join("/", pathStack);
    }

    public VfsNode getRoot() {
        return root;
    }
}
