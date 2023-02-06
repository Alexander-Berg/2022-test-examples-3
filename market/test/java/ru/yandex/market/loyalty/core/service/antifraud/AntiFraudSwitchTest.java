package ru.yandex.market.loyalty.core.service.antifraud;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestFiltersDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseDtoV2;
import ru.yandex.market.loyalty.core.model.coin.UserInfo;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.core.service.antifraud.AntiFraudService.NULL_VERDICT;

/**
 * @author artemmz
 */
public class AntiFraudSwitchTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private AntiFraudService antiFraudService;
    @Autowired
    private RestTemplate antifraudRestTemplate;

    @Test
    public void discountsAntiFraudSwitch() throws Exception {
        configurationService.set("market.loyalty.config.antifraud.enabled", "true");
        callAntifraudDetect();
        verify(antifraudRestTemplate, times(1)).exchange(any(RequestEntity.class),
                eq(LoyaltyVerdictDto.class));

        configurationService.set("market.loyalty.config.antifraud.enabled", "false");
        LoyaltyVerdictDto antifraudVerdict = callAntifraudDetect();
        assertEquals(antifraudVerdict, NULL_VERDICT);
        // was called only previous time when antifraud.enabled = true
        verify(antifraudRestTemplate, times(1)).exchange(any(RequestEntity.class),
                eq(LoyaltyVerdictDto.class));

    }

    @Test
    public void processOrdersCountRequestAntifraudSwitch() {
        configurationService.set("market.loyalty.config.antifraud.enabled", "true");
        callAntifraudOrdersCount();

        verify(antifraudRestTemplate, times(1)).exchange(any(RequestEntity.class),
                eq(OrderCountResponseDtoV2.class));

        configurationService.set("market.loyalty.config.antifraud.enabled", "false");
        int result = callAntifraudOrdersCount();
        assertEquals(0, result);
        // was called only previous time when antifraud.enabled = true
        verify(antifraudRestTemplate, times(1)).exchange(any(RequestEntity.class),
                eq(OrderCountResponseDtoV2.class));
    }

    @Test
    public void getUserRestrictionsAntifraudSwitch() throws Exception {
        var uid = 123L;
        configurationService.set("market.loyalty.config.antifraud.enabled", "true");
        callAntifraudUserRestrictions(uid);

        verify(antifraudRestTemplate, times(1))
                .exchange(any(RequestEntity.class), any(Class.class));

        configurationService.set("market.loyalty.config.antifraud.enabled", "false");
        var result = callAntifraudUserRestrictions(uid);
        var expected = UserRestrictions.ok(UserInfo.builder().setUid(uid).build());
        assertTrue(
                "Unexpected result",
                Objects.equals(expected.getUserInfo().getUid(), result.getUserInfo().getUid())
                        && expected.isCoinEmissionProhibited() == result.isCoinEmissionProhibited()
        );

        // was called only previous time when antifraud.enabled = true
        verify(antifraudRestTemplate, times(1))
                .exchange(any(RequestEntity.class), any(Class.class));
    }

    @Test
    public void isBonusRestrictedAntifraudSwitch() throws Exception {
        configurationService.set("market.loyalty.config.antifraud.enabled", "true");
        callAntifraudBonusRestricted();

        verify(antifraudRestTemplate, times(1))
                .exchange(any(RequestEntity.class), any(Class.class));

        configurationService.set("market.loyalty.config.antifraud.enabled", "false");
        var result = callAntifraudBonusRestricted();
        assertFalse(result);

        // was called only previous time when antifraud.enabled = true
        verify(antifraudRestTemplate, times(1))
                .exchange(any(RequestEntity.class), any(Class.class));
    }

    private boolean callAntifraudBonusRestricted() throws Exception {
        return antiFraudService.isBonusRestricted(10L, "10").get();
    }

    private UserRestrictions callAntifraudUserRestrictions(long uid) throws Exception {
        return antiFraudService.getUserRestrictions(UserInfo.builder().setUid(uid).build()).get();
    }

    private LoyaltyVerdictDto callAntifraudDetect() throws ExecutionException, InterruptedException {
        return antiFraudService.discountsAntiFraud(Collections.emptyList(), 1L, Collections.emptyList(),
                AntifraudCallReason.CALC, null).get();
    }

    private int callAntifraudOrdersCount() {
        return antiFraudService.processOrdersCountRequest(
                1L,
                OrderCountRequestFiltersDto.builder().build(),
                resp -> 1
        );
    }
}
