package ru.yandex.market.protobuf.readers;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.protobuf.ObjectMessageReader;
import ru.yandex.market.protobuf.tools.MessageIterator;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class AutoClosableMessageReaderTest {

    @Test
    public void testEmptySubReader() throws IOException {
        ObjectMessageReader subReader = new ObjectMessageReader(0);
        AutoClosableMessageReader autoClosableMessageReader = new AutoClosableMessageReader<>(subReader);
        Assert.assertFalse(subReader.isClosed());

        Assert.assertNull(autoClosableMessageReader.read());
        Assert.assertTrue(subReader.isClosed());

        // one more call
        try {
            autoClosableMessageReader.read();
            Assert.fail("Expected exception");
        } catch (Exception e) {
            Assert.assertEquals("Trying to get object from closed message reader", e.getMessage());
        }
        Assert.assertTrue(subReader.isClosed());

        autoClosableMessageReader.close(); // no effect
        autoClosableMessageReader.close(); // no effect
    }

    @Test
    public void testSubReaderWith1Invokation() throws IOException {
        ObjectMessageReader subReader = new ObjectMessageReader(1);
        AutoClosableMessageReader autoClosableMessageReader = new AutoClosableMessageReader<>(subReader);
        Assert.assertFalse(subReader.isClosed());

        Assert.assertNotNull(autoClosableMessageReader.read());
        Assert.assertFalse(subReader.isClosed());

        Assert.assertNull(autoClosableMessageReader.read());
        Assert.assertTrue(subReader.isClosed());

        // one more call
        try {
            autoClosableMessageReader.read();
            Assert.fail("Expected exception");
        } catch (Exception e) {
            Assert.assertEquals("Trying to get object from closed message reader", e.getMessage());
        }
        Assert.assertTrue(subReader.isClosed());

        autoClosableMessageReader.close(); // no effect
        autoClosableMessageReader.close(); // no effect
    }

    @Test
    public void testSubReaderWith5Invokations() throws IOException {
        ObjectMessageReader subReader = new ObjectMessageReader(5);
        AutoClosableMessageReader autoClosableMessageReader = new AutoClosableMessageReader<>(subReader);
        Assert.assertFalse(subReader.isClosed());

        for (int i = 0; i < 5; i++) {
            Assert.assertNotNull(autoClosableMessageReader.read());
            Assert.assertFalse(subReader.isClosed());
        }

        // one more call
        Assert.assertNull(autoClosableMessageReader.read());
        Assert.assertTrue(subReader.isClosed());

        autoClosableMessageReader.close(); // no effect
        autoClosableMessageReader.close(); // no effect
    }

    @Test
    public void testReaderWillBeClosedIfItWrapToIterator() throws IOException {
        ObjectMessageReader subReader = new ObjectMessageReader(5);
        AutoClosableMessageReader<Object> autoClosableMessageReader = new AutoClosableMessageReader<>(subReader);
        MessageIterator messageIterator = new MessageIterator<>(autoClosableMessageReader);

        Assert.assertFalse(subReader.isClosed());
        messageIterator.forEachRemaining(o -> { });

        Assert.assertTrue(subReader.isClosed());

        autoClosableMessageReader.close(); // no effect
        autoClosableMessageReader.close(); // no effect
    }

    @Test
    public void testReaderWillBeClosedIfItWrapToIteratorAndClose() throws IOException {
        try (ObjectMessageReader subReader = new ObjectMessageReader(5)) {
            AutoClosableMessageReader<Object> autoClosableMessageReader = new AutoClosableMessageReader<>(subReader);
            try (MessageIterator messageIterator = new MessageIterator<>(autoClosableMessageReader)) {
                Assert.assertFalse(subReader.isClosed());
                messageIterator.forEachRemaining(o -> { });
                Assert.assertTrue(subReader.isClosed());
            }

            autoClosableMessageReader.close(); // no effect
            autoClosableMessageReader.close(); // no effect
        }
    }
}
