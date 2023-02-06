package ru.yandex.market.gutgin.tms.pipeline.csku;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import com.google.common.collect.HashMultimap;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku.CSKUDataPreparation;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku.TicketWrapper;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.LocalizedStringUtils;
import ru.yandex.market.markup3.api.Markup3IntegrationsApi.AddSkuParametersConflictRequest;
import ru.yandex.market.markup3.api.Markup3IntegrationsApi.AddSkuParametersConflictRequest.ConflictParameter;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.csku.KnownParameters;
import ru.yandex.market.partner.content.common.csku.ModelGenerator;
import ru.yandex.market.partner.content.common.csku.OfferParameterType;
import ru.yandex.market.partner.content.common.csku.OffersGenerator;
import ru.yandex.market.partner.content.common.csku.SimplifiedOfferParameter;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.csku.judge.ModelData;
import ru.yandex.market.partner.content.common.csku.util.ParameterCreator;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ir.autogeneration.common.helpers.ModelBuilderHelper.createDefaultModelBuilder;
import static ru.yandex.market.ir.autogeneration.common.helpers.ModelBuilderHelper.idForModel;

public class ExtractConflictInfoTaskActionTest extends DBDcpStateGenerator {
    private ExtractConflictInfoTaskAction extractConflictInfoTaskAction;
    private final CategoryData categoryData = mock(CategoryData.class);
    GcSkuValidationDao gcSkuValidationDao = mock(GcSkuValidationDao.class);
    private static final Long PARAM_ID_1 = 1L;
    private static final Long PARAM_ID_2 = 2L;
    private static final Long PARAM_ID_3 = 3L;
    private static final Long PARAM_ID_4 = 4L;
    private static final Long PARAM_ID_6 = 6L;
    private static final String PARAM_NAME_1 = "new";
    private static final String PARAM_NAME_2 = "new2";
    private static final String PARAM_NAME_3 = "new3";
    private static final String PARAM_NAME_4 = "new4";
    private static final String PARAM_NAME_6 = "new6";
    private static final String VALUE_1 = "Some value";
    private static final String VALUE_2 = "Some value2";
    private static final boolean VALUE_3 = true;
    private static final String VALUE_4 = "222";
    private static final String VALUE_5_1 = "multivalue_1";
    private static final String VALUE_5_2 = "08765432109876";
    private static final String VALUE_5_3 = "0876532109876";
    private static final Long VALUE_6 = 1L;
    private static final String SHOP_SKU_VALUE = "Shop sku";
    private static final String EXISTING_BARCODE = "087653212776";
    private static final int SUPPLIER_ID = 123;
    private static final Integer GROUP_ID = 14567;
    private static final long CATEGORY_ID = 123L;
    private static final long NEW_CATEGORY_ID = 12345L;
    private final static Long TICKET_ID = 1L;
    private DataCampOffer.Offer.Builder offerBuilder;
    private DataCampOffer.Offer offer;
    private CSKUDataPreparation cskuDataPreparation;
    private static final List<SimplifiedOfferParameter> params = Arrays.asList(
            SimplifiedOfferParameter.forOffer(PARAM_ID_1, PARAM_NAME_1, VALUE_1, OfferParameterType.STRING)
    );

    @Before
    public void setUp() {
        super.setUp();
        offerBuilder = OffersGenerator.generateOfferBuilder(params);
        when(categoryData.containsParam(PARAM_ID_1)).thenReturn(true);
        when(categoryData.containsParam(PARAM_ID_2)).thenReturn(true);
        when(categoryData.containsParam(PARAM_ID_3)).thenReturn(true);
        when(categoryData.containsParam(PARAM_ID_4)).thenReturn(true);
        when(categoryData.containsParam(PARAM_ID_6)).thenReturn(true);
        when(categoryData.containsParam(ParameterValueComposer.BARCODE_ID)).thenReturn(true);
        when(categoryData.isSkuParameter(PARAM_ID_1)).thenReturn(true);
        when(categoryData.isSkuParameter(PARAM_ID_2)).thenReturn(true);
        when(categoryData.isSkuParameter(PARAM_ID_3)).thenReturn(true);
        when(categoryData.isSkuParameter(PARAM_ID_4)).thenReturn(true);
        when(categoryData.isSkuParameter(PARAM_ID_6)).thenReturn(true);
        when(categoryData.isSkuParameter(ParameterValueComposer.BARCODE_ID)).thenReturn(true);
        when(categoryData.getParamById(PARAM_ID_1)).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(PARAM_ID_1)
                .setValueType(MboParameters.ValueType.STRING)
                .setXslName(PARAM_NAME_1)
                .build());
        when(categoryData.getParamById(PARAM_ID_2)).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(PARAM_ID_2)
                .setValueType(MboParameters.ValueType.STRING)
                .setXslName(PARAM_NAME_2)
                .build());
        when(categoryData.getParamById(PARAM_ID_3)).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(PARAM_ID_3)
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setXslName(PARAM_NAME_3)
                .addOption(0, MboParameters.Option.newBuilder().setFilterValue(false)
                        .addName(0, MboParameters.Word.newBuilder().setName("false")
                                .build())
                        .build())
                .addOption(1, MboParameters.Option.newBuilder().setFilterValue(true)
                        .addName(0, MboParameters.Word.newBuilder().setName("true")
                                .build())
                        .build())
                .build());
        when(categoryData.getParamById(PARAM_ID_4)).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(PARAM_ID_4)
                .setValueType(MboParameters.ValueType.NUMERIC)
                .setXslName(PARAM_NAME_4)
                .build());
        when(categoryData.getParamById(PARAM_ID_6)).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(PARAM_ID_6)
                .setValueType(MboParameters.ValueType.ENUM)
                .setXslName(PARAM_NAME_6)
                .addOption(0, MboParameters.Option.newBuilder().setFilterValue(true)
                        .setId(0L)
                        .addName(0, MboParameters.Word.newBuilder().setName("Zero")
                                .build())
                        .build())
                .addOption(1, MboParameters.Option.newBuilder().setFilterValue(true)
                        .setId(1L)
                        .addName(0, MboParameters.Word.newBuilder().setName("One")
                                .build())
                        .build())
                .build());
        when(categoryData.getParamById(ParameterValueComposer.BARCODE_ID)).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.BARCODE_ID)
                .setValueType(MboParameters.ValueType.STRING)
                .setMultivalue(true)
                .setXslName(CategoryData.BAR_CODE)
                .build());

        HashMultimap<String, Long> existingBarcodes = HashMultimap.create();
        existingBarcodes.put(EXISTING_BARCODE, 10000L);
        ModelStorageHelper modelStorageHelper = mock(ModelStorageHelper.class);
        when(modelStorageHelper.getModelIdsByBarcodes(any())).thenReturn(existingBarcodes);

        GcSkuTicketDao gcSkuTicketDao = mock(GcSkuTicketDao.class);
        Judge judge = new Judge();
        cskuDataPreparation = mock(CSKUDataPreparation.class);
        CategoryDataHelper categoryDataHelper = mock(CategoryDataHelper.class);
        when(categoryDataHelper.getCategoryData(anyLong())).thenReturn(categoryData);
        extractConflictInfoTaskAction = new ExtractConflictInfoTaskAction(gcSkuTicketDao, gcSkuValidationDao, judge,
                cskuDataPreparation, categoryDataHelper, null, modelStorageHelper);
    }

    @Test
    public void whenConflictsCheckConflictsInfoSent() {
        generateOffer();
        ModelStorage.Model sku = getModel();
        ModelData skuData = new ModelData(sku, true, SHOP_SKU_VALUE);

        GcSkuTicket ticket = new GcSkuTicket();
        ticket.setCategoryId(CATEGORY_ID);
        ticket.setId(TICKET_ID);
        ticket.setPartnerShopId(SUPPLIER_ID);
        ticket.setDatacampOffer(offer);
        ticket.setShopSku("123");

        long parentModelId = idForModel(ticket.getId());
        TicketWrapper ticketWrapper = new TicketWrapper(sku,
                createDefaultModelBuilder(parentModelId, ticket.getPartnerShopId()).build(),
                true, ticket);
        AddSkuParametersConflictRequest request =
                extractConflictInfoTaskAction.getAddSkuAndModelParametersConflictRequest(List.of(ticketWrapper));
        assertThat(request).isNotNull();
        assertThat(request.getConflictParametersCount()).isEqualTo(3);
        List<ConflictParameter> conflictParametersList = request.getConflictParametersList();

        conflictParametersList.forEach(conflictParameter -> {
            assertThat(conflictParameter.getMappingSkuId().getValue()).isEqualTo(sku.getId());
            assertThat(conflictParameter.getModelId()).isEqualTo(parentModelId);
        });
        ConflictParameter booleanParam = conflictParametersList
                .stream()
                .filter(param -> param.getParameterId() == PARAM_ID_3)
                .findFirst().get();

        ConflictParameter numericParam = conflictParametersList
                .stream()
                .filter(param -> param.getParameterId() == PARAM_ID_4)
                .findFirst().get();

        ConflictParameter optionParam = conflictParametersList
                .stream()
                .filter(param -> param.getParameterId() == PARAM_ID_6)
                .findFirst().get();

        assertThat(numericParam.getParameterId()).isEqualTo(PARAM_ID_4);
        assertThat(numericParam.getBusinessId()).isEqualTo(SUPPLIER_ID);
        assertThat(numericParam.getCategoryId()).isEqualTo(CATEGORY_ID);

        assertThat(numericParam.getConflictCardId().getValue()).isEqualTo(skuData.getModel().getId());
        assertThat(numericParam.getShopSku()).isEqualTo(ticket.getShopSku());
        assertThat(numericParam.getParameterValue(0)).isEqualTo(VALUE_4);

        assertThat(booleanParam.getParameterId()).isEqualTo(PARAM_ID_3);
        assertThat(booleanParam.getParameterValue(0)).isEqualTo(String.valueOf(VALUE_3));

        assertThat(optionParam.getParameterId()).isEqualTo(PARAM_ID_6);
        assertThat(optionParam.getParameterValue(0)).isEqualTo(VALUE_6.toString());
    }

    @Test
    public void whenInvalidParamsCheckExcluded() {
        generateOffer();
        ModelStorage.Model model = getModel();
        when(gcSkuValidationDao.getFailData(TICKET_ID)).thenReturn(Collections.singletonList(
                new FailData(Collections.singletonList(new ParamInfo(PARAM_ID_3, PARAM_NAME_3,
                        false)))));
        GcSkuTicket ticket = new GcSkuTicket();
        ticket.setCategoryId(CATEGORY_ID);
        ticket.setId(TICKET_ID);
        ticket.setPartnerShopId(SUPPLIER_ID);
        ticket.setDatacampOffer(offer);
        ticket.setShopSku("123");

        TicketWrapper ticketWrapper = new TicketWrapper(model,
                createDefaultModelBuilder(idForModel(ticket.getId()), ticket.getPartnerShopId()).build(),
                true, ticket);
        AddSkuParametersConflictRequest request =
                extractConflictInfoTaskAction.getAddSkuAndModelParametersConflictRequest(List.of(ticketWrapper));
        assertThat(request).isNotNull();
        assertThat(request.getConflictParametersCount()).isEqualTo(2);
    }

    @Test
    public void whenMultivalueConflictsCheckConflictsInfoSent() {
        generateMultivalueOffer();
        ModelStorage.Model model = getMultivalueModel();
        ModelData skuData = new ModelData(model, true, SHOP_SKU_VALUE);

        GcSkuTicket ticket = new GcSkuTicket();
        ticket.setCategoryId(CATEGORY_ID);
        ticket.setId(TICKET_ID);
        ticket.setPartnerShopId(SUPPLIER_ID);
        ticket.setDatacampOffer(offer);
        ticket.setShopSku("123");

        TicketWrapper ticketWrapper = new TicketWrapper(model,
                createDefaultModelBuilder(idForModel(ticket.getId()), ticket.getPartnerShopId()).build(),
                true, ticket);
        AddSkuParametersConflictRequest request =
                extractConflictInfoTaskAction.getAddSkuAndModelParametersConflictRequest(List.of(ticketWrapper));

        assertThat(request).isNotNull();
        assertThat(request.getConflictParametersCount()).isEqualTo(1);
        Optional<ConflictParameter> multiValueParam = request.getConflictParametersList()
                .stream()
                .filter(param -> param.getParameterId() == ParameterValueComposer.BARCODE_ID)
                .findFirst();

        assertThat(multiValueParam.isPresent()).isTrue();
        assertThat(multiValueParam.get().getParameterValueCount()).isEqualTo(2);
        assertThat(multiValueParam.get().getParameterValueList()).containsExactlyInAnyOrder(VALUE_5_2, VALUE_5_3);
    }

    @Test
    public void whenCategoryIsChangedThenOnlyBarcodeShouldBeChecked() {
        GcSkuTicket ticket = generateDBDcpInitialStateNew(1, datacampOffers -> {
            datacampOffers.get(0).getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(12345))
                    .withStringParam(KnownParameters.BARCODE.getId(), "100")
                    .withStringParam(PARAM_ID_1, "1000")
                    .build();
        }).get(0);
        ModelStorage.Model sku = ModelStorage.Model.newBuilder()
                .setId(10)
                .setSupplierId(ticket.getPartnerShopId() + 1)
                .setCategoryId(CATEGORY_ID)
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(KnownParameters.BARCODE.getId())
                        .addStrValue(LocalizedStringUtils.defaultString("101"))
                        .build())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(PARAM_ID_1)
                        .addStrValue(LocalizedStringUtils.defaultString("1001"))
                        .build())
                .build();

        TicketWrapper ticketWrapper = new TicketWrapper(sku, getModel(), true, ticket);
        AddSkuParametersConflictRequest request =
                extractConflictInfoTaskAction.getAddSkuAndModelParametersConflictRequest(List.of(ticketWrapper));

        assertThat(request.getConflictParametersCount()).isEqualTo(1);
        assertThat(request.getConflictParametersList()).extracting(ConflictParameter::getParameterId)
                .containsOnly(KnownParameters.BARCODE.getId());
    }

    @Test
    public void whenBarcodeAlreadyExistsNoConflict() {
        GcSkuTicket ticket = generateDBDcpInitialStateNew(1, datacampOffers -> {
            datacampOffers.get(0).getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(12345))
                    .withBarCodes(EXISTING_BARCODE)
                    .withStringParam(PARAM_ID_1, "1000")
                    .build();
        }).get(0);
        ModelStorage.Model sku = ModelStorage.Model.newBuilder()
                .setId(10)
                .setSupplierId(ticket.getPartnerShopId() + 1)
                .setCategoryId(CATEGORY_ID)
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(KnownParameters.BARCODE.getId())
                        .addStrValue(LocalizedStringUtils.defaultString("101"))
                        .build())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(PARAM_ID_1)
                        .addStrValue(LocalizedStringUtils.defaultString("1001"))
                        .build())
                .build();

        TicketWrapper ticketWrapper = new TicketWrapper(sku, getModel(), true, ticket);
        AddSkuParametersConflictRequest request =
                extractConflictInfoTaskAction.getAddSkuAndModelParametersConflictRequest(List.of(ticketWrapper));

        assertThat(request.getConflictParametersList()).isEmpty();
    }

    @Test
    public void skipOffersWithoutMapping() {
        GcSkuTicket ticket = generateDBDcpInitialStateNew(1, datacampOffers -> {
            datacampOffers.get(0).getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(12345))
                    .withStringParam(KnownParameters.BARCODE.getId(), "100")
                    .withStringParam(PARAM_ID_1, "1000")
                    .build();
        }).get(0);
        TicketWrapper ticketWrapper = new TicketWrapper(null, null, true, ticket);

        AddSkuParametersConflictRequest request =
                extractConflictInfoTaskAction.getAddSkuAndModelParametersConflictRequest(List.of(ticketWrapper));

        assertThat(request).isNotNull();
        assertThat(request).extracting(AddSkuParametersConflictRequest::getConflictParametersCount).isEqualTo(0);
    }

    @Test
    public void skipOffersWithCategoryChange() {
        //Категория CATEGORY_ID
        ModelStorage.Model sku = getModel();
        GcSkuTicket ticket = generateDBDcpInitialStateNew(1, datacampOffers -> {
            datacampOffers.get(0).getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(NEW_CATEGORY_ID))
                    .withStringParam(KnownParameters.BARCODE.getId(), "100")
                    .withStringParam(PARAM_ID_1, "1000")
                    .build();
        }).get(0);
        ticket.setCategoryId(NEW_CATEGORY_ID);
        TicketWrapper ticketWrapper = new TicketWrapper(sku,
                createDefaultModelBuilder(idForModel(ticket.getId()), ticket.getPartnerShopId()).build(),
                true, ticket);

        when(cskuDataPreparation.collectDataForRequest(List.of(ticket))).thenReturn(List.of(ticketWrapper));
        ProcessTaskResult<ProcessDataBucketData> taskResult =
                extractConflictInfoTaskAction.runOnTickets(List.of(ticket), new ProcessDataBucketData(1L));

        assertThat(taskResult).isNotNull();
        assertThat(taskResult).extracting(ProcessTaskResult::hasProblems).isEqualTo(false);
    }

    @Test
    public void skipOffersWithStrictMode() {
        //Категория CATEGORY_ID
        ModelStorage.Model sku = getModel().toBuilder().setStrictChecksRequired(true).build();
        GcSkuTicket ticket = generateDBDcpInitialStateNew(1, datacampOffers -> {
            datacampOffers.get(0).getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(CATEGORY_ID))
                    .withStringParam(KnownParameters.BARCODE.getId(), "100")
                    .withStringParam(PARAM_ID_1, "1000")
                    .build();
        }).get(0);
        ticket.setCategoryId(CATEGORY_ID);
        TicketWrapper ticketWrapper = new TicketWrapper(sku,
                createDefaultModelBuilder(idForModel(ticket.getId()), ticket.getPartnerShopId())
                        .setStrictChecksRequired(true).build(),
                true, ticket);

        when(cskuDataPreparation.collectDataForRequest(List.of(ticket))).thenReturn(List.of(ticketWrapper));
        ProcessTaskResult<ProcessDataBucketData> taskResult =
                extractConflictInfoTaskAction.runOnTickets(List.of(ticket), new ProcessDataBucketData(1L));

        assertThat(taskResult).isNotNull();
        assertThat(taskResult).extracting(ProcessTaskResult::hasProblems).isEqualTo(false);
    }

    @Test
    public void skipOffersForBrokenModels() {
        //Категория CATEGORY_ID
        ModelStorage.Model sku = getModel().toBuilder().setBroken(true).build();
        GcSkuTicket ticket = generateDBDcpInitialStateNew(1, datacampOffers -> {
            datacampOffers.get(0).getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(CATEGORY_ID))
                    .withStringParam(KnownParameters.BARCODE.getId(), "100")
                    .withStringParam(PARAM_ID_1, "1000")
                    .build();
        }).get(0);
        ticket.setCategoryId(CATEGORY_ID);
        TicketWrapper ticketWrapper = new TicketWrapper(sku,
                createDefaultModelBuilder(idForModel(ticket.getId()), ticket.getPartnerShopId())
                        .setBroken(true).build(),
                true, ticket);

        when(cskuDataPreparation.collectDataForRequest(List.of(ticket))).thenReturn(List.of(ticketWrapper));
        ProcessTaskResult<ProcessDataBucketData> taskResult =
                extractConflictInfoTaskAction.runOnTickets(List.of(ticket), new ProcessDataBucketData(1L));

        assertThat(taskResult).isNotNull();
        assertThat(taskResult).extracting(ProcessTaskResult::hasProblems).isEqualTo(false);
    }

    @NotNull
    private ModelStorage.Model getModel() {
        ModelStorage.Model.Builder sku = ModelGenerator.generateModelBuilder(Collections.emptyList(), 0,
                        ModelStorage.ModificationSource.VENDOR_OFFICE, 0)
                .setSupplierId(0)
                .setCategoryId(CATEGORY_ID)
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addAllParameterValues(
                Arrays.asList(
                        ParameterCreator.createStringParam(
                                PARAM_ID_1,
                                PARAM_NAME_1,
                                Collections.singletonList(VALUE_1),
                                0,
                                ModelStorage.ModificationSource.AUTO,
                                0
                        ),
                        ParameterCreator.createStringParam(
                                PARAM_ID_2,
                                PARAM_NAME_2,
                                Collections.singletonList(VALUE_2),
                                SUPPLIER_ID + 1,
                                ModelStorage.ModificationSource.AUTO,
                                0
                        ),
                        ParameterCreator.createBooleanParam(
                                PARAM_ID_3,
                                PARAM_NAME_3,
                                0,
                                false,
                                SUPPLIER_ID + 1,
                                ModelStorage.ModificationSource.AUTO,
                                0
                        ),
                        ParameterCreator.createNumericParam(
                                PARAM_ID_4,
                                PARAM_NAME_4,
                                "111",
                                SUPPLIER_ID + 1,
                                ModelStorage.ModificationSource.AUTO,
                                0
                        ),
                        ParameterCreator.createNumericParam(
                                PARAM_ID_6,
                                PARAM_NAME_6,
                                "1",
                                SUPPLIER_ID + 1,
                                ModelStorage.ModificationSource.AUTO,
                                0
                        )
                )
        );
        return sku.build();
    }

    private void generateOffer() {
        offerBuilder = OffersGenerator.generateOfferBuilder(Arrays.asList(
                SimplifiedOfferParameter.forOffer(PARAM_ID_1, PARAM_NAME_1, VALUE_1, OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(PARAM_ID_2, PARAM_NAME_2, VALUE_2, OfferParameterType.STRING)
        ));

        DataCampContentMarketParameterValue.MarketParameterValue param3 =
                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                        .setParamId(PARAM_ID_3)
                        .setParamName(PARAM_NAME_3)
                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.BOOLEAN)
                                .setBoolValue(VALUE_3)
                        )
                        .build();
        DataCampContentMarketParameterValue.MarketParameterValue param4 =
                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                        .setParamId(PARAM_ID_4)
                        .setParamName(PARAM_NAME_4)
                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.NUMERIC)
                                .setNumericValue(VALUE_4)
                        )
                        .build();

        DataCampContentMarketParameterValue.MarketParameterValue param6 =
                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                        .setParamId(PARAM_ID_6)
                        .setParamName(PARAM_NAME_6)
                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                .setStrValue(String.valueOf(VALUE_6))
                                .setOptionId(VALUE_6)
                        )
                        .build();

        offerBuilder.setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                        .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent
                                .newBuilder()
                                .setParameterValues(offerBuilder.getContent().getPartner().getMarketSpecificContent()
                                        .getParameterValues().toBuilder()
                                        .addAllParameterValues(Arrays.asList(param3, param4, param6))
                                        .build())
                                .build()
                        ).build())
                .build());

        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        offer = offerBuilder.build();
    }

    @NotNull
    private ModelStorage.Model getMultivalueModel() {
        ModelStorage.Model.Builder sku = ModelGenerator.generateModelBuilder(Collections.emptyList(), 0,
                        ModelStorage.ModificationSource.VENDOR_OFFICE, 0)
                .setSupplierId(0)
                .setCategoryId(CATEGORY_ID)
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addAllParameterValues(
                Arrays.asList(
                        ParameterCreator.createStringParam(
                                PARAM_ID_1,
                                PARAM_NAME_1,
                                Collections.singletonList(VALUE_1),
                                0,
                                ModelStorage.ModificationSource.VENDOR_OFFICE,
                                0
                        ),
                        ParameterCreator.createStringParam(
                                PARAM_ID_2,
                                PARAM_NAME_2,
                                Collections.singletonList(VALUE_2),
                                SUPPLIER_ID + 1,
                                ModelStorage.ModificationSource.VENDOR_OFFICE,
                                0
                        ),
                        ParameterCreator.createStringParam(
                                ParameterValueComposer.BARCODE_ID,
                                CategoryData.BAR_CODE,
                                Collections.singletonList(VALUE_5_1),
                                SUPPLIER_ID + 1,
                                ModelStorage.ModificationSource.VENDOR_OFFICE,
                                0
                        )
                )
        );
        return sku.build();
    }

    private void generateMultivalueOffer() {
        offerBuilder = OffersGenerator.generateOfferBuilder(Arrays.asList(
                SimplifiedOfferParameter.forOffer(PARAM_ID_1, PARAM_NAME_1, VALUE_1, OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(PARAM_ID_2, PARAM_NAME_2, VALUE_2, OfferParameterType.STRING)
        ));

        DataCampContentMarketParameterValue.MarketParameterValue param3 =
                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                        .setParamId(ParameterValueComposer.BARCODE_ID)
                        .setParamName(CategoryData.BAR_CODE)
                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.STRING)
                                .setStrValue(VALUE_5_2)
                        )
                        .build();

        DataCampContentMarketParameterValue.MarketParameterValue param4 =
                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                        .setParamId(ParameterValueComposer.BARCODE_ID)
                        .setParamName(CategoryData.BAR_CODE)
                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.STRING)
                                .setStrValue(VALUE_5_3)
                        )
                        .build();

        offerBuilder.setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                        .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                                .setBarcode(DataCampOfferMeta.StringListValue.newBuilder()
                                        .addAllValue(Arrays.asList(VALUE_5_2, VALUE_5_3)).build())
                                .build())
                        .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent
                                .newBuilder()
                                .setParameterValues(offerBuilder.getContent().getPartner().getMarketSpecificContent()
                                        .getParameterValues().toBuilder()
                                        .addAllParameterValues(Arrays.asList(param3, param4))
                                        .build())
                                .build()
                        ).build())
                .build());


        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        offer = offerBuilder.build();
    }
}
