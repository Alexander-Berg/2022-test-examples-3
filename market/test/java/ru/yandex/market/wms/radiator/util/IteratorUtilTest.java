package ru.yandex.market.wms.radiator.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

class IteratorUtilTest {

    @Test
    public void mergeSortedPreferLeft() {
        Assert.assertEquals(
                expected(),
                toList(IteratorUtil.mergeSortedPreferLeft(l1().iterator(), l2().iterator(), Comparator.comparing(e -> e.key)))
        );
        Assert.assertEquals(
                expected().subList(0, 5),
                toList(IteratorUtil.mergeSortedPreferLeft(l1().iterator(), l2().iterator(), Comparator.comparing(e -> e.key)), 5)
        );
    }


    static class Node implements Comparable<Node> {
        private final int key;
        private final int value;

        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public int compareTo(Node o) {
            return Integer.compare(key, o.key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Node node = (Node) o;
            return key == node.key &&
                    value == node.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }

        @Override
        public String toString() {
            return "Node{key=" + key + ", value=" + value + '}';
        }
    }

    private static List<Node> expected() {
        return Arrays.asList(n(0, 0), n(1, -1), n(2, -2), n(3, -3), n(5, 5), n(10, -10), n(11, 11));
    }

    private static List<Node> l1() {
        return Arrays.asList(n(1, -1), n(2, -2), n(3, -3), n(10, -10));
    }

    private List<Node> l2() {
        return Arrays.asList(n(0, 0), n(2, 2), n(5, 5), n(10, 10), n(11, 11));
    }

    static List<Node> toList(Supplier<Node> s) {
        return Stream.generate(s).takeWhile(Objects::nonNull).collect(Collectors.toList());
    }

    static List<Node> toList(Supplier<Node> s, int limit) {
        return Stream.generate(s).takeWhile(Objects::nonNull).limit(limit).collect(Collectors.toList());
    }

    static Node n(int key, int value) {
        return new Node(key, value);
    }
}
