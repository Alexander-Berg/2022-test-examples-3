package ru.yandex.market.antifraud.orders.service.loyalty.detectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyAntifraudContext;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyCoin;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyPromo;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyDetectorResult;
import ru.yandex.market.antifraud.orders.storage.entity.rules.UsedCoinsDetectorConfiguration;
import ru.yandex.market.antifraud.orders.web.dto.CoinDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.PromoVerdictDto;
import ru.yandex.market.antifraud.orders.web.dto.ReferralInfo;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;
import ru.yandex.market.antifraud.orders.web.entity.PromoVerdictType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class UsedCoinsRuleTest {

    @Test
    public void shouldReturnOkCauseNoGluedUsers() {
        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder()
                .gluedUsers(Set.of())
                .build();
        UsedCoinsDetector rule = new UsedCoinsDetector();
        assertThat(rule.check(context)).isEqualTo(LoyaltyDetectorResult.ok(rule.getUniqName()));
    }

    @Test
    public void shouldReturnUsedCoins() {
        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder()
                .uid(123L)
                .gluedUsers(Set.of(
                        MarketUserId.fromUid(123L),
                        MarketUserId.fromUid(124L)
                ))
                .historyCoins(Arrays.asList(
                        LoyaltyCoin.builder().coinId(2001L).promoId(1201L).uid(124L).build(),
                        LoyaltyCoin.builder().coinId(2007L).promoId(1201L).uid(125L).build(),
                        LoyaltyCoin.builder().coinId(2008L).promoId(1201L).uid(126L).build(),
                        LoyaltyCoin.builder().coinId(2002L).promoId(1202L).uid(124L).build(),
                        LoyaltyCoin.builder().coinId(2003L).promoId(1203L).uid(123L).build()
                ))
                .promosToCheck(Arrays.asList(
                        LoyaltyPromo.builder().promoId(1201L).actionOnceRestrictionType("CHECK_USER").build(),  //
                        // основная акция для проверки
                        LoyaltyPromo.builder().promoId(1202L).actionOnceRestrictionType("").build(),    // обычная акция
                        LoyaltyPromo.builder().promoId(1203L).actionOnceRestrictionType("CHECK_USER").build(),  //
                        // для проверки повтора
                        LoyaltyPromo.builder().promoId(1205L).actionOnceRestrictionType("CHECK_USER").build()  //
                        // акция для новой валидной монеты
                ))
                .originRequest(LoyaltyVerdictRequestDto.builder().coins(
                        Arrays.asList(
                                new CoinDto(2003L, 1203L),  // повторная монета "одна в руки"

                                new CoinDto(2004L, 1201L),  // новая монета акции "одни в руки", для которой уже была
                                // монета
                                new CoinDto(2005L, 1202L),  // просто ещё одна монета
                                new CoinDto(2006L, 1205L)   // новая монета "одна в руки"
                        )
                ).build())
                .build();
        UsedCoinsDetector usedCoinsDetector = new UsedCoinsDetector();
        LoyaltyDetectorResult result = usedCoinsDetector.check(context, new UsedCoinsDetectorConfiguration(true, 5, 3));
        PromoVerdictDto expectedPromoVerdictDto = new PromoVerdictDto(2004L, 1201L, PromoVerdictType.USED);
        assertThat(result.getVerdict()).isEqualTo(LoyaltyVerdictType.OTHER);
        assertThat(result.getPromoVerdicts()).hasSize(1);
        assertThat(result.getPromoVerdicts().get(0)).isEqualTo(expectedPromoVerdictDto);
    }

    @Test
    public void noDataTest() {
        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder()
                .uid(123L)
                .build();
        UsedCoinsDetector rule = new UsedCoinsDetector();
        LoyaltyDetectorResult result = rule.check(context);
        assertThat(result.getVerdict()).isEqualTo(LoyaltyVerdictType.OK);
    }

    @Test
    public void shouldAllowForFamily() {
        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder()
                .uid(123L)
                .gluedUsers(Set.of(
                        MarketUserId.fromUid(123L),
                        MarketUserId.fromUid(124L),
                        MarketUserId.fromUuid("123123"),
                        MarketUserId.fromUuid("123414251"),
                        MarketUserId.fromUuid("1233"),
                        MarketUserId.fromUuid("33334")
                ))
                .historyCoins(Arrays.asList(
                        LoyaltyCoin.builder().coinId(2001L).promoId(1201L).uid(124L).build(),
                        LoyaltyCoin.builder().coinId(2002L).promoId(1202L).uid(124L).build(),
                        LoyaltyCoin.builder().coinId(2003L).promoId(1203L).uid(123L).build()
                ))
                .promosToCheck(Arrays.asList(
                        LoyaltyPromo.builder().promoId(1201L).actionOnceRestrictionType("CHECK_USER").build(),  //
                        // основная акция для проверки
                        LoyaltyPromo.builder().promoId(1202L).actionOnceRestrictionType("").build(),    // обычная акция
                        LoyaltyPromo.builder().promoId(1203L).actionOnceRestrictionType("CHECK_USER").build(),  //
                        // для проверки повтора
                        LoyaltyPromo.builder().promoId(1205L).actionOnceRestrictionType("CHECK_USER").build()  //
                        // акция для новой валидной монеты
                ))
                .originRequest(LoyaltyVerdictRequestDto.builder().coins(
                        Arrays.asList(
                                new CoinDto(2003L, 1203L),  // повторная монета "одна в руки"

                                new CoinDto(2004L, 1201L),  // новая монета акции "одни в руки", для которой уже была
                                // монета
                                new CoinDto(2005L, 1202L),  // просто ещё одна монета
                                new CoinDto(2006L, 1205L)   // новая монета "одна в руки"
                        )
                ).build())
                .build();
        UsedCoinsDetector usedCoinsDetector = new UsedCoinsDetector();
        LoyaltyDetectorResult result = usedCoinsDetector.check(context);
        PromoVerdictDto expectedPromoVerdictDto = new PromoVerdictDto(2004L, 1201L, PromoVerdictType.USED);
        assertThat(result.getVerdict()).isEqualTo(LoyaltyVerdictType.OK);
    }

    @Test
    public void shouldLimitBecauseOfBigGlue() {
        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder()
                .uid(123L)
                .gluedUsers(Set.of(
                        MarketUserId.fromUid(123L),
                        MarketUserId.fromUid(124L),
                        MarketUserId.fromUid(125L),
                        MarketUserId.fromUid(126L),
                        MarketUserId.fromUid(127L)
                ))
                .historyCoins(Arrays.asList(
                        LoyaltyCoin.builder().coinId(2001L).promoId(1201L).uid(124L).build(),
                        LoyaltyCoin.builder().coinId(2002L).promoId(1202L).uid(124L).build(),
                        LoyaltyCoin.builder().coinId(2003L).promoId(1203L).uid(123L).build()
                ))
                .promosToCheck(Arrays.asList(
                        LoyaltyPromo.builder().promoId(1201L).actionOnceRestrictionType("CHECK_USER").build(),  //
                        // основная акция для проверки
                        LoyaltyPromo.builder().promoId(1202L).actionOnceRestrictionType("").build(),    // обычная акция
                        LoyaltyPromo.builder().promoId(1203L).actionOnceRestrictionType("CHECK_USER").build(),  //
                        // для проверки повтора
                        LoyaltyPromo.builder().promoId(1205L).actionOnceRestrictionType("CHECK_USER").build()  //
                        // акция для новой валидной монеты
                ))
                .originRequest(LoyaltyVerdictRequestDto.builder().coins(
                        Arrays.asList(
                                new CoinDto(2003L, 1203L),  // повторная монета "одна в руки"

                                new CoinDto(2004L, 1201L),  // новая монета акции "одни в руки", для которой уже была
                                // монета
                                new CoinDto(2005L, 1202L),  // просто ещё одна монета
                                new CoinDto(2006L, 1205L)   // новая монета "одна в руки"
                        )
                ).build())
                .build();
        UsedCoinsDetector usedCoinsDetector = new UsedCoinsDetector();
        LoyaltyDetectorResult result = usedCoinsDetector.check(context, new UsedCoinsDetectorConfiguration(true, 3, 3));
        PromoVerdictDto expectedPromoVerdictDto = new PromoVerdictDto(2004L, 1201L, PromoVerdictType.USED);
        assertThat(result.getVerdict()).isEqualTo(LoyaltyVerdictType.OTHER);
        assertThat(result.getPromoVerdicts()).hasSize(1);
        assertThat(result.getPromoVerdicts().get(0)).isEqualTo(expectedPromoVerdictDto);
    }


    @Test
    public void shouldLimitReferalCoin() {
        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder()
                .uid(123L)
                .gluedUsers(Set.of(
                        MarketUserId.fromUid(123L),
                        MarketUserId.fromUid(124L)
                ))
                .historyCoins(Arrays.asList(
                        LoyaltyCoin.builder().coinId(2001L).promoId(1201L).uid(124L).build(),
                        LoyaltyCoin.builder().coinId(2002L).promoId(1202L).uid(124L).build(),
                        LoyaltyCoin.builder().coinId(2003L).promoId(1203L).uid(123L).build()
                ))
                .promosToCheck(Arrays.asList(
                        LoyaltyPromo.builder().promoId(1201L).actionOnceRestrictionType("CHECK_USER").build(),  //
                        // основная акция для проверки
                        LoyaltyPromo.builder().promoId(1202L).actionOnceRestrictionType("").build(),    // обычная акция
                        LoyaltyPromo.builder().promoId(1203L).actionOnceRestrictionType("CHECK_USER").build(),  //
                        // для проверки повтора
                        LoyaltyPromo.builder().promoId(1205L).actionOnceRestrictionType("CHECK_USER").build()  //
                        // акция для новой валидной монеты
                ))
                .originRequest(LoyaltyVerdictRequestDto.builder().coins(
                        Arrays.asList(
                                new CoinDto(2003L, 1203L),  // повторная монета "одна в руки"

                                new CoinDto(2004L, 1201L, new ReferralInfo(null, Collections.emptyList())),  // новая монета акции "одни в руки", для которой уже была
                                // монета
                                new CoinDto(2005L, 1202L),  // просто ещё одна монета
                                new CoinDto(2006L, 1205L)   // новая монета "одна в руки"
                        )
                ).build())
                .build();
        UsedCoinsDetector usedCoinsDetector = new UsedCoinsDetector();
        LoyaltyDetectorResult result = usedCoinsDetector.check(context, new UsedCoinsDetectorConfiguration(true, 1, 1));
        PromoVerdictDto expectedPromoVerdictDto = new PromoVerdictDto(2004L, 1201L, PromoVerdictType.USED);
        assertThat(result.getVerdict()).isEqualTo(LoyaltyVerdictType.OTHER);
        assertThat(result.getPromoVerdicts()).hasSize(1);
        assertThat(result.getPromoVerdicts().get(0)).isEqualTo(expectedPromoVerdictDto);
    }
}
