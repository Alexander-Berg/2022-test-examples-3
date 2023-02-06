package ru.yandex.market.antifraud.orders.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.client.config.TestConfig;
import ru.yandex.market.antifraud.orders.entity.ue.OrderUeCalculationRequest;
import ru.yandex.market.antifraud.orders.entity.ue.OrderUeCalculationResult;
import ru.yandex.market.antifraud.orders.entity.ue.UeAddressDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeBuyerDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeDeliveryDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeDeliveryType;
import ru.yandex.market.antifraud.orders.entity.ue.UeOrderDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeParcelDto;
import ru.yandex.market.antifraud.orders.entity.ue.UeParcelItemDto;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.dto.CoinDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestDtoV2;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestFiltersDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseDtoV2;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseItemDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderStatsDto;
import ru.yandex.market.antifraud.orders.web.dto.PromoVerdictDto;
import ru.yandex.market.antifraud.orders.web.dto.ReferralInfo;
import ru.yandex.market.antifraud.orders.web.dto.loyalty.BonusState;
import ru.yandex.market.antifraud.orders.web.dto.loyalty.LoyaltyBonusInfoRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.loyalty.LoyaltyBonusInfoResponseDto;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyRestrictionType;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;
import ru.yandex.market.antifraud.orders.web.entity.PromoVerdictType;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author dzvyagin
 */
@RunWith(SpringRunner.class)
@RestClientTest(MstatAntifraudOrdersLoyaltyClient.class)
@ContextConfiguration(classes = {TestConfig.class})
public class MstatAntifraudOrdersLoyaltyClientTest {

    @Autowired
    private MstatAntifraudOrdersLoyaltyClient mstatAntifraudOrdersLoyaltyClient;

    @Autowired
    private RestTemplate mstatAntifraudOltpClientRestTemplate;

    private MockRestServiceServer mockServer;

    @Before
    public void init() {
        mockServer = MockRestServiceServer.createServer(mstatAntifraudOltpClientRestTemplate);
        mockServer.reset();
    }


    @Test
    public void checkPromoFraud() throws Exception {
        LoyaltyVerdictRequestDto request = LoyaltyVerdictRequestDto.builder()
                .coins(Arrays.asList(
                        new CoinDto(12L, 11L, new ReferralInfo(null, Collections.emptyList())),
                        new CoinDto(15L, 22L)
                ))
                .orderIds(Collections.singletonList(432L))
                .uid(654L)
                .reason("USER_CHECK")
                .build();
        LoyaltyVerdictDto response = new LoyaltyVerdictDto(
                LoyaltyVerdictType.OTHER,
                Collections.singletonList(654L),
                Arrays.asList(
                        new PromoVerdictDto(12L, 11L, PromoVerdictType.OK),
                        new PromoVerdictDto(15L, 22L, PromoVerdictType.USED)
                ), false);
        // language=json
        String responseJson = "{\"verdict\":\"OTHER\",\"uids\":[654]," +
                "\"promos\":[{\"coinId\":12,\"promoId\":11,\"verdict\":\"OK\"}," +
                "{\"coinId\":15,\"promoId\":22,\"verdict\":\"USED\"}],\"firstOrder\":false}\n";
        mockServer.expect(requestTo("/antifraud/loyalty/detect"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
        LoyaltyVerdictDto result = mstatAntifraudOrdersLoyaltyClient.checkPromoFraud(request);
        assertEquals(response, result);
        mockServer.verify();
    }


    @Test
    public void checkRestrictions() throws Exception {
        LoyaltyBuyerRestrictionsRequestDto request = new LoyaltyBuyerRestrictionsRequestDto(123L, null);
        LoyaltyBuyerRestrictionsDto response =
            new LoyaltyBuyerRestrictionsDto(LoyaltyRestrictionType.PROHIBITED, OrderStatsDto.empty());
        String responseJson = AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(response);
        mockServer.expect(requestTo("/antifraud/loyalty/restrictions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
        LoyaltyBuyerRestrictionsDto result = mstatAntifraudOrdersLoyaltyClient.checkRestrictions(request);
        assertEquals(response, result);
        mockServer.verify();
    }

    @Test
    public void checkBonusRestrictions() throws Exception {
        LoyaltyBonusInfoRequestDto request = new LoyaltyBonusInfoRequestDto(123L, "yuid", "uuid", "experiments");
        LoyaltyBonusInfoResponseDto response =
                new LoyaltyBonusInfoResponseDto(123L, "yuid", "uuid", BonusState.DISABLED);
        String responseJson = AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(response);
        mockServer.expect(requestTo("/antifraud/loyalty/restrictions/bonus"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
        LoyaltyBonusInfoResponseDto result = mstatAntifraudOrdersLoyaltyClient.checkBonusRestrictions(request);
        assertEquals(response, result);
        mockServer.verify();
    }

    @Test
    public void checkRestrictionsBatch() throws Exception {
        Collection<LoyaltyBuyerRestrictionsRequestDto> request = Arrays.asList(
            new LoyaltyBuyerRestrictionsRequestDto(123L, null),
            new LoyaltyBuyerRestrictionsRequestDto(124L, null));
        List<LoyaltyBuyerRestrictionsDto> response = Arrays.asList(
            LoyaltyBuyerRestrictionsDto.prohibited(123L, OrderStatsDto.empty()),
            LoyaltyBuyerRestrictionsDto.prohibited(124L, OrderStatsDto.empty())
        );
        String responseJson = AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(response);
        mockServer.expect(requestTo("/antifraud/loyalty/restrictions/many"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
        Collection<LoyaltyBuyerRestrictionsDto> result = mstatAntifraudOrdersLoyaltyClient.checkRestrictionsBatch(request);
        Assertions.assertThat(result).containsAll(response);
        mockServer.verify();
    }

    @Test
    public void getUeForOrder() throws Exception {
        UeBuyerDto buyer = UeBuyerDto.builder()
                .uid(1L)
                .uuid("uuid")
                .yandexUid("yandexuid")
                .email("mail@yandex.ru")
                .normalizedPhone("88005553535")
                .build();
        UeDeliveryDto delivery = UeDeliveryDto.builder()
                .buyerAddress(new UeAddressDto("Russia", "123456", "Omsk", 33L))
                .shopAddress(new UeAddressDto("Russia", "234566", "Novosibirsk", 34L))
                .deliveryPrice(BigDecimal.TEN)
                .deliveryServiceId(101L)
                .deliveryServiceName("Boxberry")
                .deliveryType(UeDeliveryType.PICKUP)
                .outletId(1234L)
                .parcels(Arrays.asList(UeParcelDto.builder()
                        .id(1L)
                        .depth(100L)
                        .height(100L)
                        .weight(100L)
                        .parcelItems(Arrays.asList(
                                UeParcelItemDto.builder()
                                        .msku(12345L)
                                        .count(2)
                                        .depth(90L)
                                        .weight(90L)
                                        .height(90L)
                                        .itemId(123L)
                                        .price(BigDecimal.TEN)
                                        .supplierId(112L)
                                        .width(90L)
                                        .build()
                        ))
                        .build()))
                .build();
        UeOrderDto order = UeOrderDto.builder()
            .id(1L)
            .creationDate(Instant.now())
            .buyer(buyer)
            .delivery(delivery)
            .build();
        OrderUeCalculationRequest request = new OrderUeCalculationRequest(Arrays.asList(order), null);
        String responseJson = "{\"unitEconomic\": 0}";
        mockServer.expect(requestTo("/ue/order"))
                .andExpect(header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON_UTF8));
        OrderUeCalculationResult result = mstatAntifraudOrdersLoyaltyClient.getUeForOrder(request);
        Assertions.assertThat(result.getUnitEconomic()).isEqualTo(BigDecimal.ZERO);
        mockServer.verify();
    }

    @Test
    public void getOrderCount() throws Exception {
        Instant from = Instant.ofEpochSecond(1605600000L);
        Instant to = Instant.ofEpochSecond(1605604000L);
        String requestJson = "{\"puid\":123,\"promoFilter\":\"promo\",\"from\":\"2020-11-17T08:00:00Z\",\"to\":\"2020-11-17T09:06:40Z\"}\n";
        String resultJson = "{\"puid\":123,\"promoFilter\":\"promo\",\"from\":\"2020-11-17T08:00:00Z\",\"to\":\"2020-11-17T09:06:40Z\"," +
                "\"userOrderCount\":{\"active\":1,\"delivered\":2,\"cancelled\":3,\"total\":6}," +
                "\"glueOrderCount\":{\"active\":2,\"delivered\":3,\"cancelled\":4,\"total\":9}}\n";
        OrderCountRequestDto request = OrderCountRequestDto.builder()
                .puid(123L)
                .promoFilter("promo")
                .from(from)
                .to(to)
                .build();
        OrderCountResponseDto response = OrderCountResponseDto.builder()
                .puid(123L)
                .promoFilter("promo")
                .from(from)
                .to(to)
                .userOrderCount(new OrderCountDto(1, 2, 3, 6))
                .glueOrderCount(new OrderCountDto(2, 3, 4, 9))
                .build();
        mockServer.expect(requestTo("/antifraud/loyalty/orders-count"))
                .andExpect(header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(requestJson))
                .andRespond(withSuccess(resultJson, MediaType.APPLICATION_JSON_UTF8));
        OrderCountResponseDto result = mstatAntifraudOrdersLoyaltyClient.getOrdersCount(request);
        Assertions.assertThat(result).isEqualTo(response);
        mockServer.verify();
    }

    @Test
    public void getOrderCountV2() throws Exception {
        Instant from = Instant.ofEpochSecond(1605600000L);
        Instant to = Instant.ofEpochSecond(1605604000L);
        String requestJson = "{\"puid\":123,\"requestItems\":{\"promo\":{\"promoFilters\":\"promo\",\"from\":\"2020-11-17T08:00:00Z\",\"to\":\"2020-11-17T09:06:40Z\"}," +
                "\"mobile\":{\"platformType\":\"MOBILE\",\"from\":\"2020-11-17T08:00:00Z\",\"to\":\"2020-11-17T09:06:40Z\"}}}\n";
        String resultJson = "{\"puid\":123,\"glueSize\":2,\"incomplete\":false,\"responseItems\":{" +
                "\"promo\":{\"from\":\"2020-11-17T08:00:00Z\",\"to\":\"2020-11-17T09:06:40Z\"," +
                "\"userOrderCount\":{\"active\":1,\"delivered\":2,\"cancelled\":3,\"total\":6}," +
                "\"glueOrderCount\":{\"active\":2,\"delivered\":3,\"cancelled\":4,\"total\":9}}," +
                "\"mobile\":{\"from\":\"2020-11-17T08:00:00Z\",\"to\":\"2020-11-17T09:06:40Z\"," +
                "\"userOrderCount\":{\"active\":1,\"delivered\":2,\"cancelled\":3,\"total\":6}," +
                "\"glueOrderCount\":{\"active\":2,\"delivered\":3,\"cancelled\":4,\"total\":9}}}}\n";
        OrderCountRequestDtoV2 request = OrderCountRequestDtoV2.builder()
                .puid(123L)
                .requestItems(ImmutableMap.of(
                        "promo", OrderCountRequestFiltersDto.builder()
                                .promoFilters("promo")
                                .from(from)
                                .to(to)
                                .build(),
                        "mobile", OrderCountRequestFiltersDto.builder()
                                .platformType(OrderCountRequestFiltersDto.PlatformType.MOBILE)
                                .from(from)
                                .to(to)
                                .build()))
                .build();
        OrderCountResponseDtoV2 response = OrderCountResponseDtoV2.builder()
                .puid(123L)
                .glueSize(2)
                .responseItems(ImmutableMap.of(
                        "promo", OrderCountResponseItemDto.builder()
                                .from(from)
                                .to(to)
                                .userOrderCount(new OrderCountDto(1, 2, 3, 6))
                                .glueOrderCount(new OrderCountDto(2, 3, 4, 9))
                                .build(),
                        "mobile", OrderCountResponseItemDto.builder()
                                .from(from)
                                .to(to)
                                .userOrderCount(new OrderCountDto(1, 2, 3, 6))
                                .glueOrderCount(new OrderCountDto(2, 3, 4, 9))
                                .build()))
                .build();
        mockServer.expect(requestTo("/antifraud/loyalty/orders-count/v2"))
                .andExpect(header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(requestJson))
                .andRespond(withSuccess(resultJson, MediaType.APPLICATION_JSON_UTF8));
        OrderCountResponseDtoV2 result = mstatAntifraudOrdersLoyaltyClient.getOrdersCount(request);
        Assertions.assertThat(result).isEqualTo(response);
        mockServer.verify();
    }
}
