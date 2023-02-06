package ru.yandex.market.aliasmaker.offers;

import java.util.function.Supplier;

import com.google.protobuf.ByteString;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.aliasmaker.cache.KnowledgeService;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.matcher.be.OfferCopy;

import static org.mockito.Matchers.anyInt;

/**
 * @author galaev@yandex-team.ru
 * @since 12/09/2018.
 */
public class OfferConversionTest {
    private static final int SEED = 42;
    private static final String[] EXCLUDED_FIELDS = {"skutcherOfferRequestBase", "offerCopy"};

    private KnowledgeService knowledgeService;

    private static ByteString randomByteString() {
        var object = RANDOM.nextObject(String.class);
        return ByteString.copyFromUtf8(object);
    }    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(SEED)
            .randomize(ByteString.class, (Supplier<ByteString>) OfferConversionTest::randomByteString)
            .build();

    public static Offer randomOffer(EnhancedRandom random) {
        Offer offer = random.nextObject(Offer.class, EXCLUDED_FIELDS);
        OfferCopy offerCopy = random.nextObject(OfferCopy.class);
        offer.setOfferCopy(offerCopy);
        offer.setSkutcherOfferRequestBase(SkuBDApi.OfferInfo.getDefaultInstance());
        return offer;
    }

    @Before
    public void setUp() {
        knowledgeService = Mockito.mock(KnowledgeService.class);
        Mockito.when(knowledgeService.getVendorName(anyInt())).thenAnswer(i -> "name");
        Mockito.when(knowledgeService.getModelName(anyInt(), anyInt(), anyInt())).thenAnswer(i -> "name");
    }

    @Test
    public void testOfferConversion() {
        Offer offer = randomOffer(RANDOM);
        AliasMaker.Offer convertedOffer = offer.toOfferSafely(knowledgeService);
        assertOffersEqual(offer, convertedOffer);
    }

    private void assertOffersEqual(Offer offer, AliasMaker.Offer convertedOffer) {
        Assert.assertEquals(offer.getClassifierMagicId(), convertedOffer.getOfferId());
        Assert.assertEquals(offer.getMatchedType(), convertedOffer.getMatchType());
        Assert.assertEquals(offer.getShopOfferId(), convertedOffer.getShopOfferId());
        Assert.assertEquals(offer.getDescription(), convertedOffer.getDescription());
        Assert.assertEquals(offer.getShopId(), convertedOffer.getShopId());
        Assert.assertEquals(offer.getModelId(), convertedOffer.getModelId());
        Assert.assertEquals(offer.getMarketSkuId(), convertedOffer.getMarketSkuId());
        Assert.assertEquals(offer.getOfferParams(), convertedOffer.getOfferParams());
        Assert.assertEquals(offer.getOfferModel(), convertedOffer.getOfferModel());
        Assert.assertEquals(offer.getShopName(), convertedOffer.getShopName());
        Assert.assertEquals(offer.getPictures(), convertedOffer.getPictures());
        Assert.assertEquals(offer.getShopCategoryName(), convertedOffer.getShopCategoryName());
        Assert.assertEquals(offer.getUrls(), convertedOffer.getUrlsList());
        Assert.assertEquals(offer.getUrlSeparatedByNewLine(), convertedOffer.getUrl());
        Assert.assertEquals(offer.getSkuShop(), convertedOffer.getShopSkuId());
        Assert.assertEquals(offer.getSupplierMappingSkuId(),
                convertedOffer.getSupplierMappingInfo().getSkuId());
        Assert.assertEquals(offer.getSupplierMappingCategoryId(),
                convertedOffer.getSupplierMappingInfo().getCategoryId());
        Assert.assertEquals(offer.getSuggestMappingSkuId(),
                convertedOffer.getSuggestMappingInfo().getSkuId());
        Assert.assertEquals(offer.getSuggestMappingCategoryId(),
                convertedOffer.getSuggestMappingInfo().getCategoryId());
        Assert.assertEquals(offer.getProcessingStatusTs(), convertedOffer.getProcessingStatusTs());
        Assert.assertEquals(offer.isReclassified(), convertedOffer.getReclassified());
        Assert.assertEquals(offer.getTrackerTicket(), convertedOffer.getTrackerTicket());
        Assert.assertEquals(offer.getTicketDeadlineAsEpochDay(), convertedOffer.getTicketDeadlineDate());
        Assert.assertEquals(offer.isDeleted(), convertedOffer.getDeleted());
        Assert.assertEquals(offer.getOfferCopy().getHid(), convertedOffer.getCategoryId());
    }


}
