package ru.yandex.market.api.partner.controllers.offers.stored;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.api.partner.controllers.offers.stored.base.OfferResultDTO;
import ru.yandex.market.api.partner.controllers.offers.stored.base.OfferUpdateDTO;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.offer.IndexerOfferKey;
import ru.yandex.market.core.offer.KnownShopIndexerOfferKey;
import ru.yandex.market.core.offer.OfferUpdate;
import ru.yandex.market.core.price.PapiOfferPriceValue;
import ru.yandex.market.core.tax.model.ShopVat;
import ru.yandex.market.core.tax.model.TaxSystem;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.functional.Changed;

@ParametersAreNonnullByDefault
class OfferValidationPipelineTest {
    private static final ObjectMapper OBJECT_MAPPER = new ApiObjectMapperFactory().createJsonMapper();
    private static final LocalValidatorFactoryBean SPRING_VALIDATOR = createValidator();

    private static final long SHOP_ID = 774;
    private static final long SUPPLIER_ID = 93423;
    private static final long SUPPLIER_FEED_ID = 234720;

    private static final long SHOP_FEED_ID1 = 956394L;
    private static final long SHOP_FEED_ID2 = 956397L;
    private static final List<Long> TWO_SHOP_FEED_IDS = Arrays.asList(SHOP_FEED_ID1, SHOP_FEED_ID2);

    static Collection<Arguments> testSupplierDiscountsArguments() {
        return Arrays.asList(
                // 99%
                Arguments.of(false, Changed.fromTo(100L, 1L).map(BigDecimal::valueOf)),

                // 99%
                Arguments.of(false, Changed.fromTo(50_000L, 500L).map(BigDecimal::valueOf)),

                // Больше 95%
                Arguments.of(false, Changed.fromTo(100L, 4L).map(BigDecimal::valueOf)),

                // Больше 95%
                Arguments.of(false, Changed.fromTo(101L, 5L).map(BigDecimal::valueOf)),

                Arguments.of(true, Changed.fromTo(100L, 5L).map(BigDecimal::valueOf)),
                Arguments.of(true, Changed.fromTo(100L, 50L).map(BigDecimal::valueOf)),
                Arguments.of(true, Changed.fromTo(100L, 95L).map(BigDecimal::valueOf)),
                Arguments.of(false, Changed.fromTo(100L, 96L).map(BigDecimal::valueOf)),

                // Скидка в 500 рублей и это составляет 1%
                Arguments.of(true, Changed.fromTo(50_000L, 49_500L).map(BigDecimal::valueOf)),

                // Скидка в 500 рублей и это составляет 2%
                Arguments.of(true, Changed.fromTo(25_000L, 24_500L).map(BigDecimal::valueOf)),

                // Скидка в 500 рублей и это составляет 0.9%, не валидна, так как меньще 1%
                Arguments.of(false, Changed.fromTo(55555, 55055).map(BigDecimal::valueOf)),

                // Скидка в 500 рублей и это составляет 0.5%, не валидна, так как меньще 1%
                Arguments.of(false, Changed.fromTo(100_000L, 99_500L).map(BigDecimal::valueOf)),

                // Скидка в 499 рублей и это составляет 1%, не валидна, так как меньще 500 рублей
                Arguments.of(false, Changed.fromTo(49_900L, 49_401L).map(BigDecimal::valueOf)),

                // Скидка в 498 рублей и это составляет 2%, не валидна, так как меньще 500 рублей
                Arguments.of(false, Changed.fromTo(24_900L, 24_402L).map(BigDecimal::valueOf)),

                // Скидка в 499 рублей и это составляет 5%
                Arguments.of(true, Changed.fromTo(9_980L, 9_481L).map(BigDecimal::valueOf))
        );
    }

    static Collection<Arguments> testShopDiscountsArguments() {
        return Arrays.asList(
                // 99%
                Arguments.of(false, Changed.fromTo(100L, 1L).map(BigDecimal::valueOf)),

                // 99%
                Arguments.of(false, Changed.fromTo(50_000L, 500L).map(BigDecimal::valueOf)),

                // Больше 95%
                Arguments.of(false, Changed.fromTo(100L, 4L).map(BigDecimal::valueOf)),

                // Больше 95%
                Arguments.of(false, Changed.fromTo(101L, 5L).map(BigDecimal::valueOf)),

                Arguments.of(true, Changed.fromTo(100L, 5L).map(BigDecimal::valueOf)),
                Arguments.of(true, Changed.fromTo(100L, 50L).map(BigDecimal::valueOf)),
                Arguments.of(true, Changed.fromTo(100L, 95L).map(BigDecimal::valueOf)),
                Arguments.of(false, Changed.fromTo(100L, 96L).map(BigDecimal::valueOf)),

                // Скидка в 500 рублей и это составляет 1%
                Arguments.of(false, Changed.fromTo(50_000L, 49_500L).map(BigDecimal::valueOf)),

                // Скидка в 500 рублей и это составляет 2%
                Arguments.of(false, Changed.fromTo(25_000L, 24_500L).map(BigDecimal::valueOf)),

                // Скидка в 500 рублей и это составляет 0.9%
                Arguments.of(false, Changed.fromTo(55555, 55055).map(BigDecimal::valueOf)),

                // Скидка в 500 рублей и это составляет 0.5%
                Arguments.of(false, Changed.fromTo(100_000L, 99_500L).map(BigDecimal::valueOf)),

                // Скидка в 499 рублей и это составляет 1%
                Arguments.of(false, Changed.fromTo(49_900L, 49_401L).map(BigDecimal::valueOf)),

                // Скидка в 498 рублей и это составляет 2%
                Arguments.of(false, Changed.fromTo(24_900L, 24_402L).map(BigDecimal::valueOf)),

                // Скидка в 499 рублей и это составляет 5%
                Arguments.of(true, Changed.fromTo(9_980L, 9_481L).map(BigDecimal::valueOf))
        );
    }

    static Collection<Arguments> testFractionalDiscountsArguments() {
        return Arrays.asList(
                // Больше 95%
                Arguments.of(Changed.fromTo("100.3", "5").map(BigDecimal::new)),

                // Скидка в 499.99 рублей и это состовляет почти 2%
                Arguments.of(Changed.fromTo("24999.99", "24500").map(BigDecimal::new)),

                // Скидка в 499.99 рублей и это состовляет чуть больше 1%
                Arguments.of(Changed.fromTo("49998.99", "49499").map(BigDecimal::new))
        );
    }

    @Nonnull
    private static OfferValidationResult assertUpdateValidationResult(
            String expectedOfferResultJson,
            String offerUpdateJson,
            Supplier<OfferValidationPipelineFactory> pipelineFactory
    ) throws IOException {
        OfferUpdateDTO offerUpdate = OBJECT_MAPPER.readValue(offerUpdateJson, OfferUpdateDTO.class);
        OfferValidationPipeline offerValidationPipeline =
                pipelineFactory.get().createPipeline(Collections.singletonList(offerUpdate));
        offerValidationPipeline.validateAndFixOffers();
        OfferValidationResult result = offerValidationPipeline.getResult();
        List<String> actualOfferResultJsons =
                result.results()
                        .stream()
                        .map(OfferValidationPipelineTest::writeValueAsString)
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(actualOfferResultJsons, Matchers.allOf(
                Matchers.hasSize(1),
                MbiMatchers.transformedBy(list -> list.get(0), MbiMatchers.jsonEquals(expectedOfferResultJson))
        ));
        return result;
    }

    @Nonnull
    private static OfferValidationResult assertIsValidUpdate(
            String offerUpdateJson,
            Supplier<OfferValidationPipelineFactory> pipelineFactory
    ) throws IOException {
        OfferUpdateDTO offerUpdate = OBJECT_MAPPER.readValue(offerUpdateJson, OfferUpdateDTO.class);
        OfferValidationPipeline offerValidationPipeline =
                pipelineFactory.get().createPipeline(Collections.singletonList(offerUpdate));
        offerValidationPipeline.validateAndFixOffers();
        OfferValidationResult result = offerValidationPipeline.getResult();
        List<String> actualOfferResultJsons =
                result.results()
                        .stream()
                        .map(OfferValidationPipelineTest::writeValueAsString)
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(actualOfferResultJsons, MbiMatchers.isEmptyCollection());
        MatcherAssert.assertThat(result.hasErrors(), Matchers.is(false));
        return result;
    }

    private static String writeValueAsString(OfferResultDTO offerResult) {
        try {
            return OBJECT_MAPPER.writeValueAsString(offerResult);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Nonnull
    private static LocalValidatorFactoryBean createValidator() {
        LocalValidatorFactoryBean result = new LocalValidatorFactoryBean();
        result.afterPropertiesSet();
        return result;
    }

    private static OfferValidationPipelineFactory supplierPipelineFactory(
            Instant updateTime,
            Supplier<ShopVat> shopVatFactory
    ) {
        return new SupplierOfferValidationBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setSupplierFeedId(SUPPLIER_FEED_ID)
                .setShopVat(shopVatFactory.get())
                .setUseMarketOrShopSkuIgnoreOther(false)
                .setBeanValidator(SPRING_VALIDATOR)
                .setUpdateTime(updateTime)
                .build();
    }

    private static OfferValidationPipelineFactory shopPipelineFactory(Instant now) {
        return new ShopOfferValidationBuilder()
                .setBeanValidator(SPRING_VALIDATOR)
                .setPartnerId(PartnerId.datasourceId(SHOP_ID))
                .addFeedIds(TWO_SHOP_FEED_IDS)
                .setUpdateTime(now)
                .build();
    }

    @Nonnull
    private static ShopVat createUsnShopVat() {
        ShopVat shopVat = new ShopVat();
        shopVat.setDatasourceId(SUPPLIER_ID);
        shopVat.setVatRate(VatRate.NO_VAT);
        shopVat.setTaxSystem(TaxSystem.USN);
        return shopVat;
    }

    @Nonnull
    private static ShopVat createOsnShopVat() {
        ShopVat shopVat = new ShopVat();
        shopVat.setDatasourceId(SUPPLIER_ID);
        shopVat.setVatRate(VatRate.VAT_20);
        shopVat.setTaxSystem(TaxSystem.OSN);
        return shopVat;
    }

    @Test
    void testFractionalPrice() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123.45"
                + "        }"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"WARNING\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"FRACTIONAL_PRICE\","
                + "            \"level\": \"WARNING\","
                + "            \"description\": \"Price is fractional, rounded value will be used\""
                + "        }"
                + "    ]"
                + "}";

        OfferValidationResult result = assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
        Assertions.assertFalse(result.hasErrors());
        MatcherAssert.assertThat(result.updates(), Matchers.contains(MbiMatchers.<OfferUpdate>newAllOfBuilder()
                .add(OfferUpdate::offerKey, MbiMatchers.<KnownShopIndexerOfferKey>newAllOfBuilder()
                        .add(KnownShopIndexerOfferKey::partnerId, MbiMatchers.<PartnerId>newAllOfBuilder()
                                .add(PartnerId::toLong, SUPPLIER_ID)
                                .add(PartnerId::type, CampaignType.SUPPLIER)
                                .build())
                        .add(KnownShopIndexerOfferKey::withoutPartnerKnowledge,
                                MbiMatchers.<IndexerOfferKey>newAllOfBuilder()
                                .add(IndexerOfferKey::feedId, SUPPLIER_FEED_ID)
                                .add(IndexerOfferKey::shopSku, "ABV.124")
                                .build())
                        .build())
                .add(OfferUpdate::requestedPriceUpdate,
                        MbiMatchers.isPresent(MbiMatchers.<PapiOfferPriceValue>newAllOfBuilder()
                        .add(PapiOfferPriceValue::currency, Currency.RUR)
                        .add(PapiOfferPriceValue::vat, MbiMatchers.isPresent(VatRate.VAT_20))
                        .add(PapiOfferPriceValue::discountBase, Optional.empty())
                        .add(PapiOfferPriceValue::value, BigDecimal.valueOf(123))
                        .build()))
                .add(OfferUpdate::requestedAutoOldPriceUpdate, Optional.empty())
                .add(OfferUpdate::requestedIsHiddenUpdate, Optional.empty())
                .build()));
    }

    @MethodSource("testSupplierDiscountsArguments")
    @ParameterizedTest(name = "testSupplierDiscounts({1}, {2})")
    void testSupplierDiscounts(boolean shouldBeValid, Changed<BigDecimal> price) throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": " + price.value()
                + "        },"
                + "        \"oldprice\": {"
                + "            \"value\": " + price.previousValue()
                + "        }"
                + "    }"
                + "}";
        OfferValidationResult result;
        if (shouldBeValid) {
            result = assertIsValidUpdate(
                    offerUpdateJson,
                    () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
            );
        } else {
            //language=JSON
            String expectedOfferResultJson = ""
                    + "{"
                    + "    \"shopSku\":\"ABV.124\","
                    + "    \"result\": \"WARNING\","
                    + "    \"messages\": ["
                    + "        {"
                    + "            \"code\": \"WRONG_DISCOUNT\","
                    + "            \"level\": \"WARNING\","
                    + "            \"description\": \"Discount value is wrong\""
                    + "        }"
                    + "    ]"
                    + "}";

            result = assertUpdateValidationResult(
                    expectedOfferResultJson,
                    offerUpdateJson,
                    () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
            );
        }
        Assertions.assertFalse(result.hasErrors());
        MatcherAssert.assertThat(result.updates(), Matchers.contains(MbiMatchers.<OfferUpdate>newAllOfBuilder()
                .add(OfferUpdate::offerKey, MbiMatchers.<KnownShopIndexerOfferKey>newAllOfBuilder()
                        .add(KnownShopIndexerOfferKey::partnerId, MbiMatchers.<PartnerId>newAllOfBuilder()
                                .add(PartnerId::toLong, SUPPLIER_ID)
                                .add(PartnerId::type, CampaignType.SUPPLIER)
                                .build())
                        .add(KnownShopIndexerOfferKey::withoutPartnerKnowledge,
                                MbiMatchers.<IndexerOfferKey>newAllOfBuilder()
                                .add(IndexerOfferKey::feedId, SUPPLIER_FEED_ID)
                                .add(IndexerOfferKey::shopSku, "ABV.124")
                                .build())
                        .build())
                .add(OfferUpdate::requestedPriceUpdate,
                        MbiMatchers.isPresent(MbiMatchers.<PapiOfferPriceValue>newAllOfBuilder()
                        .add(PapiOfferPriceValue::currency, Currency.RUR)
                        .add(PapiOfferPriceValue::vat, MbiMatchers.isPresent(VatRate.VAT_20))
                        .add(PapiOfferPriceValue::discountBase,
                                shouldBeValid ? MbiMatchers.isPresent(price.previousValue()) :
                                        Matchers.is(Optional.empty()))
                        .add(PapiOfferPriceValue::value, price.value())
                        .build()))
                .add(OfferUpdate::requestedAutoOldPriceUpdate, Optional.empty())
                .add(OfferUpdate::requestedIsHiddenUpdate, Optional.empty())
                .build()));
    }

    @MethodSource("testShopDiscountsArguments")
    @ParameterizedTest(name = "testShopDiscounts({1}, {2})")
    void testShopDiscounts(boolean shouldBeValid, Changed<BigDecimal> price) throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"feedId\": 956394,"
                + "    \"id\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": " + price.value()
                + "        },"
                + "        \"oldprice\": {"
                + "            \"value\": " + price.previousValue()
                + "        }"
                + "    }"
                + "}";
        OfferValidationResult result;
        if (shouldBeValid) {
            result = assertIsValidUpdate(
                    offerUpdateJson,
                    () -> shopPipelineFactory(Instant.now())
            );
        } else {
            //language=JSON
            String expectedOfferResultJson = ""
                    + "{"
                    + "    \"feedId\": 956394,"
                    + "    \"id\":\"ABV.124\","
                    + "    \"result\": \"WARNING\","
                    + "    \"messages\": ["
                    + "        {"
                    + "            \"code\": \"WRONG_DISCOUNT\","
                    + "            \"level\": \"WARNING\","
                    + "            \"description\": \"Discount value is wrong\""
                    + "        }"
                    + "    ]"
                    + "}";

            result = assertUpdateValidationResult(
                    expectedOfferResultJson,
                    offerUpdateJson,
                    () -> shopPipelineFactory(Instant.now())
            );
        }
        Assertions.assertFalse(result.hasErrors());
        MatcherAssert.assertThat(result.updates(), Matchers.contains(MbiMatchers.<OfferUpdate>newAllOfBuilder()
                .add(OfferUpdate::offerKey, MbiMatchers.<KnownShopIndexerOfferKey>newAllOfBuilder()
                        .add(KnownShopIndexerOfferKey::partnerId, MbiMatchers.<PartnerId>newAllOfBuilder()
                                .add(PartnerId::toLong, SHOP_ID)
                                .add(PartnerId::type, CampaignType.SHOP)
                                .build())
                        .add(KnownShopIndexerOfferKey::withoutPartnerKnowledge,
                                MbiMatchers.<IndexerOfferKey>newAllOfBuilder()
                                .add(IndexerOfferKey::feedId, 956394L)
                                .add(IndexerOfferKey::shopSku, "ABV.124")
                                .build())
                        .build())
                .add(OfferUpdate::requestedPriceUpdate,
                        MbiMatchers.isPresent(MbiMatchers.<PapiOfferPriceValue>newAllOfBuilder()
                        .add(PapiOfferPriceValue::currency, Currency.RUR, "currency")
                        .add(PapiOfferPriceValue::vat, Optional.empty(), "vat")
                        .add(PapiOfferPriceValue::discountBase,
                                shouldBeValid ? MbiMatchers.isPresent(price.previousValue()) :
                                        Matchers.is(Optional.empty()), "discountBase")
                        .add(PapiOfferPriceValue::value, price.value(), "priceValue")
                        .build()))
                .add(OfferUpdate::requestedAutoOldPriceUpdate, Optional.empty(), "requestedAutoOldPriceUpdate")
                .add(OfferUpdate::requestedIsHiddenUpdate, Optional.empty(), "requestedIsHiddenUpdate")
                .build()));
    }

    @MethodSource("testFractionalDiscountsArguments")
    @ParameterizedTest(name = "testFractionalDiscounts({1})")
    void testFractionalDiscounts(Changed<BigDecimal> price) throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": " + price.value()
                + "        },"
                + "        \"oldprice\": {"
                + "            \"value\": " + price.previousValue()
                + "        }"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"WARNING\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\":\"FRACTIONAL_OLD_PRICE\","
                + "            \"level\":\"WARNING\","
                + "            \"description\":\"Old price is fractional, rounded value will be used\""
                + "        },"
                + "        {"
                + "            \"code\": \"WRONG_DISCOUNT\","
                + "            \"level\": \"WARNING\","
                + "            \"description\": \"Discount value is wrong\""
                + "        }"
                + "    ]"
                + "}";

        OfferValidationResult result = assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
        Assertions.assertFalse(result.hasErrors());
        MatcherAssert.assertThat(result.updates(), Matchers.contains(MbiMatchers.<OfferUpdate>newAllOfBuilder()
                .add(OfferUpdate::offerKey, MbiMatchers.<KnownShopIndexerOfferKey>newAllOfBuilder()
                        .add(KnownShopIndexerOfferKey::partnerId, MbiMatchers.<PartnerId>newAllOfBuilder()
                                .add(PartnerId::toLong, SUPPLIER_ID)
                                .add(PartnerId::type, CampaignType.SUPPLIER)
                                .build())
                        .add(KnownShopIndexerOfferKey::withoutPartnerKnowledge,
                                MbiMatchers.<IndexerOfferKey>newAllOfBuilder()
                                .add(IndexerOfferKey::feedId, SUPPLIER_FEED_ID)
                                .add(IndexerOfferKey::shopSku, "ABV.124")
                                .build())
                        .build())
                .add(OfferUpdate::requestedPriceUpdate,
                        MbiMatchers.isPresent(MbiMatchers.<PapiOfferPriceValue>newAllOfBuilder()
                        .add(PapiOfferPriceValue::currency, Currency.RUR)
                        .add(PapiOfferPriceValue::vat, MbiMatchers.isPresent(VatRate.VAT_20))
                        .add(PapiOfferPriceValue::discountBase, Matchers.is(Optional.empty()))
                        .add(PapiOfferPriceValue::value, price.value())
                        .build()))
                .add(OfferUpdate::requestedAutoOldPriceUpdate, Optional.empty())
                .add(OfferUpdate::requestedIsHiddenUpdate, Optional.empty())
                .build()));
    }

    @Test
    void testWrongVat() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        },"
                + "        \"vat\": \"VAT_20\""
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"OUT_OF_TAX_SYSTEM_VAT\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Tax system does not support given vat value\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createUsnShopVat)
        );
    }

    @Test
    void testUnrecognizedVat() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        },"
                + "        \"vat\": \"VAT_13\""
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"UNRECOGNIZED_VAT\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Unrecognized vat\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @Test
    void testVat18IsUnrecognized() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        },"
                + "        \"vat\": \"VAT_18\""
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"UNRECOGNIZED_VAT\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Unrecognized vat\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @Test
    void testVatShouldNotBeProvidedWhenNoPriceIsProvided() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"vat\": \"VAT_20\""
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"PRESENT_VAT\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"VAT can be present only when price value is present\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @Test
    void testOldPriceShouldNotBeProvidedWhenNoPriceIsProvided() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"oldprice\": {"
                + "            \"value\": 123"
                + "        }"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"PRESENT_OLDPRICE\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Old price value can be present only when price value is present\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @Test
    void testCurrencyIdShouldNotBeProvidedWhenNoPriceIsProvided() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\""
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"PRESENT_CURRENCY_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Present currencyId\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @Test
    void testMissingCurrencyId() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"price\": {\"value\": 123}"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"MISSING_CURRENCY_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Missing currencyId\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @Test
    void blankCurrencyIdTestXml() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"\","
                + "        \"price\": {\"value\": 123}"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"WRONG_CURRENCY_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Wrong currencyId: Only 'RUR' currencyId is currently supported\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @Test
    void nonRurCurrencyIdTestXml() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"USD\","
                + "        \"price\": {\"value\": 123}"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"WRONG_CURRENCY_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Wrong currencyId: Only 'RUR' currencyId is currently supported\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @ValueSource(strings = {"-123", "0", "922337203686"})
    @ParameterizedTest
    void testWrongPrice(String price) throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": " + price
                + "        }"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"INVALID_PRICE\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Invalid price value\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @ValueSource(strings = {"1", "123", "922337203685"})
    @ParameterizedTest
    void testIsValidPrice(String price) throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": " + price
                + "        }"
                + "    }"
                + "}";
        assertIsValidUpdate(
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @ValueSource(strings = {"true", "false"})
    @ParameterizedTest
    void testIsValidAutoOldPriceWithoutPrice(String auto) throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"oldprice\": {"
                + "            \"auto\": " + auto
                + "        }"
                + "    }"
                + "}";
        OfferValidationResult result = assertIsValidUpdate(
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
        MatcherAssert.assertThat(result.updates(), Matchers.contains(
                MbiMatchers.<OfferUpdate>newAllOfBuilder()
                        .add(OfferUpdate::offerKey, MbiMatchers.<KnownShopIndexerOfferKey>newAllOfBuilder()
                                .add(KnownShopIndexerOfferKey::partnerId, MbiMatchers.<PartnerId>newAllOfBuilder()
                                        .add(PartnerId::toLong, SUPPLIER_ID)
                                        .add(PartnerId::type, CampaignType.SUPPLIER)
                                        .build())
                                .add(KnownShopIndexerOfferKey::withoutPartnerKnowledge,
                                        MbiMatchers.<IndexerOfferKey>newAllOfBuilder()
                                        .add(IndexerOfferKey::feedId, SUPPLIER_FEED_ID)
                                        .add(IndexerOfferKey::shopSku, "ABV.124")
                                        .build())
                                .build())
                        .add(OfferUpdate::requestedPriceUpdate, Optional.empty())
                        .add(OfferUpdate::requestedAutoOldPriceUpdate,
                                MbiMatchers.isPresent(Boolean.parseBoolean(auto)))
                        .add(OfferUpdate::requestedIsHiddenUpdate, Optional.empty())
                        .build()
        ));
    }

    @ValueSource(strings = {"true", "false"})
    @ParameterizedTest
    void testIsValidAutoOldPriceWithPrice(String auto) throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 100"
                + "        },"
                + "        \"oldprice\": {"
                + "            \"auto\": " + auto
                + "        }"
                + "    }"
                + "}";
        OfferValidationResult result = assertIsValidUpdate(
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
        MatcherAssert.assertThat(result.updates(), Matchers.contains(
                MbiMatchers.<OfferUpdate>newAllOfBuilder()
                        .add(OfferUpdate::offerKey, MbiMatchers.<KnownShopIndexerOfferKey>newAllOfBuilder()
                                .add(KnownShopIndexerOfferKey::partnerId, MbiMatchers.<PartnerId>newAllOfBuilder()
                                        .add(PartnerId::toLong, SUPPLIER_ID)
                                        .add(PartnerId::type, CampaignType.SUPPLIER)
                                        .build())
                                .add(KnownShopIndexerOfferKey::withoutPartnerKnowledge,
                                        MbiMatchers.<IndexerOfferKey>newAllOfBuilder()
                                        .add(IndexerOfferKey::feedId, SUPPLIER_FEED_ID)
                                        .add(IndexerOfferKey::shopSku, "ABV.124")
                                        .build())
                                .build())
                        .add(OfferUpdate::requestedPriceUpdate,
                                MbiMatchers.isPresent(MbiMatchers.<PapiOfferPriceValue>newAllOfBuilder()
                                .add(PapiOfferPriceValue::currency, Currency.RUR)
                                .add(PapiOfferPriceValue::vat, MbiMatchers.isPresent(VatRate.VAT_20))
                                .add(PapiOfferPriceValue::discountBase, Matchers.is(Optional.empty()))
                                .add(PapiOfferPriceValue::value, BigDecimal.valueOf(100))
                                .build()))
                        .add(OfferUpdate::requestedAutoOldPriceUpdate,
                                MbiMatchers.isPresent(Boolean.parseBoolean(auto)))
                        .add(OfferUpdate::requestedIsHiddenUpdate, Optional.empty())
                        .build()
        ));
    }

    @Test
    void testAutoTrueIsInvalidWhenOldPriceIsPresent() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 100"
                + "        },"
                + "        \"oldprice\": {"
                + "            \"value\": 200,"
                + "            \"auto\": true"
                + "        }"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"BOTH_VALUE_AND_AUTO_SET_FOR_OLDPRICE\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Both value and auto is set for oldprice\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @Test
    void testIsValidAutoSetToFalseWithOldPrice() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 100"
                + "        },"
                + "        \"oldprice\": {"
                + "            \"value\": 200,"
                + "            \"auto\": false"
                + "        }"
                + "    }"
                + "}";
        OfferValidationResult result = assertIsValidUpdate(
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
        MatcherAssert.assertThat(result.updates(), Matchers.contains(
                MbiMatchers.<OfferUpdate>newAllOfBuilder()
                        .add(OfferUpdate::offerKey, MbiMatchers.<KnownShopIndexerOfferKey>newAllOfBuilder()
                                .add(KnownShopIndexerOfferKey::partnerId, MbiMatchers.<PartnerId>newAllOfBuilder()
                                        .add(PartnerId::toLong, SUPPLIER_ID)
                                        .add(PartnerId::type, CampaignType.SUPPLIER)
                                        .build())
                                .add(KnownShopIndexerOfferKey::withoutPartnerKnowledge,
                                        MbiMatchers.<IndexerOfferKey>newAllOfBuilder()
                                        .add(IndexerOfferKey::feedId, SUPPLIER_FEED_ID)
                                        .add(IndexerOfferKey::shopSku, "ABV.124")
                                        .build())
                                .build())
                        .add(OfferUpdate::requestedPriceUpdate,
                                MbiMatchers.isPresent(MbiMatchers.<PapiOfferPriceValue>newAllOfBuilder()
                                .add(PapiOfferPriceValue::currency, Currency.RUR)
                                .add(PapiOfferPriceValue::vat, MbiMatchers.isPresent(VatRate.VAT_20))
                                .add(PapiOfferPriceValue::discountBase, MbiMatchers.isPresent(BigDecimal.valueOf(200)))
                                .add(PapiOfferPriceValue::value, BigDecimal.valueOf(100))
                                .build()))
                        .add(OfferUpdate::requestedAutoOldPriceUpdate, MbiMatchers.isPresent(false))
                        .add(OfferUpdate::requestedIsHiddenUpdate, Optional.empty())
                        .build()
        ));
    }

    @Test
    void testWarehouseIdIsPresentInBasicPrice() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"warehouseId\": 145,"
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        },"
                + "        \"vat\": \"VAT_20\""
                + "     },"
                + "             \"pricingOverrides\": ["
                + "                {"
                + "                         \"currencyId\":\"RUR\","
                + "                         \"warehouseId\": 147,"
                + "                         \"vat\":\"VAT_20\","
                + "                         \"price\": { \"value\": 125 }"
                + "                 }"
                + "              ]"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"WAREHOUSE_ID_PRESENT\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Warehouse id should not be present for basic price\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @Test
    void testWarehouseIdIsNotPresentInOverriddenPrice() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        },"
                + "        \"vat\": \"VAT_20\""
                + "     },"
                + "             \"pricingOverrides\": ["
                + "                {"
                + "                         \"currencyId\":\"RUR\","
                + "                         \"warehouseId\": 147,"
                + "                         \"vat\":\"VAT_20\","
                + "                         \"price\": { \"value\": 125 }"
                + "                 },"
                + "                {"
                + "                         \"currencyId\":\"RUR\","
                + "                         \"vat\":\"VAT_20\","
                + "                         \"price\": { \"value\": 124 }"
                + "                 }"
                + "              ]"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"MISSING_WAREHOUSE_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Missing warehouse id in pricing-overrides\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }

    @Test
    void testDuplicatedWarehouseIdInOverriddenPrice() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricingOverrides\": ["
                + "         {"
                + "             \"currencyId\":\"RUR\","
                + "             \"warehouseId\": 147,"
                + "             \"vat\":\"VAT_20\","
                + "             \"price\": { \"value\": 125 }"
                + "          },"
                + "          {"
                + "             \"currencyId\":\"RUR\","
                + "             \"warehouseId\": 147,"
                + "             \"vat\":\"VAT_20\","
                + "             \"price\": { \"value\": 124 }"
                + "          }"
                + "   ]"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"DUPLICATE_WAREHOUSE_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Duplicated warehouses in pricing-overrides\""
                + "        }"
                + "    ]"
                + "}";

        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> supplierPipelineFactory(Instant.now(), OfferValidationPipelineTest::createOsnShopVat)
        );
    }
}
