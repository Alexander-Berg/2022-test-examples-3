package ru.yandex.market.clickphite.dictionary.loaders.host2dc;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.clickphite.dictionary.Dictionary;
import ru.yandex.market.clickphite.dictionary.dicts.HostToDcDictionary;

@Ignore
public class Host2DcDictionaryLoaderIntegrationTest {
    private final Host2DcDictionaryLoader loader = new Host2DcDictionaryLoader();
    private final Dictionary dictionary = new HostToDcDictionary();
    private static final Logger log = LogManager.getLogger();

    @Before
    public void setUp() {
        loader.setWalleAllHostWithDcBotUrl(
            "https://bot.yandex-team.ru/api/view.php?name=view_oops_hardware&format=json"
        );
        loader.setWalleAllHostWithDcAPIUrl(
            "https://api.wall-e.yandex-team.ru/v1/hosts?fields=name,locatio.short_datacenter_name," +
                "location.switch&cursor=%s&limit=10000"
        );
        loader.setConductorDataCenterHostsUrl("https://c.yandex-team.ru/api/dc2hosts");
        loader.setConductorDataCentersUrl("https://c.yandex-team.ru/api/datacenters?format=json");
    }

    @Test
    public void loadDataFromHost2DcDataSources() {

        log.info("data source = conductor");
        Set<String> firstDataSet = getData(this::loadFromConductor);
        int firstDataSetSize = firstDataSet.size();

        log.info("data source = walle bot");
        Set<String> secondDataSet = getData(this::loadFromWalleBot);


        /* Хотим узнать сколько данных нет во втором источнике, но есть в первом */
        firstDataSet.removeAll(secondDataSet);
        int firstDataSetNotFoundInSecondOneSize = firstDataSet.size();

        log.info("Number of hosts from first data source that aren't found in second data source: "
            + firstDataSetNotFoundInSecondOneSize);
        int firstDataSetFoundInSecondOneSize = firstDataSetSize - firstDataSetNotFoundInSecondOneSize;
        log.info("Number of hosts from first data source that are found in second data source: "
            + firstDataSetFoundInSecondOneSize);

        /* Листинг хостов */
        //log.info("\n\n----Hosts from first data source that aren't found in second data source----");
        //firstDataSet.stream().sorted().forEach(log::info);
    }


    private Set<String> getData(Supplier<Collection<DictionaryData>> s) {
        log.info("Loading data from data source");
        Stopwatch sw = Stopwatch.createStarted();


        Collection<DictionaryData> dataSource = s.get();

        Set<String> hostsSet = dataSource.stream()
            .map(DictionaryData::getHost)
            .collect(Collectors.toSet());


        log.info("Number of hosts from data source: " + hostsSet.size());
        log.info("-- Elapsed load time: " + sw.elapsed(TimeUnit.SECONDS) + " seconds\n");

        return hostsSet;
    }

    /* дает около 100к хостов в добавок к Bot (Walle). Number of hosts from data source: 149635 (16 sec) */
    private Collection<DictionaryData> loadFromConductor() {
        try {
            return loader.appendConductorData(Collections.emptyList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* загрузка быстрая, батчами. Number of hosts from data source: 131881 (12 sec) */
    private Collection<DictionaryData> loadFromWalleAPI() {
        try {
            return loader.getWalleAPIData(dictionary);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* (больше данных чем в API примерно на 12k). Number of hosts from data source: 144242  (55 sec)*/
    private Collection<DictionaryData> loadFromWalleBot() {
        try {
            return loader.getWalleBotData(dictionary);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
