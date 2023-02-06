package ru.yandex.market.stat.dicts.loaders.anaplan;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.stat.dicts.bazinga.BazingaHelper;
import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.loaders.jdbc.SchemelessDictionaryRecord;
import ru.yandex.market.stat.dicts.services.DictionaryStorage;
import ru.yandex.mysqlDiff.dep.scala.actors.threadpool.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.stat.dicts.bazinga.BazingaHelper.HALF_HOURLY;
import static ru.yandex.market.stat.dicts.bazinga.BazingaHelper.HALF_HOURLY_REGEX;
import static ru.yandex.market.stat.dicts.bazinga.BazingaHelper.HOURLY_REGEX;

public class TypelessAnaplanLoaderV2Test {
    public static final String DEFAULT_CLUSTER = "hahn";
    public static final String EXPORT_NAME = "exportName";
    public static final String MODELID = "modelid";
    public static final String WORKSPACE = "workspace";
    public static final String TOKEN = "token";
    public static final String EXPORT_ID = "exportId";
    public static final String TASK_ID = "taskId";
    public static final String FILE_ID = "fileid";
    private static final String KEY_STORE_FILE = "test_anaplan_keystore.pkcs12";
    private static final String ANAPLAN_CERT_ALIAS = "testanaplancerts";
    private static final String ANAPLAN_KEY_PASS = "test_password";
    private static final String KEY_STORE_PASSWORD = "test_password";
    @Mock
    private DictionaryStorage dictionaryStorage;
    @Mock
    private AnaplanApiV2 api;
    @Mock
    private File file;
    private CertificateInfo certificateInfo = getTestCertInfo();

    private AnaplanTaskDefinition task;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSanitizer() {
        assertEquals("01_98_pl_export_oroboros_tabular_single_c",
                AnaplanNameUtil.sanitizeAnaplanName("01.98 PL Export - oroboros - tabular single c"));
        assertEquals("_hf",
                AnaplanNameUtil.sanitizeAnaplanName("@@*&#(*$&(HF"));
        assertEquals("абвёё01",
                AnaplanNameUtil.sanitizeAnaplanName("абвёЁ01"));
    }

    @Test
    public void testSanitizerAndPrepender() {
        assertEquals("pref__01_98_pl_export_oroboros_tabular_single_c",
                AnaplanNameUtil.sanitizeAnaplanNameAndPrepend("01.98 PL Export - oroboros - tabular single c", "pref"));
        assertEquals("01_98_pl_export_oroboros_tabular_single_c",
                AnaplanNameUtil.sanitizeAnaplanNameAndPrepend("01.98 PL Export - oroboros - tabular single c", null));
    }

    @Test
    public void testWtfDictName() throws Exception {
        configurationToSimulateLoad();

        TypelessAnaplanLoaderV2 loader = new TypelessAnaplanLoaderV2(dictionaryStorage, task, api, certificateInfo);
        Dictionary<SchemelessDictionaryRecord> dictionary = loader.getDictionary();
        assertEquals("anaplan__exportname", dictionary.getName());
        assertEquals("anaplan__exportname-1d", dictionary.nameForLoader());
        loader.load(DEFAULT_CLUSTER, LocalDateTime.now());
        Dictionary<SchemelessDictionaryRecord> dictionaryAfterLoad = loader.getDictionary();
        assertEquals("anaplan__exportname", dictionaryAfterLoad.getName());
        assertEquals("anaplan__exportname-1d", dictionaryAfterLoad.nameForLoader());
        assertEquals(Long.valueOf(1L), dictionary.getTtlDays());

    }

    @Test
    public void testAnaplanTaskDefinition() {
        AnaplanTaskDefinition task = AnaplanTaskDefinition.builder()
                .workspaceId("8a81b01067620e45016766207301057a")
                .modelId("2379A0358909451CA244BCA59472E0CC")
                .exportName("Grid_export_filtered".trim())
                .ttlDays(LoaderScale.DAYLY.getDefaultTTLDays())
                .loadTimeoutMinutes(2 * 60L)
                .build();

        assertEquals(task.getWorkspaceId(), "8a81b01067620e45016766207301057a");
        assertEquals(task.getModelId(), "2379A0358909451CA244BCA59472E0CC");
        assertEquals(task.getExportName(), "Grid_export_filtered");
        assertEquals(task.getLoadTimeoutMinutes(), Long.valueOf(2 * 60));
        assertEquals(task.getTtlDays(), Long.valueOf(-1));
    }

    @Test
    public void testAnaplanLoadIterator() throws IOException {
        String oneHeader = "First,Second,abc,def\n" +
                "bla,abl,lab,123\n";
        String twoHeaders = "2020\n" +
                "First,Second,abc,def\n" +
                "1234,4321,2342,ggrd\n";

        Set<String> firstResult = new HashSet<>(Sets.newHashSet("bla", "abl", "lab", "123"));
        Set<String> secondResult = new HashSet<>(Sets.newHashSet("1234", "4321", "2342", "ggrd"));

        // Column Names after sanitizing
        String[] columns = new String[]{"first", "second", "abc", "def"};

        CSVFormat firstFormat = AnaplanLoadIterator.getCSVFormat(',', '"');
        CSVParser firstParser = firstFormat.parse(new StringReader(oneHeader));
        Iterator<CSVRecord> firstIterator = firstParser.iterator();
        AnaplanLoadIterator.skipHeaderLines(firstIterator, Arrays.asList(columns), UTF_8.name());
        assertEquals(Sets.newHashSet(firstIterator.next().iterator()), firstResult);


        CSVFormat secondFormat = AnaplanLoadIterator.getCSVFormat(',', '"');
        CSVParser secondParser = secondFormat.parse(new StringReader(twoHeaders));
        Iterator<CSVRecord> secondIterator = secondParser.iterator();
        AnaplanLoadIterator.skipHeaderLines(secondIterator, Arrays.asList(columns), UTF_8.name());
        assertEquals(Sets.newHashSet(secondIterator.next().iterator()), secondResult);
    }

    @Test
    public void testAnaplanLoadIteratorEncodedData() throws IOException {
        String value = "Первый столбец,Second,abc,def\n" +
                "bla,abl,lab,123\n";

        byte[] ptext = value.getBytes(UTF_8);
        String oneHeader = new String(ptext, UTF_8);

        Set<String> firstResult = new HashSet<>(Sets.newHashSet("bla", "abl", "lab", "123"));

        // Column Names after sanitizing
        String[] columns = new String[]{"первый_столбец", "second", "abc", "def"};

        CSVFormat firstFormat = AnaplanLoadIterator.getCSVFormat(',', '"');
        CSVParser firstParser = firstFormat.parse(new StringReader(oneHeader));
        Iterator<CSVRecord> firstIterator = firstParser.iterator();
        AnaplanLoadIterator.skipHeaderLines(firstIterator, Arrays.asList(columns), UTF_8.name());
        assertEquals(Sets.newHashSet(firstIterator.next().iterator()), firstResult);
    }

    @Test
    public void testAllowEmpty() {
        task = AnaplanTaskDefinition.builder().exportName(EXPORT_NAME)
                .modelId(MODELID).workspaceId(WORKSPACE).ttlDays(1L).allowEmpty(false).skipEmpty(false).build();
        TypelessAnaplanLoaderV2 loader = new TypelessAnaplanLoaderV2(dictionaryStorage, task, api, certificateInfo);
        System.out.println(loader.getCron().toPrettyString());
        assertFalse("Wrong allowEmpty, should be false!",
                loader.allowEmpty());
        assertFalse("Wrong skipEmpty, should be false!",
                loader.skipEmpty());

        task = AnaplanTaskDefinition.builder().exportName(EXPORT_NAME)
                .modelId(MODELID).workspaceId(WORKSPACE).ttlDays(1L).allowEmpty(true).skipEmpty(false).build();
        loader = new TypelessAnaplanLoaderV2(dictionaryStorage, task, api, certificateInfo);
        System.out.println(loader.getCron().toPrettyString());
        assertTrue("Wrong allowEmpty, should be true!",
                loader.allowEmpty());
        assertFalse("Wrong skipEmpty, should be false!",
                loader.skipEmpty());

        task = AnaplanTaskDefinition.builder().exportName(EXPORT_NAME)
                .modelId(MODELID).workspaceId(WORKSPACE).ttlDays(1L).allowEmpty(true).skipEmpty(true).build();
        loader = new TypelessAnaplanLoaderV2(dictionaryStorage, task, api, certificateInfo);
        System.out.println(loader.getCron().toPrettyString());
        assertTrue("Wrong allowEmpty, should be true!", loader.allowEmpty());
        assertTrue("Wrong skipEmpty, should be true!", loader.skipEmpty());
    }


    @Test
    public void testCron() {
        //cron by period_hours
        task = AnaplanTaskDefinition.builder().exportName(EXPORT_NAME)
                .modelId(MODELID).workspaceId(WORKSPACE).ttlDays(1L).periodHours(1L).cron(HALF_HOURLY).build();
        TypelessAnaplanLoaderV2 loader = new TypelessAnaplanLoaderV2(dictionaryStorage, task, api, certificateInfo);
        System.out.println(loader.getCron().toPrettyString());
        assertTrue("Wrong cron for 1h reloads!",
                loader.getCron().toPrettyString().matches(HALF_HOURLY_REGEX));

        //cron not by period_hours
        task = AnaplanTaskDefinition.builder().exportName(EXPORT_NAME)
                .modelId(MODELID).workspaceId(WORKSPACE).ttlDays(1L).periodHours(4L).build();
        loader = new TypelessAnaplanLoaderV2(dictionaryStorage, task, api, certificateInfo);
        assertTrue("Wrong cron default value!",
                loader.getCron().toPrettyString().matches(HOURLY_REGEX));

        //cron by config
        String cron = "7 * * * * *";
        task = AnaplanTaskDefinition.builder().exportName(EXPORT_NAME)
                .modelId(MODELID).workspaceId(WORKSPACE).ttlDays(1L).periodHours(4L).cron(cron).build();
        loader = new TypelessAnaplanLoaderV2(dictionaryStorage, task, api, certificateInfo);
        assertThat("Wrong cron for config value!",
                loader.getCron().toPrettyString(), is(BazingaHelper.cron(cron).toPrettyString()));


        //cron by config half_hourly
        task = AnaplanTaskDefinition.builder().exportName(EXPORT_NAME)
                .modelId(MODELID).workspaceId(WORKSPACE).ttlDays(1L).cron(HALF_HOURLY).build();
        loader = new TypelessAnaplanLoaderV2(dictionaryStorage, task, api, certificateInfo);
        assertTrue("Wrong cron for config half_hourly",
                loader.getCron().toPrettyString().matches(HALF_HOURLY_REGEX));

    }

    @Test
    public void testNamePrefixProvided() {
        task = AnaplanTaskDefinition.builder().exportName(EXPORT_NAME).env("tests").folder("this_is_folder")
                .modelId(MODELID).workspaceId(WORKSPACE).ttlDays(1L).periodHours(1L).cron(HALF_HOURLY).build();
        TypelessAnaplanLoaderV2 loader = new TypelessAnaplanLoaderV2(dictionaryStorage, task, api, certificateInfo);
        assertEquals(
                "Wrong env prefix in relative path",
                ("this_is_folder/tests__" + EXPORT_NAME.toLowerCase()),
                loader.getRelativePath()
        );
    }

    @Test
    public void testNamePrefixNotProvided() {
        task = AnaplanTaskDefinition.builder().exportName(EXPORT_NAME).folder("this_is_folder")
                .modelId(MODELID).workspaceId(WORKSPACE).ttlDays(1L).periodHours(1L).cron(HALF_HOURLY).build();
        TypelessAnaplanLoaderV2 loader = new TypelessAnaplanLoaderV2(dictionaryStorage, task, api, certificateInfo);
        assertEquals(
                "Wrong env prefix in relative path",
                ("this_is_folder/" + EXPORT_NAME.toLowerCase()),
                loader.getRelativePath()
        );
    }

    @Test(expected = IllegalStateException.class)
    public void testAnaplanFileWithoutHeader() throws IOException {
        String oneHeader = Strings.repeat("abc,sdsd,a123,f54f\n", 1001);

        String[] columns = new String[]{"first", "second", "abc", "def"};

        CSVFormat firstFormat = AnaplanLoadIterator.getCSVFormat(',', '"');
        CSVParser firstParser = firstFormat.parse(new StringReader(oneHeader));
        Iterator<CSVRecord> firstIterator = firstParser.iterator();
        AnaplanLoadIterator.skipHeaderLines(firstIterator, Arrays.asList(columns), UTF_8.name());
    }

    private void configurationToSimulateLoad() throws IOException {
        task = AnaplanTaskDefinition.builder().exportName(EXPORT_NAME).modelId(MODELID).workspaceId(WORKSPACE).ttlDays(1L).build();
        AnaplanExportMetadata anaplanExportMetadata = new AnaplanExportMetadata();
        anaplanExportMetadata.setExportFormat("text/csv");
        anaplanExportMetadata.setColumnCount(1L);
        anaplanExportMetadata.setHeaderNames(Collections.singletonList("status"));
        anaplanExportMetadata.setDataTypes(Collections.singletonList("String"));
        anaplanExportMetadata.setSeparator(",");
        anaplanExportMetadata.setDelimiter("/");
        anaplanExportMetadata.setEncoding(UTF_8.name());

        when(api.getExportId(WORKSPACE, MODELID, EXPORT_NAME, certificateInfo)).thenReturn(EXPORT_ID);
        when(api.getExportInfo(WORKSPACE, MODELID, EXPORT_ID, certificateInfo)).thenReturn(anaplanExportMetadata);
        when(api.runExport(WORKSPACE, MODELID, EXPORT_ID, certificateInfo)).thenReturn(TASK_ID);
        when(api.monitorTaskStatus(WORKSPACE, MODELID, "exports", EXPORT_ID, TASK_ID, certificateInfo)).thenReturn(FILE_ID);
        File tmpFile = File.createTempFile("dictionaries_test", null);
        new GZIPOutputStream(new FileOutputStream(tmpFile)).close();
        tmpFile.deleteOnExit();
        when(api.getFile(WORKSPACE, MODELID, FILE_ID, certificateInfo)).thenReturn(tmpFile);
    }

    @SneakyThrows
    private CertificateInfo getTestCertInfo() {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        ClassLoader classLoader = getClass().getClassLoader();

        keyStore.load(classLoader.getResourceAsStream(KEY_STORE_FILE), KEY_STORE_PASSWORD.toCharArray());
        CertificateInfo certificateInfo =
                new CertificateInfo(keyStore, ANAPLAN_CERT_ALIAS, ANAPLAN_KEY_PASS);
        return certificateInfo;
    }
}
