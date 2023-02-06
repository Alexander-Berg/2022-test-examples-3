package ru.yandex.market.loyalty.back.util;

import java.util.Date;

import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsDto;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.config.Blackbox;
import ru.yandex.market.loyalty.core.dao.ydb.CashbackOrdersDao;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.TOTAL_CASHBACK_DAILY_THRESHOLD;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.TOTAL_CASHBACK_MONTHLY_THRESHOLD;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@Service
public class AntifraudUtils {
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    @Blackbox
    private RestTemplate blackboxRestTemplate;
    @Autowired
    private CashbackOrdersDao cashbackOrdersDao;

    private AntifraudUtils() {
    }

    public static void setupDeclinedAntifraudResponseForUid(
            RestTemplate antifraudRestTemplate,
            long uid,
            LoyaltyBuyerRestrictionsDto response) {
        when(antifraudRestTemplate.exchange(
                argThat(Matchers.<RequestEntity>both(hasProperty("url", hasProperty("path", equalTo("/antifraud" +
                        "/loyalty/restrictions"))))
                        .and(hasProperty("body", hasProperty("uid", equalTo(uid))))),
                eq(LoyaltyBuyerRestrictionsDto.class)
        )).thenReturn(ResponseEntity.ok(response));
    }

    @SafeVarargs
    public final void mockTotalCashbackThresholdExhausted(long uid, Date regDate, Pair<PerkType, Boolean>... types) {
        configurationService.set(TOTAL_CASHBACK_DAILY_THRESHOLD, 1000);
        configurationService.set(TOTAL_CASHBACK_MONTHLY_THRESHOLD, 10000);

        BlackboxUtils.mockBlackbox(DEFAULT_UID,
                blackboxRestTemplate,
                BlackboxUtils.mockBlackboxResponse(regDate, types)
        );

        when(cashbackOrdersDao.checkTotalCashbackThreshold(
                eq(uid),
                any(),
                any()
        )).thenReturn(false);

        when(cashbackOrdersDao.checkTotalCashbackThreshold(
                eq(uid),
                any(),
                any()
        )).thenReturn(false);
    }

    @SafeVarargs
    public final void mockTotalCashbackThresholdAvailable(long uid, Date regDate, Pair<PerkType, Boolean>... types) {
        configurationService.set(TOTAL_CASHBACK_DAILY_THRESHOLD, 1000);
        configurationService.set(TOTAL_CASHBACK_MONTHLY_THRESHOLD, 10000);

        BlackboxUtils.mockBlackbox(DEFAULT_UID,
                blackboxRestTemplate,
                BlackboxUtils.mockBlackboxResponse(regDate, types)
        );

        when(cashbackOrdersDao.checkTotalCashbackThreshold(
                eq(uid),
                any(),
                any()
        )).thenReturn(true);

        when(cashbackOrdersDao.checkTotalCashbackThreshold(
                eq(uid),
                any(),
                any()
        )).thenReturn(true);
    }

}
