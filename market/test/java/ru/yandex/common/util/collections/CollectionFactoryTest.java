package ru.yandex.common.util.collections;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author leftie
 *         <p/>
 *         Created 04.10.12 18:14
 */
public class CollectionFactoryTest extends TestCase {

    public void testImmutableList() throws Exception {
        final ArrayList<Object> list = Cf.newList();
        list.add(new Object());
        assertFalse(Cf.isUnmodifiable(list));
        final List<Object> ilist = Cf.immutable(list);
        assertTrue(Cf.isUnmodifiable(ilist));
        assertTrue(ilist != list);
        assertTrue(ilist.equals(list));
        list.add(new Object());
        try {
            ilist.add(new Object());
            fail();
        } catch (Exception ignored) {
        }
        assertEquals(Cf.immutable(list), list);
    }

    public void testImmutableSet() throws Exception {
        final HashSet<Object> set = Cf.newUnorderedSet();
        set.add(new Object());
        assertFalse(Cf.isUnmodifiable(set));
        final Set<Object> iset = Cf.immutable(set);
        assertTrue(Cf.isUnmodifiable(iset));
        assertTrue(iset != set);
        assertTrue(iset.equals(set));
        set.add(new Object());
        try {
            iset.add(new Object());
            fail();
        } catch (Exception ignored) {
        }
        assertEquals(Cf.immutable(set), set);
    }

    public void testImmutableMap() throws Exception {
        final HashMap<Object, Object> map = Cf.newUnorderedMap();
        map.put(new Object(), new Object());
        assertFalse(Cf.isUnmodifiable(map));
        final Map<Object, Object> imap = Cf.immutable(map);
        assertTrue(Cf.isUnmodifiable(imap));
        assertTrue(imap != map);
        assertTrue(imap.equals(map));
        map.put(new Object(), new Object());
        try {
            imap.put(new Object(), new Object());
            fail();
        } catch (Exception ignored) {
        }
        assertEquals(Cf.immutable(map), map);
    }

    public void testImmutableCollection() throws Exception {
        final Collection<Object> col = Cf.newList();
        col.add(new Object());
        assertFalse(Cf.isUnmodifiable(col));
        final Collection<Object> icol = Cf.immutable(col);
        assertTrue(Cf.isUnmodifiable(icol));
        assertTrue(icol != col);
        assertEquals(Cf.newList(icol), Cf.newList(col));
        col.add(new Object());
        try {
            icol.add(new Object());
            fail();
        } catch (Exception ignored) {
        }
        assertEquals(Cf.newList(Cf.immutable(col)), Cf.newList(col));
    }

    public void testImmutableSset() throws Exception {
        final TreeSet<Object> sset = Cf.newTreeSet();
        sset.add(new String());
        assertFalse(Cf.isUnmodifiable(sset));
        final SortedSet<Object> isset = Cf.immutable(sset);
        assertTrue(Cf.isUnmodifiable(isset));
        assertTrue(isset != sset);
        assertTrue(isset.equals(sset));
        sset.add(new String());
        try {
            isset.add(new String());
            fail();
        } catch (Exception ignored) {
        }
        assertEquals(Cf.immutable(sset), sset);
    }

    public void testImmutableSmap() throws Exception {
        final TreeMap<Object, Object> smap = Cf.newTreeMap();
        smap.put(new String(), new Object());
        assertFalse(Cf.isUnmodifiable(smap));
        final SortedMap<Object, Object> ismap = Cf.immutable(smap);
        assertTrue(Cf.isUnmodifiable(ismap));
        assertTrue(ismap != smap);
        assertTrue(ismap.equals(smap));
        smap.put(new String(), new Object());
        try {
            ismap.put(new String(), new Object());
            fail();
        } catch (Exception ignored) {
        }
        assertEquals(Cf.immutable(smap), smap);
    }

}
