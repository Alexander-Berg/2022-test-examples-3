package ru.yandex.market.mboc.common.services.msku.sync;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.MarketSkuMboContentOuterClass;
import Market.DataCamp.MarketSkuOuterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.market.mbo.export.ExportReportModels;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.db.jooq.generated.msku.enums.SkuQualityEnum;
import ru.yandex.market.mboc.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.mboc.common.queue.QueueItem;
import ru.yandex.market.mboc.common.queue.QueueService;
import ru.yandex.market.mboc.common.repo.bindings.pojos.MskuParameters;
import ru.yandex.market.mboc.common.services.modelstorage.models.SimpleModel;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.services.msku.sync.MskuLogbrokerHandler.INSERT_MSKU;
import static ru.yandex.market.mboc.common.services.msku.sync.MskuLogbrokerHandler.generateParametersMap;

public class MskuLogbrokerHandlerTest extends BaseImportMskuServiceTest {

    private MskuLogbrokerHandler mskuLogbrokerHandler;
    @Mock
    private QueueService<QueueItem> mappedMskuChangedQueueMock;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        mskuLogbrokerHandler = new MskuLogbrokerHandler(
            namedParameterJdbcTemplate,
            mskuConverter,
            mappedMskuChangedQueueMock,
            transactionHelper,
            true
        );
    }

    @Test
    public void insertUnsupportedTypesTest() {
        List<ExportReportModels.ExportReportModel.Builder> models = List.of(
            createModel(1L, SimpleModel.ModelType.SKU.name()),
            createModel(2L, SimpleModel.ModelType.FAST_SKU.name()),
            createModel(3L, SimpleModel.ModelType.PARTNER_SKU.name()),
            createModel(5L, SimpleModel.ModelType.GURU.name()),
            createModel(6L, SimpleModel.ModelType.PARTNER.name()),
            createModel(7L, "ANY_OTHER_TYPE")
        );
        List<DatacampMessageOuterClass.DatacampMessage> messages =
            models.stream().map(builder -> createMessage(builder, false)).collect(Collectors.toList());
        mskuLogbrokerHandler.process(messages);
        List<Msku> actual = mskuRepository.findAll();
        assertThat(actual).hasSize(3);
    }

    @Test
    public void insertAndUpdateFastSku() {
        ExportReportModels.ExportReportModel.Builder fastModel = createFastSkuModel(1L);
        DatacampMessageOuterClass.DatacampMessage message = createMessage(fastModel, false);
        mskuLogbrokerHandler.process(Collections.singletonList(message));

        ExportReportModels.ExportReportModel.Builder fastModelUpdated = createFastSkuModel(1L)
            .setTitles(0, createLocalizedString("new name"));
        DatacampMessageOuterClass.DatacampMessage message2 = createMessage(fastModelUpdated, false);
        mskuLogbrokerHandler.process(Collections.singletonList(message2));

        List<Msku> actual = mskuRepository.findAll();
        assertThat(actual).hasSize(1);
        Msku updatedMsku = actual.get(0);
        assertThat(updatedMsku).extracting(Msku::getMarketSkuId).isEqualTo(1L);
        assertThat(updatedMsku).extracting(Msku::getSkuType).isEqualTo(SkuTypeEnum.FAST_SKU);
        assertThat(updatedMsku).extracting(Msku::getTitle).isEqualTo("new name");
    }

    @Test
    public void multipleUpdatesInASingleBatch() {
        Msku msku1 = randomMskuWithId(1L);
        ExportReportModels.ExportReportModel.Builder modelFirstUpdate = createSkuModel(1L)
            .addTitles(createLocalizedString("new title"));
        ExportReportModels.ExportReportModel.Builder modelSecondUpdate = createSkuModel(1L)
            .addTitles(createLocalizedString("newest title"));
        namedParameterJdbcTemplate.batchUpdate(INSERT_MSKU, generateParametersMap(Collections.singletonList(msku1)));
        DatacampMessageOuterClass.DatacampMessage message1 = createMessage(modelFirstUpdate, false);
        DatacampMessageOuterClass.DatacampMessage message2 = createMessage(modelSecondUpdate, false);

        mskuLogbrokerHandler.process(Arrays.asList(message1, message2));

        List<Msku> actual = mskuRepository.findAll();
        assertThat(actual).hasSize(1);
        Msku updatedMsku = actual.get(0);
        assertThat(updatedMsku).extracting(Msku::getMarketSkuId).isEqualTo(1L);
        assertThat(updatedMsku).extracting(Msku::getTitle).isEqualTo("newest title");
    }

    @Test
    public void restoreDeletedMsku() {
        Msku msku1 = randomMskuWithId(1L);
        msku1.setDeleted(true);
        namedParameterJdbcTemplate.batchUpdate(INSERT_MSKU, generateParametersMap(Collections.singletonList(msku1)));

        ExportReportModels.ExportReportModel.Builder model = createSkuModel(1L);
        model.setDeleted(false);
        DatacampMessageOuterClass.DatacampMessage message = createMessage(model, false);

        mskuLogbrokerHandler.process(Collections.singletonList(message));

        List<Msku> actual = mskuRepository.findAll();
        assertThat(actual).hasSize(1);
        Msku updatedMsku = actual.get(0);
        assertThat(updatedMsku).extracting(Msku::getMarketSkuId).isEqualTo(1L);
        assertThat(updatedMsku).extracting(Msku::getDeleted).isEqualTo(false);
    }

    @Test
    public void dontCreateExperimentalSkus() {
        ExportReportModels.ExportReportModel.Builder model = createSkuModel(1L)
            .setCurrentType("EXPERIMENTAL_SKU")
            .setRelations(0, createParent(101L, ExportReportModels.RelationType.EXPERIMENTAL_BASE_MODEL));
        DatacampMessageOuterClass.DatacampMessage message = createMessage(model, false);
        mskuLogbrokerHandler.process(Collections.singletonList(message));
        assertThat(mskuRepository.findAll()).isEmpty();
    }

    @Test
    public void deleteNotExistingInDbNotCausesException() {
        ExportReportModels.ExportReportModel.Builder model = createSkuModel(1L);
        DatacampMessageOuterClass.DatacampMessage message = createMessage(model, true);
        mskuLogbrokerHandler.process(Collections.singletonList(message));
        assertThat(mskuRepository.findAll()).isEmpty();
    }

    @Test
    public void onDeleteMarkDeleted() {
        Msku msku1 = randomMskuWithId(1L);
        Msku msku2 = randomMskuWithId(2L);
        namedParameterJdbcTemplate.batchUpdate(INSERT_MSKU, generateParametersMap(Arrays.asList(msku1, msku2)));

        ExportReportModels.ExportReportModel.Builder model = createSkuModel(msku2.getMarketSkuId());
        DatacampMessageOuterClass.DatacampMessage message = createMessage(model, true);
        mskuLogbrokerHandler.process(Collections.singletonList(message));
        List<Msku> actual = mskuRepository.findAll();
        assertMsku(actual, msku1, msku2.setDeleted(true));
    }

    @Test
    public void onInsertCreateNew() {
        ExportReportModels.ExportReportModel.Builder model = createSkuModel(1L);
        DatacampMessageOuterClass.DatacampMessage message = createMessage(model, false);

        mskuLogbrokerHandler.process(Collections.singletonList(message));

        List<Msku> actual = mskuRepository.findAll();
        assertThat(actual).hasSize(1);
        Msku createdMsku = actual.get(0);
        assertThat(createdMsku).extracting(Msku::getMarketSkuId).isEqualTo(1L);
    }

    @Test
    public void onUpdateUpdateExisting() {
        Msku msku1 = randomMskuWithId(1L);
        namedParameterJdbcTemplate.batchUpdate(INSERT_MSKU, generateParametersMap(Collections.singletonList(msku1)));

        ExportReportModels.ExportReportModel.Builder model = createSkuModel(msku1.getMarketSkuId())
            .addTitles(createLocalizedString("new title"));
        DatacampMessageOuterClass.DatacampMessage message = createMessage(model, false);
        mskuLogbrokerHandler.process(Collections.singletonList(message));

        List<Msku> actual = mskuRepository.findAll();
        assertThat(actual).hasSize(1);
        Msku updatedMsku = actual.get(0);
        assertThat(updatedMsku).extracting(Msku::getMarketSkuId).isEqualTo(1L);
        assertThat(updatedMsku).extracting(Msku::getTitle).isEqualTo("new title");
    }

    /**
     * Meaningful параметры это categoryId или cargo_type_lms_ids если они не менялись изменения не должны тригерить
     * mskuAvailabilityChangedHandler.registerMskuChanges(...)
     */
    @Test
    public void onUpdateUpdateExistingNotMeaningfulParam() {
        Msku msku1 = randomMskuWithId(1L);
        msku1.setCategoryId(500L);
        MskuParameters mskuParameters = new MskuParameters();
        mskuParameters.setCargoParameters(Map.of());
        msku1.setMskuParameterValues(mskuParameters);
        namedParameterJdbcTemplate.batchUpdate(INSERT_MSKU, generateParametersMap(Collections.singletonList(msku1)));

        ExportReportModels.ExportReportModel.Builder model = createSkuModel(msku1.getMarketSkuId())
            .addTitles(createLocalizedString("name"))
            .setCategoryId(500L);

        DatacampMessageOuterClass.DatacampMessage message = createMessage(model, false);
        mskuLogbrokerHandler.process(Collections.singletonList(message));

        List<Msku> actual = mskuRepository.findAll();
        assertThat(actual).hasSize(1);
        Msku updatedMsku = actual.get(0);
        assertThat(updatedMsku).extracting(Msku::getMarketSkuId).isEqualTo(1L);
        assertThat(updatedMsku).extracting(Msku::getTitle).isEqualTo("name");
    }

    private Msku randomMskuWithId(long id) {
        return random.nextObject(
            Msku.class,
            "marketSkuId", "deleted", "mskuParameterValues", "cargoTypeLmsIds", "parameterValuesProto")
            .setMarketSkuId(id)
            .setSkuType(SkuTypeEnum.SKU)
            .setSkuQuality(SkuQualityEnum.OPERATOR)
            .setCategoryQuality(null)
            .setDeleted(false)
            .setMskuParameterValues(new MskuParameters().setCargoParameters(Map.of()))
            .setCargoTypeLmsIds()
            .setYtImportTimeout(Instant.now())
            .setYtImportTs(Instant.now())
            .setAllowPartnerContent(null);
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
            .addRelations(createParent(modelId + 100, ExportReportModels.RelationType.SKU_PARENT_MODEL));
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
