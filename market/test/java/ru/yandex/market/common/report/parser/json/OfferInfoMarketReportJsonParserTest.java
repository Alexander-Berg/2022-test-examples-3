package ru.yandex.market.common.report.parser.json;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import junit.framework.AssertionFailedError;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.report.model.AdditionalOffer;
import ru.yandex.market.common.report.model.CancelType;
import ru.yandex.market.common.report.model.Color;
import ru.yandex.market.common.report.model.FoodtechType;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.LocalDeliveryOption;
import ru.yandex.market.common.report.model.MarkupData;
import ru.yandex.market.common.report.model.OfferPicture;
import ru.yandex.market.common.report.model.OfferPromo;
import ru.yandex.market.common.report.model.OfferSeller;
import ru.yandex.market.common.report.model.OfferService;
import ru.yandex.market.common.report.model.OrderCancelPolicy;
import ru.yandex.market.common.report.model.PickupOption;
import ru.yandex.market.common.report.model.PromoBound;
import ru.yandex.market.common.report.model.PromoDetails;
import ru.yandex.market.common.report.model.PromoThreshold;
import ru.yandex.market.common.report.model.PromoType;
import ru.yandex.market.common.report.model.RawParam;
import ru.yandex.market.common.report.model.json.credit.BnplDenial;
import ru.yandex.market.common.report.model.json.credit.InstallmentsInfo;
import ru.yandex.market.common.report.model.json.credit.MonthlyPayment;
import ru.yandex.market.common.report.model.resale.ResaleSpecs;
import ru.yandex.market.common.report.model.specs.InternalSpec;
import ru.yandex.market.common.report.model.specs.UsedParam;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.common.report.model.json.BnplDenialReason.TOO_CHEAP;

public class OfferInfoMarketReportJsonParserTest {
    private static final double DELTA = 0.0001;
    private OfferInfoMarketReportJsonParser parser;
    private OfferInfoMarketReportJsonParser parserWithLocalDelivery;

    private static OfferInfoMarketReportJsonParser createParserWithLocalDelivery() {
        OfferInfoMarketReportJsonParserSettings settings = new OfferInfoMarketReportJsonParserSettings();
        settings.setParseDeliveryOptions(true);
        return new OfferInfoMarketReportJsonParser(settings);
    }

    @Before
    public void setUp() {
        parser = new OfferInfoMarketReportJsonParser(new OfferInfoMarketReportJsonParserSettings());

        parserWithLocalDelivery = createParserWithLocalDelivery();
    }

    @Test
    public void testEmptyResult() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/empty_offer_info.json")) {
            List<FoundOffer> result = parser.parse(resourceAsStream);

            assertEquals(0, result.size());
        }
    }

    @Test
    public void testSpecsInternal() throws IOException {
        Set<InternalSpec> required = new HashSet<>();
        required.add(new InternalSpec("prescription"));
        required.add(new InternalSpec("psychotropic"));
        required.add(new InternalSpec("baa"));
        required.add(new InternalSpec("medicine"));
        required.add(new InternalSpec("medical_product"));
        required.add(new InternalSpec("narcotic"));
        required.add(new InternalSpec("precursor"));
        required.add(new InternalSpec("ethanol"));
        required.add(new InternalSpec("vidal", Collections.singletonList(new UsedParam("J05AX13"))));

        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream(
                "/files/offer_info_medical_options.json")) {
            List<FoundOffer> result = parser.parse(resourceAsStream);

            assertEquals(required.size(), result.get(0).getSpecs().getInternal().size());
            assertEquals(required, result.get(0).getSpecs().getInternal());
        }
    }

    @Test
    public void testPriorityRegionId() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertEquals(213L, offer.getPriorityRegionId().longValue());
        }
    }

    @Test
    public void testFeeShow() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertEquals("8-qH2tqoDtJamZlTQsA8ugT_vzmawg8OaqyOfrhkXkJv84HAxcttZpth99jL_1gNShF4nzKb" +
                            "-oDl_cQT9bJMc1cFzrCIsi9p3-DZ0kGqJwvdFb5VSCFUsw,,",
                    offer.getFeeShow());
        }
    }

    @Test
    public void testHyperCategoryName() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertEquals("Музыка", offer.getHyperCategoryName());
        }
    }

    @Test
    public void testHyperCategoryFullName() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertEquals("Музыкальные фильмы", offer.getHyperCategoryFullName());
        }
    }

    @Test
    public void testHsCode() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertEquals(12345678901234L, offer.getHsCode().longValue());
        }
    }

    @Test
    public void testNullWarehouseId() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(offers);

            assertNull(offer.getWarehouseId());
        }
    }

    @Test
    public void testNotNullWarehouseId() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_warehouseId.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(offers);

            Assert.assertEquals(2, offer.getWarehouseId().intValue());
            Assert.assertTrue(offer.getMainPicture().isEmpty());
        }
    }

    @Test
    public void testLoyaltyProgramPartnerEnabled() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_warehouseId.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(offers);
            Assert.assertTrue(offer.isLoyaltyProgramPartner());
        }
    }

    @Test
    public void testLoyaltyProgramPartnerDisabled() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_english_name.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(offers);
            Assert.assertFalse(offer.isLoyaltyProgramPartner());
        }
    }

    @Test
    public void testFeedPrice() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertThat(offer.getFeedPrice(), comparesEqualTo(new BigDecimal("245.2")));
        }
    }

    @Test
    public void testPriceWithoutVat() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_price_without_vat.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertThat(offer.getPriceWithoutVat(), comparesEqualTo(BigDecimal.valueOf(195)));
        }
    }

    @Test
    public void testVendorId() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertThat(offer.getVendorId(), is(10545982L));
        }
    }

    @Test
    public void testFeedGroupIdHash() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertThat(offer.getFeedGroupIdHash(), is("MTQxODg1MTM4OTE3NDgwNjkzNTY"));
        }
    }

    @Test
    public void testFeedGroupIdNotZero() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertThat(offer.getFeedGroupId(), is("123"));
        }
    }

    @Test
    public void testFeedGroupZero() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_preorder.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertThat(offer.getFeedGroupId(), nullValue());
        }
    }

    @Test
    public void testFulfillmentLight() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertEquals("light", offer.getFulfillmentType());
        }
    }

    @Test
    public void testPromoStocks() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_promo.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertEquals("xMpCOKC554INzFCab3WE2w", offer.getPromoMd5());
            assertThat(offer.getPromoStock(), comparesEqualTo(BigDecimal.valueOf(50)));
        }
    }

    @Test
    public void testPromoDetailsParse() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_promo.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertEquals("xMpCOKC554INzFCab3WE2w", offer.getPromoMd5());
            assertThat(offer.getPromoStock(), comparesEqualTo(BigDecimal.valueOf(50)));
            assertThat(offer.getPromoDetails(), notNullValue());
            assertThat(offer.getPromoDetails().getPromoType(), is("market-model-for-fixed-price"));
            assertThat(offer.getPromoDetails().getPromoKey(), is("xMpCOKC554INzFCab3WE2w"));
            assertThat(offer.getPromoDetails().getStartDate(), is(LocalDateTime.parse("1985-06-23T04:00:00Z",
                    DateTimeFormatter.ISO_DATE_TIME)));
            assertThat(offer.getPromoDetails().getEndDate(), is(LocalDateTime.parse("1985-06-25T04:00:00Z",
                    DateTimeFormatter.ISO_DATE_TIME)));
            assertThat(offer.getPromoDetails().getPromoFixedPrice(), is(BigDecimal.valueOf(550)));
            assertThat(offer.getPromoDetails().isAllowPromocode(), is(true));
            assertThat(offer.getPromoDetails().isAllowBeruBonus(), is(true));

            assertThat(offer.getPromos(), hasItems(allOf(
                    hasProperty("promoMd5", is("direct-discount with subsidy")),
                    hasProperty("promoType", is("direct-discount")),
                    hasProperty("promoDetails", allOf(
                            hasProperty("promoKey", is("direct-discount with subsidy")),
                            hasProperty("promoType", is("direct-discount")),
                            hasProperty("promoTypeEnum", is(PromoType.DIRECT_DISCOUNT)),
                            hasProperty("promoFixedPrice", comparesEqualTo(BigDecimal.valueOf(450))),
                            hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(550))),
                            hasProperty("promoFixedSubsidy", comparesEqualTo(BigDecimal.valueOf(560))),
                            hasProperty("allowPromocode", is(true)),
                            hasProperty("allowBeruBonus", is(true)),
                            hasProperty("hasDcoSubsidy", is(true))
                    ))
                    ), allOf(
                    hasProperty("promoMd5", is("direct-discount with fixed price")),
                    hasProperty("promoType", is("direct-discount")),
                    hasProperty("promoDetails", allOf(
                            hasProperty("promoKey", is("direct-discount with fixed price")),
                            hasProperty("promoType", is("direct-discount")),
                            hasProperty("promoTypeEnum", is(PromoType.DIRECT_DISCOUNT)),
                            hasProperty("promoFixedPrice", comparesEqualTo(BigDecimal.valueOf(450))),
                            hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(550))),
                            hasProperty("promoFixedSubsidy", nullValue()),
                            hasProperty("allowPromocode", is(true)),
                            hasProperty("allowBeruBonus", is(true)),
                            hasProperty("hasDcoSubsidy", is(false))
                    ))
                    ), allOf(
                    hasProperty("promoMd5", is("promo-code promo")),
                    hasProperty("promoType", is("promo-code")),
                    hasProperty("promoDetails", allOf(
                            hasProperty("promoKey", is("promo-code promo")),
                            hasProperty("promoType", is("promo-code")),
                            hasProperty("promoTypeEnum", is(PromoType.PROMOCODE)),
                            hasProperty("promoFixedPrice", comparesEqualTo(BigDecimal.valueOf(450))),
                            hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(550))),
                            hasProperty("promoFixedSubsidy", nullValue()),
                            hasProperty("allowPromocode", is(true)),
                            hasProperty("allowBeruBonus", is(true)),
                            hasProperty("hasDcoSubsidy", is(false))
                    ))
                    ), allOf(
                    hasProperty("promoMd5", is("blue-flash promo")),
                    hasProperty("promoType", is("blue-flash")),
                    hasProperty("promoDetails", allOf(
                            hasProperty("promoKey", is("blue-flash promo")),
                            hasProperty("promoType", is("blue-flash")),
                            hasProperty("promoTypeEnum", is(PromoType.BLUE_FLASH)),
                            hasProperty("promoFixedPrice", comparesEqualTo(BigDecimal.valueOf(450))),
                            hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(550))),
                            hasProperty("promoFixedSubsidy", nullValue()),
                            hasProperty("allowPromocode", is(true)),
                            hasProperty("allowBeruBonus", is(true)),
                            hasProperty("hasDcoSubsidy", is(false))
                    ))
                    ), allOf(
                    hasProperty("promoMd5", is("RcPSuDjo_UPfRH82zDmtCA")),
                    hasProperty("promoType", is("cheapest-as-gift")),
                    hasProperty("promoDetails", allOf(
                            hasProperty("promoKey", is("RcPSuDjo_UPfRH82zDmtCA")),
                            hasProperty("promoType", is("cheapest-as-gift")),
                            hasProperty("promoTypeEnum", is(PromoType.CHEAPEST_AS_GIFT)),
                            hasProperty("allowPromocode", is(false)),
                            hasProperty("allowBeruBonus", is(false)),
                            hasProperty("hasDcoSubsidy", is(false))
                    ))
                    ), allOf(
                    hasProperty("promoMd5", is("TDEUCBiihdbkTGDh")),
                    hasProperty("promoType", is("cheapest-as-gift")),
                    hasProperty("promoDetails", allOf(
                            hasProperty("promoKey", is("TDEUCBiihdbkTGDh")),
                            hasProperty("promoType", is("cheapest-as-gift")),
                            hasProperty("promoTypeEnum", is(PromoType.CHEAPEST_AS_GIFT)),
                            hasProperty("allowPromocode", is(true)),
                            hasProperty("allowBeruBonus", is(false)),
                            hasProperty("hasDcoSubsidy", is(false))
                    ))
                    ), allOf(
                    hasProperty("promoMd5", is("FakeUniq")),
                    hasProperty("promoType", is("spread-discount-count")),
                    hasProperty("promoDetails", allOf(
                            hasProperty("promoKey", is("FakeUniq")),
                            hasProperty("promoType", is("spread-discount-count")),
                            hasProperty("promoTypeEnum", is(PromoType.SPREAD_DISCOUNT_COUNT)),
                            hasProperty("bounds", hasItems(
                                    allOf(
                                            hasProperty("countBound", equalTo(2)),
                                            hasProperty("receiptBound", nullValue()),
                                            hasProperty("countPercentDiscount",
                                                    comparesEqualTo(BigDecimal.valueOf(10))),
                                            hasProperty("countAbsoluteDiscount",
                                                    comparesEqualTo(BigDecimal.valueOf(20))),
                                            hasProperty("receiptPercentDiscount", nullValue()),
                                            hasProperty("receiptAbsoluteDiscount", nullValue())
                                    ),
                                    allOf(
                                            hasProperty("countBound", equalTo(5)),
                                            hasProperty("receiptBound", nullValue()),
                                            hasProperty("countPercentDiscount",
                                                    comparesEqualTo(BigDecimal.valueOf(20))),
                                            hasProperty("countAbsoluteDiscount",
                                                    comparesEqualTo(BigDecimal.valueOf(40))),
                                            hasProperty("receiptPercentDiscount", nullValue()),
                                            hasProperty("receiptAbsoluteDiscount", nullValue())
                                    )
                            )),
                            hasProperty("priority", is(1)),
                            hasProperty("promoBucketName", is("bucketName1")),
                            hasProperty("share", is(BigDecimal.valueOf(0.5))),
                            hasProperty("cmsDescriptionSemanticId", is("partner-default-cashback")),
                            hasProperty("partnerId", nullValue()),
                            hasProperty("marketTariffsVersionId", is(100L)),
                            hasProperty("maxOfferCashbackThresholds", hasItems(
                                    allOf(
                                            hasProperty("code", is("cumulative10K")),
                                            hasProperty("value", is(BigDecimal.valueOf(10000)))
                                    ),
                                    allOf(
                                            hasProperty("code", is("cumulative20K")),
                                            hasProperty("value", is(BigDecimal.valueOf(20000)))
                                    ),
                                    allOf(
                                            hasProperty("code", is("cumulative30K")),
                                            hasProperty("value", is(BigDecimal.valueOf(30000)))
                                    )
                            )),
                            hasProperty("minOrderTotalThresholds", hasItems(
                                    allOf(
                                            hasProperty("code", is("threshold3K")),
                                            hasProperty("value", is(BigDecimal.valueOf(3000)))
                                    ),
                                    allOf(
                                            hasProperty("code", is("threshold2K")),
                                            hasProperty("value", is(BigDecimal.valueOf(2000)))
                                    )
                            )),
                            hasProperty("sourceType", is("anaplan"))
                    ))
                    ), allOf(
                    hasProperty("promoMd5", is("FakeUniq2")),
                    hasProperty("promoType", is("spread-discount-receipt")),
                    hasProperty("promoDetails", allOf(
                            hasProperty("promoKey", is("FakeUniq2")),
                            hasProperty("promoType", is("spread-discount-receipt")),
                            hasProperty("promoTypeEnum", is(PromoType.SPREAD_DISCOUNT_RECEIPT)),
                            hasProperty("bounds", hasItems(
                                    allOf(
                                            hasProperty("countBound", nullValue()),
                                            hasProperty("receiptBound",
                                                    comparesEqualTo(BigDecimal.valueOf(1000))),
                                            hasProperty("countPercentDiscount", nullValue()),
                                            hasProperty("countAbsoluteDiscount", nullValue()),
                                            hasProperty("receiptPercentDiscount",
                                                    comparesEqualTo(BigDecimal.valueOf(10))),
                                            hasProperty("receiptAbsoluteDiscount",
                                                    comparesEqualTo(BigDecimal.valueOf(100)))
                                    ),
                                    allOf(
                                            hasProperty("countBound", nullValue()),
                                            hasProperty("receiptBound",
                                                    comparesEqualTo(BigDecimal.valueOf(2000))),
                                            hasProperty("countPercentDiscount", nullValue()),
                                            hasProperty("countAbsoluteDiscount", nullValue()),
                                            hasProperty("receiptPercentDiscount",
                                                    comparesEqualTo(BigDecimal.valueOf(20))),
                                            hasProperty("receiptAbsoluteDiscount",
                                                    comparesEqualTo(BigDecimal.valueOf(200)))
                                    )
                            )),
                            hasProperty("priority", is(2)),
                            hasProperty("promoBucketName", is("bucketName2")),
                            hasProperty("share", is(BigDecimal.valueOf(0.03))),
                            hasProperty("cmsDescriptionSemanticId", is("market-default-cashback")),
                            hasProperty("partnerId", is(1003L)),
                            hasProperty("marketTariffsVersionId", is(100L)),
                            hasProperty("maxOfferCashbackThresholds", hasItem(
                                    allOf(
                                            hasProperty("code", is("cumulative10K")),
                                            hasProperty("value", is(BigDecimal.valueOf(10000)))
                                    )
                            )),
                            hasProperty("minOrderTotalThresholds", hasItem(
                                    allOf(
                                            hasProperty("code", is("threshold3K")),
                                            hasProperty("value", is(BigDecimal.valueOf(3000)))
                                    )
                            )),
                            hasProperty("additionalOffers", hasItem(
                                    allOf(
                                            hasProperty("feeShow", is("OTNDnItfwROQypMce5YPJ_UL80lG7a4UIOMphyPK" +
                                                    "cua9ROB3zsqo_k7iY2ORifN70t8b8eAV39hvozTx16p3sbD6TVhVMtIdpvrxWcms" +
                                                    "AvwFVcnGisU9XIVSvCoSdWZQ")),
                                            hasProperty("showUid", is("")),
                                            hasProperty("wareId", is("HrsZKR2fE2q2WCAuB_U3Tg")),
                                            hasProperty("urls", is("https://delonghi.ru/product/delonghi-nabor-" +
                                                    "dlsc317")),
                                            hasProperty("priceCurrency", is(Currency.RUR)),
                                            hasProperty("price", is(BigDecimal.valueOf(3455))),
                                            hasProperty("primaryPriceCurrency", is(Currency.RUR)),
                                            hasProperty("primaryPrice", is(BigDecimal.valueOf(41535))),
                                            hasProperty("totalPriceCurrency", is(Currency.RUR)),
                                            hasProperty("totalPrice", is(BigDecimal.valueOf(44990))),
                                            hasProperty("totalOldPriceCurrency", is(Currency.RUR)),
                                            hasProperty("totalOldPrice", is(BigDecimal.valueOf(48480)))
                                    )
                            )),
                            hasProperty("sourceType", is("partner_source"))
                            )
                    ))
            ));
        }
    }

    @Test
    public void testMultiPromoParse() throws IOException {
        try (InputStream in =
                     OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files/offer_info_with_promo.json")
        ) {
            List<FoundOffer> parsedOffers = parser.parse(in);
            FoundOffer offer = Iterables.getOnlyElement(parsedOffers);
            List<OfferPromo> promos = offer.getPromos();

            assertEquals(8, promos.size());

            OfferPromo promo1 = new OfferPromo();
            promo1.setPromoType("direct-discount");
            promo1.setPromoMd5("direct-discount with subsidy");
            promo1.setPromoDetails(
                    PromoDetails.builder()
                            .promoKey("direct-discount with subsidy")
                            .promoType("direct-discount")
                            .build()
            );

            OfferPromo promo2 = new OfferPromo();
            promo2.setPromoType("direct-discount");
            promo2.setPromoMd5("direct-discount with fixed price");
            promo2.setPromoStock(BigDecimal.valueOf(50));
            promo2.setPromoDetails(
                    PromoDetails.builder()
                            .promoKey("direct-discount with fixed price")
                            .promoType("direct-discount")
                            .build()
            );

            OfferPromo promo3 = new OfferPromo();
            promo3.setPromoType("blue-flash");
            promo3.setPromoMd5("blue-flash promo");
            promo3.setPromoDetails(
                    PromoDetails.builder()
                            .promoKey("blue-flash promo")
                            .promoType("blue-flash")
                            .build()
            );

            OfferPromo promo4 = new OfferPromo();
            promo4.setPromoType("cheapest-as-gift");
            promo4.setPromoMd5("RcPSuDjo_UPfRH82zDmtCA");
            promo4.setPromoDetails(
                    PromoDetails.builder()
                            .promoKey("RcPSuDjo_UPfRH82zDmtCA")
                            .promoType("cheapest-as-gift")
                            .build()
            );

            OfferPromo promo5 = new OfferPromo();
            promo5.setPromoType("cheapest-as-gift");
            promo5.setPromoMd5("TDEUCBiihdbkTGDh");
            promo5.setPromoDetails(
                    PromoDetails.builder()
                            .promoKey("TDEUCBiihdbkTGDh")
                            .promoType("cheapest-as-gift")
                            .build()
            );

            OfferPromo promo6 = new OfferPromo();
            promo6.setPromoType("promo-code");
            promo6.setPromoMd5("promo-code promo");
            promo6.setPromoDetails(
                    PromoDetails.builder()
                            .promoKey("promo-code promo")
                            .promoType("promo-code")
                            .build()
            );

            OfferPromo promo7 = new OfferPromo();
            promo7.setPromoType("spread-discount-count");
            promo7.setPromoMd5("FakeUniq");
            promo7.setPromoDetails(
                    PromoDetails.builder()
                            .promoKey("FakeUniq")
                            .promoType("spread-discount-count")
                            .bounds(new HashSet<>(Arrays.asList(
                                    PromoBound.builder()
                                            .countBound(2)
                                            .countPercentDiscount(BigDecimal.valueOf(10))
                                            .countAbsoluteDiscount(BigDecimal.valueOf(20))
                                            .build(),
                                    PromoBound.builder()
                                            .countBound(5)
                                            .countPercentDiscount(BigDecimal.valueOf(20))
                                            .countAbsoluteDiscount(BigDecimal.valueOf(40))
                                            .build()
                            )))
                            .priority(1)
                            .promoBucketName("bucketName1")
                            .share(BigDecimal.valueOf(0.5))
                            .cmsDescriptionSemanticId("partner-default-cashback")
                            .marketTariffsVersionId(100L)
                            .maxOfferCashbackThresholds(new HashSet<>(Arrays.asList(
                                    PromoThreshold.builder()
                                            .code("cumulative10K")
                                            .value(BigDecimal.valueOf(10000))
                                            .build(),
                                    PromoThreshold.builder()
                                            .code("cumulative20K")
                                            .value(BigDecimal.valueOf(20000))
                                            .build(),
                                    PromoThreshold.builder()
                                            .code("cumulative30K")
                                            .value(BigDecimal.valueOf(30000))
                                            .build()
                            )))
                            .minOrderTotalThresholds(new HashSet<>(Arrays.asList(
                                    PromoThreshold.builder()
                                            .code("threshold3K")
                                            .value(BigDecimal.valueOf(3000))
                                            .build(),
                                    PromoThreshold.builder()
                                            .code("threshold2K")
                                            .value(BigDecimal.valueOf(2000))
                                            .build()
                            )))
                            .sourceType("anaplan")
                            .build()
            );
            OfferPromo promo8 = new OfferPromo();
            promo8.setPromoType("spread-discount-receipt");
            promo8.setPromoMd5("FakeUniq2");
            promo8.setPromoDetails(
                    PromoDetails.builder()
                            .promoKey("FakeUniq2")
                            .promoType("spread-discount-receipt")
                            .bounds(new HashSet<>(Arrays.asList(
                                    PromoBound.builder()
                                            .receiptBound(BigDecimal.valueOf(1000))
                                            .receiptPercentDiscount(BigDecimal.valueOf(10))
                                            .receiptAbsoluteDiscount(BigDecimal.valueOf(100))
                                            .build(),
                                    PromoBound.builder()
                                            .receiptBound(BigDecimal.valueOf(2000))
                                            .receiptPercentDiscount(BigDecimal.valueOf(20))
                                            .receiptAbsoluteDiscount(BigDecimal.valueOf(200))
                                            .build()
                            )))
                            .share(BigDecimal.valueOf(0.03))
                            .priority(2)
                            .promoBucketName("bucketName2")
                            .cmsDescriptionSemanticId("market-default-cashback")
                            .marketTariffsVersionId(100L)
                            .partnerId(1003L)
                            .maxOfferCashbackThresholds(new HashSet<PromoThreshold>() {
                                {
                                    add(PromoThreshold.builder()
                                            .code("cumulative10K")
                                            .value(BigDecimal.valueOf(10000))
                                            .build());
                                }
                            })
                            .minOrderTotalThresholds(new HashSet<PromoThreshold>() {
                                {
                                    add(PromoThreshold.builder()
                                            .code("threshold3K")
                                            .value(BigDecimal.valueOf(3000))
                                            .build());
                                }
                            })
                            .additionalOffers(new HashSet<AdditionalOffer>() {
                                {
                                    add(AdditionalOffer.builder()
                                            .feeShow("OTNDnItfwROQypMce5YPJ_UL80lG7a4UIOMphyPKcua9ROB3zsqo_k7iY2ORifN" +
                                                    "70t8b8eAV39hvozTx16p3sbD6TVhVMtIdpvrxWcmsAvwFVcnGisU9XIVSvCoSdWZQ")
                                            .showUid("")
                                            .wareId("HrsZKR2fE2q2WCAuB_U3Tg")
                                            .urls("https://delonghi.ru/product/delonghi-nabor-dlsc317")
                                            .priceCurrency(Currency.RUR)
                                            .price(BigDecimal.valueOf(3455))
                                            .primaryPriceCurrency(Currency.RUR)
                                            .primaryPrice(BigDecimal.valueOf(41535))
                                            .totalPriceCurrency(Currency.RUR)
                                            .totalPrice(BigDecimal.valueOf(44990))
                                            .totalOldPriceCurrency(Currency.RUR)
                                            .totalOldPrice(BigDecimal.valueOf(48480))
                                            .build());
                                }
                            })
                            .sourceType("partner_source")
                            .build()
            );

            assertThat(promos, containsInAnyOrder(
                    promo1,
                    promo2,
                    promo3,
                    promo4,
                    promo5,
                    promo6,
                    promo7,
                    promo8
            ));
        }
    }

    @Test
    public void testCpa20() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_promo.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertEquals(Boolean.TRUE, offer.isCpa20());
        }
    }

    @Test
    public void testPrepayEnabledRecommendedByVendorAndShopComment() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertEquals(Boolean.FALSE, offer.isRecommendedByVendor());
            assertEquals(Boolean.FALSE, offer.isPrepayEnabled());

            assertNotNull(offer.getWarningsRaw());
            assertEquals(1, offer.getWarningsRaw().length());

            assertNotNull(offer.getFiltersRaw());
            assertEquals(1, offer.getFiltersRaw().length());

            assertNotNull(offer.getPicturesRaw());
            assertEquals(1, offer.getPicturesRaw().length());
        }
    }

    @Test
    public void testPictures() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertNotNull(offer.getMainPicture());
            assertEquals(17, offer.getMainPicture().size());
            OfferPicture firstPicture = offer.getMainPicture().iterator().next();
            assertNotNull(firstPicture);
            assertEquals("//0.cs-ellpic01gt.yandex.ru/market_cdCgPBgxO_mtmIJDV49_jw_50x50.jpg", firstPicture.getUrl());

            assertNotNull(offer.getPictures());
            assertEquals(17, offer.getPictures().size());
        }
    }

    @Test
    public void testPrices() throws IOException {
        try (InputStream resourceAsStream =
                     OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertNotNull(offer.getPrice());
            assertEquals(BigDecimal.valueOf(275), offer.getPrice());

            assertNotNull(offer.getOldMin());
            assertEquals(BigDecimal.valueOf(1500), offer.getOldMin());

            assertNotNull(offer.getOldDiscountOldMin());
            assertEquals(BigDecimal.valueOf(1300), offer.getOldDiscountOldMin());
        }
    }

    @Test
    public void testReturnDeliveryAddressAndReturnDeliveryPolicy() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertEquals("Москва, Тестовая, дом 1, 123456", offer.getReturnDeliveryAddress());
            assertEquals("7d", offer.getReturnPolicy());
        }
    }

    @Test
    public void testDownloadable() throws IOException {
        for (Pair<String, Boolean> p :
            // файл-пример и что ожидать в downloadable
                Arrays.asList(
                        Pair.of("/files/offer_info.json", false),
                        Pair.of("/files/offer_info_with_downloadable.json", true),
                        Pair.of("/files/offer_info_without_downloadable.json", false)
                )) {
            try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class
                    .getResourceAsStream(p.getFirst())) {
                List<FoundOffer> parse = parser.parse(resourceAsStream);

                FoundOffer offer = Iterables.getOnlyElement(parse);
                assertEquals("Expected downloadable=" + p.getSecond() + " for sample: " + p.getFirst(),
                        p.getSecond(), offer.isDownloadable());
            }
        }
    }

    @Test
    public void testSubscription() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class
                .getResourceAsStream("/files/offer_info_subscription_prime.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);
            assertEquals("prime", offer.getSubscriptionName());
        }
    }

    @Test
    public void testPreorder() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class
                .getResourceAsStream("/files/offer_info_preorder.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);
            assertThat(offer.isPreorder(), is(true));
        }
    }

    @Test
    public void shouldParseLocalDelivery() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_delivery_options.json")) {
            List<FoundOffer> parse = parserWithLocalDelivery.parse(resourceAsStream);
            assertEquals(2, parse.size());

            FoundOffer firstOffer = parse.get(0);
            assertEquals("Оплата:наличные,банк.карты,баллы \"С-Клуб\",кредит", firstOffer.getSellerComment());

            Collection<LocalDeliveryOption> localDelivery = firstOffer.getLocalDelivery();
            assertEquals(3, localDelivery.size());

            LocalDeliveryOption first = Iterables.get(localDelivery, 0);
            assertThat(first.getDeliveryServiceId(), Matchers.allOf(Matchers.notNullValue(), Matchers.is(99L)));
            assertEquals(24, (long) first.getOrderBefore());

            LocalDeliveryOption second = Iterables.get(localDelivery, 1);
            assertEquals(14, second.getDayFrom().intValue());
            assertEquals(14, second.getDayTo().intValue());
            assertEquals(50.14d, second.getCost().doubleValue(), DELTA);
            assertEquals(Currency.RUR, second.getCurrency());
            assertEquals(24, (long) second.getOrderBefore());

            LocalDeliveryOption third = Iterables.get(localDelivery, 2);
            assertEquals(18, (long) third.getOrderBefore());
            assertThat(first.getPaymentMethods(), Matchers.allOf(Matchers.<String>iterableWithSize(1),
                    contains("CARD_ON_DELIVERY")));
            assertThat(second.getPaymentMethods(), Matchers.allOf(Matchers.<String>iterableWithSize(1),
                    contains("YANDEX")));
            assertThat(third.getPaymentMethods(), Matchers.allOf(Matchers.<String>iterableWithSize(1),
                    contains("CASH_ON_DELIVERY")));
        }
    }

    @Test
    public void shouldParsePostDelivery() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_post_options.json")) {
            List<FoundOffer> parse = parserWithLocalDelivery.parse(resourceAsStream);
            assertEquals(1, parse.size());

            FoundOffer firstOffer = parse.get(0);
            assertEquals("Оплата:наличные,банк.карты,баллы \"С-Клуб\",кредит", firstOffer.getSellerComment());

            List<PickupOption> postDelivery = firstOffer.getPost();
            assertEquals(1, postDelivery.size());

            PickupOption postOption = postDelivery.get(0);
            assertThat(postOption.getDeliveryServiceId(), Matchers.allOf(Matchers.notNullValue(), Matchers.is(142L)));
            assertEquals(24, (long) postOption.getOrderBefore());
            assertEquals(6, postOption.getDayFrom().intValue());
            assertEquals(8, postOption.getDayTo().intValue());
            assertTrue(postOption.isMarketBranded());

            assertThat(postOption.getPrice(), comparesEqualTo(new BigDecimal("248")));

            assertThat(postOption.getOutlet(), notNullValue());
            assertThat(postOption.getOutlet().getId(), is(24428975L));
            assertThat(postOption.getOutlet().getType(), is("post"));
            assertThat(postOption.getOutlet().getPostCode(), is("117216"));
            assertThat(postOption.getOutlet().getPurpose(), hasItems("post"));
        }
    }

    @Test
    public void shouldParseEnglishNameAndCargoType() throws Exception {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class
                .getResourceAsStream("/files/offer_info_english_name.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);
            assertThat(offer.getCargoType(), is(1));
            assertThat(offer.getEnglishName(), is("englishName"));
        }
    }

    @Test
    public void shouldParseRawParams() throws Exception {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class
                .getResourceAsStream("/files/offer_info_raw_params.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);
            assertThat(offer.getRawParams(), notNullValue());
            assertThat(offer.getRawParams(), hasSize(6));

            RawParam rawParam = offer.getRawParams().stream().filter(rp -> "Material".equals(rp.getName()))
                    .findFirst()
                    .orElse(null);

            assertThat(rawParam, notNullValue());
            assertThat(rawParam.getValue(), is("%100 POLYESTER"));

        }
    }

    @Test
    public void testSupplierInformation() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class
                .getResourceAsStream("/files/offer_info_with_supplier_information.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);
            assertThat(offer.getSupplierDescription(), is("Информация о товаре от поставщика"));
            assertThat(offer.getSupplierWorkSchedule(), is("Пн-Пт: 10:00-19:00, Сб-Вс: 10:00-18:00"));
            assertThat(offer.getManufacturerCountries().get(0).getId().get(), is(213));
            assertThat(offer.getManufacturerCountries().get(0).getName().get(), is("Москва"));
            assertThat(
                    offer.getManufacturerCountries().get(0).getLingua().get().getName().get().getGenitive().get(),
                    is("Москвы")
            );
            assertThat(offer.getManufacturerCountries().get(1).getId().get(), is(65));
            assertThat(offer.getManufacturerCountries().get(1).getName().get(), is("Новосибирск"));
            assertThat(
                    offer.getManufacturerCountries().get(1).getLingua().get().getName().get().getGenitive().get(),
                    is("Новосибирска")
            );
        }
    }

    @Test
    public void shouldParseCargoTypes() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertEquals(0, offer.getCargoType());
            assertThat(offer.getCargoTypes(), containsInAnyOrder(is(1), is(2), is(3)));
        }
    }

    @Test
    public void testOfferSeller() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);
            assertNotNull(offer.getOfferSeller());
            OfferSeller offerSeller = offer.getOfferSeller();
            assertEquals(new BigDecimal("274.99"), offerSeller.getPrice());
            assertEquals(Currency.RUR, offerSeller.getCurrency());
            assertEquals(new BigDecimal("1"), offerSeller.getSellerToUserExchangeRate());
            assertEquals("comment", offerSeller.getComment());
            assertNotNull(offerSeller.getMarkupData());
            MarkupData markupData = offerSeller.getMarkupData();
            assertEquals(Long.valueOf(1550439631453L), markupData.getUpdateTime());
            assertEquals(new BigDecimal("952.3809524"), markupData.getPartnerPrice());
            assertEquals("VendorOfferito", markupData.getVendorString());
            assertNotNull(markupData.getCoefficients());
            assertEquals(new BigDecimal("0.5"), markupData.getCoefficients().get("marketCommission"));
            assertEquals(new BigDecimal("1.61"), markupData.getCoefficients().get("marketMultiplier"));
            assertEquals(new BigDecimal("2.1"), markupData.getCoefficients().get("vendorCharge"));
        }
    }

    @Test
    public void testOfferServices() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream(
                "/files/offer_info_with_services.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer offer = Iterables.getOnlyElement(parse);
            assertThat(offer.getServices(), hasSize(1));

            OfferService service = offer.getServices().get(0);
            assertNotNull(service);
            assertEquals(123L, service.getServiceId());
            assertEquals("String", service.getTitle());
            assertEquals("Text", service.getDescription());
            assertEquals("service id", service.getYaServiceId());
            assertEquals(BigDecimal.valueOf(12345, 2), service.getPrice());
            assertEquals(Currency.RUR, service.getCurrency());
        }
    }

    @Test
    public void shouldParseDeliveryPartnerTypes() throws IOException {
        InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class
                .getResourceAsStream("/files/offer_info.json");
        List<FoundOffer> parse = parser.parse(resourceAsStream);

        FoundOffer offer = Iterables.getOnlyElement(parse);

        assertThat(offer.getDeliveryPartnerTypes(), hasSize(2));
        assertThat(offer.getDeliveryPartnerTypes(), hasItems("YANDEX_MARKET", "SHOP"));
    }

    @Test
    public void shouldParseAtSupplierWarehouse() throws IOException {
        InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class
                .getResourceAsStream("/files/offer_info.json");
        List<FoundOffer> parse = parser.parse(resourceAsStream);

        FoundOffer offer = Iterables.getOnlyElement(parse);

        assertThat(offer.isAtSupplierWarehouse(), is(true));
    }

    @Test
    public void shouldParseExternalFeedId() throws IOException {
        InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class
                .getResourceAsStream("/files/offer_info.json");
        List<FoundOffer> parse = parser.parse(resourceAsStream);

        FoundOffer offer = Iterables.getOnlyElement(parse);

        assertThat(offer.getExternalFeedId(), is(456789L));
    }

    @Test
    public void shouldParseFulfilmentWarehouseId() throws IOException {
        InputStream resourceAtStream = OfferInfoMarketReportJsonParserTest.class
                .getResourceAsStream("/files/offer_info.json");
        List<FoundOffer> parse = parser.parse(resourceAtStream);

        FoundOffer offer = Iterables.getOnlyElement(parse);

        assertThat(offer.getFulfillmentWarehouseId(), equalTo(172L));
    }

    @Test
    public void shouldParseDynamicPrice3P() throws IOException {
        InputStream resourceAtStream = OfferInfoMarketReportJsonParserTest.class
                .getResourceAsStream("/files/offer_info.json");
        List<FoundOffer> parse = parser.parse(resourceAtStream);

        FoundOffer offer = Iterables.getOnlyElement(parse);

        assertThat(offer.getRefMinPrice(), comparesEqualTo(new BigDecimal("200")));
        assertThat(offer.getRefMinPriceCurrency(), equalTo(Currency.RUR));
        assertThat(offer.isGoldenMatrix(), is(true));
        assertThat(offer.getDynamicPriceStrategy(), is(2));
    }

    @Test
    public void testRgb() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_colors.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);

            FoundOffer blueOffer = parse.stream()
                    .filter(it -> it.getFeedOfferId().getId().equals("6"))
                    .findAny()
                    .orElseThrow(AssertionFailedError::new);
            assertNotNull(blueOffer.getRgb());
            assertEquals(Color.BLUE, blueOffer.getRgb());

            FoundOffer turboOffer = parse.stream()
                    .filter(it -> it.getFeedOfferId().getId().equals("7"))
                    .findAny()
                    .orElseThrow(AssertionFailedError::new);
            assertNotNull(turboOffer.getRgb());
            assertEquals(Color.TURBO, turboOffer.getRgb());

            FoundOffer whiteOffer = parse.stream()
                    .filter(it -> it.getFeedOfferId().getId().equals("8"))
                    .findAny()
                    .orElseThrow(AssertionFailedError::new);
            assertNotNull(whiteOffer.getRgb());
            assertEquals(Color.WHITE, whiteOffer.getRgb());
        }
    }

    @Test
    public void testAdult() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_without_adult.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            assertTrue(offers.get(0).isAdult());
        }
    }

    @Test
    public void testIsExpress() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_delivery_options.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            assertTrue(offers.get(0).isExpress());
        }
    }

    @Test
    public void testIsExpressNull() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            assertFalse(offers.get(0).isExpress());
        }
    }

    @Test
    public void testIsEda() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_delivery_options.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            assertTrue(offers.get(0).isEda());
        }
    }

    @Test
    public void testIsEdaNull() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            assertFalse(offers.get(0).isEda());
        }
    }

    @Test
    public void testIsEats() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_delivery_options.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            assertEquals(FoodtechType.LAVKA, offers.get(0).getFoodtechType()); // lavka -> LAVKA
            assertEquals(FoodtechType.UNKNOWN, offers.get(1).getFoodtechType()); // unknown_type -> UNKNOWN
        }
    }

    @Test
    public void testIsEatsNullConvertsToFalse() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            assertEquals(FoodtechType.UNKNOWN, offers.get(0).getFoodtechType()); // null -> UNKNOWN
        }
    }

    @Test
    public void testIsLargeSize() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_large_size.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            assertTrue(offers.get(0).isLargeSize());
        }
    }

    @Test
    public void testLargeSizeIsNull() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            assertNull(offers.get(0).isLargeSize());
        }
    }

    @Test
    public void testIsSample() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_is_sample.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            assertTrue(offers.get(0).isSample());
        }
    }

    @Test
    public void testIsSampleFalse() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> offers = parser.parse(resourceAsStream);

            assertFalse(offers.get(0).isSample());
        }
    }

    @Test
    public void testPayByYaPlus() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(parse);
            assertThat(offer.getPayByYaPlus(), is(not(nullValue())));
            assertThat(offer.getPayByYaPlus().getPrice(), is(990));
        }
    }

    @Test
    public void testInt64ModelId() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_int64_model.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertThat(offer.getHyperId(), is(2000006450728L));
        }
    }

    @Test
    public void testYandexBnplInfoTrue() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_yandex_bnpl_info_enabled.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertThat(offer.getInstallmentsInfoSet(), empty());
            assertTrue(offer.getYandexBnplInfo().isEnabled());
            assertNull(offer.getYandexBnplInfo().getBnplDenial());
        }
    }

    @Test
    public void testYandexBnplInfoFalse() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_yandex_bnpl_info_disabled.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(parse);
            assertThat(offer.getInstallmentsInfoSet(), empty());
            assertFalse(offer.getYandexBnplInfo().isEnabled());
            BnplDenial bnplDenial = offer.getYandexBnplInfo().getBnplDenial();
            assertThat(bnplDenial.getReason(), equalTo(TOO_CHEAP));
            assertThat(bnplDenial.getThreshold(), equalTo(new BigDecimal(2000)));
        }
    }

    @Test
    public void testInstallmentInfo() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_installments_options.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertThat(offer.getInstallmentsInfoSet(), equalTo(getExpectedInstallmentsInfoSet()));
            assertTrue(offer.getYandexBnplInfo().isEnabled());
        }
    }

    @Test
    public void testUniqueOrder() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertTrue(offer.isUniqueOffer());
        }
    }

    @Test
    public void testParallelImport() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_parallel_import.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertTrue(offer.isParallelImport());
            assertNotNull(offer.getOfferSeller());
            assertEquals("P1Y2M10DT2H30M", offer.getOfferSeller().getWarrantyPeriod());
            assertEquals("CHARGE_BACK", offer.getParallelImportWarrantyAction());
        }
    }

    @Test
    public void testNoParallelImport() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(parse);

            assertFalse(offer.isParallelImport());
            assertNotNull(offer.getOfferSeller());
            assertNull(offer.getOfferSeller().getWarrantyPeriod());
            assertNull(offer.getParallelImportWarrantyAction());
        }
    }

    @Test
    public void testCancelPolicy() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(parse);

            OrderCancelPolicy cancelPolicy = offer.getOrderCancelPolicy();
            assertNotNull(cancelPolicy);

            assertEquals(CancelType.TIME_LIMIT, cancelPolicy.getType());
            assertEquals((Integer) 3, cancelPolicy.getDaysForCancel());
            assertEquals("unique-order", cancelPolicy.getReason());
        }
    }

    @Test
    public void shouldParseResaleSpecs() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info_with_resale_specs.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(parse);
            ResaleSpecs expectedResaleSpecs = new ResaleSpecs();
            expectedResaleSpecs.setConditionValue("resale_perfect");
            expectedResaleSpecs.setConditionText("Хорошее");
            expectedResaleSpecs.setReasonValue("1");
            expectedResaleSpecs.setReasonText("Б/У");
            assertEquals(expectedResaleSpecs, offer.getResaleSpecs());
        }
    }

    @Test
    public void shouldNotRecognizeEmptyResaleSpecsAsResaleSpecsWithNullFields() throws IOException {
        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/offer_info.json")) {
            List<FoundOffer> parse = parser.parse(resourceAsStream);
            FoundOffer offer = Iterables.getOnlyElement(parse);
            assertNull(offer.getResaleSpecs());
        }
    }

    private Set<InstallmentsInfo> getExpectedInstallmentsInfoSet() {
        InstallmentsInfo firstInstallment = new InstallmentsInfo();
        firstInstallment.setTerm(Double.parseDouble("6"));
        MonthlyPayment firstMonthlyPayment = new MonthlyPayment();
        firstMonthlyPayment.setCurrency("RUR");
        firstMonthlyPayment.setValue(new BigDecimal("363.5"));
        firstInstallment.setMonthlyPayment(firstMonthlyPayment);
        firstInstallment.setBnplAvailable(true);

        InstallmentsInfo secondInstallment = new InstallmentsInfo();
        secondInstallment.setTerm(Double.parseDouble("12"));
        MonthlyPayment secondMonthlyPayment = new MonthlyPayment();
        secondMonthlyPayment.setCurrency("RUR");
        secondMonthlyPayment.setValue(new BigDecimal("700.5"));
        secondInstallment.setMonthlyPayment(secondMonthlyPayment);
        secondInstallment.setBnplAvailable(true);

        Set<InstallmentsInfo> resultSet = new HashSet<>();
        resultSet.add(firstInstallment);
        resultSet.add(secondInstallment);
        return resultSet;
    }
}
