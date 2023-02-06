package ru.yandex.market.loyalty.admin.tms;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.loyalty.admin.config.TestAuthorizationContext;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.core.dao.budgeting.UserExpectationDao;
import ru.yandex.market.loyalty.core.model.budgeting.Expectations;
import ru.yandex.market.loyalty.core.model.budgeting.UserExpectation;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.mail.YabacksMailer;
import ru.yandex.market.loyalty.core.utils.CoinRequestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
@TestFor(CheckUserExpectationsProcessor.class)
public class CheckUserExpectationsProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private CheckUserExpectationsProcessor checkUserExpectationsProcessor;
    @Autowired
    private YabacksMailer yabacksMailer;
    @Value("${market.loyalty.alert.mail.subscription}")
    private String alertEmail;
    @Autowired
    private UserExpectationDao userExpectationDao;
    @Autowired
    private DiscountUtils discountUtils;


    @Test
    public void testAverageBillSendEmailOnce() {
        createCoinAndSpend5Times();

        clock.spendTime(1, ChronoUnit.SECONDS);

        checkUserExpectationsProcessor.checkAverageBill();

        verify(yabacksMailer).sendMail(eq(alertEmail), anyString(), anyString());
        verify(yabacksMailer).sendMail(
                eq(TestAuthorizationContext.DEFAULT_USER_NAME + "@yandex-team.ru"),
                anyString(),
                anyString()
        );

        checkUserExpectationsProcessor.checkAverageBill();

        verifyZeroInteractions(yabacksMailer);
    }

    @Test
    public void testAverageBillSendEmailAfterEdit() {
        Promo promo = createCoinAndSpend5Times();

        clock.spendTime(1, ChronoUnit.SECONDS);

        userExpectationDao.saveUserExpectation(promo.getId(), new UserExpectation<>(
                Expectations.AVERAGE_BILL, BigDecimal.valueOf(301)
        ));

        checkUserExpectationsProcessor.checkAverageBill();

        verify(yabacksMailer).sendMail(eq(alertEmail), anyString(), anyString());
        verify(yabacksMailer).sendMail(
                eq(TestAuthorizationContext.DEFAULT_USER_NAME + "@yandex-team.ru"),
                anyString(),
                anyString()
        );
    }

    @Test
    public void testAverageBillSendEmailAfterEditToCorrectValue() {
        Promo promo = createCoinAndSpend5Times();

        clock.spendTime(1, ChronoUnit.SECONDS);

        userExpectationDao.saveUserExpectation(promo.getId(), new UserExpectation<>(
                Expectations.AVERAGE_BILL, BigDecimal.valueOf(1000)
        ));

        checkUserExpectationsProcessor.checkAverageBill();

        verifyZeroInteractions(yabacksMailer);
    }

    private Promo createCoinAndSpend5Times() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultPercent(BigDecimal.TEN)
                .setAverageBill(BigDecimal.valueOf(300))
                .setConversion(BigDecimal.TEN)
                .setBudget(BigDecimal.valueOf(3000))
                .setEmissionBudget(BigDecimal.valueOf(732))
        );

        promoService.updateStatus(promo, PromoStatus.ACTIVE);

        OrderWithBundlesRequest orderRequest = orderRequestWithBundlesBuilder()
                .withOrderItem()
                .build();

        for (int i = 0; i < 5; ++i) {
            CoinKey coin = coinService.create.createCoin(promo, CoinRequestUtils.defaultAuth().build());

            discountService.spendDiscount(DiscountRequestWithBundlesBuilder.builder(orderRequest)
                            .withCoins(coin)
                            .build(),
                    configurationService.currentPromoApplicabilityPolicy(),
                    null
            );

            assertEquals(CoreCoinStatus.USED, coinService.search.getCoin(coin).orElseThrow(AssertionError::new).getStatus());
        }

        return promo;
    }

}
