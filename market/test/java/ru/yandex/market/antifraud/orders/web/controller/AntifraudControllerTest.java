package ru.yandex.market.antifraud.orders.web.controller;


import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.antifraud.orders.service.AntifraudService;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.service.exceptions.TooManyRequestsException;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyAntifraudService;
import ru.yandex.market.antifraud.orders.service.loyalty.orderCount.OrderCountRequest;
import ru.yandex.market.antifraud.orders.service.loyalty.orderCount.OrderCountRequestConverter;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.TestUidsMockConfig;
import ru.yandex.market.antifraud.orders.test.annotations.WebLayerTest;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestDtoV2;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseDtoV2;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseItemDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderStatsDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.loyalty.BonusState;
import ru.yandex.market.antifraud.orders.web.dto.loyalty.LoyaltyBonusInfoRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.loyalty.LoyaltyBonusInfoResponseDto;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyRestrictionType;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.domain.UidType;
import ru.yandex.market.sdk.userinfo.service.ResolveUidService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil.OBJECT_MAPPER;

/**
 * @author dzvyagin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@EnableAspectJAutoProxy
@WebLayerTest({AntifraudController.class, MockingAspect.class})
public class AntifraudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AntifraudService antifraudService;

    @MockBean
    private LoyaltyAntifraudService loyaltyAntifraudService;

    @MockBean
    private OrderCountRequestConverter orderCountRequestConverter;

    @MockBean
    private ConfigurationService configurationService;

    // TODO drop after redis launch
    @MockBean
    private ResolveUidService resolveUidService;

    //language=json
    private static final String LOYALTY_DETECT_REQ = "{\"coins\":[{\"coinId\":12,\"promoId\":11,\"referral\":{\"rewardCoin\":{\"coinId\":1," +
            "\"promoId\":2,\"uid\":3},\"referralCoins\":[]}},{\"coinId\":15,\"promoId\":22}],\"uid\":654," +
            "\"orderIds\":[432],\"reason\":\"USER_CHECK\"}\n";

    @Before
    public void init() {
        when(configurationService.getMockResponseUsers()).thenReturn(new long[]{});
        when(configurationService.getTestUidMockConfig()).thenReturn(TestUidsMockConfig.disabled());
        when(configurationService.getMocksConfig()).thenCallRealMethod();
        when(resolveUidService.resolve(anyLong())).thenReturn(new Uid(1L, UidType.PUID, false));
    }

    @Test
    public void testPromo() throws Exception {
        when(loyaltyAntifraudService.checkPromoRequest(any()))
                .thenReturn(new LoyaltyVerdictDto(LoyaltyVerdictType.OTHER, Collections.emptyList(), Collections.emptyList(), false));
        //language=json
        String resultJson = "{\"verdict\":\"OTHER\",\"uids\":[],\"promos\":[],\"firstOrder\":false}";
        mockMvc.perform(
                post("/antifraud/loyalty/detect")
                        .content(LOYALTY_DETECT_REQ)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(resultJson));
    }

    @Test
    public void testRestrictions() throws Exception {
        LoyaltyBuyerRestrictionsDto okResult = new LoyaltyBuyerRestrictionsDto(LoyaltyRestrictionType.OK, OrderStatsDto.empty());
        when(loyaltyAntifraudService.checkRestrictions(any()))
                .thenReturn(okResult);
        mockMvc.perform(
                post("/antifraud/loyalty/restrictions")
                    .content(OBJECT_MAPPER.writeValueAsString(new LoyaltyBuyerRestrictionsRequestDto(123L, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(OBJECT_MAPPER.writeValueAsString(okResult)));
    }

    @Test
    public void testBonusRestrictions() throws Exception {
        LoyaltyBonusInfoResponseDto okResult =
                new LoyaltyBonusInfoResponseDto(123L, "yuid", "uuid", BonusState.DISABLED);
        when(loyaltyAntifraudService.checkBonuses(any()))
                .thenReturn(okResult);
        mockMvc.perform(
                post("/antifraud/loyalty/restrictions/bonus")
                        .content(OBJECT_MAPPER.writeValueAsString(new LoyaltyBonusInfoRequestDto(123L, "yuid", "uuid", "experiments")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(OBJECT_MAPPER.writeValueAsString(okResult)));
    }

    @Test
    public void testRestrictionsBatch() throws Exception {
        Long UID_1 = 123L;
        Long UID_2 = 124L;
        LoyaltyBuyerRestrictionsDto okResult1 = LoyaltyBuyerRestrictionsDto.ok(UID_1, OrderStatsDto.empty());
        LoyaltyBuyerRestrictionsDto okResult2 = LoyaltyBuyerRestrictionsDto.ok(UID_2, OrderStatsDto.empty());
        when(loyaltyAntifraudService.checkRestrictionsBatch(anyCollection()))
                .thenReturn(List.of(okResult1, okResult2));
        //language=json
        String resultJson = "[" +
                "{\"uid\":123,\"restriction\":\"OK\",\"orderStats\":" +
                "{\"lastDayUserOrderStat\":{\"active\":0,\"delivered\":0,\"cancelled\":0,\"total\":0}," +
                "\"lastWeekUserOrderStat\":{\"active\":0,\"delivered\":0,\"cancelled\":0,\"total\":0}," +
                "\"lastDayGlueOrderStat\":{\"active\":0,\"delivered\":0,\"cancelled\":0,\"total\":0}," +
                "\"lastWeekGlueOrderStat\":{\"active\":0,\"delivered\":0,\"cancelled\":0,\"total\":0}}}," +
                "{\"uid\":124,\"restriction\":\"OK\",\"orderStats\":" +
                "{\"lastDayUserOrderStat\":{\"active\":0,\"delivered\":0,\"cancelled\":0,\"total\":0}," +
                "\"lastWeekUserOrderStat\":{\"active\":0,\"delivered\":0,\"cancelled\":0,\"total\":0}," +
                "\"lastDayGlueOrderStat\":{\"active\":0,\"delivered\":0,\"cancelled\":0,\"total\":0}," +
                "\"lastWeekGlueOrderStat\":{\"active\":0,\"delivered\":0,\"cancelled\":0,\"total\":0}}}]\n";
        mockMvc.perform(
                post("/antifraud/loyalty/restrictions/many")
                        .content(OBJECT_MAPPER.writeValueAsString(new LoyaltyBuyerRestrictionsRequestDto[]{
                            new LoyaltyBuyerRestrictionsRequestDto(UID_1, null),
                            new LoyaltyBuyerRestrictionsRequestDto(UID_2, null)
                        }))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(resultJson));
    }

    @Test
    public void testOrderCount() throws Exception {
        Instant from = Instant.ofEpochSecond(1605600000L);
        Instant to = Instant.ofEpochSecond(1605604000L);
        ArgumentCaptor<OrderCountRequest> captor = ArgumentCaptor.forClass(OrderCountRequest.class);
        OrderCountResponseDtoV2 responseDto = OrderCountResponseDtoV2.builder()
                .puid(123L)
                .responseItems(Map.of("ordersCount", OrderCountResponseItemDto.builder()
                        .from(from)
                        .to(to)
                        .userOrderCount(new OrderCountDto(1, 2, 3, 6))
                        .glueOrderCount(new OrderCountDto(2, 3, 4, 9))
                        .build()))
                .build();
        when(loyaltyAntifraudService.countOrders(captor.capture())).thenReturn(
                CompletableFuture.completedFuture(responseDto)
        );
        String requestJson = "{\"puid\":123,\"requestItems\":{\"ordersCount\":{\"promoFilters\":\"promo\",\"from\":\"2020-11-17T08:00:00Z\",\"to\":\"2020-11-17T09:06:40Z\"}}}\n";
        String resultJson = "{\"puid\":123,\"responseItems\":{\"ordersCount\":{\"from\":\"2020-11-17T08:00:00Z\",\"to\":\"2020-11-17T09:06:40Z\"," +
                "\"userOrderCount\":{\"active\":1,\"delivered\":2,\"cancelled\":3,\"total\":6}," +
                "\"glueOrderCount\":{\"active\":2,\"delivered\":3,\"cancelled\":4,\"total\":9}}}}\n";
        mockMvc.perform(asyncDispatch(
                        mockMvc.perform(post("/antifraud/loyalty/orders-count/v2")
                                        .content(requestJson)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON))
                                .andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().json(resultJson));
    }

    @Test
    public void testPumpkinableFail() throws Exception {
        mockMvc.perform(
                post("/antifraud/loyalty/detect")
                    .content(LOYALTY_DETECT_REQ)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Market-Rearrfactors", "antifraud-chaosmonkey-500=1"))
            .andExpect(status().is(500));
    }

    @Test
    public void testMockUser() throws Exception {
        when(configurationService.getMockResponseUsers()).thenReturn(new long[]{123L});
        when(antifraudService.checkOrder(any(OrderRequestDto.class))).thenThrow(new RuntimeException());
        mockMvc.perform(
                post("/antifraud/detect")
                    .content("{\"buyer\":{\"uid\": 123}}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"check_results\":[],\"fraud\":false}"));
    }

    @Test
    public void testDisabledPaths() throws Exception {
        when(configurationService.getDisabledPaths()).thenReturn(Set.of("/antifraud/detect"));
        when(antifraudService.checkOrder(any(OrderRequestDto.class))).thenThrow(new RuntimeException());
        mockMvc.perform(
                post("/antifraud/detect")
                    .content("{\"buyer\":{\"uid\": 123}}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"check_results\":[],\"fraud\":false}"));
    }

    @Test
    public void testMockUserConfig() throws Exception {
        when(configurationService.getMockResponseUsers()).thenReturn(new long[]{});
        when(configurationService.getTestUidMockConfig()).thenReturn(TestUidsMockConfig.builder().enabled(true).endpoints(Set.of("/antifraud/detect")).build());
        when(antifraudService.checkOrder(any(OrderRequestDto.class))).thenThrow(new RuntimeException());
        mockMvc.perform(
                post("/antifraud/detect")
                    .content("{\"buyer\":{\"uid\": 2190550858753437199}}")
                    .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"check_results\":[],\"fraud\":false}"));
    }

    @Test
    public void testTooManyRequests() throws Exception {
        when(configurationService.getMockResponseUsers()).thenReturn(new long[]{});
        String requestJson = "{\"puid\":123,\"requestItems\":{\"ordersCount\":{\"promoFilters\":\"promo\",\"from\":\"2020-11-17T08:00:00Z\",\"to\":\"2020-11-17T09:06:40Z\"}}}\n";
        when(loyaltyAntifraudService.countOrders(any(OrderCountRequest.class))).thenReturn(
                CompletableFuture.supplyAsync(() -> {throw new TooManyRequestsException("test");})
        );
        when(orderCountRequestConverter.makeRequestEntity(any(OrderCountRequestDtoV2.class)))
                .thenReturn(OrderCountRequest.builder().build());
        mockMvc.perform(asyncDispatch(
                mockMvc.perform(post("/antifraud/loyalty/orders-count/v2")
                        .content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                        .andReturn()))
                .andExpect(status().is(429));
    }
}
