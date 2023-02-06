package ru.yandex.market.checkout.checkouter.order;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;

public class VatConversionTest extends AbstractWebTestBase {

    @Test
    public void vat18To20Conversion() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getItems().forEach(i -> i.setVat(VatType.VAT_18));

        Order order = orderCreateHelper.createOrder(parameters);

        order = orderService.getOrder(order.getId());
        assertThat(
                order.getItems(),
                everyItem(hasProperty("vat", equalTo(VatType.VAT_20_120)))
        );
    }

    @Test
    public void vat18To20ConversionWithPushApiInfluence() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getItems().forEach(i -> i.setVat(VatType.VAT_20));

        forcePushApiToReturnVat18(parameters);
        parameters.setMockPushApi(false);
        Order order = orderCreateHelper.createOrder(parameters);

        order = orderService.getOrder(order.getId());
        assertThat(
                order.getItems(),
                everyItem(hasProperty("vat", equalTo(VatType.VAT_20_120)))
        );
    }

    private void forcePushApiToReturnVat18(Parameters parameters) {
        pushApiConfigurer.mockCart(
                parameters.getOrder().getItems().stream()
                        .map(OrderItem::new)
                        .peek(i -> i.setVat(VatType.VAT_18))
                        .collect(Collectors.toList()),
                OrderProvider.SHOP_ID,
                parameters.getPushApiDeliveryResponses(),
                parameters.getOrder().getAcceptMethod(),
                false
        );

        pushApiConfigurer.mockAccept(parameters.getOrder());
    }
}
