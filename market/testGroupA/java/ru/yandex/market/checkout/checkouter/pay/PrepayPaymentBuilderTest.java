package ru.yandex.market.checkout.checkouter.pay;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.auth.AuthService;
import ru.yandex.market.checkout.checkouter.balance.trust.model.CreateBasketRequest;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.builders.PrepayPaymentBuilder;
import ru.yandex.market.checkout.checkouter.pay.cashier.CreatePaymentContext;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.common.util.UrlBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.checkout.checkouter.pay.PaymentBuilderTest.createOrder;

@DisplayName("Проверяем отключение сard selector")
public class PrepayPaymentBuilderTest extends AbstractServicesTestBase {

    @Autowired
    private AuthService service;

    @Autowired
    private PersonalDataService personalDataService;

    @Test
    @DisplayName("Проверяем что при включенном тогле блок blocks_visibility передается.")
    public void cardSelectorByToggleOn() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.DISABLE_CARD_SELECTOR, true);
        List<Order> orders = List.of(createOrder());
        CreatePaymentContext context = CreatePaymentContext.builder().build();
        PrepayPaymentBuilder paymentBuilder =
                new PrepayPaymentBuilder(orders, service, context, PaymentGoal.ORDER_PREPAY,
                        checkouterFeatureReader.getBoolean(BooleanFeatureType.DISABLE_CARD_SELECTOR),
                        false, personalDataService::getPersAddress);
        paymentBuilder.setCurrency(Currency.RUR);
        paymentBuilder.setServiceUrl(UrlBuilder.fromString("http://market.yandex.net:39001"));

        CreateBasketRequest basketRequest = paymentBuilder.build();
        assertThat(basketRequest.getDeveloperPayload(),
                equalTo("{\"call_preview_payment\":\"card_info\"," +
                        "\"blocks_visibility\":{\"cardSelector\":false}}"));
    }

    @Test
    @DisplayName("Проверяем что при выключенном тогле блок blocks_visibility НЕ передается.")
    public void cardSelectorByToggleOff() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.DISABLE_CARD_SELECTOR, false);
        List<Order> orders = List.of(createOrder());
        CreatePaymentContext context = CreatePaymentContext.builder().build();
        PrepayPaymentBuilder paymentBuilder =
                new PrepayPaymentBuilder(orders, service, context, PaymentGoal.ORDER_PREPAY,
                        checkouterFeatureReader.getBoolean(BooleanFeatureType.DISABLE_CARD_SELECTOR),
                        false, personalDataService::getPersAddress);
        paymentBuilder.setCurrency(Currency.RUR);
        paymentBuilder.setServiceUrl(UrlBuilder.fromString("http://market.yandex.net:39001"));

        CreateBasketRequest basketRequest = paymentBuilder.build();
        assertThat(basketRequest.getDeveloperPayload(),
                equalTo("{\"call_preview_payment\":\"card_info\"}"));
    }

    @Test
    @DisplayName("Проверяем что при передаче в PrepayPaymentBuilder isSbpActiveTouch==true в поле devPayload " +
            "передается блок is_sbp_active_touch со значением banks")
    public void isSbpActiveTouch() {
        List<Order> orders = List.of(createOrder());
        CreatePaymentContext context = CreatePaymentContext.builder().build();
        PrepayPaymentBuilder paymentBuilder =
                new PrepayPaymentBuilder(orders, service, context, PaymentGoal.ORDER_PREPAY, false, true,
                        personalDataService::getPersAddress);
        paymentBuilder.setCurrency(Currency.RUR);
        paymentBuilder.setServiceUrl(UrlBuilder.fromString("http://market.yandex.net:39001"));

        CreateBasketRequest basketRequest = paymentBuilder.build();
        assertThat(basketRequest.getDeveloperPayload(),
                equalTo("{\"call_preview_payment\":\"card_info\",\"is_sbp_active_touch\":\"banks\"}"));
    }

    @Test
    @DisplayName("Проверяем что при передаче в PrepayPaymentBuilder isSbpActiveTouch==false в поле devPayload " +
            "НЕ передается блок is_sbp_active_touch со значением banks")
    public void isNotSbpActiveTouch() {
        List<Order> orders = List.of(createOrder());
        CreatePaymentContext context = CreatePaymentContext.builder().build();
        PrepayPaymentBuilder paymentBuilder =
                new PrepayPaymentBuilder(orders, service, context, PaymentGoal.ORDER_PREPAY, false, false,
                        personalDataService::getPersAddress);
        paymentBuilder.setCurrency(Currency.RUR);
        paymentBuilder.setServiceUrl(UrlBuilder.fromString("http://market.yandex.net:39001"));

        CreateBasketRequest basketRequest = paymentBuilder.build();
        assertThat(basketRequest.getDeveloperPayload(),
                equalTo("{\"call_preview_payment\":\"card_info\"}"));
    }
}
