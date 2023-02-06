package ru.yandex.market.ultracontroller.ext.datastorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.ir.uc.SkuMappingKey;
import ru.yandex.market.ir.uc.SkuMappingValue;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.ultracontroller.dao.CategoryInfo;

public final class DataStorage {
    public static final int SHOP_ID = 1010;
    public static final String SHOP_SKU_APPROVED_SKU_MAPPING = "ShopSkuApprovedSkuMapping";
    public static final String SHOP_SKU_SKU_ANTI_MAPPING = "ShopSkuSkuAntiMapping";
    public static final String SHOP_SKU_APPROVED_MODEL_MAPPING = "ShopSkuApprovedModelMapping";
    public static final String SHOP_SKU_MODEL_ANTI_MAPPING = "ShopSkuModelAntiMapping";


    public static final String SHOP_OFFER_ID_APPROVED_SKU_MAPPING = "ShopOfferIdApprovedSkuMapping";
    public static final String SHOP_OFFER_ID_APPROVED_MODEl_MAPPING = "ShopOfferIdApprovedModelMapping";

    public static final String SHOP_OFFER_ID_PARTNER_SKU_MAPPING = "ShopOfferIdPartnerSkuMapping";

    public static final String ABSENT_IN_SKUTCHER_MAPPING = "ABSENT_IN_SKUTCHER_MAPPING";

    public static final Long MODEL_ID_WITH_REJECTED_SKU = 875990875L;
    public static final Long MODEL_ID_IS_SKU_REJECTED = 877354677L;

    private final Map<SkuMappingKey, SkuMappingValue> skuMappingValueMap = new HashMap<>();
    private final Map<Integer, ModelForTest> modelIdToModelForTestMap = new HashMap<>();
    private final Map<Integer, SkuForTest> modelIdToRejectedSkuTestMap = new HashMap<>();
    private final Map<Long, SkuForTest> skuIdToSkuForTestMap = new HashMap<>();
    private final Int2ObjectMap<CategoryInfo> categoryInfos = new Int2ObjectOpenHashMap<>();


    private ModelForTest generateModel(int modelId, String modelName, int categotyId) {
        ModelForTest model = new ModelForTest();
        model.setModelId(modelId);
        model.setModelName(modelName);
        model.setCategotyId(categotyId);
        return model;
    }

    private SkuForTest generateSku(long marketSkuId,
                                   String marketSkuName,
                                   boolean published,
                                   boolean publishedOnMarket,
                                   boolean publishedOnBlueMarket,
                                   FormalizerParam.FormalizedParamPosition formalizedParamPosition,
                                   int modelId
    ) {
        return generateSku(marketSkuId,
            marketSkuName,
            published,
            publishedOnMarket,
            publishedOnBlueMarket,
            formalizedParamPosition,
            modelId, SkuBDApi.SkuOffer.SkuType.SKU);
    }

    private SkuForTest generateSku(long marketSkuId,
                                   String marketSkuName,
                                   boolean published,
                                   boolean publishedOnMarket,
                                   boolean publishedOnBlueMarket,
                                   FormalizerParam.FormalizedParamPosition formalizedParamPosition,
                                   int modelId,
                                   SkuBDApi.SkuOffer.SkuType skuType
    ) {
        SkuForTest sku = new SkuForTest();
        sku.setMarketSkuId(marketSkuId);
        sku.setMarketSkuName(marketSkuName);
        sku.setPublished(published);
        sku.setPublishedOnMarket(publishedOnMarket);
        sku.setPublishedOnBlueMarket(publishedOnBlueMarket);
        sku.setFormalizedParamPosition(formalizedParamPosition);
        sku.setModelId(modelId);
        sku.setSkuType(skuType);
        return sku;
    }

    private void generateMapping(String shopSku, long approvedSkuId,
                                 long approvedModelId,
                                 int approvedCategoryId) {
        SkuMappingKey key = SkuMappingKey.of(SHOP_ID, shopSku);
        SkuMappingValue value = SkuMappingValue.newBuilder()
                .setApprovedSkuId(approvedSkuId)
                .setApprovedModelId(approvedModelId)
                .setApprovedCategoryId(approvedCategoryId)
                .setDatacampOffer(false)
                .build();
        skuMappingValueMap.put(key, value);
    }

    public void init() {
        List<CategoryInfo> categoryInfoList = ImmutableList.of(
                new CategoryInfo(91491, 160043, 444, "Мобильные телефоны", true),
                new CategoryInfo(6427100, 6427101, 19205, "Планшеты", true)
        );

        categoryInfoList.forEach(categoryInfo ->
                categoryInfos.put(categoryInfo.getCategoryId(), categoryInfo)
        );

        //используется https://mbo.market.yandex.ru/gwt/#modelEditor/entity-id=1732171388
        ModelForTest model1 = generateModel(1732171388, "iPhone 8 64GB", 91491);
        modelIdToModelForTestMap.put(model1.getModelId(), model1);

        //используется https://mbo.market.yandex.ru/gwt/#modelEditor/entity-id=1732171388 , золотой
        SkuForTest sku1 = generateSku(100210864679L, "iPhone 8 64GB, золотой", true, true, true,
            FormalizerParam.FormalizedParamPosition.newBuilder().getDefaultInstanceForType(), model1.getModelId());
        skuIdToSkuForTestMap.put(sku1.getMarketSkuId(), sku1);

        //используется https://mbo.market.yandex.ru/gwt/#modelEditor/entity-id=1732171388 , серебристый
        SkuForTest sku2 = generateSku(100210864680L, "iPhone 8 64GB, серебристый", true, true, true,
            FormalizerParam.FormalizedParamPosition.newBuilder().getDefaultInstanceForType(), model1.getModelId());
        skuIdToSkuForTestMap.put(sku2.getMarketSkuId(), sku2);

        //используется https://mbo.market.yandex.ru/gwt/#modelEditor/entity-id=1721714804
        ModelForTest model2 = generateModel(1721714804, "iPad 32Gb Wi-Fi", 6427100);
        modelIdToModelForTestMap.put(model2.getModelId(), model2);

        //используется https://mbo.market.yandex.ru/gwt/#modelEditor/category-id=6427100&entity-id=415565462
        ModelForTest model3 = generateModel(415565462, "iPad Air (2019) 64Gb Wi-Fi + Cellular", 6427100);
        modelIdToModelForTestMap.put(model3.getModelId(), model3);

        //используется https://mbo.market.yandex.ru/gwt/#modelEditor/entity-id=1721714804, iPad 32Gb Wi-Fi, silver
        SkuForTest sku3 = generateSku(100235855601L, "iPad 32Gb Wi-Fi, silver", true, true, true,
            FormalizerParam.FormalizedParamPosition.newBuilder().build(), model2.getModelId());
        skuIdToSkuForTestMap.put(sku3.getMarketSkuId(), sku3);

        //используется https://mbo.market.yandex.ru/gwt/#modelEditor/entity-id=1721714804, iPad 32Gb Wi-Fi, gold
        SkuForTest sku4 = generateSku(100235855602L, "iPad 32Gb Wi-Fi, gold", true, true, true,
            FormalizerParam.FormalizedParamPosition.newBuilder().build(), model2.getModelId());
        skuIdToSkuForTestMap.put(sku4.getMarketSkuId(), sku4);

        //используется https://mbo.market.yandex.ru/gwt/#modelEditor/entity-id=100608245947&category-id=6427100,
        // iPad Air (2019) 64Gb Wi-Fi + Cellular, space grey
        SkuForTest sku5 = generateSku(100608245947L, "iPad Air (2019) 64Gb Wi-Fi + Cellular, space grey",
                true, true, true,
                FormalizerParam.FormalizedParamPosition.newBuilder().build(), model2.getModelId(),
                SkuBDApi.SkuOffer.SkuType.PARTNER_SKU
        );
        skuIdToSkuForTestMap.put(sku5.getMarketSkuId(), sku5);

        //https://mbo.market.yandex-team.ru/gwt/#modelEditor/entity-id=877354677
        var rejectedModel = generateModel(MODEL_ID_IS_SKU_REJECTED.intValue(), "стиральный порошок + кондиционер для " +
                "детского белья", 90688);
        var rejectedSku = generateSku(MODEL_ID_IS_SKU_REJECTED,
                "Набор Meine Liebe стиральный порошок + кондиционер для детского белья, 2 уп.\n",
                true, true, true,
                FormalizerParam.FormalizedParamPosition.newBuilder().build(), rejectedModel.getModelId(),
                SkuBDApi.SkuOffer.SkuType.SKU
        );
        modelIdToRejectedSkuTestMap.put(rejectedModel.getModelId(), rejectedSku);
        modelIdToModelForTestMap.put(rejectedModel.getModelId(), rejectedModel);


        // https://mbo.market.yandex-team.ru/gwt/#modelEditor/entity-id=875990875
        var modelWithRejectedSku = generateModel(
                MODEL_ID_WITH_REJECTED_SKU.intValue(),
                "Для детского цветного и белого белья", 90688
        );
        // https://mbo.market.yandex-team.ru/gwt/#modelEditor/entity-id=101233632997
        var rejectedSku2 = generateSku(
                101233632997L,
                "Набор Molecola Для детского цветного и белого белья, 2.4 кг, 2 уп.\n",
                true, true, true,
                FormalizerParam.FormalizedParamPosition.newBuilder().build(),
                MODEL_ID_WITH_REJECTED_SKU.intValue(),
                SkuBDApi.SkuOffer.SkuType.SKU
        );
        modelIdToRejectedSkuTestMap.put(modelWithRejectedSku.getModelId(), rejectedSku2);
        modelIdToModelForTestMap.put(modelWithRejectedSku.getModelId(), modelWithRejectedSku);


        generateMapping(SHOP_SKU_APPROVED_SKU_MAPPING,
                sku1.getMarketSkuId(),
                model1.getModelId(),
                model1.getCategotyId()
        );

        generateMapping(SHOP_SKU_APPROVED_MODEL_MAPPING,
                SkuMappingValue.UNDEFINED_MAPPING,
                model1.getModelId(),
            model1.getCategotyId()
        );

        generateMapping(SHOP_OFFER_ID_APPROVED_SKU_MAPPING,
            sku3.getMarketSkuId(),
            model2.getModelId(),
            model2.getCategotyId()
        );

        generateMapping(SHOP_OFFER_ID_APPROVED_MODEl_MAPPING,
                SkuMappingValue.UNDEFINED_MAPPING,
            model2.getModelId(),
            model2.getCategotyId()
        );

        generateMapping(SHOP_OFFER_ID_PARTNER_SKU_MAPPING,
                sku5.getMarketSkuId(),
            model3.getModelId(),
            model3.getCategotyId()
        );

        generateMapping(ABSENT_IN_SKUTCHER_MAPPING,
            100500200,
            SkuMappingValue.UNDEFINED_MAPPING,
            91491
        );
    }

    public Map<SkuMappingKey, SkuMappingValue> getSkuMappingValueMap() {
        return skuMappingValueMap;
    }

    public Map<Integer, ModelForTest> getModelIdToModelForTestMap() {
        return modelIdToModelForTestMap;
    }

    public Map<Long, SkuForTest> getSkuIdToSkuForTestMap() {
        return skuIdToSkuForTestMap;
    }

    public Map<Integer, SkuForTest> getModelIdToRejectedSkuTestMap() {
        return modelIdToRejectedSkuTestMap;
    }

    public Int2ObjectMap<CategoryInfo> getCategoryInfos() {
        return categoryInfos;
    }
}
