package ru.yandex.market.mboc.common.services.msku.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.CargoTypeSnapshot;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.msku.KnownMboParams;
import ru.yandex.market.mboc.common.offers.repository.CargoTypeSnapshotRepository;
import ru.yandex.market.mboc.common.queue.QueueItem;
import ru.yandex.market.mboc.common.queue.QueueService;
import ru.yandex.market.mboc.common.services.cargo_type.CargoTypeCachingService;
import ru.yandex.market.mboc.common.services.cargo_type.CargoTypeCachingServiceImpl;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.msku.convertion.MskuToModelConverter;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public abstract class BaseImportMskuServiceTest extends BaseDbTestClass {
    protected static final int SEED = 4324;
    protected static final int BATCH_SIZE = 10;

    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    protected MskuRepository mskuRepository;
    @Autowired
    protected StorageKeyValueService storageKeyValueService;
    @Autowired
    protected TransactionHelper transactionHelper;
    @Autowired
    protected CargoTypeSnapshotRepository cargoTypeSnapshotRepository;
    @Autowired
    @Qualifier("mappedMskuChangesQueueService")
    protected QueueService<QueueItem> mappedMskuChangesQueueService;

    protected MboModelsServiceMock mboModelsService;
    protected CargoTypeCachingService cargoTypeCachingService;
    protected YtImportOperationsMock ytImportOperations;

    protected ImportMskuService importMskuService;
    protected List<Model> stuffModels;
    protected EnhancedRandom random;
    protected MskuConverter mskuConverter;

    @Before
    public void setUp() {
        stuffModels = new ArrayList<>();
        random = TestDataUtils.defaultRandom(SEED);

        cargoTypeSnapshotRepository.save(
            cargoTypeSnapshot(10L, 1L),
            cargoTypeSnapshot(20L, 2L)
        );
        Set<String> modelParamsToKeep = Set.of(KnownMboParams.EXPIR_DATE.mboXslName());

        ytImportOperations = new YtImportOperationsMock(storageKeyValueService);

        cargoTypeCachingService = new CargoTypeCachingServiceImpl(cargoTypeSnapshotRepository);
        mboModelsService = new MboModelsServiceMock();
        mskuConverter = new MskuConverter(cargoTypeCachingService, modelParamsToKeep);
        importMskuService = new ImportMskuService(namedParameterJdbcTemplate, transactionHelper,
            storageKeyValueService, modelParamsToKeep, cargoTypeCachingService, mboModelsService,
            mappedMskuChangesQueueService, ytImportOperations, mskuConverter);
        importMskuService.setBatchSize(BATCH_SIZE);
    }

    protected CargoTypeSnapshot cargoTypeSnapshot(long id, long mboParameter) {
        return new CargoTypeSnapshot().setId(id).setDescription("cargo-type#" + id).setMboParameterId(mboParameter);
    }

    protected Model nexModel() {
        Model model = random.nextObject(Model.class);
        return model.setTitle("title#" + model.getId())
            .setModelType(Model.ModelType.SKU)
            .setModelQuality(Model.ModelQuality.OPERATOR)
            .setDeleted(false);
    }

    protected List<Model> getAllMskus() {
        List<Msku> all = mskuRepository.findAll();
        return all.stream().map(MskuToModelConverter::convert).collect(Collectors.toList());
    }

    protected void assertMsku(List<Msku> actual, Msku... expected) {
        assertThat(actual)
            .usingElementComparatorIgnoringFields(
                "vendorCodes", "barCodes", "cargoTypeLmsIds",
                "parameterValuesProto",
                "ytDataHash", "ytImportTimeout", "ytImportTs"
            )
            .containsExactlyInAnyOrder(expected);
        var expectedMap = Stream.of(expected).collect(Collectors.toMap(
            Msku::getMarketSkuId,
            Function.identity()
        ));
        assertThat(actual).allSatisfy(a -> {
            var e = expectedMap.get(a.getMarketSkuId());
            assertThat(e).isNotNull();
            assertThat(a.getBarCodes()).containsExactlyInAnyOrder(e.getBarCodes());
            assertThat(a.getVendorCodes()).containsExactlyInAnyOrder(e.getVendorCodes());
            assertThat(a.getCargoTypeLmsIds()).containsExactlyInAnyOrder(e.getCargoTypeLmsIds());
        });
    }
}
