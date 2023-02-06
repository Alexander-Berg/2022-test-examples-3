package ru.yandex.market.stat.dicts.services;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.common.YtException;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeBooleanNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.config.DictionariesITestConfig;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.records.DictionaryRecord;
import ru.yandex.market.stat.dicts.records.TestDictionary;
import ru.yandex.market.stat.dicts.records.TestDictionaryModified;
import ru.yandex.market.stat.dicts.utils.YtServiceUtils;
import ru.yandex.market.stat.yt.YtDictionaryPath;
import ru.yandex.market.stats.test.config.LocalPostgresInitializer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.stat.dicts.config.TestConstants.TEST_DIR;
import static ru.yandex.market.stat.dicts.config.TestConstants.THATS_WHY;
import static ru.yandex.market.stat.dicts.records.TestDictionary.item;
import static ru.yandex.market.stat.dicts.records.TestDictionary.makeDataWithIds;
import static ru.yandex.market.stat.yt.YtAttributes.SCHEMA;

/**
 * Created by kateleb on 20.04.17.
 */
@ActiveProfiles("integration-tests")
@ContextConfiguration(classes = DictionariesITestConfig.class, initializers = LocalPostgresInitializer.class)
@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@SpringBootTest
public class YtServiceITest {

    @Autowired
    private DictionaryYtService yt;

    private static final String REGEX = "^.*(.*\"name\"=.*){3}$";

    private static final LocalDate TEST_DAY = LocalDate.parse("2018-05-20");
    private Dictionary dictionary = Dictionary.fromClass(TestDictionary.class, LoaderScale.HOURLY);

    private YPath tablepath;
    private LocalDateTime now = LocalDateTime.now();
    private LocalDateTime yesterday = now.minusDays(1);

    @Before
    public void setup() {
        tablepath = yt.dictionaryPartitionTable(dictionary, now);
        yt.createTable(null, tablepath.append(true), YtServiceUtils.getTableSchema(dictionary));
    }

    @After
    public void tearDown() {
        yt.removePath(null, yt.absolutePath(TEST_DIR));
        yt.removePath(null, tablepath.parent());
    }

    @Test
    @Ignore(THATS_WHY)
    public void rootDirectoryExists() {
        assertTrue("Root directory does not exist!", yt.rootExists());
    }

    @Test
    @Ignore(THATS_WHY)
    public void createDirectory() {
        assertTrue("Root directory does not exist!", yt.rootExists());
        yt.mkdirs(yt.absolutePath(TEST_DIR));
        assertTrue("Test directory was not created!", yt.pathExists(yt.absolutePath(TEST_DIR)));
        yt.removePath(null, yt.absolutePath(TEST_DIR));
        assertFalse("Test directory still pathExists after removal!", yt.pathExists(yt.absolutePath(TEST_DIR)));
    }

    @Test
    @Ignore(THATS_WHY)
    public void listFiles() {
        yt.mkdirs(yt.absolutePath(TEST_DIR));
        List<YtDictionaryPath> files = yt.list(yt.getServiceRootPath());
        YtDictionaryPath ytDictionaryPath = files.stream().filter(f -> f.getPath().toString().contains(TEST_DIR)).findAny().orElse(null);
        assertThat(ytDictionaryPath, is(new YtDictionaryPath(yt.absolutePath(TEST_DIR), CypressNodeType.MAP)));
    }

    @Test
    @Ignore(THATS_WHY)
    public void isDirectory() {
        yt.mkdirs(yt.absolutePath(TEST_DIR));
        Assert.assertTrue("Not a directory!", yt.isDirectory(yt.absolutePath(TEST_DIR)));
    }

    @Test
    @Ignore(THATS_WHY)
    public void testTableCreated() {
        Assert.assertTrue("Can't create table", yt.pathExists(tablepath));
    }

    @Test
    @Ignore(THATS_WHY)
    public void testSchemaIsValid() {
        YTreeNode actualSchema = yt.getAttribute(tablepath, SCHEMA);
        log.debug("Existing table schema for {} : {}", dictionary.getName(), actualSchema);
        actualSchema.clearAttributes();

        YTreeListNode expectedSchema = YtServiceUtils.getTableSchema(dictionary);
        log.debug("Expected table schema for {} : {}", dictionary.getName(), expectedSchema);

        expectedSchema.asList().forEach(l -> l.asMap().put("required", new YTreeBooleanNodeImpl(false, null)));
        Assert.assertFalse(String.format("Schema is outated! Expected %s, but was %s",
            expectedSchema.toString(), actualSchema.toString()),
            !actualSchema.equals(expectedSchema));
        Assert.assertTrue("Invalid schema!", actualSchema.toString().matches(REGEX));
    }

    @Test
    @Ignore(THATS_WHY)
    public void insertValidData() {
        List<TestDictionary> data = makeDataWithIds("id1", "id2");
        writeToTableRawData(tablepath, data);
        checkDataPresent(data);
    }

    @Test
    @Ignore(THATS_WHY)
    public void checkRowCount() {
        List<TestDictionary> data = makeDataWithIds("id1", "id2");
        writeToTableRawData(tablepath, data);
        assertThat("Wrong row_count!", yt.getAttribute(tablepath, "row_count").integerNode().getLong(), is(2L));
    }


    @Test
    @Ignore(THATS_WHY)
    public void insertValidDataAndLinkToLatest() {
        List<TestDictionary> data = makeDataWithIds("id11", "id12");
        writeToTableRawData(tablepath, data);
        yt.markAsLatest(tablepath);
        List<TestDictionary> readData = Lists.newArrayList(yt.readFromTableAsIterator(
            tablepath.parent().child("latest"),
            Dictionary.fromClass(TestDictionary.class)
        ));
        log.info("Prepared data: {}", data);
        log.info("Yt data: {}", readData);
        assertThat("Wrong data in yt!", readData, equalTo(data));
    }

    @Test
    @Ignore(THATS_WHY)
    public void insertValidDataAndLinkToLatestTwice() {
        List<TestDictionary> data = makeDataWithIds("id11", "id12");
        writeToTableRawData(tablepath, data);
        yt.markAsLatest(tablepath);
        data = makeDataWithIds("id13", "id16");
        writeToTableRawData(tablepath, data);
        yt.markAsLatest(tablepath);
        List<TestDictionary> readData = Lists.newArrayList(yt.readFromTableAsIterator(
            tablepath.parent().child("latest"),
            Dictionary.fromClass(TestDictionary.class)
        ));
        log.info("Prepared data: {}", data);
        log.info("Yt data: {}", readData);
        assertThat("Wrong data in yt!", readData, equalTo(data));
    }

    @Test
    @Ignore(THATS_WHY)
    public void linkToLatestAndThenChangeLink() {
        List<TestDictionary> data = makeDataWithIds("id11", "id12");
        YPath tablePathYesterday = yt.dictionaryPartitionTable(dictionary, yesterday);
        writeToTableRawData(tablePathYesterday, data);
        yt.markAsLatest(tablePathYesterday);
        data = makeDataWithIds("id13", "id16");
        writeToTableRawData(tablepath, data);
        yt.markAsLatest(tablepath);
        List<TestDictionary> readData = Lists.newArrayList(
            yt.readFromTableAsIterator(tablepath.parent().child("latest"),
                Dictionary.fromClass(TestDictionary.class))
        );
        log.info("Prepared data: {}", data);
        log.info("Yt data: {}", readData);
        assertThat("Wrong data in yt!", readData, equalTo(data));
    }

    @Test
    @Ignore(THATS_WHY)
    public void testGetSize() {
        List<TestDictionary> data = makeDataWithIds("id11", "id12");
        YPath tablePathYesterday = yt.dictionaryPartitionTable(dictionary, yesterday);
        writeToTableRawData(tablePathYesterday, data);
        Long size = yt.getSize(dictionary,yesterday);
        assertThat(size, greaterThan(0L));
    }

    @Test
    @Ignore(THATS_WHY)
    public void insertInValidData() {
        List<TestDictionary> oldData = makeDataWithIds("id1");
        writeToTableRawData(tablepath, oldData);
        List<DictionaryRecord> invalidData = Arrays.asList((DictionaryRecord) item("id6"), modifiedItem("id7"));
        try {
            writeToTableRawData(tablepath, invalidData);
        } catch (YtException exception) {
            //we expect this
        }
        checkDataPresent(oldData);
    }

    @Test
    @Ignore(THATS_WHY)
    public void insertNullData() {
        List<TestDictionary> data = makeDataWithIds("id3", "id4");
        TestDictionary id5 = TestDictionary.builder().test_id("id5").build();
        data.add(id5);
        writeToTableRawData(tablepath, data);
        id5.setTest_number(0L);
        checkDataPresent(data);
    }

    private TestDictionaryModified modifiedItem(String id1) {
        return TestDictionaryModified.builder()
            .test_id(id1)
            .test_number(RandomUtils.nextInt())
            .experimental_field(new BigDecimal(RandomUtils.nextInt()))
            .yet_another("field")
            .build();
    }

    private void checkDataPresent(List<TestDictionary> data) {
        List<TestDictionary> readData =
            Lists.newArrayList(yt.readFromTableAsIterator(tablepath, Dictionary.fromClass(TestDictionary.class)));
        log.info("Prepared data: {}", data);
        log.info("Yt data: {}", readData);
        assertThat("Wrong data in yt!", readData, equalTo(data));
    }

    public <T extends DictionaryRecord> void writeToTableRawData(YPath tablePath, Collection<T> records) {
        ListF<YTreeMapNode> recordNodes = Cf.wrap(records).map(r -> YtServiceUtils.convertToYsonNode(r, TEST_DAY));
        yt.writeToTable(null, tablePath, recordNodes.iterator());
    }

}
