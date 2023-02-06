package ru.yandex.market.clickphite.dictionary.loaders.host2dc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.clickphite.dictionary.Dictionary;
import ru.yandex.market.clickphite.dictionary.DictionaryLoader;
import ru.yandex.market.clickphite.dictionary.dicts.HostToDcDictionary;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class Host2DcDictionaryLoaderTest {
    @Spy
    private final Host2DcDictionaryLoader loader = new Host2DcDictionaryLoader();
    private final Dictionary dictionary = new HostToDcDictionary();
    private List<DictionaryData> walleBotData;
    private String walleBotDataJson;
    private static final String[] WALLE_API_DATA_JSON = {
        "{\"next_cursor\": 111881, \"result\": [{\"inv\": 111809, \"location\": {\"short_datacenter_name\": \"iva\", " +
            "\"switch\": \"iva5-s41\"}, \"name\": \"iva1-01.search.yandex.net\"}, {\"inv\": 111827, \"location\": " +
            "{\"short_datacenter_name\": \"iva\", \"switch\": \"iva5-s41\"}, \"name\": \"iva1-02.search.yandex" +
            ".net\"}, {\"inv\": 111844, \"location\": {\"short_datacenter_name\": \"iva\", \"switch\": \"iva5-s41\"}," +
            " \"name\": \"iva1-y0-03.search.yandex.net\"}], \"total\": 132604}",
        "{\"next_cursor\": 111887, \"result\": [{\"inv\": 111881, \"location\": {\"short_datacenter_name\": \"iva\", " +
            "\"switch\": \"iva5-s37\"}, \"name\": \"iva2-01.cloud.yandex.net\"}, {\"inv\": 111882, \"location\": " +
            "{\"short_datacenter_name\": \"iva\", \"switch\": \"iva5-s37\"}, \"name\": \"iva2-02.cloud.yandex.net\"}," +
            " {\"inv\": 111885, \"location\": {\"short_datacenter_name\": \"iva\", \"switch\": \"iva5-s37\"}, " +
            "\"name\": \"iva2-y0-03.cloud.yandex.net\"}]}",
        "{\"result\": [{\"inv\": 111887, \"location\": {\"short_datacenter_name\": \"iva\", \"switch\": " +
            "\"iva5-s45\"}, \"name\": \"iva3-01.cloud.yandex.net\"}, {\"inv\": 111888, \"location\": " +
            "{\"short_datacenter_name\": \"iva\", \"switch\": \"iva5-s45\"}, \"name\": \"iva3-02.cloud.yandex.net\"}," +
            " {\"inv\": 111889, \"location\": {\"short_datacenter_name\": \"iva\", \"switch\": \"iva5-s45\"}, " +
            "\"name\": \"iva3-y0-03.cloud.yandex.net\"}]}"
    };

    private static final List<DictionaryData> WALLE_API_DATA = Arrays.asList(
        new DictionaryData("iva1-01.search.yandex.net", "iva", "iva5-s41"),
        new DictionaryData("iva1-02.search.yandex.net", "iva", "iva5-s41"),
        new DictionaryData("iva1-y0-03.search.yandex.net", "iva", "iva5-s41"),
        new DictionaryData("iva2-01.cloud.yandex.net", "iva", "iva5-s37"),
        new DictionaryData("iva2-02.cloud.yandex.net", "iva", "iva5-s37"),
        new DictionaryData("iva2-y0-03.cloud.yandex.net", "iva", "iva5-s37"),
        new DictionaryData("iva3-01.cloud.yandex.net", "iva", "iva5-s45"),
        new DictionaryData("iva3-02.cloud.yandex.net", "iva", "iva5-s45"),
        new DictionaryData("iva3-y0-03.cloud.yandex.net", "iva", "iva5-s45")
    );

    private static final String[] WALLE_DC_API_URLS = {
        "https://api.wall-e.yandex-team.ru/test/hosts?fields=name,location.short_datacenter_name," +
            "location.switch&cursor=0&limit=3",
        "https://api.wall-e.yandex-team.ru/test/hosts?fields=name,location.short_datacenter_name," +
            "location.switch&cursor=111881&limit=3",
        "https://api.wall-e.yandex-team.ru/test/hosts?fields=name,location.short_datacenter_name," +
            "location.switch&cursor=111887&limit=3"
    };

    @Before
    public void setUp() {
        walleBotData = Arrays.asList(
            new DictionaryData("sas2-4631.search.yandex.net", "sas", "sas-1.1.3"),
            new DictionaryData("vla2-4632.search.yandex.net", "vla", "vla-1.1.4")
        );

        loader.setWalleAllHostWithDcAPIUrl(
            "https://api.wall-e.yandex-team.ru/test/hosts?fields=name," +
                "location.short_datacenter_name,location.switch&cursor=%s&limit=3"
        );

        walleBotDataJson = "[{\"Inv\":\"900156285\",\"FQDN\":\"sas2-4631.search.yandex.net\"," +
            "\"Status\":\"OPERATION\",\"LocationSegment1\":\"RU\",\"LocationSegment2\":\"SAS\"," +
            "\"LocationSegment3\":\"SASTA\",\"LocationSegment4\":\"SAS-1.1.3\",\"LocationSegment5\":\"12\"," +
            "\"LocationSegment6\":\"15\",\"LocationSegment7\":\"-\",\"MAC1\":\"00259094133E\"," +
            "\"MAC2\":\"00259094133F\",\"MAC3\":null,\"MAC4\":null,\"ExMACs\":null,\"Service\":\"Market Services > " +
            "\\u042d\\u043a\\u0441\\u043f\\u043b\\u0443\\u0430\\u0442\\u0430\\u0446\\u0438\\u044f " +
            "\\u041c\\u0430\\u0440\\u043a\\u0435\\u0442\\u0430 > -\",\"segment1\":\"SM\\/SYS6017RNTF\\/4T3" +
            ".5\\/1U\\/1P\",\"segment2\":\"XEONE5-2660\",\"segment3\":\"SERVERS\",\"segment4\":\"SRV\"," +
            "\"motherboard\":\"X9DRW-IF\",\"IPMI\":\"0025909DFD6A\",\"planner_id\":\"969\",\"connected_slot\":null}";
        walleBotDataJson += ",{\"Inv\":\"900156285777\",\"FQDN\":\"vla2-4632.search.yandex.net\"," +
            "\"Status\":\"OPERATION\",\"LocationSegment1\":\"RU\",\"LocationSegment2\":\"VLADIMIR\"," +
            "\"LocationSegment3\":\"VLATA\",\"LocationSegment4\":\"VLA-1.1.4\"}]";
    }

    @Test
    public void toConductorDc() {
        String conductorDataJson = "[" +
            "{\"name\":\"rootDc\",\"golem_name\":\"Root DC\",\"root_id\":null,\"parent\":null,\"children\": " +
            "[\"childDc\"]}," +
            "{\"name\":\"ignoredChildDc\",\"golem_name\":\"Ignored child DC\",\"root_id\":1,\"parent\":\"rootDc\"," +
            "\"children\": []}," +
            "{\"name\":\"oneMoreRootDc\",\"golem_name\":\"One more root DC\",\"root_id\":null,\"parent\":null," +
            "\"children\": []}" +
            "]";

        List<ConductorDc> expected = Arrays.asList(
            buildRootConductorDc("rootDc", "Root DC", Collections.singletonList("childDc")),
            buildRootConductorDc("oneMoreRootDc", "One more root DC", Collections.emptyList())
        );

        List<ConductorDc> result = new ArrayList<>();
        loader.toConductorDc(getReader(conductorDataJson), result);
        assertEquals(expected, result);
    }

    private BufferedReader getReader(String data) {
        return new BufferedReader(new StringReader(data));
    }

    private ConductorDc buildRootConductorDc(String name, String golemName, List<String> children) {
        ConductorDc result = new ConductorDc();
        result.setName(name);
        result.setGolemName(golemName);
        result.setChildren(children);

        return result;
    }

    @Test
    public void parseJson() throws IOException {
        List<DictionaryData> loadedData = new ArrayList<>();
        DictionaryLoader.DictionaryDataReader processor = loader.walleDataProcessor(dictionary, loadedData);
        processor.process(getReader(walleBotDataJson));

        assertEquals(walleBotData, loadedData);
    }

    @Test
    public void parseJsonForWalleAPIData() throws IOException {
        List<DictionaryData> loadedData = new ArrayList<>();
        DictionaryLoader.DictionaryDataReader processor = loader.walleAPIDataProcessor(dictionary, loadedData);
        // Perform 3 requests and process it in case if next_cursor is not null 2 times
        processor.process(getReader(WALLE_API_DATA_JSON[0]));
        processor.process(getReader(WALLE_API_DATA_JSON[1]));
        processor.process(getReader(WALLE_API_DATA_JSON[2]));

        assertEquals(WALLE_API_DATA, loadedData);
    }

    @Test
    public void processWalleAPIData() throws IOException {
        Mockito.doReturn(111881L).when(loader).loadOverHttp(
            eq(WALLE_DC_API_URLS[0]),
            any(DictionaryLoader.DictionaryDataReader.class)
        );
        Mockito.doReturn(111887L).when(loader).loadOverHttp(
            eq(WALLE_DC_API_URLS[1]),
            any(DictionaryLoader.DictionaryDataReader.class)
        );
        Mockito.doReturn(null).when(loader).loadOverHttp(
            eq(WALLE_DC_API_URLS[2]),
            any(DictionaryLoader.DictionaryDataReader.class)
        );

        loader.getWalleAPIData(dictionary);

        Mockito.verify(loader, Mockito.times(3)).loadOverHttp(anyString(),
            any(DictionaryLoader.DictionaryDataReader.class));
        Mockito.verify(loader).loadOverHttp(eq(WALLE_DC_API_URLS[0]), any(DictionaryLoader.DictionaryDataReader.class));
        Mockito.verify(loader).loadOverHttp(eq(WALLE_DC_API_URLS[1]), any(DictionaryLoader.DictionaryDataReader.class));
        Mockito.verify(loader).loadOverHttp(eq(WALLE_DC_API_URLS[2]), any(DictionaryLoader.DictionaryDataReader.class));
    }
}
