package ru.yandex.market.partner.content.common.csku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampContentMarketParameterValue.MarketValueSource;
import Market.DataCamp.DataCampContentMarketParameterValue.MarketValueType;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.csku.judge.ModelData;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.VENDOR;

/**
 * Кейс: в SKU был заполнен enum параметр<br>
 * Тест: для SKU приходит новый оффер, в котором enum параметр изменен
 * Результат: в размерном параметре в результате всегда должно быть самое актуальное значение без "старых" (значение
 * может быть как гипотезой, так и нормальным параметром)
 */
@RunWith(Parameterized.class)
public class ModelFromOfferBuilderEnumParamsTest {

    private static final int SUPPLIER_ID = 123;
    private static final Integer GROUP_ID = 14567;
    private static final String SHOP_SKU_VALUE = "Shop sku";

    private static final long COLOR_PARAM_ID = 14474255L;
    private static final ColorInfo COLOR_1 = new ColorInfo(11111111L, "red");
    private static final ColorInfo COLOR_2 = new ColorInfo(22222222L, "blue");
    private static final List<ColorInfo> COLORS = List.of(COLOR_1, COLOR_2);

    private static CategoryData categoryData;

    @Parameterized.Parameters(name = "{index}: enum param multivalue={0}; {1} --> {2}; expected " +
            "params={3}; " +
            "expected hypothesis={4}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {false, List.of("red"), List.of("blue"), 1, 0},
                {true, List.of("red"), List.of("blue"), 1, 0},
                {false, List.of("red"), List.of("black"), 0, 1},
                {true, List.of("red"), List.of("black"), 0, 1},
                {false, List.of("black"), List.of("red"), 1, 0},
                {true, List.of("black"), List.of("red"), 1, 0},

                // явные multi-value
                {true, List.of("black"), List.of("red", "blue"), 2, 0},
                {true, List.of("red"), List.of("red", "blue"), 2, 0},
                {true, List.of("red", "blue"), List.of("red"), 1, 0},
                {true, List.of("red", "blue"), List.of("black"), 0, 1},
                {true, List.of("red"), List.of("red", "white"), 1, 1},
                {true, List.of("black"), List.of("red", "white"), 1, 1},
        });
    }

    @Parameterized.Parameter(0)
    public boolean isParamMultivalue;

    @Parameterized.Parameter(1)
    public List<String> oldColors;

    @Parameterized.Parameter(2)
    public List<String> newColors;

    @Parameterized.Parameter(3)
    public int paramsExpected;

    @Parameterized.Parameter(4)
    public int hypothesisExpected;

    @BeforeClass
    public static void init() {
        categoryData = mock(CategoryData.class);
        when(categoryData.containsParam(COLOR_PARAM_ID)).thenReturn(true);
        when(categoryData.isSkuParameter(COLOR_PARAM_ID)).thenReturn(true);

        var vendorCategoryParameter = generateEnumMboParameter(VENDOR.getId(), VENDOR.getXslName());
        when(categoryData.getParamById(ParameterValueComposer.VENDOR_ID)).thenReturn(vendorCategoryParameter.build());
    }

    @Test
    public void whenChangeVendorThenRecalculateSizeParam() {
        var sizeParameter = generateEnumColorParameter()
                .setMultivalue(isParamMultivalue)
                .build();
        when(categoryData.getParamById(COLOR_PARAM_ID)).thenReturn(sizeParameter);

        List<Optional<ColorInfo>> colorsInSku = new ArrayList<>();
        for (String color : oldColors) {
            colorsInSku.add(
                    COLORS.stream()
                            .filter(c -> c.name.equals(color))
                            .findFirst()
            );
        }
        // существующая SKU с сохраненным размером
        var skuBuilder = ModelStorage.Model.newBuilder()
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name());
        for (int i = 0; i < colorsInSku.size(); i++) {
            Optional<ColorInfo> colorInfo = colorsInSku.get(i);
            if (colorInfo.isPresent()) {
                skuBuilder.addParameterValues(
                    generateEnumParameterValue(COLOR_PARAM_ID, colorInfo.get().id, colorInfo.get().name)
                );
            } else {
                skuBuilder.addParameterValueHypothesis(
                    generateHypothesisParameterValue(COLOR_PARAM_ID, oldColors.get(i))
                );
            }
        }
        var sku = skuBuilder.build();

        var offerBuilder = DataCampOffer.Offer.newBuilder();
        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        var partnerBuilder = offerBuilder.getContentBuilder().getPartnerBuilder();
        newColors.forEach(
                newColor -> partnerBuilder.getMarketSpecificContentBuilder()
                        .getParameterValuesBuilder().addParameterValues(
                                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                        .setParamId(COLOR_PARAM_ID)
                                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                                .setStrValue(newColor)
                                                .setValueType(MarketValueType.ENUM)
                                                .build())
                                        .setValueSource(MarketValueSource.PARTNER)
                                        .build()
                        )
        );

        var offer = offerBuilder.build();

        ModelData skuData = new ModelData(sku, true, SHOP_SKU_VALUE);
        ModelFromOfferBuilder modelBuilder = ModelFromOfferBuilder.builder(sku, true, categoryData, SUPPLIER_ID);

        // рассчитываем какие параметры можно изменять
        Judge judge = new Judge();
        var decisionMap = judge.calculateAllowedModelChanges(offer, skuData, categoryData, new HashSet<>());
        decisionMap.forEach((wrapper, decision) -> {
            if (decision.isModify()) {
                wrapper.putValuesInSkuAndModel(modelBuilder);
            }
        });
        ModelStorage.Model resultingSku = modelBuilder.build();

        assertThat(
                resultingSku.getParameterValuesList()
                        .stream()
                        .filter(p -> p.getParamId() == COLOR_PARAM_ID)
                        .count()
        ).isEqualTo(paramsExpected);

        assertThat(
                resultingSku.getParameterValueHypothesisList()
                        .stream()
                        .filter(p -> p.getParamId() == COLOR_PARAM_ID)
                        .count()
        ).isEqualTo(hypothesisExpected);
    }

    private ModelStorage.ParameterValueHypothesis generateHypothesisParameterValue(long id, String strValue) {
        return ModelStorage.ParameterValueHypothesis.newBuilder()
                .setParamId(id)
                .addStrValue(MboParameters.Word.newBuilder().setName(strValue).build())
                .build();
    }

    private ModelStorage.ParameterValue generateEnumParameterValue(long id, long optionId, String strValue) {
        return ModelStorage.ParameterValue.newBuilder()
                .setParamId(id)
                .setOptionId((int) optionId)
                .setValueType(MboParameters.ValueType.ENUM)
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(strValue))
                .build();
    }

    private static MboParameters.Parameter.Builder generateEnumColorParameter() {
        var sizeParam = generateEnumMboParameter(COLOR_PARAM_ID, "color");
        COLORS.forEach(color -> sizeParam.addOption(MboParameters.Option.newBuilder()
                .setId(color.id)
                .addName(MboParameters.Word.newBuilder().setName(color.name).build())
                .build()));
        return sizeParam;
    }

    private static MboParameters.Parameter.Builder generateEnumMboParameter(long id, String xslName) {
        return MboParameters.Parameter.newBuilder()
                .setId(id)
                .setXslName(xslName)
                .setValueType(MboParameters.ValueType.ENUM);
    }

    private static class ColorInfo {
        private final long id;
        private final String name;

        public ColorInfo(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "Color[" + id + "," + name + ']';
        }
    }
}
