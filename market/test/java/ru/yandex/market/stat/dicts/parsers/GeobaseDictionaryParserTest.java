package ru.yandex.market.stat.dicts.parsers;

import com.google.common.collect.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.stat.dicts.config.ParsersDictsConfig;
import ru.yandex.market.stat.dicts.records.RegionDictionaryRecord;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;

/**
 * @author aostrikov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ParsersDictsConfig.class)
public class GeobaseDictionaryParserTest {

    @Autowired
    @Qualifier("geobaseDictionaryParser")
    private GeobaseDictionaryParser parser;

    @Test
    public void parse_geobase_snapshotWithSandboxData() throws IOException {
        // When
        List<RegionDictionaryRecord> records = loadRecords(parser, "sandbox:geobase.tsv.gz");
        Map<Integer, RegionDictionaryRecord> geobase = Maps.uniqueIndex(records, RegionDictionaryRecord::getId);

        // Then
        assertThat(geobase.entrySet(), hasSize(93844));

        RegionDictionaryRecord rfRegion = geobase.get(225);
        assertThat(rfRegion, is(notNullValue()));
        assertThat(rfRegion.getRu_name(), is("Россия"));
        assertThat(rfRegion.getCountry_id(), is(rfRegion.getId()));
        assertThat(rfRegion.getCountry_ru_name(), is(rfRegion.getRu_name()));
        assertThat(rfRegion.getType(), is(3));
        assertThat(rfRegion.getParent_id(), is(10001));
        assertThat(rfRegion.getParent_ru_name(), is("Евразия"));
        assertThat(rfRegion.getParent_type(), is(1));
        assertThat(rfRegion.getParents(), containsInAnyOrder(10000, 10001));
        assertThat(rfRegion.getChildren(), containsInAnyOrder(3, 17, 26, 40, 52, 59, 73, 381, 382, 102444));
        assertThat(rfRegion.getPath_ru_name(), contains("Россия", "Евразия", "Земля"));
        assertThat(rfRegion.getEn_name(), is("Russia"));
        assertThat(rfRegion.getCountry_en_name(), is("Russia"));
        assertThat(rfRegion.getParent_en_name(), is("Eurasia"));
        assertThat(rfRegion.getPath_en_name(), contains("Russia", "Eurasia", "Earth"));

        final RegionDictionaryRecord record = geobase.get(162721);
        assertThat(record.getPath_en_name(), is(
                Arrays.asList("Poselok 4-y km Mamayevskoy zh/d vetki",
                        "Kletnyanskoye urban settlement",
                        "Kletnyansky District",
                        "Bryansk Oblast",
                        "Central Federal District",
                        "Russia",
                        "Eurasia",
                        "Earth")));
        assertThat(record.getPath_ru_name(), is(
                Arrays.asList(
                        "Посёлок 4-й км Мамаевской ж/д ветки",
                        "Клетнянское городское поселение",
                        "Клетнянский район",
                        "Брянская область",
                        "Центральный федеральный округ",
                        "Россия",
                        "Евразия",
                        "Земля"
                )));
    }
}
