package ru.yandex.market.ir.pipeline.dcp;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.gutgin.tms.assertions.GutginAssertions;
import ru.yandex.market.gutgin.tms.service.SskuLockService;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.DatacampPipelineSchedulerService;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.OfferInfoBatchProducer;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.util.ModelProtoUtils;
import ru.yandex.market.ir.autogeneration_api.http.service.MboMappingsServiceMock;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.ir.autogeneration_api.http.service.PictureStatus;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.controller.manager.DatacampConverter;
import ru.yandex.market.ir.config.DcpPipelineTestConfig;
import ru.yandex.market.ir.pipeline.BasePipelineTest;
import ru.yandex.market.ir.pipeline.OfferContentProcessingResultsServiceMock;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.content.common.db.dao.PipelineService;
import ru.yandex.market.partner.content.common.db.dao.SskuLockDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.BusinessToLockInfoDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.MrgrienPipelineStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.BusinessToLockInfo;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.db.utils.JsonBinders;
import ru.yandex.market.partner.content.common.partner.content.SourceController;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = DcpPipelineTestConfig.class)
// This can't seem to work out of the box, we catch an exception on the secondary context initialization.
// TODO: Try to make it work
// @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
@TestPropertySource(properties = {"categoryDataRefreshersCount=1000000"})
public class DcpPipelineTest extends BasePipelineTest {
    public static final int SOURCE_ID = 100500;
    public static final int PARTNER_SHOP_ID = 1050;

    private static final long CATEGORY_ID = 90554;

    private static final long PARENT_MODEL_ID = 16;

    private static final long SKU_MODEL_ID_1 = 17;
    private static final String BARCODE_1 = "1211200220009";

    private static final long SKU_MODEL_ID_2 = 18;
    private static final String BARCODE_2 = "1211200210000";
    public static final int SUPPLIER_ID = 10462389;


    @Autowired
    DatacampOfferDao datacampOfferDao;

    @Autowired
    PipelineService pipelineService;

    @Autowired
    SskuLockService sskuLockService;

    @Autowired
    SskuLockDao sskuLockDao;

    @Autowired
    DatacampConverter datacampConverter;

    @Autowired
    ModelStorageServiceMock modelStorageServiceMock;

    @Autowired
    MboMappingsServiceMock mboMappingsServiceMock;

    @Autowired
    CategoryDataKnowledgeMock categoryDataKnowledgeMock;

    @Autowired
    SourceController sourceController;

    @Autowired
    OfferContentProcessingResultsServiceMock offerContentProcessingResultsServiceMock;

    @Autowired
    BusinessToLockInfoDao businessToLockInfoDao;

    @Autowired
    OfferInfoBatchProducer offerInfoBatchProducer;

    private DatacampPipelineSchedulerService datacampPipelineSchedulerService;

    public void setUp1() {
        datacampPipelineSchedulerService = new DatacampPipelineSchedulerService(
                datacampOfferDao,
                pipelineService,
                dataBucketDao,
                gcSkuTicketDao,
                sskuLockService,
                sourceController,
                businessToLockInfoDao,
                offerInfoBatchProducer
        );

        businessToLockInfoDao.insert(new BusinessToLockInfo(
            SUPPLIER_ID, null, 0, 0
        ));

        modelStorageServiceMock.setMissingPictureStatus(picUrl -> new PictureStatus(
                ModelStorage.Picture.newBuilder()
                        .setUrl("http://fake_mbo_avatar.yandex-team.ru?origin=" + picUrl)
                        .setIsWhiteBackground(true)
                        .build(),
                ModelStorage.OperationStatus.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setType(ModelStorage.OperationType.UPLOAD_IMAGE)
                        .build()
        ));

    }

    public void setUpShop() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);
    }

    public void setUpCategory() {
        categoryDataKnowledgeMock.addCategoryData(CATEGORY_ID, CategoryData.build(MboParameters.Category.newBuilder()
                .setLeaf(true)
                .setHid(CATEGORY_ID)
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.NAME_ID).setXslName("name")
                        .setValueType(MboParameters.ValueType.STRING))
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.VENDOR_ID).setXslName("vendor")
                        .setValueType(MboParameters.ValueType.ENUM)
                        .addOption(MboParameters.Option.newBuilder()
                                .addAlias(MboParameters.EnumAlias.newBuilder()
                                        .setAlias(MboParameters.Word.newBuilder()
                                                .setName("ABK").buildPartial()).build())
                                .build())
                        .addName(MboParameters.Word.newBuilder().setLangId(225).setName("производитель")))
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(14871214)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                        .addOption(MboParameters.Option.newBuilder().setId(14899397).addName(MboParameters.Word.newBuilder().setLangId(225).setName("белый")))
                        .addOption(MboParameters.Option.newBuilder().setId(14896458).addName(MboParameters.Word.newBuilder().setLangId(225).setName("бежевый")))
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(13887626)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                        .addOption(MboParameters.Option.newBuilder().setId(13887686).addName(MboParameters.Word.newBuilder().setLangId(225).setName("белый")))
                        .addOption(MboParameters.Option.newBuilder().setId(13887677).addName(MboParameters.Word.newBuilder().setLangId(225).setName("бежевый")))
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(4899886)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                        .addOption(MboParameters.Option.newBuilder().setId(12110527).addName(MboParameters.Word.newBuilder().setLangId(225).setName("AV-процессор")))
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(4899888)
                        .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                        .setValueType(MboParameters.ValueType.NUMERIC)
                )
        ));
    }

    public void setUpModelStorage() {
        modelStorageServiceMock.putModel(ModelStorage.Model.newBuilder()
                .setId(PARENT_MODEL_ID)
                .setCurrentType(ModelStorage.ModelType.GURU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER.name())
                .addRelations(ModelStorage.Relation.newBuilder().setId(SKU_MODEL_ID_1).setType(ModelStorage.RelationType.SKU_MODEL).build())
                .addRelations(ModelStorage.Relation.newBuilder().setId(SKU_MODEL_ID_2).setType(ModelStorage.RelationType.SKU_MODEL).build())
        );

        modelStorageServiceMock.putModel(ModelStorage.Model.newBuilder()
                .setId(SKU_MODEL_ID_1)
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(ParameterValueComposer.BARCODE_ID)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue(BARCODE_1)
                                .build())
                        .build())
                .addRelations(ModelStorage.Relation.newBuilder().setId(PARENT_MODEL_ID).setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
        );

        modelStorageServiceMock.putModel(ModelStorage.Model.newBuilder()
                .setId(SKU_MODEL_ID_2)
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(ParameterValueComposer.BARCODE_ID)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue(BARCODE_2)
                                .build())
                        .build())
                .addRelations(ModelStorage.Relation.newBuilder().setId(PARENT_MODEL_ID).setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
        );
    }

    @Test
    public void testSimple() {
        modelStorageServiceMock.clearModels();
        setUp1();
        setUpShop();
        setUpCategory();

        String offer1Json = DcpTestOfferData.getOffer1String();
        String offer2Json = DcpTestOfferData.getOffer2String();

        DataCampOffer.Offer offer1 = JsonBinders.DataCampOfferOffer.proto().converter().from(offer1Json);
        DataCampOffer.Offer offer2 = JsonBinders.DataCampOfferOffer.proto().converter().from(offer2Json);

        offer1 = overrideOfferId(offer1, "testSimple");
        offer2 = overrideOfferId(offer2, "testSimple");

        addDebugOffers(Arrays.asList(offer1, offer2), new Timestamp(System.currentTimeMillis()));

        String offer1Id = offer1.getIdentifiers().getOfferId();
        String offer2Id = offer2.getIdentifiers().getOfferId();

        datacampPipelineSchedulerService.schedulePipelines(SUPPLIER_ID, Optional.empty());

        long pipelineId = getLastPipelineId(PipelineType.CSKU);
        runPipeline(pipelineId);

        Pipeline pipeline = getPipeline(pipelineId);
        GutginAssertions.assertThat(pipeline).hasStatus(MrgrienPipelineStatus.FINISHED);
        assertAllTasksFinished(pipeline.getId());

        DataCampOfferContent.OfferContent offer1Content = offerContentProcessingResultsServiceMock
            .getOffers().get(Pair.of(SUPPLIER_ID, offer1Id));
        DataCampOfferContent.OfferContent offer2Content = offerContentProcessingResultsServiceMock
            .getOffers().get(Pair.of(SUPPLIER_ID, offer2Id));

        //мы выбросили тикеты, для которых нет офферов
        assertThat(offer1Content).isNull();
        assertThat(offer2Content).isNull();;
    }

    @Test
    public void testSimpleWithOffers() {
        modelStorageServiceMock.clearModels();
        setUp1();
        setUpShop();
        setUpCategory();
        setUpModelStorage();

        String offer1Json = DcpTestOfferData.getOffer1String();
        String offer2Json = DcpTestOfferData.getOffer2String();

        DataCampOffer.Offer offer1 = JsonBinders.DataCampOfferOffer.proto().converter().from(offer1Json);
        DataCampOffer.Offer offer2 = JsonBinders.DataCampOfferOffer.proto().converter().from(offer2Json);

        offer1 = overrideOfferId(offer1, "test2");
        offer2 = overrideOfferId(offer2, "test2");

        addDebugOffers(Arrays.asList(offer1, offer2), new Timestamp(System.currentTimeMillis()));

        String offer1Id = offer1.getIdentifiers().getOfferId();
        String offer2Id = offer2.getIdentifiers().getOfferId();

        mboMappingsServiceMock.addMapping(CATEGORY_ID,
            SUPPLIER_ID,
            offer1Id,
            SKU_MODEL_ID_1,
            SupplierOffer.ApprovedMappingConfidence.MAPPING_PARTNER_SELF);

        mboMappingsServiceMock.addMapping(CATEGORY_ID,
            SUPPLIER_ID,
            offer2Id,
            SKU_MODEL_ID_2,
            SupplierOffer.ApprovedMappingConfidence.MAPPING_PARTNER_SELF);

        datacampPipelineSchedulerService.schedulePipelines(SUPPLIER_ID, Optional.empty());

        long pipelineId = getLastPipelineId(PipelineType.CSKU);
        runPipeline(pipelineId);

        Pipeline pipeline = getPipeline(pipelineId);
        GutginAssertions.assertThat(pipeline).hasStatus(MrgrienPipelineStatus.FINISHED);
        assertAllTasksFinished(pipeline.getId());

        DataCampOfferContent.OfferContent offer1Content = offerContentProcessingResultsServiceMock.getOffers().get(Pair.of(SUPPLIER_ID, offer1Id));
        DataCampOfferContent.OfferContent offer2Content = offerContentProcessingResultsServiceMock.getOffers().get(Pair.of(SUPPLIER_ID, offer2Id));

        assertThat(offer1Content).isNotNull();
        assertThat(offer2Content).isNotNull();
        // The offers are saved into pskus alright.
        long savedPskuId1 = offer1Content.getBinding().getPartner().getMarketSkuId();
        long savedPskuId2 = offer2Content.getBinding().getPartner().getMarketSkuId();

        assertThat(savedPskuId1).isNotEqualTo(0);
        assertThat(savedPskuId2).isNotEqualTo(0);
        assertThat(offer1Content.getBinding().getPartner().getMeta().getSource())
            .isEqualTo(DataCampOfferMeta.DataSource.MARKET_DATACAMP);
        assertThat(offer2Content.getBinding().getPartner().getMeta().getSource())
            .isEqualTo(DataCampOfferMeta.DataSource.MARKET_DATACAMP);

        // No problems are reported back to MBOC.
        assertThat(offer1Content.getPartner().getMarketSpecificContent().getProcessingResponse().getResult()).isEqualTo(
            DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_OK
        );
        assertThat(offer2Content.getPartner().getMarketSpecificContent().getProcessingResponse().getResult()).isEqualTo(
            DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_OK
        );

        // PSKUs got created.
        ModelStorage.Model pskuInStorage1 = modelStorageServiceMock.getModelsMap().get(savedPskuId1);
        ModelStorage.Model pskuInStorage2 = modelStorageServiceMock.getModelsMap().get(savedPskuId2);
        assertThat(pskuInStorage1.getDeleted()).isFalse();
        assertThat(pskuInStorage2.getDeleted()).isFalse();

        // They have the same parent model.
        assertThat(
            Stream.of(pskuInStorage1, pskuInStorage2)
                .map(ModelProtoUtils::getParentModelIdOrFail)
                .collect(Collectors.toSet())
        ).hasSize(1);
    }

    @NotNull
    private DataCampOffer.Offer overrideOfferId(DataCampOffer.Offer offer, String suffix) {
        DataCampOffer.Offer.Builder builder = offer.toBuilder();
        return builder.setIdentifiers(
            builder
                .getIdentifiersBuilder()
                .setOfferId(offer.getIdentifiers().getOfferId() + suffix)
                .build()
        ).build();
    }

    @Test
    @Ignore("Need to skip CW")
    public void testReupload() {

        setUp1();
        setUpShop();
        setUpCategory();
        setUpModelStorage();

        String offer1Json = DcpTestOfferData.getOffer1String();
        String offer2Json = DcpTestOfferData.getOffer2String();

        DataCampOffer.Offer offer1 = JsonBinders.DataCampOfferOffer.proto().converter().from(offer1Json);
        DataCampOffer.Offer offer2 = JsonBinders.DataCampOfferOffer.proto().converter().from(offer2Json);

        addDebugOffers(Arrays.asList(offer1, offer2), new Timestamp(System.currentTimeMillis()));

        String offer1Id = offer1.getIdentifiers().getOfferId();
        String offer2Id = offer2.getIdentifiers().getOfferId();

        mboMappingsServiceMock.addMapping(CATEGORY_ID,
                SUPPLIER_ID,
                offer1Id,
                SKU_MODEL_ID_1,
                SupplierOffer.ApprovedMappingConfidence.MAPPING_PARTNER_SELF);

        mboMappingsServiceMock.addMapping(CATEGORY_ID,
                SUPPLIER_ID,
                offer2Id,
                SKU_MODEL_ID_2,
                SupplierOffer.ApprovedMappingConfidence.MAPPING_PARTNER_SELF);

        datacampPipelineSchedulerService.schedulePipelines(SUPPLIER_ID, Optional.empty());

        long pipelineId = getLastPipelineId(PipelineType.CSKU);
        runPipeline(pipelineId);

        Pipeline pipeline = getPipeline(pipelineId);
        GutginAssertions.assertThat(pipeline).hasStatus(MrgrienPipelineStatus.FINISHED);
        assertAllTasksFinished(pipeline.getId());

        // PSKUs are still there.
        ModelStorage.Model pskuInStorage1 = modelStorageServiceMock.getModelsMap().get(SKU_MODEL_ID_1);
        ModelStorage.Model pskuInStorage2 = modelStorageServiceMock.getModelsMap().get(SKU_MODEL_ID_2);
        assertThat(pskuInStorage1.getDeleted()).isFalse();
        assertThat(pskuInStorage2.getDeleted()).isFalse();

        // They have the same parent model.
        assertThat(
                Stream.of(pskuInStorage1, pskuInStorage2)
                        .map(ModelProtoUtils::getParentModelIdOrFail)
                        .collect(Collectors.toSet())
        ).hasSize(1);

        DataCampOfferContent.OfferContent offer1Content = offerContentProcessingResultsServiceMock.getOffers().get(Pair.of(SUPPLIER_ID, offer1Id));
        DataCampOfferContent.OfferContent offer2Content = offerContentProcessingResultsServiceMock.getOffers().get(Pair.of(SUPPLIER_ID, offer2Id));

        // Mappings are sent back to MBOC.
        assertThat(offer1Content.getBinding().getPartner().getMarketSkuId()).isEqualTo(SKU_MODEL_ID_1);
        assertThat(offer2Content.getBinding().getPartner().getMarketSkuId()).isEqualTo(SKU_MODEL_ID_2);

        // No problems are reported back to MBOC.
        assertThat(offer1Content.getPartner().getMarketSpecificContent().getProcessingResponse().getResult()).isEqualTo(
                DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_OK
        );
        assertThat(offer2Content.getPartner().getMarketSpecificContent().getProcessingResponse().getResult()).isEqualTo(
                DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_OK
        );
    }

    private void addDebugOffers(List<DataCampOffer.Offer> offerList, Timestamp requestTs1) {
        try {
            List<DatacampOffer> datacampOffers =
                    datacampConverter.convertToJooqPojo(
                            offerList,
                            requestTs1,
                            datacampOfferDao.now()
                    );
            datacampOfferDao.insert(datacampOffers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
