package ru.yandex.search.mail.yt.consumer.cypress;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CypressNode {
    public static final String ROOT = "//";
    public static final String SEPARATOR = "/";
    public static final String ROW_COUNT_ATTR = "row_count";

    private final NodeType type;
    private final String path;
    private final String name;
    private final Map<String, CypressNode> children;
    private final List<String> data;
    private final Map<String, String> attributes;
    private final CypressNode parent;

    public CypressNode(
        final CypressNode parent,
        final CypressNode source,
        final String name)
    {
        this.parent = parent;
        this.children = source.children;
        this.data = source.data;
        this.attributes = source.attributes;
        this.type = source.type;

        this.name = name;

        if (parent.root()) {
            this.path = parent.path + name;
        } else {
            this.path = parent.path + SEPARATOR + name;
        }
    }

    public CypressNode(
        final CypressNode parent,
        final NodeType type,
        final String name)
    {
        this.type = type;
        this.parent = parent;
        this.name = name;
        this.children = new LinkedHashMap<>();

        if (parent.root()) {
            this.path = parent.path + name;
        } else {
            this.path = parent.path + SEPARATOR + name;
        }

        parent.add(this);

        this.data = new ArrayList<>();
        this.attributes = new LinkedHashMap<>();
        initAttributes();
    }

    private CypressNode() {
        this.type = NodeType.MAP_NODE;
        this.path = ROOT;
        this.name = ROOT;
        this.parent = null;

        this.children = new LinkedHashMap<>();
        this.data = new ArrayList<>();
        this.attributes = new LinkedHashMap<>();
        initAttributes();
    }

    private void initAttributes() {
        this.set(ROW_COUNT_ATTR, String.valueOf(data.size()));
    }

    public boolean root() {
        return parent == null;
    }

    public synchronized CypressNode remove(final String name) {
        return children.remove(name);
    }

    public synchronized CypressNode add(final CypressNode node) {
        this.children.put(node.name(), node);
        return this;
    }

    public synchronized CypressNode add(final String name) {
        CypressNode node =
            new CypressNode(this, NodeType.MAP_NODE, name);
        this.add(node);
        return node;
    }

    public synchronized CypressNode addTable(final String name) {
        CypressNode node =
            new CypressNode(this, NodeType.TABLE, name);
        this.add(node);
        return node;
    }

    public NodeType type() {
        return type;
    }

    public String path() {
        return path;
    }

    public String name() {
        return name;
    }

    public synchronized Map<String, CypressNode> children() {
        return children;
    }

    public CypressNode parent() {
        return parent;
    }

    public synchronized void set(final String name, final String value) {
        this.attributes.put(name, value);
    }

    public synchronized String get(final String name) {
        return this.attributes.get(name);
    }

    public synchronized List<String> data() {
        return data;
    }

    public synchronized void write(final List<String> data) {
        this.data.clear();
        this.data.addAll(data);
        this.set(ROW_COUNT_ATTR, String.valueOf(data.size()));
    }

    public static CypressNode createRoot() {
        return new CypressNode();
    }

    @Override
    public String toString() {
        StringBuilder sb =
            new StringBuilder(
                "[Node name=" + name + " path="
                    + path + " type=" + type + ']');

        sb.append('\n');
        for (Map.Entry<String, CypressNode> entry: children.entrySet()) {
            sb.append('\t');
            sb.append(entry.getValue().toString());
        }

        return sb.toString();
    }
}
