package ru.yandex.market.stat.dicts.loaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.loaders.tvm.TvmTicketSupplier;
import ru.yandex.market.stat.dicts.records.DistributionRecord;
import ru.yandex.market.stat.dicts.services.YtDictionaryStorage;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Bogdan Timofeev <timofeevb@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class DistributionLoaderTest {
    private static final String DEFAULT_CLUSTER = "hahn";

    @Mock
    private YtDictionaryStorage dictionaryStorage;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private TvmTicketSupplier tvmTicketSupplier;

    private DistributionLoader distributionLoader;
    private BufferedReader reportJsonReader;
    private BufferedReader dictJsonReader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        distributionLoader = new DistributionLoader(httpClient, dictionaryStorage, tvmTicketSupplier);
        reportJsonReader = new BufferedReader(new InputStreamReader(DistributionLoaderTest.class.getResourceAsStream("/parsers/distribution-response.json")));
        dictJsonReader = new BufferedReader(new InputStreamReader(DistributionLoaderTest.class.getResourceAsStream("/parsers/distribution-dict.json")));
        when(tvmTicketSupplier.get(anyInt())).thenReturn("testTvmTicket");
    }

    @Test
    public void testParseReport() {
        LocalDate date = LocalDate.now();
        List<DistributionRecord> records =
            distributionLoader.parseReportData(reportJsonReader, new HashMap<>(), date);
        assertThat(records.size(), equalTo(5));
        assertThat(records.get(0).getTurnover_market_cpc(), equalTo(new BigDecimal("4.3220339")));
        assertThat(records.get(1).getPartner_market_cpc(), equalTo(new BigDecimal("628.61965678")));
        assertThat(records.get(1).getSoft_name(), equalTo("Настройки Браузеров"));
        assertThat(records.get(2).getPartner_market_cpa(), equalTo(new BigDecimal("32.74688136")));
        assertThat(records.get(2).getCurrency_id(), equalTo(2));
        assertThat(records.get(3).getClicks_market(), equalTo(new BigDecimal(6832)));
        assertThat(records.get(3).getOrders_market_cpa(), equalTo(new BigDecimal(2)));
        assertThat(records.get(4).getTurnover_market_cpa(), equalTo(new BigDecimal("34235.5465")));
        assertThat(records.get(4).getDay(), equalTo(date));
    }

    @Test
    public void testParseDictData() {
        Map<String, Integer> map = distributionLoader.parseDictData(dictJsonReader);
        assertThat(map.get("Opera"), equalTo(3));
        assertThat(map.get("Адаптер"), equalTo(1031));
    }

    @Test
    public void testParseReportWithDict() {
        Map<String, Integer> map = distributionLoader.parseDictData(dictJsonReader);
        List<DistributionRecord> records = distributionLoader.parseReportData(reportJsonReader, map, LocalDate.now());
        assertThat(records.get(0).getSoft_name(), equalTo("Мобильный Яндекс.Браузер"));
        assertThat(records.get(0).getSoft_id(), equalTo(1010));
        assertThat(records.get(4).getSoft_name(), equalTo("Яндекс.Элементы"));
        assertThat(records.get(4).getSoft_id(), equalTo(26));
    }

    @Test
    public void testGetPeriods() {
        LocalDate date = LocalDate.parse("2017-04-11");
        Map<String, LocalDate> periods = distributionLoader.getDayPeriodsBefore(date);
        assertThat(periods.get("[\"2017-04-11\", \"2017-04-11\"]"), equalTo(date));
        assertThat(periods.get("[\"2017-04-06\", \"2017-04-06\"]"), equalTo(LocalDate.parse("2017-04-06")));
        assertThat(periods.get("[\"2017-04-01\", \"2017-04-01\"]"), equalTo(LocalDate.parse("2017-04-01")));
    }


    @Test
    public void testScale() {
        assertThat(distributionLoader.getDictionary(), equalTo(Dictionary.fromClass(DistributionRecord.class, LoaderScale.DEFAULT_MONTH)));
        assertThat(distributionLoader.getScale(), equalTo(LoaderScale.DEFAULT_MONTH));
        assertThat(distributionLoader.getDictionary().tablePartition(LocalDate.parse("2018-12-05").atStartOfDay()), equalTo("2018-12-01"));

    }

    @Test
    public void testLoad() throws IOException {
        Map<String, Integer> map = distributionLoader.parseDictData(dictJsonReader);
        LocalDateTime dateTime = LocalDate.parse("2018-12-05").atStartOfDay();
        List<DistributionRecord> records = distributionLoader.parseReportData(reportJsonReader, map, dateTime.toLocalDate());
        DictionaryLoadIterator<DistributionRecord> iterator = DictionaryLoadIterator.from(records);
        Dictionary expectedDict = Dictionary.fromClass(DistributionRecord.class,  LoaderScale.DEFAULT_MONTH);
        when(dictionaryStorage.save(DEFAULT_CLUSTER, expectedDict, dateTime, iterator)).thenReturn(17L);
        distributionLoader = new DistributionLoader(httpClient, dictionaryStorage, tvmTicketSupplier) {
            @Override
            public DictionaryLoadIterator iterator(LocalDateTime day) {
                return iterator;
            }

            @Override
            public DictionaryLoadIterator iterator(String cluster, LocalDateTime day) {
                return iterator;
            }
        };
        long periods = distributionLoader.load(DEFAULT_CLUSTER, dateTime);
        verify(dictionaryStorage).save(DEFAULT_CLUSTER, expectedDict, dateTime, iterator);
        assertThat(periods, equalTo(17L));
    }

}
