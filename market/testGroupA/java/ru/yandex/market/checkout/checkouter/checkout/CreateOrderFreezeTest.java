package ru.yandex.market.checkout.checkouter.checkout;

import java.text.ParseException;
import java.util.List;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.configuration.MockConfiguration;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class CreateOrderFreezeTest extends AbstractWebTestBase {

    private static final long DROPSHIP_SC_WAREHOUSE_ID = 100136;
    private static final int WAREHOUSE_ID = 47908;

    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    @Disabled("На самом деле не работает для синего")
    @Test
    public void shouldFreezeStocksWhenCreatingBlueOrder() throws ParseException {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getItems().forEach(oi -> oi.setWarehouseId(null));
        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.isFulfilment(), is(true));

        List<ServeEvent> events = stockStorageConfigurer.getServeEvents();
        assertThat(events, hasSize(3));

        ServeEvent freeze = events.get(0);
        LoggedRequest request = freeze.getRequest();
        assertThat(request.getMethod(), is(RequestMethod.POST));
        assertThat(request.getUrl(), is("/order"));
        String bodyAsString = request.getBodyAsString();
        JsonTest.checkJson(bodyAsString, "$.orderId", String.valueOf(order.getId()));
        JsonTest.checkJson(bodyAsString, "$.items", JsonPathExpectationsHelper::assertValueIsArray);
        JsonTest.checkJsonMatcher(bodyAsString, "$.items", hasSize(1));
        JsonTest.checkJson(bodyAsString, "$.items[0].item.shopSku", FulfilmentProvider.TEST_SHOP_SKU);
        JsonTest.checkJson(bodyAsString, "$.items[0].item.vendorId", String.valueOf(FulfilmentProvider.FF_SHOP_ID));
        JsonTest.checkJson(bodyAsString, "$.items[0].item.warehouseId", "1");
    }

    @Test
    public void shouldFailToCreateOrderIfFreezeWasFailed() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setColor(Color.BLUE);
        parameters.setStockStorageMockType(MockConfiguration.StockStorageMockType.ERROR);
        parameters.setCheckOrderCreateErrors(false);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        OrderFailure orderFailure = Iterables.getOnlyElement(multiOrder.getOrderFailures());

        assertThat(orderFailure.getErrorCode(), is(OrderFailure.Code.UNKNOWN_ERROR));
        assertThat(orderFailure.getErrorDetails(), is("Unable to freeze stock in StockStorage"));

        Long orderId = orderFailure.getOrder().getId();
        Order order = orderService.getOrder(orderId);

        assertThat(order.getStatus(), is(OrderStatus.PLACING));

    }

    @Test
    public void shouldDoubleFreezeIfAtSupplierWarehouse() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getItems()
                .forEach(oi -> {
                    ItemInfo itemInfo = parameters.getReportParameters().overrideItemInfo(oi.getFeedOfferId());
                    itemInfo.getFulfilment().warehouseId = 700;
                    itemInfo.setFulfillmentWarehouseId(172L);
                    itemInfo.setAtSupplierWarehouse(true);
                });
        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.isFulfilment(), is(true));
        assertThat(order.getItems().iterator().next().getWarehouseId(), is(700));

        List<ServeEvent> events = stockStorageConfigurer.getServeEvents();
        assertThat(events, hasSize(3));

        ServeEvent freeze = events.get(0);
        LoggedRequest request = freeze.getRequest();
        assertThat(request.getMethod(), is(RequestMethod.POST));
        assertThat(request.getUrl(), is("/order"));
        String bodyAsString = request.getBodyAsString();
        JsonTest.checkJson(bodyAsString, "$.orderId", String.valueOf(order.getId()));
        JsonTest.checkJson(bodyAsString, "$.items", JsonPathExpectationsHelper::assertValueIsArray);
        JsonTest.checkJsonMatcher(bodyAsString, "$.items", hasSize(2));

        JsonTest.checkJson(bodyAsString, "$.items[0].item.shopSku", FulfilmentProvider.TEST_SHOP_SKU);
        JsonTest.checkJson(bodyAsString, "$.items[0].item.vendorId", String.valueOf(FulfilmentProvider.FF_SHOP_ID));
        JsonTest.checkJson(bodyAsString, "$.items[0].item.warehouseId", "172");
        JsonTest.checkJson(bodyAsString, "$.items[0].backorder", true);

        JsonTest.checkJson(bodyAsString, "$.items[1].item.shopSku", FulfilmentProvider.TEST_SHOP_SKU);
        JsonTest.checkJson(bodyAsString, "$.items[1].item.vendorId", String.valueOf(FulfilmentProvider.FF_SHOP_ID));
        JsonTest.checkJson(bodyAsString, "$.items[1].item.warehouseId", "700");
    }

    @Test
    public void shouldNotDoubleFreezeIfAtSupplierWarehouseButFfWhIdEqWhId() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getItems()
                .forEach(oi -> {
                    ItemInfo itemInfo = parameters.getReportParameters().overrideItemInfo(oi.getFeedOfferId());
                    itemInfo.getFulfilment().warehouseId = 700;
                    itemInfo.setFulfillmentWarehouseId(700L);
                    itemInfo.setAtSupplierWarehouse(true);
                });
        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.isFulfilment(), is(true));
        assertThat(order.getItems().iterator().next().getWarehouseId(), is(700));

        List<ServeEvent> events = stockStorageConfigurer.getServeEvents();
        assertThat(events, hasSize(3));

        ServeEvent freeze = events.get(0);
        LoggedRequest request = freeze.getRequest();
        assertThat(request.getMethod(), is(RequestMethod.POST));
        assertThat(request.getUrl(), is("/order"));
        String bodyAsString = request.getBodyAsString();
        JsonTest.checkJson(bodyAsString, "$.orderId", String.valueOf(order.getId()));
        JsonTest.checkJson(bodyAsString, "$.items", JsonPathExpectationsHelper::assertValueIsArray);
        JsonTest.checkJsonMatcher(bodyAsString, "$.items", hasSize(1));

        JsonTest.checkJson(bodyAsString, "$.items[0].item.shopSku", FulfilmentProvider.TEST_SHOP_SKU);
        JsonTest.checkJson(bodyAsString, "$.items[0].item.vendorId", String.valueOf(FulfilmentProvider.FF_SHOP_ID));
        JsonTest.checkJson(bodyAsString, "$.items[0].item.warehouseId", "700");
    }

    @DisplayName("Не должны фризить с backorder=true для заказа дропшип на СЦ")
    @Test
    public void shouldNotUseBackorderStockOnDropshipViaSortingCenter() throws ParseException {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getReportParameters().setIgnoreStocks(false);

        OrderItem item = parameters.getOrder().getItems().iterator().next();

        ItemInfo itemInfo = parameters.getReportParameters().overrideItemInfo(item.getFeedOfferId());
        itemInfo.getFulfilment().fulfilment = false;
        itemInfo.getFulfilment().warehouseId = WAREHOUSE_ID;
        itemInfo.getFulfilment().supplierId = FulfilmentProvider.FF_SHOP_ID;
        itemInfo.setFulfillmentWarehouseId(DROPSHIP_SC_WAREHOUSE_ID);
        itemInfo.setAtSupplierWarehouse(true);

        Order order = orderCreateHelper.createOrder(parameters);

        List<ServeEvent> events = stockStorageConfigurer.getServeEvents();

        assertThat(events, hasSize(3));

        ServeEvent freeze = events.get(0);
        LoggedRequest request = freeze.getRequest();
        assertThat(request.getMethod(), is(RequestMethod.POST));
        assertThat(request.getUrl(), is("/order"));
        String bodyAsString = request.getBodyAsString();
        JsonTest.checkJson(bodyAsString, "$.orderId", String.valueOf(order.getId()));
        JsonTest.checkJson(bodyAsString, "$.items", JsonPathExpectationsHelper::assertValueIsArray);
        JsonTest.checkJsonMatcher(bodyAsString, "$.items", hasSize(1));

        JsonTest.checkJson(bodyAsString, "$.items[0].item.shopSku", FulfilmentProvider.TEST_SHOP_SKU);
        JsonTest.checkJson(bodyAsString, "$.items[0].item.vendorId", String.valueOf(OrderProvider.SHOP_ID));
        JsonTest.checkJson(bodyAsString, "$.items[0].item.warehouseId", String.valueOf(WAREHOUSE_ID));
    }
}
