package ru.yandex.market.ultracontroller.ext;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.ir.util.ImmutableMonitoringResult;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ultracontroller.dao.KMFResult;
import ru.yandex.market.ultracontroller.dao.OfferEntity;
import ru.yandex.market.ultracontroller.ext.datastorage.DataStorage;
import ru.yandex.market.ultracontroller.utils.RequestLogEntity;

import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:anti-mapping-test.xml"})
public class AntiMappingWorkerTest {

    @Autowired
    private AntiMappingWorker antiMappingWorker;

    @Test
    public void antiMappingSkuTest() {
        UltraController.Offer shopSkuOffer = UltraController.Offer.newBuilder()
                .setShopId(DataStorage.SHOP_ID)
                .setSkuShop(DataStorage.SHOP_SKU_SKU_ANTI_MAPPING)
                .setReturnMarketNames(true)
                .addAntiMappingForUcMarketSkuIdFromDatacamp(100608245947L)
                .build();

        OfferEntity shopSkuOfferEntity = new OfferEntity(shopSkuOffer, ImmutableMonitoringResult.OK);
        shopSkuOfferEntity.setMarketSkuId(100608245947L);

        antiMappingWorker.work(shopSkuOfferEntity, new RequestLogEntity());

        Assert.assertEquals(
                UltraController.EnrichedOffer.EnrichType.ANTI_MAPPING_SKU,
                shopSkuOfferEntity.getEnrichType()
        );
        Assert.assertEquals(
                KMFResult.UNDEFINED,
                shopSkuOfferEntity.getMarketSkuId()
        );
    }

    @Test
    public void antiMappingModelTest() {
        UltraController.Offer shopModelOffer = UltraController.Offer.newBuilder()
                .setShopId(DataStorage.SHOP_ID)
                .setSkuShop(DataStorage.SHOP_SKU_MODEL_ANTI_MAPPING)
                .addAntiMappingForUcModelIdFromDatacamp(1732171388)
                .build();

        OfferEntity offerEntity = Mockito.spy(new OfferEntity(shopModelOffer, ImmutableMonitoringResult.OK));
        when(offerEntity.getFinalModelId()).thenReturn(Math.toIntExact(1732171388));

        antiMappingWorker.work(offerEntity, new RequestLogEntity());

        when(offerEntity.getFinalMatchedId()).thenCallRealMethod();
        Assert.assertEquals(
                UltraController.EnrichedOffer.EnrichType.ANTI_MAPPING_MODEL,
                offerEntity.getEnrichType()
        );

        Assert.assertEquals(
                UltraController.EnrichedOffer.MatchType.CUT_OFF_ANTI_MAPPING,
                offerEntity.getFinalMatchType()
        );
        Assert.assertEquals(
                0,
                offerEntity.getFinalMatchedId()
        );
        Assert.assertEquals(
                KMFResult.UNDEFINED,
                offerEntity.getMarketSkuId()
        );

    }

    @Test
    public void notAntiMappingTest() {
        UltraController.Offer shopSkuOffer = UltraController.Offer.newBuilder()
                .setShopId(DataStorage.SHOP_ID)
                .setSkuShop(DataStorage.SHOP_SKU_SKU_ANTI_MAPPING)
                .setReturnMarketNames(true)
                .build();
        OfferEntity shopSkuOfferEntity = new OfferEntity(shopSkuOffer, ImmutableMonitoringResult.OK);

        antiMappingWorker.work(shopSkuOfferEntity, new RequestLogEntity());

        Assert.assertEquals(
                UltraController.EnrichedOffer.EnrichType.MAIN,
                shopSkuOfferEntity.getEnrichType()
        );
    }

    public void setAntiMappingWorker(AntiMappingWorker antiMappingWorker) {
        this.antiMappingWorker = antiMappingWorker;
    }
}
