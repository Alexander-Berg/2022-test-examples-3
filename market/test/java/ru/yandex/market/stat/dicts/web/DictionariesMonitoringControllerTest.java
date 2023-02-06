package ru.yandex.market.stat.dicts.web;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.support.RetryTemplate;
import ru.yandex.market.stat.dicts.common.LoadStatus;
import ru.yandex.market.stat.dicts.common.ShameDict;
import ru.yandex.market.stat.dicts.common.SlaDict;
import ru.yandex.market.stat.dicts.common.SlaDictionariesHolder;
import ru.yandex.market.stat.dicts.config.ShameConfiguration;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.loaders.jdbc.JdbcTaskDefinition;
import ru.yandex.market.stat.dicts.loaders.jdbc.TypelessJdbcLoader;
import ru.yandex.market.stat.dicts.services.BazingaTaskUrlService;
import ru.yandex.market.stat.dicts.services.DictionaryStorage;
import ru.yandex.market.stat.dicts.services.MetadataService;
import ru.yandex.market.stat.dicts.services.TmpDirectoryConfig;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.stat.dicts.web.DictionariesMonitoringController.MONITORING_START_HOUR;
import static ru.yandex.market.stat.dicts.web.DictionariesMonitoringController.MONITORING_WEEKEND_START_HOUR;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class DictionariesMonitoringControllerTest {

    private static final int MONDAY = 1;
    private static final int SATURDAY = 6;
    private static final int SUNDAY = 7;
    private static final Set<String> DICTS = new HashSet<>(Arrays.asList("dict_ok", "dict_warn", "dict_crit"));

    private DictionariesMonitoringController controller;
    @Mock
    private MetadataService metadataDao;
    @Mock
    private TmpDirectoryConfig tmpDirectoryConfig;
    @Mock
    @Qualifier("dictsForMonitoring")
    private SlaDictionariesHolder slaDicts;

    @Mock
    private ShameConfiguration shameConfiguration;

    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private RetryTemplate retryTemplate;

    private final String BAZINGA_URL = "https://data-capture.vs.market.yandex.net/z/bazinga/tasks";

    private final BazingaTaskUrlService bazingaTaskUrlService = new BazingaTaskUrlService(
            httpClient,
            retryTemplate,
            BAZINGA_URL,
            "",
            false
    );

    @Test
    public void testOK() {
        initFor(MONITORING_START_HOUR, Collections.emptyList());
        String result = controller.topSlaForHour(7, MONDAY);
        log.info(result);
        assertThat(result, is(DictionariesMonitoringController.OK_STATUS));
    }

    @Test
    public void testWARN() {
        initFor(MONITORING_START_HOUR, someDictNotLoaded());
        String result = controller.topSlaForHour(7, MONDAY);
        log.info(result);
        assertThat(result, containsString(DictionariesMonitoringController.WARN));
    }

    @Test
    public void testCRIT() {
        initFor(MONITORING_START_HOUR, someDictNotLoaded());
        String result = controller.topSlaForHour(8, MONDAY);
        log.info(result);
        assertThat(result, containsString(DictionariesMonitoringController.CRIT));
    }

    @Test
    public void testNoWarnBefore12Weekend() {
        initFor(MONITORING_WEEKEND_START_HOUR, someDictNotLoaded());
        String result = controller.topSlaForHour(MONITORING_WEEKEND_START_HOUR - 3, SATURDAY);
        log.info(result);
        assertThat(result, containsString(DictionariesMonitoringController.OK_STATUS));
    }

    @Test
    public void testNoCritBefore12Weekend() {
        initFor(MONITORING_WEEKEND_START_HOUR, someDictNotLoaded());
        String result = controller.topSlaForHour(MONITORING_WEEKEND_START_HOUR - 1, SUNDAY);
        log.info(result);
        assertThat(result, containsString(DictionariesMonitoringController.OK_STATUS));
    }

    @Test
    public void testCritWeekend() {
        initFor(MONITORING_WEEKEND_START_HOUR, someDictNotLoaded());
        String result = controller.topSlaForHour(MONITORING_WEEKEND_START_HOUR, SUNDAY);
        log.info(result);
        assertThat(result, containsString(DictionariesMonitoringController.CRIT));
    }

    @Test
    public void testCheckDBOK() {
        controller = new DictionariesMonitoringController(metadataDao, tmpDirectoryConfig, slaDicts, new ArrayList<>(), shameConfiguration, bazingaTaskUrlService);
        String result = controller.checkDB();
        log.info(result);
        assertThat(result, containsString(DictionariesMonitoringController.OK_STATUS));
    }

    @Test
    public void testCheckDBThrows() {
        doThrow(new QueryTimeoutException("Test")).when(metadataDao).checkDB();
        controller = new DictionariesMonitoringController(metadataDao, tmpDirectoryConfig, slaDicts, new ArrayList<>(), shameConfiguration, bazingaTaskUrlService);
        String result = controller.checkDB();
        log.info(result);
        assertThat(result, containsString(DictionariesMonitoringController.CRIT));
    }

    @Test
    public void testGrowing() throws IOException {
        when(metadataDao.getGrowingDictionaries(any(Long.class), any(Double.class), any(Double.class), any(Set.class)))
                .thenReturn(ImmutableMap.of(
                        "dict_pasha", ImmutablePair.of(3.12345678, 12.12345678),
                        "dict_sasha", ImmutablePair.of(2.12345678, 4.12345678)));

        controller = new DictionariesMonitoringController(metadataDao, tmpDirectoryConfig, slaDicts, new ArrayList<>(), shameConfiguration, bazingaTaskUrlService);
        String result = controller.getGrowingDicts();
        log.info(result);
        assertThat("Wrong status, CRIT expected", result, containsString(DictionariesMonitoringController.CRIT));
        assertThat("Wrong problem count, expected 2!", result, containsString("2 dictionaries are growing too fast"));
        assertThat("Wrong data for dict_pasha", result, containsString("dict_pasha " + BAZINGA_URL + "/dict_pasha : 3.12 times per week / 12.12 times per month"));
        assertThat("Wrong data for dict_sasha", result, containsString("dict_sasha " + BAZINGA_URL + "/dict_sasha : 2.12 times per week / 4.12 times per month"));
    }

    @Test
    public void testLargeDicts() throws IOException {
        when(metadataDao.getLargeDictionaries(any(Long.class), any(Set.class)))
                .thenReturn(ImmutableMap.of(
                        "dict_pasha", 1283456789L,
                        "dict_sasha", 8883456789L));

        controller = new DictionariesMonitoringController(metadataDao, tmpDirectoryConfig, slaDicts, new ArrayList<>(), shameConfiguration, bazingaTaskUrlService);
        String result = controller.getLargeDicts();
        log.info(result);
        assertThat("Wrong status, CRIT expected", result, containsString(DictionariesMonitoringController.CRIT));
        assertThat("Wrong problem count, expected 2!", result, containsString("2 oversized dict(s)"));
        assertThat("Wrong data for dict_pasha", result,
                containsString("dict_pasha " + BAZINGA_URL + "/dict_pasha : 1224.0 MiB"));
        assertThat("Wrong data for dict_sasha", result,
                containsString("dict_sasha " + BAZINGA_URL + "/dict_sasha : 8471.9 MiB"));
    }


    @Test
    public void testMassFail() throws IOException {
        when(metadataDao.getFailPercentage())
                .thenReturn(0.31235);

        controller = new DictionariesMonitoringController(metadataDao, tmpDirectoryConfig, slaDicts, new ArrayList<>(), shameConfiguration, bazingaTaskUrlService);
        String result = controller.getMassFailResult();
        log.info(result);
        assertThat("Wrong status, CRIT expected", result, containsString(DictionariesMonitoringController.CRIT));
        assertThat("Wrong problem count, expected 2!", result, containsString("31% of all tasks were failed"));
    }


    @Test
    public void testNoLoads() throws IOException {
        when(metadataDao.getFreshLoadsCount()).thenReturn(0);

        controller = new DictionariesMonitoringController(metadataDao, tmpDirectoryConfig, slaDicts, new ArrayList<>(), shameConfiguration, bazingaTaskUrlService);
        String result = controller.getMassNoLoadsResult();
        log.info(result);
        assertThat("Wrong status, CRIT expected", result, containsString(DictionariesMonitoringController.CRIT));
        assertThat("Wrong problem count, expected 2!", result, containsString("No fresh loads in an hour"));
    }


    @Test
    public void testExceptions() {
        initFor(MONITORING_WEEKEND_START_HOUR, someDictNotLoaded());
        Set<String> sizeExceptions = controller.getSizeExceptions();
        assertThat("wrong large whitelist size!", sizeExceptions.size(), is(2));

        assertThat("wrong large dicts whitelist!", sizeExceptions, containsInAnyOrder("fatAndGrowing", "fat"));
        Set<String> growExceptions = controller.getGrowingExceptions();

        assertThat("wrong growing whitelist size!", growExceptions.size(), is(2));
        assertThat("wrong growing dicts whitelist!",
                growExceptions, containsInAnyOrder("growing", "fatAndGrowing"));

    }

    @Test
    public void testReloadDurationMon() {

        List<DictionaryLoadersHolder> knownLoaders = Arrays.asList(
                new DictionaryLoadersHolder(Arrays.asList(
                        getTypelessJdbcLoader(LoaderScale.DAYLY, "vasya", 1),
                        getTypelessJdbcLoader(LoaderScale.DAYLY, "kostya", 24),
                        getTypelessJdbcLoader(LoaderScale.DEFAULT, "kesha", 3),
                        getTypelessJdbcLoader(LoaderScale.DAYLY, "petya", null),
                        getTypelessJdbcLoader(LoaderScale.DAYLY, "sveta", 3),
                        getTypelessJdbcLoader(LoaderScale.DAYLY, "ira", -1),
                        getTypelessJdbcLoader(LoaderScale.DAYLY, "gabriil", 4)
                )),
                new DictionaryLoadersHolder(Arrays.asList(
                        getTypelessJdbcLoader(LoaderScale.HOURLY, "pasha", null),
                        getTypelessJdbcLoader(LoaderScale.HOURLY, "sveta", 3)
                ))
        );
        controller = new DictionariesMonitoringController(metadataDao, tmpDirectoryConfig, slaDicts, knownLoaders, shameConfiguration, bazingaTaskUrlService);

        when(metadataDao.getDictsLoadingMoreThan(any(Double.class))).thenReturn(
                ImmutableMap.of("vasya", 99L, // раз в час, это проблема
                        "kostya", 120L, // раз в сутки, это не проблема
                        "kesha", 100L, // раз в 180 минут, это проблема
                        "sveta", 60L, // раз в 3 часа, это не проблема
                        "petya", 120L // раз в сутки, это ок
                )
        );

        when(metadataDao.get1HourDictsLoadingMoreThan(any(Double.class))).thenReturn(
                ImmutableMap.of("sveta", 40L)); //40 минут раз в час - не ок

        String result = controller.getFatReloads();
        System.out.println(result);
        assertThat("Wrong status, CRIT expected", result, containsString(DictionariesMonitoringController.CRIT));
        assertThat("Wrong problems count", result, containsString(" 3problem(s)"));
        assertThat("Wrong status for kesha", result, containsString("kesha " + BAZINGA_URL + "/kesha : 100min/every 180min"));
        assertThat("Wrong status for vasya", result, containsString("vasya-1d " + BAZINGA_URL + "/vasya_1d : 99min/every 60min"));
        assertThat("Wrong status for sveta-1h", result, containsString("sveta-1h " + BAZINGA_URL + "/sveta_1h : 40min/every 60min"));
    }


    private TypelessJdbcLoader getTypelessJdbcLoader(LoaderScale scale, String name, Integer loadPeriodHours) {
        JdbcTaskDefinition taskDaily = new JdbcTaskDefinition("schema",
                null, LocalDate.now(), false, null, false, loadPeriodHours,
                120L, null, null, null, null, null, scale,
                null, null, 1L, "", name, name,
                true, "", null, "", false, false, false);
        return new TypelessJdbcLoader(mock(JdbcTemplate.class), mock(DictionaryStorage.class), taskDaily
        );
    }


    private List<LoadStatus> someDictNotLoaded() {
        return Arrays.asList(
                new LoadStatus("dict_warn", LocalDate.now().minusDays(1)),
                new LoadStatus("dict_crit", LocalDate.now().minusDays(2))
        );
    }


    private void initFor(int monitoringStartHour, List<LoadStatus> objects) {
        when(metadataDao.checkTopDicts(DICTS, monitoringStartHour)).thenReturn(objects);
        when(slaDicts.getDicts()).thenReturn(defaultMonitoringConf());
        when(shameConfiguration.getBodypositiveDicts()).thenReturn(Collections.singletonList(new ShameDict("fat", "reason1")));
        when(shameConfiguration.getSuperGrowingDicts()).thenReturn(Collections.singletonList(new ShameDict("growing", "reason2")));
        when(shameConfiguration.getBodypositiveAndGrowing()).thenReturn(Collections.singletonList(new ShameDict("fatAndGrowing", "reason3")));
        controller = new DictionariesMonitoringController(metadataDao, tmpDirectoryConfig, slaDicts, new ArrayList<>(), shameConfiguration, bazingaTaskUrlService);
    }

    private List<SlaDict> defaultMonitoringConf() {
        return Arrays.asList(
                new SlaDict("dict_ok", 10, false),
                new SlaDict("dict_warn", 9, false),
                new SlaDict("dict_crit", 8, false)
        );
    }

}
