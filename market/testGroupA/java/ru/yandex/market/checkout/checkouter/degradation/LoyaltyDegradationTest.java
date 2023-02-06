package ru.yandex.market.checkout.checkouter.degradation;

import java.net.SocketTimeoutException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.CashbackResponseConverter;
import ru.yandex.market.checkout.checkouter.cashback.CashbackService;
import ru.yandex.market.checkout.checkouter.cashback.recalc.RecalculationRequestFactory;
import ru.yandex.market.checkout.checkouter.cashback.recalc.RecalculationResponseConverter;
import ru.yandex.market.checkout.checkouter.degradation.strategy.loyalty.LoyaltySpendDegradationStrategy;
import ru.yandex.market.checkout.checkouter.metrics.CheckouterRestTemplateBuilder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.promo.exceptions.PromoCodeException;
import ru.yandex.market.checkout.checkouter.promo.blueset.BlueSetPromoFeatureSupportHelper;
import ru.yandex.market.checkout.checkouter.promo.bundles.BundleItemsJoiner;
import ru.yandex.market.checkout.checkouter.promo.bundles.BundlesFeatureSupportHelper;
import ru.yandex.market.checkout.checkouter.promo.flash.FlashPromoFeatureSupportHelper;
import ru.yandex.market.checkout.checkouter.promo.loyalty.client.DiscountRequestFactory;
import ru.yandex.market.checkout.checkouter.promo.loyalty.client.DiscountResponseConverter;
import ru.yandex.market.checkout.checkouter.promo.promocode.PromocodeActivationRequestFactory;
import ru.yandex.market.checkout.checkouter.promo.promocode.PromocodeActivationResponseConverter;
import ru.yandex.market.checkout.checkouter.service.ApplePayDiscountPromotion;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyContext;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyServiceImpl;
import ru.yandex.market.checkout.checkouter.service.business.OrderFinancialService;
import ru.yandex.market.checkout.checkouter.service.business.PromosService;
import ru.yandex.market.checkout.checkouter.service.business.ReportDiscountCalculator;
import ru.yandex.market.checkout.checkouter.service.loyalty.LoyaltyClientImpl;
import ru.yandex.market.checkout.checkouter.service.loyalty.LoyaltyDiscountServiceImpl;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.degradation.DegradationStage.CHECKOUT;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.OTHER_ERROR;

@ContextConfiguration(classes = {LoyaltyServiceImpl.class, LoyaltySpendDegradationStrategy.class,
        LoyaltyClientImpl.class, LoyaltyDiscountServiceImpl.class})
@MockBean(classes = {
        OrderFinancialService.class,
        PromosService.class,
        BundleItemsJoiner.class,
        DiscountRequestFactory.class,
        DiscountResponseConverter.class,
        PromocodeActivationRequestFactory.class,
        CashbackResponseConverter.class,
        PromocodeActivationResponseConverter.class,
        BundlesFeatureSupportHelper.class,
        FlashPromoFeatureSupportHelper.class,
        BlueSetPromoFeatureSupportHelper.class,
        Tvm2.class,
        CashbackService.class,
        CheckouterRestTemplateBuilder.class,
        ApplePayDiscountPromotion.class,
        ReportDiscountCalculator.class,
        RecalculationRequestFactory.class,
        RecalculationResponseConverter.class
})
@TestPropertySource(properties = "market.loyalty.tvm.client_id=0")
public class LoyaltyDegradationTest extends AbstractDegradationTest {

    @Autowired
    private Tvm2 tvm2;
    @Autowired
    private LoyaltySpendDegradationStrategy strategy;
    @Autowired
    private LoyaltyServiceImpl loyaltyService;
    @Autowired
    private RestTemplate loyaltyRestTemplate;
    @MockBean
    @Qualifier("defaultConversionService")
    private ConfigurableConversionService conversionService;
    @Autowired
    private CheckouterProperties properties;

    @BeforeEach
    void init() {
        log.addAppender(appender);
        appender.clear();
        appender.start();
        when(properties.getEnableLoyaltySpendDegradationStrategy()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        log.detachAppender(appender);
    }

    @Test
    void onErrorTest() {

        ImmutableMultiCartParameters cartParameters = mock(ImmutableMultiCartParameters.class);
        LoyaltyContext context = mock(LoyaltyContext.class);
        doThrow(new RuntimeException("Some exception"))
                .when(loyaltyRestTemplate)
                .exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));

        DegradationContextHolder.setStage(CHECKOUT);

        MultiCart cart = mock(MultiCart.class);
        Order order = mock(Order.class);

        when(tvm2.getServiceTicket(anyInt())).thenReturn(Option.empty());
        when(cart.getCarts()).thenReturn(List.of(order));
        when(cart.getPromoCode()).thenReturn("");
        when(order.isFake()).thenReturn(false);

        assertThrows(PromoCodeException.class, () -> loyaltyService.applyDiscounts(cart, cartParameters, context));

        assertOnErrorLog(strategy.getCallName());
    }

    @Test
    void onTimeoutErrorTest() {

        ImmutableMultiCartParameters cartParameters = mock(ImmutableMultiCartParameters.class);
        LoyaltyContext context = mock(LoyaltyContext.class);
        doThrow(new ResourceAccessException("Some exception", new SocketTimeoutException()))
                .when(loyaltyRestTemplate)
                .exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));

        DegradationContextHolder.setStage(CHECKOUT);

        MultiCart cart = mock(MultiCart.class);
        Order order = mock(Order.class);

        when(tvm2.getServiceTicket(anyInt())).thenReturn(Option.empty());
        when(cart.getCarts()).thenReturn(List.of(order));
        when(cart.getPromoCode()).thenReturn("");
        when(order.isFake()).thenReturn(false);

        PromoCodeException promoCodeException = assertThrows(PromoCodeException.class,
                () -> loyaltyService.applyDiscounts(cart, cartParameters, context));
        assertThat(promoCodeException.getErrorCode(), is(OTHER_ERROR.name()));

        assertOnErrorLog(strategy.getCallName());
    }
}
