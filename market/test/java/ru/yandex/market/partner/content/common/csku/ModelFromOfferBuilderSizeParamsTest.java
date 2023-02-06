package ru.yandex.market.partner.content.common.csku;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampContentMarketParameterValue.MarketValueSource;
import Market.DataCamp.DataCampContentMarketParameterValue.MarketValueType;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.db.NamedParamRestriction;
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
 * Кейс: в SKU был заполнен вендор и значение размера из сетки вендора (optionId). <br>
 * Тест: для SKU приходит новый оффер, в котором могут быть изменены вендор или значение размера
 * Результат: в размерном параметре в результате всегда должно быть самое актуальное значение без "старых" (значение
 * может быть как гипотезой, так и нормальным параметром)
 */
@RunWith(Parameterized.class)
public class ModelFromOfferBuilderSizeParamsTest {

    private static final int SUPPLIER_ID = 123;
    private static final Integer GROUP_ID = 14567;
    private static final String SHOP_SKU_VALUE = "Shop sku";

    private static final long SIZE_PARAM_ID = 14474255L;

    private static final VendorInfo VENDOR_1 = new VendorInfo(
            13, "First vendor", Map.of((long) 11111111, "XS", (long) 22222222, "S")
    );
    private static final VendorInfo VENDOR_2 = new VendorInfo(
            14, "Second vendor", Map.of((long) 33333333, "XS")
    );
    private static final VendorInfo VENDOR_3 = new VendorInfo(
            15, "Third vendor", Map.of()
    );
    private static final List<VendorInfo> VENDORS = List.of(VENDOR_1, VENDOR_2, VENDOR_3);
    private static CategoryData categoryData;

    @Parameterized.Parameters(name = "{index}: sizeParam multivalue={0}; {1}[{2}] --> {3}[{4}]; expected " +
            "params={5}; " +
            "expected hypothesis={6}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // смена вендора, но размер не меняется
                {false, VENDOR_2, "XS", VENDOR_1, "XS", 1, 0},
                {true, VENDOR_2, "XS", VENDOR_1, "XS", 1, 0},
                // ничего не меняется
                {false, VENDOR_2, "XS", VENDOR_2, "XS", 1, 0},
                {true, VENDOR_2, "XS", VENDOR_2, "XS", 1, 0},
                // смена вендора и размера
                {false, VENDOR_2, "XS", VENDOR_1, "S", 1, 0},
                {true, VENDOR_2, "XS", VENDOR_1, "S", 1, 0},

                // смена размера
                {true, VENDOR_1, "XS", VENDOR_1, "S", 1, 0},

                // смена вендора, но размера нет у нового вендора
                {false, VENDOR_1, "XS", VENDOR_3, "XS", 0, 1},
                {true, VENDOR_1, "XS", VENDOR_3, "XS", 0, 1},
        });
    }

    @Parameterized.Parameter(0)
    public boolean isSizeParamMultivalue;

    @Parameterized.Parameter(1)
    public VendorInfo oldVendor;

    @Parameterized.Parameter(2)
    public String oldSize;

    @Parameterized.Parameter(3)
    public VendorInfo newVendor;

    @Parameterized.Parameter(4)
    public String newSize;

    @Parameterized.Parameter(5)
    public int sizeParamsExpected;

    @Parameterized.Parameter(6)
    public int hypothesisExpected;

    @BeforeClass
    public static void init() {
        categoryData = mock(CategoryData.class);
        when(categoryData.containsParam(SIZE_PARAM_ID)).thenReturn(true);
        when(categoryData.isSkuParameter(SIZE_PARAM_ID)).thenReturn(true);
        when(categoryData.getSizeParamIds()).thenReturn(new LongArraySet(List.of(SIZE_PARAM_ID)));


        when(categoryData.getNamedParamRestrictions()).thenReturn(List.of(
                new NamedParamRestriction("dummy", VENDOR.getId(), SIZE_PARAM_ID,
                        new Long2ObjectOpenHashMap<>(
                                VENDORS.stream()
                                        .collect(Collectors.toMap(v -> v.id,
                                                v -> new LongArraySet(v.sizeOptions.keySet())))
                        )
                )
        ));

        var vendorCategoryParameter = generateEnumMboParameter(VENDOR.getId(), VENDOR.getXslName());
        VENDORS.forEach(v -> vendorCategoryParameter.addOption(
                MboParameters.Option.newBuilder()
                        .setId(v.id)
                        .addName(MboParameters.Word.newBuilder().setName(v.name).build())
                        .build()
        ));
        when(categoryData.getParamById(ParameterValueComposer.VENDOR_ID)).thenReturn(vendorCategoryParameter.build());
    }

    @Test
    public void whenChangeVendorThenRecalculateSizeParam() {
        var sizeParameter = generateSizeParameter()
                .setMultivalue(isSizeParamMultivalue)
                .build();
        when(categoryData.getParamById(SIZE_PARAM_ID)).thenReturn(sizeParameter);

        Map.Entry<Long, String> optionEntry = oldVendor.sizeOptions.entrySet().stream()
                .filter(entry -> entry.getValue().equals(oldSize))
                .findFirst().get();
        // существующая SKU с сохраненным размером
        var sku = ModelStorage.Model.newBuilder()
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .addParameterValues(generateEnumParameterValue(SIZE_PARAM_ID, optionEntry.getKey(),
                        optionEntry.getValue()))
                .addParameterValues(generateEnumParameterValue(VENDOR.getId(), oldVendor.id, oldVendor.name))
                .build();


        var offerBuilder = DataCampOffer.Offer.newBuilder();
        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers
                .newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .build());
        var partnerBuilder = offerBuilder.getContentBuilder().getPartnerBuilder();
        partnerBuilder.getMarketSpecificContentBuilder()
                .getParameterValuesBuilder()
                .addParameterValues(
                        DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                .setParamId(SIZE_PARAM_ID)
                                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                        .setStrValue(newSize)
                                        .setValueType(MarketValueType.ENUM)
                                        .build())
                                .setValueSource(MarketValueSource.PARTNER)
                                .build()
                );

        // меняет вендора в офере
        partnerBuilder.getActualBuilder().setVendor(
                DataCampOfferMeta.StringValue.newBuilder()
                        .setValue(newVendor.name)
                        .build()
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
                        .filter(p -> p.getParamId() == SIZE_PARAM_ID)
                        .count()
        ).isEqualTo(sizeParamsExpected);

        // all parameters should be defined for vendor
        assertThat(
                resultingSku.getParameterValuesList()
                        .stream()
                        .filter(p -> p.getParamId() == SIZE_PARAM_ID)
                        .map(p -> newVendor.sizeOptions.containsKey((long) p.getOptionId()))
                        .findAny()
                        .orElse(true)
        ).isEqualTo(true);

        assertThat(
                resultingSku.getParameterValueHypothesisList()
                        .stream()
                        .filter(p -> p.getParamId() == SIZE_PARAM_ID)
                        .count()
        ).isEqualTo(hypothesisExpected);
    }

    private ModelStorage.ParameterValue generateEnumParameterValue(long id, long optionId, String strValue) {
        ModelStorage.ParameterValue.Builder builder = ModelStorage.ParameterValue.newBuilder()
                .setParamId(id)
                .setOptionId((int) optionId)
                .setValueType(MboParameters.ValueType.ENUM);
        if (strValue != null) {
            builder.addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(strValue));
        }
        return builder.build();
    }

    private static MboParameters.Parameter.Builder generateSizeParameter() {
        var sizeParam = generateEnumMboParameter(SIZE_PARAM_ID, "girth_chest_woman");
        VENDORS.stream().flatMap(vendorInfo -> vendorInfo.sizeOptions.entrySet().stream())
                .forEach(entry -> sizeParam.addOption(
                        MboParameters.Option.newBuilder()
                                .setId(entry.getKey())
                                .addName(MboParameters.Word.newBuilder().setName(entry.getValue()).build())
                                .build()
                ));
        return sizeParam;
    }

    private static MboParameters.Parameter.Builder generateEnumMboParameter(long id, String xslName) {
        return MboParameters.Parameter.newBuilder()
                .setId(id)
                .setXslName(xslName)
                .setValueType(MboParameters.ValueType.ENUM);
    }

    private static class VendorInfo {
        private final long id;
        private final String name;
        private final LinkedHashMap<Long, String> sizeOptions;

        public VendorInfo(long id, String name, Map<Long, String> sizeOptions) {
            this.id = id;
            this.name = name;
            this.sizeOptions = new LinkedHashMap<>(sizeOptions);
        }

        @Override
        public String toString() {
            return "Vendor[" + id + "," + name + ']';
        }
    }

}
