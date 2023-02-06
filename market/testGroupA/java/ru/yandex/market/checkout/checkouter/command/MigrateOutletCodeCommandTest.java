package ru.yandex.market.checkout.checkouter.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.MatcherAssert.assertThat;

public class MigrateOutletCodeCommandTest extends AbstractWebTestBase {

    @Autowired
    private MigrateOutletCodeCommand migrateOutletCodeCommand;


    @Test
    public void shouldMigrateOutletCode() throws UnsupportedEncodingException {
        Parameters parameters =
                BlueParametersProvider.clickAndCollectOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setColor(Color.WHITE);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.setDeliveryServiceId(Delivery.SELF_DELIVERY_SERVICE_ID);

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getDelivery().getType(), CoreMatchers.is(DeliveryType.PICKUP));
        assertThat(order.getDelivery().getDeliveryServiceId(),
                CoreMatchers.equalTo(Delivery.SELF_DELIVERY_SERVICE_ID));

        assertThat(order.getDelivery().getOutletId(), CoreMatchers.equalTo(DeliveryProvider.MARKET_OUTLET_ID));

        transactionTemplate.execute(tc -> {
            masterJdbcTemplate.update("update order_delivery set outlet_code = null where order_id = ?", order.getId());
            return null;
        });

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        migrateOutletCodeCommand.executeCommand(new CommandInvocation(
                        "migrate-outlet-code",
                        new String[]{
                                String.valueOf(order.getId()),
                                String.valueOf(order.getId())
                        },
                        Collections.emptyMap()),
                new TestTerminal(new ByteArrayInputStream(new byte[0]), output)
        );

        assertThat(output.toString(StandardCharsets.UTF_8.name()),
                CoreMatchers.equalTo("Migrated 2 entries" + System.lineSeparator()));

        Order updated = orderService.getOrder(order.getId());

        assertThat(updated.getDelivery().getOutletCode(),
                CoreMatchers.equalTo(DeliveryProvider.MARKET_OUTLET_ID + ""));

    }

}
