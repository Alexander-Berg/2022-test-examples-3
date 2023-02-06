package ru.yandex.market.checkout.util.loyalty;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.GenericMockHelper;
import ru.yandex.market.checkout.util.loyalty.response.DiscountResponseBuilder;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.loyalty.api.model.CashbackOptionsResponse;
import ru.yandex.market.loyalty.api.model.cashback.details.CashbackMergeOption;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackResponses;
import ru.yandex.market.loyalty.api.model.certificate.CreatedCertificate;
import ru.yandex.market.loyalty.api.model.coin.BindOrderCoinResult;
import ru.yandex.market.loyalty.api.model.coin.BindOrdersCoinResponse;
import ru.yandex.market.loyalty.api.model.perk.BuyPerkResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkStat;
import ru.yandex.market.loyalty.api.model.perk.PerkStatResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResult;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.PRIME;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_PLUS;
import static ru.yandex.market.loyalty.api.utils.PumpkinUtils.PUMPKIN_HEADER_NAME;

/**
 * @author Nikolai Iusiumbeli
 * date: 20/07/2017
 */
@Component
public class LoyaltyConfigurer {

    public static final String URI_PROMOCODE_ACTIVATE_V1 = "/promocodes/v1/activate";
    public static final String URI_CALC_V3 = "/discount/calc/v3";
    public static final String URI_SPEND_V3 = "/discount/spend/v3";
    public static final String URI_REVERT = "/discount/revert";
    public static final String URI_PERK_BUY = "/perk/buy";
    public static final String URI_CREATE_CERTIFICATE = "/certificates/createForMsku";
    public static final String URI_CANCEL_CERTIFICATE = "/certificates/cancel/";
    public static final String URI_PERK_STATUS = "/perk/status";
    public static final String URI_BIND_BY_ORDER_IDS = "/coins/bindByOrderId";
    public static final String URI_CASHBACK_OPTIONS = "/cashback/options";
    public static final String URI_CASHBACK_DETAILS = "/cashback/getStructuredCashback";
    public static final String URI_RECALC_CASHBACK = "/discount/recalcCashback";
    public static final String URI_RESPEND_CASHBACK = "/discount/respendCashback";

    @Autowired
    private WireMockServer marketLoyaltyMock;
    @Autowired
    private TestSerializationService serializationService;

    public void mockCalcsWithDynamicResponse(Parameters parameters) {
        mockWithDynamicCalcResponse(URI_CALC_V3, "loyalty-calc-v3-transformer", parameters.getLoyaltyParameters());
        mockWithDynamicCalcResponse(URI_SPEND_V3, "loyalty-spend-v3-transformer", parameters.getLoyaltyParameters());
        mockWithDynamicCalcResponse(
                URI_RECALC_CASHBACK,
                "loyalty-recalc-cashback-transformer",
                parameters.getLoyaltyParameters()
        );
        mockWithDynamicCalcResponse(
                URI_RESPEND_CASHBACK,
                "loyalty-respend-cashback-transformer",
                parameters.getLoyaltyParameters()
        );
    }

    public void mockCashbackDetailsEmptyResponse() {
        mockCashbackDetails(new StructuredCashbackResponses(Collections.emptyList(), null));
    }

    public void mockCashbackDetails(StructuredCashbackResponses response) {
        mockResponse(
                response,
                post(urlPathEqualTo(URI_CASHBACK_DETAILS))
                        .withQueryParam("mergeOption", equalTo(CashbackMergeOption.UNKNOWN.getCode())),
                HttpStatus.OK.value()
        );
    }

    public <T> void mockSuccessRevert() {
        mockResponse(null, post(urlPathEqualTo(URI_REVERT)), HttpStatus.OK.value());
    }

    public void mockResponse(DiscountResponseBuilder discountResponseBuilder, int responseHttpStatus) {
        mockResponse(discountResponseBuilder.buildResponseWithBundles(), post(urlPathEqualTo(URI_CALC_V3)),
                responseHttpStatus);
        mockResponse(discountResponseBuilder.buildResponseWithBundles(), post(urlPathEqualTo(URI_SPEND_V3)),
                responseHttpStatus);
    }

    public <T> void mockCalcError(T discountResponse, int responseHttpStatus) {
        mockResponse(discountResponse, post(urlPathEqualTo(URI_CALC_V3)), responseHttpStatus);
    }

    public <T> void mockSpendError(T discountResponse, int responseHttpStatus) {
        mockResponse(discountResponse, post(urlPathEqualTo(URI_SPEND_V3)), responseHttpStatus);
    }

    public <T> void mockCashbackOptionsError(T discountResponse, int responseHttpStatus) {
        mockResponse(discountResponse, post(urlPathEqualTo(URI_CASHBACK_OPTIONS)), responseHttpStatus);
    }

    public void mockCalcRequestTimeout() {
        ResponseDefinitionBuilder responseDefinitionBuilder = new ResponseDefinitionBuilder()
                .withStatus(200).withFixedDelay(30000);
        marketLoyaltyMock.stubFor(post(urlPathEqualTo(URI_CALC_V3)).willReturn(responseDefinitionBuilder));
    }

    public void mockSpendRequestTimeout() {
        ResponseDefinitionBuilder responseDefinitionBuilder = new ResponseDefinitionBuilder()
                .withStatus(200).withFixedDelay(30000);
        marketLoyaltyMock.stubFor(post(urlPathEqualTo(URI_SPEND_V3)).willReturn(responseDefinitionBuilder));
    }

    public void mockCalcPumpkinResponse() {
        ResponseDefinitionBuilder responseDefinitionBuilder = new ResponseDefinitionBuilder()
                .withStatus(200).withHeader(PUMPKIN_HEADER_NAME, "true");
        marketLoyaltyMock.stubFor(post(urlPathEqualTo(URI_CALC_V3)).willReturn(responseDefinitionBuilder));
    }

    public void mockSpendPumpkinResponse() {
        ResponseDefinitionBuilder responseDefinitionBuilder = new ResponseDefinitionBuilder()
                .withStatus(200).withHeader(PUMPKIN_HEADER_NAME, "true");
        marketLoyaltyMock.stubFor(post(urlPathEqualTo(URI_SPEND_V3)).willReturn(responseDefinitionBuilder));
    }

    public void mockPromocodeActivationResponse(@Nonnull List<PromocodeActivationResult> activationResults) {
        mockResponse(new PromocodeActivationResponse(activationResults),
                post(urlPathEqualTo(URI_PROMOCODE_ACTIVATE_V1)), 200);
    }

    /**
     * мочит с использованием построителя ответов
     */
    private void mockWithDynamicCalcResponse(String methodUri, String responseTransformer,
                                             LoyaltyParameters loyaltyParameters) {
        marketLoyaltyMock.stubFor(
                createMappingBuilder(methodUri, loyaltyParameters)
                        .willReturn(
                                aResponse()
                                        .withTransformers(responseTransformer)
                                        .withTransformerParameter("loyaltyParameters", loyaltyParameters)
                                        .withStatus(HttpStatus.OK.value())
                        )
        );
    }

    private MappingBuilder createMappingBuilder(String methodUri, LoyaltyParameters loyaltyParameters) {
        return post(urlPathEqualTo(methodUri));
    }

    public void mockPerkBuy(BuyPerkResponse response) {
        mockPerkBuy(response, HttpStatus.OK.value());
    }

    public void mockPerkBuy(BuyPerkResponse response, int status) {
        MappingBuilder builder = post(urlPathEqualTo(URI_PERK_BUY));
        mockResponse(response, builder, status);
    }

    public void mockCoinBinding(Collection<Long> orderIds, long toUid, int status) {
        MappingBuilder builder = put(urlPathEqualTo(URI_BIND_BY_ORDER_IDS))
                .withQueryParam("uid", equalTo(Long.toString(toUid)));
        Random random = new Random();
        List<BindOrdersCoinResponse> responses = orderIds.stream()
                .map(orderId ->
                        new BindOrdersCoinResponse(
                                orderId,
                                Arrays.asList(
                                        new BindOrderCoinResult(random.nextLong(), true),
                                        new BindOrderCoinResult(random.nextLong(), false)
                                )
                        )
                )
                .collect(Collectors.toList());
        mockResponse(responses, builder, status);
    }

    public void mockPerkStatus(Parameters params) {
        LoyaltyParameters loyaltyParameters = params.getLoyaltyParameters();
        PerkStatResponse response = new PerkStatResponse();

        var mappingBuilder = get(urlPathEqualTo(URI_PERK_STATUS));

        if (loyaltyParameters.getPerkFailure() != null) {
            mappingBuilder.willReturn(new ResponseDefinitionBuilder().withFault(loyaltyParameters.getPerkFailure()));
        } else {
            List<PerkStat> perkStats = Lists.newArrayList(
                    PerkStat.builder()
                            .setType(PRIME)
                            .setPurchased(loyaltyParameters.getPrimeFreeDelivery() != null)
                            .setFreeDelivery(Boolean.TRUE.equals(loyaltyParameters.getPrimeFreeDelivery()))
                            .build(),
                    PerkStat.builder()
                            .setType(YANDEX_PLUS)
                            .setPurchased(loyaltyParameters.getYandexPlusFreeDelivery() != null)
                            .setFreeDelivery(Boolean.TRUE.equals(loyaltyParameters.getYandexPlusFreeDelivery()))
                            .build());
            response.setStatuses(perkStats);

            mappingBuilder.willReturn(new ResponseDefinitionBuilder()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .withBody(serializationService.serializeLoyaltyObject(response)));
        }
        marketLoyaltyMock.stubFor(mappingBuilder);
    }

    private <T> void mockResponse(T responseBody, MappingBuilder builder, int responseHttpStatus) {
        ResponseDefinitionBuilder responseDefinitionBuilder = new ResponseDefinitionBuilder()
                .withStatus(responseHttpStatus)
                .withHeader("Content-type", MediaType.APPLICATION_JSON_UTF8_VALUE);

        if (responseBody != null) {
            responseDefinitionBuilder.withBody(serializationService.serializeLoyaltyObject(responseBody));
        }

        marketLoyaltyMock.stubFor(builder.willReturn(responseDefinitionBuilder));
    }

    public List<LoggedRequest> findAll(RequestPatternBuilder requestPatternBuilder) {
        return marketLoyaltyMock.findAll(requestPatternBuilder);
    }

    public void resetAll() {
        marketLoyaltyMock.resetAll();
    }

    public List<ServeEvent> servedEvents() {
        return GenericMockHelper.servedEvents(marketLoyaltyMock);
    }

    public void verify(RequestPatternBuilder pattern) {
        marketLoyaltyMock.verify(pattern);
    }

    public void verifyZeroInteractions(RequestPatternBuilder pattern) {
        marketLoyaltyMock.verify(0, pattern);
    }

    public void mockMskuCertificate(String msku, HttpStatus status) {
        MappingBuilder builder = post(urlPathEqualTo(URI_CREATE_CERTIFICATE));
        CreatedCertificate createdCertificate = new CreatedCertificate(
                UUID.randomUUID().toString()
        );
        mockResponse(createdCertificate, builder, status.value());
    }

    public void mockCancelCertificate(String token, HttpStatus status) {
        MappingBuilder builder = post(urlPathEqualTo(URI_CANCEL_CERTIFICATE + token));
        mockResponse(null, builder, status.value());
    }

    public void mockCashbackOptions(CashbackOptionsResponse cashbackOptionsResponse, HttpStatus status) {
        MappingBuilder mappingBuilder = post(urlPathEqualTo(URI_CASHBACK_OPTIONS));
        mockResponse(cashbackOptionsResponse, mappingBuilder, status.value());
    }

    public void mockCashbackOptions(Parameters parameters) {
        LoyaltyParameters loyaltyParameters = parameters.getLoyaltyParameters();
        mockCashbackOptions(loyaltyParameters.getExpectedCashbackOptionsResponse(), HttpStatus.OK);
    }
}
