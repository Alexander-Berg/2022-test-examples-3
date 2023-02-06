package ru.yandex.market.mboc.common.datacamp;

import java.util.List;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferPictures;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static Market.DataCamp.DataCampOfferPictures.MarketPicture.Status.AVAILABLE;

public class DataCampOfferUtilTest extends BaseDbTestClass {
    @Autowired
    protected CategoryInfoRepository categoryInfoRepository;
    @Autowired
    protected StorageKeyValueService storageKeyValueService;

    private ContextedOfferDestinationCalculator calculator;

    @Before
    public void setUp() {
        var categoryInfoCache = new CategoryInfoCacheImpl(categoryInfoRepository);
        calculator = new ContextedOfferDestinationCalculator(
            categoryInfoCache,
            storageKeyValueService
        );
    }

    @Test
    public void getOfferPicturesShouldCorrectlyExtractPictureUrls() {
        var offer = testOffer(List.of(Pair.of("pic1", AVAILABLE)));
        var pictures = DataCampOfferUtil.getOfferPictures(offer);
        Assert.assertEquals(1, pictures.size());
        Assert.assertEquals("pic1", pictures.get(0).getUrl());
    }

    DataCampOffer.Offer testOffer(List<Pair<String, DataCampOfferPictures.MarketPicture.Status>> pics) {
        return OfferBuilder.create()
            .withIdentifiers(1, "offer1")
            .withDefaultProcessedSpecification()
            .withDefaultMarketContent()
            .withDefaultMarketSpecificContent()
            .withPictures(pics)
            .get()
            .build();
    }

}
