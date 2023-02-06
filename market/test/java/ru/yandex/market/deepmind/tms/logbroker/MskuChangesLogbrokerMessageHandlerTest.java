package ru.yandex.market.deepmind.tms.logbroker;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.MarketSkuMboContentOuterClass;
import Market.DataCamp.MarketSkuOuterClass;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.tms.executors.msku.BaseImportMskuTest;
import ru.yandex.market.mbo.export.ExportReportModels;
import ru.yandex.market.mboc.common.services.modelstorage.models.SimpleModel;

import static org.assertj.core.api.Assertions.assertThat;

public class MskuChangesLogbrokerMessageHandlerTest extends BaseImportMskuTest {
    private MskuChangesLogbrokerMessageHandler handler;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        handler = new MskuChangesLogbrokerMessageHandler(importMskuService, new StorageKeyValueServiceMock());
    }

    @Test
    public void insertUnsupportedTypesTest() {
        var models = List.of(
            createModel(1L, SimpleModel.ModelType.SKU.name()),
            createModel(2L, SimpleModel.ModelType.FAST_SKU.name()),
            createModel(3L, SimpleModel.ModelType.PARTNER_SKU.name()),
            createModel(5L, SimpleModel.ModelType.GURU.name()),
            createModel(6L, SimpleModel.ModelType.PARTNER.name()),
            createModel(7L, "ANY_OTHER_TYPE")
        );
        var messages = models.stream()
            .map(builder -> createMessage(builder, false))
            .collect(Collectors.toList());
        handler.process(messages);
        var actual = deepmindMskuRepository.findAll();
        assertThat(actual).hasSize(3);
    }

    @Test
    public void insertAndUpdateFastSku() {
        var fastModel = createFastSkuModel(1L);
        var message = createMessage(fastModel, false);
        handler.process(List.of(message));

        clearQueue();

        var fastModelUpdated = createFastSkuModel(1L)
            .setTitles(0, createLocalizedString("new name"));
        var message2 = createMessage(fastModelUpdated, false);
        handler.process(List.of(message2));

        var actual = deepmindMskuRepository.findAll();
        assertThat(actual).hasSize(1);
        var updatedMsku = actual.get(0);
        assertThat(updatedMsku).extracting(Msku::getId).isEqualTo(1L);
        assertThat(updatedMsku).extracting(Msku::getSkuType).isEqualTo(SkuTypeEnum.FAST_SKU);
        assertThat(updatedMsku).extracting(Msku::getTitle).isEqualTo("new name");
        assertMskuStatus(Pair.of(updatedMsku.getId(), MskuStatusValue.EMPTY));
        assertTaskQueue("SYNC_WITH_LOGBROKER", 1L);
    }

    @Test
    public void multipleUpdatesInASingleBatch() {
        var msku1 = msku(1L, 10L, "Msku 1");
        var modelFirstUpdate = createSkuModel(1L)
            .addTitles(createLocalizedString("new title"))
            .setModifiedTs(Instant.now().plusMillis(1000).toEpochMilli());
        var modelSecondUpdate = createSkuModel(1L)
            .addTitles(createLocalizedString("newest title"))
            .setModifiedTs(Instant.now().plusMillis(2000).toEpochMilli());

        deepmindMskuRepository.save(msku1);
        var message1 = createMessage(modelFirstUpdate, false);
        var message2 = createMessage(modelSecondUpdate, false);

        handler.process(List.of(message1, message2));

        var actual = deepmindMskuRepository.findAll();
        assertThat(actual).hasSize(1);
        var updatedMsku = actual.get(0);
        assertThat(updatedMsku).extracting(Msku::getId).isEqualTo(1L);
        assertThat(updatedMsku).extracting(Msku::getTitle).isEqualTo("newest title");
        assertMskuStatus(Pair.of(updatedMsku.getId(), MskuStatusValue.EMPTY));
        assertTaskQueue("SYNC_WITH_LOGBROKER", 1L);
    }

    @Test
    public void restoreDeletedMsku() {
        var msku1 = msku(1L, 10L, "Msku 1");
        msku1.setDeleted(true);
        deepmindMskuRepository.save(msku1);

        var model = createSkuModel(1L);
        model.setDeleted(false);
        var message = createMessage(model, false);

        handler.process(List.of(message));

        var actual = deepmindMskuRepository.findAll();
        assertThat(actual).hasSize(1);
        var updatedMsku = actual.get(0);
        assertThat(updatedMsku).extracting(Msku::getId).isEqualTo(1L);
        assertThat(updatedMsku).extracting(Msku::getDeleted).isEqualTo(false);
        assertMskuStatus(Pair.of(1L, MskuStatusValue.EMPTY));
        assertTaskQueue("SYNC_WITH_LOGBROKER", 1L);
    }

    @Test
    public void dontCreateExperimentalSkus() {
        var model = createSkuModel(1L)
            .setCurrentType("EXPERIMENTAL_SKU")
            .setRelations(0, createParent(101L, ExportReportModels.RelationType.EXPERIMENTAL_BASE_MODEL));
        var message = createMessage(model, false);
        handler.process(List.of(message));
        assertThat(deepmindMskuRepository.findAll()).isEmpty();
        assertNoMskuStatus(1L);
        assertTaskQueue("SYNC_WITH_LOGBROKER");
    }

    @Test
    public void deleteNotExistingInDbNotCausesException() {
        var model = createSkuModel(1L);
        var message = createMessage(model, true);
        handler.process(List.of(message));
        assertThat(deepmindMskuRepository.findAll()).isEmpty();
        assertNoMskuStatus(1L);
        assertTaskQueue("SYNC_WITH_LOGBROKER");
    }

    @Test
    public void onDeleteMarkDeleted() {
        var msku1 = msku(1L, 10L, "Msku 1");
        var msku2 = msku(2L, 10L, "Msku 2");
        deepmindMskuRepository.save(List.of(msku1, msku2));

        var model = createSkuModel(msku2.getId());
        var message = createMessage(model, true);
        handler.process(List.of(message));
        var actual = deepmindMskuRepository.findAll();
        assertMsku(actual, msku1, msku2.setDeleted(true));
        assertNoMskuStatus(1L, 2L);
        assertTaskQueue("SYNC_WITH_LOGBROKER", 2L);
    }

    @Test
    public void onInsertCreateNew() {
        var model = createSkuModel(1L);
        var message = createMessage(model, false);

        handler.process(List.of(message));

        var actual = deepmindMskuRepository.findAll();
        assertThat(actual).hasSize(1);
        Msku createdMsku = actual.get(0);
        assertThat(createdMsku).extracting(Msku::getId).isEqualTo(1L);
        assertMskuStatus(Pair.of(createdMsku.getId(), MskuStatusValue.EMPTY));
        assertTaskQueue("SYNC_WITH_LOGBROKER", 1L);
    }

    @Test // DEEPMIND-1040
    public void onInsertCreateNewButWithExistingMskuStatus() {
        var model = createSkuModel(1L);
        var message = createMessage(model, false);

        mskuStatusRepository.save(new MskuStatus()
            .setMarketSkuId(model.getId())
            .setMskuStatus(MskuStatusValue.REGULAR));

        handler.process(List.of(message));

        var actual = deepmindMskuRepository.findAll();
        assertThat(actual).hasSize(1);
        Msku createdMsku = actual.get(0);
        assertThat(createdMsku).extracting(Msku::getId).isEqualTo(1L);
        assertMskuStatus(Pair.of(createdMsku.getId(), MskuStatusValue.REGULAR));
        assertTaskQueue("SYNC_WITH_LOGBROKER", 1L);
    }

    @Test
    public void onUpdateUpdateExisting() {
        var msku1 = msku(1L, 10L, "Msku 1");
        deepmindMskuRepository.save(msku1);

        var model = createSkuModel(msku1.getId())
            .addTitles(createLocalizedString("new title"));
        var message = createMessage(model, false);
        handler.process(List.of(message));

        var actual = deepmindMskuRepository.findAll();
        assertThat(actual).hasSize(1);
        var updatedMsku = actual.get(0);
        assertThat(updatedMsku).extracting(Msku::getId).isEqualTo(1L);
        assertThat(updatedMsku).extracting(Msku::getTitle).isEqualTo("new title");
        assertMskuStatus(Pair.of(1L, MskuStatusValue.EMPTY));
        assertTaskQueue("SYNC_WITH_LOGBROKER", 1L);
    }

    private ExportReportModels.LocalizedString.Builder createLocalizedString(String value) {
        return ExportReportModels.LocalizedString.newBuilder()
            .setValue(value)
            .setIsoCode("ru");
    }

    private DatacampMessageOuterClass.DatacampMessage createMessage(ExportReportModels.ExportReportModel.Builder model,
                                                                    boolean deleted) {
        DataCampOfferMeta.UpdateMeta.Builder meta = createMeta(deleted);
        return DatacampMessageOuterClass.DatacampMessage.newBuilder()
            .setMarketSkus(
                MarketSkuOuterClass.MarketSkuBatch.newBuilder()
                    .addMsku(
                        MarketSkuOuterClass.MarketSku.newBuilder()
                            .setMboContent(
                                MarketSkuMboContentOuterClass.MarketSkuMboContent.newBuilder()
                                    .setMeta(meta)
                                    .setMsku(model)
                            )
                    )
            ).build();
    }

    private DataCampOfferMeta.UpdateMeta.Builder createMeta(boolean deleted) {
        return DataCampOfferMeta.UpdateMeta.newBuilder()
            .setRemoved(deleted);
    }

    private ExportReportModels.ExportReportModel.Builder createModel(Long modelId, String currentType) {
        return ExportReportModels.ExportReportModel.newBuilder()
            .setId(modelId)
            .setCurrentType(currentType)
            .addRelations(createParent(modelId + 100, ExportReportModels.RelationType.SKU_PARENT_MODEL))
            .setModifiedTs(Instant.now().toEpochMilli());
    }

    private ExportReportModels.ExportReportModel.Builder createSkuModel(Long marketSkuId) {
        return createModel(marketSkuId, SkuTypeEnum.SKU.getLiteral())
            .addRelations(createParent(marketSkuId + 100, ExportReportModels.RelationType.SKU_PARENT_MODEL));
    }

    private ExportReportModels.ExportReportModel.Builder createFastSkuModel(Long marketSkuId) {
        return ExportReportModels.ExportReportModel.newBuilder()
            .setId(marketSkuId)
            .addTitles(createLocalizedString("fast title"))
            .addDescriptions(createLocalizedString("fast description"))
            .setCurrentType("FAST_SKU");
    }

    private ExportReportModels.Relation.Builder createParent(Long parentId,
                                                             ExportReportModels.RelationType skuParentModel) {
        return ExportReportModels.Relation.newBuilder()
            .setId(parentId)
            .setType(skuParentModel)
            .setCategoryId(0L);
    }
}
