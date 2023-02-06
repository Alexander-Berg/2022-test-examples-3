package ru.yandex.collection;

import java.net.InetAddress;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class BinaryTrieTest extends TestBase {
    private static final int ITERATION_LIMIT = 1024;
    private static final int BYTE_MASK = 255;
    private static final int BYTE_SHIFT = 8;

    private static byte reverseByte(final int b) {
        return (byte) Integer.reverseBytes(Integer.reverse(b));
    }

    private static ByteArrayBitIterator empty() {
        return new ByteArrayBitIterator(new byte[0]);
    }

    private static ByteArrayBitIterator zero() {
        return new ByteArrayBitIterator(new byte[1], 0, 1);
    }

    private static ByteArrayBitIterator one() {
        return new ByteArrayBitIterator(new byte[] {reverseByte(1)}, 0, 1);
    }

    private static ByteArrayBitIterator zeroZero() {
        return new ByteArrayBitIterator(new byte[1], 0, 2);
    }

    private static ByteArrayBitIterator oneZero() {
        return new ByteArrayBitIterator(new byte[] {reverseByte(1)}, 0, 2);
    }

    private static void checkLengths(
        final String newString,
        final String prevString)
    {
        if (newString.length() <= prevString.length()) {
            throw new AssertionError(
                '<' + newString + "> expected to be longer than <"
                + prevString + '>');
        }
    }

    @Test
    public void test() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object obj3 = new Object();
        Object obj4 = new Object();
        BinaryTrieMap<Object> trie = new BinaryTrieMap<>();

        Assert.assertNull(trie.get(zero()));
        Assert.assertNull(trie.put(zero(), obj2));
        Assert.assertSame(obj2, trie.get(zero()));
        Assert.assertNull(trie.getShallowest(one()));

        Assert.assertNull(trie.get(one()));
        Assert.assertNull(trie.put(one(), obj3));
        Assert.assertSame(obj3, trie.get(one()));

        Assert.assertNull(trie.get(oneZero()));
        Assert.assertNull(trie.put(oneZero(), obj4));
        Assert.assertSame(obj4, trie.get(oneZero()));

        Assert.assertSame(obj2, trie.getShallowest(zeroZero()));
        Assert.assertSame(obj2, trie.getDeepest(zeroZero()));

        Assert.assertSame(obj3, trie.getShallowest(oneZero()));
        Assert.assertSame(obj4, trie.getDeepest(oneZero()));

        Assert.assertNull(trie.get(empty()));
        Assert.assertNull(trie.getShallowest(empty()));
        Assert.assertNull(trie.put(empty(), obj1));
        Assert.assertSame(obj1, trie.get(empty()));

        Assert.assertSame(obj1, trie.put(empty(), obj2));
        Assert.assertSame(obj2, trie.get(empty()));
        Assert.assertSame(obj2, trie.getShallowest(oneZero()));
        Assert.assertSame(obj4, trie.getDeepest(oneZero()));
    }

    @Test
    public void testResize() {
        BinaryTrieMap<Object> trie = new BinaryTrieMap<>();
        Object[] objs = new Object[ITERATION_LIMIT];
        String prevString = "";
        for (int i = 0; i < ITERATION_LIMIT; ++i) {
            objs[i] = new Object();
            Assert.assertNull(
                trie.put(
                    new ByteArrayBitIterator(
                        new byte[] {
                            (byte) (i & BYTE_MASK),
                            (byte) (i >> BYTE_SHIFT)
                        }),
                    objs[i]));
            String newString = trie.toString();
            checkLengths(newString, prevString);
            prevString = newString;
            for (int j = 0; j <= i; ++j) {
                Assert.assertSame(
                    objs[j],
                    trie.get(
                        new ByteArrayBitIterator(
                            new byte[] {
                                (byte) (j & BYTE_MASK),
                                (byte) (j >> BYTE_SHIFT)
                            })));
            }
            for (int j = i + 1; j < ITERATION_LIMIT; ++j) {
                Assert.assertNull(
                    trie.get(
                        new ByteArrayBitIterator(
                            new byte[] {
                                (byte) (j & BYTE_MASK),
                                (byte) (j >> BYTE_SHIFT)
                            })));
            }
        }
    }

    @Test
    public void testSet() {
        BinaryTrieSet set = new BinaryTrieSet();
        Assert.assertFalse(set.contains(empty()));
        Assert.assertFalse(set.contains(zero()));
        Assert.assertFalse(set.contains(one()));
        Assert.assertFalse(set.contains(oneZero()));
        Assert.assertFalse(set.contains(zeroZero()));
        Assert.assertFalse(set.containsPrefix(oneZero()));
        Assert.assertFalse(set.containsPrefix(zeroZero()));

        Assert.assertTrue(set.add(zero()));
        Assert.assertTrue(set.contains(zero()));
        Assert.assertFalse(set.containsPrefix(oneZero()));
        Assert.assertTrue(set.containsPrefix(zeroZero()));
        Assert.assertFalse(set.add(zero()));
        Assert.assertTrue(set.contains(zero()));

        Assert.assertFalse(set.contains(oneZero()));
        Assert.assertFalse(set.containsPrefix(oneZero()));
        Assert.assertTrue(set.add(one()));
        Assert.assertTrue(set.contains(one()));
        Assert.assertTrue(set.containsPrefix(oneZero()));
        Assert.assertFalse(set.add(one()));
        Assert.assertTrue(set.contains(one()));
        Assert.assertTrue(set.containsPrefix(oneZero()));

        Assert.assertFalse(set.contains(empty()));
        Assert.assertFalse(set.containsPrefix(empty()));
        Assert.assertFalse(set.contains(oneZero()));
        Assert.assertFalse(set.contains(zeroZero()));
        Assert.assertTrue(set.containsPrefix(oneZero()));
        Assert.assertTrue(set.containsPrefix(zeroZero()));

        Assert.assertTrue(set.add(zeroZero()));
        Assert.assertTrue(set.contains(zeroZero()));
        Assert.assertTrue(set.containsPrefix(zeroZero()));
        Assert.assertFalse(set.add(zeroZero()));
    }

    @Test
    public void testSetResize() {
        BinaryTrieSet set = new BinaryTrieSet();
        String prevString = "";
        for (int i = 0; i < ITERATION_LIMIT; ++i) {
            Assert.assertTrue(
                set.add(
                    new ByteArrayBitIterator(
                        new byte[] {
                            (byte) (i & BYTE_MASK),
                            (byte) (i >> BYTE_SHIFT)
                        })));
            String newString = set.toString();
            checkLengths(newString, prevString);
            prevString = newString;
            for (int j = 0; j <= i; ++j) {
                Assert.assertTrue(
                    set.contains(
                        new ByteArrayBitIterator(
                            new byte[] {
                                (byte) (j & BYTE_MASK),
                                (byte) (j >> BYTE_SHIFT)
                            })));
            }
            for (int j = i + 1; j < ITERATION_LIMIT; ++j) {
                Assert.assertFalse(
                    set.contains(
                        new ByteArrayBitIterator(
                            new byte[] {
                                (byte) (j & BYTE_MASK),
                                (byte) (j >> BYTE_SHIFT)
                            })));
            }
        }
    }

    @Test
    public void testCidrMatch() throws Exception {
        BinaryTrieSet set = new BinaryTrieSet();
        final int len = 23;
        set.add(
            new ByteArrayBitIterator(
                InetAddress.getByName("77.88.46.0").getAddress(),
                0,
                len));
        Assert.assertTrue(
            set.containsPrefix(
                new ByteArrayBitIterator(
                    InetAddress.getByName("77.88.47.126").getAddress())));
    }
}

