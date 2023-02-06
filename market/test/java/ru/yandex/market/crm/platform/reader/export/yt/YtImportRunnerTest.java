package ru.yandex.market.crm.platform.reader.export.yt;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeTextSerializer;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.platform.services.config.ConfigRepository;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.config.YtSourceConfig;
import ru.yandex.market.crm.platform.reader.export.FactImportTaskStatus;
import ru.yandex.market.crm.platform.reader.export.FactsImportTasksService;
import ru.yandex.market.crm.platform.reader.export.YtImportContext;
import ru.yandex.market.crm.platform.reader.test.ServicesTestConfig;
import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;
import ru.yandex.market.crm.platform.yt.YtTables;
import ru.yandex.market.crm.util.yt.CommonAttributes;
import ru.yandex.market.mcrm.db.test.DbTestTool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ServicesTestConfig.class)
public class YtImportRunnerTest {

    private static final String FOLDER_PATH = "//home/market/example/folder";
    private static final String TABLE_PATH_1 = FOLDER_PATH + "/single_column_test_table_1";
    private static final String TABLE_PATH_2 = FOLDER_PATH + "/single_column_test_table_2";
    private static final String MAPPER = "SingleColumnMinimalExampleMapper";

    @Inject
    private DbTestTool dbTestTool;

    @Inject
    private Yt yt;

    @Inject
    private YtImportFactory importFactory;

    @Inject
    private FactsImportTasksService importTasksService;

    @Inject
    private ConfigRepository configRepository;

    @Inject
    private YtSchemaTestUtils schemaTestUtils;

    @Inject
    private YtTables ytTables;

    @Inject
    private SingleColumnMinimalExampleMapper mapper;

    private YPath table1;
    private YPath table2;

    private FactConfig targetFactMinimalExample;

    @Before
    public void before() {
        targetFactMinimalExample = configRepository.getFact("MinimalExample");
        schemaTestUtils.prepareFactTable(targetFactMinimalExample);
        mapper.reset();

        table1 = prepareTable(TABLE_PATH_1);
        table2 = prepareTable(TABLE_PATH_2);
    }

    @After
    public void after() {
        dbTestTool.clearDatabase();
        schemaTestUtils.removeCreated();

        yt.cypress().remove(table1);
        yt.cypress().remove(table2);
    }

    @Test
    public void newFactImportsAllDataFromTableTest() {
        addYtSource(TABLE_PATH_1);
        insertRows(table1, 100);
        runImport();
        assertSaved(100, 99, table1);
    }

    @Test
    public void existingTableImportFactStartFromLastRow() {
        addYtSource(TABLE_PATH_1);

        insertRows(table1, 100);
        runImport();
        assertSaved(100, 99, table1);

        // чистим таблицу с импортированными фактами
        schemaTestUtils.removeCreated();
        schemaTestUtils.prepareFactTable(targetFactMinimalExample);

        insertRows(table1, 105);
        runImport();
        assertSaved(5, 104, table1);
    }

    @Test
    public void existingTableImportDoesntRunWithoutUpdatesInTableTest() {
        addYtSource(TABLE_PATH_1);

        insertRows(table1, 10);
        runImport();
        assertSaved(10, 9, table1);

        // очищаем прошлые факты
        schemaTestUtils.removeCreated();
        schemaTestUtils.prepareFactTable(targetFactMinimalExample);

        runImport();
        assertSaved(0, 9, table1);
    }

    @Test
    public void failedImportHasCorrectStatusTest() {
        addYtSource(TABLE_PATH_1);

        insertRows(table1, 1);
        runImport();
        String importId = makeImportId(targetFactMinimalExample);
        YtImportContext lastSuccessCtx =
                importTasksService.getImportContext(importId, YtImportContext.class).orElse(null);
        assertNotNull(lastSuccessCtx);
        assertEquals(0, lastSuccessCtx.getLastRowImported());

        // кораптим таблицу для ошибки
        schemaTestUtils.removeCreated();

        // запускаем еще раз
        insertRows(table1, 2);
        runImport();

        FactImportTaskStatus status = importTasksService.getStatus(importId);
        assertEquals(FactImportTaskStatus.Status.ERROR, status.getStatus());
        YtImportContext contextAfterFail =
                importTasksService.getImportContext(importId, YtImportContext.class).orElse(null);

        assertNotNull(contextAfterFail);
        assertEquals(lastSuccessCtx.getModificationTime(), contextAfterFail.getModificationTime());
        assertEquals(lastSuccessCtx.getTablePath(), contextAfterFail.getTablePath());
        assertEquals(lastSuccessCtx.getLastRowImported(), contextAfterFail.getLastRowImported());
    }

    @Test
    public void newFolderImportStartsFromTheNewestTableTest() {
        addYtSource(FOLDER_PATH);

        insertRows(table1, 5);
        insertRows(table2, 6);

        runImport();

        assertSaved(6, 5, table2);
    }

    @Test
    public void existingFolderImportUploadAllNewTablesTest() {
        addYtSource(FOLDER_PATH);

        importTasksService.saveImportContext(
                makeImportId(targetFactMinimalExample),
                new YtImportContext(ZonedDateTime.now().toString(), "someTable", 100L));

        insertRows(table1, 7);
        insertRows(table2, 8);

        runImport();
        assertSaved(7, 6, table1);

        runImport();
        assertSaved(15, 7, table2);
    }

    @Test
    public void failedImportStartsFromLastSuccessfullRow() {
        addYtSource(TABLE_PATH_1);

        insertRows(table1, 200);
        mapper.setRowNumberOnError(151);
        runImport();

        YtImportContext ctx =
                importTasksService.getImportContext(makeImportId(targetFactMinimalExample), YtImportContext.class).get();
        assertNotNull(ctx);

        // батчер сохраняет по 100
        assertEquals(99, ctx.getLastRowImported());

        mapper.reset();

        // очищаем прошлые факты
        schemaTestUtils.removeCreated();
        schemaTestUtils.prepareFactTable(targetFactMinimalExample);

        // должны сохранить оставшиеся 100 из 200, а не читать заново
        runImport();
        assertSaved(100, 199, table1);
    }

    @Test
    public void failedImportInFolderStartsFromPrevSuccessffullRowTest() {
        addYtSource(FOLDER_PATH);

        String initModificationTime = ZonedDateTime.now().toString();
        importTasksService.saveImportContext(
                makeImportId(targetFactMinimalExample),
                new YtImportContext(initModificationTime, "somePreviousTable", 20));

        insertRows(table1, 200);
        insertRows(table2, 200);
        mapper.setRowNumberOnError(151);

        runImport();

        YtImportContext ctx = importTasksService.getImportContext(
                makeImportId(targetFactMinimalExample),
                YtImportContext.class
        ).get();

        // начинаем с 0, до 100 - загружаем, на 151 фейл
        assertEquals(99, ctx.getLastRowImported());
        // таблица сфейлилась, курсор остался на предыдущей успешной
        assertEquals(initModificationTime, ctx.getModificationTime());
        assertEquals(table1.toString(), ctx.getTablePath());

        mapper.reset();

        // дочитываем первую таблицу
        runImport();

        // обновляем контекст по первой таблице
        assertSaved(200, 199, table1);

        // считываем полностью вторую
        runImport();

        // контекст обновлен по второй таблице
        assertSaved(400, 199, table2);
    }

    private void addYtSource(String path) {
        YtSourceConfig source = new YtSourceConfig(
                Collections.singleton(Environment.INTEGRATION_TEST),
                "plato." + path, MAPPER,
                YtSourceConfig.NewTableStrategy.LAST
        );

        targetFactMinimalExample = new FactConfig(
                targetFactMinimalExample.getId(),
                targetFactMinimalExample.getTitle(),
                Collections.singleton(source),
                targetFactMinimalExample.getModel(),
                targetFactMinimalExample.getReducer(),
                targetFactMinimalExample.getLock(),
                List.of(),
                targetFactMinimalExample.getClusterStorages()
        );
    }

    private YPath prepareTable(String pathRaw) {
        YPath path = YPath.simple(pathRaw);
        InputStream is = getClass().getResourceAsStream("schema/single_column_test_table.yson");

        Map<String, YTreeNode> attrs = YTreeTextSerializer.deserialize(is).asMap();

        yt.cypress().create(path, CypressNodeType.TABLE, true, true, attrs);

        return path;
    }

    private void insertRows(YPath path, int rows) {
        List<YTreeMapNode> nodes = IntStream.rangeClosed(1, rows)
                .mapToObj(s -> YTree.mapBuilder().key("test_column").value(UUID.randomUUID().toString()).buildMap())
                .collect(Collectors.toList());

        yt.tables().write(path, YTableEntryTypes.YSON, Cf.x(nodes));
    }

    private void assertSaved(int totalFacts, int lastRow, YPath lastTableImported) {
        String importId = makeImportId(targetFactMinimalExample);
        Optional<YtImportContext> importContext =
                importTasksService.getImportContext(importId, YtImportContext.class);

        assertTrue(importContext.isPresent());
        assertEquals(totalFacts, getAllFacts(targetFactMinimalExample).size());

        String newModTime = yt.cypress().get(lastTableImported, Cf.set(CommonAttributes.MODIFICATION_TIME))
                .getAttributeOrThrow(CommonAttributes.MODIFICATION_TIME).stringValue();

        assertEquals(newModTime, importContext.get().getModificationTime());
        assertEquals(lastRow, importContext.get().getLastRowImported());
    }

    private List<Message> getAllFacts(FactConfig config) {
        YPath path = ytTables.getFactTable(config.getId());
        List<Message> res = new ArrayList<>();
        yt.tables().selectRows("* FROM [" + path + "]", YTableEntryTypes.YSON, node -> {
            try {
                res.add(config.getModel().getParser().parseFrom(node.get("fact").get().bytesValue()));
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        });
        return res;
    }

    private void runImport() {
        importFactory.create(Collections.singleton(targetFactMinimalExample)).run();
    }

    private String makeImportId(FactConfig config) {
        YtSourceConfig source = config.getSources()
                .stream()
                .filter(YtSourceConfig.class::isInstance)
                .findFirst()
                .map(YtSourceConfig.class::cast)
                .orElseThrow();

        return config.getId() + "#" + source.getPath();
    }
}
