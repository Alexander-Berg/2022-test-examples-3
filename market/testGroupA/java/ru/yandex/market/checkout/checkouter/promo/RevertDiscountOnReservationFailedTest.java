package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.assertj.core.api.ListAssert;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount.createCoin;

public class RevertDiscountOnReservationFailedTest extends AbstractWebTestBase {

    public static final long COIN_ID = 212L;
    @Autowired
    private WireMockServer marketLoyaltyMock;

    @BeforeEach
    void setUp() {
        loyaltyConfigurer.mockSuccessRevert();
    }

    @Test
    public void shouldRevertDiscountOnReservationFailed() throws Exception {
        Parameters parameters = new Parameters();
        Parameters anotherParameter = new Parameters();
        anotherParameter.setShopId(1774L);
        parameters.addOrder(anotherParameter);

        OrderItem first = parameters.getOrder().getItems().iterator().next();

        LoyaltyParameters loyaltyParameters = parameters.getLoyaltyParameters();
        loyaltyParameters
                .addLoyaltyDiscount(first, PromoType.MARKET_COUPON, BigDecimal.TEN)
                .addLoyaltyDiscount(first, createCoin(BigDecimal.valueOf(15L), "testCoin", COIN_ID))
                .addLoyaltyDiscount(first, PromoType.MARKET_COUPON, BigDecimal.valueOf(4L))
                .addLoyaltyDiscount(first, createCoin(BigDecimal.valueOf(8L), "testCoin", COIN_ID));

        parameters.configureMultiCart(mc -> mc.setCoinIdsToUse(Collections.singletonList(COIN_ID)));
        parameters.setMockLoyalty(true);
        parameters.setMockPushApi(false);
        pushApiConfigurer.mockCart(parameters.getOrder(), anotherParameter.getPushApiDeliveryResponses(), false);
        pushApiConfigurer.mockCart(anotherParameter.getOrder(), anotherParameter.getPushApiDeliveryResponses(), false);

        pushApiConfigurer.mockAccept(parameters.getOrder());
        pushApiConfigurer.mockAcceptFailure(anotherParameter.getOrder());

        parameters.turnOffErrorChecks();

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        MatcherAssert.assertThat(multiOrder.getOrderFailures(), hasSize(1));
        MatcherAssert.assertThat(multiOrder.getOrderFailures().get(0).getErrorCode(),
                equalTo(OrderFailure.Code.UNKNOWN_ERROR));

        checkOrderCreatedAndCanceled(multiOrder.getCartFailures().get(0).getOrder().getId());

        assertThatLoyaltyRevertCalls().hasSize(1);
    }

    private void checkOrderCreatedAndCanceled(long orderId) throws Exception {
        setFixedTime(getClock().instant().plus(10, ChronoUnit.MINUTES));
        tmsTaskHelper.runExpireOrderTaskV2();

        Order order = orderService.getOrder(orderId);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(OrderSubstatus.RESERVATION_FAILED, order.getSubstatus());
    }

    private ListAssert<ServeEvent> assertThatLoyaltyRevertCalls() {
        return assertThat(
                marketLoyaltyMock.getServeEvents().getServeEvents().stream()
                        .filter(se -> se.getRequest().getUrl().contains("revert"))
        );
    }

}
