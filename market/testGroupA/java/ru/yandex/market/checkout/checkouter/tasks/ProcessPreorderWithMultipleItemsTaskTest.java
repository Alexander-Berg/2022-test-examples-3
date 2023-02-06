package ru.yandex.market.checkout.checkouter.tasks;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.FFDeliveryProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.AWAIT_CONFIRMATION;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PREORDER;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.PICKUP_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;

public class ProcessPreorderWithMultipleItemsTaskTest extends AbstractWebTestBase {

    private static final String FIRST_OFFER = "first offer";
    private static final String SECOND_OFFER = "second offer";
    private static final Integer SUPPLIER = 774;

    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    @BeforeEach
    void configure() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_PREORDER_MULTIPLE_ITEMS_SUPPORT, true);
    }

    @Test
    public void shouldProcessPreorderTaskWithMultipleItems() {
        Order preordered = createPreorder();

        Set<SSItemAmount> itemAmounts = preordered.getItems().stream()
                .map(item -> SSItemAmount.of(
                        SSItem.of(item.getShopSku(), item.getSupplierId(), item.getWarehouseId()), 10))
                .collect(Collectors.toUnmodifiableSet());

        stockStorageConfigurer.mockGetAvailableCount(
                itemAmounts.stream()
                        .map(SSItemAmount::getItem)
                        .collect(Collectors.toUnmodifiableList()), false,
                new ArrayList<>(itemAmounts));

        stockStorageConfigurer.mockOkForUnfreezePreorder();
        stockStorageConfigurer.mockOkForRefreeze();

        tmsTaskHelper.runProcessPreorderTaskV2();

        Order changed = orderService.getOrder(preordered.getId());
        assertThat(changed.getStatus(), is(PENDING));
        assertThat(changed.getSubstatus(), is(AWAIT_CONFIRMATION));

        List<ServeEvent> serveEvents = stockStorageConfigurer.getServeEvents();

        long orderId = changed.getId();

        ServeEvent checkStocksEvent = serveEvents.stream()
                .filter(se -> ("/order/getAvailableAmounts").equals(se.getRequest().getUrl()))
                .findAny()
                .orElse(null);
        assertThat(checkStocksEvent, notNullValue());

        ServeEvent unfreezeEvent = serveEvents.stream()
                .filter(se -> ("/preorder/" + orderId + "?cancel=false").equals(se.getRequest().getUrl())
                        && RequestMethod.DELETE.equals(se.getRequest().getMethod()))
                .findAny()
                .orElse(null);
        assertThat(unfreezeEvent, notNullValue());

        ServeEvent freezeEvent = serveEvents.stream()
                .filter(se -> "/order".equals(se.getRequest().getUrl())
                        && RequestMethod.POST.equals(se.getRequest().getMethod()))
                .findAny()
                .orElse(null);
        assertThat(freezeEvent, notNullValue());
    }

    @Test
    public void shouldWaitStocksForAllItemsInPreorder() {
        Order preordered = createPreorder();

        SSItemAmount firstAmmount = SSItemAmount.of(
                SSItem.of(FIRST_OFFER, SUPPLIER, MOCK_SORTING_CENTER_HARDCODED.intValue()), 1);

        SSItemAmount secondAmmount = SSItemAmount.of(
                SSItem.of(SECOND_OFFER, SUPPLIER, MOCK_SORTING_CENTER_HARDCODED.intValue()), 1);

        stockStorageConfigurer.mockGetAvailableCount(
                List.of(firstAmmount.getItem(), secondAmmount.getItem()),
                false,
                List.of(firstAmmount));

        stockStorageConfigurer.mockOkForUnfreezePreorder();
        stockStorageConfigurer.mockOkForRefreeze();

        tmsTaskHelper.runProcessPreorderTaskV2();

        //should not update
        Order changed = orderService.getOrder(preordered.getId());
        assertThat(changed.getStatus(), is(PENDING));
        assertThat(changed.getSubstatus(), is(PREORDER));

        //when all ready - update
        stockStorageConfigurer.mockGetAvailableCount(
                List.of(firstAmmount.getItem(), secondAmmount.getItem()),
                false,
                List.of(firstAmmount, secondAmmount));
        stockStorageConfigurer.mockOkForUnfreezePreorder();
        stockStorageConfigurer.mockOkForRefreeze();

        tmsTaskHelper.runProcessPreorderTaskV2();

        changed = orderService.getOrder(preordered.getId());
        assertThat(changed.getStatus(), is(PENDING));
        assertThat(changed.getSubstatus(), is(AWAIT_CONFIRMATION));

        List<ServeEvent> serveEvents = stockStorageConfigurer.getServeEvents();

        long orderId = changed.getId();

        ServeEvent checkStocksEvent = serveEvents.stream()
                .filter(se -> ("/order/getAvailableAmounts").equals(se.getRequest().getUrl()))
                .findAny()
                .orElse(null);
        assertThat(checkStocksEvent, notNullValue());

        ServeEvent unfreezeEvent = serveEvents.stream()
                .filter(se -> ("/preorder/" + orderId + "?cancel=false").equals(se.getRequest().getUrl())
                        && RequestMethod.DELETE.equals(se.getRequest().getMethod()))
                .findAny()
                .orElse(null);
        assertThat(unfreezeEvent, notNullValue());

        ServeEvent freezeEvent = serveEvents.stream()
                .filter(se -> "/order".equals(se.getRequest().getUrl())
                        && RequestMethod.POST.equals(se.getRequest().getMethod()))
                .findAny()
                .orElse(null);
        assertThat(freezeEvent, notNullValue());
    }

    private Order createPreorder() {
        Parameters parameters = createPreorderParameters(OrderProvider.orderBuilder()
                .configure(OrderProvider::applyDefaults)
                .itemBuilder(OrderItemProvider.orderItemBuilder()
                        .configure(OrderItemProvider::applyDefaults)
                        .offer(FIRST_OFFER)
                        .shopSku(FIRST_OFFER)
                        .supplierId(SUPPLIER)
                        .warehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue())
                        .preorder(true))
                .itemBuilder(OrderItemProvider.orderItemBuilder()
                        .configure(OrderItemProvider::applyDefaults)
                        .offer(SECOND_OFFER)
                        .shopSku(SECOND_OFFER)
                        .supplierId(SUPPLIER)
                        .warehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue())
                        .preorder(true))
                .build());

        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.isFulfilment(), is(true));
        assertThat(order.isPreorder(), is(true));
        orderStatusHelper.proceedOrderToStatus(order, PENDING);

        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), is(PENDING));
        assertThat(order.getSubstatus(), is(PREORDER));
        return order;
    }

    private Parameters createPreorderParameters(Order orderRequest) {
        Parameters parameters = new Parameters(orderRequest.getBuyer(), orderRequest);
        parameters.getReportParameters().setDeliveryVat(VatType.VAT_20);
        parameters.setColor(Color.BLUE);
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.addShopMetaData(parameters.getOrder().getShopId(),
                ShopSettingsHelper.createCustomNewPrepayMeta(parameters.getOrder().getShopId().intValue()));
        FFDeliveryProvider.setFFDeliveryParameters(parameters);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(PICKUP_SERVICE_ID, 2, Collections.singletonList(12312303L))
                        .addDelivery(DELIVERY_SERVICE_ID)
                        .addPost(7)
                        .build()
        );
        parameters.getBuiltMultiCart().getCarts().forEach(cart -> {
            cart.setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        });
        parameters.setEmptyPushApiDeliveryResponse();
        parameters.setWeight(BigDecimal.ONE);
        parameters.setDimensions("10", "10", "10");
        parameters.setDeliveryServiceId(100501L);
        return parameters;
    }
}
