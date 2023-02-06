package ru.yandex.market.mboc.common.datacamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPictures;
import Market.UltraControllerServiceData.UltraController;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferUtils;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static Market.DataCamp.DataCampOfferPictures.MarketPicture.Status.AVAILABLE;
import static Market.DataCamp.DataCampOfferPictures.MarketPicture.Status.FAILED;
import static Market.DataCamp.DataCampOfferPictures.MarketPicture.Status.REMOVED;
import static Market.DataCamp.DataCampOfferPictures.MarketPicture.Status.UNDEFINED;
import static ru.yandex.market.mboc.common.datacamp.HashCalculator.CATEGORY_CONFIDENCE_HASH_SINCE_KEY;
import static ru.yandex.market.mboc.common.datacamp.HashCalculator.FORMALIZED_HASH_BIZ_IDS_KEY;

/**
 * @author apluhin
 * @created 5/17/22
 */
public class HashCalculatorTest extends BaseDbTestClass {

    @Autowired
    protected StorageKeyValueService storageKeyValueService;

    private ContextedOfferDestinationCalculator calculator;

    private static final int ALLOWED_CHANGE_CATEGORY_ID = 100900;
    private static final int NOT_ALLOWED_CHANGE_CATEGORY_ID = 100901;

    @Before
    public void setUp() {
        calculator = new ContextedOfferDestinationCalculator(
            categoryInfoCache,
            storageKeyValueService
        );
        var allowedToChangeCategoryInfo = new CategoryInfo(ALLOWED_CHANGE_CATEGORY_ID);
        var notAllowedToChangeCategoryInfo = new CategoryInfo(NOT_ALLOWED_CHANGE_CATEGORY_ID);
        allowedToChangeCategoryInfo.setAllowChange(true);
        categoryInfoRepository.insert(allowedToChangeCategoryInfo);
        categoryInfoRepository.insert(notAllowedToChangeCategoryInfo);
    }

    @Test
    public void marketSpecificContentHashCategoryConfidence() {
        DataCampOffer.Offer dcOffer = testOffer(List.of(Pair.of("pic1", AVAILABLE)));
        Offer offer = testMbocBlueOffer()
            .setCategoryIdForTests((long) dcOffer.getContent().getMarket().getCategoryId(), Offer.BindingKind.SUGGESTED)
            .setCategoryConfidence(Offer.CategoryConfidence.AUTO)
            .setCreated(LocalDateTime.now());

        storageKeyValueService.putValue(CATEGORY_CONFIDENCE_HASH_SINCE_KEY, LocalDate.now().minusDays(1));
        var newHash = hashCalculator.marketSpecificContentHashUnchecked(dcOffer, offer);
        storageKeyValueService.putValue(CATEGORY_CONFIDENCE_HASH_SINCE_KEY, LocalDate.now().plusDays(1));
        storageKeyValueService.invalidateCache();
        var oldHash = hashCalculator.marketSpecificContentHashUnchecked(dcOffer, offer);
        Assertions.assertThat(newHash).isNotEqualTo(oldHash);
    }

    @Test
    public void marketSpecificContentHashFormalized() {
        DataCampOffer.Offer dcOffer = formalizedOffer(List.of(Pair.of("pic1", AVAILABLE)));
        Offer offer = new Offer();
        offer.setCategoryIdForTests((long) dcOffer.getContent().getMarket().getCategoryId(),
            Offer.BindingKind.SUGGESTED);
        offer.setCreated(LocalDateTime.now());
        offer.setBusinessId(dcOffer.getIdentifiers().getBusinessId());
        offer.setShopSku(dcOffer.getIdentifiers().getOfferId());

        storageKeyValueService.putValue(FORMALIZED_HASH_BIZ_IDS_KEY, List.of(1));
        var formalizedHash = hashCalculator.marketSpecificContentHashUnchecked(dcOffer, offer);
        storageKeyValueService.putValue(FORMALIZED_HASH_BIZ_IDS_KEY, null);
        storageKeyValueService.invalidateCache();
        var oldHash = hashCalculator.marketSpecificContentHashUnchecked(dcOffer, offer);
        Assertions.assertThat(formalizedHash).isNotEqualTo(oldHash);
    }

    @Test
    public void marketSpecificContentHashCalculatedCorrectlyForAllowedToChangeCategory() {
        DataCampOffer.Offer dcOfferWithAllow = OfferBuilder.create()
            .withIdentifiers(1, "shop_sku1")
            .withDefaultProcessedSpecification()
            .withApprovedMapping(OfferBuilder.mapping((long) ALLOWED_CHANGE_CATEGORY_ID, null, null, null, null, null))
            .withDefaultMarketSpecificContent()
            .withPictures(List.of(Pair.of("pic1", AVAILABLE)))
            .withDefaultMarketContent(it -> it.setCategoryId(ALLOWED_CHANGE_CATEGORY_ID))
            .get()
            .build();

        DataCampOffer.Offer dcUsualOffer = OfferBuilder.create()
            .withIdentifiers(1, "shop_sku1")
            .withDefaultProcessedSpecification()
            .withApprovedMapping(OfferBuilder.mapping((long) NOT_ALLOWED_CHANGE_CATEGORY_ID, null, null, null, null,
                null))
            .withDefaultMarketSpecificContent()
            .withPictures(List.of(Pair.of("pic1", AVAILABLE)))
            .withDefaultMarketContent(it -> it.setCategoryId(NOT_ALLOWED_CHANGE_CATEGORY_ID))
            .get()
            .build();
        Offer usualOffer = new Offer();
        usualOffer.setCategoryIdForTests((long) dcUsualOffer.getContent().getMarket().getCategoryId(),
            Offer.BindingKind.SUGGESTED);
        usualOffer.setCreated(LocalDateTime.now());
        usualOffer.setBusinessId(dcUsualOffer.getIdentifiers().getBusinessId());
        usualOffer.setShopSku(dcUsualOffer.getIdentifiers().getOfferId());
        Offer offerWithAllow = new Offer();
        offerWithAllow.setCategoryIdForTests((long) dcOfferWithAllow.getContent().getMarket().getCategoryId(),
            Offer.BindingKind.APPROVED);
        offerWithAllow.setCreated(LocalDateTime.now());
        offerWithAllow.setBusinessId(dcOfferWithAllow.getIdentifiers().getBusinessId());
        offerWithAllow.setShopSku(dcOfferWithAllow.getIdentifiers().getOfferId());

        categoryInfoCache.resetCache();

        var hash1 = hashCalculator.marketSpecificContentHashUnchecked(dcOfferWithAllow, offerWithAllow);
        var hash2 = hashCalculator.marketSpecificContentHashUnchecked(dcUsualOffer, usualOffer);

        Assertions.assertThat(hash1).isNotEqualTo(hash2);

        var categoryInfo = new CategoryInfo(ALLOWED_CHANGE_CATEGORY_ID);
        categoryInfo.setAllowChange(false);
        categoryInfoRepository.update(categoryInfo);

        categoryInfoCache.resetCache();

        hash1 = hashCalculator.marketSpecificContentHashUnchecked(dcOfferWithAllow, offerWithAllow);
        hash2 = hashCalculator.marketSpecificContentHashUnchecked(dcUsualOffer, usualOffer);

        Assertions.assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    public void marketSpecificContentShouldDependOnPicturesDownloadStatus() {
        Assert.assertTrue(
            "Should be not empty if all pictures were downloaded",
            hashCalculator.marketSpecificContentHash(testOffer(List.of(
                Pair.of("pic1", AVAILABLE)
            )), testMbocBlueOffer(), calculator).isPresent()
        );

        Assert.assertTrue(
            "Should be not empty if pictures were removed or download failed",
            hashCalculator.marketSpecificContentHash(testOffer(List.of(
                Pair.of("pic1", REMOVED),
                Pair.of("pic2", FAILED)
            )), testMbocBlueOffer(), calculator).isPresent()
        );

        Assert.assertTrue(
            "Should be not empty if no pictures at all",
            hashCalculator.marketSpecificContentHash(testOffer(Collections.emptyList()), testMbocBlueOffer(),
                calculator).isPresent()
        );

        Assert.assertTrue(
            "Should be empty if one of the pictures weren't downloaded",
            hashCalculator.marketSpecificContentHash(testOffer(List.of(
                Pair.of("pic1", AVAILABLE),
                Pair.of("pic2", UNDEFINED),
                Pair.of("pic3", AVAILABLE)
            )), testMbocBlueOffer(), calculator).isEmpty()
        );
    }

    @Test
    public void testLightSend() {
        OfferUtils.setBusinessIdsWithNewDsbsPipeline(null);

        var offerNoPictures = OfferBuilder.create()
            .withIdentifiers(1, "offer1")
            .withDefaultProcessedSpecification()
            .withDefaultMarketContent()
            .withDefaultMarketSpecificContent()
            .get()
            .build();

        Assert.assertTrue(
            "Light send without pictures",
            hashCalculator.marketSpecificContentHash(offerNoPictures, testMbocBlueOffer(), calculator).isPresent()
        );

        var offerJustTitle = OfferBuilder.create()
            .withIdentifiers(1, "offer1")
            .get()
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                    .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                        .setTitle(DataCampOfferMeta.StringValue.newBuilder()
                            .setValue("asdf").build())
                        .build())
                    .build())
                .build())
            .build();

        Assert.assertTrue(
            "Light send only title",
            hashCalculator.marketSpecificContentHash(offerJustTitle, testMbocBlueOffer(), calculator).isPresent()
        );

        MbocSupplierType dsbsSupplierType = OfferTestUtils.dropshipBySellerSupplier().getType();
        Assert.assertFalse(
            "Do not light send DSBS",
            hashCalculator.marketSpecificContentHash(
                offerNoPictures, testMbocDsbsOffer(), calculator).isPresent()
        );
    }

    @Test
    public void testNullPictureUrl() {
        OfferUtils.setBusinessIdsWithNewDsbsPipeline(null);

        var offer1 = OfferBuilder.create()
            .withIdentifiers(1, "offer1")
            .withDefaultProcessedSpecification()
            .withDefaultMarketContent()
            .withDefaultMarketSpecificContent()
            .withDefaultProcessedSpecification()
            .build().toBuilder();
        var pictures = offer1.getPictures().toBuilder();
        var partnerPictures = DataCampOfferPictures.PartnerPictures.newBuilder();
        partnerPictures.getOriginalBuilder().addSource(DataCampOfferPictures.SourcePicture.newBuilder()
            .setUrl("pic1"));
        partnerPictures.getOriginalBuilder().addSource(DataCampOfferPictures.SourcePicture.newBuilder()
            .setUrl("pic2"));

        pictures.setPartner(partnerPictures);
        offer1.setPictures(pictures);

        Assert.assertFalse(
            "Pictures not downloaded - empty actual map",
            hashCalculator.marketSpecificContentHash(
                offer1.build(), testMbocBlueOffer(), calculator).isPresent()
        );
    }

    Offer testMbocBlueOffer() {
        return OfferTestUtils.simpleOffer();
    }

    Offer testMbocDsbsOffer() {
        return OfferTestUtils.simpleOffer().setServiceOffers(
            new Offer.ServiceOffer(1234, MbocSupplierType.DSBS, Offer.AcceptanceStatus.OK));
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

    DataCampOffer.Offer formalizedOffer(List<Pair<String, DataCampOfferPictures.MarketPicture.Status>> pics) {
        return OfferBuilder.create()
            .withIdentifiers(1, "offer1")
            .withDefaultProcessedSpecification()
            .withDefaultMarketContent()
            .withDefaultMarketSpecificContent()
            .withPictures(pics)
            .withConfidentParamList(
                List.of(UltraController.FormalizedParamPosition.newBuilder()
                    .setParamId(100).setType(UltraController.FormalizedParamType.BOOLEAN)
                    .setBooleanValue(true).build())
            )
            .get()
            .build();
    }
}
