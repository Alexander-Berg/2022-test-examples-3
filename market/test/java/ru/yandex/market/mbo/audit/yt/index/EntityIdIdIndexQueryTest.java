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
public class EntityIdIdIndexQueryTest {

    @Test
    public void testSearchByAll() {
        YtAuditFilter auditFilter = new YtAuditFilter();
        auditFilter
            .findByEntityIds(Arrays.asList("1", "2", "3"))
            .entityTypes(Arrays.asList(MboAudit.EntityType.CM_BLUE_OFFER))
            .timestampFrom(100L)
            .timestampTo(200L)
            .addOrderBy(SearchFields.TIMESTAMP, Direction.DESC);
        Assert.assertTrue(EntityIdIdIndexQuery.isSupportFilter(auditFilter));
        String query = new EntityIdIdIndexQuery(auditFilter).query(true);
        Assert.assertEquals("entity_id in ('1','2','3') AND entity_type in (16) " +
            "AND timestamp >= 100 AND timestamp < 200 ORDER BY timestamp DESC", query);
    }

    @Test
    public void testFailedCheckWithoutMainKey() {
        YtAuditFilter auditFilter = new YtAuditFilter();
        auditFilter.timestampFrom(100L);
        auditFilter.timestampTo(200L);
        Assert.assertFalse(EntityIdIdIndexQuery.isSupportFilter(auditFilter));
    }
}
