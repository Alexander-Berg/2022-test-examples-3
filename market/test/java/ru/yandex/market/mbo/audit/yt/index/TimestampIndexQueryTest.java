package ru.yandex.market.mbo.audit.yt.index;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.audit.yt.SearchFields;
import ru.yandex.market.mbo.audit.yt.YtAuditFilter;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.yt.index.read.Direction;

/**
 * @author apluhin
 * @created 7/14/21
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TimestampIndexQueryTest {

    @Test
    public void testSearchByAll() {
        YtAuditFilter auditFilter = new YtAuditFilter();
        auditFilter.entityTypes(Arrays.asList(MboAudit.EntityType.CM_BLUE_OFFER));
        auditFilter.timestampFrom(100L);
        auditFilter.timestampTo(200L);
        auditFilter.addOrderBy(SearchFields.TIMESTAMP, Direction.DESC);
        Assert.assertTrue(TimestampIndexQuery.isSupportFilter(auditFilter));
        String query = new TimestampIndexQuery(auditFilter).query(true);
        Assert.assertEquals("timestamp >= 100 AND timestamp < 200 " +
            "AND entity_type in (16) ORDER BY timestamp DESC", query);
    }

    @Test
    public void testFailedCheckWithoutMainKey() {
        YtAuditFilter auditFilter = new YtAuditFilter();
        auditFilter.timestampFrom(100L);
        auditFilter.timestampTo(200L);
        Assert.assertFalse(TimestampIndexQuery.isSupportFilter(auditFilter));
    }

}
