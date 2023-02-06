package ru.yandex.market.stat.dicts.loaders;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.stat.dicts.config.ParsersDictsConfig;
import ru.yandex.market.stat.dicts.parsers.DictionaryParser;
import ru.yandex.market.stat.dicts.parsers.GeobaseDictionaryParser;
import ru.yandex.market.stat.dicts.records.DistributionRegionGroupRecord;
import ru.yandex.market.stat.dicts.records.RegionDictionaryRecord;
import ru.yandex.market.stat.parsers.ParserUtil;
import ru.yandex.market.stat.parsers.formats.TsvFormatParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;
import static ru.yandex.market.stat.parsers.ParserAnnotationUtils.allClassFields;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ParsersDictsConfig.class)
public class GeobaseLoaderTest {

    @Autowired
    @Qualifier("geobaseDictionaryParser")
    private GeobaseDictionaryParser parser;

    @Test
    public void testWithSandboxData() throws IOException {
        DictionaryLoadIterator<RegionDictionaryRecord> records =
            DictionaryLoadIterator.from(loadRecords(parser, "sandbox:geobase.tsv.gz"));
        List<DistributionRegionGroupRecord> regionGroups = loadRegionGroups();

        List<RegionDictionaryRecord> enrichedRecords =
            Lists.newArrayList(GeobaseLoader.withRegionGroup(records, regionGroups));

        assertThat(enrichedRecords.size(), equalTo(93844));
    }

    private List<DistributionRegionGroupRecord> loadRegionGroups() throws IOException {
        return loadRecords(new DictionaryParser<DistributionRegionGroupRecord>() {
            @Override
            public Class<DistributionRegionGroupRecord> getRecordClass() {
                return DistributionRegionGroupRecord.class;
            }

            @Override
            public DictionaryLoadIterator<DistributionRegionGroupRecord> createLoadIterator(InputStream is) throws IOException {
                TsvFormatParser parser = new TsvFormatParser(
                    allClassFields(DistributionRegionGroupRecord.class).map(Field::getName).collect(toList())
                );
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    return DictionaryLoadIterator.from(
                        reader.lines()
                            .map(parser::parse)
                            .map(kvs -> new DistributionRegionGroupRecord(
                                Integer.parseInt(kvs.get("id")),
                                kvs.get("name"),
                                parseIntList(kvs.get("inc_region_ids")),
                                parseIntList(kvs.get("exc_region_ids"))
                            ))
                            .collect(toList())
                    );
                }
            }

            private List<Integer> parseIntList(String value) {
                return ParserUtil.parseStringListValues(value, ",").stream()
                    .map(Integer::valueOf)
                    .collect(toList());
            }
        }, "/parsers/distr-groups.tsv");
    }
}
