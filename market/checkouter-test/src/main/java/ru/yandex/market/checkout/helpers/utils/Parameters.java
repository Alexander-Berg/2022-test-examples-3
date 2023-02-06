package ru.yandex.market.checkout.helpers.utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.order.PresetInfo;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.helpers.utils.configuration.MockConfiguration;
import ru.yandex.market.checkout.helpers.utils.configuration.MockConfiguration.StockStorageMockType;
import ru.yandex.market.checkout.helpers.utils.configuration.ParametersConfiguration;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.ItemServiceProvider;
import ru.yandex.market.checkout.test.providers.ItemServiceProvider.ItemServiceBuilder;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.balance.TrustParameters;
import ru.yandex.market.checkout.util.geocoder.GeocoderParameters;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.report.ItemInfo.Fulfilment;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;

import static ru.yandex.common.util.collections.CollectionUtils.isEmpty;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.fulfilmentize;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.whiteOffer;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.defaultActualDelivery;

/**
 * Проперти для создания заказа.
 */
public class Parameters {

    private final ParametersConfiguration configuration = new ParametersConfiguration();

    /**
     * @deprecated use {@link ru.yandex.market.checkout.providers.BlueParametersProvider}
     * or {@link ru.yandex.market.checkout.providers.WhiteParametersProvider} instead.
     */
    @Deprecated
    public Parameters() {
        init(null);
    }

    /**
     * @deprecated use {@link ru.yandex.market.checkout.providers.BlueParametersProvider}
     * or {@link ru.yandex.market.checkout.providers.WhiteParametersProvider} instead.
     */
    @Deprecated
    public Parameters(int size) {
        List<Order> orders = IntStream.range(0, size)
                .mapToObj(i -> OrderProvider.getBlueOrder())
                .collect(Collectors.toList());
        init(orders);
    }

    /**
     * @deprecated use {@link ru.yandex.market.checkout.providers.BlueParametersProvider}
     * or {@link ru.yandex.market.checkout.providers.WhiteParametersProvider} instead.
     */
    @Deprecated
    public Parameters(Buyer buyer, Order order) {
        configuration.cart().body().setBuyer(buyer);
        init(order != null ? Collections.singletonList(order) : null);
    }

    /**
     * @deprecated use {@link ru.yandex.market.checkout.providers.BlueParametersProvider}
     * or {@link ru.yandex.market.checkout.providers.WhiteParametersProvider} instead.
     */
    @Deprecated
    public Parameters(Order order) {
        this(null, order);
    }

    /**
     * @deprecated use {@link ru.yandex.market.checkout.providers.BlueParametersProvider}
     * or {@link ru.yandex.market.checkout.providers.WhiteParametersProvider} instead.
     */
    @Deprecated
    public Parameters(Buyer buyer) {
        this(buyer, (Order) null);
    }

    /**
     * @deprecated use {@link ru.yandex.market.checkout.providers.BlueParametersProvider}
     * or {@link ru.yandex.market.checkout.providers.WhiteParametersProvider} instead.
     */
    @Deprecated
    public Parameters(MultiCart multiCart) {
        configuration.cart().body().setMultiCart(multiCart);
        init(multiCart.getCarts());
    }

    /**
     * @deprecated use {@link ru.yandex.market.checkout.providers.BlueParametersProvider}
     * or {@link ru.yandex.market.checkout.providers.WhiteParametersProvider} instead.
     */
    @Deprecated
    public Parameters(Buyer buyer, MultiCart multiCart) {
        configuration.cart().body().setBuyer(buyer);
        configuration.cart().body().setMultiCart(multiCart);
        init(multiCart.getCarts());
    }

    private void init(List<Order> orders) {
        if (configuration.cart().body().getBuyer() == null) {
            configuration.cart().body().setBuyer(BuyerProvider.getBuyer());
        }
        if (CollectionUtils.isEmpty(orders)) {
            setColor(Color.BLUE);
            orders = Collections.singletonList(OrderProvider.getBlueOrder());
        } else {
            setColor(orders.get(0).getRgb());
        }
        if (configuration.cart().body().multiCart() == null) {
            configuration.cart().body().setMultiCart(MultiCartProvider.buildMultiCart(orders));
        }

        configuration.cart().body().multiCart().setBuyer(configuration.cart().body().getBuyer());
        orders.forEach(o -> o.setBuyer(configuration.cart().body().getBuyer()));

        for (Order order : orders) {
            var reportParameters = new ReportGeneratorParameters(order, defaultActualDelivery());
            reportParameters.setRegionId(configuration.cart().body().multiCart().getBuyerRegionId());
            configuration.cart().mocks(order.getLabel()).setReportParameters(reportParameters);
            configuration.checkout().mocks(order.getLabel()).setReportParameters(reportParameters);

            if (order.getDelivery() != null &&
                    order.getDelivery().getRegionId() != null &&
                    !Objects.equals(configuration.cart().body().multiCart().getBuyerRegionId(),
                            order.getDelivery().getRegionId())) {
                var deliveryRegionReportParameters = new ReportGeneratorParameters(order, defaultActualDelivery());
                deliveryRegionReportParameters.setRegionId(order.getDelivery().getRegionId());
                configuration.cart().mocks(order.getLabel())
                        .setDeliveryRegionReportParameters(deliveryRegionReportParameters);
                configuration.checkout().mocks(order.getLabel())
                        .setDeliveryRegionReportParameters(deliveryRegionReportParameters);
            }

            //C&C и DBS (АПИ).
            //FBS идет без ответа опций
            addDefaultPushApiDeliveryResponse(order);

            addDefaultReportDeliveryResponse(order);

            boolean preorder = order.isPreorder() || order.getItems().stream().anyMatch(OrderItem::isPreorder);
            configuration.cart().mocks(order.getLabel()).setStockStorageFreezeMockType(preorder ?
                    StockStorageMockType.PREORDER_OK : StockStorageMockType.OK);
            configuration.checkout().mocks(order.getLabel()).setStockStorageFreezeMockType(preorder ?
                    StockStorageMockType.PREORDER_OK : StockStorageMockType.OK);

            configuration.checkout().orderOption(order.getLabel()).setDeliveryType(DeliveryType.DELIVERY);
            configuration.checkout().orderOption(order.getLabel())
                    .setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
            if (configuration.cart().request().getColor() != Color.BLUE) {
                //Синие заказы не ипользуют SELF_DELIVERY_SERVICE_ID
                configuration.checkout().orderOption(order.getLabel())
                        .setDeliveryServiceId(Delivery.SELF_DELIVERY_SERVICE_ID);
            }
        }
    }

    private void addDefaultPushApiDeliveryResponse(Order order) {
        var pushApiDeliveryResponses =
                DeliveryProvider.defaultDeliveryList(OrderAcceptMethod.PUSH_API).stream()
                        .filter(b -> b.getPartnerType() == DeliveryPartnerType.SHOP)
                        .map(b -> b.buildResponse(DeliveryResponse::new))
                        .collect(Collectors.toList());
        configuration.cart().mocks(order.getLabel()).setPushApiDeliveryResponses(pushApiDeliveryResponses);
        configuration.checkout().mocks(order.getLabel()).setPushApiDeliveryResponses(pushApiDeliveryResponses);
    }

    private void addDefaultReportDeliveryResponse(Order order) {
        var actualDeliveryBuilder = ActualDeliveryProvider.builder();
        for (var deliveryBuilder : DeliveryProvider.defaultDeliveryList(OrderAcceptMethod.WEB_INTERFACE)) {
            actualDeliveryBuilder.addOption(deliveryBuilder.getType(),
                    deliveryBuilder.buildLocalDeliveryOption());
        }

        configuration.cart().mocks(order.getLabel()).getReportParameters()
                .setActualDelivery(actualDeliveryBuilder.build());
    }


    public ParametersConfiguration configuration() {
        return configuration;
    }

    /**
     * Можно конфигурировать дефолтовый мультикарт
     */
    public void configureMultiCart(Consumer<MultiCart> multiCartConsumer) {
        multiCartConsumer.accept(configuration.cart().body().multiCart());
    }

    /**
     * Кастомные проверки "сырого" ответа на ручку /cart
     */
    public ResultActionsContainer cartResultActions() {
        return configuration.cart().response().resultActions();
    }

    /**
     * Кастомные проверки "сырого" ответа на ручку /checkout
     */
    public ResultActionsContainer checkoutResultActions() {
        return configuration.checkout().response().resultActions();
    }

    /**
     * Эмулирует работу чекаутера как если бы заказ был принят по АПИ/ПИ
     *
     * @param orderAcceptMethod - разумно выбирать PUSH_API (АПИ) или WEB_INTERFACE (ПИ)
     */
    public void setAcceptMethod(OrderAcceptMethod orderAcceptMethod) {
        getOrder().setAcceptMethod(orderAcceptMethod);
    }

    public PaymentMethod getPaymentMethod() {
        return configuration.cart().body().multiCart().getPaymentMethod();
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        configuration.cart().body().multiCart().setPaymentMethod(paymentMethod);
        if (paymentMethod != null) {
            configuration.cart().body().multiCart().setPaymentType(paymentMethod.getPaymentType());
        }
    }

    // Используйте setPaymentMethod
    @Deprecated
    public void setPaymentType(PaymentType paymentType) {
        configuration.cart().body().multiCart().setPaymentType(paymentType);
    }

    public void setExpectedCartReturnCode(int expectedCartReturnCode) {
        configuration.cart().response().setExpectedCartReturnCode(expectedCartReturnCode);
    }

    public void setExpectedCheckoutReturnCode(int expectedCheckoutReturnCode) {
        configuration.checkout().response().setExpectedCheckoutReturnCode(expectedCheckoutReturnCode);
    }

    /**
     * Кастомные проверки корзины после /cart
     */
    public void setMultiCartChecker(Consumer<MultiCart> multiCartChecker) {
        configuration.cart().response().setMultiCartChecker(multiCartChecker);
    }

    /**
     * Кастомные проверки заказа после /checkout
     */
    public void setMultiOrderChecker(Consumer<MultiOrder> multiOrderChecker) {
        configuration.checkout().response().setMultiOrderChecker(multiOrderChecker);
    }

    public MultiCart getBuiltMultiCart() {
        return configuration.cart().body().multiCart();
    }

    public Buyer getBuyer() {
        return configuration.cart().body().getBuyer();
    }

    public void setBuyer(Buyer buyer) {
        configuration.cart().body().setBuyer(buyer);
        Optional.ofNullable(getBuiltMultiCart()).ifPresent(m -> {
            m.setBuyer(buyer);
            if (m.getCarts() != null) {
                m.getCarts().forEach(c -> c.setBuyer(buyer));
            }
        });
    }

    public Order getOrder() {
        return configuration.cart().body().multiCart().getCarts().get(0);
    }

    public List<Order> getOrders() {
        return configuration.cart().body().multiCart().getCarts();
    }

    public Parameters addOrder(Parameters anotherOrderParameters) {
        Order anotherOrder = anotherOrderParameters.getOrder();
        List<Order> oldOrders = getOrders();
        ArrayList<Order> newOrders = new ArrayList<>(oldOrders);
        newOrders.add(anotherOrder);
        configuration.cart().body().setMultiCart(MultiCartProvider.buildMultiCart(newOrders));

        configuration.cart().addMockConfiguration(
                anotherOrder.getLabel(),
                Iterables.getOnlyElement(anotherOrderParameters.configuration().cart().mockConfigurations().values())
        );
        configuration.checkout().addMockConfiguration(
                anotherOrder.getLabel(),
                Iterables.getOnlyElement(anotherOrderParameters.configuration().checkout().mockConfigurations()
                        .values())
        );
        configuration.checkout().addOrderOptions(
                anotherOrder.getLabel(),
                Iterables.getOnlyElement(anotherOrderParameters.configuration().checkout().orderOptions().values())
        );
        anotherOrderParameters.getShopMetaData().forEach(this::addShopMetaData);
        if (oldOrders.size() == 1) {
            Order firstNewOrder = newOrders.iterator().next();
            String firstNewOrderLabel = firstNewOrder.getLabel();

            replaceNullKeyWithLabel(firstNewOrderLabel, configuration.cart().mockConfigurations());
            replaceNullKeyWithLabel(firstNewOrderLabel, configuration.checkout().mockConfigurations());
            replaceNullKeyWithLabel(firstNewOrderLabel, configuration.checkout().orderOptions());
        }
        return this;
    }

    private <T> void replaceNullKeyWithLabel(String label, Map<String, T> map) {
        if (map.containsKey(null)) {
            map.put(label, map.remove(null));
        }
    }

    public String getPromoCode() {
        return configuration.cart().body().multiCart().getPromoCode();
    }

    public ReportGeneratorParameters getReportParameters() {
        return Iterables.getOnlyElement(configuration.cart().mockConfigurations().values()).getReportParameters();
    }

    public void setDeliveryRegionReportParameters(ReportGeneratorParameters deliveryRegionReportParameters) {
        Iterables.getOnlyElement(configuration.cart().mockConfigurations().values())
                .setDeliveryRegionReportParameters(deliveryRegionReportParameters);
    }

    public void setSandbox(boolean sandbox) {
        configuration.cart().request().setSandbox(sandbox);
    }

    public void setDeliveryType(DeliveryType deliveryType) {
        Iterables.getOnlyElement(configuration.checkout().orderOptions().values()).setDeliveryType(deliveryType);
    }

    public void setDeliveryPartnerType(DeliveryPartnerType deliveryPartnerType) {
        Iterables.getOnlyElement(configuration.checkout().orderOptions().values())
                .setDeliveryPartnerType(deliveryPartnerType);
    }

    public void setDeliveryServiceId(Long deliveryServiceId) {
        Iterables.getOnlyElement(configuration.checkout().orderOptions().values())
                .setDeliveryServiceId(deliveryServiceId);
    }

    public void setOutletId(Long outletId) {
        Iterables.getOnlyElement(configuration.checkout().orderOptions().values()).setOutletId(outletId);
    }

    public void setFreeDelivery(Boolean freeDelivery) {
        Iterables.getOnlyElement(configuration.checkout().orderOptions().values()).setFreeDelivery(freeDelivery);
    }

    public void setPlatform(Platform platform) {
        configuration.cart().request().setPlatform(platform);
    }

    public void setCheckOrderCreateErrors(boolean checkOrderCreateErrors) {
        configuration.checkout().response().setCheckOrderCreateErrors(checkOrderCreateErrors);
    }

    public void setUseErrorMatcher(boolean useErrorMatcher) {
        configuration.checkout().response().setUseErrorMatcher(useErrorMatcher);
    }

    public void setCheckCartErrors(boolean checkCartErrors) {
        configuration.cart().response().setCheckCartErrors(checkCartErrors);
    }

    public List<DeliveryResponse> getPushApiDeliveryResponses() {
        return Iterables.getOnlyElement(configuration.cart().mockConfigurations().values())
                .getPushApiDeliveryResponses();
    }

    public void setPushApiDeliveryResponse(DeliveryResponse... deliveryResponse) {
        Iterables.getOnlyElement(configuration.cart().mockConfigurations().values()).setPushApiDeliveryResponses(
                new ArrayList<>(Arrays.asList(deliveryResponse)));
    }

    public void setEmptyPushApiDeliveryResponse() {
        Iterables.getOnlyElement(configuration.cart().mockConfigurations().values())
                .setPushApiDeliveryResponses(List.of());
    }

    public void setNullPushApiDeliveryResponse() {
        Iterables.getOnlyElement(configuration.cart().mockConfigurations().values()).setPushApiDeliveryResponses(null);
    }

    /**
     * Ожидаемые 4хх и им подобные коды ответа
     */
    public void setErrorMatcher(ResultMatcher errorMatcher) {
        this.setCheckOrderCreateErrors(false);
        this.setCheckCartErrors(false);
        configuration.checkout().response().setErrorMatcher(errorMatcher);
    }

    public Map<Long, ShopMetaData> getShopMetaData() {
        return configuration.cart().multiCartMocks().getShopMetaData();
    }

    public void addShopMetaData(Long shopId, ShopMetaData shopMetaData) {
        configuration.cart().multiCartMocks().addShopMetaData(shopId, shopMetaData);
    }

    /**
     * Кастомное действия с мультикорзиной, после запроса /cart перед /checkout
     */
    public void setMultiCartAction(Consumer<MultiCart> multiCartAction) {
        configuration.checkout().response().setPrepareBeforeCheckoutAction(multiCartAction);
    }

    /**
     * Выполняет все необходимые настройки для создания Глобал-заказа
     */
    public void setupGlobal() {
        if (!isEmpty(configuration.cart().body().multiCart().getCarts())) {
            configuration.cart().body().multiCart().getCarts().forEach(c -> {
                c.setGlobal(true);
                addShopMetaData(c.getShopId(), ShopSettingsHelper.getRedPrepayMeta());
            });
        }
        getReportParameters().setGlobal(true);
    }

    /**
     * Выполняет все необходимые настройки для создания заказа с промоакцией
     */
    @Deprecated
    public void setupPromo(String promoCode) {
        configuration.cart().body().multiCart().setPromoCode(promoCode);
        getReportParameters().setShopSupportsSubsidies(true);
        setMockLoyalty(true);
    }

    /**
     * Выполняет все необходимые настройки для создания заказа с промоакцией
     */
    public void setupPostpaidPromo(String promoCode) {
        configuration.cart().body().multiCart().setPromoCode(promoCode);
        getReportParameters().setShopSupportsSubsidies(true);
        setMockLoyalty(true);
    }

    /**
     * Выполняет все необходимые настройки для создания фф заказа
     */
    @Deprecated
    public void setupFulfillment(Fulfilment fulfilment) {
        Order order = getOrder();
        for (OrderItem item : order.getItems()) {
            getReportParameters().overrideItemInfo(item.getFeedOfferId()).setFulfilment(fulfilment);
        }
        fulfilmentize(order);
    }

    @Deprecated
    public void cleanFulfillmentInfo() {
        Order order = getOrder();
        for (OrderItem item : order.getItems()) {
            getReportParameters().overrideItemInfo(item.getFeedOfferId()).setFulfilment(null);
        }
    }

    public void setupFulfilmentData() {
        getOrder().getItems().forEach(oi -> {
            getReportParameters().overrideItemInfo(oi.getFeedOfferId()).setFulfilment(
                    new Fulfilment(
                            FulfilmentProvider.FF_SHOP_ID,
                            FulfilmentProvider.TEST_SKU,
                            FulfilmentProvider.TEST_SHOP_SKU,
                            null,
                            true
                    )
            );
        });
    }

    public void setMockLoyalty(boolean mockLoyalty) {
        configuration().cart().multiCartMocks().setMockLoyalty(mockLoyalty);
    }

    public void setMockPushApi(boolean mockPushApi) {
        configuration().cart().multiCartMocks().setMockPushApi(mockPushApi);
    }

    public void turnOffErrorChecks() {
        this.setCheckOrderCreateErrors(false);
        this.setCheckCartErrors(false);
    }

    public void setUnfreezeStocksTime(LocalDateTime unfreezeStocksTime) {
        configuration.cart().request().setUnfreezeStocksTime(unfreezeStocksTime);
    }

    public void setHitRateGroup(HitRateGroup hitRateGroup) {
        configuration.cart().request().setHitRateGroup(hitRateGroup);
    }

    public void setWeight(BigDecimal weight) {
        Order order = getOrder();
        order.getItems().forEach(item -> getReportParameters().overrideItemInfo(item.getFeedOfferId())
                .setWeight(weight));
    }

    public void hideWeight(boolean value) {
        Order order = getOrder();
        order.getItems().forEach(item -> getReportParameters().overrideItemInfo(item.getFeedOfferId())
                .setHideWeight(value));
    }

    public void setDimensions(String width, String height, String depth) {
        Order order = getOrder();
        order.getItems().forEach(
                item -> getReportParameters().overrideItemInfo(
                        item.getFeedOfferId()
                ).setDimensions(Arrays.asList(depth, width, height))
        );
    }

    public void hideDimensions(boolean value) {
        Order order = getOrder();
        order.getItems().forEach(item -> getReportParameters().overrideItemInfo(item.getFeedOfferId())
                .setHideDimensions(value));
    }

    public void setSupplierTypeForAllItems(SupplierType type) {
        getOrder().getItems().forEach(
                item -> getReportParameters().overrideItemInfo(
                        item.getFeedOfferId()
                ).setSupplierType(type)
        );
    }

    public void setWarehouseForAllItems(Integer warehouse) {
        getOrder().getItems().forEach(
                item -> {
                    getReportParameters().overrideItemInfo(
                            item.getFeedOfferId()
                    ).getFulfilment().warehouseId = warehouse;
                    item.setWarehouseId(warehouse);
                }
        );
    }

    /**
     * Выставляем vat доставке и всем айтемам в заказах.
     * Для более тонкой настройки можно пользоваться ручными вызовами.
     *
     * @param vat
     */
    public void setVat(VatType vat) {
        getOrders().stream()
                .peek(o -> o.getDelivery().setVat(vat))
                .map(Order::getItems)
                .flatMap(Collection::stream)
                .forEach(i -> i.setVat(vat));
        getReportParameters().setItemVat(vat);
    }

    public void setUserGroup(UserGroup userGroup) {
        configuration.cart().request().setUserGroup(userGroup);
    }

    public void setContext(Context context) {
        configuration.cart().request().setContext(context);
        if (context == Context.SANDBOX) {
            configuration.cart().request().setSandbox(true);
        }
    }

    public void setApiSettings(ApiSettings apiSettings) {
        configuration.cart().request().setApiSettings(apiSettings);
    }

    public void setReserveOnly(boolean reserveOnly) {
        configuration().checkout().request().setReserveOnly(reserveOnly);
    }

    public Long getShopId() {
        return getOrder().getShopId();
    }

    public void setShopId(long shopId) {
        getOrder().setShopId(shopId);
    }

    public void setColor(Color color) {
        configuration.cart().request().setColor(color);
    }

    public void setAcceptOrder(boolean acceptOrder) {
        Iterables.getOnlyElement(configuration.checkout().mockConfigurations().values()).setAcceptOrder(acceptOrder);
    }

    public void setShopAdmin(boolean isShopAdmin) {
        Iterables.getOnlyElement(configuration.cart().mockConfigurations().values()).setShopAdmin(isShopAdmin);
    }

    @Nullable
    public Boolean isShopAdmin() {
        MockConfiguration onlyElement = Iterables.getFirst(
                configuration.cart().mockConfigurations().values(), null);
        return onlyElement == null ? null : onlyElement.isShopAdmin();
    }

    public void setMinifyOutlets(boolean minifyOutlets) {
        configuration.cart().request().setMinifyOutlets(minifyOutlets);
        getReportParameters().setMinifyOutlets(minifyOutlets);
    }

    public void setMetaInfo(String metaInfo) {
        configuration.cart().request().setMetaInfo(metaInfo);
    }

    public void setUserHasPrime(boolean userHasPrime) {
        setPrime(userHasPrime, String.valueOf(Integer.MAX_VALUE));
    }

    public void setPrime(boolean prime, String primeOrderId) {
        configuration.cart().request().setUserHasPrime(prime);
        getReportParameters().setPrime(prime);
        configuration().cart().multiCartMocks().getLoyaltyParameters().setPrimeFreeDelivery(prime ? true : null,
                prime ? primeOrderId : null);
    }

    public void setYandexPlus(boolean yandexPlus) {
        configuration.cart().request().setYandexPlus(yandexPlus);
        getReportParameters().setYandexPlus(yandexPlus);
        configuration().cart().multiCartMocks().getLoyaltyParameters().setYandexPlusFreeDelivery(yandexPlus ? true :
                null);
    }

    public void setYandexEmployee(boolean yandexEmployee) {
        configuration().cart().request().setYandexEmployee(yandexEmployee);
    }

    @Nonnull
    public LoyaltyParameters getLoyaltyParameters() {
        return configuration().cart().multiCartMocks().getLoyaltyParameters();
    }

    @Nonnull
    public TrustParameters getTrustParameters() {
        return configuration().cart().multiCartMocks().getTrustParameters();
    }

    public void setExperiments(String experiments) {
        configuration().cart().request().setExperiments(experiments);
    }

    public void setExperiments(@Nonnull Experiments experiments) {
        configuration().cart().request().setExperiments(experiments.toExperimentString());
    }

    public void addExperiment(@Nonnull String exp, @Nullable String value) {
        Experiments experiments = Experiments.fromString(configuration().cart().request().getExperiments());
        experiments.addExperiment(exp, value);
        setExperiments(experiments);
    }

    public void setTestBuckets(String testBuckets) {
        configuration().cart().request().setTestBuckets(testBuckets);
    }

    @Nonnull
    public GeocoderParameters getGeocoderParameters() {
        return configuration().cart().multiCartMocks().getGeocoderParameters();
    }

    public void setBusinessId(long businessId) {
        getOrder().setBusinessId(businessId);
    }

    public boolean isMultiCart() {
        return getOrders().size() > 1;
    }

    public void setFromDate(Date fromDate) {
        Iterables.getOnlyElement(configuration.checkout().orderOptions().values()).setFromDate(fromDate);
    }

    public void setLeaveAtTheDoor(boolean leaveAtTheDoor) {
        Iterables.getOnlyElement(configuration.checkout().orderOptions().values()).setLeaveAtTheDoor(leaveAtTheDoor);
    }

    public void setForceShipmentDay(Long forceShipmentDay) {
        configuration.cart().request().setForceShipmentDay(forceShipmentDay);
    }

    public void setStockStorageResponse(List<SSItemAmount> stockStorageResponse) {
        configuration.cart().mockConfigurations().forEach((label, configuration) ->
                configuration.setStockStorageResponse(stockStorageResponse));
    }

    public void setStockStorageMockType(StockStorageMockType stockStorageMockType) {
        configuration.cart().mockConfigurations().forEach((label, configuration) ->
                configuration.setStockStorageFreezeMockType(stockStorageMockType));
    }

    public void setMarketRequestId(String marketRequestId) {
        configuration.cart().request().setMarketRequestId(marketRequestId);
    }

    public OrderItem addItem(FeedOfferId feedOfferId, int count, BigDecimal quantity, BigDecimal price) {
        OrderItem newItem = OrderItemProvider.buildOrderItem(feedOfferId.getId(), price, count);
        newItem.setQuantity(quantity);
        newItem.setMsku(332L);
        newItem.setShopSku("sku-2");
        newItem.setSku("332");
        newItem.setSupplierId(667L);
        newItem.setWareMd5(OrderItemProvider.ANOTHER_WARE_MD5);
        newItem.setShowInfo(OrderItemProvider.ANOTHER_SHOW_INFO);
        this.getOrder().addItem(newItem);
        return newItem;
    }

    public void addAnotherItem() {
        addAnotherItem(1);
    }

    public void addAnotherItem(int count) {
        OrderItem newItem = OrderItemProvider.buildOrderItem("item-2", new BigDecimal("123.00"), count);
        newItem.setMsku(332L);
        newItem.setShopSku("sku-2");
        newItem.setSku("332");
        newItem.setSupplierId(667L);
        newItem.setWareMd5(OrderItemProvider.ANOTHER_WARE_MD5);
        newItem.setShowInfo(OrderItemProvider.ANOTHER_SHOW_INFO);
        this.getOrder().addItem(newItem);
    }

    public void addOtherItem() {
        addOtherItem(1);
    }

    public void addOtherItem(int count) {
        OrderItem newItem = OrderItemProvider.buildOrderItem("item-3", new BigDecimal("777.00"), count);
        newItem.setMsku(332L);
        newItem.setShopSku("sku-3");
        newItem.setSku("332");
        newItem.setSupplierId(667L);
        newItem.setWareMd5(OrderItemProvider.OTHER_WARE_MD5);
        newItem.setShowInfo(OrderItemProvider.OTHER_SHOW_INFO);
        this.getOrder().addItem(newItem);
    }

    public Collection<OrderItem> getItems() {
        return this.getOrder().getItems();
    }

    public ItemService addItemService() {
        return addItemService(Function.identity());
    }

    public ItemService addItemService(Function<ItemServiceBuilder, ItemServiceBuilder> configureBuilder) {
        OrderItem lastItem = Iterables.getLast(getItems());
        ItemService itemService = ItemServiceProvider.builder()
                .configure(ItemServiceProvider::applyDefaults)
                .configure(configureBuilder)
                .id(null)
                .date(Optional.ofNullable(getOrder().getDelivery())
                        .map(Delivery::getDeliveryDates)
                        .map(DeliveryDates::getToDate)
                        .orElse(new Date()))
                .build();
        lastItem.getServices().add(itemService);
        if (getReportParameters() != null) {
            getReportParameters().setOffers(getOrder().getItems().stream()
                    .map(FoundOfferBuilder::createFrom)
                    .peek(b -> {
                        if (getOrder().getRgb() == Color.WHITE) {
                            b.configure(whiteOffer());
                        }
                    })
                    .map(FoundOfferBuilder::build)
                    .collect(Collectors.toUnmodifiableList()));
        }
        return itemService;
    }

    public void setDebugAllCourierOptions(boolean debugAllCourierOptions) {
        configuration.cart().request().setDebugAllCourierOptions(debugAllCourierOptions);
    }

    public void setPerkPromoId(String perkPromoId) {
        configuration.cart().request().setPerkPromoId(perkPromoId);
    }

    public void setShouldMockStockStorageGetAmountResponse(boolean shouldMockStockStorageGetAmountResponse) {
        Iterables.getOnlyElement(configuration.cart().mockConfigurations().values())
                .setShouldMockStockStorageGetAmountResponse(shouldMockStockStorageGetAmountResponse);
    }

    public void setSkipDiscountCalculation(Boolean skipDiscountCalculation) {
        configuration.cart().request().setSkipDiscountCalculation(skipDiscountCalculation);
    }

    public void setShowCredits(boolean value) {
        configuration.cart().request().setShowCredits(value);
    }

    public void setShowCreditBroker(boolean value) {
        configuration.cart().request().setShowCreditBroker(value);
    }

    public void setShowInstallments(boolean value) {
        configuration.cart().request().setShowInstallments(value);
    }

    public void setExpectedCartException(Class<? extends Exception> expectedCartException) {
        configuration.cart().response().setExpectedException(expectedCartException);
    }

    public void setExpectedCheckoutException(Class<? extends Exception> expectedCheckoutException) {
        configuration.checkout().response().setExpectedException(expectedCheckoutException);
    }

    public void setForceDeliveryId(Long forceDeliveryId) {
        configuration.cart().request().setForceDeliveryId(forceDeliveryId);
    }

    public void setPresets(List<PresetInfo> presets) {
        configuration.cart().body().multiCart().setPresets(presets);
    }

    public void makeCashOnly() {
        // Мока пушапи формирует ответ с постоплатой на основе способов оплаты опций доставки
        getPushApiDeliveryResponses().forEach(
                d -> {
                    Set<PaymentMethod> paymentMethods = new HashSet<>();
                    paymentMethods.add(PaymentMethod.CASH_ON_DELIVERY);
                    paymentMethods.add(PaymentMethod.CARD_ON_DELIVERY);
                    d.setPaymentOptions(paymentMethods);
                }
        );
    }

    public void setAsyncPaymentCardId(String asyncPaymentCardId) {
        configuration.checkout().request().setAsyncPaymentCardId(asyncPaymentCardId);
    }

    public void setLoginId(String loginId) {
        configuration.checkout().request().setLoginId(loginId);
    }

    public void setShowSbp(boolean showSbp) {
        configuration.cart().request().setShowSbp(showSbp);
    }

    public void setUid(long uid) {
        var multicart = getBuiltMultiCart();
        if (multicart.getBuyer() != null) {
            multicart.getBuyer().setUid(uid);
        }
        if (multicart.getCarts() != null) {
            for (Order cart : multicart.getCarts()) {
                if (cart.getBuyer() != null) {
                    cart.getBuyer().setUid(uid);
                }
            }
        }
    }

    public void setYandexUid(String yandexUid) {
        var multicart = getBuiltMultiCart();
        if (multicart.getBuyer() != null) {
            multicart.getBuyer().setYandexUid(yandexUid);
        }
        if (multicart.getCarts() != null) {
            for (Order cart : multicart.getCarts()) {
                if (cart.getBuyer() != null) {
                    cart.getBuyer().setYandexUid(yandexUid);
                }
            }
        }
    }

    public void setGoogleServiceId(String googleServiceId) {
        setYandexUid(null);
        configuration.cart().request().setGoogleServiceId(googleServiceId);
    }

    public void setIosDeviceId(String iosDeviceId) {
        setYandexUid(null);
        configuration.cart().request().setIosDeviceId(iosDeviceId);
    }

    public void setShowVat(boolean value) {
        configuration.cart().request().setShowVat(value);
    }

    public void setBnplFeatures(Collection<String> bnplFeatures) {
        configuration.cart().request().setBnplFeatures(bnplFeatures);
    }

    public void setSpreadAlgorithmV2(boolean spreadAlgorithmV2) {
        configuration.cart().request().setSpreadAlgorithmV2(spreadAlgorithmV2);
    }
}
