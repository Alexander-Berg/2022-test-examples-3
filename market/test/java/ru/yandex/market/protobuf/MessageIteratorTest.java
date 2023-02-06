package ru.yandex.market.protobuf;

import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.protobuf.readers.MessageReader;
import ru.yandex.market.protobuf.tools.MessageIterator;

/**
 * Тест проверяет, что MessageIterator работает корректно
 * при отсутствующих, единичных и больших объемах данных.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MessageIteratorTest {

    @Test(expected = NoSuchElementException.class)
    public void checkEmpty() throws Exception {
        MessageReader<Object> messageReader = new ObjectMessageReader(0);
        MessageIterator<Object> messageIterator = new MessageIterator<>(messageReader);

        Assert.assertFalse(messageIterator.hasNext());
        messageIterator.next();
    }

    @Test
    public void checkOne() throws Exception {
        MessageReader<Object> messageReader = new ObjectMessageReader(1);
        MessageIterator<Object> messageIterator = new MessageIterator<>(messageReader);

        Assert.assertTrue(messageIterator.hasNext());
        Assert.assertNotNull(messageIterator.next());
        Assert.assertFalse(messageIterator.hasNext());
        Assert.assertFalse(messageIterator.hasNext());
    }

    @Test
    public void checkTen() throws Exception {
        checkMultiTimes(10);
    }

    @Test
    public void checkBigNumber() throws Exception {
        checkMultiTimes(100500);
    }

    private void checkMultiTimes(int count) throws Exception {
        MessageReader<Object> messageReader = new ObjectMessageReader(count);
        MessageIterator<Object> messageIterator = new MessageIterator<>(messageReader);

        int i = 0;
        for (; messageIterator.hasNext(); i++) {
            messageIterator.next();
            Assert.assertTrue(i < count);
        }

        Assert.assertEquals(i, count);
        Assert.assertFalse(messageIterator.hasNext());
    }
}
