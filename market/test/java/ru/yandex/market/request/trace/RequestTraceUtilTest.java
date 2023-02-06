package ru.yandex.market.request.trace;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 01/08/16
 */
public class RequestTraceUtilTest {
    @Test
    public void generateRequestId() throws Exception {
        String id = RequestTraceUtil.generateRequestId();
        Assert.assertEquals(46, id.length());
    }
}