package ru.yandex.common.util.collections;

import junit.framework.TestCase;

/**
 * Date: 10.10.2010
 * Time: 20:23:11
 *
 * @author Dima Schitinin, dimas@yandex-team.ru
 */
public class DisjointSetTest extends TestCase {
    public void testDisjointSet() throws Exception {
        final DisjointSet<Integer> disjointSet = new DisjointSet<Integer>();
        disjointSet.add(1);
        disjointSet.add(2);
        disjointSet.union(1, 2);

        disjointSet.add(10);
        disjointSet.add(20);
        disjointSet.union(10, 20);

        disjointSet.add(40);
        disjointSet.union(40, 20);

        disjointSet.unionMayNotAppear(20, 100);
        disjointSet.unionMayNotAppear(200, 300);
        disjointSet.unionMayNotAppear(400, 200);
        disjointSet.unionMayNotAppear(400, 500);
        disjointSet.unionMayNotAppear(200, 600);

        final MultiMap<Integer, Integer> sets = disjointSet.getSetsMap();
        assertEquals(3, sets.keyCount());
    }
}
