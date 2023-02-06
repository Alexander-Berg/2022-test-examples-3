package ru.yandex.market.ultracontroller.ext;


import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ultracontroller.dao.KMFResult;
import ru.yandex.market.ultracontroller.dao.MatchValidity;
import ru.yandex.market.ultracontroller.dao.OfferEntity;
import ru.yandex.market.ultracontroller.dao.SingleMatchResult;
import ru.yandex.market.ultracontroller.ext.datastorage.DataStorage;
import ru.yandex.market.ultracontroller.utils.RequestLogEntity;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:sku-worker-test.xml"})
public class SkuWorkerTest {

    @Autowired
    SkuWorker skuWorker;

    @Test
    public void rejectSkuAndModelIsSkuTest() {
        var modelId = DataStorage.MODEL_ID_IS_SKU_REJECTED.intValue();
        OfferEntity offerEntity = buildOffer(modelId);
        skuWorker.work(
                List.of(offerEntity), new RequestLogEntity()
        );


        Assert.assertEquals(
                KMFResult.UNDEFINED,
                offerEntity.getMarketSkuId()
        );
        Assert.assertEquals(
                0,
                offerEntity.getFinalMatchedId()
        );
        Assert.assertEquals(
                modelId,
                offerEntity.getMatchedModelId()
        );
        Assert.assertEquals(
                UltraController.EnrichedOffer.MatchType.REJECT_BY_PARAMETERS,
                offerEntity.getFinalMatchType()
        );
    }


    @Test
    public void rejectSkuTest() {
        var modelId = DataStorage.MODEL_ID_WITH_REJECTED_SKU;
        OfferEntity offerEntity = buildOffer(modelId.intValue());
        skuWorker.work(
                List.of(offerEntity), new RequestLogEntity()
        );


        Assert.assertEquals(
                KMFResult.UNDEFINED,
                offerEntity.getMarketSkuId()
        );
        Assert.assertEquals(
                modelId.intValue(),
                offerEntity.getFinalMatchedId()
        );
        Assert.assertEquals(
                modelId.intValue(),
                offerEntity.getMatchedModelId()
        );
        Assert.assertEquals(
                UltraController.EnrichedOffer.MatchType.MATCH_OK,
                offerEntity.getFinalMatchType()
        );
    }

    private OfferEntity buildOffer(int modelId) {
        var offerEntity = new OfferEntity(
                UltraController.Offer.newBuilder().build(),
                MonitoringResult.OK);
        var singleMatchResult = new SingleMatchResult();
        singleMatchResult.update(
                MatchValidity.OK,
                new MatcherWorker.MatcherRes(Matcher.MatchResult.newBuilder()
                        .setMatchedId(modelId)
                        .addMatchedHierarchy(Matcher.MatchLevel.newBuilder().setMatchedId(0).build())
                        .addMatchedHierarchy(Matcher.MatchLevel.newBuilder().setMatchedId(modelId).build())
                        .build(), 0),
                0f
        );
        offerEntity.setBestMatch(singleMatchResult);
        return offerEntity;
    }

}
