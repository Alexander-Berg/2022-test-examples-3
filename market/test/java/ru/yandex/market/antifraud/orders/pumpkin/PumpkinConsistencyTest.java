package ru.yandex.market.antifraud.orders.pumpkin;

import java.util.Collections;
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

import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.service.AntifraudService;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.service.GluesService;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyAntifraudService;
import ru.yandex.market.antifraud.orders.service.loyalty.orderCount.OrderCountRequest;
import ru.yandex.market.antifraud.orders.service.loyalty.orderCount.OrderCountRequestConverter;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.TestUidsMockConfig;
import ru.yandex.market.antifraud.orders.test.annotations.WebLayerTest;
import ru.yandex.market.antifraud.orders.web.controller.AntifraudController;
import ru.yandex.market.antifraud.orders.web.controller.GraphController;
import ru.yandex.market.antifraud.orders.web.controller.MockingAspect;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestDtoV2;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseDtoV2;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.domain.UidType;
import ru.yandex.market.sdk.userinfo.service.ResolveUidService;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil.OBJECT_MAPPER;

/**
 * Тест на проверку целостности ответов тыквы и антифрода
 * @author: aproskriakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebLayerTest({AntifraudController.class, GraphController.class, MockingAspect.class})
@EnableAspectJAutoProxy
public class PumpkinConsistencyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoyaltyAntifraudService loyaltyAntifraudService;

    @MockBean
    private AntifraudService antifraudService;

    @MockBean
    private OrderCountRequestConverter orderCountRequestConverter;

    @MockBean
    private ConfigurationService configurationService;

    @MockBean
    private GluesService gluesService;

    // TODO drop after redis launch
    @MockBean
    private ResolveUidService resolveUidService;

    @Before
    public void init() {
        when(configurationService.getMockResponseUsers()).thenReturn(new long[]{});
        when(configurationService.getTestUidMockConfig()).thenReturn(TestUidsMockConfig.disabled());
        when(configurationService.getMocksConfig()).thenCallRealMethod();
        when(resolveUidService.resolve(anyLong())).thenReturn(new Uid(1L, UidType.PUID, false));
    }

    @Test
    public void testDetect() throws Exception {
        OrderVerdict res = OrderVerdict.EMPTY;
        String req = "{\"buyer\": {\"uid\":654}}";
        when(antifraudService.checkOrder(any(OrderRequestDto.class)))
                .thenReturn(res);

        check("/antifraud/detect", req, false);
    }

    @Test
    public void testLoyaltyDetect() throws Exception {
        LoyaltyVerdictDto res = new LoyaltyVerdictDto(LoyaltyVerdictType.OK, Collections.emptyList(), Collections.emptyList(), null);
        String req = "{\"coins\":[{\"coinId\":12,\"promoId\":11,\"referral\":{\"rewardCoin\":{\"coinId\":1," +
                "\"promoId\":2,\"uid\":3},\"referralCoins\":[]}},{\"coinId\":15,\"promoId\":22}],\"uid\":654," +
                "\"orderIds\":[432],\"reason\":\"USER_CHECK\"}\n";
        when(loyaltyAntifraudService.checkPromoRequest(any()))
                .thenReturn(res);

        check("/antifraud/loyalty/detect", req, false);
    }

    @Test
    public void testLoyaltyRestrictions() throws Exception {
        long uid = 123L;
        String req = OBJECT_MAPPER.writeValueAsString(new LoyaltyBuyerRestrictionsRequestDto(uid, null));
        LoyaltyBuyerRestrictionsDto okResult = LoyaltyBuyerRestrictionsDto.ok(uid, null);
        when(loyaltyAntifraudService.checkRestrictions(any()))
                .thenReturn(okResult);

        check("/antifraud/loyalty/restrictions", req, false);
    }

    @Test
    public void testOrdersCount() throws Exception {
        long puid =123L;
        ArgumentCaptor<OrderCountRequest> captor = ArgumentCaptor.forClass(OrderCountRequest.class);
        String req = OBJECT_MAPPER.writeValueAsString(OrderCountRequestDtoV2.builder().puid(puid).build());
        OrderCountResponseDtoV2 okResult = OrderCountResponseDtoV2.builder().puid(puid).build();
        when(loyaltyAntifraudService.countOrders(captor.capture()))
                .thenReturn(CompletableFuture.completedFuture(okResult));

        check("/antifraud/loyalty/orders-count/v2", req, true);
    }

    private void check(String endpoint, String requestContent, boolean afAsync) throws Exception {
        String pumpkinResult = makeRequest("/pumpkin" + endpoint, requestContent, afAsync);
        String antifraudResult = makeRequest(endpoint, requestContent, afAsync);

        assertEquals(pumpkinResult, antifraudResult);
    }

    private String makeRequest(String endpoint, String requestContent, boolean async) throws Exception {
        var mvcResult = mockMvc.perform(post(endpoint)
                .content(requestContent)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andReturn();
        if (async) {
            mvcResult = mockMvc.perform(asyncDispatch(mvcResult)).andReturn();
        }
        return mvcResult.getResponse().getContentAsString();
    }
}
