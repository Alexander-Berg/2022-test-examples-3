package ru.yandex.market.stat.dicts.services;

import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.config.DictionariesITestConfig;
import ru.yandex.market.stat.dicts.integration.help.SpringDataProviderRunner;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.records.CatalogerDictionaryRecord;
import ru.yandex.market.stat.dicts.records.RegionDictionaryRecord;
import ru.yandex.market.stat.dicts.records.ShopDatasourceDictionaryRecord;
import ru.yandex.market.stats.test.config.LocalPostgresInitializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@ActiveProfiles("integration-tests")
@RunWith(SpringDataProviderRunner.class)
@Slf4j
@ContextConfiguration(classes = DictionariesITestConfig.class, initializers = LocalPostgresInitializer.class)
public class MetadataServiceITest {
    private static final String DEFAULT_CLUSTER = "hahn";

    private static final String TOKEN_1 = "token1";
    private static final String TOKEN_2 = "token2";
    private static final String TOKEN_3 = "token3";
    private static final String USERNAME = "kl";
    private static final String USERNAME_2 = "klkl";
    private static final String HOSTNAME = "hostname-kl-sas-eeee.yandex.ru";

    @Autowired
    private MetadataService metadataService;

    @Test
    public void testSaveToken() {

        metadataService.saveToken(USERNAME, TOKEN_1, HOSTNAME);
        String token = metadataService.getToken(USERNAME, 10).orElse(null);
        assertThat("Wrong  token", token, Matchers.is(TOKEN_1));

        metadataService.saveToken(USERNAME, TOKEN_2, HOSTNAME);
        token = metadataService.getToken(USERNAME, 10).orElse(null);
        assertThat("Wrong  token", token, Matchers.is(TOKEN_2));

        //expired
        Optional<String> expiredToken = metadataService.getToken(USERNAME, 0);
        assertFalse("Wrong  token", expiredToken.isPresent());

        metadataService.saveToken(USERNAME, TOKEN_3, HOSTNAME);
        metadataService.saveToken(USERNAME, TOKEN_1, HOSTNAME);
        token = metadataService.getToken(USERNAME, 10).orElse(null);
        assertThat("Wrong  token", token, Matchers.is(TOKEN_1));

    }

    @Test
    public void testUsedToken() {
        //сохранили токен для использования
        metadataService.saveToken(USERNAME, TOKEN_1, HOSTNAME);
        //запросили токен  для использования ещё раз
        String token = metadataService.getToken(USERNAME, 10).orElse(null);
        assertThat("Wrong  token", token, Matchers.is(TOKEN_1));
        assertTrue("Token should be used!", metadataService.hasUsedToken(USERNAME, 10));
        //убрали одно  использование из 2х
        metadataService.freeToken(USERNAME);
        assertTrue("Token still should be used!", metadataService.hasUsedToken(USERNAME, 10));
        //убрали второе использование из 2х
        metadataService.freeToken(USERNAME);
        assertFalse("Token should NOT be used!", metadataService.hasUsedToken(USERNAME, 10));
        //еще раз взяли токен
        metadataService.getToken(USERNAME, 10);
        assertTrue("Token should be used!", metadataService.hasUsedToken(USERNAME, 10));

        //симуляция протухшего токена
        assertFalse("Token should NOT be used!", metadataService.hasUsedToken(USERNAME, 0));
    }

    @Test
    public void testUsedTokenThenChanged() {
        //сохранили токен для использования
        metadataService.saveToken(USERNAME, TOKEN_1, HOSTNAME);
        //запросили токен  для использования ещё 3 раза => used=4
        metadataService.getToken(USERNAME, 10);
        metadataService.getToken(USERNAME, 10);
        metadataService.getToken(USERNAME, 10);

        assertTrue("Token should be used!", metadataService.hasUsedToken(USERNAME, 10));

        //сохранили новый токен (used=1)
        metadataService.saveToken(USERNAME, TOKEN_2, HOSTNAME);
        //убрали одно  использование
        metadataService.freeToken(USERNAME);
        //новый токен юзался лишь раз, теперь свободен
        assertFalse("Token should NOT be used!", metadataService.hasUsedToken(USERNAME, 10));
    }

    @Test
    public void testNoToken() {
        Optional<String> token = metadataService.getToken(USERNAME_2, 10);
        assertFalse("Token should NOT be found!", token.isPresent());
    }

    @Test
    public void testForDurationQuery() {
        metadataService.save(DEFAULT_CLUSTER, Dictionary.from("hello", CatalogerDictionaryRecord.class),
                LocalDate.now().atStartOfDay(), 1L, 1L, 110*60000,
                LocalDateTime.now().minusHours(2), 1L, 1024*1024);
        Map<String, Long> res = metadataService.getDictsLoadingMoreThan(10.1);
        assertNotNull("Something went wrong", res);
        assertTrue("No expected dict", res.containsKey("hello"));
        assertThat("Wrong duration", res.get("hello"), Matchers.is(110L));
    }


    @Test
    public void testForMassFail() {
        metadataService.getMetadataTemplate().update("insert into cron_job (task, cpu_usage, status, create_time) " +
                "values ('categories', 3, 'completed', now()), ('cpa_categories', 5, 'completed', now()), " +
                "('shops', 0, 'completed', now()), ('shops_xxx', 1, 'completed', now())," + //игнорятся
                "('domains', 4, 'failed', now()), ('ddd', 0, 'failed', now()) ", Collections.emptyMap());

        Double res = metadataService.getFailPercentage();
        assertNotNull("Something went wrong", res);
        assertThat("Wrong fail ratio!", res, Matchers.is(0.5));
    }


    @Test
    public void testForNoLoads() {
        Dictionary dictionary = Dictionary.from("hello", CatalogerDictionaryRecord.class);
        long id = metadataService.save(DEFAULT_CLUSTER, dictionary,
                LocalDate.now().atStartOfDay(), 1L, 1L, 110*60000,
                LocalDateTime.now().minusMinutes(1), 1L, 1024*1024);

        Integer res = metadataService.getFreshLoadsCount();
        assertThat("Wrong fresh tasks count!!", res, Matchers.is(0));

        metadataService.publish(DEFAULT_CLUSTER, id, "hello");
        res = metadataService.getFreshLoadsCount();

        assertThat("Wrong fresh tasks count!", res, Matchers.is(1));
    }

    @Test
    public void testForHeavyDictsQuery() {
        metadataService.save(DEFAULT_CLUSTER, Dictionary.from("dict1", CatalogerDictionaryRecord.class),
                LocalDate.now().atStartOfDay(), 1L, 1L, 130*60000,
                LocalDateTime.now().minusHours(2), 1L, 1024*1024);
        metadataService.save(DEFAULT_CLUSTER, Dictionary.from("dict1", CatalogerDictionaryRecord.class),
                LocalDate.now().atStartOfDay(), 1L, 1L, 120*60000,
                LocalDateTime.now().minusHours(2), 1L, 1024*1024);
        metadataService.save(DEFAULT_CLUSTER, Dictionary.from("dict2", RegionDictionaryRecord.class),
                LocalDate.now().atStartOfDay(), 1L, 1L, 110*60000,
                LocalDateTime.now().minusHours(2), 1L, 1024*1024);
        metadataService.save(DEFAULT_CLUSTER, Dictionary.from("dict3", ShopDatasourceDictionaryRecord.class,
                LoaderScale.DAYLY, 10L),
                LocalDate.now().atStartOfDay(), 1L, 1L, 130*60000,
                LocalDateTime.now().minusHours(2), 1L, 1024*1024);

        List<String> heavyDicts = metadataService.getPreviousLoadsLogerThan(120L);
        assertThat("Something went wrong", heavyDicts.size(), Matchers.greaterThan(0));
        assertThat("No expected dict", heavyDicts, Matchers.containsInAnyOrder("dict1", "dict3-1d"));
        assertThat("Extra dict", heavyDicts, Matchers.not(Matchers.contains("dict2")));
    }
}
