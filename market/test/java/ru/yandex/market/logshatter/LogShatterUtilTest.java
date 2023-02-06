package ru.yandex.market.logshatter;

import com.google.common.collect.Range;
import junit.framework.Assert;
import org.junit.Test;

import ru.yandex.market.health.configs.logshatter.LogShatterUtil;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 08/02/16
 */
public class LogShatterUtilTest {

    @Test
    public void testCreateRange() throws Exception {
        Assert.assertEquals(Range.closed(1454949780, 1454949840), LogShatterUtil.createRange(1454949789, 60));
        Assert.assertEquals(Range.closed(1454949780, 1454949840), LogShatterUtil.createRange(1454949780, 60));
        Assert.assertEquals(Range.closed(1454949780, 1454949840), LogShatterUtil.createRange(1454949839, 60));

    }
}
