package ru.yandex.market.mbo.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import ru.yandex.market.mbo.common.ZooKeeper.ZooKeeperService;

/**
 * @author ayratgdl
 * @date 07.05.17
 */
public class ZooKeeperServiceMock extends ZooKeeperService {
    private static final String PARENT_NODE_NAME = "root";

    private static class Node {
        String content;
        Map<String, Node> children = new ConcurrentHashMap<>();
    }

    private Node root;

    public ZooKeeperServiceMock() {
        setParentNode(PARENT_NODE_NAME);
        root = new Node();
        root.children.put(PARENT_NODE_NAME, new Node());
    }

    @Override
    public List<String> getChildren(String nodePath) {
        Node node = getNode(nodePath);
        return new ArrayList<>(node.children.keySet());
    }

    @Override
    public String read(String nodePath) {
        return getNode(nodePath).content;
    }

    @Override
    public boolean exists(String nodePath) {
        try {
            getNode(nodePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void create(String nodePath) {
        Node node = getNode(getPrefix(nodePath));
        node.children.put(getSuffix(nodePath), new Node());
    }

    @Override
    public void write(String nodePath, String data) {
        if (!exists(nodePath)) {
            create(nodePath);
        }
        getNode(nodePath).content = data;
    }

    @Override
    public void delete(String nodePath) {
        Node node = getNode(getPrefix(nodePath));
        node.children.remove(getSuffix(nodePath));
    }

    @Override
    public void doWithLock(String path, long time, TimeUnit unit, Runnable action) throws TimeoutException {
        action.run();
    }

    @Override
    public <T> T doWithLock(String path, long time, TimeUnit unit, Supplier<T> action) throws TimeoutException {
        return action.get();
    }

    private Node getNode(String nodePath) {
        Node node = root;
        for (String name : nodePath.split("/")) {
            node = node.children.get(name);

            if (node == null) {
                throw new RuntimeException();
            }
        }
        return node;
    }

    private static String getPrefix(String path) {
        int to = path.lastIndexOf("/");
        if (to == -1) {
            return "";
        }
        return path.substring(0, to);
    }

    private static String getSuffix(String path) {
        int from = path.lastIndexOf("/");
        if (from == path.length() - 1) {
            return "";
        }
        return path.substring(from + 1, path.length());
    }
}
