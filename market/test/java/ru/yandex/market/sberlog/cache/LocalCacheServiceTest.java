package ru.yandex.market.sberlog.cache;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.sberlog.SberlogConfig;
import ru.yandex.market.sberlog.config.LocalCacheConfig;
import ru.yandex.market.sberlog.dao.model.MarketidLinksModel;

import static org.junit.Assert.assertEquals;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 03.12.19
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SberlogConfig.class})
public class LocalCacheServiceTest {
    String puid1 = "testLinksModelPUID1";
    String puid2 = "testLinksModelPUID2";
    String puid3 = "testLinksModelPUID3";

    // puid1 models
    MarketidLinksModel marketidLinksModel1 = new MarketidLinksModel("testLinksModelMID1", puid1, 0);
    MarketidLinksModel marketidLinksModel2 = new MarketidLinksModel("testLinksModelMID2", puid1, 0);
    MarketidLinksModel marketidLinksModel3 = new MarketidLinksModel("testLinksModelMID3", puid1, 0);
    MarketidLinksModel marketidLinksModel4 = new MarketidLinksModel("testLinksModelMID4", puid1, 0);

    // puid2 models
    MarketidLinksModel marketidLinksModel5 = new MarketidLinksModel("testLinksModelMID5", puid2, 0);
    MarketidLinksModel marketidLinksModel6 = new MarketidLinksModel("testLinksModelMID6", puid2, 0);

    // puid3 models
    MarketidLinksModel marketidLinksModel7 = new MarketidLinksModel("testLinksModelMID7", puid3, 0);
    MarketidLinksModel marketidLinksModel8 = new MarketidLinksModel("testLinksModelMID8", puid3, 0);

    @Autowired
    private EmbeddedPostgres embeddedPostgres;

    @Test
    public void testLinksModelCache() throws Exception {
        int expireSec = 3;
        MarketidLinksModel result;

        LocalCacheConfig config = new LocalCacheConfig();
        config.setLinksModelSize(2);
        config.setLinksModelExpire(expireSec);
        config.setLinksModelIterationTime(3);

        try (LocalCacheService localCacheService = new LocalCacheService(embeddedPostgres.getPostgresDatabase(), config)) {
            // Wait cache to setup
            TimeUnit.MILLISECONDS.sleep(100);

            localCacheService.getLinksModel(puid1, () -> marketidLinksModel1);

            result = localCacheService.getLinksModel(puid1, () -> marketidLinksModel2);
            assertEquals("Test Cached", marketidLinksModel1.getMarketid(), result.getMarketid());

            TimeUnit.SECONDS.sleep(expireSec * 2);
            result = localCacheService.getLinksModel(puid1, () -> marketidLinksModel3);
            assertEquals("Test Expired", marketidLinksModel3.getMarketid(), result.getMarketid());

            localCacheService.invalidateLinksModel(puid1);
            result = localCacheService.getLinksModel(puid1, () -> marketidLinksModel4);
            assertEquals("Test Invalidate", marketidLinksModel4.getMarketid(), result.getMarketid());

            localCacheService.invalidateLinksModel(puid1);
            result = localCacheService.getLinksModel(puid1, () -> marketidLinksModel4);
            assertEquals("Test Invalidate All", marketidLinksModel4.getMarketid(), result.getMarketid());

            localCacheService.getLinksModel(puid2, () -> marketidLinksModel5);
            localCacheService.getLinksModel(puid3, () -> marketidLinksModel7);
            result = localCacheService.getLinksModel(puid2, () -> marketidLinksModel6);
            assertEquals("Test Size puid2 Present", marketidLinksModel5.getMarketid(), result.getMarketid());
            result = localCacheService.getLinksModel(puid3, () -> marketidLinksModel8);
            assertEquals("Test Size puid3 Present", marketidLinksModel7.getMarketid(), result.getMarketid());
            result = localCacheService.getLinksModel(puid1, () -> marketidLinksModel1);
            assertEquals("Test Size puid1 Evicted", marketidLinksModel1.getMarketid(), result.getMarketid());

        }
    }

    @Test
    public void testLinksModelCacheTrigger() throws Exception {
        MarketidLinksModel result;
        LocalCacheConfig config = new LocalCacheConfig();
        config.setLinksModelExpire(1000);
        config.setLinksModelIterationTime(3);

        try (LocalCacheService localCacheService = new LocalCacheService(embeddedPostgres.getPostgresDatabase(), config)) {
            TimeUnit.MILLISECONDS.sleep(100);

            localCacheService.getLinksModel(puid1, () -> marketidLinksModel1);

            Connection c = embeddedPostgres.getPostgresDatabase().getConnection();
            Statement s = c.createStatement();

            s.execute("INSERT INTO marketid_links (marketid, puid) VALUES ('" + marketidLinksModel1.getMarketid() + "', '" + puid1 + "')");
            TimeUnit.MILLISECONDS.sleep(100);
            result = localCacheService.getLinksModel(puid1, () -> marketidLinksModel2);
            assertEquals("Test INSERT trigger", marketidLinksModel2.getMarketid(), result.getMarketid());

            s.execute("UPDATE marketid_links SET linked = 0 WHERE puid =  '" + puid1 + "'");
            TimeUnit.MILLISECONDS.sleep(100);
            result = localCacheService.getLinksModel(puid1, () -> marketidLinksModel3);
            assertEquals("Test UPDATE trigger", marketidLinksModel3.getMarketid(), result.getMarketid());

            s.execute("DELETE FROM marketid_links WHERE puid = '" + puid1 + "'");
            TimeUnit.MILLISECONDS.sleep(100);
            result = localCacheService.getLinksModel(puid1, () -> marketidLinksModel4);
            assertEquals("Test DELETE trigger", marketidLinksModel4.getMarketid(), result.getMarketid());
        }
    }

}
