package ru.yandex.market.deepmind.tms.executors.msku;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.MskuAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CargoTypeSnapshot;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.mocks.MboModelsServiceMock;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.repository.DeepmindCargoTypeSnapshotRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.services.cargotype.DeepmindCargoTypeCachingService;
import ru.yandex.market.deepmind.common.services.cargotype.DeepmindCargoTypeCachingServiceImpl;
import ru.yandex.market.deepmind.common.services.msku.ImportMskuService;
import ru.yandex.market.deepmind.tms.executors.ImportMskuExecutor;
import ru.yandex.market.deepmind.tracker_approver.utils.CurrentThreadExecutorService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.mboc.common.config.YtAndYqlJdbcAutoCluster;
import ru.yandex.market.mboc.common.msku.KnownMboParams;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;

import static org.assertj.core.api.Assertions.assertThat;

public class ImportMskuExecutorTest extends BaseImportMskuTest {
    private static final YPath MSKU_PATH = YPath.simple("//tmp/recent/models/msku");

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private MskuRepository deepmindMskuRepository;
    @Autowired
    private MskuStatusRepository mskuStatusRepository;
    @Autowired
    private DeepmindCargoTypeSnapshotRepository deepmindCargoTypeSnapshotRepository;

    private ImportMskuExecutor executor;

    private TestYt yt;
    private ImportMskuService importMskuService;
    private DeepmindCargoTypeCachingService cargoTypeCachingService;
    private NamedParameterJdbcTemplate yqlNamedParameterJdbcTemplate;
    private StorageKeyValueServiceMock storageKeyValue;
    private MskuAvailabilityChangedHandler mskuAvailabilityChangedHandler;
    private CargoTypeSnapshot cargoType1;
    private CargoTypeSnapshot cargoType2;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        cargoType1 = cargoTypeSnapshot(10L, 1L);
        cargoType2 = cargoTypeSnapshot(20L, 2L);
        deepmindCargoTypeSnapshotRepository.save(cargoType1, cargoType2);

        cargoTypeCachingService = new DeepmindCargoTypeCachingServiceImpl(deepmindCargoTypeSnapshotRepository);
        mskuAvailabilityChangedHandler = new MskuAvailabilityChangedHandler(changedSskuRepository,
            taskQueueRegistrator);
        importMskuService = new ImportMskuService(
            cargoTypeCachingService,
            mskuAvailabilityChangedHandler,
            mskuStatusRepository,
            namedParameterJdbcTemplate,
            transactionTemplate,
            new MboModelsServiceMock()
        );

        yt = new TestYt();
        yqlNamedParameterJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);

        storageKeyValue = new StorageKeyValueServiceMock();

        var ytAndYql = YtAndYqlJdbcAutoCluster.createMock(yt, yqlNamedParameterJdbcTemplate);
        executor = new ImportMskuExecutor(importMskuService, ytAndYql, namedParameterJdbcTemplate,
            storageKeyValue, MSKU_PATH, 1);
        executor.setExecutorService(new CurrentThreadExecutorService());

        yt.cypress().create(new CreateNode(MSKU_PATH, CypressNodeType.TABLE)
            .addAttribute("strict", true)
            .addAttribute("schema", YTree.listBuilder()
                .value(Map.of("required", "true", "name", "category_id", "type", "uint64", "sort_order", "asc"))
                .value(Map.of("required", "true", "name", "vendor_id", "type", "uint64", "sort_order", "asc"))
                .value(Map.of("required", "true", "name", "model_id", "type", "uint64", "sort_order", "asc"))
                .value(Map.of("required", "true", "name", "data", "type", "any"))
                .buildList())
        );
        Mockito.when(yqlNamedParameterJdbcTemplate.queryForList(Mockito.anyString(), Mockito.anyMap(),
                Mockito.eq(Long.class)))
            .thenAnswer(invocation -> {
                return yt.tables().read(MSKU_PATH, YTableEntryTypes.YSON).stream()
                    .map(v -> v.getLong("category_id"))
                    .distinct()
                    .collect(Collectors.toList());
            });
    }

    @Test
    public void testSimpleImport() {
        mockRecent("20200101_0001");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, List.of(
            node(11, 1, "Msku 11"),
            node(12, 1, "Msku 12"),
            node(23, 2, "Msku 23")
        ));

        executor.execute();

        var all = deepmindMskuRepository.findAll();
        assertThat(all)
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                msku(11L, 1L, "Msku 11"),
                msku(12L, 1L, "Msku 12"),
                msku(23L, 2L, "Msku 23")
            );
    }

    @Test
    public void noMsku() {
        mockRecent("20200101_0001");
        executor.execute();

        var all = deepmindMskuRepository.findAll();
        assertThat(all).isEmpty();
    }

    @Test
    public void addNewMsku() {
        mockRecent("20200101_0000");
        Msku msku1 = msku(11L, 1L, "Msku 11");
        Msku msku2 = msku(12L, 1L, "Msku 12");
        Msku msku3 = msku(13L, 1L, "Msku 13");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2, msku3));
        executor.execute();

        mockRecent("20200101_0001");
        clearQueue();
        Msku msku4 = msku(14L, 1L, "Msku 14");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2, msku3, msku4));
        executor.execute();

        List<Msku> actual = deepmindMskuRepository.findAll();
        assertMsku(actual, msku1, msku2, msku3, msku4);
        assertMskuStatus(
            Pair.of(msku1.getId(), MskuStatusValue.EMPTY),
            Pair.of(msku2.getId(), MskuStatusValue.EMPTY),
            Pair.of(msku3.getId(), MskuStatusValue.EMPTY),
            Pair.of(msku4.getId(), MskuStatusValue.EMPTY)
        );
        assertTaskQueue("SYNC_WITH_STUFF", 14L);
    }

    @Test // DEEPMIND-1040
    public void addNewMskuButWithExistingMskuStatus() {
        // arrange
        mockRecent("20200101_0001");
        Msku msku4 = msku(14L, 1L, "Msku 14");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku4));
        mskuStatusRepository.save(new MskuStatus()
            .setMarketSkuId(msku4.getId())
            .setMskuStatus(MskuStatusValue.REGULAR));

        // act
        executor.execute();

        // assert
        List<Msku> actual = deepmindMskuRepository.findAll();
        assertMsku(actual, msku4);
        assertMskuStatus(
            Pair.of(msku4.getId(), MskuStatusValue.REGULAR)
        );
        assertTaskQueue("SYNC_WITH_STUFF", 14L);
    }

    @Test
    public void markDeletedMsku() {
        mockRecent("20200101_0000");
        Msku msku1 = msku(11L, 1L, "Msku 11");
        Msku msku2 = msku(12L, 1L, "Msku 12");
        Msku msku3 = msku(13L, 1L, "Msku 13");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2, msku3));
        executor.execute();

        mockRecent("20200101_0001");
        clearQueue();
        Msku msku2deleted = msku2.setDeleted(true);
        Msku msku3deleted = msku3.setDeleted(true);
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2deleted, msku3deleted));

        executor.execute();

        List<Msku> actual = deepmindMskuRepository.findAll();
        assertMsku(actual, msku1, msku2deleted, msku3deleted);
        assertTaskQueue("SYNC_WITH_STUFF", 12L, 13L);
    }

    @Test
    public void testFastMskuIsNotDeleted() {
        mockRecent("20200101_0000");
        var model = model(11L, 1L, "Msku 11")
            .setCurrentType("FAST_SKU")
            .build();

        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, List.of(node(model)));
        executor.execute();

        mockRecent("20200101_0001");
        clearQueue();

        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, List.of());
        executor.execute();

        //verifying that msku4 is not deleted
        Msku actualMsku4 = deepmindMskuRepository.findById(11L).get();
        assertThat(actualMsku4.getDeleted()).isFalse();
        assertTaskQueue("SYNC_WITH_STUFF"); // no events in task_queue
    }

    @Test
    public void deleteMsku() {
        mockRecent("20200101_0000");
        Msku msku1 = msku(11L, 1L, "Msku 11");
        Msku msku2 = msku(12L, 1L, "Msku 12");
        Msku msku3 = msku(13L, 1L, "Msku 13");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2, msku3));
        executor.execute();

        mockRecent("20200101_0001");
        clearQueue();
        Msku msku2deleted = msku2.setDeleted(true);
        Msku msku3deleted = msku3.setDeleted(true);
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1));
        executor.execute();

        List<Msku> actual = deepmindMskuRepository.findAll();
        assertMsku(actual, msku1, msku2deleted, msku3deleted);
        assertTaskQueue("SYNC_WITH_STUFF", 12L, 13L);
    }

    @Test
    public void updateMskuVendorId() {
        mockRecent("20200101_0000");
        Msku msku1 = msku(11L, 1L, "Msku 11");
        Msku msku2 = msku(12L, 1L, "Msku 12");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2));
        executor.execute();

        mockRecent("20200101_0001");
        clearQueue();
        msku2.setVendorId(msku2.getVendorId() + 1);
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2));

        executor.execute();

        List<Msku> actual = deepmindMskuRepository.findAll();
        assertMsku(actual, msku1, msku2);
        assertTaskQueue("SYNC_WITH_STUFF", 12L);
    }

    @Test
    public void updateMskuCategory1() {
        mockRecent("20200101_0000");
        Msku msku1 = msku(11L, 2L, "Msku 11");
        Msku msku2 = msku(12L, 2L, "Msku 12");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2));
        executor.execute();

        mockRecent("20200101_0001");
        clearQueue();
        msku1.setCategoryId(1L);
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2));

        executor.execute();

        List<Msku> actual = deepmindMskuRepository.findAll();
        assertMsku(actual, msku1, msku2);
        assertTaskQueue("SYNC_WITH_STUFF", 11L);
    }

    @Test
    public void updateMskuCategory2() {
        mockRecent("20200101_0000");
        Msku msku1 = msku(11L, 2L, "Msku 11");
        Msku msku2 = msku(12L, 2L, "Msku 12");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2));
        executor.execute();

        mockRecent("20200101_0001");
        clearQueue();
        msku2.setCategoryId(100500L);
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2));

        executor.execute();

        List<Msku> actual = deepmindMskuRepository.findAll();
        assertMsku(actual, msku1, msku2);
        // Из-за особенностей реализации msku=12 будет 2 раза добавлено в очередь
        // Это, конечно, жертва во имя быстрого импорта
        assertTaskQueue("SYNC_WITH_STUFF", 12L, 12L);
    }

    @Test
    public void updateMskuCargoTypeLmsIds() {
        mockRecent("20200101_0000");
        Msku msku = msku(11L, 1L, "Msku 11");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku));
        executor.execute();

        mockRecent("20200101_0001");
        clearQueue();
        ModelStorage.Model.Builder modelBuilder = toProto(msku).toBuilder();
        cargoTypeCachingService.getMboParameterIds().forEach(paramId -> {
            ModelStorage.ParameterValue value = ModelStorage.ParameterValue.newBuilder()
                .setUserId(1)
                .setParamId(paramId)
                .setBoolValue(true)
                .setXslName("1")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setTypeId(MboParameters.ValueType.BOOLEAN_VALUE)
                .build();
            modelBuilder.addParameterValues(value);
        });
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, convertToNodes(modelBuilder.build()));

        executor.execute();

        assertTaskQueue("SYNC_WITH_STUFF", 11L);
    }

    @Test
    public void updateMskuExpirDate() {
        mockRecent("20200101_0000");
        Msku msku = msku(11L, 1L, "Msku 11");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku));
        executor.execute();

        mockRecent("20200101_0001");
        clearQueue();
        ModelStorage.Model.Builder modelBuilder = toProto(msku)
            .toBuilder();

        long paramId = 646313254L;
        ModelStorage.ParameterValue.Builder expirDate = ModelStorage.ParameterValue.newBuilder()
            .setUserId(1)
            .setParamId(paramId) // not same with cargotype
            .setBoolValue(true)
            .setXslName(KnownMboParams.EXPIR_DATE.mboXslName())
            .setValueType(MboParameters.ValueType.BOOLEAN)
            .setTypeId(MboParameters.ValueType.BOOLEAN_VALUE);

        modelBuilder.addParameterValues(expirDate);
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, convertToNodes(modelBuilder.build()));
        executor.execute();

        assertTaskQueue("SYNC_WITH_STUFF");
    }

    @Test
    public void updateSetCargotypeToFalse() {
        mockRecent("20200101_0000");
        Msku msku = msku(11L, 1L, "Msku 11");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku));
        executor.execute();

        mockRecent("20200101_0001");
        clearQueue();
        ModelStorage.Model.Builder modelBuilder = toProto(msku)
            .toBuilder();

        ModelStorage.ParameterValue.Builder expirDate = ModelStorage.ParameterValue.newBuilder()
            .setUserId(1)
            .setParamId(cargoType1.getMboParameterId())
            .setBoolValue(false)
            .setXslName(cargoType1.getDescription())
            .setValueType(MboParameters.ValueType.BOOLEAN)
            .setTypeId(MboParameters.ValueType.BOOLEAN_VALUE);

        modelBuilder.addParameterValues(expirDate);
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, convertToNodes(modelBuilder.build()));
        executor.execute();

        assertTaskQueue("SYNC_WITH_STUFF");
    }

    @Test
    public void updateSetCargotypeToTrue() {
        mockRecent("20200101_0000");
        Msku msku = msku(11L, 1L, "Msku 11");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku));
        executor.execute();

        mockRecent("20200101_0001");
        clearQueue();
        ModelStorage.Model.Builder modelBuilder = toProto(msku)
            .toBuilder();

        ModelStorage.ParameterValue.Builder expirDate = ModelStorage.ParameterValue.newBuilder()
            .setUserId(1)
            .setParamId(cargoType1.getMboParameterId())
            .setBoolValue(true)
            .setXslName(cargoType1.getDescription())
            .setValueType(MboParameters.ValueType.BOOLEAN)
            .setTypeId(MboParameters.ValueType.BOOLEAN_VALUE);

        modelBuilder.addParameterValues(expirDate);
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, convertToNodes(modelBuilder.build()));
        executor.execute();

        assertTaskQueue("SYNC_WITH_STUFF", 11L);
    }

    @Test
    public void importSameStuff() {
        mockRecent("20200101_0000");
        Msku msku1 = msku(11L, 1L, "Msku 11");
        Msku msku2 = msku(12L, 1L, "Msku 12");
        Msku msku3 = msku(13L, 1L, "Msku 13");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2, msku3));
        executor.execute();

        mockRecent("20200101_0000");
        Msku msku4 = msku(14L, 1L, "Msku 14");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2, msku3, msku4));

        executor.execute();

        List<Msku> actual = deepmindMskuRepository.findAll();
        assertMsku(actual, msku1, msku2, msku3);
    }

    @Test
    public void importOlderStuff() {
        mockRecent("20200101_0000");
        Msku msku1 = msku(11L, 1L, "Msku 11");
        Msku msku2 = msku(12L, 1L, "Msku 12");
        Msku msku3 = msku(13L, 1L, "Msku 13");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2, msku3));
        executor.execute();

        mockRecent("20190101_0000");
        Msku msku4 = msku(14L, 1L, "Msku 14");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, nodes(msku1, msku2, msku3, msku4));
        executor.execute();

        List<Msku> actual = deepmindMskuRepository.findAll();
        assertMsku(actual, msku1, msku2, msku3);
    }

    @Test
    public void differentOrderOfParameterValuesSameHash() {
        mockRecent("20200101_0000");
        Msku msku = msku(11L, 1L, "Msku 11");
        ModelStorage.ParameterValue value1 = ModelStorage.ParameterValue.newBuilder()
            .setUserId(1)
            .setParamId(1)
            .setOptionId(1)
            .setXslName("1")
            .setValueType(MboParameters.ValueType.ENUM)
            .setTypeId(MboParameters.ValueType.ENUM_VALUE)
            .build();
        ModelStorage.ParameterValue value2 = ModelStorage.ParameterValue.newBuilder()
            .setUserId(2)
            .setParamId(2)
            .setOptionId(2)
            .setXslName("2")
            .setValueType(MboParameters.ValueType.ENUM)
            .setTypeId(MboParameters.ValueType.ENUM_VALUE)
            .build();

        ModelStorage.Model model1 = toProto(msku)
            .toBuilder()
            .addParameterValues(value1)
            .addParameterValues(value2)
            .build();
        ModelStorage.Model model2 = toProto(msku)
            .toBuilder()
            .addParameterValues(value2)
            .addParameterValues(value1)
            .build();

        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, convertToNodes(model1));
        executor.execute();

        mockRecent("20200101_0001");
        yt.tables().write(MSKU_PATH, YTableEntryTypes.YSON, convertToNodes(model2));
        executor.execute();
    }

    private void mockRecent(String currentYtId) {
        var recentLink = MSKU_PATH.parent().parent();
        yt.cypress().create(new CreateNode(recentLink, CypressNodeType.MAP)
            .setIgnoreExisting(true));
        yt.cypress().set(recentLink.attribute("key"), currentYtId);
    }
}
