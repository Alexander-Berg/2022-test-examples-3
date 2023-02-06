package ru.yandex.market.loyalty.admin.tms;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.loyalty.admin.config.TestAuthorizationContext;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.mail.YabacksMailer;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
@TestFor(PromoEndProcessor.class)
public class PromoEndProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final int DAYS_TO_BE_ROTTEN = 90;

    @Value("${market.loyalty.alert.mail.subscription}")
    private String alertEmail;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoEndProcessor promoEndProcessor;
    @Autowired
    private YabacksMailer yabacksMailer;
    @Autowired
    private PromoService promoService;

    @Test
    public void testPromoEnd() {
        testNotifications(promoEndProcessor::checkPromoEnd);
    }

    @Test
    public void testPromoEmissionEnd() {
        testNotifications(promoEndProcessor::checkPromoEmissionEnd);
    }

    @Test
    public void shouldGroupPromosByType() {
        String couponPromoName = "coupon promo";
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .setName(couponPromoName)
                        .setEndDate(Date.from(clock.instant().plus(DAYS_TO_BE_ROTTEN, ChronoUnit.DAYS)))
        );
        String firstSmartshoppingName = "first smartshopping";
        String secondSmartshoppingName = "second smartshopping";
        promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setName(firstSmartshoppingName)
                .setEndDate(Date.from(clock.instant().plus(DAYS_TO_BE_ROTTEN, ChronoUnit.DAYS)))
        );
        promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setName(secondSmartshoppingName)
                .setEndDate(Date.from(clock.instant().plus(DAYS_TO_BE_ROTTEN, ChronoUnit.DAYS)))
        );

        advanceAndCheck(DAYS_TO_BE_ROTTEN - 1, promoEndProcessor::checkPromoEnd);

        verify(yabacksMailer).sendMail(eq(alertEmail), anyString(), argThat(allOf(
                containsString("Купонные акции:"),
                containsString(couponPromoName),
                containsString("Смартшоппинг:"),
                containsString(firstSmartshoppingName),
                containsString(secondSmartshoppingName)
        )));
    }

    @Test
    public void shouldAddNamesOfCreatorAndLastEditorAndSendMailToThem() {
        String creatorName = "user1";
        String firstEditor = "user2";
        String secondEditor = "user3";

        authorizationContext.setUserName(creatorName);
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setEndDate(Date.from(clock.instant().plus(DAYS_TO_BE_ROTTEN, ChronoUnit.DAYS)))
        );
        authorizationContext.setUserName(firstEditor);
        promoManager.updateCoinPromo(PromoUtils.SmartShopping.defaultFixed()
                .setId(promo.getId())
                .setName("changed")
                .setEndDate(Date.from(clock.instant().plus(DAYS_TO_BE_ROTTEN, ChronoUnit.DAYS)))
        );
        authorizationContext.setUserName(secondEditor);
        promoManager.updateCoinPromo(PromoUtils.SmartShopping.defaultFixed()
                .setId(promo.getId())
                .setName("another change")
                .setEndDate(Date.from(clock.instant().plus(DAYS_TO_BE_ROTTEN, ChronoUnit.DAYS)))
        );

        advanceAndCheck(DAYS_TO_BE_ROTTEN - 1, promoEndProcessor::checkPromoEnd);

        verify(yabacksMailer).sendMail(eq(alertEmail), anyString(), argThat(allOf(
                containsString(creatorName),
                containsString(secondEditor)
        )));

        verify(yabacksMailer).sendMail(eq(creatorName + "@yandex-team.ru"), anyString(), argThat(
                containsString(creatorName)
        ));

        verify(yabacksMailer).sendMail(eq(secondEditor + "@yandex-team.ru"), anyString(), argThat(
                containsString(secondEditor)
        ));
    }

    @Test
    public void shouldOffPromoByDeactivationThreshold() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.TEN)
                        .setEmissionBudget(BigDecimal.ONE)
                        .setDeactivationBudgetThreshold(BigDecimal.TEN)
        );

        promoEndProcessor.deactivatePromoByDeactivationThreshold();

        assertEquals(PromoStatus.INACTIVE, promoService.getPromo(promo.getId()).getStatus());
    }

    @Test
    public void shouldNotOffPromoByDeactivationThreshold() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.TEN)
                        .setEmissionBudget(BigDecimal.TEN)
                        .setDeactivationBudgetThreshold(BigDecimal.ONE)
        );

        promoEndProcessor.deactivatePromoByDeactivationThreshold();

        assertEquals(PromoStatus.ACTIVE, promoService.getPromo(promo.getId()).getStatus());
    }

    private void testNotifications(Runnable checker) {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .setEndDate(Date.from(clock.instant().plus(DAYS_TO_BE_ROTTEN, ChronoUnit.DAYS)))
                        .setStartEmissionDate(Date.from(clock.instant()))
                        .setEndEmissionDate(Date.from(clock.instant().plus(DAYS_TO_BE_ROTTEN, ChronoUnit.DAYS)))
        );

        advanceAndCheck(0, checker);
        advanceAndCheck(DAYS_TO_BE_ROTTEN - 4, checker);
        advanceAndCheck(1, checker);
        verifyZeroInteractions(yabacksMailer);

        for (int day = 0; day < 3; ++day) {
            advanceAndCheck(1, checker);
        }
        verify(yabacksMailer, times(3)).sendMail(eq(alertEmail), anyString(), anyString());
        verify(yabacksMailer, times(3)).sendMail(eq(TestAuthorizationContext.DEFAULT_USER_NAME + "@yandex-team.ru"),
                anyString(), anyString());

        advanceAndCheck(1, checker);
        verifyZeroInteractions(yabacksMailer);
    }

    private void advanceAndCheck(int daysToAdvance, Runnable checker) {
        clock.spendTime(daysToAdvance, ChronoUnit.DAYS);
        checker.run();
    }

}
