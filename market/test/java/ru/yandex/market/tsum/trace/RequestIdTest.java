package ru.yandex.market.tsum.trace;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.tsum.trace.model.RequestId;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 05.09.16
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RequestIdTest {
    @Test
    public void testParse() throws Exception {
        RequestId id = RequestId.parse("1472563944638/08ee2513df933c1ae7bf5b73f04b8ea4/2/3/6/5/4");

        Assert.assertEquals(1472563944638L, id.getTimestampMillis());
        Assert.assertEquals("08ee2513df933c1ae7bf5b73f04b8ea4", id.getHash());
        Assert.assertArrayEquals(new int[]{2, 3, 6, 5, 4}, id.getSeq());
    }

    @Test
    public void testParseIdWithoutSeq() throws Exception {
        RequestId id = RequestId.parse("1472563944638/08ee2513df933c1ae7bf5b73f04b8ea4");

        Assert.assertEquals(1472563944638L, id.getTimestampMillis());
        Assert.assertEquals("08ee2513df933c1ae7bf5b73f04b8ea4", id.getHash());
        Assert.assertArrayEquals(new int[]{}, id.getSeq());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidRequestId() {
        RequestId id = RequestId.parse("1472563944638");
    }

    @Test
    public void testToString() throws Exception {
        RequestId requestId = new RequestId(
            1472563944638L, "08ee2513df933c1ae7bf5b73f04b8ea4", new int[]{2, 3, 6, 5, 4});

        Assert.assertEquals("1472563944638/08ee2513df933c1ae7bf5b73f04b8ea4/2/3/6/5/4", requestId.toString());
    }
}
