package ru.yandex.market.pers.basket;

import java.util.Random;

import com.google.common.cache.Cache;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.test.common.AbstractPersWebTest;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
    PersBasketTestConfiguration.class,
})
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({"junit"})
@TestPropertySource("classpath:/test-application.properties")
public abstract class PersBasketTest extends AbstractPersWebTest {

    protected static Random RND = new Random();
    protected ResultMatcher ok = status().isOk();
    protected ResultMatcher badRequest = status().isBadRequest();
    protected ResultMatcher is4xx = status().is4xxClientError();
    protected ResultMatcher notFound = status().isNotFound();

    @Autowired
    @Qualifier("mockedCacheMap")
    protected Cache<String, Object> mockedCacheMap;

    @Autowired
    protected JdbcTemplate pgaasJdbcTemplate;

    @Before
    public void commonInit() {
        initTestPgaasDatabase();
    }

    @After
    public void commonTearDown() {
        truncatePgaasDatabase();
    }

    public void initTestPgaasDatabase() {
    }

    public void truncatePgaasDatabase() {
        pgaasJdbcTemplate.execute("truncate wishlist_relation cascade");
        pgaasJdbcTemplate.execute("truncate wishlist cascade");
        pgaasJdbcTemplate.execute("truncate basket_items cascade");
        pgaasJdbcTemplate.execute("truncate basket_items_archive cascade");
        pgaasJdbcTemplate.execute("truncate account_relation cascade");
        pgaasJdbcTemplate.execute("truncate category_entry cascade");
        pgaasJdbcTemplate.execute("truncate users cascade");
        pgaasJdbcTemplate.execute("truncate alice_entry cascade");
        pgaasJdbcTemplate.execute("truncate basket_collections_queue cascade");
        pgaasJdbcTemplate.execute("truncate basket_collections_queue cascade");
        pgaasJdbcTemplate.execute("TRUNCATE TABLE model_transition");
        pgaasJdbcTemplate.execute("TRUNCATE TABLE model_transition_history");
    }

    @Before
    public void resetCache() {
        mockedCacheMap.invalidateAll();
    }

    @Before
    public final void resetAllMocks() {
        PersTestMocksHolder.resetMocks();
    }
}
