package ru.yandex.market.checkout.checkouter.pay;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actualization.services.PostpaidMlDeciderService;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;


public class EnablePostpaidTest extends AbstractWebTestBase {

    @Autowired
    private CheckouterFeatureWriter featureWriter;


    @Test
    public void checkPostpaidFiltering() {
        Parameters parameters = BlueParametersProvider.postpaidBlueOrderParameters();
        trustMockConfigurer.mockWholeTrust();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertTrue(order.isFulfilment());
        Assertions.assertEquals(PaymentType.PREPAID, order.getPaymentType());

        featureWriter.writeValue(BooleanFeatureType.ENABLE_ML_POSTPAID_INCLUDE, true);
        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);
        Assertions.assertTrue(cart.getPaymentOptions().stream()
                .anyMatch(pm -> pm.getPaymentType() == PaymentType.POSTPAID), "Should have postpaid");
        Assertions.assertTrue(
                cart.getCarts().stream()
                        .flatMap(c -> c.getDeliveryOptions().stream())
                        .map(Delivery::getPaymentOptions)
                        .anyMatch(d -> d.contains(PaymentMethod.CARD_ON_DELIVERY) &&
                                d.contains(PaymentMethod.CASH_ON_DELIVERY))
        );
    }


    @TestConfiguration
    public static class Configuration {

        @Bean
        @Primary
        public PostpaidMlDeciderService postpaidMlDeciderServiceSpy(PostpaidMlDeciderService postpaidMlDeciderService) {
            var spyService = Mockito.spy(postpaidMlDeciderService);
            Mockito.doReturn(true)
                    .when(spyService).makeOrderDecision(Mockito.any(), Mockito.any(), Mockito.any());
            return spyService;
        }

    }
}
