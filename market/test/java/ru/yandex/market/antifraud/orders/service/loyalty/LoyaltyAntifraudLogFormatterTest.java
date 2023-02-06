package ru.yandex.market.antifraud.orders.service.loyalty;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyAntifraudContext;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyCoin;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyPromo;
import ru.yandex.market.antifraud.orders.web.dto.CoinDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.ReferralCoin;
import ru.yandex.market.antifraud.orders.web.dto.ReferralInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class LoyaltyAntifraudLogFormatterTest {

    @Test
    public void test() {
        LoyaltyAntifraudLogFormatter formatter = new LoyaltyAntifraudLogFormatter();
        LoyaltyAntifraudContext context = getTestContext();
        String msg = formatter.format(context, "rule_name", LoyaltyDetectorResult.OK_RESULT)
                .replaceFirst("\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2} \\+\\d{4}]", "[datetime]");
        System.out.println(msg);
        assertThat(msg)
                .isEqualTo(
                        "tskv\tdatetime=[datetime]\tfraudStatus=OK\tpromoVerdicts=\tdetectorName=rule_name\t" +
                                "request={coins=[CoinDto(coinId=1, promoId=2, referralInfo=ReferralInfo(rewardCoin=ReferralCoin(uid=7), referralCoins=[])), " +
                                "CoinDto(coinId=3, promoId=4, referralInfo=null)], " +
                                "uid=123, order_ids=[33, 33], reason=USER_CHECK }\t" +
                                "users=[userId=124, idType=uid, ]");
        System.out.println(msg);
    }

    @Test
    public void testEmpty() {
        LoyaltyAntifraudLogFormatter formatter = new LoyaltyAntifraudLogFormatter();
        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder().build();
        String msg = formatter.format(context, "rule_name", LoyaltyDetectorResult.OK_RESULT)
                .replaceFirst("\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2} \\+\\d{4}]", "[datetime]");
        assertThat(msg).isEqualTo(
                "tskv\tdatetime=[datetime]\tfraudStatus=OK\tpromoVerdicts=\tdetectorName=rule_name\trequest={}\t" +
                        "users=[]");
    }

    private LoyaltyAntifraudContext getTestContext() {
        return LoyaltyAntifraudContext.builder()
                .uid(123L)
                .originRequest(LoyaltyVerdictRequestDto.builder()
                        .coins(Arrays.asList(
                                new CoinDto(1L, 2L, new ReferralInfo(new ReferralCoin(5L, 6L, 7L), Collections.emptyList())),
                                new CoinDto(3L, 4L)
                        ))
                        .uid(123L)
                        .orderIds(Arrays.asList(33L, 33L))
                        .reason("USER_CHECK")
                        .build())
                .gluedUsers(Set.of(
//                        MarketUserId.fromUid(123L),
                        MarketUserId.fromUid(124L)
                ))
                .promosToCheck(Arrays.asList(
                        LoyaltyPromo.builder().promoId(123L).promoName("promo1").build(),
                        LoyaltyPromo.builder().promoId(124L).promoName("promo2").build()
                ))
                .historyCoins(Arrays.asList(
                        LoyaltyCoin.builder().uid(123L).build(),
                        LoyaltyCoin.builder().uid(124L).build()
                ))
                .build();
    }

}
