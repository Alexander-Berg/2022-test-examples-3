package ru.yandex.market.checkout.checkouter.actualization.multicart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.configuration.MockConfiguration;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.FIRST_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SECOND_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.THIRD_OFFER;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class StockWhiteTest extends AbstractWebTestBase {

    private static final long SHOP_ID = 774L;
    private static final int WAREHOUSE_ID = 100500;

    protected final List<FoundOffer> reportOffers = new ArrayList<>();

    protected OrderItemProvider.OrderItemBuilder firstOffer =
            OrderItemProvider.orderItemBuilder()
                    .configure(OrderItemProvider::applyDefaults)
                    .weight(null)
                    .supplierId(SHOP_ID)
                    .offerId(FIRST_OFFER)
                    .price(1000)
                    .atSupplierWarehouse(true)
                    .warehouseId(WAREHOUSE_ID)
                    .shopSku(null)
                    .count(5);
    protected OrderItemProvider.OrderItemBuilder secondOffer =
            OrderItemProvider.orderItemBuilder()
                    .configure(OrderItemProvider::applyDefaults)
                    .weight(null)
                    .supplierId(SHOP_ID)
                    .offerId(SECOND_OFFER)
                    .price(2000)
                    .atSupplierWarehouse(true)
                    .warehouseId(WAREHOUSE_ID)
                    .shopSku(null)
                    .count(3);
    protected OrderItemProvider.OrderItemBuilder thirdOffer =
            OrderItemProvider.orderItemBuilder()
                    .configure(OrderItemProvider::applyDefaults)
                    .weight(null)
                    .supplierId(SHOP_ID)
                    .offerId(THIRD_OFFER)
                    .price(3000)
                    .atSupplierWarehouse(true)
                    .warehouseId(WAREHOUSE_ID)
                    .shopSku(null)
                    .count(1);

    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    @Test
    public void cartChange() {
        Parameters parameters = buildParameters(
                List.of(firstOffer, secondOffer),
                List.of(similar(firstOffer).count(5),
                        similar(secondOffer).count(2),
                        similar(thirdOffer).count(2))
        );
        mockStockStorage(List.of(FIRST_OFFER, SECOND_OFFER, THIRD_OFFER), 10, 1, 0);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiCart), hasProperty("changes", nullValue()));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", nullValue()));
        assertThat(firstOrder(multiCart).getItems(), everyItem(allOf(
                hasProperty("bundleId", nullValue()),
                hasProperty("promos", empty())
        )));
        Map<String, OrderItem> orderItemMap = firstOrder(multiCart).getItems().stream()
                .collect(toMap(OfferItem::getOfferId, Function.identity()));
        assertThat(orderItemMap.get(FIRST_OFFER), hasProperty("changes", nullValue()));
        assertThat(orderItemMap.get(SECOND_OFFER), hasProperty("changes", hasItem(ItemChange.COUNT)));
        assertThat(orderItemMap.get(THIRD_OFFER), hasProperty("changes", hasItem(ItemChange.MISSING)));
        assertThat(multiCart.getCostLimitInformation().getErrors(), empty());
        List<ServeEvent> events = stockStorageConfigurer.getServeEvents();
        assertThat(events, hasSize(1));
        assertThat(events.stream()
                .filter(req -> req.getRequest().getUrl().equals("/order/getAvailableAmounts"))
                .count(), equalTo(1L));
    }

    @Test
    public void cartOk() {
        Parameters parameters = buildParameters(List.of(firstOffer, secondOffer),
                List.of(similar(firstOffer).count(2), similar(secondOffer).count(1))
        );
        mockStockStorage(List.of(FIRST_OFFER, SECOND_OFFER), 10, 10);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiCart), hasProperty("changes", nullValue()));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", nullValue()));
        assertThat(firstOrder(multiCart).getItems(), everyItem(allOf(
                hasProperty("bundleId", nullValue()),
                hasProperty("changes", nullValue()),
                hasProperty("promos", empty())
        )));
        assertThat(multiCart.getCostLimitInformation().getErrors(), empty());
        List<ServeEvent> events = stockStorageConfigurer.getServeEvents();
        assertThat(events, hasSize(1));
        assertThat(events.stream().filter(req -> req.getRequest().getUrl().equals("/order/getAvailableAmounts"))
                .count(), equalTo(1L));
    }

    @Test
    public void createOrder() {
        Parameters parameters = buildParameters(List.of(firstOffer, secondOffer),
                List.of(similar(firstOffer).count(2), similar(secondOffer).count(1))
        );
        mockStockStorage(List.of(FIRST_OFFER, SECOND_OFFER), 10, 10);

        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getRgb(), Is.is(Color.WHITE));
        assertThat(order.isFulfilment(), Is.is(false));
        assertThat(order.getAcceptMethod(), Is.is(OrderAcceptMethod.WEB_INTERFACE));
        assertThat(order.getItems(), hasSize(2));
        assertThat(order.getDelivery().getDeliveryPartnerType(), Is.is(DeliveryPartnerType.SHOP));
        assertThat(order.getDelivery().getDeliveryServiceId(), Is.is(99L));
        List<ServeEvent> events = stockStorageConfigurer.getServeEvents();
        assertThat(events, hasSize(3));
        assertThat(events.stream()
                .filter(req -> req.getRequest().getUrl().equals("/order/getAvailableAmounts"))
                .count(), equalTo(2L));
        assertThat(events.stream()
                .filter(req -> req.getRequest().getUrl().equals("/order"))
                .count(), equalTo(1L));

    }

    private static Parameters buildParameters(List<OrderItemProvider.OrderItemBuilder> reportOffers,
                                              List<OrderItemProvider.OrderItemBuilder> orderOffers) {
        List<OrderItem> items = orderOffers.stream()
                .map(OrderItemProvider.OrderItemBuilder::build)
                .collect(toList());
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getOrder().setItems(items);

        parameters.getReportParameters().setOffers(reportOffers.stream()
                .map(OrderItemProvider.OrderItemBuilder::build)
                .map(FoundOfferBuilder::createFrom)
                .map(b -> b.color(ru.yandex.market.common.report.model.Color.WHITE)
                        .isFulfillment(false)
                        .supplierType(SupplierType.THIRD_PARTY)
                        .deliveryPartnerType(DeliveryPartnerType.SHOP.name())
                        .atSupplierWarehouse(true))
                .map(FoundOfferBuilder::build)
                .collect(toList()));

        parameters.turnOffErrorChecks();
        parameters.setUseErrorMatcher(false);
        parameters.setShouldMockStockStorageGetAmountResponse(false);
        parameters.setShopId(SHOP_ID);
        parameters.addShopMetaData(
                SHOP_ID,
                ShopSettingsHelper.getDefaultMeta()
        );
        parameters.setApiSettings(ApiSettings.STUB);
        parameters.getReportParameters().setIgnoreStocks(false);
        parameters.setStockStorageMockType(MockConfiguration.StockStorageMockType.NO);
        return parameters;
    }

    private void mockStockStorage(List<String> offers, int... count) {
        List<SSItem> ssItems = offers.stream().map(offer -> SSItem.of(offer, SHOP_ID, WAREHOUSE_ID)).collect(toList());
        List<SSItemAmount> ssItemAmount = IntStream.range(0, ssItems.size())
                .mapToObj(i -> SSItemAmount.of(ssItems.get(i), count[i]))
                .collect(Collectors.toList());
        stockStorageConfigurer.resetMappings();
        stockStorageConfigurer.mockGetAvailableCount(ssItems, false, ssItemAmount);
        stockStorageConfigurer.mockOkForFreeze(ssItems);
    }

}
