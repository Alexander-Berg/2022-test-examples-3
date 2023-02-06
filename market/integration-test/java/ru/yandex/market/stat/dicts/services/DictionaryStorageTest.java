package ru.yandex.market.stat.dicts.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.config.DictionariesITestConfig;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoadIterator;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoader;
import ru.yandex.market.stat.dicts.loaders.DistributionLoader;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.loaders.tvm.TvmTicketSupplier;
import ru.yandex.market.stat.dicts.records.DictionaryRecord;
import ru.yandex.market.stat.dicts.records.DistributionRecord;
import ru.yandex.market.stat.dicts.records.ProtestDictionary;
import ru.yandex.market.stat.dicts.records.TestDictionary;
import ru.yandex.market.stat.dicts.records.TestDictionaryLoader;
import ru.yandex.market.stat.dicts.utils.YtServiceUtils;
import ru.yandex.market.stat.parsers.services.FieldParserHelper;
import ru.yandex.market.stats.test.config.LocalPostgresInitializer;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.stat.dicts.config.TestConstants.THATS_WHY;
import static ru.yandex.market.stat.yt.YtAttributes.EXPIRATION_TIME;

/**
 * Created by kateleb on 04.05.17.
 */
@ActiveProfiles("integration-tests")
@ContextConfiguration(classes = DictionariesITestConfig.class, initializers = LocalPostgresInitializer.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class DictionaryStorageTest {
    private static final String DEFAULT_CLUSTER = "hahn";

    @Autowired
    private DictionaryYtService yt;

    @Autowired
    private YtDictionaryStorage storage;

    @Autowired
    private FieldParserHelper fieldParserHelper;

    @Test
    @Ignore(THATS_WHY)
    public void testDataIsLoadedToYt() throws IOException {
        List<String> ids = Arrays.asList("id1", "id2", "id3", "id4", "id5", "id6", "id7");
        TestDictionaryLoader loader = new TestDictionaryLoader(storage, ids, LoaderScale.DEFAULT);
        Dictionary<TestDictionary> dictionary = loader.getDictionary();
        LocalDateTime loadDayTime = LocalDate.parse("2018-12-05").atStartOfDay().plusHours(7);
        loader.load(DEFAULT_CLUSTER, loadDayTime);

        YPath yPath = yt.dictionaryPartitionTable(dictionary, dictionary.tablePartition(loadDayTime));
        Assert.assertThat("Wrong path in yt!!", yPath.toString(), endsWith("test_dictionary/2018-12-05"));

        List<TestDictionary> dictionaryRecords = readFromTable(yPath, dictionary);
        List<String> ytIds = dictionaryRecords.stream().map(TestDictionary::getTest_id).collect(toList());
        Assert.assertThat("Wrong data in yt!!", ids, everyItem(isIn(ytIds)));
        Assert.assertNotNull("expiration_time attribute doesn't exist", yt.getAttribute(yPath, EXPIRATION_TIME));
        yt.removePath(yPath);
    }

    @Test
    @Ignore(THATS_WHY)
    public void testDataIsLoadedToYtToSubdir() throws IOException {
        List<String> ids = Arrays.asList("id1", "id2", "id3", "id4", "id5", "id6", "id7");
        ProTestDictionaryLoader loader = new ProTestDictionaryLoader(storage, ids, LoaderScale.HOURLY);
        Dictionary<ProtestDictionary> dictionary = loader.getDictionary();
        LocalDateTime loadDayTime = LocalDate.parse("2018-12-05").atStartOfDay().plusHours(6);
        loader.load(DEFAULT_CLUSTER, loadDayTime);

        YPath ytPath = yt.dictionaryPartitionTable(dictionary, dictionary.tablePartition(loadDayTime));
        Assert.assertThat("Wrong path in yt!!", ytPath.toString(),
            endsWith("testDir/protest_dictionary/1h/2018-12-05T06:00:00"));

        List<ProtestDictionary> dictionaryRecords = readFromTable(ytPath, dictionary);

        List<String> ytIds = dictionaryRecords.stream().map(ProtestDictionary::getTest_id).collect(toList());
        Assert.assertThat("Wrong data in yt!!", ids, everyItem(isIn(ytIds)));
        yt.removePath(ytPath);
    }

    @Test
    @Ignore(THATS_WHY)
    public void testSave() throws IOException {

        LocalDateTime dateTime = LocalDate.parse("2018-12-05").atStartOfDay();
        List<DistributionRecord> data = Arrays.asList(
            new DistributionRecord(
                BigDecimal.valueOf(1L), BigDecimal.valueOf(2L), BigDecimal.valueOf(3L),
                BigDecimal.valueOf(4L), BigDecimal.valueOf(5L), BigDecimal.valueOf(6L),
                7, "8", 9, dateTime.toLocalDate()),
            new DistributionRecord(
                BigDecimal.valueOf(2L), BigDecimal.valueOf(3L), BigDecimal.valueOf(4L),
                BigDecimal.valueOf(5L), BigDecimal.valueOf(6L), BigDecimal.valueOf(7L),
                8, "9", 10, dateTime.toLocalDate()));
        DistributionLoader loader = new DistributionLoader(null, storage, mock(TvmTicketSupplier.class)) {
            @Override
            public DictionaryLoadIterator iterator(String cluster, LocalDateTime day) {
                return DictionaryLoadIterator.from(data);
            }
        };
        Dictionary dictionary = loader.getDictionary();
        YPath yPath = yt.dictionaryPartitionTable(dictionary, dictionary.tablePartition(dateTime));
        Assert.assertThat("Wrong data in yt!!", yPath.toString(), endsWith("distribution/2018-12-01"));
        yt.removePath(yt.dictionaryPartitionTable(dictionary, dateTime));
        loader.load(DEFAULT_CLUSTER, dateTime);
        Assert.assertTrue(yt.partitionExists(dictionary, dateTime));
        yt.removePath(yt.dictionaryPartitionTable(dictionary, dateTime));
    }

    @After
    public void tearDown() {
        yt.removePath(yt.getServiceRootPath());
    }

    @AllArgsConstructor
    public class ProTestDictionaryLoader implements DictionaryLoader<ProtestDictionary> {

        @Getter
        private final Class<ProtestDictionary> recordClass = ProtestDictionary.class;

        private DictionaryStorage dictionaryStorage;
        private List<String> ids;
        private LoaderScale scale;

        @Override
        public DictionaryLoadIterator<ProtestDictionary> iterator(LocalDateTime day) {
            return DictionaryLoadIterator.from(ProtestDictionary.makeDataWithIds(ids));
        }

        @Override
        public long load(String cluster, LocalDateTime day) throws IOException {
            return dictionaryStorage.save(DEFAULT_CLUSTER, getDictionary(), day, iterator(DEFAULT_CLUSTER, day));
        }

        @Override
        public Dictionary<ProtestDictionary> getDictionary() {
            return Dictionary.fromClass(getRecordClass(), scale);
        }

        @Override
        public String getSystemSource() {
            return "pro_test";
        }
    }

    public <T extends DictionaryRecord> List<T> readFromTable(YPath tablepath, Dictionary dictionary) {
        return yt.readFromTable(tablepath, ytreeIterator -> YtServiceUtils.parseData(
            Cf.x(ytreeIterator), dictionary, fieldParserHelper
        ));
    }
}
