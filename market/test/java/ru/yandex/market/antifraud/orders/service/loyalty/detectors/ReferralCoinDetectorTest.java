package ru.yandex.market.antifraud.orders.service.loyalty.detectors;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyAntifraudContext;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyDetector;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyDetectorResult;
import ru.yandex.market.antifraud.orders.web.dto.CoinDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.ReferralCoin;
import ru.yandex.market.antifraud.orders.web.dto.ReferralInfo;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;
import ru.yandex.market.antifraud.orders.web.entity.PromoVerdictType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class ReferralCoinDetectorTest {

    private LoyaltyDetector detector = new ReferralCoinDetector();

    @Test
    public void shouldTriggerReward() {
        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder()
                .uid(1234L)
                .gluedUsers(
                        Set.of(
                                MarketUserId.fromUid(1234L),
                                MarketUserId.fromUid(1235L)
                        ))
                .originRequest(LoyaltyVerdictRequestDto.builder()
                        .coins(List.of(
                                new CoinDto(12L, 13L, new ReferralInfo(new ReferralCoin(14L, 15L, 1235L), null))
                        ))
                        .build())
                .build();
        LoyaltyDetectorResult result = detector.check(context);
        assertThat(result.getVerdict()).isEqualTo(LoyaltyVerdictType.OTHER);
        assertThat(result.getPromoVerdicts()).isNotEmpty();
        assertThat(result.getPromoVerdicts().stream()
                .anyMatch(c -> c.getVerdict().equals(PromoVerdictType.USED) && c.getCoinId().equals(12L)))
                .isTrue();
    }

    @Test
    public void shouldTriggerReferral() {
        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder()
                .uid(1234L)
                .gluedUsers(
                        Set.of(
                                MarketUserId.fromUid(1234L),
                                MarketUserId.fromUid(1235L)
                        ))
                .originRequest(LoyaltyVerdictRequestDto.builder()
                        .coins(List.of(
                                new CoinDto(
                                        12L,
                                        13L,
                                        new ReferralInfo(
                                                null,
                                                List.of(new ReferralCoin(14L, 15L, 1235L))
                                        ))
                        ))
                        .build())
                .build();
        LoyaltyDetectorResult result = detector.check(context);
        assertThat(result.getVerdict()).isEqualTo(LoyaltyVerdictType.OTHER);
        assertThat(result.getPromoVerdicts()).isNotEmpty();
        assertThat(result.getPromoVerdicts().stream()
                .anyMatch(c -> c.getVerdict().equals(PromoVerdictType.USED) && c.getCoinId().equals(12L)))
                .isTrue();
    }

    @Test
    public void shouldNotTrigger() {
        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder()
                .uid(1234L)
                .gluedUsers(
                        Set.of(
                                MarketUserId.fromUid(1234L),
                                MarketUserId.fromUid(1235L)
                        ))
                .originRequest(LoyaltyVerdictRequestDto.builder()
                        .coins(List.of(
                                new CoinDto(12L, 13L, new ReferralInfo(new ReferralCoin(14L, 15L, 1236L), null))
                        ))
                        .build())
                .build();
        LoyaltyDetectorResult result = detector.check(context);
        assertThat(result.getVerdict()).isEqualTo(LoyaltyVerdictType.OK);
    }

}
