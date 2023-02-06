package ru.yandex.market.api.partner.controllers.offers.stored;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.api.partner.controllers.offers.stored.base.OfferResultDTO;
import ru.yandex.market.api.partner.controllers.offers.stored.base.OfferUpdateDTO;
import ru.yandex.market.api.partner.controllers.offers.stored.base.OfferUpdateRequestDTO;
import ru.yandex.market.api.partner.controllers.offers.stored.base.OfferUpdateResponseDTO;
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

@ParametersAreNonnullByDefault
class SupplierIdentificationOfferValidationPipelineTest {
    private static final ObjectMapper OBJECT_MAPPER = new ApiObjectMapperFactory().createJsonMapper();
    private static final LocalValidatorFactoryBean SPRING_VALIDATOR = createValidator();

    private static final long SUPPLIER_ID = 93423;
    private static final long SUPPLIER_FEED_ID = 234720;

    private static void assertUpdateValidationResult(
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
                        .map(SupplierIdentificationOfferValidationPipelineTest::writeValueAsString)
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(actualOfferResultJsons, Matchers.allOf(
                Matchers.hasSize(1),
                MbiMatchers.transformedBy(list -> list.get(0), MbiMatchers.jsonEquals(expectedOfferResultJson))
        ));
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
                        .map(SupplierIdentificationOfferValidationPipelineTest::writeValueAsString)
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

    private static OfferValidationPipelineFactory pipelineFactory(Instant updateTime) {
        return pipelineFactory(updateTime, false);
    }

    private static OfferValidationPipelineFactory pipelineFactory(Instant updateTime, boolean usedMixedIdentification) {
        ShopVat shopVat = new ShopVat();
        shopVat.setDatasourceId(SUPPLIER_ID);
        shopVat.setVatRate(VatRate.VAT_20);
        shopVat.setTaxSystem(TaxSystem.OSN);
        return new SupplierOfferValidationBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setSupplierFeedId(SUPPLIER_FEED_ID)
                .setShopVat(shopVat)
                .setUseMarketOrShopSkuIgnoreOther(usedMixedIdentification)
                .setBeanValidator(SPRING_VALIDATOR)
                .setUpdateTime(updateTime)
                .build();
    }

    @Test
    void testValidUpdate() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        }"
                + "    }"
                + "}";
        OfferValidationResult result = assertIsValidUpdate(
                offerUpdateJson,
                () -> pipelineFactory(Instant.now())
        );
        Assertions.assertFalse(result.hasErrors());
        MatcherAssert.assertThat(result.updates(), Matchers.contains(MbiMatchers.<OfferUpdate>newAllOfBuilder()
                .add(OfferUpdate::offerKey, MbiMatchers.<KnownShopIndexerOfferKey>newAllOfBuilder()
                        .add(KnownShopIndexerOfferKey::partnerId, MbiMatchers.<PartnerId>newAllOfBuilder()
                                .add(PartnerId::toLong, SUPPLIER_ID)
                                .add(PartnerId::type, CampaignType.SUPPLIER)
                                .build())
                        .add(KnownShopIndexerOfferKey::withoutPartnerKnowledge, MbiMatchers.<IndexerOfferKey>newAllOfBuilder()
                                .add(IndexerOfferKey::feedId, SUPPLIER_FEED_ID)
                                .add(IndexerOfferKey::shopSku, "ABV.124")
                                .build())
                        .build())
                .add(OfferUpdate::requestedPriceUpdate, MbiMatchers.isPresent(MbiMatchers.<PapiOfferPriceValue>newAllOfBuilder()
                        .add(PapiOfferPriceValue::currency, Currency.RUR)
                        .add(PapiOfferPriceValue::vat, MbiMatchers.isPresent(VatRate.VAT_20))
                        .add(PapiOfferPriceValue::discountBase, Optional.empty())
                        .add(PapiOfferPriceValue::value, BigDecimal.valueOf(123))
                        .build()))
                .add(OfferUpdate::requestedAutoOldPriceUpdate, Optional.empty())
                .add(OfferUpdate::requestedIsHiddenUpdate, Optional.empty())
                .build()));
    }

    @Test
    void testIdIsUsedInsteadOfShopSku() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"id\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        }"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"id\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"MISSING_SHOP_SKU\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"ShopSku is missing\""
                + "        },"
                + "        {"
                + "            \"code\": \"UNSUPPORTED_OFFER_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"id should not be used for offer\""
                + "        }"
                + "    ]"
                + "}";
        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> pipelineFactory(Instant.now())
        );
    }

    @Test
    void testShopSkuIsMissing() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        }"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"MISSING_SHOP_SKU\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"ShopSku is missing\""
                + "        }"
                + "    ]"
                + "}";
        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> pipelineFactory(Instant.now())
        );
    }

    @Test
    void testMarketSkuIsForbidden() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"marketSku\":34593053,"
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        }"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"marketSku\":34593053,"
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"MARKET_AND_SHOP_SKU_BOTH_PRESENT\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Both marketSku and shopSku is passed\""
                + "        }"
                + "    ]"
                + "}";
        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> pipelineFactory(Instant.now())
        );
    }

    @Test
    void testShopSkuAndMarketSkuIdentification() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"marketSku\":34593053,"
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        }"
                + "    }"
                + "}";
        assertIsValidUpdate(
                offerUpdateJson,
                () -> pipelineFactory(Instant.now(), true)
        );
    }

    @Test
    void testShopSkuAndMarketSkuIdentificationWithoutShopSku() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"marketSku\":34593053,"
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        }"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"marketSku\":34593053,"
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"MISSING_SHOP_SKU\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"ShopSku is missing\""
                + "        }"
                + "    ]"
                + "}";
        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> pipelineFactory(Instant.now(), true)
        );
    }

    @Test
    void testShopSkuAndMarketSkuIdentificationWithoutMarketSku() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"shopSku\":\"SKU-123\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        }"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"shopSku\":\"SKU-123\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"MISSING_MARKET_SKU\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"MarketSku is missing\""
                + "        }"
                + "    ]"
                + "}";
        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> pipelineFactory(Instant.now(), true)
        );
    }

    @Test
    void testFeedIdIsForbidden() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"feedId\": 234720,"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        }"
                + "    }"
                + "}";
        //language=JSON
        String expectedOfferResultJson = ""
                + "{"
                + "    \"feedId\": 234720,"
                + "    \"shopSku\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"UNSUPPORTED_FEED_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"feed id should not be used for offer\""
                + "        }"
                + "    ]"
                + "}";
        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> pipelineFactory(Instant.now())
        );
    }

    @Test
    void testDuplicateShopSku() throws IOException {
        //language=JSON
        String requestJson = ""
                + "{"
                + "    \"offers\": ["
                + "        {"
                + "            \"shopSku\":\"ABV.124\","
                + "            \"pricing\": {"
                + "                \"currencyId\": \"RUR\","
                + "                \"price\": {"
                + "                        \"value\": 123"
                + "                }"
                + "            }"
                + "        },"
                + "        {"
                + "            \"shopSku\":\"ABV.124\","
                + "            \"pricing\": {"
                + "                \"currencyId\": \"RUR\","
                + "                \"price\": {"
                + "                    \"value\": 145"
                + "                }"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        //language=JSON
        String expectedResponseJson = ""
                + "{"
                + "    \"hasIgnoredErrors\": false,"
                + "    \"offers\": ["
                + "        {"
                + "            \"shopSku\":\"ABV.124\","
                + "            \"result\": \"ERROR\","
                + "            \"messages\": ["
                + "                {"
                + "                    \"code\": \"DUPLICATE_SHOP_SKU\","
                + "                    \"level\": \"ERROR\","
                + "                    \"description\": \"Offers with the same shop SKU are received\""
                + "                }"
                + "            ]"
                + "        },"
                + "        {"
                + "            \"shopSku\":\"ABV.124\","
                + "            \"result\": \"ERROR\","
                + "            \"messages\": ["
                + "                {"
                + "                    \"code\": \"DUPLICATE_SHOP_SKU\","
                + "                    \"level\": \"ERROR\","
                + "                    \"description\": \"Offers with the same shop SKU are received\""
                + "                }"
                + "            ]"
                + "        }"
                + "    ]"
                + "}";
        OfferUpdateRequestDTO request = OBJECT_MAPPER.readValue(requestJson, OfferUpdateRequestDTO.class);
        OfferValidationPipeline offerValidationPipeline =
                pipelineFactory(Instant.now()).createPipeline(request.getOffers());
        offerValidationPipeline.validateAndFixOffers();
        OfferUpdateResponseDTO response = new OfferUpdateResponseDTO();
        response.setOffers(offerValidationPipeline.getResult().results());
        String actualResponseJson = OBJECT_MAPPER.writeValueAsString(response);
        MatcherAssert.assertThat(actualResponseJson, MbiMatchers.jsonEquals(expectedResponseJson));
    }
}
