package ru.yandex.market.supercontroller.mbologs.dao;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.supercontroller.mbologs.Mocks;
import ru.yandex.market.supercontroller.mbologs.model.TableInfo;

import java.util.Collections;
import java.util.List;

/**
 * @author amaslak
 */
public class GenerationsTest {

    private final Generations generations = Mocks.mockGenerations();

    @Before
    public void setUp() throws Exception {
        List<String> sessionIds = generations.getAllSessionIds("whatever");
        Assert.assertEquals(Mocks.SESSIONS, sessionIds);
    }

    @Test
    public void testTableSessionId() throws Exception {
        TableInfo info = new TableInfo("//tmp/mbo_offers_20101213_1415");
        generations.fillTableInfo(info, false);
        Assert.assertEquals("20101213_1415", info.getTableSessionId());

        info = new TableInfo("//tmp/mbo-fake/20121011_1000/mbo_offers_mr");
        generations.fillTableInfo(info, false);
        Assert.assertEquals("20121011_1000", info.getTableSessionId());

        info = new TableInfo("//tmp/mbo_offers_20101213_1415");
        info.setTableSessionId("20361212_0000");
        generations.fillTableInfo(info, false);
        Assert.assertEquals("20361212_0000", info.getTableSessionId());
    }

    @Test
    public void testBaseTableName() throws Exception {
        TableInfo info = new TableInfo("//tmp/mbo_offers_20101213_1415");
        generations.fillTableInfo(info, false);
        Assert.assertEquals("mbo_offers", info.getBaseTableName());

        info = new TableInfo("//tmp/mbo-fake/20121011_1000/mbo_offers_mr");
        generations.fillTableInfo(info, false);
        Assert.assertEquals("mbo_offers_mr", info.getBaseTableName());

        info = new TableInfo("//tmp/mbo_offers_20101213_1415");
        info.setBaseTableName("custom_table_name");
        generations.fillTableInfo(info, false);
        Assert.assertEquals("custom_table_name", info.getBaseTableName());
    }

    @Test
    public void testBaseSession() throws Exception {
        TableInfo info = new TableInfo("//tmp/mbo_offers_20101213_1415");
        generations.fillTableInfo(info, false);
        Assert.assertNull(info.getBaseSessionId());

        info = new TableInfo("//tmp/mbo_offers_20101213_1415");
        generations.fillTableInfo(info, true);

        String baseSession = Collections.min(Mocks.SESSIONS);
        Assert.assertEquals(baseSession, info.getBaseSessionId());
    }

    @Test
    public void testSessions() throws Exception {
        TableInfo info = new TableInfo("//tmp/mbo_offers_20101213_1415");
        generations.fillTableInfo(info, false);
        Assert.assertNull(info.getSessions());

        info = new TableInfo("//tmp/mbo_offers_20101213_1415");
        generations.fillTableInfo(info, true);

        ImmutableSet<String> sessions = ImmutableSet.copyOf(Mocks.SESSIONS);
        Assert.assertEquals(sessions, info.getSessions());
    }

}
