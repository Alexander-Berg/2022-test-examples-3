package ru.yandex.market.health;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickphite.whitelist.SplitWhitelistCache;
import ru.yandex.market.clickphite.whitelist.SplitWhitelistService;
import ru.yandex.market.health.configs.clickphite.SplitWhitelistDao;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistAutoUpdateEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistEntity;
import ru.yandex.market.health.configs.common.TableEntity;

import static org.assertj.core.api.Assertions.assertThat;

//бОльшая часть ситуаций проверяется в ClickphiteSplitWhitelistTests. Здесь только отдельные частные случаи
public class SplitWhitelistServiceTest extends IntegrationTestsBase {

    private static final SplitWhitelistEntity.Id WHITELIST_ID = new SplitWhitelistEntity.Id(
        new TableEntity("some_db", "some_table"),
        "some_expression",
        new SplitWhitelistAutoUpdateEntity(1, 10)
    );

    private ClickhouseTemplate clickhouseTemplate;

    @Test
    public void updatesCacheInSlaveMode() throws Exception {
        AnnotationConfigApplicationContext logshatterContext = new AnnotationConfigApplicationContext();
        initLogshatterContextAndTablesMinConfig(logshatterContext);
        clickhouseTemplate = logshatterContext.getBean(ClickhouseTemplate.class);
        AnnotationConfigApplicationContext clickphiteContext = createClickphiteContext();
        final SplitWhitelistDao splitWhitelistDao = clickphiteContext.getBean(SplitWhitelistDao.class);
        final SplitWhitelistCache splitWhitelistCache = clickphiteContext.getBean(SplitWhitelistCache.class);
        final SplitWhitelistService splitWhitelistService = clickphiteContext.getBean(SplitWhitelistService.class);
        final Instant now = Instant.now();
        Function<List<String>, SplitWhitelistEntity> splitWhitelistEntityFactory = values -> new SplitWhitelistEntity(
            WHITELIST_ID,
            now,
            values.stream().map(value -> new SplitWhitelistEntity.Element(value, now, 100)).collect(Collectors.toList())
        );
        final SplitWhitelistEntity originalEntity = splitWhitelistEntityFactory.apply(Collections.singletonList("A"));
        splitWhitelistDao.put(originalEntity);
        assertThat(splitWhitelistCache.get(WHITELIST_ID)).isEqualToComparingFieldByFieldRecursively(originalEntity);
        final SplitWhitelistEntity modifiedEntity = splitWhitelistEntityFactory.apply(Arrays.asList("A", "B"));
        splitWhitelistDao.put(modifiedEntity);

        splitWhitelistService.call();

        assertThat(splitWhitelistCache.get(WHITELIST_ID)).isEqualToComparingFieldByFieldRecursively(modifiedEntity);
    }

    @After
    public void teardown() {
        dropDatabases(clickhouseTemplate);
    }

}
