package ru.yandex.market.mbo.yt;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.BatchRequest;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeBinarySerializer;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.yt.ytclient.proxy.request.ColumnFilter;
import ru.yandex.yt.ytclient.proxy.request.CopyNode;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.ExistsNode;
import ru.yandex.yt.ytclient.proxy.request.GetNode;
import ru.yandex.yt.ytclient.proxy.request.LinkNode;
import ru.yandex.yt.ytclient.proxy.request.ListNode;
import ru.yandex.yt.ytclient.proxy.request.LockNode;
import ru.yandex.yt.ytclient.proxy.request.MoveNode;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;
import ru.yandex.yt.ytclient.proxy.request.RemoveNode;
import ru.yandex.yt.ytclient.proxy.request.SetNode;
import ru.yandex.yt.ytclient.proxy.request.TransactionalOptions;

/**
 * @author york
 * @since 26.04.2018
 */
public class TestCypress implements Cypress {

    private final TestYt testYt;

    private final NavigableMap<YPath, CypressNode> nodes = Collections.synchronizedNavigableMap(
            new TreeMap<>(Comparator.comparing(YPath::toString))
    );

    private final NavigableMap<YPath, YPath> links = Collections.synchronizedNavigableMap(
            new TreeMap<>(Comparator.comparing(YPath::toString))
    );

    TestCypress(TestYt testYt) {
        this.testYt = testYt;
    }

    private boolean startsWithHeadPath(YPath path, YPath headPath) {
        String headPathStr = headPath.toString();
        String pathStr = path.toString();

        if (pathStr.equals(headPathStr)) {
            return true;
        }

        if (pathStr.startsWith(headPathStr)) {
            // //home/path/table and //home/path/table_2 have nothing in common
            String parentStr = path.parent().toString();
            if (parentStr.startsWith(headPathStr)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void create(CreateNode req) {
        YPath path = req.getPath();
        ObjectType type = req.getType();
        boolean recursive = req.isRecursive();
        boolean ignoreExisting = req.isIgnoreExisting();
        Optional<TransactionalOptions> transactionalOptions = req.getTransactionalOptions();
        Map<String, YTreeNode> attributes = req.getAttributes();

        synchronized (nodes) {
            if (recursive && !path.isRoot() && !nodes.containsKey(path.parent())) {
                CreateNode node = new CreateNode(path.parent(), CypressNodeType.MAP, Collections.emptyMap())
                        .setTransactionalOptions(transactionalOptions.orElse(null))
                        .setRecursive(true)
                        .setIgnoreExisting(false);
                create(node);
            }

            CypressNode node = nodes.get(path);
            if (node != null && !ignoreExisting) {
                throw new RuntimeException("node '" + path + "' already exists");
            }
            for (Map.Entry<YPath, CypressNode> head : nodes.headMap(path, false).entrySet()) {
                YPath headPath = head.getKey();
                if (startsWithHeadPath(path, headPath)) {
                    if (!compareTransactions(transactionalOptions, head.getValue().transactionId)) {
                        throw new RuntimeException("node '" + headPath + "' in other transaction. " +
                                "Current transaction: " + transactionalOptions + ", " +
                                "node: " + head.getValue().transactionId);
                    }
                    if (!CypressNodeType.MAP.equals(head.getValue().type)) {
                        throw new RuntimeException("node '" + headPath + "' is not map");
                    }
                }
            }
            HashMap<String, YTreeNode> copyAttributes = new HashMap<>(attributes);
            if (type == ObjectType.from(CypressNodeType.TABLE)) {
                // compute if created table is sorted
                List<String> sortedColumns = Optional.ofNullable(copyAttributes.get("schema"))
                        .map(schema -> schema.asList().stream()
                                .map(YTreeNode::mapNode)
                                .filter(s -> s.containsKey("sort_order"))
                                .map(s -> s.getString("name"))
                                .collect(Collectors.toList()))
                        .orElse(null);
                if (sortedColumns != null && !sortedColumns.isEmpty()) {
                    YTreeNode sortByYTree = YTree.builder().value(sortedColumns).build();
                    copyAttributes.computeIfAbsent("sorted_by", s -> sortByYTree);
                    copyAttributes.computeIfAbsent("sorted", s -> YTree.booleanNode(true));
                } else {
                    copyAttributes.computeIfAbsent("sorted", s -> YTree.booleanNode(false));
                }
                testYt.tables().createIfNotExists(path);
            }
            nodes.put(path, new CypressNode(path, transactionalOptions.flatMap(v -> v.getTransactionId()),
                    type.toCypressNodeType(), copyAttributes));
        }
    }

    @Override
    public void remove(RemoveNode req) {
        YPath path = req.getPath();
        boolean recursive = req.isRecursive();
        boolean force = req.isForce();
        Optional<TransactionalOptions> transactionalOptions = req.getTransactionalOptions();

        synchronized (nodes) {
            CypressNode node = nodes.get(path);
            if (node == null || !compareTransactions(transactionalOptions, node.transactionId)) {
                if (!force) {
                    throw new RuntimeException("no such node " + path);
                }
                return;
            }
            String attrName = getAttributeName(path, false);
            if (attrName != null) {
                node.attributes.remove(attrName);
            } else {
                for (CypressNode childNode : getSubNodes(path)) {
                    if (recursive) {
                        nodes.remove(childNode.path);
                        testYt.tables().remove(childNode.path);
                    } else {
                        throw new RuntimeException("Child exists " + childNode.path);
                    }
                }
                nodes.remove(path);
                testYt.tables().remove(path);
            }
        }
    }

    @Override
    public void set(SetNode req) {
        YPath path = req.getPath();
        Optional<TransactionalOptions> transactionalOptions = req.getTransactionalOptions();
        YTreeNode value = YTreeBinarySerializer.deserialize(new ByteArrayInputStream(req.getValue()));
        synchronized (nodes) {
            // we take parent() to process path kind '//home/path/table/@unflushed_timestamp'
            CypressNode node = getNodeOrThrow(transactionalOptions, path.parent());
            String attrName = getAttributeName(path, false);
            if (attrName != null) {
                node.attributes.put(attrName, value);
            } else {
                CreateNode createNode = new CreateNode(path, getNodeType(value)).setIgnoreExisting(true);
                transactionalOptions.ifPresent(createNode::setTransactionalOptions);
                create(createNode);

                // Somewhat ugly way to set value on node after it's created.
                CypressNode n = nodes.get(path);
                nodes.put(path, new CypressNode(n.path, n.transactionId, n.type, n.attributes, value));
            }
        }
    }

    @Override
    public YTreeNode get(GetNode getNode) {
        YPath path = getNode.getPath();
        Optional<TransactionalOptions> transactionalOptions = getNode.getTransactionalOptions();
        Optional<ColumnFilter> attributes = getNode.getAttributes();

        synchronized (nodes) {
            String attrName = getAttributeName(path, false);
            if (attrName != null) {
                return getAttribute(transactionalOptions, path.parent(), attrName)
                        .orElseThrow(() -> new RuntimeException("No attribute " + attrName + " at node " + path));
            }

            CypressNode node = getNodeOrThrow(transactionalOptions, path);
            Map<String, YTreeNode> attrs = new HashMap<>();
            if (attributes.isPresent() && !attributes.get().isAllColumns()) {
                for (String attribute : attributes.get().getColumns()) {
                    getAttribute(transactionalOptions, path, attribute).ifPresent(value -> {
                        attrs.put(attribute, value);
                    });
                }
            }
            return createNode(path, node.type, node.value, attrs);
        }
    }

    @Override
    public List<YTreeStringNode> list(ListNode listNode) {
        YPath path = listNode.getPath();
        Optional<ColumnFilter> attributes = listNode.getAttributes();

        List<YTreeStringNode> result = new ArrayList<>();
        synchronized (nodes) {
            for (CypressNode child : getAdjacentChildren(path)) {
                Map<String, YTreeNode> attrs = new HashMap<>();
                if (attributes.isPresent() && !attributes.get().isAllColumns()) {
                    child.attributes.entrySet().stream()
                            .filter(e -> attributes.get().getColumns().contains(e.getKey()))
                            .forEach(e -> attrs.put(e.getKey(), e.getValue()));
                }
                result.add((YTreeStringNode) createNode(child.path, CypressNodeType.STRING,
                        getLastToken(child.path), attrs));
            }
        }
        return result;
    }

    private String getLastToken(YPath path) {
        String pathStr = path.toString();
        List<String> tokens = Arrays.stream(pathStr.split("/"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return tokens.get(tokens.size() - 1);
    }

    @Override
    public GUID lock(LockNode req) {
        return GUID.create();
    }

    @Override
    public GUID copy(CopyNode copyNode) {
        synchronized (nodes) {
            return copyOrMove(
                    copyNode.getTransactionalOptions(),
                    copyNode.getSource(),
                    copyNode.getDestination(),
                    copyNode.getRecursive(),
                    copyNode.getForce(),
                    false
            );
        }
    }

    @Override
    public GUID link(LinkNode linkNode) {
        YPath link = linkNode.getSource();
        YPath target = linkNode.getDestination();
        synchronized (links) {
            links.put(link, target);
        }
        YPath linkPath = link.parent().child(link.name() + "&");
        YPath linkAttributePath = linkPath.attribute("target_path");
        CreateNode createNode = new CreateNode(linkPath, CypressNodeType.STRING)
                .setTransactionalOptions(linkNode.getTransactionalOptions().orElse(null))
                .setRecursive(linkNode.getRecursive())
                .setIgnoreExisting(linkNode.getIgnoreExisting())
                .setForce(linkNode.getForce());

        SetNode setNode = new SetNode(linkAttributePath, YTree.stringNode(target.toString()))
                .setTransactionalOptions(linkNode.getTransactionalOptions().orElse(null))
                .setRecursive(linkNode.getRecursive())
                .setForce(linkNode.getForce());
        create(createNode);
        set(setNode);
        return linkNode.getTransactionalOptions().flatMap(v -> v.getTransactionId()).orElse(GUID.create());
    }

    @Override
    public void move(MoveNode moveNode) {
        synchronized (nodes) {
            copyOrMove(
                    moveNode.getTransactionalOptions(),
                    moveNode.getSource(),
                    moveNode.getDestination(),
                    moveNode.getRecursive(),
                    moveNode.getForce(),
                    true
            );
        }
    }

    @Override
    public boolean exists(ExistsNode existsNode) {
        YPath path = existsNode.getPath();
        Optional<TransactionalOptions> transactionalOptions = existsNode.getTransactionalOptions();
        synchronized (nodes) {
            CypressNode node = getNode(transactionalOptions, path, false);
            YPath targetPath = links.get(path);
            return node != null || targetPath != null;
        }
    }

    @Override
    public void concatenate(@Nullable GUID transactionId, boolean pingAncestorTransactions,
                            List<YPath> sourceTablesOrFiles,
                            YPath destinationTableOrFile) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public BatchRequest executeBatch(@Nullable GUID transactionId,
                                     boolean pingAncestorTransactions,
                                     @Nullable Integer concurrency) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Optional<YPath> getFromCache(YPath cachePath, String md5) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public YPath putToCache(YPath filePath, YPath cachePath, String md5) {
        throw new RuntimeException("Not implemented");
    }

    public void commit(GUID transactionId) {
        synchronized (nodes) {
            List<CypressNode> currentNodes = new ArrayList<>(nodes.values());
            for (CypressNode currentNode : currentNodes) {
                if (currentNode.transactionId.isPresent() && currentNode.transactionId.get().equals(transactionId)) {
                    // remove node, if expiration_time finished
                    if (currentNode.containsAttribute("expiration_time")) {
                        String expirationTimeStr = currentNode.getAttributeString("expiration_time");
                        OffsetDateTime time = OffsetDateTime.parse(expirationTimeStr, DateTimeFormatter.ISO_DATE_TIME);
                        if (!time.isAfter(OffsetDateTime.now())) {
                            remove(currentNode.transactionId, false, currentNode.path, true, false);
                            continue;
                        }
                    }

                    CypressNode copy = new CypressNode(currentNode.path, Optional.empty(), currentNode.type,
                            currentNode.attributes, currentNode.value);
                    nodes.put(copy.path, copy);
                }
            }
        }
    }

    private GUID copyOrMove(Optional<TransactionalOptions> transactionalOptions, YPath source,
                            YPath destination, boolean recursive, boolean force, boolean deleteSource) {
        CypressNode node = getNodeOrThrow(transactionalOptions, source);

        List<CypressNode> targetSubnodes = getSubNodes(destination);
        if (!force && targetSubnodes.size() > 0) {
            throw new RuntimeException("target subnodes exist");
        }
        targetSubnodes.forEach(ts -> {
            nodes.remove(ts.path);
            testYt.tables().remove(ts.path);
        });
        Optional<GUID> destinationGuid = Optional.of(transactionalOptions
                .flatMap(v -> v.getTransactionId())
                .orElse(GUID.create()));
        nodes.put(destination, new CypressNode(destination, destinationGuid, node));
        testYt.tables().copy(source, destination);
        for (CypressNode currentSubnode : getSubNodes(source)) {
            String sourceStr = replaceTrailingSlashes(source.toString());
            String targetStr = replaceTrailingSlashes(destination.toString());
            String pathStr = currentSubnode.path.toString();
            YPath newPath = YPath.simple(pathStr.replace(sourceStr, targetStr));
            nodes.put(newPath, new CypressNode(newPath, destinationGuid, currentSubnode));
            testYt.tables().copy(YPath.simple(pathStr), newPath);
            if (deleteSource) {
                nodes.remove(currentSubnode.path);
                testYt.tables().remove(currentSubnode.path);
            }
        }
        if (deleteSource) {
            nodes.remove(node.path);
            testYt.tables().remove(node.path);
        }
        return destinationGuid.get();
    }

    private String replaceTrailingSlashes(String s) {
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private YTreeNode createNode(YPath path, CypressNodeType type, Map<String, YTreeNode> attrs) {
        return createNode(path, type, null, attrs);
    }

    private YTreeNode createNode(YPath path, CypressNodeType type, Object value, Map<String, YTreeNode> attrs) {
        YTreeNode result;
        switch (type) {
            case MAP:
                List<CypressNode> children = getAdjacentChildren(path);
                if (children.size() == 0) {
                    result = YTree.stringNode(getLastToken(path));
                } else {
                    YTreeBuilder builder = YTree.mapBuilder();
                    for (CypressNode node : children) {
                        builder.key(getLastToken(node.path)).value("#");
                    }
                    result = builder.buildMap();
                }
                break;
            case TABLE:
                // FIXME: What's correct value when getting a TABLE? (.builder().build() doesn't work at all)
                result = YTree.mapBuilder().buildMap();
                break;
            case STRING:
                result = value instanceof YTreeStringNode ? (YTreeNode) value : YTree.stringNode(value.toString());
                break;
            default:
                throw new RuntimeException("Not implemented " + type);
        }
        attrs.entrySet().forEach(e -> {
            result.putAttribute(e.getKey(), e.getValue());
        });
        return result;
    }

    private Optional<YTreeNode> getAttribute(Optional<TransactionalOptions> transactionOptions,
                                             YPath path, String attrName) {
        Optional<GUID> transactionId = transactionOptions.flatMap(v -> v.getTransactionId());
        CypressNode node = getNodeOrThrow(transactionOptions, path);
        YTreeNode attribute = node.attributes.get(attrName);
        if (attribute != null) {
            return Optional.of(attribute);
        }
        if (Arrays.asList("row_count", "unmerged_row_count").contains(attrName)) {
            Iterator<YTreeMapNode> iterator = testYt.tables()
                    .read(transactionId, false, path, YTableEntryTypes.YSON,
                            (Function<Iterator<YTreeMapNode>, Iterator<YTreeMapNode>>) v -> v);
            int count = 0;
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
            return Optional.of(YTree.node(count));
        }
        return Optional.empty();
    }

    private CypressNode getNodeOrThrow(Optional<TransactionalOptions> transactionalOptions, YPath path) {
        return getNode(transactionalOptions, path, true);
    }

    private CypressNode getNode(Optional<TransactionalOptions> transactionalOptions, YPath path, boolean failIfNo) {
        CypressNode node = nodes.get(path.justPath());
        if (failIfNo && (node == null || !compareTransactions(transactionalOptions, node.transactionId))) {
            throw new RuntimeException("no node at path " + path);
        }
        return ((node == null) || !compareTransactions(transactionalOptions, node.transactionId)) ? null : node;
    }

    private String getAttributeName(YPath path, boolean failIfNo) {
        int i = path.justPath().toString().indexOf("/@");
        if (i < 0) {
            if (!failIfNo) {
                return null;
            }
            throw new RuntimeException("no attributeName " + path);
        }
        return path.justPath().toString().substring(i + 2);
    }

    private List<CypressNode> getAdjacentChildren(YPath path) {
        List<CypressNode> result = new ArrayList<>();
        for (Map.Entry<YPath, CypressNode> entry : nodes.tailMap(path, false).entrySet()) {
            if (entry.getKey().parent().toString().equals(path.toString())) {
                result.add(entry.getValue());
            }
        }
        return result;
    }


    private List<CypressNode> getSubNodes(YPath path) {
        return nodes.tailMap(path, false).entrySet().stream()
                .filter(e -> startsWithHeadPath(e.getKey(), path))
                .map(e -> e.getValue())
                .collect(Collectors.toList());
    }

    private boolean compareTransactions(Optional<TransactionalOptions> currentTransaction,
                                        Optional<GUID> prevTransaction) {
        return !prevTransaction.isPresent() ||
                prevTransaction.get().equals(currentTransaction.flatMap(v -> v.getTransactionId()).orElse(null));
    }

    private CypressNodeType getNodeType(YTreeNode value) {
        CypressNodeType type;
        if (value.isStringNode()) {
            type = CypressNodeType.STRING;
        } else if (value.isBooleanNode()) {
            type = CypressNodeType.BOOLEAN;
        } else if (value.isDoubleNode()) {
            type = CypressNodeType.DOUBLE;
        } else if (value.isIntegerNode()) {
            type = CypressNodeType.INT64;
        } else if (value.isMapNode()) {
            type = CypressNodeType.MAP;
        } else {
            throw new IllegalArgumentException("Node type is not yet supported in TestCypress: " + value);
        }
        return type;
    }

    private static class CypressNode {
        final YPath path;
        final Optional<GUID> transactionId;
        final CypressNodeType type;
        final Map<String, YTreeNode> attributes;

        @Nullable
        final YTreeNode value;

        CypressNode(YPath path, Optional<GUID> transactionId,
                    CypressNodeType type, Map<String, YTreeNode> attributes) {
            this(path, transactionId, type, attributes, null);
        }

        CypressNode(YPath path, Optional<GUID> transactionId,
                    CypressNodeType type, Map<String, YTreeNode> attributes, YTreeNode value) {
            this.path = path;
            this.transactionId = transactionId;
            this.type = type;
            this.attributes = attributes;
            this.value = value;

            attributes.put("id", YTree.stringNode(GUID.create().toString()));
        }

        CypressNode(YPath path, Optional<GUID> transactionId, CypressNode source) {
            this(path, transactionId, source.type, new HashMap<>(source.attributes));
        }

        public boolean containsAttribute(String attribute) {
            return attributes.containsKey(attribute);
        }

        public String getAttributeString(String attribute) {
            return attributes.get(attribute).stringValue();
        }
    }
}
