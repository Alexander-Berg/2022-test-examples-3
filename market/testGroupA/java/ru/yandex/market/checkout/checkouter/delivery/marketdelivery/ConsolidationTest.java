package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.common.report.model.SupplierProcessing;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;

public class ConsolidationTest extends AbstractWebTestBase {

    @Test
    public void shouldCreateConsolidatedOrderWithDefaultParcelItemDateValues() {
        Parameters parameters = defaultBlueOrderParameters();
        fillOrderWithTwoItemsFromWarehouses(parameters, 12345, MOCK_SORTING_CENTER_HARDCODED.intValue());

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        assertThat(multiOrder.getOrders(), hasSize(1));
        assertTrue(multiOrder.getOrders().get(0).isFulfilment());
        Parcel parcel = multiOrder.getOrders().get(0).getDelivery().getParcels().get(0);
        checkParcelItemDatesHaveDefaultValues(parcel);
    }

    @Test
    public void shouldCreateFFOrderWithSameFFWarehouseId() {
        Parameters parameters = defaultBlueOrderParameters();
        fillOrderWithTwoItemsFromWarehouses(parameters, 12345, MOCK_SORTING_CENTER_HARDCODED.intValue());
        parameters.getOrder().getItems()
                .forEach(oi -> parameters.getReportParameters()
                        .overrideItemInfo(oi.getFeedOfferId()).getFulfilment().fulfilment = true);
        orderCreateHelper.cart(parameters);
    }

    @Test
    public void shouldNotCreateConsolidatedOrderWhenNotFulfilment() {
        Parameters parameters = defaultBlueOrderParameters();
        fillOrderWithTwoItemsFromWarehouses(parameters, 12345, MOCK_SORTING_CENTER_HARDCODED.intValue());
        parameters.getOrder().getItems()
                .forEach(oi -> parameters.getReportParameters()
                        .overrideItemInfo(oi.getFeedOfferId()).getFulfilment().fulfilment = false);

        parameters.setCheckCartErrors(false);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart.getValidationErrors(), contains(hasProperty("code",
                is("DIFFERENT_WAREHOUSES_ERROR"))));
    }

    @Test
    public void shouldCreateMultiOrderWithDefaultParcelItemDateValues() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        fillOrderWithTwoItemsFromWarehouses(parameters, MOCK_SORTING_CENTER_HARDCODED.intValue(),
                MOCK_SORTING_CENTER_HARDCODED.intValue());

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);

        assertThat(multiOrder.getCarts(), hasSize(1));
        Parcel parcel = multiOrder.getCarts().get(0).getDelivery().getParcels().get(0);
        checkParcelItemDatesHaveDefaultValues(parcel);
    }

    @Test
    public void shouldCreateConsolidatedOrderWithCorrectParcelItemDateValues() {
        Parameters parameters = defaultBlueOrderParameters();
        fillOrderWithTwoItemsFromWarehouses(parameters, 12345, MOCK_SORTING_CENTER_HARDCODED.intValue());

        List<SupplierProcessing> supplierProcessings = mockReportWithSupplierProcessings(parameters);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        assertThat(multiOrder.getCarts(), hasSize(1));

        List<ParcelItem> parcelItems = multiOrder.getCarts().get(0).getDelivery().getParcels().get(0).getParcelItems();
        assertThat(parcelItems, hasSize(2));

        Map<Long, Integer> itemsToWarehouses = multiOrder.getCarts().get(0).getItems().stream()
                .collect(toMap(OrderItem::getId, OrderItem::getWarehouseId));
        assertThat(itemsToWarehouses.entrySet(), hasSize(2));

        parcelItems.forEach(i -> checkCorretParcelItemDateValues(i, itemsToWarehouses, supplierProcessings));
    }

    @Nonnull
    private List<SupplierProcessing> mockReportWithSupplierProcessings(Parameters parameters) {
        SupplierProcessing firstSupplierProcessing = new SupplierProcessing(12345,
                Instant.parse("2020-02-01T12:00:00Z"), Instant.parse("2020-02-01T23:00:00Z"), null, null);
        SupplierProcessing secondSupplierProcessing = new SupplierProcessing(MOCK_SORTING_CENTER_HARDCODED.intValue(),
                Instant.parse("2020-02-02T12:00:00Z"), Instant.parse("2020-02-02T23:00:00Z"), null, null);
        List<SupplierProcessing> supplierProcessings = List.of(firstSupplierProcessing, secondSupplierProcessing);
        parameters.getReportParameters().getActualDelivery().getResults().get(0).getDelivery()
                .forEach(d -> d.setSupplierProcessings(supplierProcessings));
        return supplierProcessings;
    }

    private void checkCorretParcelItemDateValues(
            ParcelItem parcelItem, Map<Long, Integer> itemsToWarehouses, List<SupplierProcessing> supplierProcessings) {
        Integer warehouseId = itemsToWarehouses.get(parcelItem.getItemId());
        SupplierProcessing supplierProcessing = supplierProcessings.stream()
                .filter(sp -> warehouseId.equals(sp.getWarehouseId())).findFirst().get();
        assertThat(parcelItem.getSupplierStartDateTime(), equalTo(supplierProcessing.getStartDateTime()));
        assertThat(parcelItem.getSupplierShipmentDateTime(), equalTo(supplierProcessing.getShipmentDateTime()));
    }

    private void checkParcelItemDatesHaveDefaultValues(Parcel parcel) {
        Instant expectedSupplierShipmentDateTime =
                parcel.getShipmentDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        assertThat(
                parcel.getParcelItems().stream().map(pi -> pi.getSupplierStartDateTime()).collect(Collectors.toList()),
                everyItem(Matchers.equalTo(expectedSupplierShipmentDateTime))
        );
        assertThat(
                parcel.getParcelItems().stream().map(pi -> pi.getSupplierShipmentDateTime())
                        .collect(Collectors.toList()),
                everyItem(Matchers.equalTo(expectedSupplierShipmentDateTime))
        );
    }

    private void fillOrderWithTwoItemsFromWarehouses(Parameters parameters, int firstWarehouse, int secondWarehouse) {
        parameters.getOrder().getItems().clear();

        OrderItem item1 = OrderItemProvider.buildOrderItem("item-1", new BigDecimal("111.00"), 1);
        item1.setMsku(332L);
        item1.setShopSku("sku-1");
        item1.setSku("332");
        item1.setSupplierId(123L);
        item1.setWareMd5(OrderItemProvider.OTHER_WARE_MD5);
        item1.setShowInfo(OrderItemProvider.OTHER_SHOW_INFO);
        item1.setWarehouseId(firstWarehouse);
        item1.setFulfilmentWarehouseId(MOCK_SORTING_CENTER_HARDCODED);
        parameters.addShopMetaData(123L, ShopSettingsHelper.createCustomNewPrepayMeta(123));

        parameters.getOrder().addItem(item1);

        OrderItem item2 = OrderItemProvider.buildOrderItem("item-2", new BigDecimal("777.00"), 1);
        item2.setMsku(334L);
        item2.setShopSku("sku-2");
        item2.setSku("334");
        item2.setSupplierId(456L);
        item2.setWareMd5(OrderItemProvider.ANOTHER_WARE_MD5);
        item2.setShowInfo(OrderItemProvider.ANOTHER_SHOW_INFO);
        item2.setWarehouseId(secondWarehouse);
        item2.setFulfilmentWarehouseId(MOCK_SORTING_CENTER_HARDCODED);
        parameters.addShopMetaData(456L, ShopSettingsHelper.createCustomNewPrepayMeta(456));

        parameters.getOrder().addItem(item2);
    }
}
