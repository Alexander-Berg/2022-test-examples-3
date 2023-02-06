package ru.yandex.market.pers.notify.executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.cluster.ClusterDiffManager;
import ru.yandex.market.pers.notify.comparison.ComparisonItemDAO;
import ru.yandex.market.pers.notify.comparison.model.ComparisonItem;
import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityDAO;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         21.12.16
 */
public class ClusterActualizeExecutorTest extends MarketMailerMockedDbTest {
    @Autowired
    protected SubscriptionAndIdentityDAO subscriptionAndIdentityDAO;
    @Autowired
    private ComparisonItemDAO comparisonItemDAO;
    @Autowired
    private ClusterActualizeExecutor clusterActualizeExecutor;
    @Autowired
    private ClusterDiffManager clusterDiffManager;

    @BeforeEach
    public void init() throws Exception {
        clusterDiffManager.setLoadDays(1_000_000);
    }

    @Test
    public void doJob() throws Exception {
        Identity identity = new Uuid("FFFFUUUU324lj12");
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        String clusterIdDeprecated = "1372408752";
        String clusterIdNew = "1373942455";
        assertTrue(comparisonItemDAO.saveItem(identity, new ComparisonItem(id, 3241237L, clusterIdDeprecated)));
        String clusterIdActual = "1372408753";
        assertTrue(comparisonItemDAO.saveItem(identity, new ComparisonItem(id, 3241237L, clusterIdActual)));

        List<ComparisonItem> items = comparisonItemDAO.getItems(id);
        assertEquals(2, items.size());
        assertEquals(clusterIdActual, items.get(0).getProductId());
        assertEquals(clusterIdDeprecated, items.get(1).getProductId());

        clusterActualizeExecutor.doRealJob(null);
        items = comparisonItemDAO.getItems(id);
        assertEquals(2, items.size());
        assertEquals(clusterIdActual, items.get(0).getProductId());
        assertEquals(clusterIdNew, items.get(1).getProductId());
    }
}
