package ru.yandex.market.checkout.checkouter.returns;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.pay.exceptions.ReturnDeliveryValidationException;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_SINGLE_STEP_RETURN_CREATION;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_FULL_NAME_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_PHONE_ID;
import static ru.yandex.market.checkout.helpers.ReturnHelper.copyWithRandomizeItemComments;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class ReturnByCourierDeliveryTest extends AbstractReturnTestBase {

    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private ReturnService returnService;
    @Autowired
    private CheckouterFeatureWriter featureWriter;

    private final Long returnDeliveryServiceId = 324685L;
    private final LocalTime slotFromTime = LocalTime.of(12, 0);
    private final LocalTime slotToTime = LocalTime.of(18, 0);

    @BeforeEach
    void setUp() {
        reportMock.resetRequests();
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
    }

    @Test
    @DisplayName("Создание возврата с курьерской доставкой за 1 шаг (сразу указав доставку при создании возврата)")
    void returnByCourierBy1Step() {
        Order order = makeOrder();
        Return returnTemplate = ReturnProvider.generateReturnWithDelivery(order, returnDeliveryServiceId);
        mockReturnActualDelivery(returnTemplate, order);
        setDefaultDelivery(order, returnTemplate, DeliveryType.DELIVERY, false);
        returnTemplate.getDelivery().setSenderAddress(AddressProvider.getSenderAddress());
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.USER).withId(order.getBuyer().getUid()).build();
        assertThat(returnTemplate.getDelivery().getType()).isEqualTo(DeliveryType.DELIVERY);
        assertThat(returnTemplate.getDelivery().getDates()).isNotNull();
        assertThat(returnTemplate.getDelivery().getSenderAddress()).isNotNull();

        Return ret = returnService.initReturn(order.getId(), clientInfo, returnTemplate);

        assertDelivery(ret.getDelivery(), returnTemplate.getDelivery());
        assertItemFields(ret, order);
    }

    // Checks legacy API; to delete in MARKETCHECKOUT-27094
    @Test
    @DisplayName("Создание возврата с курьерской доставкой за 1 шаг (без идентификатора телефона в сервисе Personal)")
    void returnByCourierBy1StepWithoutPersonalPhoneId() {
        featureWriter.writeValue(USE_PERSONAL_PHONE_ID, false);
        Order order = makeOrder();
        Return returnTemplate = ReturnProvider.generateReturnWithDelivery(order, returnDeliveryServiceId);
        mockReturnActualDelivery(returnTemplate, order);
        setDefaultDelivery(order, returnTemplate, DeliveryType.DELIVERY, false);
        SenderAddress senderAddress = AddressProvider.getSenderAddress();
        senderAddress.setPersonalPhoneId(null);
        returnTemplate.getDelivery().setSenderAddress(senderAddress);
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.USER).withId(order.getBuyer().getUid()).build();

        Return ret = returnService.initReturn(order.getId(), clientInfo, returnTemplate);

        assertDelivery(ret.getDelivery(), returnTemplate.getDelivery());
    }

    // Checks future API; to delete in MARKETCHECKOUT-27094
    @Test
    @DisplayName("Создание возврата с курьерской доставкой за 1 шаг (без номера телефона, только в идентификатором)")
    void returnByCourierBy1StepWithPersonalPhoneIdOnly() {
        featureWriter.writeValue(USE_PERSONAL_PHONE_ID, true);
        featureWriter.writeValue(USE_PERSONAL_FULL_NAME_ID, true);
        personalMockConfigurer.mockV1MultiTypesRetrieve();
        Order order = makeOrder();
        Return returnTemplate = ReturnProvider.generateReturnWithDelivery(order, returnDeliveryServiceId);
        mockReturnActualDelivery(returnTemplate, order);
        setDefaultDelivery(order, returnTemplate, DeliveryType.DELIVERY, false);
        SenderAddress senderAddress = AddressProvider.getSenderAddress();
        senderAddress.setPhone(null);
        returnTemplate.getDelivery().setSenderAddress(senderAddress);
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.USER).withId(order.getBuyer().getUid()).build();

        Return ret = returnService.initReturn(order.getId(), clientInfo, returnTemplate);

        assertDelivery(ret.getDelivery(), returnTemplate.getDelivery());
    }

    @Test
    @DisplayName("Создание возврата с курьерской доставкой за 2 шага (выбрав доставку отдельной ручкой)")
    void returnByCourierBy2Step() {
        Order order = makeOrder();
        Return returnTemplate = ReturnProvider.generateReturnWithDelivery(order, returnDeliveryServiceId);
        mockReturnActualDelivery(returnTemplate, order);
        setDefaultDelivery(order, returnTemplate, DeliveryType.DELIVERY, false);
        returnTemplate.getDelivery().setSenderAddress(AddressProvider.getSenderAddress());
        ReturnDelivery delivery = returnTemplate.getDelivery();
        returnTemplate.setDelivery(null);
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.USER).withId(order.getBuyer().getUid()).build();
        assertThat(delivery.getType()).isEqualTo(DeliveryType.DELIVERY);
        assertThat(delivery.getDates()).isNotNull();
        assertThat(delivery.getSenderAddress()).isNotNull();

        Return ret = returnService.initReturn(order.getId(), clientInfo, returnTemplate, Experiments.empty());
        ret = returnService.addReturnDelivery(order.getId(), ret.getId(), delivery, clientInfo, Experiments.empty());

        assertDelivery(ret.getDelivery(), delivery);
        assertItemFields(ret, order);
    }

    @Test
    @DisplayName("Проверяем, что работает ограничение создания курьерских возвратов")
    public void returnCapacityTest() {
        checkouterFeatureWriter.writeValue(ENABLE_SINGLE_STEP_RETURN_CREATION, true);
        featureWriter.writeValue(IntegerFeatureType.RETURN_MAX_CAPACITY, 1);
        Order order = makeOrder();
        Return returnTemplate = ReturnProvider.generateReturnWithDelivery(order, returnDeliveryServiceId);
        mockReturnActualDelivery(returnTemplate, order, 243);
        setDefaultDelivery(order, returnTemplate, DeliveryType.DELIVERY, false);
        returnTemplate.getDelivery().setSenderAddress(AddressProvider.getSenderAddress());
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.USER).withId(order.getBuyer().getUid()).build();

        //Изначально есть только одна курьерская опция
        List<ReturnDelivery> courierOptions = extractCourierOptions(getReturnOptions(order));
        assertThat(courierOptions).hasSize(1);

        //После создания возврата не должно остаться доступных опций
        returnService.initReturn(order.getId(), clientInfo, returnTemplate);
        courierOptions = extractCourierOptions(getReturnOptions(order));
        assertThat(courierOptions).isEmpty();

        //Создание возврата оканчивается ошибкой, т.к. нет доступных опций
        ReturnDeliveryValidationException exception = Assertions.assertThrows(ReturnDeliveryValidationException.class,
                () -> returnService
                        .initReturn(order.getId(), clientInfo, copyWithRandomizeItemComments(returnTemplate)));
        assertThat(exception.getMessage()).contains("Chosen delivery option with type");
    }

    private List<ReturnDelivery> extractCourierOptions(ReturnOptionsResponse allOptions) {
        try {
            return (allOptions
                    .getDeliveryOptions().stream()
                    .filter(option -> option.getType() == DeliveryType.DELIVERY)
                    .collect(Collectors.toList()));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void assertItemFields(Return ret, Order order) {
        ret.getItems().stream()
                .filter(item -> item.getType() == ReturnItemType.ORDER_DELIVERY)
                .findAny()
                .orElseThrow(() -> new AssertionError("deliveryItem did not find"));
        ReturnItem returnOrderItem = ret.getItems().stream()
                .filter(item -> item.getType() == ReturnItemType.ORDER_ITEM)
                .findAny()
                .orElseThrow(() -> new AssertionError("returnOrderItem did not find"));
        OrderItem orderItem = order.getItem(returnOrderItem.getItemId());
        assertThat(orderItem).isNotNull();
        assertThat(returnOrderItem.getShopUrl()).isEqualTo(orderItem.getShopUrl());
        assertThat(returnOrderItem.getKind2Params()).containsExactlyElementsOf(orderItem.getKind2Parameters());
        assertThat(returnOrderItem.getWeight()).isEqualTo(orderItem.getWeight());
        assertThat(returnOrderItem.getWidth()).isEqualTo(orderItem.getWidth());
        assertThat(returnOrderItem.getHeight()).isEqualTo(orderItem.getHeight());
        assertThat(returnOrderItem.getDepth()).isEqualTo(orderItem.getDepth());
        assertThat(returnOrderItem.getCategoryFullName()).isEqualTo(orderItem.getCategoryFullName());
        assertThat(returnOrderItem.getDescription()).isEqualTo(orderItem.getDescription());
        assertThat(returnOrderItem.getBuyerPrice()).isEqualTo(orderItem.getBuyerPrice());
        assertThat(returnOrderItem.getItemTitle()).isEqualTo(orderItem.getOfferName());
    }

    private void assertDelivery(ReturnDelivery actualDelivery, ReturnDelivery expectedDelivery) {
        assertThat(actualDelivery.getType()).isEqualTo(DeliveryType.DELIVERY);
        assertThat(actualDelivery.getDates()).isEqualTo(expectedDelivery.getDates());
        assertThat(actualDelivery.getDeliveryServiceId()).isEqualTo(returnDeliveryServiceId);
        assertThat(actualDelivery.getPrice()).isNotNull();
        assertThat(actualDelivery.getPrice().getValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(actualDelivery.getPrice().getCurrency()).isEqualTo(Currency.RUR);
        assertThat(actualDelivery.getSenderAddress()).isNotNull();
        assertThat(actualDelivery.getSenderAddress().getCountry())
                .isEqualTo(expectedDelivery.getSenderAddress().getCountry());
        SenderAddress address = actualDelivery.getSenderAddress();
        SenderAddress expectedAddress = expectedDelivery.getSenderAddress();
        assertThat(address.getCountry()).isEqualTo(expectedAddress.getCountry());
        assertThat(address.getPostcode()).isEqualTo(expectedAddress.getPostcode());
        assertThat(address.getCity()).isEqualTo(expectedAddress.getCity());
        assertThat(address.getSubway()).isEqualTo(expectedAddress.getSubway());
        assertThat(address.getStreet()).isEqualTo(expectedAddress.getStreet());
        assertThat(address.getKm()).isEqualTo(expectedAddress.getKm());
        assertThat(address.getHouse()).isEqualTo(expectedAddress.getHouse());
        assertThat(address.getBlock()).isEqualTo(expectedAddress.getBlock());
        assertThat(address.getBuilding()).isEqualTo(expectedAddress.getBuilding());
        assertThat(address.getEstate()).isEqualTo(expectedAddress.getEstate());
        assertThat(address.getEntrance()).isEqualTo(expectedAddress.getEntrance());
        assertThat(address.getEntryPhone()).isEqualTo(expectedAddress.getEntryPhone());
        assertThat(address.getFloor()).isEqualTo(expectedAddress.getFloor());
        assertThat(address.getApartment()).isEqualTo(expectedAddress.getApartment());
        assertThat(address.getSender()).isEqualTo(expectedAddress.getSender());
        assertThat(address.getPhone()).isEqualTo(expectedAddress.getPhone());
        assertThat(address.getPersonalPhoneId()).isEqualTo(expectedAddress.getPersonalPhoneId());
        assertThat(address.getLanguage()).isEqualTo(expectedAddress.getLanguage());
        assertThat(address.getGps()).isEqualTo(expectedAddress.getGps());
        assertThat(address.getEmail()).isEqualTo(expectedAddress.getEmail());
        assertThat(address.getNotes()).isEqualTo(expectedAddress.getNotes());
    }

    private Order makeOrder() {
        Parameters params = defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        return orderService.getOrder(order.getId());
    }

    private void mockReturnActualDelivery(Return retTemplate, Order order) {
        mockReturnActualDelivery(retTemplate, order, 1);
    }

    private void mockReturnActualDelivery(Return retTemplate, Order order, int dayFrom) {
        List<Long> returnItemIds = retTemplate.getItems().stream()
                .filter(item -> item.getType() == ReturnItemType.ORDER_ITEM)
                .map(ReturnItem::getId)
                .collect(Collectors.toList());
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(
                order.getItems().stream()
                        .filter(orderItem -> returnItemIds.contains(orderItem.getId()))
                        .collect(Collectors.toList())
        );
        ActualDeliveryOption courierOption = new ActualDeliveryOption();
        courierOption.setDeliveryServiceId(returnDeliveryServiceId);
        courierOption.setDayFrom(dayFrom);
        courierOption.setDayTo(dayFrom);
        courierOption.setTimeIntervals(List.of(new DeliveryTimeInterval(slotFromTime, slotToTime)));
        courierOption.setCost(BigDecimal.ZERO);
        courierOption.setCurrency(Currency.RUR);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(courierOption)
                        .build());
        parameters.getReportParameters().setExtraActualDeliveryParams(Map.of("is-return", "1"));
        reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, parameters.getReportParameters());
    }
}
