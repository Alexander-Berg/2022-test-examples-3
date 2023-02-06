package ru.yandex.market.partner.content.common.csku.judge;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.UltraControllerServiceData.UltraController;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.OfferParameterType;
import ru.yandex.market.partner.content.common.csku.OffersGenerator;
import ru.yandex.market.partner.content.common.csku.SimplifiedOfferParameter;
import ru.yandex.market.partner.content.common.csku.util.OfferParametersActualizer;
import ru.yandex.market.partner.content.common.csku.wrappers.BaseParameterWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FormalizationJudgeTest {

    private static final Logger log = LoggerFactory.getLogger(FormalizationJudgeTest.class);

    private final CategoryData categoryData = mock(CategoryData.class);
    private static final Long PARAM_ID = 1L;
    private static final String XSL_NAME = "face_color";
    private static final String SHOP_SKU_VALUE = "Shop sku";
    private static final int SUPPLIER_ID = 123;
    private static final int ANOTHER_SUPPLIER_ID = 321;
    private static final Integer GROUP_ID = 14567;
    private static final Long SKU_PARAM_VALUE = 0xFF0000L; // это "красненький";
    private static final Long OFFER_PARAM_VALUE = 0x0000FFL; // это "синенький";

    private final Judge judge = new Judge();

    @Before
    public void init() {
        OfferParametersActualizer actualizer = new OfferParametersActualizer("testing", "");
    }

    /*
    матрица тестирования /тестим на нумериках/ :
    A,B -- произвольные значения, C -- непустое значение, '-' -- пустое значение, '*' -- любое значение
    # m_formalized  o_formalized    m_sup_id    o_sup_id    m_value o_value m_weight    o_weight    action
    1 false         true            1           2           *       *       *           *           NONE
    2 false         true            1           1           *       *       *           *           NONE
    3 true          false           1           1           A       A       50          -           MODIFY
    4 true          false           1           1           A       B       50          -           MODIFY
    5 true          false           1           2           A       B       50          -           MODIFY
    6 true          true            1           1           A       C       50          *           MODIFY
    7 true          true            1           2           A       B       50          100         NONE
    8 true          true            1           2           A       B       50          20          NONE
    9 true          true            1           2           A       B       50          50          NONE
   10 true          true            1           1           A       -       *           *           NONE
   11 true          false           1           1           A       -       50          -           NONE
   12 true          false           1           2           -       *       -           50          MODIFY
     */

    @Test
    public void matrixTest1() {
//        1 false         true            1           2           *       *       *           *           NONE
        ModelStorage.Model.Builder sku = generateSKU(PARAM_ID, SKU_PARAM_VALUE, SUPPLIER_ID, false, 0);

        DataCampOffer.Offer offer = generateOffer(PARAM_ID, OFFER_PARAM_VALUE, ANOTHER_SUPPLIER_ID, true, 50);

        Map<BaseParameterWrapper, Decision> thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isDenial()).isTrue());
    }

    @Test
    public void matrixTest2() {
//        2 false         true            1           1           *       *       *           *           NONE
        ModelStorage.Model.Builder sku = generateSKU(PARAM_ID, SKU_PARAM_VALUE, SUPPLIER_ID, false, 0);

        DataCampOffer.Offer offer = generateOffer(PARAM_ID, OFFER_PARAM_VALUE, SUPPLIER_ID, true, 50);

        Map<BaseParameterWrapper, Decision> thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isDenial()).isTrue());
    }

    @Test
    public void matrixTest3() {
//        3 true          false           1           1           A       A       50          -           MODIFY
        ModelStorage.Model.Builder sku = generateSKU(PARAM_ID, SKU_PARAM_VALUE, SUPPLIER_ID, true, 50);

        DataCampOffer.Offer offer = generateOffer(PARAM_ID, SKU_PARAM_VALUE, SUPPLIER_ID, false, 0);

        Map<BaseParameterWrapper, Decision> thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isModify()).isTrue());
    }

    @Test
    public void matrixTest4() {
//        4 true          false           1           1           A       B       50          -           MODIFY
        ModelStorage.Model.Builder sku = generateSKU(PARAM_ID, SKU_PARAM_VALUE, SUPPLIER_ID, true, 50);

        DataCampOffer.Offer offer = generateOffer(PARAM_ID, OFFER_PARAM_VALUE, SUPPLIER_ID, false, 0);

        Map<BaseParameterWrapper, Decision> thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isModify()).isTrue());
    }

    @Test
    public void matrixTest5() {
//        5 true          false           1           2           A       B       50          -           MODIFY
        ModelStorage.Model.Builder sku = generateSKU(PARAM_ID, SKU_PARAM_VALUE, SUPPLIER_ID, true, 50);

        DataCampOffer.Offer offer = generateOffer(PARAM_ID, OFFER_PARAM_VALUE, ANOTHER_SUPPLIER_ID, false, 0);

        Map<BaseParameterWrapper, Decision> thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isModify()).isTrue());
    }

    @Test
    public void matrixTest6() {
//      6 true          true            1           1           A       C       50          *           MODIFY
        ModelStorage.Model.Builder sku = generateSKU(PARAM_ID, SKU_PARAM_VALUE, SUPPLIER_ID, true, 50);

        DataCampOffer.Offer offer = generateOffer(PARAM_ID, OFFER_PARAM_VALUE, SUPPLIER_ID, true, 20);

        Map<BaseParameterWrapper, Decision> thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isModify()).isTrue());

        offer = generateOffer(PARAM_ID, OFFER_PARAM_VALUE, SUPPLIER_ID, true, 50);

        thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isModify()).isTrue());

        offer = generateOffer(PARAM_ID, OFFER_PARAM_VALUE, SUPPLIER_ID, true, 100);

        thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isModify()).isTrue());

    }

    @Test
    public void matrixTest7() {
//        7 true          true            1           2           A       B       50          100         NONE
        ModelStorage.Model.Builder sku = generateSKU(PARAM_ID, SKU_PARAM_VALUE, SUPPLIER_ID, true, 50);

        DataCampOffer.Offer offer = generateOffer(PARAM_ID, OFFER_PARAM_VALUE, ANOTHER_SUPPLIER_ID, true, 100);

        Map<BaseParameterWrapper, Decision> thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isDenial()).isTrue());
    }

    @Test
    public void matrixTest8() {
//        8 true          true            1           2           A       B       50          20          NONE
        ModelStorage.Model.Builder sku = generateSKU(PARAM_ID, SKU_PARAM_VALUE, SUPPLIER_ID, true, 50);

        DataCampOffer.Offer offer = generateOffer(PARAM_ID, OFFER_PARAM_VALUE, ANOTHER_SUPPLIER_ID, true, 20);

        Map<BaseParameterWrapper, Decision> thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isDenial()).isTrue());
    }

    @Test
    public void matrixTest9() {
//        9 true          true            1           2           A       B       50          50          NONE
        ModelStorage.Model.Builder sku = generateSKU(PARAM_ID, SKU_PARAM_VALUE, SUPPLIER_ID, true, 50);

        DataCampOffer.Offer offer = generateOffer(PARAM_ID, OFFER_PARAM_VALUE, ANOTHER_SUPPLIER_ID, true, 50);

        Map<BaseParameterWrapper, Decision> thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isDenial()).isTrue());
    }

    @Test
    public void matrixTest10() {
//   10   true          true            1           1           A       -       *           *           NONE
        ModelStorage.Model.Builder sku = generateSKU(PARAM_ID, SKU_PARAM_VALUE, SUPPLIER_ID, true, 50);

        DataCampOffer.Offer offer = generateOffer(PARAM_ID, null, ANOTHER_SUPPLIER_ID, true, 100);

        Map<BaseParameterWrapper, Decision> thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isDenial()).isTrue());
    }

    @Test
    public void matrixTest11() {
//     11 true          false           1           1           A       -       50          -           NONE
        ModelStorage.Model.Builder sku = generateSKU(PARAM_ID, SKU_PARAM_VALUE, SUPPLIER_ID, true, 50);

        DataCampOffer.Offer offer = generateOffer(PARAM_ID, null, ANOTHER_SUPPLIER_ID, false, 0);

        Map<BaseParameterWrapper, Decision> thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isDenial()).isTrue());
    }

    @Test
    public void matrixTest12() {
//      12 true          false           1           2           -       *       -           50          MODIFY
        ModelStorage.Model.Builder sku = generateSKU(PARAM_ID, null, SUPPLIER_ID, false, 50);

        DataCampOffer.Offer offer = generateOffer(PARAM_ID, OFFER_PARAM_VALUE, ANOTHER_SUPPLIER_ID, true, 50);

        Map<BaseParameterWrapper, Decision> thisDecisionMap = getThisDecisionMap(sku, offer);

        log.debug("this decision: {}", thisDecisionMap);

        assertThat(thisDecisionMap.size() == 1).isTrue();

        thisDecisionMap.entrySet().stream()
                .filter(entry -> entry.getKey().getParamId().equals(PARAM_ID))
                .forEach(entry -> assertThat(entry.getValue().isModify()).isTrue());
    }

    private ModelStorage.Model.Builder generateSKU(Long paramId, Long skuParamValue, int supplierId, boolean formalized,
                                                   int weight) {
        // ---- sku gen
        ModelStorage.Model.Builder sku =
                ModelStorage.Model.newBuilder().setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());

        if (skuParamValue != null) {
            ModelStorage.ParameterValue.Builder pvbuilder = sku.addParameterValuesBuilder();
            pvbuilder
                    .setParamId(paramId)
                    .setValueType(MboParameters.ValueType.NUMERIC)
                    .setNumericValue("" + skuParamValue)
//                .setValueType(MboParameters.ValueType.STRING)
//                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("" + skuParamValue).build())
                    .setOwnerId(supplierId);

            if (formalized) {
                pvbuilder
                        .setValueSource(ModelStorage.ModificationSource.FORMALIZATION)
                        .setConfidence(weight);
            }
        }

        log.debug("sku: {}", sku);

        when(categoryData.containsParam(paramId)).thenReturn(true);
        when(categoryData.isSkuParameter(paramId)).thenReturn(true);
        when(categoryData.getParamById(paramId)).thenReturn(
                numericCategoryData(paramId)
//                enumCategoryData(paramId)
        );
        log.debug("category_data: {}", categoryData.getParamById(paramId));

        return sku;
    }

    @NotNull
    private MboParameters.Parameter enumCategoryData(Long paramId) {
        return MboParameters.Parameter.newBuilder()
                // пока так, т.к. надо уточнять/править логику в
                // https://a.yandex-team.ru/arc_vcs/market/ir/autogeneration/partner-content-common/src/main/java/ru/yandex/market/partner/content/common/csku/util/OfferParametersActualizer.java?rev=r9361741#L198
                .addOption(
                        MboParameters.Option.newBuilder().setId(SKU_PARAM_VALUE)
                                .addName(
                                        MboParameters.Word.newBuilder().setName("model option")
                                )
                                // обязателен ли category_data?
                                .addAlias(MboParameters.EnumAlias.newBuilder()
                                        .setAlias(MboParameters.Word.newBuilder().setName("model alias"))
                                )
                                .build()
                )
                .addOption(
                        MboParameters.Option.newBuilder().setId(OFFER_PARAM_VALUE)
                                .addName(
                                        MboParameters.Word.newBuilder().setName("offer option")
                                )
                                .addAlias(MboParameters.EnumAlias.newBuilder()
                                        .setAlias(MboParameters.Word.newBuilder().setName("offer alias"))
                                )
                                .build()
                )
                .setId(paramId)
                .setValueType(MboParameters.ValueType.ENUM)
                .setXslName(XSL_NAME)
                .build();
    }

    @NotNull
    private MboParameters.Parameter numericCategoryData(Long paramId) {
        return MboParameters.Parameter.newBuilder()
                .setId(paramId)
                .setValueType(MboParameters.ValueType.NUMERIC)
                .setXslName(XSL_NAME)
                .build();
    }


    private DataCampOffer.Offer generateOffer(long paramId, Long paramValue, int supplierId, boolean formalized,
                                              int weight) {
        // ---- offer gen
        DataCampOffer.Offer.Builder offerBuilder;
        if (formalized) {
            offerBuilder = DataCampOffer.Offer.newBuilder();
//            addEnumFormalization(offerBuilder, paramId, paramValue, weight);
            addNumericFormalization(offerBuilder, paramId, paramValue == null ? null : paramValue.doubleValue(),
                    weight);
        } else {
            List<SimplifiedOfferParameter> simplifiedOfferParameters = Collections.singletonList(
                    SimplifiedOfferParameter.forOffer(paramId, XSL_NAME, paramValue == null ? "" : "" + paramValue,
//                            OfferParameterType.OPTION
                            OfferParameterType.STRING
                    ));

            offerBuilder = OffersGenerator.generateOfferBuilder(simplifiedOfferParameters);
        }
        DataCampOffer.Offer offer = offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(supplierId)
                        .setOfferId(SHOP_SKU_VALUE))
                .build();

        log.debug("offer: {}", offer);
        return offer;
    }

    private void addEnumFormalization(DataCampOffer.Offer.Builder offerBuilder,
                                      Long paramId,
                                      Long formalizedOptionId,
                                      int weight) {
        UltraController.FormalizedParamPosition formalizedParamPosition =
                UltraController.FormalizedParamPosition.newBuilder()
                        .setParamId(paramId.intValue())
                        .setWeight(weight)
                        .setType(UltraController.FormalizedParamType.ENUM)
                        .setOptionId(formalizedOptionId.intValue())
                        .build();
        offerBuilder.getContentBuilder().getMarketBuilder().getIrDataBuilder()
                .addConfidentParamsForPsku(formalizedParamPosition);

    }

    private void addNumericFormalization(DataCampOffer.Offer.Builder offerBuilder,
                                         Long paramId,
                                         Double formalizedValue,
                                         int weight) {
        UltraController.FormalizedParamPosition.Builder formalizedParamPosition =
                UltraController.FormalizedParamPosition.newBuilder()
                        .setParamId(paramId.intValue())
                        .setWeight(weight)
                        .setType(UltraController.FormalizedParamType.NUMERIC);
        if (formalizedValue != null) {
            formalizedParamPosition.setNumberValue(formalizedValue);
        }
        offerBuilder.getContentBuilder().getMarketBuilder().getIrDataBuilder()
                .addConfidentParamsForPsku(formalizedParamPosition);

    }

    private void addStringFormalization(DataCampOffer.Offer.Builder offerBuilder,
                                        Long paramId,
                                        Double formalizedValue,
                                        int weight) {
        UltraController.FormalizedParamPosition formalizedParamPosition =
                UltraController.FormalizedParamPosition.newBuilder()
                        .setParamId(paramId.intValue())
                        .setWeight(weight)
                        .setType(UltraController.FormalizedParamType.NUMERIC)
                        .setNumberValue(formalizedValue)
                        .build();
        offerBuilder.getContentBuilder().getMarketBuilder().getIrDataBuilder()
                .addConfidentParamsForPsku(formalizedParamPosition);

    }

    @NotNull
    private Map<BaseParameterWrapper, Decision> getThisDecisionMap(ModelStorage.Model.Builder sku,
                                                                   DataCampOffer.Offer offer) {
        ModelData skuData = new ModelData(sku.build(), true, SHOP_SKU_VALUE);
        Map<BaseParameterWrapper, Decision> decisionMap =
                judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());

        Map<BaseParameterWrapper, Decision> thisDecisionMap =
                decisionMap.entrySet().stream().filter(e -> Objects.equals(e.getKey().getParamId(), PARAM_ID))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return thisDecisionMap;
    }


}
