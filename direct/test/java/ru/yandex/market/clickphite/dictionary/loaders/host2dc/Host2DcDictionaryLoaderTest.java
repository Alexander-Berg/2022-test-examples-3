package ru.yandex.market.clickphite.dictionary.loaders.host2dc;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.clickphite.dictionary.Dictionary;
import ru.yandex.market.clickphite.dictionary.DictionaryLoader;
import ru.yandex.market.clickphite.dictionary.dicts.HostToDcDictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Host2DcDictionaryLoaderTest {
    private final Host2DcDictionaryLoader loader = new Host2DcDictionaryLoader();
    private final Dictionary dictionary = new HostToDcDictionary();

    private List<DictionaryData> golemData;
    private String golemDataTsv;

    @Before
    public void setUp() {
        golemData = Arrays.asList(
            new DictionaryData("first-host", "with-first-dc", "with-stage"),
            new DictionaryData("second-host", "with-second-dc", null)
        );

        golemDataTsv = "first-host\twith-first-dc\twith-stage\n" +
            "second-host\twith-second-dc\t\n" +
            "third-host\t\t";
    }

    @Test
    public void toTsvFormat() {
        String hostsWithDc = "first-host\twith-first-dc\twith-stage\n" +
            "second-host\twith-second-dc\t";

        assertEquals(hostsWithDc, loader.toTsvFormat(golemData));
    }

    @Test
    public void fromTsv() throws IOException {
        List<DictionaryData> loadedData = new ArrayList<>();
        DictionaryLoader.DictionaryDataReader processor = loader.golemDataProcessor(dictionary, loadedData);
        processor.process(getReader(golemDataTsv));

        assertEquals(golemData, loadedData);
    }

    @Test
    public void appendConductorDcData() {
        Map<String, DictionaryData> hostToData = golemData.stream().collect(Collectors.toMap(DictionaryData::getHost, Function.identity()));

        loader.appendConductorDcData(getReader("first-host"), "wrong-dc", hostToData);
        loader.appendConductorDcData(getReader("third-host"), "dc-for-third-host", hostToData);
        loader.appendConductorDcData(getReader("absent-host-in-golem-without-dc"), "with-dc", hostToData);

        assertEquals(4, hostToData.size());
        assertEquals("Golem dc info was replaced", "with-first-dc", hostToData.get("first-host").getDc());
        assertEquals("Dc info from conductor not filled to empty golem data", "dc-for-third-host", hostToData.get("third-host").getDc());
        assertTrue("Host from conductor not added", hostToData.containsKey("absent-host-in-golem-without-dc"));
    }

    @Test
    public void toConductorDc() {
        String conductorDataJson = "[" +
            "{\"name\":\"rootDc\",\"golem_name\":\"Root DC\",\"root_id\":null,\"parent\":null,\"children\": [\"childDc\"]}," +
            "{\"name\":\"ignoredChildDc\",\"golem_name\":\"Ignored child DC\",\"root_id\":1,\"parent\":\"rootDc\",\"children\": []}," +
            "{\"name\":\"oneMoreRootDc\",\"golem_name\":\"One more root DC\",\"root_id\":null,\"parent\":null,\"children\": []}" +
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
}