package ru.yandex.market.core.supplier.promo.model.validation.strategy.xls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampPromo;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.supplier.promo.model.offer.xls.DiscountXlsPromoOffer;
import ru.yandex.market.core.supplier.promo.model.validation.PromoOfferValidationError;

import static org.assertj.core.api.Assertions.assertThat;

public class DiscountOfferXlsValidationStrategyTest extends AbstractXlsValidationStrategyTest {
    private static final String PROMO_PRICE_MAX_INVALID = PromoOfferValidationError
            .PROMO_PRICE_MAX_INVALID.getErrorMessage();
    private static final String PROMO_PRICE_MIN_INVALID = PromoOfferValidationError
            .PROMO_PRICE_MIN_INVALID.getErrorMessage();
    private static final String BLANK_MARKET_SKU = PromoOfferValidationError.BLANK_MARKET_SKU.getErrorMessage();
    private static final String BLANK_SHOP_SKU = PromoOfferValidationError.BLANK_SHOP_SKU.getErrorMessage();
    private static final String DUPLICATE_SKU = PromoOfferValidationError.DUPLICATE_SKU.getErrorMessage();
    private static final String MARKET_SKU_INCORRECT = PromoOfferValidationError.MARKET_SKU_INCORRECT.getErrorMessage();
    private static final String OFFER_NOT_ELIGIBLE_FOR_PROMO = PromoOfferValidationError
            .OFFER_NOT_ELIGIBLE_FOR_PROMO.getErrorMessage();

    protected static final DiscountOfferXlsValidationStrategy strategy = new DiscountOfferXlsValidationStrategy(false);

    private static final DataCampPromo.PromoDescription directDiscountPromo = DataCampPromo.PromoDescription.newBuilder()
            .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                    .setPromoId("promo-id")
                    .build()
            )
            .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                    .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT))
            .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                    .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                            .setCategoryRestriction(
                                    DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                            .addAllPromoCategory(
                                                    List.of(
                                                            DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                                    .setId(1)
                                                                    .setMinDiscount(50)
                                                                    .build()
                                                    )
                                            )
                                            .build()
                            )
                    )
            )
            .build();

    private static final DataCampPromo.PromoDescription blueFlashPromo = DataCampPromo.PromoDescription.newBuilder()
            .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                    .setPromoId("blue-flash-promo-id")
                    .build()
            )
            .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                    .setPromoType(DataCampPromo.PromoType.BLUE_FLASH))
            .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                    .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                            .setCategoryRestriction(
                                    DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                            .addAllPromoCategory(
                                                    List.of(
                                                            DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                                    .setId(1)
                                                                    .setMinDiscount(50)
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
        List<DiscountXlsPromoOffer> fileOffers = List.of(getFileOffer());
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(directDiscountPromo, fileOffers, datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertNull(validatedOffers.get(0).getErrors());
    }

    @Test
    public void testCountOfDiscountOffers() {
        final var offersWithParticipate = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withPromoPrice(20L)
                .build();
        List<DiscountXlsPromoOffer> offersWithParticipateList = new ArrayList<>();
        offersWithParticipateList.add(offersWithParticipate);
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(directDiscountPromo, offersWithParticipateList, datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertNull(validatedOffers.get(0).getErrors());
    }

    @Test
    public void testShopSkuBlank() {
        final DiscountXlsPromoOffer badFileOffer = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withShopSku(null)
                .build();
        List<DiscountXlsPromoOffer> badFileOffers = new ArrayList<>();
        badFileOffers.add(badFileOffer);
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(directDiscountPromo, badFileOffers, datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertEquals("1. " + BLANK_SHOP_SKU, validatedOffers.get(0).getSortedErrorsAsString());
    }

    @Test
    public void testMarketSkuBlank() {
        final DiscountXlsPromoOffer badFileOffer = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withMarketSku(null)
                .build();
        List<DiscountXlsPromoOffer> badFileOffers = new ArrayList<>();
        badFileOffers.add(badFileOffer);
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(directDiscountPromo, badFileOffers, datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertEquals("1. " + BLANK_MARKET_SKU, validatedOffers.get(0).getSortedErrorsAsString());
    }

    @Test
    public void testInvalidPromoPriceMax() {
        final DiscountXlsPromoOffer badFileOffer = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withPromoPrice(1001L)
                .build();
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(directDiscountPromo, List.of(badFileOffer), datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertEquals("1. " + PROMO_PRICE_MAX_INVALID, validatedOffers.get(0).getSortedErrorsAsString());
    }

    @Test
    public void testInvalidPromoPriceMin() {
        final DiscountXlsPromoOffer badFileOffer = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withPromoPrice(19L)
                .build();
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(directDiscountPromo, List.of(badFileOffer), datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertEquals("1. " + PROMO_PRICE_MIN_INVALID, validatedOffers.get(0).getSortedErrorsAsString());
    }

    @Test
    public void testDuplicateSkus() {
        List<DiscountXlsPromoOffer> validatedOffers =
                strategy.apply(directDiscountPromo, List.of(getFileOffer(), getFileOffer()), datacampOffers);

        Assert.assertEquals(2, validatedOffers.size());
        Assert.assertEquals("1. " + DUPLICATE_SKU, validatedOffers.get(0).getSortedErrorsAsString());
        Assert.assertEquals("1. " + DUPLICATE_SKU, validatedOffers.get(1).getSortedErrorsAsString());
    }

    @Test
    public void testOfferNotEligible() {
        List<DiscountXlsPromoOffer> fileOffers = List.of(getFileOffer());
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(directDiscountPromo, fileOffers, Collections.emptyMap());

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertEquals("1. " + OFFER_NOT_ELIGIBLE_FOR_PROMO, validatedOffers.get(0).getSortedErrorsAsString());
    }

    @Test
    public void testMarketSkuValid() {
        final DiscountXlsPromoOffer badFileOffer = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withMarketSku(2L)
                .build();
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(directDiscountPromo, List.of(badFileOffer), datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertEquals("1. " + MARKET_SKU_INCORRECT, validatedOffers.get(0).getSortedErrorsAsString());
    }

    @Test
    public void testMultipleErrors() {
        final DiscountXlsPromoOffer badFileOffer = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withShopSku(null)
                .withMarketSku(null)
                .build();
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(directDiscountPromo, List.of(badFileOffer), datacampOffers);

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertEquals("1. " + BLANK_SHOP_SKU + "\n2. " + BLANK_MARKET_SKU,
                validatedOffers.get(0).getSortedErrorsAsString());
    }

    @Test
    public void testOfferHasActivePromo() {
        List<DiscountXlsPromoOffer> fileOffers = List.of(getFileOffer());
        List<DiscountXlsPromoOffer> validatedOffers =
                strategy.apply(directDiscountPromo, fileOffers, Map.of("shop-sku-1", dcOfferWithActivePromo));

        Assert.assertEquals(1, validatedOffers.size());
        Assert.assertNull(validatedOffers.get(0).getErrors());
    }

    @Test
    // shop-sku-1 НЕТ promo-id, promo-id-1 ДА
    // shop-sku-2 НЕТ promo-id, promo-id-1 ДА
    public void testMultipleWarehouseId() {
        ArrayList<DiscountXlsPromoOffer> filePromoOffers = new ArrayList<>();
        filePromoOffers.add(getFileOffer());

        DiscountXlsPromoOffer fileOffer2 = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withShopSku("shop-sku-2")
                .build();
        filePromoOffers.add(fileOffer2);

        Map<String, DataCampOffer.Offer> datacampOffersCopy = new HashMap<>();
        var dcOffer = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-1")
                        .setWarehouseId(461)
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo("promo-id-1", 1000L, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo("promo-id", null, 2000L),
                                                        createPromo("promo-id-1", null, 2000L)
                                                )
                                        )
                                )
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                .setCategoryId(1)
                                .build())
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(1L)
                                )
                        )
                        .build())
                .build();
        datacampOffersCopy.put("shop-sku-1", dcOffer);

        DataCampOffer.Offer dcOffer2 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-2")
                        .setWarehouseId(461)
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo("promo-id-1", 1000L, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo("promo-id", null, 2000L),
                                                        createPromo("promo-id-1", null, 2000L)
                                                )
                                        )
                                )
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                .setCategoryId(1)
                                .build())
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(1L)
                                )
                        )
                        .build())
                .build();
        datacampOffersCopy.put("shop-sku-2", dcOffer2);

        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(directDiscountPromo, filePromoOffers, datacampOffersCopy);

        Assert.assertEquals(2, validatedOffers.size());
        //shop-sku-1
        DiscountXlsPromoOffer discountPromoOffer0 = validatedOffers.get(0);
        Assert.assertEquals("shop-sku-1", discountPromoOffer0.getShopSku());
        Assert.assertNull(discountPromoOffer0.getErrors());

        //shop-sku-2
        DiscountXlsPromoOffer discountPromoOffer1 = validatedOffers.get(1);
        Assert.assertEquals("shop-sku-2", discountPromoOffer1.getShopSku());
        Assert.assertNull(discountPromoOffer1.getErrors());
    }

    private static DiscountXlsPromoOffer getFileOffer() {
        return new DiscountXlsPromoOffer.Builder()
                .withShopSku("shop-sku-1")
                .withMarketSku(1L)
                .withPromoPrice(1000L)
                .withOldPrice(2000L)
                .build();
    }

    @Test
    public void testMaxBlueFlashDiscount() {
        final var offersWithParticipate = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withPromoPrice(20L)
                .build();
        List<DiscountXlsPromoOffer> offersWithParticipateList = new ArrayList<>();
        offersWithParticipateList.add(offersWithParticipate);
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(blueFlashPromo, offersWithParticipateList, Map.of("shop-sku-1", flashDcOffer));

        assertThat(validatedOffers).hasSize(1);
        assertThat(validatedOffers).singleElement().satisfies(e -> assertThat(e.getErrors()).isNull());
    }

    @Test
    public void testBlankOldPrice() {
        final var offersWithParticipate = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withPromoPrice(20L)
                .withOldPrice(null)
                .build();
        List<DiscountXlsPromoOffer> offersWithParticipateList = new ArrayList<>();
        offersWithParticipateList.add(offersWithParticipate);
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(blueFlashPromo, offersWithParticipateList, Map.of("shop-sku-1", flashDcOffer));

        assertThat(validatedOffers).hasSize(1);
        assertThat(validatedOffers).singleElement().satisfies(e -> assertThat(e.getErrors()).containsExactly(PromoOfferValidationError.BLANK_OLD_PRICE_IN_DISCOUNTS));
    }

    @Test
    public void testCorrectNewHonestDiscountsWithHistoryPriceValidation() {
        final var offersWithParticipate = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withPromoPrice(20L)
                .withOldPrice(2000L)
                .build();
        List<DiscountXlsPromoOffer> offersWithParticipateList = new ArrayList<>();
        offersWithParticipateList.add(offersWithParticipate);
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(blueFlashPromo, offersWithParticipateList, Map.of("shop-sku-1", flashDcOffer));

        assertThat(validatedOffers).hasSize(1);
        assertThat(validatedOffers).singleElement().satisfies(e -> assertThat(e.getErrors()).isNull());
    }

    @Test
    public void testCorrectNewHonestDiscountsWithoutHistoryPriceValidation() {
        final var offersWithParticipate = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withPromoPrice(20L)
                .withOldPrice(2000L)
                .build();
        List<DiscountXlsPromoOffer> offersWithParticipateList = new ArrayList<>();
        offersWithParticipateList.add(offersWithParticipate);
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(blueFlashPromo, offersWithParticipateList, Map.of("shop-sku-1", flashDcOfferWOBasePrice));

        assertThat(validatedOffers).hasSize(1);
        assertThat(validatedOffers).singleElement().satisfies(e -> assertThat(e.getErrors()).isNull());
    }

    @Test
    public void testZeroPricesOfferValidation() {
        DiscountXlsPromoOffer offersWithParticipate = new DiscountXlsPromoOffer.Builder(getFileOffer())
                .withPromoPrice(0L)
                .withOldPrice(0L)
                .build();
        List<DiscountXlsPromoOffer> offersWithParticipateList = new ArrayList<>();
        offersWithParticipateList.add(offersWithParticipate);
        List<DiscountXlsPromoOffer> validatedOffers = strategy.apply(blueFlashPromo, offersWithParticipateList, Map.of("shop-sku-1", flashDcOfferWOBasePrice));

        assertThat(validatedOffers).hasSize(1);
        assertThat(validatedOffers).singleElement().satisfies(e -> assertThat(e.getErrors()).containsExactly(PromoOfferValidationError.PROMO_PRICE_MAX_INVALID));
    }
}
