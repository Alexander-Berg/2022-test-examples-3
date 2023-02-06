package ru.yandex.market.core.supplier.promo.model.validation.strategy.xls;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampPromo;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.supplier.promo.model.offer.xls.CheapestAsGiftXlsPromoOffer;
import ru.yandex.market.core.supplier.promo.model.validation.PromoOfferValidationError;

public class CheapestAsGiftOfferXlsValidationStrategyTest extends AbstractXlsValidationStrategyTest {
    protected static final CheapestAsGiftOfferXlsValidationStrategy strategy = new CheapestAsGiftOfferXlsValidationStrategy();

    private static final String BLANK_MARKET_SKU = PromoOfferValidationError.BLANK_MARKET_SKU.getErrorMessage();
    private static final String BLANK_SHOP_SKU = PromoOfferValidationError.BLANK_SHOP_SKU.getErrorMessage();
    private static final String DUPLICATE_SKU = PromoOfferValidationError.DUPLICATE_SKU.getErrorMessage();
    private static final String MARKET_SKU_INCORRECT = PromoOfferValidationError.MARKET_SKU_INCORRECT.getErrorMessage();
    private static final String OFFER_NOT_ELIGIBLE_FOR_PROMO = PromoOfferValidationError
            .OFFER_NOT_ELIGIBLE_FOR_PROMO.getErrorMessage();

    private static final DataCampPromo.PromoDescription promo = DataCampPromo.PromoDescription.newBuilder()
            .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                    .setPromoId("promo-id")
                    .build()
            )
            .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                    .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                            .setCategoryRestriction(
                                    DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                            .addAllPromoCategory(
                                                    List.of(
                                                            DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                                    .setId(1)
                                                                    .build()
                                                    )
                                            )
                                            .build()
                            )
                    )
            )
            .build();

    @Test
    public void testAllOk() {
        final var offersWithParticipate = new CheapestAsGiftXlsPromoOffer.Builder(getFileOffer())
                .withParticipate(true)
                .build();
        List<CheapestAsGiftXlsPromoOffer> validatedOffers =
                strategy.apply(promo, List.of(offersWithParticipate), datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertNull(validatedOffers.get(0).getErrors());
    }

    @Test
    public void testShopSkuBlank() {
        final CheapestAsGiftXlsPromoOffer badFileOffer = new CheapestAsGiftXlsPromoOffer.Builder(getFileOffer())
                .withShopSku(null)
                .build();
        List<CheapestAsGiftXlsPromoOffer> validatedOffers =
                strategy.apply(promo, List.of(badFileOffer), datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertEquals("1. " + BLANK_SHOP_SKU, validatedOffers.get(0).getSortedErrorsAsString());
    }

    @Test
    public void testMarketSkuBlank() {
        final CheapestAsGiftXlsPromoOffer badFileOffer = new CheapestAsGiftXlsPromoOffer.Builder(getFileOffer())
                .withMarketSku(null)
                .build();
        List<CheapestAsGiftXlsPromoOffer> validatedOffers =
                strategy.apply(promo, List.of(badFileOffer), datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertEquals("1. " + BLANK_MARKET_SKU, validatedOffers.get(0).getSortedErrorsAsString());
    }

    @Test
    public void testDuplicateSkus() {
        List<CheapestAsGiftXlsPromoOffer> validatedOffers =
                strategy.apply(promo, List.of(getFileOffer(), getFileOffer()), datacampOffers);

        Assert.assertEquals(2, validatedOffers.size());
        Assert.assertEquals("1. " + DUPLICATE_SKU, validatedOffers.get(0).getSortedErrorsAsString());
        Assert.assertEquals("1. " + DUPLICATE_SKU, validatedOffers.get(1).getSortedErrorsAsString());
    }

    @Test
    public void testOfferNotEligible() {
        List<CheapestAsGiftXlsPromoOffer> fileOffers = List.of(getFileOffer());
        List<CheapestAsGiftXlsPromoOffer> validatedOffers =
                strategy.apply(promo, fileOffers, Collections.emptyMap());

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertEquals("1. " + OFFER_NOT_ELIGIBLE_FOR_PROMO, validatedOffers.get(0).getSortedErrorsAsString());
    }

    @Test
    public void testMarketSkuValid() {
        final CheapestAsGiftXlsPromoOffer badFileOffer = new CheapestAsGiftXlsPromoOffer.Builder(getFileOffer())
                .withMarketSku(2L)
                .build();
        List<CheapestAsGiftXlsPromoOffer> validatedOffers =
                strategy.apply(promo, List.of(badFileOffer), datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertEquals("1. " + MARKET_SKU_INCORRECT, validatedOffers.get(0).getSortedErrorsAsString());
    }


    @Test
    public void testMultipleErrors() {
        final CheapestAsGiftXlsPromoOffer badFileOffer = new CheapestAsGiftXlsPromoOffer.Builder(getFileOffer())
                .withShopSku(null)
                .withMarketSku(null)
                .build();
        List<CheapestAsGiftXlsPromoOffer> validatedOffers =
                strategy.apply(promo, List.of(badFileOffer), datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertEquals("1. " + BLANK_SHOP_SKU + "\n2. " + BLANK_MARKET_SKU, validatedOffers.get(0).getSortedErrorsAsString());
    }

    @Test
    public void testOfferHasActivePromo() {
        List<CheapestAsGiftXlsPromoOffer> fileOffers = List.of(getFileOffer());
        List<CheapestAsGiftXlsPromoOffer> validatedOffers =
                strategy.apply(promo, fileOffers, Map.of("shop-sku-1", dcOfferWithActivePromo));

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertNull(validatedOffers.get(0).getErrors());
    }

    private static CheapestAsGiftXlsPromoOffer getFileOffer() {
        return new CheapestAsGiftXlsPromoOffer.Builder()
                .withShopSku("shop-sku-1")
                .withMarketSku(1L)
                .build();
    }
}
