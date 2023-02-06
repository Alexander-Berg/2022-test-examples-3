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
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;
import ru.yandex.market.mbi.util.MbiMatchers;

@ParametersAreNonnullByDefault
class ShopIdentificationOfferValidationPipelineTest {
    private static final ObjectMapper OBJECT_MAPPER = new ApiObjectMapperFactory().createJsonMapper();
    private static final LocalValidatorFactoryBean SPRING_VALIDATOR = createValidator();

    private static final long SHOP_ID = 774;

    private static final long SHOP_FEED_ID1 = 956394L;
    private static final long SHOP_FEED_ID2 = 956397L;
    private static final List<Long> TWO_SHOP_FEED_IDS = Arrays.asList(SHOP_FEED_ID1, SHOP_FEED_ID2);

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
                        .map(ShopIdentificationOfferValidationPipelineTest::writeValueAsString)
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
                        .map(ShopIdentificationOfferValidationPipelineTest::writeValueAsString)
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

    private static OfferValidationPipelineFactory shopPipelineFactory(Instant now, Collection<Long> feedIds) {
        return new ShopOfferValidationBuilder()
                .setBeanValidator(SPRING_VALIDATOR)
                .setPartnerId(PartnerId.datasourceId(SHOP_ID))
                .addFeedIds(feedIds)
                .setUpdateTime(now)
                .build();
    }

    @Test
    void testValidUpdate() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"feedId\": 956394,"
                + "    \"id\":\"ABV.124\","
                + "    \"pricing\": {"
                + "        \"currencyId\": \"RUR\","
                + "        \"price\": {"
                + "            \"value\": 123"
                + "        }"
                + "    }"
                + "}";
        OfferValidationResult result = assertIsValidUpdate(
                offerUpdateJson,
                () -> shopPipelineFactory(Instant.now(), TWO_SHOP_FEED_IDS)
        );
        Assertions.assertFalse(result.hasErrors());
        MatcherAssert.assertThat(result.updates(), Matchers.contains(MbiMatchers.<OfferUpdate>newAllOfBuilder()
                .add(OfferUpdate::offerKey, MbiMatchers.<KnownShopIndexerOfferKey>newAllOfBuilder()
                        .add(KnownShopIndexerOfferKey::partnerId, MbiMatchers.<PartnerId>newAllOfBuilder()
                                .add(PartnerId::toLong, SHOP_ID)
                                .add(PartnerId::type, CampaignType.SHOP)
                                .build())
                        .add(KnownShopIndexerOfferKey::withoutPartnerKnowledge, MbiMatchers.<IndexerOfferKey>newAllOfBuilder()
                                .add(IndexerOfferKey::feedId, 956394L)
                                .add(IndexerOfferKey::shopSku, "ABV.124")
                                .build())
                        .build())
                .add(OfferUpdate::requestedPriceUpdate, MbiMatchers.isPresent(MbiMatchers.<PapiOfferPriceValue>newAllOfBuilder()
                        .add(PapiOfferPriceValue::currency, Currency.RUR)
                        .add(PapiOfferPriceValue::vat, Optional.empty())
                        .add(PapiOfferPriceValue::discountBase, Optional.empty())
                        .add(PapiOfferPriceValue::value, BigDecimal.valueOf(123))
                        .build()))
                .add(OfferUpdate::requestedAutoOldPriceUpdate, Optional.empty())
                .add(OfferUpdate::requestedIsHiddenUpdate, Optional.empty())
                .build()));
    }

    @Test
    void testMissingFeedId() throws IOException {
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
                + "            \"code\": \"MISSING_FEED_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Missing feed id\""
                + "        }"
                + "    ]"
                + "}";
        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> shopPipelineFactory(Instant.now(), TWO_SHOP_FEED_IDS)
        );
    }

    @Test
    void testSingleFeedId() throws IOException {
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
                + "    \"feedId\":956394,"
                + "    \"result\": \"WARNING\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"MISSING_FEED_ID\","
                + "            \"level\": \"WARNING\","
                + "            \"description\": \"Missing feed id\""
                + "        }"
                + "    ]"
                + "}";
        OfferValidationResult result = assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> shopPipelineFactory(Instant.now(), Collections.singleton(SHOP_FEED_ID1))
        );
        Assertions.assertFalse(result.hasErrors());
        MatcherAssert.assertThat(result.updates(), Matchers.contains(MbiMatchers.<OfferUpdate>newAllOfBuilder()
                .add(OfferUpdate::offerKey, MbiMatchers.<KnownShopIndexerOfferKey>newAllOfBuilder()
                        .add(KnownShopIndexerOfferKey::partnerId, MbiMatchers.<PartnerId>newAllOfBuilder()
                                .add(PartnerId::toLong, SHOP_ID)
                                .add(PartnerId::type, CampaignType.SHOP)
                                .build())
                        .add(KnownShopIndexerOfferKey::withoutPartnerKnowledge, MbiMatchers.<IndexerOfferKey>newAllOfBuilder()
                                .add(IndexerOfferKey::feedId, SHOP_FEED_ID1)
                                .add(IndexerOfferKey::shopSku, "ABV.124")
                                .build())
                        .build())
                .add(OfferUpdate::requestedPriceUpdate, MbiMatchers.isPresent(MbiMatchers.<PapiOfferPriceValue>newAllOfBuilder()
                        .add(PapiOfferPriceValue::currency, Currency.RUR)
                        .add(PapiOfferPriceValue::vat, Optional.empty())
                        .add(PapiOfferPriceValue::discountBase, Optional.empty())
                        .add(PapiOfferPriceValue::value, BigDecimal.valueOf(123))
                        .build()))
                .add(OfferUpdate::requestedAutoOldPriceUpdate, Optional.empty())
                .add(OfferUpdate::requestedIsHiddenUpdate, Optional.empty())
                .build()));
    }

    @Test
    void testIncorrectFeedId() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"feedId\": 956398,"
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
                + "    \"feedId\": 956398,"
                + "    \"id\":\"ABV.124\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"FEED_ID_NOT_FOUND\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Feed id not found\""
                + "        }"
                + "    ]"
                + "}";
        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> shopPipelineFactory(Instant.now(), TWO_SHOP_FEED_IDS)
        );
    }

    @Test
    void testMissingId() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"feedId\": 956394,"
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
                + "    \"feedId\": 956394,"
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"MISSING_OFFER_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Missing offer id\""
                + "        }"
                + "    ]"
                + "}";
        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> shopPipelineFactory(Instant.now(), TWO_SHOP_FEED_IDS)
        );
    }

    @Test
    void testMissingFeedIdAndOfferId() throws IOException {
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
                + "            \"code\": \"MISSING_FEED_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Missing feed id\""
                + "        },"
                + "        {"
                + "            \"code\": \"MISSING_OFFER_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Missing offer id\""
                + "        }"
                + "    ]"
                + "}";
        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> shopPipelineFactory(Instant.now(), TWO_SHOP_FEED_IDS)
        );
    }

    @Test
    void testShopSkuInsteadOfOfferId() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"feedId\": 956394,"
                + "    \"shopSku\": \"SKU-1423\","
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
                + "    \"feedId\": 956394,"
                + "    \"shopSku\": \"SKU-1423\","
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"MISSING_OFFER_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Missing offer id\""
                + "        },"
                + "        {"
                + "            \"code\": \"UNSUPPORTED_SHOP_SKU\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"ShopSku should not be used\""
                + "        }"
                + "    ]"
                + "}";
        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> shopPipelineFactory(Instant.now(), TWO_SHOP_FEED_IDS)
        );
    }

    @Test
    void testMarketSkuInsteadOfOfferId() throws IOException {
        //language=JSON
        String offerUpdateJson = ""
                + "{"
                + "    \"feedId\": 956394,"
                + "    \"marketSku\": 3456702367,"
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
                + "    \"feedId\": 956394,"
                + "    \"marketSku\": 3456702367,"
                + "    \"result\": \"ERROR\","
                + "    \"messages\": ["
                + "        {"
                + "            \"code\": \"MISSING_OFFER_ID\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"Missing offer id\""
                + "        },"
                + "        {"
                + "            \"code\": \"UNSUPPORTED_MARKET_SKU\","
                + "            \"level\": \"ERROR\","
                + "            \"description\": \"MarketSku should not be used\""
                + "        }"
                + "    ]"
                + "}";
        assertUpdateValidationResult(
                expectedOfferResultJson,
                offerUpdateJson,
                () -> shopPipelineFactory(Instant.now(), TWO_SHOP_FEED_IDS)
        );
    }

    @Test
    void testDuplicateFeedOfferId() throws IOException {
        //language=JSON
        String requestJson = ""
                + "{"
                + "    \"offers\": ["
                + "        {"
                + "            \"feedId\": 956394,"
                + "            \"id\": \"offer1\","
                + "            \"pricing\": {"
                + "                \"currencyId\": \"RUR\","
                + "                \"price\": {"
                + "                        \"value\": 123"
                + "                }"
                + "            }"
                + "        },"
                + "        {"
                + "            \"feedId\": 956394,"
                + "            \"id\": \"offer1\","
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
                + "            \"feedId\": 956394,"
                + "            \"id\": \"offer1\","
                + "            \"result\": \"ERROR\","
                + "            \"messages\": ["
                + "                {"
                + "                    \"code\": \"DUPLICATE_FEED_OFFER_ID\","
                + "                    \"level\": \"ERROR\","
                + "                    \"description\": \"Offers with same feed and offer id are received\""
                + "                }"
                + "            ]"
                + "        },"
                + "        {"
                + "            \"feedId\": 956394,"
                + "            \"id\": \"offer1\","
                + "            \"result\": \"ERROR\","
                + "            \"messages\": ["
                + "                {"
                + "                    \"code\": \"DUPLICATE_FEED_OFFER_ID\","
                + "                    \"level\": \"ERROR\","
                + "                    \"description\": \"Offers with same feed and offer id are received\""
                + "                }"
                + "            ]"
                + "        }"
                + "    ]"
                + "}";
        OfferUpdateRequestDTO request = OBJECT_MAPPER.readValue(requestJson, OfferUpdateRequestDTO.class);
        OfferValidationPipeline offerValidationPipeline =
                shopPipelineFactory(Instant.now(), TWO_SHOP_FEED_IDS).createPipeline(request.getOffers());
        offerValidationPipeline.validateAndFixOffers();
        OfferUpdateResponseDTO response = new OfferUpdateResponseDTO();
        response.setOffers(offerValidationPipeline.getResult().results());
        String actualResponseJson = OBJECT_MAPPER.writeValueAsString(response);
        MatcherAssert.assertThat(actualResponseJson, MbiMatchers.jsonEquals(expectedResponseJson));
    }
}
