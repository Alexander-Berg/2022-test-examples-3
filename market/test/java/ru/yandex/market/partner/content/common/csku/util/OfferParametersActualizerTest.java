package ru.yandex.market.partner.content.common.csku.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.UltraControllerServiceData.UltraController;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.OfferParameterType;
import ru.yandex.market.partner.content.common.csku.OffersGenerator;
import ru.yandex.market.partner.content.common.csku.SimplifiedOfferParameter;
import ru.yandex.market.partner.content.common.csku.judge.MarketParameterValueWrapper;
import ru.yandex.market.partner.content.common.csku.judge.ModelData;
import ru.yandex.market.partner.content.common.csku.wrapperGroups.WrapperGroupsFactory;
import ru.yandex.market.partner.content.common.csku.wrapperGroups.holders.WrapperGroupsHolder;
import ru.yandex.market.partner.content.common.csku.wrappers.BaseParameterWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.BARCODE;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.DESCRIPTION;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.NAME;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.URL;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.VENDOR;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.VENDOR_CODE;

public class OfferParametersActualizerTest {

    private CategoryData categoryData;
    private DataCampOffer.Offer offer;
    private static final Long NEW_PARAM_ID = 22L;
    private static final String NEW_PARAM_NAME = "new";
    private static final Long OLD_PARAM_ID = 11L;
    private static final Long PARAM_ID = 33L;
    private static final String SHOP_SKU = "Shop sku";
    private static final Integer GROUP_ID = 14567;

    @Before
    public void init() {
        categoryData = mock(CategoryData.class);
    }

    @Test
    public void whenMigratedSkuAndOldParamForModelThenProcessOnlyMigrated() {
        when(categoryData.getMigratedParamId(OLD_PARAM_ID)).thenReturn(NEW_PARAM_ID);
        when(categoryData.containsParam(NEW_PARAM_ID)).thenReturn(true);
        when(categoryData.getParamById(NEW_PARAM_ID)).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(NEW_PARAM_ID)
                .setValueType(MboParameters.ValueType.STRING)
                .setXslName(NEW_PARAM_NAME)
                .build());

        when(categoryData.containsParam(PARAM_ID)).thenReturn(true);
        when(categoryData.getParamById(PARAM_ID)).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(PARAM_ID)
                .setValueType(MboParameters.ValueType.STRING)
                .setXslName("param")
                .build());

        when(categoryData.isSkuParameter(PARAM_ID)).thenReturn(true);
        when(categoryData.isSkuParameter(NEW_PARAM_ID)).thenReturn(false);

        List<SimplifiedOfferParameter> simplifiedOfferParameters = Arrays.asList(
                SimplifiedOfferParameter.forOffer(OLD_PARAM_ID, "old_name", "Old",
                        OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(NEW_PARAM_ID, "new_name", "New",
                        OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(PARAM_ID, "piu_name", "Param-param-piu",
                        OfferParameterType.STRING));
        offer = OffersGenerator.generate(simplifiedOfferParameters);

        List<MarketParameterValueWrapper> filteredParams =
                OfferParametersActualizer.actualize(offer, categoryData, false);
        assertThat(filteredParams).hasSize(1);
        assertThat(filteredParams.get(0).getParamId()).isEqualTo(NEW_PARAM_ID);
    }

    @Test
    public void whenVendorParameterThenProcessInModelAndSku() {
        List<SimplifiedOfferParameter> simplifiedOfferParameters = Collections.singletonList(
                SimplifiedOfferParameter.forOffer(VENDOR.getId(), VENDOR.getXslName(), "vendor",
                        OfferParameterType.STRING)
        );

        offer = OffersGenerator.generate(simplifiedOfferParameters);

        ModelStorage.Model model = ModelStorage.Model.newBuilder().setId(-1).build();

        ModelData data = new ModelData(model, false, SHOP_SKU);
        WrapperGroupsHolder groupsHolder
                = WrapperGroupsFactory.getOfferWrapperGroup(offer, data, categoryData, new HashSet<>());
        List<BaseParameterWrapper> parameterWrappers = groupsHolder.getParameterWrappers();

        assertThat(parameterWrappers).hasSize(3);
        assertThat(parameterWrappers.stream()
                .map(BaseParameterWrapper::extractSelfFromOffer)
                .map(SimplifiedOfferParameter::getParamId))
                .contains(VENDOR.getId());

        data = new ModelData(model, true, SHOP_SKU);
        groupsHolder = WrapperGroupsFactory.getOfferWrapperGroup(offer, data, categoryData, new HashSet<>());
        parameterWrappers = groupsHolder.getParameterWrappers();

        assertThat(parameterWrappers).hasSize(4);
        assertThat(parameterWrappers.stream()
                .map(BaseParameterWrapper::extractSelfFromOffer)
                .map(SimplifiedOfferParameter::getParamId))
                .contains(VENDOR.getId());
    }

    @Test
    public void actualizeRemoveAllDcpSpecialParameters() {
        List<SimplifiedOfferParameter> simplifiedOfferParameters = Arrays.asList(
                SimplifiedOfferParameter.forOffer(NAME.getId(), NAME.getXslName(), NAME.getXslName(),
                        OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(URL.getId(), URL.getXslName(), URL.getXslName(),
                        OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(VENDOR_CODE.getId(), VENDOR_CODE.getXslName(),
                        VENDOR_CODE.getXslName(), OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(BARCODE.getId(), BARCODE.getXslName(), BARCODE.getXslName(),
                        OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(VENDOR.getId(), VENDOR.getXslName(), VENDOR.getXslName(),
                        OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(DESCRIPTION.getId(), DESCRIPTION.getXslName(),
                        DESCRIPTION.getXslName(), OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(NEW_PARAM_ID, NEW_PARAM_NAME, NEW_PARAM_NAME,
                        OfferParameterType.STRING)
        );
        when(categoryData.containsParam(NAME.getId())).thenReturn(true);
        when(categoryData.getParamById(NAME.getId()))
                .thenReturn(MboParameters.Parameter.newBuilder()
                        .setId(NAME.getId()).setXslName(NAME.getXslName()).build());
        when(categoryData.containsParam(URL.getId())).thenReturn(true);
        when(categoryData.getParamById(URL.getId()))
                .thenReturn(MboParameters.Parameter.newBuilder()
                        .setId(URL.getId()).setXslName(URL.getXslName()).build());
        when(categoryData.containsParam(VENDOR_CODE.getId())).thenReturn(true);
        when(categoryData.getParamById(VENDOR_CODE.getId()))
                .thenReturn(MboParameters.Parameter.newBuilder()
                        .setId(VENDOR_CODE.getId()).setXslName(VENDOR_CODE.getXslName()).build());
        when(categoryData.containsParam(BARCODE.getId())).thenReturn(true);
        when(categoryData.getParamById(BARCODE.getId()))
                .thenReturn(MboParameters.Parameter.newBuilder()
                        .setId(BARCODE.getId()).setXslName(BARCODE.getXslName()).build());
        when(categoryData.containsParam(VENDOR.getId())).thenReturn(true);
        when(categoryData.getParamById(VENDOR.getId()))
                .thenReturn(MboParameters.Parameter.newBuilder()
                        .setId(VENDOR.getId()).setXslName(VENDOR.getXslName()).build());
        when(categoryData.containsParam(DESCRIPTION.getId())).thenReturn(true);
        when(categoryData.getParamById(DESCRIPTION.getId()))
                .thenReturn(MboParameters.Parameter.newBuilder()
                        .setId(DESCRIPTION.getId()).setXslName(DESCRIPTION.getXslName()).build());
        when(categoryData.containsParam(NEW_PARAM_ID)).thenReturn(true);
        when(categoryData.getParamById(NEW_PARAM_ID))
                .thenReturn(MboParameters.Parameter.newBuilder()
                        .setId(NEW_PARAM_ID).setXslName(NEW_PARAM_NAME).build());

        offer = OffersGenerator.generate(simplifiedOfferParameters);

        List<MarketParameterValueWrapper> filteredParams =
                OfferParametersActualizer.actualize(offer, categoryData, false);
        assertThat(filteredParams).hasSize(1);
        assertThat(filteredParams.get(0).getParamId()).isEqualTo(NEW_PARAM_ID);


        filteredParams = OfferParametersActualizer.actualize(offer, categoryData, true);
        assertThat(filteredParams).hasSize(0);
    }


    @Test
    public void getParamsFromOfferRawReturnsOnlyOfferParamsWhenProductionEnvironment() {
        long paramId = 1L;
        long formalizedParam = 2L;

        OfferParametersActualizer offerParametersActualizer = new OfferParametersActualizer("production", "");

        offer = generateOfferWithFormalizedParams(
                Collections.singletonList(generateParam(paramId, "name",
                        DataCampContentMarketParameterValue.MarketValueType.ENUM,
                        "opt1", 3L)),
                Collections.singletonList(generateFormalizedParam(formalizedParam,
                        UltraController.FormalizedParamType.NUMERIC, 4.0))
        );

        List<MarketParameterValueWrapper> result = OfferParametersActualizer.getParamsFromOfferRaw(offer, categoryData);
        List<Long> paramIds = result.stream().map(MarketParameterValueWrapper::getParamId).collect(Collectors.toList());

        assertThat(paramIds).hasSize(1);
        assertThat(paramIds).contains(paramId);
        assertThat(paramIds).doesNotContain(formalizedParam);
    }

    @Test
    public void getParamsFromOfferRawReturnsBothOfferAndFormalizedParamsWhenTestingEnvironment() {
        long paramId = 1L;
        long formalizedParam = 2L;

        when(categoryData.containsParam(formalizedParam)).thenReturn(true);
        when(categoryData.getParamById(formalizedParam)).thenReturn(MboParameters.Parameter.newBuilder()
                .setNameForPartnerNew("formalized").build());

        OfferParametersActualizer offerParametersActualizer = new OfferParametersActualizer("testing", "");

        offer = generateOfferWithFormalizedParams(
                Collections.singletonList(
                        generateParam(paramId, "name", DataCampContentMarketParameterValue.MarketValueType.ENUM,
                                "opt1", 3L)
                ),
                Collections.singletonList(
                        generateFormalizedParam(formalizedParam, UltraController.FormalizedParamType.NUMERIC, 5.0)
                )
        );

        List<MarketParameterValueWrapper> result = OfferParametersActualizer.getParamsFromOfferRaw(offer, categoryData);
        List<Long> paramIds = result.stream().map(MarketParameterValueWrapper::getParamId).collect(Collectors.toList());

        assertThat(paramIds).hasSize(2);
        assertThat(paramIds).containsOnly(paramId, formalizedParam);
        assertThat(result.stream().filter(p -> p.getParamId() == paramId)
                .map(MarketParameterValueWrapper::getMarketParameterValueWrapperType)
                .allMatch(t -> t.equals(MarketParameterValueWrapper.MarketParameterValueWrapperType.OFFER))).isTrue();

        assertThat(result.stream().filter(p -> p.getParamId() == formalizedParam)
                .map(MarketParameterValueWrapper::getMarketParameterValueWrapperType)
                .allMatch(t -> t.equals(MarketParameterValueWrapper.MarketParameterValueWrapperType.FORMALIZED))).isTrue();
    }

    @Test
    public void getParamsFromOfferRawReturnsWithoutSpetialParameters() {
        long paramId = 1L;
        long formalizedParam = 2L;

        when(categoryData.containsParam(formalizedParam)).thenReturn(true);
        when(categoryData.getParamById(formalizedParam)).thenReturn(MboParameters.Parameter.newBuilder()
                        .setXslName("mdm_weight_net")
                .setNameForPartnerNew("formalized").build());

        OfferParametersActualizer offerParametersActualizer = new OfferParametersActualizer("testing", "");

        offer = generateOfferWithFormalizedParams(
                Collections.singletonList(
                        generateParam(paramId, "name", DataCampContentMarketParameterValue.MarketValueType.ENUM,
                                "opt1", 3L)
                ),
                Collections.singletonList(
                        generateFormalizedParam(formalizedParam, UltraController.FormalizedParamType.NUMERIC, 5.0)
                )
        );

        List<MarketParameterValueWrapper> result = OfferParametersActualizer.getParamsFromOfferRaw(offer, categoryData);
        List<Long> paramIds = result.stream().map(MarketParameterValueWrapper::getParamId).collect(Collectors.toList());

        assertThat(paramIds).hasSize(1);
        assertThat(paramIds).containsOnly(paramId);
        assertThat(result.stream().filter(p -> p.getParamId() == paramId)
                .map(MarketParameterValueWrapper::getMarketParameterValueWrapperType)
                .allMatch(t -> t.equals(MarketParameterValueWrapper.MarketParameterValueWrapperType.OFFER))).isTrue();
    }

    @Test
    public void getParamsFromOfferRawReturnsOnlyOfferParamsWhenProductionEnvironmentProperParamSize() {
        long paramId = 1L;
        long formalizedParamId = 2L;

        OfferParametersActualizer offerParametersActualizer = new OfferParametersActualizer("production", "");

        offer = generateOfferWithFormalizedParams(
                Lists.newArrayList(
                        generateParam(paramId, "name",
                                DataCampContentMarketParameterValue.MarketValueType.ENUM, "opt3", 3L),
                        generateParam(paramId, "name",
                                DataCampContentMarketParameterValue.MarketValueType.ENUM, "opt5", 5L)
                ),
                Collections.singletonList(
                        generateFormalizedParam(formalizedParamId, UltraController.FormalizedParamType.NUMERIC, 4.0)
                )
        );

        List<MarketParameterValueWrapper> result = OfferParametersActualizer.getParamsFromOfferRaw(offer, categoryData);
        List<Long> paramIds = result.stream().map(MarketParameterValueWrapper::getParamId).collect(Collectors.toList());

        assertThat(paramIds).hasSize(2);
        assertThat(paramIds).contains(paramId);
        assertThat(paramIds).doesNotContain(formalizedParamId);
    }

    @Test
    public void getParamsFromOfferRawReturnsBothOfferAndFormalizedParamsWhenTestingEnvironmentProperParamSize() {
        long paramId = 1L;
        long formalizedParamId = 2L;

        when(categoryData.containsParam(formalizedParamId)).thenReturn(true);
        when(categoryData.getParamById(formalizedParamId)).thenReturn(MboParameters.Parameter.newBuilder()
                .setNameForPartnerNew("formalized").build());

        OfferParametersActualizer offerParametersActualizer = new OfferParametersActualizer("testing", "");

        offer = generateOfferWithFormalizedParams(
                Lists.newArrayList(
                        generateParam(paramId, "name",
                                DataCampContentMarketParameterValue.MarketValueType.ENUM,
                                "opt3", 3L),
                        generateParam(paramId, "name",
                                DataCampContentMarketParameterValue.MarketValueType.ENUM,
                                "opt5", 5L)),
                Collections.singletonList(generateFormalizedParam(formalizedParamId,
                        UltraController.FormalizedParamType.NUMERIC, 5.0))
        );

        List<MarketParameterValueWrapper> result = OfferParametersActualizer.getParamsFromOfferRaw(offer, categoryData);

        List<Long> paramIds = result.stream().map(MarketParameterValueWrapper::getParamId).collect(Collectors.toList());

        assertThat(paramIds).hasSize(3);

        assertThat(paramIds).containsOnly(paramId, formalizedParamId);
        assertThat(result.stream().filter(p -> p.getParamId() == paramId)
                .map(MarketParameterValueWrapper::getMarketParameterValueWrapperType)
                .allMatch(t -> t.equals(MarketParameterValueWrapper.MarketParameterValueWrapperType.OFFER))).isTrue();

        assertThat(result.stream().filter(p -> p.getParamId() == formalizedParamId)
                .map(MarketParameterValueWrapper::getMarketParameterValueWrapperType)
                .allMatch(t -> t.equals(MarketParameterValueWrapper.MarketParameterValueWrapperType.FORMALIZED))).isTrue();
    }

    @Test
    public void getParamsFromOfferRawOnConflictOfOfferAndFormalizedParamsGetOfferParams() {
        long paramId = 1L;
        long formalizedParam = 1L;

        when(categoryData.containsParam(formalizedParam)).thenReturn(true);
        when(categoryData.getParamById(formalizedParam)).thenReturn(MboParameters.Parameter.newBuilder()
                .setNameForPartnerNew("formalized").build());

        OfferParametersActualizer offerParametersActualizer = new OfferParametersActualizer("testing", "");

        offer = generateOfferWithFormalizedParams(
                Collections.singletonList(generateParam(paramId, "name",
                        DataCampContentMarketParameterValue.MarketValueType.NUMERIC,
                        "5.0", 1L)),
                Collections.singletonList(generateFormalizedParam(formalizedParam,
                        UltraController.FormalizedParamType.NUMERIC, 5.0))
        );

        List<MarketParameterValueWrapper> result = OfferParametersActualizer.getParamsFromOfferRaw(offer, categoryData);
        List<Long> paramIds = result.stream().map(MarketParameterValueWrapper::getParamId).collect(Collectors.toList());

        assertThat(paramIds).hasSize(1);
        assertThat(paramIds).containsOnly(paramId);
        assertThat(result.stream().filter(p -> p.getParamId() == paramId)
                .map(MarketParameterValueWrapper::getMarketParameterValueWrapperType)
                .allMatch(t -> t.equals(MarketParameterValueWrapper.MarketParameterValueWrapperType.OFFER))).isTrue();

        assertThat(result.stream().filter(p -> p.getParamId() == formalizedParam)
                .map(MarketParameterValueWrapper::getMarketParameterValueWrapperType)
                .allMatch(t -> t.equals(MarketParameterValueWrapper.MarketParameterValueWrapperType.FORMALIZED))).isFalse();
    }

    @Test
    public void testThatNumberFormatIsNotTheSameAsBigdecimalWhenNotEquals() {
        HashMap<String, String> notEqualValues = new HashMap<>();
        notEqualValues.put("12", "12.2");
        notEqualValues.put("-12", "12.0");
        notEqualValues.put("12.0", "-12.0");

        notEqualValues.forEach((val1, val2) -> {
            DataCampContentMarketParameterValue.MarketParameterValue value1 = generateParam(1L,
                    "name", DataCampContentMarketParameterValue.MarketValueType.NUMERIC, val1, 1L);
            DataCampContentMarketParameterValue.MarketParameterValue value2 = generateParam(1L,
                    "name", DataCampContentMarketParameterValue.MarketValueType.NUMERIC, val2, 1L);
            DataCampContentMarketParameterValue.MarketParameterValue value1Sanitized =
                    OfferParametersActualizer.sanitizeMarketParameterValue(value1);
            DataCampContentMarketParameterValue.MarketParameterValue value2Sanitized =
                    OfferParametersActualizer.sanitizeMarketParameterValue(value2);

            assertThat(value1Sanitized.getValue().getNumericValue())
                    .isNotEqualTo(value2Sanitized.getValue().getNumericValue());

            assertThat(new BigDecimal(val1).compareTo(new BigDecimal(val2))).isNotEqualTo(0);
        });

    }

    @Test
    public void testThatNumberFormatIsTheSameAsBigdecimalWhenEquals() {
        HashMap<String, String> equalValues = new HashMap<>();
        equalValues.put("12", "12.0");
        equalValues.put("-12", "-12.0");
        equalValues.put("200000.0001", "200000.00010");
        equalValues.put("3000.0001", "03000.0001");
        equalValues.put("2000", "02000");
        equalValues.put("-4000", "-04000");

        equalValues.forEach((val1, val2) -> {
            DataCampContentMarketParameterValue.MarketParameterValue value1 = generateParam(1L,
                    "name", DataCampContentMarketParameterValue.MarketValueType.NUMERIC, val1, 1L);
            DataCampContentMarketParameterValue.MarketParameterValue value2 = generateParam(1L,
                    "name", DataCampContentMarketParameterValue.MarketValueType.NUMERIC, val2, 1L);
            DataCampContentMarketParameterValue.MarketParameterValue value1Sanitized =
                    OfferParametersActualizer.sanitizeMarketParameterValue(value1);
            DataCampContentMarketParameterValue.MarketParameterValue value2Sanitized =
                    OfferParametersActualizer.sanitizeMarketParameterValue(value2);

            assertThat(value1Sanitized.getValue().getNumericValue())
                    .isEqualTo(value2Sanitized.getValue().getNumericValue());

            assertThat(new BigDecimal(val1).compareTo(new BigDecimal(val2))).isEqualTo(0);
        });
    }


    @Test
    public void testThatParamsWithSpacesAtTheEndIsSanitizedPart() {
        DataCampContentMarketParameterValue.MarketParameterValue value1 = generateParam(1L,
                "name", DataCampContentMarketParameterValue.MarketValueType.STRING, "ромашка", 1L);
        DataCampContentMarketParameterValue.MarketParameterValue value2 = generateParam(1L,
                "name", DataCampContentMarketParameterValue.MarketValueType.STRING, "ромашка ", 1L);
        DataCampContentMarketParameterValue.MarketParameterValue value1Sanitized =
                OfferParametersActualizer.sanitizeMarketParameterValue(value1);
        DataCampContentMarketParameterValue.MarketParameterValue value2Sanitized =
                OfferParametersActualizer.sanitizeMarketParameterValue(value2);

        assertThat(value1Sanitized.getValue().getStrValue())
                .isEqualTo(value2Sanitized.getValue().getStrValue());
    }

    private DataCampOffer.Offer generateOfferWithFormalizedParams(
            List<DataCampContentMarketParameterValue.MarketParameterValue> marketParameterValueList,
            List<UltraController.FormalizedParamPosition> formalizedParamPositionList) {
        DataCampOfferMarketContent.MarketParameterValues.Builder paramValueBuilder =
                DataCampOfferMarketContent.MarketParameterValues.newBuilder();

        marketParameterValueList.forEach(paramValueBuilder::addParameterValues);

        return DataCampOffer.Offer.newBuilder()
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent
                                        .newBuilder()
                                        .setParameterValues(paramValueBuilder
                                                .build())
                                        .build()
                                ).build())
                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                .setIrData(DataCampOfferContent.EnrichedOfferSubset.newBuilder()
                                        .addAllConfidentParamsForPsku(formalizedParamPositionList)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private DataCampContentMarketParameterValue.MarketParameterValue generateParam
            (long paramId, String paramName, DataCampContentMarketParameterValue.MarketValueType valueType,
             Object value,
             long optionId) {
        DataCampContentMarketParameterValue.MarketParameterValue.Builder builder =
                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                        .setParamId(paramId)
                        .setParamName(paramName);

        switch (valueType) {
            case ENUM:
                if (!Objects.isNull(value)) {
                    builder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                            .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                            .setOptionId(optionId)
                            .setStrValue((String) value)
                            .build());
                }
                break;
            case NUMERIC_ENUM:
                if (!Objects.isNull(value)) {
                    builder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                            .setValueType(DataCampContentMarketParameterValue.MarketValueType.NUMERIC_ENUM)
                            .setOptionId(optionId)
                            .setStrValue((String) value)
                            .build());
                }
                break;
            case NUMERIC:
                builder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                        .setValueType(DataCampContentMarketParameterValue.MarketValueType.NUMERIC)
                        .setNumericValue((String) value)
                        .build());
                break;
            case BOOLEAN:
                builder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                        .setValueType(DataCampContentMarketParameterValue.MarketValueType.BOOLEAN)
                        .setBoolValue((Boolean) value)
                        .build());
                break;
            default:
                break;
        }

        return builder.build();
    }

    private UltraController.FormalizedParamPosition generateFormalizedParam(long paramId,
                                                                            UltraController.FormalizedParamType type,
                                                                            Object value) {
        UltraController.FormalizedParamPosition.Builder builder = UltraController.FormalizedParamPosition.newBuilder()
                .setParamId(Math.toIntExact(paramId));

        switch (type) {
            case ENUM:
                builder.setOptionId(Math.toIntExact((Long) value))
                        .setType(UltraController.FormalizedParamType.ENUM);
                break;
            case NUMERIC_ENUM:
                builder.setOptionId(Math.toIntExact((Long) value))
                        .setType(UltraController.FormalizedParamType.NUMERIC_ENUM);
                break;
            case NUMERIC:
                builder.setNumberValue((Double) value)
                        .setType(UltraController.FormalizedParamType.NUMERIC);
                break;
            case BOOLEAN:
                builder.setBooleanValue((Boolean) value)
                        .setType(UltraController.FormalizedParamType.BOOLEAN);
                break;
            default:
                break;
        }

        return builder.build();
    }

}
