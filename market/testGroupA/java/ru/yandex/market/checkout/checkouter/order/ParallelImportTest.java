package ru.yandex.market.checkout.checkouter.order;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.order.parallelimport.ParallelImportWarrantyAction.CHARGE_BACK;

/**
 * Тесты товаров, ввезенных параллельным импортом.
 *
 * @author gelvy
 * Created on: 23.06.2022
 **/
public class ParallelImportTest extends AbstractWebTestBase {

    @Autowired
    private OrderGetHelper orderGetHelper;

    @Autowired
    private OrderHistoryEventsTestHelper orderEventsHelper;

    @Test
    public void shouldNotReturnParallelImportFieldsOnCart() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getOrder().getItems().forEach(item -> {
            item.setParallelImport(true);
            item.setSellerWarrantyPeriod("P1Y2M10DT2H30M");
            item.setParallelImportWarrantyAction(CHARGE_BACK);
        });

        orderCreateHelper.cart(parameters);

        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].items[*].parallelImport").doesNotExist())
                .andExpect(jsonPath("$.carts[*].items[*].sellerWarrantyPeriod").doesNotExist())
                .andExpect(jsonPath("$.carts[*].items[*].parallelImportWarrantyAction").doesNotExist());
    }

    @Test
    public void shouldNotReturnParallelImportFieldsOnCheckout() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getOrder().getItems().forEach(item -> {
            item.setParallelImport(true);
            item.setSellerWarrantyPeriod("P1Y2M10DT2H30M");
            item.setParallelImportWarrantyAction(CHARGE_BACK);
        });

        orderCreateHelper.createOrder(parameters);

        parameters.checkoutResultActions()
                .andExpect(jsonPath("$.carts[*].items[*].parallelImport").doesNotExist())
                .andExpect(jsonPath("$.carts[*].items[*].sellerWarrantyPeriod").doesNotExist())
                .andExpect(jsonPath("$.carts[*].items[*].parallelImportWarrantyAction").doesNotExist());
    }

    @Test
    public void shouldReturnParallelImportFieldsOnGetOrder() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getOrder().getItems().forEach(item -> {
            item.setParallelImport(true);
            item.setSellerWarrantyPeriod("P1Y2M10DT2H30M");
            item.setParallelImportWarrantyAction(CHARGE_BACK);
        });


        Order reservedOrder = orderCreateHelper.createOrder(parameters);
        Order order = orderGetHelper.getOrder(reservedOrder.getId(), ClientInfo.SYSTEM);

        Collection<OrderItem> items = order.getItems();
        assertThat(items.size(), greaterThan(0));
        assertThat(items, everyItem(hasProperty("parallelImport", is(true))));
        assertThat(items, everyItem(hasProperty("sellerWarrantyPeriod", is("P1Y2M10DT2H30M"))));
        assertThat(items, everyItem(hasProperty("parallelImportWarrantyAction", is(CHARGE_BACK))));
    }

    @Test
    public void shouldReturnParallelImportFieldsOnGetOrderWhenItIsUsualOrder() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order reservedOrder = orderCreateHelper.createOrder(parameters);
        Order order = orderGetHelper.getOrder(reservedOrder.getId(), ClientInfo.SYSTEM);

        Collection<OrderItem> items = order.getItems();
        assertThat(items.size(), greaterThan(0));
        assertThat(items, everyItem(hasProperty("parallelImport", is(false))));
        assertThat(items, everyItem(hasProperty("sellerWarrantyPeriod", nullValue())));
        assertThat(items, everyItem(hasProperty("parallelImportWarrantyAction", nullValue())));
    }

    @Test
    public void shouldReturnParallelImportFieldsInEvent() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getOrder().getItems().forEach(item -> {
            item.setParallelImport(true);
            item.setSellerWarrantyPeriod("P1Y2M10DT2H30M");
            item.setParallelImportWarrantyAction(CHARGE_BACK);
        });


        Order reservedOrder = orderCreateHelper.createOrder(parameters);
        List<OrderHistoryEvent> events = orderEventsHelper.getEventsOfType(reservedOrder.getId(),
                HistoryEventType.NEW_ORDER);

        assertThat(events.size(), is(1));
        OrderHistoryEvent event = events.get(0);
        Collection<OrderItem> items = event.getOrderAfter().getItems();

        assertThat(items.size(), greaterThan(0));
        assertThat(items, everyItem(hasProperty("parallelImport", is(true))));
        assertThat(items, everyItem(hasProperty("sellerWarrantyPeriod", is("P1Y2M10DT2H30M"))));
        assertThat(items, everyItem(hasProperty("parallelImportWarrantyAction", is(CHARGE_BACK))));
    }
}
