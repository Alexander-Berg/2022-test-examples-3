package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Проверяем различные вычисления с валютой магазина и/или доставки, отличной от пользовательской.
 */
public class CreateOrderCurrencyTest extends AbstractWebTestBase {

    private static final String REAL_PROMO_CODE = "REAL-PROMO-CODE";

    private static final BigDecimal USD_TO_RUR_RATE = new BigDecimal("2.5");
    private static final BigDecimal RUR_TO_USD_RATE = new BigDecimal("0.4");

    @Autowired
    private OrderPayHelper orderPayHelper;

    private static void validatePayment(Order order, Payment payment) {
        assertThat(payment.getTotalAmount(), comparesEqualTo(order.getBuyerTotal()));
    }

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-105
     * https://testpalm.yandex-team.ru/testcase/checkouter-109
     * <p>
     * Успешное создание заказа с валютой пользователя, не отличающейся от магазинной.
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-105
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-109
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldCreateOrderWithBuyerCurrencyRURandShopCurrencyRUR(boolean prepaid) {
        Parameters parameters = new Parameters();

        if (prepaid) {
            parameters.setPaymentMethod(PaymentMethod.YANDEX);
        }

        Order order = checkoutAndGetFromApi(parameters);

        assertEquals(Currency.RUR, order.getCurrency());
        assertEquals(Currency.RUR, order.getBuyerCurrency());
        assertThat(order.getExchangeRate(), comparesEqualTo(new BigDecimal("1")));

        if (prepaid) {
            Payment payment = orderPayHelper.pay(order.getId());
            validatePayment(order, payment);
        }
    }

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-106
     * https://testpalm.yandex-team.ru/testcase/checkouter-110
     * <p>
     * Пытаемся создать заказ с валютой пользователя - рублями и валютой магазина - долларами.
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-106
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-110
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldCreateOrderWithBuyerCurrencyRURandShopCurrencyUSD(boolean prepaid) {
        Parameters parameters = createParametersForUSDShop();

        if (prepaid) {
            parameters.setPaymentMethod(PaymentMethod.YANDEX);
        }

        ReportGeneratorParameters reportParameters = parameters.getReportParameters();
        reportParameters.setShopCurrency(Currency.USD);
        reportParameters.setDeliveryCurrency(Currency.USD);
        reportParameters.setShopToUserConvertRate(USD_TO_RUR_RATE);
        parameters.getReportParameters().getCurrencyRates().put(Pair.of(Currency.RUR, Currency.USD), RUR_TO_USD_RATE);

        Order order = orderCreateHelper.createOrder(parameters);

        // Проверяем, что заказ создался с нужными валютами
        assertEquals(Currency.USD, order.getCurrency());
        assertEquals(Currency.RUR, order.getBuyerCurrency());
        assertEquals(USD_TO_RUR_RATE, order.getExchangeRate());

        // Проверяем, что заказ создался с правильной магазинной и покупательской ценами
        assertThat(order.getDelivery().getBuyerPrice(), comparesEqualTo(new BigDecimal("25")));
        assertThat(order.getDelivery().getPrice(), comparesEqualTo(new BigDecimal("10")));

        assertThat(order.getBuyerItemsTotal(), comparesEqualTo(new BigDecimal("625")));
        assertThat(order.getItemsTotal(), comparesEqualTo(new BigDecimal("250")));

        assertThat(order.getBuyerTotal(), comparesEqualTo(new BigDecimal("650")));
        assertThat(order.getTotal(), comparesEqualTo(new BigDecimal("260")));

        assertThat(order.getFeeTotal(), comparesEqualTo(new BigDecimal("12.2")));

        if (prepaid) {
            Payment payment = orderPayHelper.pay(order.getId());
            validatePayment(order, payment);
        }
    }

    /**
     * checkouter-108
     * checkouter-112
     * <p>
     * Проверяем что субсидии правильно считаются, если магазин долларовый.
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-108
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldCalculateSubsidyCorrectlyIfShopCurrencyIsNotRUR(boolean prepaid) {
        Parameters parameters = createParametersForUSDShop();

        if (prepaid) {
            parameters.setPaymentMethod(PaymentMethod.YANDEX);
        }

        //given
        parameters.configureMultiCart(multiCart -> {
            multiCart.setPaymentMethod(PaymentMethod.YANDEX);
            multiCart.setPaymentType(PaymentType.PREPAID);
            multiCart.setPromoCode(REAL_PROMO_CODE);
        });
        parameters.getReportParameters().setShopSupportsSubsidies(true);
        parameters.getReportParameters().setShopCurrency(Currency.USD);
        parameters.getReportParameters().setDeliveryCurrency(Currency.USD);
        parameters.getReportParameters().setShopToUserConvertRate(USD_TO_RUR_RATE);
        parameters.getReportParameters().getCurrencyRates().put(Pair.of(Currency.RUR, Currency.USD), RUR_TO_USD_RATE);
        parameters.setMockLoyalty(true);

        //when
        Order createdOrder = checkoutAndGetFromApi(parameters);

        // then
        // Проверяем, что заказ создался с правильной магазинной и покупательской ценами
        assertThat(createdOrder.getDelivery().getBuyerPrice(), comparesEqualTo(new BigDecimal("25.00")));
        assertThat(createdOrder.getBuyerSubsidyTotal(), comparesEqualTo(new BigDecimal("100.00")));
        assertThat(createdOrder.getBuyerItemsTotal(), comparesEqualTo(new BigDecimal("525.01")));
        assertThat(createdOrder.getBuyerTotal(), comparesEqualTo(new BigDecimal("550.01")));
        assertThat(createdOrder.getBuyerTotalWithSubsidy(), comparesEqualTo(new BigDecimal("650.01")));

        assertThat(createdOrder.getDelivery().getPrice(), comparesEqualTo(new BigDecimal("10.00")));

        // todo: Это пригодится нам потом, когда удалим CHECKOUTER_SERVICE_CALCULATE_AND_SET_PROMO_TOTALS
        assertThat(createdOrder.getPromoPrices().getSubsidyTotal(), comparesEqualTo(new BigDecimal("40.00")));
        assertThat(createdOrder.getPromoPrices().getBuyerItemsTotalDiscount(),
                comparesEqualTo(new BigDecimal("99.99")));
        assertThat(createdOrder.getPromoPrices().getBuyerTotalDiscount(), comparesEqualTo(new BigDecimal("99.99")));
        assertThat(createdOrder.getPromoPrices().getBuyerItemsTotalBeforeDiscount(),
                comparesEqualTo(new BigDecimal("625.00")));
        assertThat(createdOrder.getPromoPrices().getBuyerTotalBeforeDiscount(),
                comparesEqualTo(new BigDecimal("650.00")));
        assertThat(createdOrder.getCurrency(), comparesEqualTo(Currency.USD));

        assertThat(createdOrder.getItemsTotal(), comparesEqualTo(new BigDecimal("210.00")));
        assertThat(createdOrder.getTotal(), comparesEqualTo(new BigDecimal("220.00")));
        assertThat(createdOrder.getTotalWithSubsidy(), comparesEqualTo(new BigDecimal("260.00")));

        assertThat(createdOrder.getFeeTotal(), comparesEqualTo(new BigDecimal("12.20")));

        if (prepaid) {
            Payment payment = orderPayHelper.pay(createdOrder.getId());
            validatePayment(createdOrder, payment);
        }
    }

    @Nonnull
    private Parameters createParametersForUSDShop() {
        var params = new Parameters(OrderProvider.getWhiteOrder((o) -> {
            // чтобы нам не подмешивали левую доставку и нам не пришлось для неё мокать курренси_конверт.
            o.setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
            o.getItems().forEach(oi -> oi.setBuyerPrice(oi.getPrice().multiply(USD_TO_RUR_RATE)));
        }));
        // чтобы нам не подмешивали левую доставку и нам не пришлось для неё мокать курренси_конверт.
        params.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        params.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        params.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder()
                .build());
        params.setPushApiDeliveryResponse(DeliveryProvider.shopSelfDelivery()
                .buildResponse(DeliveryResponse::new));
        return params;
    }

    private Order checkoutAndGetFromApi(Parameters parameters) {
        Order checkoutResult = orderCreateHelper.createOrder(parameters);
        long orderId = checkoutResult.getId();
        return orderService.getOrder(orderId);
    }
}
