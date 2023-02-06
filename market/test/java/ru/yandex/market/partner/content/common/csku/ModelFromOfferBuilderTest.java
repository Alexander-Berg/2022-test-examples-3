package ru.yandex.market.partner.content.common.csku;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampContentMarketParameterValue.MarketValueSource;
import Market.DataCamp.DataCampContentMarketParameterValue.MarketValueType;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.judge.Decision;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.csku.judge.ModelData;
import ru.yandex.market.partner.content.common.csku.wrappers.BaseParameterWrapper;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.BARCODE;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.DESCRIPTION;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.FORMER_BARCODE;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.NAME;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.RAW_VENDOR;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.SUMMARY;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.UNITED_SIZE;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.VENDOR;
import static ru.yandex.market.partner.content.common.csku.util.DcpSpecialParameterCreator.VENDOR_NOT_FOUND_OPTION_ID;

public class ModelFromOfferBuilderTest {

    //Offer
    public static final String OFFER_TITLE = "offer title param name";
    public static final String OFFER_LONG_DESCRIPTION = "OFFER_LONG_DESCRIPTION";
    public static final String OFFER_DESCRIPTION = "OFFER_DESCRIPTION";
    private static final Long PARAM_ID = 2L;
    private static final String VALUE = "Value";

    private static final Long PARAM_ID_1 = 1L;
    private static final String VALUE_1 = "Some value";

    public static final long VENDOR_OPTION_ID = 13;
    public static final String VENDOR_NAME_1 = "offer";
    public static final String VENDOR_NAME_2 = "model";
    public static final String VENDOR_NAME_3 = "title";

    private static final int SUPPLIER_ID = 123;
    private static final Integer GROUP_ID = 14567;
    private static final String VENDOR_NAME = "Some vendor";
    private static final String ANOTHER_VENDOR_NAME = "Some other vendor";
    private static final String SHOP_SKU_VALUE = "Shop sku";

    private DataCampOffer.Offer offer;


    //Model
    public static final String MODEL_TITLE = "model title";
    public static final String MODEL_DESCRIPTION = "MODEL_DESCRIPTION";
    public static final String MODEL_LONG_DESCRIPTION = "MODEL_LONG_DESCRIPTION";
    private static final Long MODEL_PARAM_ID = 3L;
    private static final String MODEL_VALUE = "Model value";
    private static final String CONFLICTING_VALUE = "Alarm! Conflict is unleashed!";

    private static final int OFFER_SUPPLIER_ID = 5;

    private CategoryData categoryData;

    @Before
    public void init() {
        categoryData = mock(CategoryData.class);
        when(categoryData.getParamById(anyLong())).thenAnswer(new Answer<MboParameters.Parameter>() {
            @Override
            public MboParameters.Parameter answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Long id = (Long) args[0];
                return MboParameters.Parameter.newBuilder()
                        .setId(id)
                        .build();
            }
        });
    }

    @Test
    public void testUseModelTitleIfOfferTitleIsNull() {
        ModelStorage.ParameterValue modelTitle = generateParameterValue(NAME.getId(), MODEL_TITLE);

        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder()
                .addParameterValues(modelTitle)
                .build();

        ModelStorage.Model resultModel = ModelFromOfferBuilder
                .builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .build();

        assertThat(resultModel.getParameterValuesList()).hasSize(1);
        assertThat(resultModel.getParameterValues(0)).isEqualTo(modelTitle);
    }

    @Test
    public void testUseOfferTitleIfModelTitleIsNull() {
        ModelStorage.ParameterValue offerTitle = generateParameterValue(NAME.getId(), OFFER_TITLE);

        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder().build();

        ModelStorage.Model resultModel = ModelFromOfferBuilder
                .builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(offerTitle)
                .build();

        assertThat(resultModel.getParameterValuesList()).hasSize(1);
        assertThat(resultModel.getParameterValues(0)).isEqualTo(offerTitle);
    }

    @Test
    public void testUseModelTitleIfTitleInfoIsNull() {
        ModelStorage.ParameterValue modelTitle = generateParameterValue(NAME.getId(), MODEL_TITLE);
        ModelStorage.ParameterValue offerTitle = generateParameterValue(NAME.getId(), OFFER_TITLE);

        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder()
                .addParameterValues(modelTitle)
                .build();

        ModelStorage.Model resultModel = ModelFromOfferBuilder
                .builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(offerTitle)
                .build();

        assertThat(resultModel.getParameterValuesList()).hasSize(1);
        //assertThat(resultModel.getParameterValues(0)).isEqualTo(modelTitle);
        assertThat(resultModel.getParameterValues(0)).isEqualTo(offerTitle);

        when(categoryData.getTitleAvgCharsAmount()).thenReturn(30);
        resultModel = ModelFromOfferBuilder
                .builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(offerTitle)
                .build();

        assertThat(resultModel.getParameterValuesList()).hasSize(1);
        //Модельный при выбора тайтла по близости к MSKUшному
        //assertThat(resultModel.getParameterValues(0)).isEqualTo(modelTitle);
        assertThat(resultModel.getParameterValues(0)).isEqualTo(offerTitle);
    }

    @Test
    public void testChooseTheBestTitle1() {
        when(categoryData.getTitleAvgCharsAmount()).thenReturn(30);
        when(categoryData.getTitleAvgWordsAmount()).thenReturn(4);

        ModelStorage.ParameterValue modelTitle = generateParameterValue(NAME.getId(), MODEL_TITLE);
        ModelStorage.ParameterValue offerTitle = generateParameterValue(NAME.getId(), OFFER_TITLE);

        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder()
                .addParameterValues(modelTitle)
                .build();

        ModelStorage.Model resultModel = ModelFromOfferBuilder
                .builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(offerTitle)
                .build();

        assertThat(resultModel.getParameterValuesList()).hasSize(1);
        assertThat(resultModel.getParameterValues(0)).isEqualTo(offerTitle);
    }

    @Test
    public void testChooseTheBestTitle2() {
        when(categoryData.getTitleAvgCharsAmount()).thenReturn(15);
        when(categoryData.getTitleAvgWordsAmount()).thenReturn(2);

        ModelStorage.ParameterValue modelTitle = generateParameterValue(NAME.getId(), MODEL_TITLE);
        ModelStorage.ParameterValue offerTitle = generateParameterValue(NAME.getId(), OFFER_TITLE);

        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder()
                .addParameterValues(modelTitle)
                .build();

        ModelStorage.Model resultModel = ModelFromOfferBuilder
                .builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(offerTitle)
                .build();

        assertThat(resultModel.getParameterValuesList()).hasSize(1);
        //Модельный при выбора тайтла по близости к MSKUшному
        //assertThat(resultModel.getParameterValues(0)).isEqualTo(modelTitle);
        assertThat(resultModel.getParameterValues(0)).isEqualTo(offerTitle);
    }

    @Test
    public void testChooseTheBestTitle3() {
        when(categoryData.getTitleAvgCharsAmount()).thenReturn(30);
        when(categoryData.getTitleAvgWordsAmount()).thenReturn(1);

        ModelStorage.ParameterValue modelTitle = generateParameterValue(NAME.getId(), MODEL_TITLE);
        ModelStorage.ParameterValue offerTitle = generateParameterValue(NAME.getId(), OFFER_TITLE);

        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder()
                .addParameterValues(modelTitle)
                .build();

        ModelStorage.Model resultModel = ModelFromOfferBuilder
                .builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(offerTitle)
                .build();

        assertThat(resultModel.getParameterValuesList()).hasSize(1);
        assertThat(resultModel.getParameterValues(0)).isEqualTo(offerTitle);
    }

    @Test
    public void testChooseTheBestTitleModelContainsVendor() {
        when(categoryData.getTitleAvgCharsAmount()).thenReturn(30);
        when(categoryData.getTitleAvgWordsAmount()).thenReturn(3);
        when(categoryData.getVendorName(Math.toIntExact(VENDOR_OPTION_ID))).thenReturn(VENDOR_NAME_2);

        ModelStorage.ParameterValue modelTitle = generateParameterValue(NAME.getId(), MODEL_TITLE);
        ModelStorage.ParameterValue modelVendor = generateEnumParameterValue(VENDOR.getId(),
                Math.toIntExact(VENDOR_OPTION_ID));
        ModelStorage.ParameterValue offerTitle = generateParameterValue(NAME.getId(), OFFER_TITLE);


        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder()
                .addParameterValues(modelTitle)
                .addParameterValues(modelVendor)
                .build();

        ModelStorage.Model resultModel = ModelFromOfferBuilder
                .builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(offerTitle)
                .build();

        assertThat(resultModel.getParameterValuesList()).hasSize(2);
        assertThat(resultModel.getParameterValuesList()).containsExactlyInAnyOrder(offerTitle, modelVendor);
    }

    @Test
    public void testChooseTheBestTitleOfferContainsVendor() {
        when(categoryData.getTitleAvgCharsAmount()).thenReturn(10);
        when(categoryData.getTitleAvgWordsAmount()).thenReturn(1);
        when(categoryData.getVendorName(Math.toIntExact(VENDOR_OPTION_ID))).thenReturn(VENDOR_NAME_1);

        ModelStorage.ParameterValue modelTitle = generateParameterValue(NAME.getId(), MODEL_TITLE);
        ModelStorage.ParameterValue offerTitle = generateParameterValue(NAME.getId(), OFFER_TITLE);
        ModelStorage.ParameterValue offerVendor = generateEnumParameterValue(VENDOR.getId(),
                Math.toIntExact(VENDOR_OPTION_ID));

        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder()
                .addParameterValues(modelTitle)
                .build();

        ModelStorage.Model resultModel = ModelFromOfferBuilder
                .builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(offerTitle)
                .addParameter(offerVendor)
                .build();

        assertThat(resultModel.getParameterValuesList()).hasSize(2);
        assertThat(resultModel.getParameterValuesList()).containsExactlyInAnyOrder(offerTitle, offerVendor);
    }

    @Test
    public void testChooseTheBestTitleModelAndOfferContainsVendor() {
        when(categoryData.getTitleAvgCharsAmount()).thenReturn(30);
        when(categoryData.getTitleAvgWordsAmount()).thenReturn(4);
        when(categoryData.getVendorName(Math.toIntExact(VENDOR_OPTION_ID))).thenReturn(VENDOR_NAME_3);

        ModelStorage.ParameterValue modelTitle = generateParameterValue(NAME.getId(), MODEL_TITLE);
        ModelStorage.ParameterValue offerTitle = generateParameterValue(NAME.getId(), OFFER_TITLE);

        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder()
                .addParameterValues(modelTitle)
                .build();

        ModelStorage.Model resultModel = ModelFromOfferBuilder
                .builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(offerTitle)
                .build();

        assertThat(resultModel.getParameterValuesList()).hasSize(1);
        assertThat(resultModel.getParameterValuesList()).containsExactlyInAnyOrder(offerTitle);
    }

    @Test
    public void testUseModelDescriptionIfOfferDescriptionIsNull() {
        testDescription(MODEL_DESCRIPTION, null, true, false);
        testDescription(MODEL_DESCRIPTION, null, false, false);
    }

    @Test
    public void testUseOfferDescriptionIfModelDescriptionIsNull() {
        testDescription(null, OFFER_DESCRIPTION, true, true);
        testDescription(null, OFFER_DESCRIPTION, false, true);
    }

    @Test
    public void testChooseTheBestDescriptionOfferLonger() {
        testDescription(MODEL_DESCRIPTION, OFFER_LONG_DESCRIPTION, true, true);
        testDescription(MODEL_DESCRIPTION, OFFER_LONG_DESCRIPTION, false, true);
    }

    @Test
    public void testChooseTheBestDescriptionModelLonger() {
        testDescription(MODEL_LONG_DESCRIPTION, OFFER_DESCRIPTION, true, false);
        testDescription(MODEL_LONG_DESCRIPTION, OFFER_DESCRIPTION, false, false);
    }

    @Test
    public void testChooseTheBestDescriptionEqual() {
        testDescription(MODEL_DESCRIPTION, OFFER_DESCRIPTION, true, false);
        testDescription(MODEL_DESCRIPTION, OFFER_DESCRIPTION, false, false);
    }

    @Test
    public void testChooseOfferDescriptionWhenTheSameOwnerEqual() {
        testDescription(MODEL_DESCRIPTION, OFFER_DESCRIPTION, true, true, 1, 1);
        testDescription(MODEL_DESCRIPTION, OFFER_DESCRIPTION, false, true, 1, 1);
    }

    @Test
    public void testChooseOfferDescriptionWhenNoOwnerButSupplierEqual() {
        testDescription(MODEL_DESCRIPTION, OFFER_DESCRIPTION, true, true, 0, 1);
        testDescription(MODEL_DESCRIPTION, OFFER_DESCRIPTION, false, true, 0, 1);
    }

    private void testDescription(String modelDescription, String offerDescription,
                                 boolean isSku, boolean fromOfferInResult) {
        testDescription(modelDescription, offerDescription, isSku, fromOfferInResult, 1, 2);
    }

    private void testDescription(String modelDescription, String offerDescription,
                                 boolean isSku, boolean fromOfferInResult, int modelDescriptionOwner,
                                 int offerDescriptionOwner) {
        Long paramId = (isSku) ? DESCRIPTION.getId() : SUMMARY.getId();

        ModelStorage.ParameterValue modelDescriptionPv = (StringUtils.isNotBlank(modelDescription)) ?
                generateParameterValue(paramId, modelDescription, modelDescriptionOwner) : null;
        ModelStorage.ParameterValue offerDescriptionPv = (StringUtils.isNotBlank(offerDescription)) ?
                generateParameterValue(paramId, offerDescription, offerDescriptionOwner) : null;

        ModelStorage.Model.Builder inputModelBuilder = ModelStorage.Model.newBuilder();
        if (modelDescriptionPv != null) {
            inputModelBuilder.addParameterValues(modelDescriptionPv);
        }
        ModelFromOfferBuilder resultModelBuilder = ModelFromOfferBuilder
                .builder(inputModelBuilder.build(), isSku, categoryData, OFFER_SUPPLIER_ID);
        if (offerDescriptionPv != null) {
            resultModelBuilder.addParameter(offerDescriptionPv);
        }

        ModelStorage.Model resultModel = resultModelBuilder.build();

        assertThat(resultModel.getParameterValuesList()).hasSize(1);
        assertThat(resultModel.getParameterValues(0))
                .isEqualTo((fromOfferInResult) ? offerDescriptionPv : modelDescriptionPv);
    }

    @Test
    public void whenModelHasSameParamsAsOfferReplaceValues() {
        //Offer
        ModelStorage.ParameterValue parameterValue = generateParameterValue(PARAM_ID, VALUE);
        ModelStorage.ParameterValue parameterValue1 = generateParameterValue(PARAM_ID_1, VALUE_1);

        //Model
        ModelStorage.ParameterValue modelParameterValue = generateParameterValue(MODEL_PARAM_ID, MODEL_VALUE);
        ModelStorage.ParameterValue conflictingModelParameterValue = generateParameterValue(PARAM_ID_1,
                CONFLICTING_VALUE);

        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder()
                .addParameterValues(conflictingModelParameterValue)
                .addParameterValues(modelParameterValue)
                .build();

        ModelStorage.Model result = ModelFromOfferBuilder.builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(parameterValue)
                .addParameter(parameterValue1)
                .build();
        //Проверяем, что нет конфликтующего значения
        assertThat(result.getParameterValuesList()).containsExactlyInAnyOrder(
                parameterValue,
                parameterValue1,
                modelParameterValue);

    }

    @Test
    public void whenOfferHasFormerBarcodeIgnore() {
        //Offer
        ModelStorage.ParameterValue parameterValue = generateParameterValue(BARCODE.getId(), "123");

        //Model
        ModelStorage.ParameterValue modelParameterValue = generateParameterValue(FORMER_BARCODE.getId(), "123");

        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder()
                .addParameterValues(modelParameterValue)
                .build();

        ModelStorage.Model result = ModelFromOfferBuilder.builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(parameterValue)
                .build();

        //Проверяем, что баркод оффера игнорируется
        assertThat(result.getParameterValuesCount()).isEqualTo(1);
        assertThat(result.getParameterValuesList()).containsExactlyInAnyOrder(modelParameterValue);
    }

    @Test
    public void whenMultivalueAddAllValues() {
        ModelStorage.ParameterValue parameterValue = generateParameterValue(PARAM_ID, VALUE);
        ModelStorage.ParameterValue parameterValue1 = generateParameterValue(PARAM_ID, VALUE_1);
        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder().build();
        ModelStorage.Model result = ModelFromOfferBuilder.builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(parameterValue)
                .addParameter(parameterValue1)
                .build();
        List<ModelStorage.ParameterValue> modelParameterValuesList = result.getParameterValuesList();
        assertThat(modelParameterValuesList).hasSize(2);
        assertThat(modelParameterValuesList.stream().map(value -> value.getStrValue(0).getValue())
                .collect(Collectors.toList())).containsExactlyInAnyOrder(VALUE, VALUE_1);

    }

    @Test
    public void whenMultiValueSaveModelValuesWithOtherOwnerIds() {
        when(categoryData.getParamById(PARAM_ID_1))
                .thenReturn(MboParameters.Parameter.newBuilder().setMultivalue(true).build());
        //Offer
        ModelStorage.ParameterValue opv1 = generateParameterValue(PARAM_ID_1, "value1");
        ModelStorage.ParameterValue opv2 = generateParameterValue(PARAM_ID_1, "value4");
        ModelStorage.ParameterValue opv3 = generateParameterValue(PARAM_ID_1, "value5");

        //Model
        ModelStorage.ParameterValue mpv1 = generateParameterValue(PARAM_ID_1, "value1", OFFER_SUPPLIER_ID);
        ModelStorage.ParameterValue mpv2 = generateParameterValue(PARAM_ID_1, "value2", OFFER_SUPPLIER_ID);
        ModelStorage.ParameterValue mpv3 = generateParameterValue(PARAM_ID_1, "value3", SUPPLIER_ID);
        ModelStorage.ParameterValue mpv4 = generateParameterValue(PARAM_ID_1, "value4", SUPPLIER_ID);

        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder()
                .addParameterValues(mpv1)
                .addParameterValues(mpv2)
                .addParameterValues(mpv3)
                .addParameterValues(mpv4)
                .build();

        ModelStorage.Model result = ModelFromOfferBuilder.builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .addParameter(opv1)
                .addParameter(opv2)
                .addParameter(opv3)
                .build();

        //Проверяем, что нет конфликтующего значения
        assertThat(result.getParameterValuesList())
                .containsExactlyInAnyOrder(opv1, opv3, mpv3, mpv4);

    }

    @Test
    public void testWhenSamePartnerChangeUnknownToRealVendorThenCleanUpRawVendor() {
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValuesBuilder()
                .setParamId(VENDOR.getId())
                .setOptionId(Math.toIntExact(VENDOR_NOT_FOUND_OPTION_ID))
                .setOwnerId(SUPPLIER_ID)
                .build();
        sku.addParameterValuesBuilder()
                .setParamId(RAW_VENDOR.getId())
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(ANOTHER_VENDOR_NAME).build())
                .setOwnerId(SUPPLIER_ID)
                .build();

        MboParameters.Parameter vendorCategoryParameter = MboParameters.Parameter.newBuilder()
                .addOption(MboParameters.Option.newBuilder()
                        .setId(VENDOR_OPTION_ID)
                        .addName(MboParameters.Word.newBuilder().setName(VENDOR_NAME).build())
                        .build())
                .build();

        when(categoryData.getParamById(ParameterValueComposer.VENDOR_ID)).thenReturn(vendorCategoryParameter);
        DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder();
        Judge judge = new Judge();

        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        offerBuilder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                .setVendor(DataCampOfferMeta.StringValue.newBuilder().setValue(VENDOR_NAME).build());
        offer = offerBuilder.build();

        ModelData skuData = new ModelData(sku.build(), true, SHOP_SKU_VALUE);
        Map<BaseParameterWrapper, Decision> decisionMap =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        ModelFromOfferBuilder modelBuilder = ModelFromOfferBuilder.builder(
                sku.build(), true, categoryData, SUPPLIER_ID);
        decisionMap.forEach((wrapper, decision) -> {
            if (decision.isModify()) {
                wrapper.putValuesInSkuAndModel(modelBuilder);
            }
        });
        ModelStorage.Model resultingSku = modelBuilder.build();
        boolean rawVendorIsCleaned =
                resultingSku.getParameterValuesList().stream()
                        .noneMatch(pv -> RAW_VENDOR.getId().equals(pv.getParamId()));
        assertThat(rawVendorIsCleaned).isTrue();
    }

    @Test
    public void whenNoParamsInOfferThenModelParamsLeft() {
        //Model
        ModelStorage.ParameterValue modelParameterValue = generateParameterValue(MODEL_PARAM_ID, MODEL_VALUE);
        ModelStorage.ParameterValue modelParameterValue2 = generateParameterValue(PARAM_ID_1,
                CONFLICTING_VALUE);

        ModelStorage.ParameterValueHypothesis hypo = ModelStorage.ParameterValueHypothesis.newBuilder()
                .addStrValue(MboParameters.Word.newBuilder().setName("Hypo").build())
                .build();

        ModelStorage.Model inputModel = ModelStorage.Model.newBuilder()
                .addParameterValues(modelParameterValue2)
                .addParameterValues(modelParameterValue)
                .addParameterValueHypothesis(hypo)
                .build();

        ModelStorage.Model result = ModelFromOfferBuilder.builder(inputModel, true, categoryData, OFFER_SUPPLIER_ID)
                .build();
        //Проверяем, что сохранились параметры
        assertThat(result.getParameterValuesList()).containsExactlyInAnyOrder(
                modelParameterValue2,
                modelParameterValue);
        assertThat(result.getParameterValueHypothesisList()).containsOnly(hypo);

    }
    @Test
    public void whenOfferWithHypotheses() {
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());

        int optionId = 11111111;
        String optionName = optionId + "name";
        MboParameters.Parameter unitedSizeCategoryParameter = MboParameters.Parameter.newBuilder()
                .setId(UNITED_SIZE.getId())
                .setXslName(UNITED_SIZE.getXslName())
                .setValueType(MboParameters.ValueType.ENUM)
                .addOption(MboParameters.Option.newBuilder()
                        .setId(optionId)
                        .addName(MboParameters.Word.newBuilder()
                                .setName(optionName).build())
                        .build())
                .build();
        when(categoryData.getParamById(UNITED_SIZE.getId())).thenReturn(unitedSizeCategoryParameter);
        when(categoryData.containsParam(UNITED_SIZE.getId())).thenReturn(true);
        when(categoryData.isSkuParameter(UNITED_SIZE.getId())).thenReturn(true);
        DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder();

        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        offerBuilder.getContentBuilder().getPartnerBuilder().getMarketSpecificContentBuilder()
                .getParameterValuesBuilder()
                .addParameterValues(DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                        .setParamId(UNITED_SIZE.getId())
                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setStrValue(optionName)
                                .setValueType(MarketValueType.HYPOTHESIS)
                                .build())
                        .setValueSource(MarketValueSource.CONTENT_EXCEL)
                        .build());

        offer = offerBuilder.build();

        ModelData skuData = new ModelData(sku.build(), true, SHOP_SKU_VALUE);
        Judge judge = new Judge();
        Map<BaseParameterWrapper, Decision> decisionMap =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        ModelFromOfferBuilder modelBuilder = ModelFromOfferBuilder.builder(
                sku.build(), true, categoryData, SUPPLIER_ID);
        decisionMap.forEach((wrapper, decision) -> {
            if (decision.isModify()) {
                wrapper.putValuesInSkuAndModel(modelBuilder);
            }
        });
        ModelStorage.Model resultingSku = modelBuilder.build();

        // пришел в оффере гипотезой, поэтому отсутствует в списке параметров, но есть в списке гипотез
        assertThat(resultingSku.getParameterValuesList().stream().noneMatch(p -> p.getParamId() == UNITED_SIZE.getId())).isTrue();
        assertThat(resultingSku.getParameterValueHypothesisList().stream().anyMatch(p -> p.getParamId() == UNITED_SIZE.getId())).isTrue();

    }

    @Test
    public void whenUnitedSizeIsEnum() {
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());

        int optionId = 11111111;
        String optionName = optionId + "name";
        MboParameters.Parameter unitedSizeCategoryParameter = MboParameters.Parameter.newBuilder()
                .setId(UNITED_SIZE.getId())
                .setXslName(UNITED_SIZE.getXslName())
                .setValueType(MboParameters.ValueType.ENUM)
                .addOption(MboParameters.Option.newBuilder()
                        .setId(optionId)
                        .addName(MboParameters.Word.newBuilder()
                                .setName(optionName).build())
                        .build())
                .build();
        when(categoryData.getParamById(UNITED_SIZE.getId())).thenReturn(unitedSizeCategoryParameter);
        when(categoryData.containsParam(UNITED_SIZE.getId())).thenReturn(true);
        when(categoryData.isSkuParameter(UNITED_SIZE.getId())).thenReturn(true);
        DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder();

        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        offerBuilder.getContentBuilder().getPartnerBuilder().getMarketSpecificContentBuilder()
                .getParameterValuesBuilder()
                .addParameterValues(DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                        .setParamId(UNITED_SIZE.getId())
                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setStrValue(optionName)
                                .setValueType(MarketValueType.ENUM)
                                .build())
                        .setValueSource(MarketValueSource.CONTENT_EXCEL)
                        .build());

        offer = offerBuilder.build();

        ModelData skuData = new ModelData(sku.build(), true, SHOP_SKU_VALUE);
        Judge judge = new Judge();
        Map<BaseParameterWrapper, Decision> decisionMap =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        ModelFromOfferBuilder modelBuilder = ModelFromOfferBuilder.builder(
                sku.build(), true, categoryData, SUPPLIER_ID);
        decisionMap.forEach((wrapper, decision) -> {
            if (decision.isModify()) {
                wrapper.putValuesInSkuAndModel(modelBuilder);
            }
        });
        ModelStorage.Model resultingSku = modelBuilder.build();

        // Несмотря на то, что пришел enum, united_size оказывается в списке гипотез
        assertThat(resultingSku.getParameterValuesList().stream().noneMatch(p -> p.getParamId() == UNITED_SIZE.getId())).isTrue();
        assertThat(resultingSku.getParameterValueHypothesisList().stream().anyMatch(p -> p.getParamId() == UNITED_SIZE.getId())).isTrue();
    }

    private ModelStorage.ParameterValue generateParameterValue(long id, String value) {
        return generateParameterValue(id, value, null);
    }

    private ModelStorage.ParameterValue generateParameterValue(long id, String value, Integer ownerId) {
        ModelStorage.ParameterValue.Builder builder = ModelStorage.ParameterValue.newBuilder()
                .setParamId(id)
                .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue(value)
                        .build())
                .setValueType(MboParameters.ValueType.STRING);
        if (ownerId != null) {
            builder.setOwnerId(ownerId);
        }
        return builder.build();
    }

    private ModelStorage.ParameterValue generateEnumParameterValue(long id, int optionId) {
        return generateEnumParameterValue(id, optionId, null);
    }

    private ModelStorage.ParameterValue generateEnumParameterValue(long id, int optionId, Integer ownerId) {
        ModelStorage.ParameterValue.Builder builder = ModelStorage.ParameterValue.newBuilder()
                .setParamId(id)
                .setOptionId(optionId)
                .setValueType(MboParameters.ValueType.ENUM);
        if (ownerId != null) {
            builder.setOwnerId(ownerId);
        }
        return builder.build();
    }

}
