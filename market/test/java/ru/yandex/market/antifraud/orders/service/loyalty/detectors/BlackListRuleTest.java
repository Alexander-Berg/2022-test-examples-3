package ru.yandex.market.antifraud.orders.service.loyalty.detectors;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRule;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyAntifraudContext;
import ru.yandex.market.antifraud.orders.service.BlacklistService;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyDetectorResult;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictRequestDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;


/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.class)
public class BlackListRuleTest {

    private static final Long TEST_UID = 231L;

    @Mock
    private BlacklistService blackListService;

    @Test
    public void shouldPassNoBlacklistedUsers() {
        BlackListDetector rule = new BlackListDetector(blackListService);
        when(blackListService.checkLoyaltyBlacklist(anyLong(), anyCollection()))
                .thenReturn(Collections.emptyList());
        LoyaltyAntifraudContext context = getTestContext();
        LoyaltyDetectorResult result = rule.check(context);
        assertThat(result).isEqualTo(LoyaltyDetectorResult.ok(rule.getUniqName()));
    }

    @Test
    public void shouldNotPassUserBlacklisted() {
        BlackListDetector rule = new BlackListDetector(blackListService);
        when(blackListService.checkLoyaltyBlacklist(anyLong(), anyCollection()))
            .thenReturn(List.of(AntifraudBlacklistRule.empty()));
        LoyaltyAntifraudContext context = getTestContext();
        LoyaltyDetectorResult result = rule.check(context);
        assertThat(result).isEqualTo(LoyaltyDetectorResult.blacklist(rule.getUniqName()));
    }

    @Test
    public void noDataTest() {
        BlackListDetector rule = new BlackListDetector(blackListService);
        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder()
            .originRequest(LoyaltyVerdictRequestDto.builder().build())
            .build();
        LoyaltyDetectorResult result = rule.check(context);
        assertThat(result).isEqualTo(LoyaltyDetectorResult.ok(rule.getUniqName()));
    }

    private LoyaltyAntifraudContext getTestContext() {
        return LoyaltyAntifraudContext.builder()
            .uid(TEST_UID)
            .gluedUsers(Set.of(
                MarketUserId.fromUid(TEST_UID),
                MarketUserId.fromUid(TEST_UID + 1),
                MarketUserId.fromUid(TEST_UID + 2)
            ))
            .build();
    }

}
