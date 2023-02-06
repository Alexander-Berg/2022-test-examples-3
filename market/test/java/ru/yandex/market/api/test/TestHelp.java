package ru.yandex.market.api.test;

import org.junit.Assert;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertTrue;

public class TestHelp {

    public static byte[] loadStream(InputStream stream) throws Exception {
        final byte[] bytes = new byte[stream.available()];
        stream.read(bytes);
        return bytes;
    }

    public static <E, A> void assertCollectionEquals(E[] expectedArray, List<A> actualList, BiConsumer<E, A> asserter) {
        Assert.assertEquals(expectedArray.length, actualList.size());

        int i = 0;
        for (A actual : actualList) {
            E expected = expectedArray[i++];
            asserter.accept(expected, actual);
        }
    }

    public static <E, A> void assertEquals(E expected, A actual, BiConsumer<E, A> asserter) {
        asserter.accept(expected, actual);
    }

    public static void assertContains(Collection<?> values, Collection<?> collection) {
        assertTrue("НАбор элементов в коллекции отличается от ожидаемого",
                values.containsAll(collection) && collection.containsAll(values));
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
