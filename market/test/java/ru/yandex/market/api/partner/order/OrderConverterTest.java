package ru.yandex.market.api.partner.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinitionBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import io.github.benas.randombeans.randomizers.misc.NullRandomizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.common.util.functional.Functions;
import ru.yandex.common.util.language.LanguageCode;
import ru.yandex.market.api.partner.controllers.order.model.AddressDTO;
import ru.yandex.market.api.partner.controllers.order.model.DispatchType;
import ru.yandex.market.api.partner.controllers.order.model.ItemStatusDTO;
import ru.yandex.market.api.partner.controllers.order.model.OrderDTO;
import ru.yandex.market.api.partner.controllers.order.model.OrderItemResponseDTO;
import ru.yandex.market.api.partner.controllers.order.model.OrderStatusDTO;
import ru.yandex.market.api.partner.controllers.order.model.OrderSubstatusDTO;
import ru.yandex.market.api.partner.controllers.order.shipment.Shipment;
import ru.yandex.market.api.partner.controllers.util.CurrencyAndRegionHelper;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tariff.TariffData;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.AbstractChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.delivery.DeliveryServiceInfo;
import ru.yandex.market.core.fulfillment.model.StockType;
import ru.yandex.market.core.order.OrderReceiptService;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.core.order.model.ReceiptInfo;
import ru.yandex.market.core.order.resupply.ResupplyOrderDao;
import ru.yandex.market.core.order.resupply.ResupplyOrderItem;
import ru.yandex.market.core.order.resupply.ResupplySource;
import ru.yandex.market.core.order.resupply.ResupplyType;
import ru.yandex.market.personal_market.PersonalAddress;
import ru.yandex.market.personal_market.PersonalRetrieveResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 * Тесты для {@link OrderConverter}.
 *
 * @author Vladislav Bauer
 */
class OrderConverterTest {
    private static final DeliveryInfoService deliveryInfoServiceMock = Mockito.mock(DeliveryInfoService.class);

    private static final int MAX_IGNORE_FIELDS = 1000;

    private static final OrderConverter CONVERTER = createOrderConverter();
    private static final Address ADDRESS1 = generateAddress();
    private static final Address ADDRESS2 = generateAddress();
    private static final Address ADDRESS3 = generateAddress();

    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(ADDRESS1, ADDRESS2, ADDRESS3, generateAddressDTO(ADDRESS1)),
                Arguments.of(null, ADDRESS2, ADDRESS3, generateAddressDTO(ADDRESS2)),
                Arguments.of(null, null, ADDRESS3, generateAddressDTO(ADDRESS3)),
                Arguments.of(null, null, null, null),
                Arguments.of(null, ADDRESS2, null, generateAddressDTO(ADDRESS2))
        );
    }

    private static final Set<OrderSubstatusDTO> IGNORED_SUBSTATUSES_TO_CHECK = Set.of(
            OrderSubstatusDTO.RECEIVED_ON_DISTRIBUTION_CENTER
    );

    private static final Set<OrderStatusDTO> IGNORED_STATUSES_TO_CHECK = Set.of(
            OrderStatusDTO.REJECTED,
            OrderStatusDTO.RETURNED,
            OrderStatusDTO.PARTIALLY_RETURNED
    );

    private static Address generateAddress() {
        AddressImpl address = EnhancedRandom.random(AddressImpl.class);

        String phone = "+79111111111";
        address.setPhone(phone);

        return address;
    }

    private static AddressDTO generateAddressDTO(Address address) {
        return new AddressDTO.Builder()
                .withCountry(address.getCountry())
                .withPostcode(address.getPostcode())
                .withCity(address.getCity())
                .withSubway(address.getSubway())
                .withStreet(address.getStreet())
                .withHouse(address.getHouse())
                .withBlock(address.getBlock())
                .withEntrance(address.getEntrance())
                .withEntryPhone(address.getEntryPhone())
                .withFloor(address.getFloor())
                .withApartment(address.getApartment())
                .withRecipientPerson(address.getRecipientPerson().getLastName() + " " +
                        address.getRecipientPerson().getFirstName() + " " +
                        address.getRecipientPerson().getMiddleName())
                .withPhone(address.getPhone())
                .build();
    }

    private static Multimap<FeedOfferId, OrderItem> generateItemsByFeedOffer() {
        Multimap<FeedOfferId, OrderItem> map = LinkedListMultimap.create();
        map.put(EnhancedRandom.random(FeedOfferId.class), EnhancedRandom.random(OrderItem.class,
                "instances"));
        return map;
    }

    private static TariffData generateTariffData() {
        final EnhancedRandom randomEnumSet = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .randomize(FieldDefinitionBuilder.field().named("customsLanguages").ofType(EnumSet.class).get(),
                        (Randomizer<EnumSet>) () -> EnumSet.of(LanguageCode.BE))
                .build();
        return randomEnumSet.nextObject(TariffData.class);
    }

    private static OrderConverter createOrderConverter() {
        final CurrencyAndRegionHelper currencyAndRegionHelper = Mockito.mock(CurrencyAndRegionHelper.class);
        final OrderService orderService = Mockito.mock(OrderService.class);
        final OrderReceiptService orderReceiptService = Mockito.mock(OrderReceiptService.class);
        final ResupplyOrderDao resupplyOrderDao = Mockito.mock(ResupplyOrderDao.class);
        Mockito.when(resupplyOrderDao.getOrderResupplies(any(Long.class))).thenReturn(HashMultimap.create());

        return new OrderConverter(currencyAndRegionHelper, orderService, orderReceiptService,
                resupplyOrderDao, deliveryInfoServiceMock);
    }

    public static Stream<Arguments> marketBrandedParams() {
        return Stream.of(
                Arguments.of(false, DeliveryPartnerType.SHOP, DeliveryType.PICKUP, DispatchType.SHOP_OUTLET),
                Arguments.of(true, DeliveryPartnerType.SHOP, DeliveryType.PICKUP, DispatchType.MARKET_BRANDED_OUTLET),
                Arguments.of(false, DeliveryPartnerType.YANDEX_MARKET, DeliveryType.PICKUP, DispatchType.UNKNOWN),
                Arguments.of(true, DeliveryPartnerType.YANDEX_MARKET, DeliveryType.PICKUP, DispatchType.MARKET_BRANDED_OUTLET),
                Arguments.of(false, DeliveryPartnerType.SHOP, DeliveryType.DELIVERY, DispatchType.BUYER),
                Arguments.of(true, DeliveryPartnerType.SHOP, DeliveryType.DELIVERY, DispatchType.MARKET_BRANDED_OUTLET),
                Arguments.of(false, DeliveryPartnerType.YANDEX_MARKET, DeliveryType.DELIVERY, DispatchType.UNKNOWN),
                Arguments.of(true, DeliveryPartnerType.YANDEX_MARKET, DeliveryType.DELIVERY, DispatchType.MARKET_BRANDED_OUTLET)
        );
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("data")
    @DisplayName("Проверить что address проставляется корректно")
    void testDeliveryAddressMapping(
            final Address address,
            final Address shopAddress,
            final Address buyerAddress,
            final AddressDTO expectedAddress
    ) throws Exception {
        final Order order = generateCheckoutOrder();
        final ru.yandex.market.checkout.checkouter.delivery.Delivery delivery = order.getDelivery();
        delivery.setAddress(address);
        delivery.setShopAddress(shopAddress);
        delivery.setBuyerAddress(buyerAddress);

        final OrderDTO orderDTO = convert(order, null, Map.of(), HashMultimap.create());
        assertThat(orderDTO.getDelivery().getAddress()).isEqualTo(expectedAddress);
    }

    @Test
    @DisplayName("Если заполнено новое поле отгрузки, берем значение из него")
    void shipmentDateBySupplier() throws Exception {
        final Order order = generateCheckoutOrder();
        setDeliveryToOrder(order);

        final OrderDTO orderDTO = convert(order, null, Map.of(), HashMultimap.create());

        Map<Long, Shipment> shipments =
                orderDTO.getDelivery().getShipments()
                        .stream()
                        .collect(Collectors.toMap(Shipment::getId, Function.identity()));

        Map<String, LocalDateTime> orderDates = getDatesToOrder();
        LocalDate shipmentDate = orderDates.get("shipmentDate").toLocalDate();
        LocalDateTime shipmentDateBySupplier = orderDates.get("shipmentDateBySupplier");

        assertThat(shipments.get(123654789L).getShipmentDate()).isEqualTo(DateUtil.asDate(shipmentDateBySupplier));
        assertThat(shipments.get(321456987L).getShipmentDate()).isEqualTo(DateUtil.asDate(shipmentDate));
        assertThat(shipments.get(987456321L).getShipmentDate()).isEqualTo(DateUtil.asDate(shipmentDateBySupplier));
    }

    @ParameterizedTest
    @MethodSource("marketBrandedParams")
    @DisplayName("Поле delivery.marketBranded маппится на соответствующий тип точки назначения доставки")
    void setIsMarketBrandedFlag(
            Boolean isMarketBranded,
            DeliveryPartnerType deliveryPartnerType,
            DeliveryType deliveryType,
            DispatchType expectedDispatchType) throws Exception {
        Order order = generateCheckoutOrder();
        setDeliveryToOrder(order);
        order.getDelivery().setDeliveryPartnerType(deliveryPartnerType);
        order.getDelivery().setType(deliveryType);
        if (isMarketBranded != null) {
            order.getDelivery().setMarketBranded(isMarketBranded);
        }

        OrderDTO orderDTO = convert(order, null, Map.of(), HashMultimap.create());

        assertNotNull(orderDTO.getDelivery().getDispatchType());
        assertEquals(expectedDispatchType, orderDTO.getDelivery().getDispatchType());
    }

    private Parcel createParcel(long id,
                                LocalDate shipmentDate,
                                LocalDateTime shipmentTime,
                                LocalDateTime shipmentDateBySupplier) {
        final Parcel parcel = new Parcel();
        parcel.setId(id);
        parcel.setShipmentDate(shipmentDate);
        parcel.setShipmentTime(shipmentTime);
        parcel.setShipmentDateTimeBySupplier(shipmentDateBySupplier);
        return parcel;
    }

    /**
     * В рамках задачи  https://st.yandex-team.ru/MBI-42446 были созданы копии енумов,
     * которые содержат все возможные статусы и сабстатусы заказов.
     * См. <a href="http://wiki.yandex-team.ru/market/marketplace/Dev/API/order-statuses">
     * <p>
     * Для поддержания констистентности этих множеств значений были сделаны тесты,
     * которые проверяют одинаковый набор значений и одинаковый порядок этих значений.
     * <p>
     * Падение этого теста скорее всего связано с обновлением версии клиента чекаутера.
     * Для исправления нужно снова сделать набор значений {@link OrderStatus} и {@link OrderStatusDTO} одинаковым.
     * Чаще всего нужно добавить значение, которое появилось в новой версии, в {@link OrderStatusDTO}.
     * <p>
     * <b>Важно: Если вы добавляете новый под-статус заказа в партнерский API свяжитесь с MBI / командой технических
     * писателей, чтобы поддержать ваши изменения в документации!</b>
     */
    @Test
    void testSynchronizationOrderStatusesWithCheckouterClient() {
        final List<OrderStatus> checkouterStatuses = Arrays.asList(OrderStatus.values());
        final List<OrderStatusDTO> mbiStatuses = Arrays.asList(OrderStatusDTO.values());

        mbiStatuses
                .stream()
                .filter(s -> !IGNORED_STATUSES_TO_CHECK.contains(s))
                .forEach(s -> assertTrue(checkouterStatuses.contains(s.toCheckouter()),
                        "Expect checkouter substatus " + s)
                );

        checkouterStatuses.forEach(s -> assertTrue(
                mbiStatuses.contains(OrderStatusDTO.fromCheckouter(s)),
                String.format("Expect MBI order's status %s. You must read comments for this test!", s)
        ));
    }

    /**
     * В рамках задачи  https://st.yandex-team.ru/MBI-42446 были созданы копии енумов,
     * которые содержат все возможные статусы и сабстатусы заказов.
     * См. <a href="http://wiki.yandex-team.ru/market/marketplace/Dev/API/order-statuses">doc</a>
     * <p>
     * Для поддержания констистентности этих множеств значений были сделаны тесты,
     * которые проверяют одинаковый набор значений.
     * <p>
     * <p>Падение этого теста скорее всего связано с обновлением версии клиента чекаутера.
     * Для исправления нужно снова сделать набор значений {@link OrderSubstatus} и {@link OrderSubstatusDTO} одинаковым.
     * Чаще всего нужно добавить значение, которое появилось в новой версии, в {@link OrderSubstatusDTO}.
     * <p>
     * <b>Важно: Если вы добавляете новый под-статус заказа в партнерский API свяжитесь с MBI / командой технических
     * писателей, чтобы поддержать ваши изменения в документации!</b>
     */
    @Test
    void testSynchronizationOrderSubstatusesWithCheckouterClient() {
        final List<OrderSubstatus> checkouterStatuses = Arrays.asList(OrderSubstatus.values());
        final List<OrderSubstatusDTO> mbiStatuses = Arrays.asList(OrderSubstatusDTO.values());

        mbiStatuses
                .stream()
                .filter(s -> !IGNORED_SUBSTATUSES_TO_CHECK.contains(s))
                .forEach(s -> assertTrue(
                        checkouterStatuses.contains(s.toCheckouter()),
                        "Expect checkouter substatus " + s)
                );

        checkouterStatuses.forEach(s -> assertTrue(
                mbiStatuses.contains(OrderSubstatusDTO.fromCheckouter(s)),
                String.format("Expect MBI order's sub-status %s. You must read comments for this test!", s)
        ));
    }

    /**
     * Перекладывание Спасибо из цены в субсидии для каждого айтема и для заказа в целом.
     */
    @Test
    void moveSpasiboAndCashbackFromPriceToSubsidy() throws Exception {
        final Order order = generateCheckoutOrder();
        order.getPromoPrices().setSubsidyTotal(BigDecimal.valueOf(500));
        order.setTotal(BigDecimal.valueOf(2000));
        order.setItemsTotal(BigDecimal.valueOf(2000));

        OrderItem item = new OrderItem();
        item.setPrice(BigDecimal.valueOf(800));
        item.setId(995L);
        item.setFeedId(101L);
        item.setCount(1);
        item.getPrices().setSubsidy(BigDecimal.valueOf(200));

        OrderItem item2 = new OrderItem();
        item2.setPrice(BigDecimal.valueOf(1200));
        item2.setId(555L);
        item2.setFeedId(202L);
        item2.setCount(1);
        item2.getPrices().setSubsidy(BigDecimal.valueOf(300));

        order.setItems(List.of(item, item2));

        Map<Long, ReceiptInfo> receiptByItem =
                Map.of(
                        995L, ReceiptInfo.of(BigDecimal.valueOf(300), BigDecimal.valueOf(200)),
                        555L, ReceiptInfo.of(BigDecimal.valueOf(50), BigDecimal.valueOf(100)));

        Multimap<Long, ResupplyOrderItem> resuppliesByItem = ImmutableMultimap
                .of(995L, ResupplyOrderItem.newBuilder()
                        .setSourceId(1)
                        .setSource(ResupplySource.CTE)
                        .setItemId(995L)
                        .setItemCount(1)
                        .setResupplyType(ResupplyType.UNREDEEMED)
                        .setStockType(StockType.FIT)
                        .setCreatedAt(LocalDateTime.of(2020, 11, 18, 10, 45))
                        .build()
                );

        OrderDTO orderDTO = convert(order, receiptByItem, Map.of(), resuppliesByItem);

        assertEquals(orderDTO.getTotal().intValue(), 1350);
        assertEquals(orderDTO.getItemsTotal().intValue(), 1350);
        assertEquals(orderDTO.getSubsidyTotal().intValue(), 1150);
        assertEquals(orderDTO.getTotalWithSubsidy().intValue(), 2500);

        OrderItemResponseDTO convertedItemResponse = orderDTO.getItems()
                .stream()
                .filter(i -> i.getOrderItem().getId() == 995L)
                .findFirst()
                .orElseThrow();
        OrderItem convertedItem = convertedItemResponse.getOrderItem();

        assertEquals(300, convertedItem.getPrice().intValue());
        assertEquals(700, convertedItem.getPrices().getSubsidy().intValue());
        assertEquals(ItemStatusDTO.REJECTED, convertedItemResponse.getOrderItemDetails().get(0).getItemStatus());
        assertEquals(LocalDate.of(2020, 11, 18), convertedItemResponse.getOrderItemDetails().get(0).getUpdateDate());
    }

    @Test
    void shouldSetPartnerWarehouseId() throws Exception {
        int marketWarehouseId = 1;
        String partnerWarehouseId = "2";
        DeliveryServiceInfo deliveryServiceInfo = new DeliveryServiceInfo();
        deliveryServiceInfo.setExternalId(partnerWarehouseId);
        Mockito.when(deliveryInfoServiceMock.getDeliveryServicesByIds(Set.of((long) marketWarehouseId))).
                thenReturn(List.of(deliveryServiceInfo));

        final Order order = generateCheckoutOrder();
        OrderItem item = new OrderItem();
        item.setCount(1);
        item.setWarehouseId(marketWarehouseId);
        order.setItems(List.of(item));
        OrderDTO orderDTO = convert(order, null, Map.of((long) marketWarehouseId, partnerWarehouseId), HashMultimap.create());
        orderDTO.getItems().forEach(oi -> assertEquals(partnerWarehouseId, oi.getPartnerWarehouseId()));
    }

    @DisplayName("Проверка конвертации заказа в DTO, когда в заказа есть только идентификаторы Personal")
    @Test
    void testConvertOrderWithPersonalIds() throws Exception {
        Order order = generateCheckoutOrder();

        Buyer buyerFromCheckouter = new Buyer(1234);
        buyerFromCheckouter.setId("81dc9bdb52d04dc20036dbd8313ed055");
        buyerFromCheckouter.setPersonalFullNameId("fullname_4bb2f8b0125a7035ad727d365f38258f");
        buyerFromCheckouter.setPersonalPhoneId("phone_07e5f8af4dd789c290edc945b0ba59a4");
        buyerFromCheckouter.setPersonalEmailId("email_f43b3e6120f18c6db351a35caa1eb6ff");
        order.setBuyer(buyerFromCheckouter);

        AddressImpl address = new AddressImpl();
        address.setPersonalAddressId("address_884d9804999fc47a3c2694e49ad2536a");
        address.setPersonalFullNameId("recipientFullName_25d3d6917ba3b8688a2dcf1982a6fc02");
        address.setPersonalPhoneId("recipientPhone_6ee6567c464c064384a893ca5e7e027b");
        order.getDelivery().setAddress(address);

        PersonalAddress personalAddress = EnhancedRandom.random(PersonalAddress.class);
        PersonalRetrieveResponse personalData = PersonalRetrieveResponse.builder()
                .fullName("fullname_4bb2f8b0125a7035ad727d365f38258f", "Ivan", "Ivanov", "Ivanovich")
                .phone("phone_07e5f8af4dd789c290edc945b0ba59a4", "+71231231212")
                .email("email_f43b3e6120f18c6db351a35caa1eb6ff", "email@email.ru")
                .address("address_884d9804999fc47a3c2694e49ad2536a", personalAddress)
                .phone("recipientPhone_6ee6567c464c064384a893ca5e7e027b", "+71211122233")
                .fullName("recipientFullName_25d3d6917ba3b8688a2dcf1982a6fc02", "Fn", "Ln", "Mn")
                .build();
        OrderDTO orderDTO = convert(order, personalData, null, Map.of(), HashMultimap.create());

        assertThat(orderDTO.getBuyer()).satisfies(buyer -> {
            assertThat(buyer.getId()).isEqualTo("81dc9bdb52d04dc20036dbd8313ed055");
            assertThat(buyer.getFirstName()).isEqualTo("Ivan");
            assertThat(buyer.getLastName()).isEqualTo("Ivanov");
            assertThat(buyer.getMiddleName()).isEqualTo("Ivanovich");
            assertThat(buyer.getPhone()).isEqualTo("+71231231212");
            assertThat(buyer.getEmail()).isEqualTo("email@email.ru");
        });
        assertThat(orderDTO.getDelivery().getAddress()).satisfies(addressDTO -> {
            assertThat(addressDTO.getCountry()).isEqualTo(personalAddress.getCountry());
            assertThat(addressDTO.getPostcode()).isEqualTo(personalAddress.getPostcode());
            assertThat(addressDTO.getCity()).isEqualTo(personalAddress.getCity());
            assertThat(addressDTO.getSubway()).isEqualTo(personalAddress.getSubway());
            assertThat(addressDTO.getStreet()).isEqualTo(personalAddress.getStreet());
            assertThat(addressDTO.getHouse()).isEqualTo(personalAddress.getHouse());
            assertThat(addressDTO.getBlock()).isEqualTo(personalAddress.getBlock());
            assertThat(addressDTO.getEntrance()).isEqualTo(personalAddress.getEntrance());
            assertThat(addressDTO.getEntryPhone()).isEqualTo(personalAddress.getEntryPhone());
            assertThat(addressDTO.getFloor()).isEqualTo(personalAddress.getFloor());
            assertThat(addressDTO.getApartment()).isEqualTo(personalAddress.getApartment());

            assertThat(addressDTO.getPhone()).isEqualTo("+71211122233");
            assertThat(addressDTO.getRecipientPerson()).isEqualTo("Ln Fn Mn");
        });
    }

    @DisplayName("Проверка конвертации заказа в DTO, когда в заказа нет идентификаторов Personal")
    @Test
    void testConvertOrderWithoutPersonalIds() throws Exception {
        Order order = generateCheckoutOrder();

        Buyer buyerFromCheckouter = new Buyer(1234);
        buyerFromCheckouter.setId("81dc9bdb52d04dc20036dbd8313ed055");
        buyerFromCheckouter.setFirstName("Ivan");
        buyerFromCheckouter.setLastName("Ivanov");
        buyerFromCheckouter.setMiddleName("Ivanovich");
        buyerFromCheckouter.setPhone("+71231231212");
        buyerFromCheckouter.setEmail("email@email.ru");
        order.setBuyer(buyerFromCheckouter);

        AddressImpl address = EnhancedRandom.random(AddressImpl.class);
        address.setRecipientPerson(new RecipientPerson("Fn", "Mn", "Ln"));
        order.getDelivery().setAddress(address);

        PersonalRetrieveResponse personalData = PersonalRetrieveResponse.builder().build();
        OrderDTO orderDTO = convert(order, personalData, null, Map.of(), HashMultimap.create());

        assertThat(orderDTO.getBuyer()).satisfies(buyer -> {
            assertThat(buyer.getId()).isEqualTo("81dc9bdb52d04dc20036dbd8313ed055");
            assertThat(buyer.getFirstName()).isEqualTo("Ivan");
            assertThat(buyer.getLastName()).isEqualTo("Ivanov");
            assertThat(buyer.getMiddleName()).isEqualTo("Ivanovich");
            assertThat(buyer.getPhone()).isEqualTo("+71231231212");
            assertThat(buyer.getEmail()).isEqualTo("email@email.ru");
        });
        assertThat(orderDTO.getDelivery().getAddress()).satisfies(addressDTO -> {
            assertThat(addressDTO.getCountry()).isEqualTo(address.getCountry());
            assertThat(addressDTO.getPostcode()).isEqualTo(address.getPostcode());
            assertThat(addressDTO.getCity()).isEqualTo(address.getCity());
            assertThat(addressDTO.getSubway()).isEqualTo(address.getSubway());
            assertThat(addressDTO.getStreet()).isEqualTo(address.getStreet());
            assertThat(addressDTO.getHouse()).isEqualTo(address.getHouse());
            assertThat(addressDTO.getBlock()).isEqualTo(address.getBlock());
            assertThat(addressDTO.getEntrance()).isEqualTo(address.getEntrance());
            assertThat(addressDTO.getEntryPhone()).isEqualTo(address.getEntryPhone());
            assertThat(addressDTO.getFloor()).isEqualTo(address.getFloor());
            assertThat(addressDTO.getApartment()).isEqualTo(address.getApartment());

            assertThat(addressDTO.getPhone()).isEqualTo(address.getPhone());
            assertThat(addressDTO.getRecipientPerson()).isEqualTo("Ln Fn Mn");
        });

    }

    private OrderDTO convert(
            Order order,
            Map<Long, ReceiptInfo> receiptByItem,
            Map<Long, String> warehouseExternalIds,
            Multimap<Long, ResupplyOrderItem> resuppliesByItem
    ) {
        return convert(order, PersonalRetrieveResponse.builder().build(), receiptByItem, warehouseExternalIds, resuppliesByItem);
    }

    private OrderDTO convert(
            Order order,
            PersonalRetrieveResponse personalData,
            Map<Long, ReceiptInfo> receiptByItem,
            Map<Long, String> warehouseExternalIds,
            Multimap<Long, ResupplyOrderItem> resuppliesByItem
    ) {
        final Function<Long, String> labelUrlBuilder = Functions.always(StringUtils.EMPTY);
        return CONVERTER.convertToModel(order, personalData, labelUrlBuilder, receiptByItem, resuppliesByItem, warehouseExternalIds, null);
    }

    private Order generateCheckoutOrder() throws Exception {
        var orderRandomBuilder = getOrderRandomBuilder();
        for (int i = 0; i < MAX_IGNORE_FIELDS; i++) {
            try {
                return orderRandomBuilder.build().nextObject(Order.class);
            } catch (Exception e) {
                final var rootCause = ExceptionUtils.getRootCause(e);
                if (rootCause instanceof InstantiationError) {
                    final Class<?> badOrderFieldClass = Class.forName(rootCause.getMessage());
                    orderRandomBuilder.exclude(badOrderFieldClass);
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Unexpectedly large numbers of fields");
    }

    private EnhancedRandomBuilder getOrderRandomBuilder() {
        return EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .randomize(ItemPromo.class, new NullRandomizer())
                .randomize(Address.class, (Supplier<Address>) OrderConverterTest::generateAddress)
                .randomize(FieldDefinitionBuilder.field().named("itemsByFeedOffer").ofType(Multimap.class).get(),
                        (Randomizer<Multimap<FeedOfferId, OrderItem>>) OrderConverterTest::generateItemsByFeedOffer)
                .randomize(TariffData.class, (Randomizer<TariffData>) OrderConverterTest::generateTariffData)
                .exclude(AbstractChangeRequestPayload.class, JsonNode.class, ArrayNode.class);
    }

    private Map<String, LocalDateTime> getDatesToOrder() {
        return new HashMap<>() {{
            put("shipmentDate", LocalDateTime.of(2020, 3, 5, 0, 0));
            put("shipmentDateTimeBySupplier", LocalDateTime.of(2020, 5, 10, 12, 0));
            put("shipmentDateBySupplier", LocalDateTime.of(2020, 5, 10, 0, 0));
            put("shipmentDeadlineTime", LocalDateTime.of(2021, 10, 10, 12, 22));
        }};
    }

    private void setDeliveryToOrder(Order order) {
        final ru.yandex.market.checkout.checkouter.delivery.Delivery delivery = order.getDelivery();

        Map<String, LocalDateTime> orderDates = getDatesToOrder();
        LocalDate shipmentDate = orderDates.get("shipmentDate").toLocalDate();
        LocalDateTime shipmentDateTimeBySupplier = orderDates.get("shipmentDateTimeBySupplier");
        LocalDateTime shipmentDeadlineTime = orderDates.get("shipmentDeadlineTime");

        delivery.setParcels(
                List.of(
                        createParcel(123654789L, shipmentDate, null, shipmentDateTimeBySupplier),
                        createParcel(321456987L, shipmentDate, shipmentDeadlineTime, null),
                        createParcel(987456321L, null, shipmentDeadlineTime, shipmentDateTimeBySupplier)
                )
        );
    }
}
