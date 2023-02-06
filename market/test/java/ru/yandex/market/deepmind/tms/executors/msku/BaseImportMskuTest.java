package ru.yandex.market.deepmind.tms.executors.msku;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.events.MskuAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.MskuAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CargoTypeSnapshot;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.mocks.MboModelsServiceMock;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.services.cargotype.DeepmindCargoTypeCachingServiceMock;
import ru.yandex.market.deepmind.common.services.msku.ImportMskuService;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseImportMskuTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    protected TransactionTemplate transactionTemplate;
    @Autowired
    protected MskuRepository deepmindMskuRepository;
    @Autowired
    protected MskuStatusRepository mskuStatusRepository;

    protected ImportMskuService importMskuService;
    protected DeepmindCargoTypeCachingServiceMock cargoTypeCachingService;
    protected MskuAvailabilityChangedHandler mskuAvailabilityChangedHandler;
    protected MboModelsServiceMock mboModelsServiceMock;

    @Before
    public void setUp() throws Exception {
        cargoTypeCachingService = new DeepmindCargoTypeCachingServiceMock();
        mskuAvailabilityChangedHandler =
            new MskuAvailabilityChangedHandler(changedSskuRepository, taskQueueRegistrator);
        mboModelsServiceMock = new MboModelsServiceMock();
        importMskuService = new ImportMskuService(
            cargoTypeCachingService,
            mskuAvailabilityChangedHandler,
            mskuStatusRepository,
            namedParameterJdbcTemplate,
            transactionTemplate,
            mboModelsServiceMock
        );
    }

    protected CargoTypeSnapshot cargoTypeSnapshot(long id, long mboParameter) {
        return new CargoTypeSnapshot().setId(id).setDescription("cargo-type#" + id).setMboParameterId(mboParameter);
    }

    protected void assertTaskQueue(String type, Long... expected) {
        var tasks = getQueueTasksOfType(MskuAvailabilityChangedTask.class);
        assertThat(tasks)
            .flatExtracting(MskuAvailabilityChangedTask::getMskuIds)
            .containsExactlyInAnyOrder(expected);
        assertThat(tasks)
            .allMatch(v -> v.getAvailabilityReason().equals(type));
    }

    protected void assertMsku(List<Msku> actual, Msku... expected) {
        assertThat(actual)
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(expected);
    }

    @SafeVarargs
    protected final void assertMskuStatus(Pair<Long, MskuStatusValue>... statuses) {
        for (Pair<Long, MskuStatusValue> mskuStatus : statuses) {
            var statusO = mskuStatusRepository.findById(mskuStatus.getKey());
            assertThat(statusO).map(MskuStatus::getMskuStatus).contains(mskuStatus.getValue());
        }
    }

    protected Msku msku(long id, long categoryId, String title) {
        return new Msku()
            .setId(id)
            .setCategoryId(categoryId)
            .setVendorId(1L)
            .setTitle(title)
            .setModifiedTs(Instant.now())
            .setSkuType(SkuTypeEnum.SKU)
            .setDeleted(false)
            .setSkuType(SkuTypeEnum.SKU);
    }

    protected ModelStorage.Model.Builder model(long id, long categoryId, String title) {
        return ModelStorage.Model.newBuilder()
            .setId(id)
            .setCategoryId(categoryId)
            .setVendorId(1)
            .addTitles(ModelStorage.LocalizedString.newBuilder().setIsoCode("ru").setValue(title).build())
            .setCurrentType(SkuTypeEnum.SKU.name())
            .setModifiedTs(Instant.now().toEpochMilli());
    }

    protected YTreeMapNode node(long id, long categoryId, String title) {
        var model = model(id, categoryId, title);
        return node(model.build());
    }

    protected YTreeMapNode node(ModelStorage.Model msku) {
        return YTree.mapBuilder()
            .key("category_id").value(YTree.unsignedLongNode(msku.getCategoryId()))
            .key("vendor_id").value(YTree.unsignedLongNode(msku.getVendorId()))
            .key("model_id").value(YTree.unsignedLongNode(msku.getId()))
            .key("data").value(msku.toByteArray())
            .buildMap();
    }

    protected YTreeMapNode node(Msku msku) {
        ModelStorage.Model protoModel = toProto(msku);
        return node(protoModel);
    }

    protected List<YTreeMapNode> nodes(List<Msku> mskus) {
        return mskus.stream().map(this::node).collect(Collectors.toList());
    }

    protected List<YTreeMapNode> nodes(Msku... mskus) {
        return this.nodes(List.of(mskus));
    }

    protected List<YTreeMapNode> convertToNodes(List<ModelStorage.Model> models) {
        return models.stream().map(this::node).collect(Collectors.toList());
    }

    protected List<YTreeMapNode> convertToNodes(ModelStorage.Model... models) {
        return convertToNodes(List.of(models));
    }

    protected ModelStorage.Model toProto(Msku msku) {
        var cargoToParamId = cargoTypeCachingService.getLmsIdsToMboParameterIdsMap();

        ModelStorage.Model.Builder resultBuilder = ModelStorage.Model.newBuilder();
        resultBuilder.setId(msku.getId());
        resultBuilder.addTitles(ModelStorage.LocalizedString.newBuilder()
            .setValue(msku.getTitle()).setIsoCode("ru"));
        resultBuilder.setCategoryId(msku.getCategoryId());
        resultBuilder.setVendorId(msku.getVendorId());
        resultBuilder.setCurrentType(msku.getSkuType().name());
        resultBuilder.setDeleted(msku.getDeleted());
        resultBuilder.setModifiedTs(msku.getModifiedTs().toEpochMilli());
        resultBuilder.setDeleted(msku.getDeleted());
        var cargoTypes = msku.getCargoTypes();
        if (cargoTypes != null) {
            for (long cargotype : cargoTypes) {
                var paramId = cargoToParamId.get(cargotype);
                if (paramId == null) {
                    throw new IllegalStateException("No paramId for cargotype: " + cargotype);
                }
                resultBuilder.addParameterValues(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(paramId)
                    .setBoolValue(true)
                    .build());
            }
        }
        return resultBuilder.build();
    }

    protected void assertNoMskuStatus(Long... ids) {
        List<MskuStatus> statuses = mskuStatusRepository.findByIds(ids);
        assertThat(statuses).isEmpty();
    }
}
