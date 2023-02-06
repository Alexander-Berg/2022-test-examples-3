package ru.yandex.market.ultracontroller.ext;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.ir.util.ImmutableMonitoringResult;
import ru.yandex.market.CategoryTree;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.uc.SkuMappingKey;
import ru.yandex.market.ir.uc.SkuMappingValue;
import ru.yandex.market.ultracontroller.dao.CategoryInfo;
import ru.yandex.market.ultracontroller.dao.CategoryInfoDao;
import ru.yandex.market.ultracontroller.dao.OfferEntity;
import ru.yandex.market.ultracontroller.ext.datastorage.DataStorage;
import ru.yandex.market.ultracontroller.ext.datastorage.ModelForTest;
import ru.yandex.market.ultracontroller.ext.datastorage.SkuForTest;
import ru.yandex.market.ultracontroller.utils.RequestLogEntity;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:pipeline-test.xml"})
public class MappingPipelinesTest {
    @Autowired
    private ListWorker mainListWorker;
    @Autowired
    private DataStorage dataStorage;
    @Autowired
    private CategoryTreeDaoMock categoryTreeDaoMock;
    @Autowired
    private CategoryTree categoryTree;
    @Autowired
    private CategoryInfoDao categoryInfoDao;

    @Ignore
    public enum MappingType {
        APPROVED_SKU, APPROVED_MODEL
    }

    public MappingPipelinesTest() {
    }


    @PostConstruct
    public void init() {
        CategoryTree.CategoryTreeNode root = CategoryTree.newCategoryTreeNodeBuilder()
            .setName("root")
            .setUniqName("Root")
            .setHyperId(CategoryTree.ROOT_CATEGORY_ID)
            .setTovarId(CategoryTree.ROOT_TOVAR_CATEGORY_ID)
            .setVisible(true)
            .build();

        categoryTreeDaoMock.addNode(root, CategoryTree.ROOT_TOVAR_CATEGORY_ID);

        dataStorage.getCategoryInfos().values().stream()
            .map(this::convert)
            .peek(categoryTreeNode -> categoryTreeNode.setParent(root))
            .forEach(node -> categoryTreeDaoMock.addNode(node, CategoryTree.ROOT_TOVAR_CATEGORY_ID));
        categoryTree.reload();
        categoryInfoDao.reloadCategoryInfos();
    }

    private CategoryTree.CategoryTreeNode convert(CategoryInfo categoryInfo) {
        return CategoryTree.newCategoryTreeNodeBuilder()
            .setName(categoryInfo.getUniqueName())
            .setUniqName(categoryInfo.getUniqueName())
            .setHyperId(categoryInfo.getCategoryId())
            .setGuruLightCategoryId(categoryInfo.getCategoryId())
            .setTovarId(categoryInfo.getTovarCatId())
            .setVisible(true)
            .build();
    }

    private ModelForTest getModelBySku(long skuId) {
        SkuForTest sku = dataStorage.getSkuIdToSkuForTestMap().get(skuId);
        return dataStorage.getModelIdToModelForTestMap().get(sku.getModelId());
    }

    private ModelForTest findSuitableModelForTest(int shopId, String shopSkuId, MappingType mappingType) {
        Map<SkuMappingKey, SkuMappingValue> skuMappingKeySkuMappingValueMap = dataStorage.getSkuMappingValueMap();
        SkuMappingKey key = SkuMappingKey.of(shopId, shopSkuId);
        SkuMappingValue skuMappingValue = skuMappingKeySkuMappingValueMap.get(key);
        ModelForTest resultModel = null;
        long sku;
        int modelId;
        switch (mappingType) {
            case APPROVED_SKU:
                sku = skuMappingValue.getApprovedSkuId();
                resultModel = getModelBySku(sku);
                break;
            case APPROVED_MODEL:
                modelId = Math.toIntExact(skuMappingValue.getApprovedModelId());
                resultModel = dataStorage.getModelIdToModelForTestMap().get(modelId);
                break;
            default:
                throw new RuntimeException("Unknown mapping type.");
        }
        return resultModel;
    }

    @Test
    public void approvedSkuMappingTest() {
        UltraController.Offer shopSkuOffer = UltraController.Offer.newBuilder()
            .setShopId(DataStorage.SHOP_ID)
            .setSkuShop(DataStorage.SHOP_SKU_APPROVED_SKU_MAPPING)
            .setShopOfferId(DataStorage.SHOP_OFFER_ID_APPROVED_SKU_MAPPING)
            .setReturnMarketNames(true)
            .build();

        UltraController.Offer shopOfferIdOffer = UltraController.Offer.newBuilder()
            .setShopId(DataStorage.SHOP_ID)
            .setShopOfferId(DataStorage.SHOP_OFFER_ID_APPROVED_SKU_MAPPING)
            .setReturnMarketNames(true)
            .build();

        List<OfferEntity> offerEntityList = new ArrayList<>();
        OfferEntity shopSkuOfferEntity = new OfferEntity(shopSkuOffer, ImmutableMonitoringResult.OK);
        offerEntityList.add(shopSkuOfferEntity);
        OfferEntity shopOfferIdOfferEntity = new OfferEntity(shopOfferIdOffer, ImmutableMonitoringResult.OK);
        offerEntityList.add(shopOfferIdOfferEntity);
        mainListWorker.work(offerEntityList, new RequestLogEntity());

        Assert.assertEquals(
            shopSkuOfferEntity.getEnrichType().getNumber(),
            UltraController.EnrichedOffer.EnrichType.APPROVED_SKU.getNumber()
        );
        ModelForTest shopSkuTargetModel = findSuitableModelForTest(
            DataStorage.SHOP_ID, DataStorage.SHOP_SKU_APPROVED_SKU_MAPPING, MappingType.APPROVED_SKU
        );
        Assert.assertEquals(shopSkuTargetModel.getModelId(), shopSkuOfferEntity.getFinalModelId());
        Assert.assertEquals(shopSkuTargetModel.getCategotyId(), shopSkuOfferEntity.getFinalCategoryId());

        Assert.assertEquals(
            shopOfferIdOfferEntity.getEnrichType().getNumber(),
            UltraController.EnrichedOffer.EnrichType.APPROVED_SKU.getNumber()
        );
        ModelForTest shopOfferIdTargetModel = findSuitableModelForTest(
            DataStorage.SHOP_ID, DataStorage.SHOP_OFFER_ID_APPROVED_SKU_MAPPING, MappingType.APPROVED_SKU
        );
        Assert.assertEquals(shopOfferIdTargetModel.getModelId(), shopOfferIdOfferEntity.getFinalModelId());
        Assert.assertEquals(shopOfferIdTargetModel.getCategotyId(), shopOfferIdOfferEntity.getFinalCategoryId());
    }

    @Test
    public void approvedSkuAndCategoryMappingTest() {
        UltraController.Offer shopSkuOffer = UltraController.Offer.newBuilder()
            .setShopId(DataStorage.SHOP_ID)
            .setSkuShop(DataStorage.ABSENT_IN_SKUTCHER_MAPPING)
            .setShopOfferId(DataStorage.ABSENT_IN_SKUTCHER_MAPPING)
            .setReturnMarketNames(true)
            .build();

        OfferEntity shopSkuOfferEntity = new OfferEntity(shopSkuOffer, ImmutableMonitoringResult.OK);
        mainListWorker.work(List.of(shopSkuOfferEntity), new RequestLogEntity());

        Assert.assertEquals(
            shopSkuOfferEntity.getEnrichType().getNumber(),
            UltraController.EnrichedOffer.EnrichType.APPROVED_CATEGORY.getNumber()
        );

        Assert.assertEquals(shopSkuOfferEntity.getCategoryId(), 91491);
    }

    @Test
    public void approvedModelMappingTest() {
        UltraController.Offer shopSkuOffer = UltraController.Offer.newBuilder()
            .setShopId(DataStorage.SHOP_ID)
            .setSkuShop(DataStorage.SHOP_SKU_APPROVED_MODEL_MAPPING)
            .setShopOfferId(DataStorage.SHOP_OFFER_ID_APPROVED_MODEl_MAPPING)
            .setReturnMarketNames(true)
            .build();
        UltraController.Offer shopOfferIdOffer = UltraController.Offer.newBuilder()
            .setShopId(DataStorage.SHOP_ID)
            .setShopOfferId(DataStorage.SHOP_OFFER_ID_APPROVED_MODEl_MAPPING)
            .setReturnMarketNames(true)
            .build();

        List<OfferEntity> offerEntityList = new ArrayList();
        OfferEntity shopSkuOfferEntity = new OfferEntity(shopSkuOffer, ImmutableMonitoringResult.OK);
        offerEntityList.add(shopSkuOfferEntity);
        OfferEntity shopOfferIdOfferEntity = new OfferEntity(shopOfferIdOffer, ImmutableMonitoringResult.OK);
        offerEntityList.add(shopOfferIdOfferEntity);
        mainListWorker.work(offerEntityList, new RequestLogEntity());


        Assert.assertEquals(
            shopSkuOfferEntity.getEnrichType().getNumber(),
            UltraController.EnrichedOffer.EnrichType.APPROVED_MODEL.getNumber()
        );
        ModelForTest shopSkuTargetModel = findSuitableModelForTest(
            DataStorage.SHOP_ID, DataStorage.SHOP_SKU_APPROVED_MODEL_MAPPING, MappingType.APPROVED_MODEL
        );
        Assert.assertEquals(shopSkuTargetModel.getModelId(), shopSkuOfferEntity.getFinalModelId());
        Assert.assertEquals(shopSkuTargetModel.getCategotyId(), shopSkuOfferEntity.getFinalCategoryId());

        Assert.assertEquals(
            shopOfferIdOfferEntity.getEnrichType().getNumber(),
            UltraController.EnrichedOffer.EnrichType.APPROVED_MODEL.getNumber()
        );
        ModelForTest shopOfferIdTargetModel = findSuitableModelForTest(
            DataStorage.SHOP_ID, DataStorage.SHOP_OFFER_ID_APPROVED_MODEl_MAPPING, MappingType.APPROVED_MODEL
        );
        Assert.assertEquals(shopOfferIdTargetModel.getModelId(), shopOfferIdOfferEntity.getFinalModelId());
        Assert.assertEquals(shopOfferIdTargetModel.getCategotyId(), shopOfferIdOfferEntity.getFinalCategoryId());
    }

    @Test
    public void partnerSkuMappingTest() {
        UltraController.Offer shopSkuOffer = UltraController.Offer.newBuilder()
            .setShopId(DataStorage.SHOP_ID)
            .setSkuShop(DataStorage.SHOP_OFFER_ID_PARTNER_SKU_MAPPING)
            .setShopOfferId(DataStorage.SHOP_OFFER_ID_PARTNER_SKU_MAPPING)
            .setReturnMarketNames(true)
            .build();

        List<OfferEntity> offerEntityList = new ArrayList<>();
        OfferEntity shopSkuOfferEntity = new OfferEntity(shopSkuOffer, ImmutableMonitoringResult.OK);
        offerEntityList.add(shopSkuOfferEntity);
        mainListWorker.work(offerEntityList, new RequestLogEntity());

        final List<UltraController.EnrichedOffer> enrichedOffers = offerEntityList.stream()
            .map(offerEntity -> offerEntity.buildResult(dataStorage.getCategoryInfos()))
            .collect(Collectors.toList());

        Assert.assertEquals(1, enrichedOffers.size());

        final UltraController.EnrichedOffer enrichedOffer = enrichedOffers.get(0);

        ModelForTest shopSkuTargetModel = findSuitableModelForTest(
            DataStorage.SHOP_ID, DataStorage.SHOP_OFFER_ID_PARTNER_SKU_MAPPING, MappingType.APPROVED_SKU
        );

        Assert.assertEquals(
            UltraController.EnrichedOffer.EnrichType.APPROVED_SKU.getNumber(),
            enrichedOffer.getEnrichType().getNumber()
        );
        Assert.assertEquals(shopSkuTargetModel.getModelId(), enrichedOffer.getMatchedId());
        Assert.assertEquals(shopSkuTargetModel.getModelId(), enrichedOffer.getModelId());
        Assert.assertEquals(shopSkuTargetModel.getCategotyId(), enrichedOffer.getCategoryId());
        Assert.assertEquals(dataStorage.getCategoryInfos().get(shopSkuTargetModel.getCategotyId()).getUniqueName(),
            enrichedOffer.getMarketCategoryName());

    }

    public void setMainListWorker(ListWorker mainListWorker) {
        this.mainListWorker = mainListWorker;
    }

    public void setDataStorage(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }
}
