package ru.yandex.market.partner.content.common.csku.judge;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.KnownParameters;
import ru.yandex.market.partner.content.common.csku.ModelFromOfferBuilder;
import ru.yandex.market.partner.content.common.csku.ModelGenerator;
import ru.yandex.market.partner.content.common.csku.OfferParameterType;
import ru.yandex.market.partner.content.common.csku.OffersGenerator;
import ru.yandex.market.partner.content.common.csku.SimplifiedOfferParameter;
import ru.yandex.market.partner.content.common.csku.util.ParameterCreator;
import ru.yandex.market.partner.content.common.csku.wrappers.BaseParameterWrapper;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.BARCODE;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.IS_CSKU;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.RAW_VENDOR;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.SHOP_SKU;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.USE_NAME_AS_TITLE;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.VENDOR;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.VENDOR_LINE;
import static ru.yandex.market.partner.content.common.csku.util.DcpSpecialParameterCreator.VENDOR_NOT_FOUND_OPTION_ID;
import static ru.yandex.market.partner.content.common.csku.util.OfferSpecialParameterCreator.IS_CSKU_TRUE_OPTION_ID;
import static ru.yandex.market.partner.content.common.csku.util.OfferSpecialParameterCreator.USE_NAME_AS_TITLE_TRUE_OPTION_ID;
import static ru.yandex.market.partner.content.common.csku.util.ParameterCreator.createBooleanParam;
import static ru.yandex.market.partner.content.common.csku.util.ParameterCreator.createEnumParam;
import static ru.yandex.market.partner.content.common.csku.util.ParameterCreator.createStringParam;

public class JudgeTest {

    private ModelFromOfferBuilder builder;
    private DataCampOffer.Offer offer;
    private final CategoryData categoryData = mock(CategoryData.class);
    private static final Long PARAM_ID_1 = 1L;
    private static final Long PARAM_ID_2 = 2L;
    private static final Long HYPOTHESIS_ID_1 = 3L;
    private static final String PARAM_NAME_1 = "new";
    private static final String PARAM_NAME_2 = "new2";
    private static final String VALUE_1 = "Some value";
    private static final String VALUE_2 = "Some value2";
    private static final String VALUE_3 = "Some value3";
    private static final String SHOP_SKU_VALUE = "Shop sku";
    private static final String GTIN_BARCODE = "9771144875007";
    private static final String GTIN_BARCODE2 = "9771144875006";
    private static final int SUPPLIER_ID = 123;
    private static final int ANOTHER_SUPPLIER_ID = 321;
    private static final Integer GROUP_ID = 14567;
    private static final int VENDOR_OPTION_ID = 123456;
    private static final String VENDOR_NAME = "Some vendor";
    private static final String ANOTHER_VENDOR_NAME = "Some other vendor";
    private static final String SOME_LINE = "Some line";
    private static final String SOME_OTHER_LINE = "Some other line";
    private static final int OPTION_ID1 = 11111;
    private static final int OPTION_ID2 = 2222;
    private static final List<SimplifiedOfferParameter> params = Arrays.asList(
            SimplifiedOfferParameter.forOffer(PARAM_ID_1, PARAM_NAME_1, VALUE_1, OfferParameterType.STRING)
    );
    private DataCampOffer.Offer.Builder offerBuilder;
    private Judge judge = new Judge();

    @Before
    public void init() {
        offerBuilder = OffersGenerator.generateOfferBuilder(params);
        when(categoryData.containsParam(PARAM_ID_1)).thenReturn(true);
        when(categoryData.containsParam(PARAM_ID_2)).thenReturn(true);
        when(categoryData.isSkuParameter(PARAM_ID_1)).thenReturn(true);
        when(categoryData.isSkuParameter(PARAM_ID_2)).thenReturn(true);
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
    }

    @Test
    public void whenOfferOfTheSameSupplierAllowEverything() {
        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        offer = offerBuilder.build();

        ModelStorage.Model.Builder sku = ModelGenerator.generateModelBuilder(params, SUPPLIER_ID,
                        ModelStorage.ModificationSource.VENDOR_OFFICE, 0)
                .setSupplierId(SUPPLIER_ID)
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());

        ModelStorage.Model build = sku.build();
        ModelData skuData = new ModelData(build, true, SHOP_SKU_VALUE);
        builder = ModelFromOfferBuilder.builder(build, skuData.isSku(), categoryData, SUPPLIER_ID);
        Map<BaseParameterWrapper, Decision> parameterWrapperToAllowedAction =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        parameterWrapperToAllowedAction.forEach((wrapper, decision) -> {
            if (!Action.NONE.equals(decision.getAllowedAction())) {
                wrapper.putValuesInSkuAndModel(builder);
            }
        });
        ModelStorage.Model updatedSku = builder.build();
        assertThat(updatedSku.getParameterValuesList()).hasSize(4);

        assertThat(updatedSku.getParameterValuesList()).containsExactlyInAnyOrder(
                createStringParam(PARAM_ID_1, PARAM_NAME_1, singletonList(VALUE_1), SUPPLIER_ID,
                        ModelStorage.ModificationSource.VENDOR_OFFICE, 0),
                createEnumParam(VENDOR.getId(), VENDOR.getXslName(), VENDOR_NOT_FOUND_OPTION_ID, SUPPLIER_ID,
                        ModelStorage.ModificationSource.VENDOR_OFFICE, 0),
                createBooleanParam(USE_NAME_AS_TITLE.getId(), USE_NAME_AS_TITLE.getXslName(),
                        USE_NAME_AS_TITLE_TRUE_OPTION_ID, true, SUPPLIER_ID,
                        ModelStorage.ModificationSource.VENDOR_OFFICE, 0),
                createBooleanParam(IS_CSKU.getId(),
                        IS_CSKU.getXslName(), IS_CSKU_TRUE_OPTION_ID, true, SUPPLIER_ID,
                        ModelStorage.ModificationSource.VENDOR_OFFICE, 0)
        );
    }

    @Test
    public void whenModelAndParameterHaveNoSupplierId() {
        offerBuilder = OffersGenerator.generateOfferBuilder(Arrays.asList(
                SimplifiedOfferParameter.forOffer(PARAM_ID_1, PARAM_NAME_1, VALUE_1, OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(PARAM_ID_2, PARAM_NAME_2, VALUE_3, OfferParameterType.STRING)
        ));
        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        offer = offerBuilder.build();

        ModelStorage.Model.Builder sku = ModelGenerator.generateModelBuilder(Collections.emptyList(), 0,
                        ModelStorage.ModificationSource.VENDOR_OFFICE, 0)
                .setSupplierId(0)
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
                        )
                )
        );
        ModelStorage.Model build = sku.build();
        ModelData skuData = new ModelData(build, true, SHOP_SKU_VALUE);
        builder = ModelFromOfferBuilder.builder(build, skuData.isSku(), categoryData, SUPPLIER_ID);
        assertThat(build.getParameterValuesList().get(0).getOwnerId()).isEqualTo(0);
        Map<BaseParameterWrapper, Decision> parameterWrapperToAllowedAction =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        Decision d1 = parameterWrapperToAllowedAction.get(parameterWrapperToAllowedAction.keySet().stream()
                .filter(w -> PARAM_ID_1.equals(w.getParamId())).findFirst().orElse(null));
        Decision d2 = parameterWrapperToAllowedAction.get(parameterWrapperToAllowedAction.keySet().stream()
                .filter(w -> PARAM_ID_2.equals(w.getParamId())).findFirst().orElse(null));

        assertThat(d1.getAllowedAction()).isEqualTo(Action.MODIFY);
        assertThat(d2.getAllowedAction()).isEqualTo(Action.CONFLICT);
        assertThat(d2.getReason().getMessage()).isEqualTo("Параметр 2 (new2) в модели 1 не может быть изменен, т.к. " +
                "значение уже задано партнёром 124, а владелец офера - 123");
    }

    @Test
    public void whenOfferOfIntruderAllowNothing() {
        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(ANOTHER_SUPPLIER_ID)
                .build());
        offer = offerBuilder.build();

        ModelStorage.Model.Builder sku = ModelGenerator.generateModelBuilder(params, SUPPLIER_ID,
                        ModelStorage.ModificationSource.VENDOR_OFFICE, 0)
                .setSupplierId(SUPPLIER_ID)
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());

        ModelStorage.ParameterValue vendorParameterValue = createStringParam(VENDOR.getId(),
                VENDOR.getXslName(), singletonList("100500"), SUPPLIER_ID,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 0);
        sku.addParameterValues(vendorParameterValue);

        ModelStorage.ParameterValue useNameAsTitle = createBooleanParam(USE_NAME_AS_TITLE.getId(),
                USE_NAME_AS_TITLE.getXslName(), USE_NAME_AS_TITLE_TRUE_OPTION_ID, true, SUPPLIER_ID,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 0);
        sku.addParameterValues(useNameAsTitle);

        ModelStorage.ParameterValue shopSkuParameterValue = createStringParam(SHOP_SKU.getId(),
                SHOP_SKU.getXslName(), singletonList(SHOP_SKU_VALUE), SUPPLIER_ID,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 0);
        sku.addParameterValues(shopSkuParameterValue);

        ModelStorage.ParameterValue isCsku = createBooleanParam(IS_CSKU.getId(),
                IS_CSKU.getXslName(), IS_CSKU_TRUE_OPTION_ID, true, SUPPLIER_ID,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 0);
        sku.addParameterValues(isCsku);

        ModelStorage.Model build = sku.build();
        ModelData modelData = new ModelData(build, true, SHOP_SKU_VALUE);
        Map<BaseParameterWrapper, Decision> parameterWrapperToAllowedAction =
                judge.calculateAllowedModelChanges(offer, modelData, categoryData, new HashSet<>());
        assertThat(parameterWrapperToAllowedAction.values()).hasSize(4);
        assertThat(parameterWrapperToAllowedAction.values().stream()
                .map(Decision::getAllowedAction)
                .collect(Collectors.toSet()))
                .containsExactly(Action.NONE);
    }


    @Test
    public void whenModelIsNewAddShopSkuParameter() {
        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        offer = offerBuilder.build();

        ModelStorage.Model.Builder sku = ModelGenerator.generateModelBuilder(params, SUPPLIER_ID,
                        ModelStorage.ModificationSource.VENDOR_OFFICE, 0)
                // новая модель
                .setId(-1)
                .setSupplierId(SUPPLIER_ID)
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());

        ModelStorage.Model build = sku.build();
        ModelData skuData = new ModelData(build, true, SHOP_SKU_VALUE);
        builder = ModelFromOfferBuilder.builder(build, skuData.isSku(), categoryData, SUPPLIER_ID);
        Map<BaseParameterWrapper, Decision> parameterWrapperToAllowedAction =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        parameterWrapperToAllowedAction.forEach((wrapper, decision) -> {
            if (!Action.NONE.equals(decision.getAllowedAction())) {
                wrapper.putValuesInSkuAndModel(builder);
            }
        });
        ModelStorage.Model updatedSku = builder.build();
        assertThat(updatedSku.getParameterValuesList()).contains(
                createStringParam(SHOP_SKU.getId(), SHOP_SKU.getXslName(), singletonList(SHOP_SKU_VALUE), SUPPLIER_ID
                        , ModelStorage.ModificationSource.VENDOR_OFFICE, 0)
        );
    }

    @Test
    public void whenEmptyModelThenAllowEverything() {
        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(ANOTHER_SUPPLIER_ID)
                .build());
        offer = offerBuilder.build();

        ModelStorage.Model sku = ModelStorage.Model.newBuilder()
                .setId(-1)
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name()).build();
        ModelData modelData = new ModelData(sku, true, SHOP_SKU_VALUE);
        Map<BaseParameterWrapper, Decision> parameterWrapperToAllowedAction =
                judge.calculateAllowedModelChanges(offer, modelData, categoryData, new HashSet<>());
        //Дополнительный параметр isCsku
        assertThat(parameterWrapperToAllowedAction.values()).hasSize(5);
        assertThat(parameterWrapperToAllowedAction.values().stream()
                .map(Decision::getAllowedAction)
                .collect(Collectors.toSet()))
                .containsOnly(Action.MODIFY);
    }

    @Test
    public void whenModelHasDoNotChangeParamThenDenyToChangeIt() {
        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        offer = offerBuilder.build();

        ModelStorage.Model.Builder sku = ModelStorage.Model.newBuilder()
                .setId(1)
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        ModelStorage.ParameterValue pv = createStringParam(PARAM_ID_1, PARAM_NAME_1,
                singletonList("100500"), SUPPLIER_ID, ModelStorage.ModificationSource.VENDOR_OFFICE, 0).toBuilder()
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build();

        sku.addParameterValues(pv);

        ModelStorage.Model build = sku.build();
        ModelData modelData = new ModelData(build, true, SHOP_SKU_VALUE);
        Map<BaseParameterWrapper, Decision> parameterWrapperToAllowedAction =
                judge.calculateAllowedModelChanges(offer, modelData, categoryData, new HashSet<>());
        assertThat(parameterWrapperToAllowedAction.values()).hasSize(4);
        assertThat(parameterWrapperToAllowedAction.entrySet().stream()
                .filter(entry -> entry.getKey().extractSelfFromOffer().getParamId().equals(PARAM_ID_1))
                .map(entry -> entry.getValue().getAllowedAction())
                .collect(Collectors.toSet()))
                .containsOnly(Action.NONE);
    }

    @Test
    public void whenMultiParamsThenCommonDecision() {
        ModelStorage.Model.Builder sku = ModelStorage.Model.newBuilder()
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValuesBuilder()
                .setParamId(PARAM_ID_1)
                .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
                .setOptionId(123)
                .setOwnerId(SUPPLIER_ID)
                .build();
        sku.addParameterValuesBuilder()
                .setParamId(PARAM_ID_1)
                .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
                .setOptionId(321)
                .setOwnerId(ANOTHER_SUPPLIER_ID)
                .build();
        when(categoryData.getParamById(PARAM_ID_1))
                .thenReturn(MboParameters.Parameter.newBuilder()
                        .setId(PARAM_ID_1)
                        .setMultivalue(true)
                        .build());
        Map<Long, Decision> allowedAction = judge.getAllowedAction(SUPPLIER_ID, sku.build(), categoryData);
        assertThat(allowedAction.get(PARAM_ID_1).isDenial()).isFalse();
    }

    @Test
    public void whenMultiHypothesisThenModify() {
        ModelStorage.Model.Builder sku = ModelStorage.Model.newBuilder()
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValueHypothesisBuilder()
                .setParamId(PARAM_ID_1)
                .addStrValue(MboParameters.Word.newBuilder().setName("бирюзовенький").build())
                .build();
        sku.addParameterValueHypothesisBuilder()
                .setParamId(PARAM_ID_1)
                .addStrValue(MboParameters.Word.newBuilder().setName("бирюзоватый").build())
                .build();
        when(categoryData.getParamById(PARAM_ID_1))
                .thenReturn(MboParameters.Parameter.newBuilder()
                        .setId(PARAM_ID_1)
                        .setMultivalue(true)
                        .build());
        Map<Long, Decision> allowedAction = judge.getAllowedAction(SUPPLIER_ID, sku.build(), categoryData);
        assertThat(allowedAction.get(PARAM_ID_1).isDenial()).isFalse();
    }

    @Test
    public void whenNonMultiHypothesisOfSameOwnerThenModify() {
        ModelStorage.Model.Builder sku = ModelStorage.Model.newBuilder()
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValueHypothesisBuilder()
                .setParamId(PARAM_ID_1)
                .addStrValue(MboParameters.Word.newBuilder().setName("бирюзовенький").build())
                .setOwnerId(SUPPLIER_ID)
                .build();
        Map<Long, Decision> allowedAction = judge.getAllowedAction(SUPPLIER_ID, sku.build(), categoryData);
        assertThat(allowedAction.get(PARAM_ID_1).isDenial()).isFalse();
    }

    @Test
    public void whenNonMultiHypothesisOfAnotherOwnerThenDeny() {
        ModelStorage.Model.Builder sku = ModelStorage.Model.newBuilder()
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValueHypothesisBuilder()
                .setParamId(PARAM_ID_1)
                .addStrValue(MboParameters.Word.newBuilder().setName("бирюзовенький").build())
                .setOwnerId(ANOTHER_SUPPLIER_ID)
                .build();
        Map<Long, Decision> allowedAction = judge.getAllowedAction(SUPPLIER_ID, sku.build(), categoryData);
        assertThat(allowedAction.get(PARAM_ID_1).isConflict()).isTrue();
    }

    @Test
    public void whenNewModelThenAllowAll() {
        ModelStorage.Model.Builder sku = ModelStorage.Model.newBuilder()
                .setId(-1)
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValueHypothesisBuilder()
                .setParamId(PARAM_ID_1)
                .addStrValue(MboParameters.Word.newBuilder().setName("бирюзовенький").build())
                .setOwnerId(ANOTHER_SUPPLIER_ID)
                .build();
        Map<Long, Decision> allowedAction = judge.getAllowedAction(SUPPLIER_ID, sku.build(), categoryData);
        assertThat(allowedAction).isEmpty();
    }

    @Test
    public void whenMskuThenDeny() {
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.SKU.name());
        sku.addParameterValueHypothesisBuilder()
                .setParamId(PARAM_ID_1)
                .addStrValue(MboParameters.Word.newBuilder().setName("бирюзовенький").build())
                .build();
        Map<Long, Decision> allowedAction = judge.getAllowedAction(SUPPLIER_ID, sku.build(), categoryData);
        assertThat(allowedAction.get(PARAM_ID_1).isDenial()).isTrue();
    }

    @Test
    public void whenNpBarcodeExistsThenNoDenial() {
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        Map<Long, Decision> allowedAction = judge.getAllowedAction(SUPPLIER_ID, sku.build(), categoryData);
        assertThat(allowedAction).isEmpty();
    }

    @Test
    public void whenNewBarcodeThenConflict() {
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValuesBuilder()
                .setParamId(KnownParameters.BARCODE.getId())
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(GTIN_BARCODE2).build())
                .setOwnerId(SUPPLIER_ID)
                .build();

        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());

        offerBuilder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                .setBarcode(DataCampOfferMeta.StringListValue.newBuilder().addValue(GTIN_BARCODE).build());
        offer = offerBuilder.build();
        ModelData skuData = new ModelData(sku.build(), true, SHOP_SKU_VALUE);
        Map<BaseParameterWrapper, Decision> decisionMap =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        decisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(BARCODE.getId()))
                .forEach(entry -> assertThat(entry.getValue().isConflict()).isTrue());
    }

    @Test
    public void whenBarcodeIsFormerNoConflict() {
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValuesBuilder()
                .setParamId(KnownParameters.BARCODE.getId())
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(GTIN_BARCODE2).build())
                .setOwnerId(SUPPLIER_ID)
                .build();
        sku.addParameterValuesBuilder()
                .setParamId(KnownParameters.FORMER_BARCODE.getId())
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(GTIN_BARCODE))
                .setOwnerId(SUPPLIER_ID)
                .build();

        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());

        offerBuilder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                .setBarcode(DataCampOfferMeta.StringListValue.newBuilder().addValue(GTIN_BARCODE).build());
        offer = offerBuilder.build();
        ModelData skuData = new ModelData(sku.build(), true, SHOP_SKU_VALUE);
        Map<BaseParameterWrapper, Decision> decisionMap =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        decisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(BARCODE.getId()))
                .forEach(entry -> assertThat(entry.getValue().isDenial()).isTrue());
    }

    @Test
    public void whenOldBarcodeThenNoChange() {
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValuesBuilder()
                .setParamId(KnownParameters.BARCODE.getId())
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(GTIN_BARCODE).build())
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(GTIN_BARCODE2).build())
                .setOwnerId(SUPPLIER_ID)
                .build();

        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        offerBuilder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                .setBarcode(DataCampOfferMeta.StringListValue.newBuilder().addValue(GTIN_BARCODE).build());
        offer = offerBuilder.build();
        ModelData skuData = new ModelData(sku.build(), true, SHOP_SKU_VALUE);
        Map<BaseParameterWrapper, Decision> decisionMap =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        decisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(BARCODE.getId()))
                .forEach(entry -> assertThat(entry.getValue().isDenial()).isTrue());
    }

    @Test
    public void testWhenSamePartnerRawVendorIsAllowedToBeChanged() {
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValuesBuilder()
                .setParamId(VENDOR.getId())
                .setOptionId(123456)
                .setOwnerId(SUPPLIER_ID)
                .build();

        MboParameters.Parameter vendorCategoryParameter = MboParameters.Parameter.newBuilder()
                .setId(VENDOR.getId())
                .addOption(MboParameters.Option.newBuilder()
                        .setId(VENDOR_OPTION_ID)
                        .addName(MboParameters.Word.newBuilder().setName(VENDOR_NAME).build())
                        .build())
                .build();

        when(categoryData.getParamById(ParameterValueComposer.VENDOR_ID)).thenReturn(vendorCategoryParameter);

        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        offerBuilder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                .setVendor(DataCampOfferMeta.StringValue.newBuilder().setValue(ANOTHER_VENDOR_NAME).build());
        offer = offerBuilder.build();

        ModelData skuData = new ModelData(sku.build(), true, SHOP_SKU_VALUE);
        Map<BaseParameterWrapper, Decision> decisionMap =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        decisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(RAW_VENDOR.getId()))
                .forEach(entry -> assertThat(entry.getValue().isModify()).isTrue());
    }

    @Test
    public void testWhenAnotherPartnerRawVendorIsNotAllowedToBeChanged() {
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValuesBuilder()
                .setParamId(VENDOR.getId())
                .setOptionId(123456)
                .setOwnerId(SUPPLIER_ID)
                .build();

        MboParameters.Parameter vendorCategoryParameter = MboParameters.Parameter.newBuilder()
                .setId(VENDOR.getId())
                .addOption(MboParameters.Option.newBuilder()
                        .setId(VENDOR_OPTION_ID)
                        .addName(MboParameters.Word.newBuilder().setName(VENDOR_NAME).build())
                        .build())
                .build();

        when(categoryData.getParamById(ParameterValueComposer.VENDOR_ID)).thenReturn(vendorCategoryParameter);

        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(ANOTHER_SUPPLIER_ID)
                .build());
        offerBuilder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                .setVendor(DataCampOfferMeta.StringValue.newBuilder().setValue(ANOTHER_VENDOR_NAME).build());
        offer = offerBuilder.build();

        ModelData skuData = new ModelData(sku.build(), true, SHOP_SKU_VALUE);
        Map<BaseParameterWrapper, Decision> decisionMap =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        decisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(RAW_VENDOR.getId()) ||
                        entry.getKey().getParamId().equals(VENDOR.getId()))
                .forEach(entry -> assertThat(entry.getValue().isDenial()).isTrue());
    }

    @Test
    public void testWhenHypothesisFromOnePartnerAndSameParamFromAnotherThenNoChange() {
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValuesBuilder()
                .setParamId(VENDOR_LINE.getId())
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(SOME_LINE).build())
                .setOwnerId(SUPPLIER_ID)
                .build();

        when(categoryData.containsParam(VENDOR_LINE.getId())).thenReturn(true);
        when(categoryData.isSkuParameter(VENDOR_LINE.getId())).thenReturn(true);
        when(categoryData.getParamById(VENDOR_LINE.getId())).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(VENDOR_LINE.getId())
                .setValueType(MboParameters.ValueType.STRING)
                .setXslName(VENDOR_LINE.getXslName())
                .build());

        List<SimplifiedOfferParameter> simplifiedOfferParameters = Collections.singletonList(
                SimplifiedOfferParameter.forOffer(VENDOR_LINE.getId(), VENDOR_LINE.getXslName(), SOME_LINE,
                        OfferParameterType.STRING));

        DataCampOffer.Offer.Builder offerBuilder = OffersGenerator.generateOfferBuilder(simplifiedOfferParameters);
        offer = offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(ANOTHER_SUPPLIER_ID)
                        .setOfferId(SHOP_SKU_VALUE))
                .build();

        ModelData skuData = new ModelData(sku.build(), true, SHOP_SKU_VALUE);
        Map<BaseParameterWrapper, Decision> decisionMap =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        decisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(VENDOR_LINE.getId()))
                .forEach(entry -> assertThat(entry.getValue().isDenial()).isTrue());
    }

    @Test
    public void testWhenHypothesisFromOnePartnerAndDifferentParamValueFromAnotherThenNoChange() {
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        sku.addParameterValuesBuilder()
                .setParamId(VENDOR_LINE.getId())
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(SOME_OTHER_LINE).build())
                .setOwnerId(SUPPLIER_ID)
                .build();

        when(categoryData.containsParam(VENDOR_LINE.getId())).thenReturn(true);
        when(categoryData.isSkuParameter(VENDOR_LINE.getId())).thenReturn(true);
        when(categoryData.getParamById(VENDOR_LINE.getId())).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(VENDOR_LINE.getId())
                .setValueType(MboParameters.ValueType.STRING)
                .setXslName(VENDOR_LINE.getXslName())
                .build());

        List<SimplifiedOfferParameter> simplifiedOfferParameters = Collections.singletonList(
                SimplifiedOfferParameter.forOffer(VENDOR_LINE.getId(), VENDOR_LINE.getXslName(), SOME_LINE,
                        OfferParameterType.STRING));

        DataCampOffer.Offer.Builder offerBuilder = OffersGenerator.generateOfferBuilder(simplifiedOfferParameters);
        offer = offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(ANOTHER_SUPPLIER_ID)
                        .setOfferId(SHOP_SKU_VALUE))
                .build();

        ModelData skuData = new ModelData(sku.build(), true, SHOP_SKU_VALUE);
        Map<BaseParameterWrapper, Decision> decisionMap =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        decisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(VENDOR_LINE.getId()))
                .forEach(entry -> assertThat(entry.getValue().isConflict()).isTrue());
    }

    @Test
    public void whenMigratedParamThenCopyDecisionForActualParam() {
        Map<Long, Decision> decisions = new HashMap<>();
        decisions.put(PARAM_ID_1, new Decision(Action.NONE));
        when(categoryData.getParamById(PARAM_ID_1)).thenReturn(MboParameters.Parameter
                .newBuilder()
                .setId(PARAM_ID_2).build());
        Map<Long, Decision> finalDecisionMap = judge.actualizeDecisions(decisions, categoryData);
        assertThat(finalDecisionMap).hasSize(2);
        assertThat(finalDecisionMap.get(PARAM_ID_2).isDenial()).isTrue();
    }

    @Test
    public void whenBothOldAndNewParamsThenDoNotCopyDecision() {
        Map<Long, Decision> decisions = new HashMap<>();
        decisions.put(PARAM_ID_1, new Decision(Action.NONE));
        decisions.put(PARAM_ID_2, new Decision(Action.CONFLICT));
        when(categoryData.getParamById(PARAM_ID_1)).thenReturn(MboParameters.Parameter
                .newBuilder()
                .setId(PARAM_ID_2).build());
        when(categoryData.getParamById(PARAM_ID_2)).thenReturn(MboParameters.Parameter
                .newBuilder()
                .setId(PARAM_ID_2).build());
        Map<Long, Decision> finalDecisionMap = judge.actualizeDecisions(decisions, categoryData);
        assertThat(finalDecisionMap).hasSize(2);
        assertThat(finalDecisionMap.get(PARAM_ID_2).isConflict()).isTrue();
    }

    @Test
    public void whenModelWithMigratedParamThenSubstituteWithGlobalParam() {
        ModelStorage.ParameterValue pv = ModelStorage.ParameterValue.newBuilder()
                .setParamId(PARAM_ID_1)
                .setXslName(PARAM_NAME_1)
                .setOptionId(OPTION_ID1)
                .setValueType(MboParameters.ValueType.ENUM)
                .build();
        when(categoryData.getParamById(PARAM_ID_1)).thenReturn(MboParameters.Parameter
                .newBuilder()
                .setId(PARAM_ID_2)
                .setXslName(PARAM_NAME_2)
                .addOption(MboParameters.Option.newBuilder().setId(OPTION_ID2).build())
                .build());
        when(categoryData.getMigratedOptionId(PARAM_ID_1, OPTION_ID1)).thenReturn(Long.valueOf(OPTION_ID2));

        ModelStorage.ParameterValueHypothesis hypothesis = ModelStorage.ParameterValueHypothesis.newBuilder()
                .setParamId(HYPOTHESIS_ID_1)
                .build();
        when(categoryData.getParamById(HYPOTHESIS_ID_1)).thenReturn(MboParameters.Parameter
                .newBuilder()
                .setId(PARAM_ID_2)
                .setXslName(PARAM_NAME_2)
                .build());

        ModelStorage.Model.Builder sku = ModelGenerator.generateModelBuilder(Collections.emptyList(), SUPPLIER_ID,
                        ModelStorage.ModificationSource.VENDOR_OFFICE, 0)
                .setSupplierId(SUPPLIER_ID)
                .addParameterValues(pv)
                .addParameterValueHypothesis(hypothesis)
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        ModelStorage.Model updatedSku = judge.getModelWithUpdatedGlobalParams(sku.build(), categoryData);

        List<ModelStorage.ParameterValue> parameterValuesList = updatedSku.getParameterValuesList();
        assertThat(parameterValuesList).hasSize(1);
        ModelStorage.ParameterValue resultingParameterValue = parameterValuesList.get(0);
        assertThat(resultingParameterValue.getParamId()).isEqualTo(PARAM_ID_2);
        assertThat(resultingParameterValue.getXslName()).isEqualTo(PARAM_NAME_2);
        assertThat(resultingParameterValue.getValueType()).isEqualTo(MboParameters.ValueType.ENUM);
        assertThat(resultingParameterValue.getOptionId()).isEqualTo(OPTION_ID2);

        List<ModelStorage.ParameterValueHypothesis> parameterValueHypothesisList =
                updatedSku.getParameterValueHypothesisList();
        assertThat(parameterValueHypothesisList).hasSize(1);
        ModelStorage.ParameterValueHypothesis valueHypothesis = parameterValueHypothesisList.get(0);
        assertThat(valueHypothesis.getParamId()).isEqualTo(PARAM_ID_2);
        assertThat(valueHypothesis.getXslName()).isEqualTo(PARAM_NAME_2);
    }
}
