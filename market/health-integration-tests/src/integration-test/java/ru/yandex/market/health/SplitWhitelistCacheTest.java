package ru.yandex.market.health;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickphite.ClickphiteMasterTimeTicker;
import ru.yandex.market.clickphite.whitelist.SplitWhitelistCache;
import ru.yandex.market.health.configs.clickphite.SplitWhitelistDao;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistAutoUpdateEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistEntity;
import ru.yandex.market.health.configs.common.TableEntity;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

public class SplitWhitelistCacheTest extends IntegrationTestsBase {

    private static final Instant SOME_TIME = Instant.now();

    private static final SplitWhitelistEntity.Id SPLIT_WHITELIST_ENTITY_ID = new SplitWhitelistEntity.Id(
        new TableEntity(
            "some_db",
            "some_table"
        ),
        "some_expression",
        new SplitWhitelistAutoUpdateEntity(
            1,
            10
        )
    );
    private static final SplitWhitelistEntity SPLIT_WHITELIST_ENTITY = new SplitWhitelistEntity(
        SPLIT_WHITELIST_ENTITY_ID,
        SOME_TIME,
        Arrays.asList(
            new SplitWhitelistEntity.Element(
                "A",
                SOME_TIME,
                100
            ),
            new SplitWhitelistEntity.Element(
                "B",
                SOME_TIME,
                200
            ),
            new SplitWhitelistEntity.Element(
                "C",
                SOME_TIME,
                300
            )
        )
    );

    @Value("${clickphite.whitelist.cache.expire-duration}")
    private long expireDuration;
    @Value("${clickphite.whitelist.cache.expire-time-unit}")
    private TimeUnit expireTimeUnit;

    private ClickhouseTemplate clickhouseTemplate;
    private SplitWhitelistCache splitWhitelistCache;
    private SplitWhitelistDao splitWhitelistDao;
    private ClickphiteMasterTimeTicker clickphiteMasterTimeTicker;

    @Before
    public void setup() throws Exception {
        AnnotationConfigApplicationContext logshatterContext = new AnnotationConfigApplicationContext();
        initLogshatterContextAndTablesMinConfig(logshatterContext);
        clickhouseTemplate = logshatterContext.getBean(ClickhouseTemplate.class);
        // засыпаем, чтобы успела создаться необходимая база с таблицами в ClickHouse
        Thread.sleep(3000);
        AnnotationConfigApplicationContext clickphiteContext = createClickphiteContext();
        splitWhitelistCache = clickphiteContext.getBean(SplitWhitelistCache.class);
        splitWhitelistDao = clickphiteContext.getBean(SplitWhitelistDao.class);
        clickphiteMasterTimeTicker = clickphiteContext.getBean(ClickphiteMasterTimeTicker.class);
    }

    @After
    public void teardown() throws Exception {
        dropDatabases(clickhouseTemplate);
        // засыпаем, чтобы ClickHouse успел удалить базу
        Thread.sleep(3000);
    }

    @Test
    public void noValueInDao() {
        assertNull(getFromCache());
    }

    @Test
    public void dontExpireWhenSlave() throws InterruptedException {
        getFromCache();
        Thread.sleep(2 * expireTimeUnit.toMillis(expireDuration));
        fillData();
        assertNull(getFromCache());
    }

    @Test
    public void expireWhenMaster() throws InterruptedException {
        clickphiteMasterTimeTicker.setLeader(true);
        getFromCache();
        Thread.sleep(2 * expireTimeUnit.toMillis(expireDuration));
        fillData();
        final SplitWhitelistEntity actualValue = getFromCache();
        assertThat(actualValue).isEqualToComparingFieldByFieldRecursively(SPLIT_WHITELIST_ENTITY);
    }

    private void fillData() {
        splitWhitelistDao.put(SPLIT_WHITELIST_ENTITY);
    }

    private SplitWhitelistEntity getFromCache() {
        return splitWhitelistCache.get(SPLIT_WHITELIST_ENTITY_ID);
    }

}
