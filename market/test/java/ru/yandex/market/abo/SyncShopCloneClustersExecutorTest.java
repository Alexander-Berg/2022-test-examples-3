package ru.yandex.market.abo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.abo._public.impl.HttpAboPublicService;
import ru.yandex.market.core.util.http.UnitTestMarketHttpClient;
import ru.yandex.market.mbi.http.MarketHttpClient;


/**
 * @author sergey-fed
 * <p>
 * Тест проверяет, что джоба {@code SyncShopCloneClustersExecutor} загружает данные из ресурса abo-public
 * {@code /api/shop/clones} и успешно сохраняет их в таблицу {@code SHOPS_WEB.CLONE_CLUSTER_SHOP}
 * затирая все старые данные в ней
 */
class SyncShopCloneClustersExecutorTest extends FunctionalTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${market.abo.public.url}")
    private String aboPublicUrl;

    private SyncShopCloneClustersExecutor executor;

    @Test
    @DbUnitDataSet(
            type = DataSetType.SINGLE_CSV,
            before = "SyncShopCloneClustersExecutorTest.before.csv",
            after = "SyncShopCloneClustersExecutorTest.after.csv"
    )
    void testSyncShopCloneClustersExecutor() {
        executor = new SyncShopCloneClustersExecutor(
                new HttpAboPublicService(httpClient(), aboPublicUrl), transactionTemplate, jdbcTemplate);

        executor.doJob(null);
    }

    private MarketHttpClient httpClient() {
        UnitTestMarketHttpClient httpClient = new UnitTestMarketHttpClient();
        httpClient.setResponseText(
                "[{\"clusterId\": 100,\"shopId\": 200}]"
        );
        return httpClient;
    }

}
