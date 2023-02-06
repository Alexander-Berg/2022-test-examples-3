package ru.yandex.market.logistics.lom.controller.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.CredentialsDto;
import ru.yandex.market.logistics.lom.model.dto.DeliveryIntervalDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto.ItemDtoBuilder;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto;
import ru.yandex.market.logistics.lom.model.dto.OrderServiceDto;
import ru.yandex.market.logistics.lom.model.dto.RecipientDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto.StorageUnitDtoBuilder;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto.WaybillSegmentDtoBuilder;
import ru.yandex.market.logistics.lom.model.enums.ContactType;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.LocationType;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.management.client.LMSClient;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.lom.utils.TestUtils.ITEM_ARTICLES_UNIQUENESS_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.OPTIONAL_NOT_BLANK_ERROR_MESSAGE;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты валидации коммита заказа")
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
@ParametersAreNonnullByDefault
class OrderCommitValidationTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Валидация коммита заказа")
    @MethodSource("orderCommitValidation")
    void orderCreateAndCommitValidationTest(
        @SuppressWarnings("unused") String caseName,
        Consumer<WaybillOrderRequestDto> orderDtoConsumer,
        String message
    ) throws Exception {
        initOrder(orderDtoConsumer);

        OrderTestUtil.commitOrder(mockMvc, 1L)
            .andExpect(status().isBadRequest())
            .andExpect(content().json(message, true));
    }

    @Test
    @DisplayName("Список товаров в заказе может содержать товары с повторяющимися артикулами")
    void beruDuplicateArticles() throws Exception {
        initOrder(order -> order
            .setPlatformClientId(1L)
            .setItems(List.of(createItem().build(), createItem().build()))
        );

        OrderTestUtil.commitOrder(mockMvc, 1L)
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поля Баланса могут быть опущены для Беру")
    @MethodSource("beruCommit")
    void specificValidationForBeru(
        @SuppressWarnings("unused") String caseName,
        Consumer<WaybillOrderRequestDto> orderDtoConsumer
    ) throws Exception {
        initOrder(orderDtoConsumer);

        OrderTestUtil.commitOrder(mockMvc, 1L)
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Test
    @DisplayName("ВГХ могут быть не указаны для PLACE")
    void specificValidationForPlace() throws Exception {
        initOrder(order -> order.setUnits(List.of(
            createUnit("root", null, StorageUnitType.ROOT).build(),
            createUnit("child", "root", StorageUnitType.PLACE).dimensions(null).build()
        )));

        OrderTestUtil.commitOrder(mockMvc, 1L)
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Test
    @DisplayName("Адрес получателя может быть не заполнен для PICKUP заказа после создания драфта")
    void specificValidationForRecipientAddress() throws Exception {
        initOrder(order -> order
            .setRecipient(createRecipient(null))
            .setDeliveryType(DeliveryType.PICKUP)
        );

        OrderTestUtil.commitOrder(mockMvc, 1L)
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @DisplayName("Валидация для полностью оплаченного заказа")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(PaymentMethod.class)
    void validateWithFullyPrepaid(PaymentMethod paymentMethod) throws Exception {
        initOrder(order -> {
            CostDto cost = order.getCost()
                .toBuilder()
                .isFullyPrepaid(true)
                .paymentMethod(paymentMethod)
                .build();
            order.setCost(cost);
        });
        OrderTestUtil.commitOrder(mockMvc, 1L)
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @DisplayName("Валидация для не полностью оплаченного заказа")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(PaymentMethod.class)
    void validateWithoutFullyPrepaid(PaymentMethod paymentMethod) throws Exception {
        initOrder(order -> {
            CostDto cost = order.getCost()
                .toBuilder()
                .isFullyPrepaid(false)
                .paymentMethod(paymentMethod)
                .build();
            order.setCost(cost);
        });
        String errorMessage = "{\"fieldErrors\":[{\"propertyPath\":\"\"," +
            "\"message\":\"PREPAID order must have isFullyPrepaid=true\"}]}";
        OrderTestUtil.commitOrder(mockMvc, 1L)
            .andExpect(paymentMethod != PaymentMethod.PREPAID ? status().isOk() : status().isBadRequest())
            .andExpect(
                paymentMethod != PaymentMethod.PREPAID
                    ? noContent()
                    : content().json(errorMessage, true)
            );
    }

    private void initOrder(Consumer<WaybillOrderRequestDto> orderDtoConsumer) throws Exception {
        clock.setFixed(Instant.parse("2019-01-01T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        when(lmsClient.getPartner(10L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(10L, 20L)));

        WaybillOrderRequestDto order = createRequestOrderBuilder();
        orderDtoConsumer.accept(order);

        mockMvc.perform(request(HttpMethod.POST, "/orders", order))
            .andExpect(status().isOk());
    }

    @Nonnull
    private static Stream<Arguments> beruCommit() {
        return Stream.<Pair<String, Consumer<WaybillOrderRequestDto>>>of(
            Pair.of(
                "id клиента",
                order -> order
                    .setPlatformClientId(1L)
                    .setSenderBalanceClientId(null)
            ),
            Pair.of(
                "id продукта",
                order -> order
                    .setPlatformClientId(1L)
                    .setSenderBalanceProductId(null)
            ),
            Pair.of(
                "id контракта",
                order -> order
                    .setPlatformClientId(1L)
                    .setBalanceContractId(null)
            ),
            Pair.of(
                "id плательщика",
                order -> order
                    .setPlatformClientId(1L)
                    .setBalancePersonId(null)
            ),
            Pair.of(
                "requisiteId в вейбилле",
                order -> order
                    .setWaybill(List.of(
                        createPickupWaybillSegment(createWaybillShipment(LocalDate.of(2019, 6, 6)))
                            .requisiteId(null)
                            .build()
                    ))
                    .setPlatformClientId(1L)
            ),
            Pair.of(
                "requisiteId в вейбилле",
                order -> order
                    .setWaybill(List.of(
                        createPickupWaybillSegment(createWaybillShipment(LocalDate.of(2019, 6, 6)))
                            .requisiteId(null)
                            .build()
                    ))
                    .setPlatformClientId(1L)
            )
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @Nonnull
    private static Stream<Arguments> orderCommitValidation() {
        return Stream.of(
            orderCommitValidationArgumentsPart1(),
            orderCommitValidationArgumentsPart2(),
            newPlacesValidation(),
            segmentTypeValidation(),
            locationTypeValidation()
        )
            .flatMap(Function.identity())
            .map(triple -> Arguments.of(
                triple.getLeft(),
                triple.getMiddle(),
                triple.getRight()
            ));

    }

    @Nonnull
    private static Stream<Triple<String, Consumer<WaybillOrderRequestDto>, String>>
    orderCommitValidationArgumentsPart1() {
        return Stream.of(
            Triple.of(
                "Проверка на уникальность item.article в заказе",
                order -> order
                    .setCost(createCost().itemsSum(BigDecimal.valueOf(400)).total(BigDecimal.valueOf(420)).build())
                    .setItems(List.of(createItem().build(), createItem().build())),
                "{\"fieldErrors\":[{\"propertyPath\":\"items\","
                    + "\"message\":\"" + ITEM_ARTICLES_UNIQUENESS_ERROR_MESSAGE + "\"}]}"
            ),
            Triple.of(
                "Проверка обязательности returnSortingCenterId",
                order -> order.setReturnSortingCenterId(null),
                "{\"fieldErrors\":[{\"propertyPath\":\"returnSortingCenterId\","
                    + "\"message\":\"must not be null\"}]}"
            ),
            Triple.of(
                "Проверка обязательности pickPointId при deliveryType = PICKUP",
                order -> order.setDeliveryType(DeliveryType.PICKUP).setPickupPointId(null),
                "{\"fieldErrors\":[{\"propertyPath\":\"pickupPoint\","
                    + "\"message\":\"when delivery type is pickup point, pickupPointId must be not null\"}]}"
            ),
            Triple.of(
                "Проверка объявленной ценности почтового заказа",
                order -> order.setDeliveryType(DeliveryType.POST).setCost(createCost().build()),
                "{\"fieldErrors\":[{\"propertyPath\":\"cost.assessedValue\","
                    + "\"message\":\"when delivery type is post, assessedValue must be no less than total cost\"}]}"
            ),
            Triple.of(
                "Проверка объявленной ценности почтового заказа с total=null",
                order -> order.setDeliveryType(DeliveryType.POST).setCost(
                    createCost().isFullyPrepaid(null).total(null).build()
                ),
                "{\"fieldErrors\":[{\"propertyPath\":\"cost.amountPrepaid\",\"message\":\"must not be null\"}," +
                    "{\"propertyPath\":\"cost.isFullyPrepaid\",\"message\":\"must not be null\"}," +
                    "{\"propertyPath\":\"cost.total\",\"message\":\"must not be null\"}]}"
            ),
            Triple.of(
                "Проверка наличия услуги Страховка у заказа с ненулевой объявленной ценностью",
                order -> order.setDeliveryType(DeliveryType.POST)
                    .setCost(
                        createCost()
                            .services(List.of(createService(ShipmentOption.SORT)))
                            .assessedValue(BigDecimal.valueOf(220))
                            .build()
                    ),
                "{\"fieldErrors\":[{\"propertyPath\":\"cost\","
                    + "\"message\":\"order with positive assessedValue must have insurance service\"}]}"
            ),
            Triple.of(
                "Проверка обязательности recipient.address",
                order -> order.setDeliveryType(DeliveryType.COURIER)
                    .setRecipient(createRecipient(AddressDto.builder().country("").porch("42").build())),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"recipient.address.country\",\"message\":\"country must be not empty\"},"
                    + "{\"propertyPath\":\"recipient.address.geoId\",\"message\":\"geoId must be not null\"},"
                    + "{\"propertyPath\":\"recipient.address.house\",\"message\":\"house must be not empty\"},"
                    + "{\"propertyPath\":\"recipient.address.locality\",\"message\":\"locality must be not empty\"},"
                    + "{\"propertyPath\":\"recipient.address.region\",\"message\":\"region must be not empty\"},"
                    + "{\"propertyPath\":\"recipient.personalAddressId\","
                    + "\"message\":\"order recipient personal field address must be not null\"}"
                    + "]}"

            ),
            Triple.of(
                "Проверка обязательности recipient contact",
                order -> {
                    order.setContacts(List.of());
                    order.setWaybill(List.of(
                        createWaybillSegment(ru.yandex.market.logistics.lom.model.enums.SegmentType.COURIER, 1L, 2L)
                            .build()
                    ));
                    order.setPlatformClientId(PlatformClient.BERU.getId());
                },
                "{\"fieldErrors\":[{\"propertyPath\":\"orderContacts\",\""
                    + "message\":\"contact with type RECIPIENT must present\"}]}}"
            ),
            Triple.of(
                "Проверка заполненности waybillSegment.warehouseTo при shipment type = IMPORT",
                order -> order.setWaybill(List.of(createPickupWaybillSegment(
                    new WaybillSegmentDto.ShipmentDto(
                        ShipmentType.IMPORT,
                        LocalDate.of(2019, 8, 11),
                        OffsetDateTime.of(2019, 8, 11, 12, 0, 0, 0, ZoneOffset.of("+03:00")),
                        10800,
                        createLocation(6L),
                        null
                    )
                ).build())),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"waybill[0]\",\"message\":\"must be assigned to a shipment\"},"
                    + "{\"propertyPath\":\"waybill[0].waybillShipment\","
                    + "\"message\":\"warehouseTo must be not null when shipment type is IMPORT\"}]}"
            ),
            Triple.of(
                "Проверка обязательности marketIdFrom",
                order -> order.setMarketIdFrom(null),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"marketIdFrom\",\"message\":\"must not be null\"},"
                    + "{\"propertyPath\":\"waybill[0]\",\"message\":\"must be assigned to a shipment\"}]}"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<String, Consumer<WaybillOrderRequestDto>, String>>
    orderCommitValidationArgumentsPart2() {
        return Stream.of(
            Triple.of(
                "Проверка обязательности waybillSegment.shipment",
                order -> order.setWaybill(List.of(createPickupWaybillSegment(
                    WaybillSegmentDto.ShipmentDto.builder()
                        .type(ShipmentType.IMPORT)
                        .date(LocalDate.of(2019, 3, 3))
                        .locationTo(createLocation(6L))
                        .build()
                ).build())),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"waybill[0]\",\"message\":\"must be assigned to a shipment\"},"
                    + "{\"propertyPath\":\"waybill[0].waybillShipment.locationFrom\",\"message\":\"must not be null\"}"
                    + "]}"
            ),
            Triple.of(
                "Проверка СЦ сегмента",
                order -> order.setWaybill(List.of(
                    createPickupWaybillSegment(
                        WaybillSegmentDto.ShipmentDto.builder()
                            .type(ShipmentType.WITHDRAW)
                            .date(LocalDate.of(2019, 3, 3))
                            .locationFrom(createLocation(6L))
                            .build()
                    )
                        .segmentType(ru.yandex.market.logistics.lom.model.enums.SegmentType.SORTING_CENTER)
                        .build()
                    )
                ),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"\",\"message\":\"waybill with FULFILLMENT "
                    + "or SORTING_CENTER segment MUST contain DELIVERY segment\"},"
                    + "{\"propertyPath\":\"waybill[0].waybillShipment.locationTo\",\"message\":\"must not be null\"}]}"
            ),
            Triple.of(
                "Только начало интервала доставки",
                order -> order.setDeliveryInterval(
                    deliveryInterval().deliveryDateMin(LocalDate.of(2019, 8, 8)).build()
                ),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"deliveryInterval.dateMax\","
                    + "\"message\":\"must not be null\"}]}"
            ),
            Triple.of(
                "Только конец интервала доставки",
                order -> order.setDeliveryInterval(
                    deliveryInterval().deliveryDateMax(LocalDate.of(2019, 9, 9)).build()
                ),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"deliveryInterval.dateMin\","
                    + "\"message\":\"must not be null\"}]}"
            ),
            Triple.of(
                "Артикул айтема состоящий из пробелов",
                order -> order.setItems(List.of(createItem().article("    ").build())),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"items[].article\"," +
                    "\"message\":\"" + OPTIONAL_NOT_BLANK_ERROR_MESSAGE + "\"}]}"
            ),
            Triple.of(
                "Пустой balanceContractId",
                order -> order.setBalanceContractId(null),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"billingEntity.balanceContractId\"," +
                    "\"message\":\"must not be null\"}]}"
            ),
            Triple.of(
                "Пустой balancePersonId",
                order -> order.setBalancePersonId(null),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"billingEntity.balancePersonId\"," +
                    "\"message\":\"must not be null\"}]}"
            ),
            Triple.of(
                "Пустые чарджи",
                order -> order.setCost(createCost().services(null).assessedValue(BigDecimal.ZERO).build()),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"billingEntity.charges\"," +
                    "\"message\":\"must not be empty\"}]}"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<String, Consumer<WaybillOrderRequestDto>, String>> segmentTypeValidation() {
        return Stream.of(
            Triple.of(
                "Незаполненные поля для сегмента SHIPMENT",
                order -> order.setWaybill(List.of(
                    createPickupWaybillSegment(null).build()
                )),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"waybill[0]\",\"message\":\"must be assigned to a shipment\"},"
                    + "{\"propertyPath\":\"waybill[0].waybillShipment\",\"message\":\"must not be null\"},"
                    + "]}"
            ),
            Triple.of(
                "Неверный следующий за FULFILLMENT сегмент",
                order -> order.setWaybill(List.of(
                    createWaybillSegment(ru.yandex.market.logistics.lom.model.enums.SegmentType.FULFILLMENT, 1, 2)
                        .build(),
                    createWaybillSegment(ru.yandex.market.logistics.lom.model.enums.SegmentType.FULFILLMENT, 2, 3)
                        .build()
                ))
                    .setPlatformClientId(PlatformClient.BERU.getId()),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"\",\"message\":\"waybill with FULFILLMENT or SORTING_CENTER"
                    + " segment MUST contain DELIVERY segment\"}"
                    + "]}"
            ),
            Triple.of(
                "Незаполненные поля для сегмента COURIER",
                order -> order
                    .setWaybill(List.of(
                        createWaybillSegment(ru.yandex.market.logistics.lom.model.enums.SegmentType.COURIER, 1, 2)
                            .build()
                    ))
                    .setPlatformClientId(PlatformClient.BERU.getId())
                    .setRecipient(null)
                    .setDeliveryType(DeliveryType.COURIER)
                    .setDeliveryInterval(null),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"deliveryInterval\","
                    + "\"message\":\"Delivery interval must not be null when delivery type is COURIER\"},"
                    + "{\"propertyPath\":\"recipient\","
                    + "\"message\":\"Recipient must not be null when delivery type is COURIER\"}"
                    + "]}"
            ),
            Triple.of(
                "Незаполненные поля для сегмента PICKUP",
                order -> order.setWaybill(List.of(
                    createWaybillSegment(ru.yandex.market.logistics.lom.model.enums.SegmentType.PICKUP, 1, 2).build()
                ))
                    .setPlatformClientId(PlatformClient.BERU.getId())
                    .setDeliveryType(DeliveryType.PICKUP)
                    .setPickupPointId(null),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"pickupPoint\","
                    + "\"message\":\"when delivery type is pickup point, pickupPointId must be not null\"}"
                    + "]}"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<String, Consumer<WaybillOrderRequestDto>, String>> locationTypeValidation() {
        return Stream.of(
            Triple.of(
                "Валидация локации без типа",
                order -> order.setWaybill(List.of(
                    createSegmentWithLocationTo(
                        ru.yandex.market.logistics.lom.model.enums.SegmentType.PICKUP,
                        WaybillSegmentDto.ShipmentDto.builder()
                            .type(ShipmentType.WITHDRAW)
                            .date(LocalDate.of(2020, 6, 4))
                            .locationFrom(createLocation(1L))
                            .locationTo(LocationDto.builder().build())
                            .build()
                    ).build()
                )),
                "{\"fieldErrors\":[{\"propertyPath\":\"waybill[0].waybillShipment.locationTo.type\","
                    + "\"message\":\"must not be null\"}]}"
            ),
            Triple.of(
                "Валидация локации WAREHOUSE",
                order -> order.setWaybill(List.of(createSegmentWithLocationTo(
                    ru.yandex.market.logistics.lom.model.enums.SegmentType.PICKUP,
                    WaybillSegmentDto.ShipmentDto.builder()
                        .type(ShipmentType.WITHDRAW)
                        .date(LocalDate.of(2020, 6, 4))
                        .locationFrom(createLocation(1L))
                        .locationTo(LocationDto.builder().type(LocationType.WAREHOUSE).build())
                        .build()
                    ).build()
                )),
                "{\"fieldErrors\":[{\"propertyPath\":\"waybill[0].waybillShipment.locationTo.warehouseId\","
                    + "\"message\":\"WAREHOUSE and PICKUP segments require logistics point identifier\"}]}"
            ),
            Triple.of(
                "Валидация локации PICKUP",
                order -> order.setWaybill(List.of(createSegmentWithLocationTo(
                    ru.yandex.market.logistics.lom.model.enums.SegmentType.PICKUP,
                    WaybillSegmentDto.ShipmentDto.builder()
                        .type(ShipmentType.WITHDRAW)
                        .date(LocalDate.of(2020, 6, 4))
                        .locationFrom(createLocation(1L))
                        .locationTo(LocationDto.builder().type(LocationType.WAREHOUSE).build())
                        .build()
                    ).build()
                )),
                "{\"fieldErrors\":[{\"propertyPath\":\"waybill[0].waybillShipment.locationTo.warehouseId\","
                    + "\"message\":\"WAREHOUSE and PICKUP segments require logistics point identifier\"}]}"
            ),
            Triple.of(
                "Валидация локации RECIPIENT",
                order -> order.setWaybill(List.of(createSegmentWithLocationTo(
                    ru.yandex.market.logistics.lom.model.enums.SegmentType.PICKUP,
                    WaybillSegmentDto.ShipmentDto.builder()
                        .type(ShipmentType.WITHDRAW)
                        .date(LocalDate.of(2020, 6, 4))
                        .locationFrom(createLocation(1L))
                        .locationTo(LocationDto.builder().type(LocationType.RECIPIENT).build())
                        .build()
                    ).build()
                )),
                "{\"fieldErrors\":[{\"propertyPath\":\"waybill[0].waybillShipment.locationTo.address\","
                    + "\"message\":\"RECIPIENT segment address require region, locality and country at least\"}]}"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<String, Consumer<WaybillOrderRequestDto>, String>> newPlacesValidation() {
        return Stream.of(
            Triple.of(
                "Проверка на уникальность item.article в заказе",
                order -> order
                    .setCost(createCost().itemsSum(BigDecimal.valueOf(400)).total(BigDecimal.valueOf(420)).build())
                    .setItems(List.of(createItem().build(), createItem().build())),
                "{\"fieldErrors\":[{\"propertyPath\":\"items\","
                    + "\"message\":\"" + ITEM_ARTICLES_UNIQUENESS_ERROR_MESSAGE + "\"}]}"

            ),
            Triple.of(
                "Артикул айтема состоящий из пробелов",
                order -> order.setItems(List.of(createItem().article("    ").build())),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"items[].article\"," +
                    "\"message\":\"" + OPTIONAL_NOT_BLANK_ERROR_MESSAGE + "\"}"
                    + "]}"
            ),
            Triple.of(
                "Пустой vendorId у айтема",
                order -> order.setItems(List.of(createItem().vendorId(null).build())),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"items[].vendorId\"," +
                    "\"message\":\"must not be null\"}"
                    + "]}"
            ),
            Triple.of(
                "Пустое имя у айтема",
                order -> order.setItems(List.of(createItem().name(null).build())),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"items[].name\"," +
                    "\"message\":\"must not be blank\"}"
                    + "]}"
            ),
            Triple.of(
                "Имя айтема, состоящее из пробелов",
                order -> order.setItems(List.of(createItem().name("    ").build())),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"items[].name\"," +
                    "\"message\":\"must not be blank\"}"
                    + "]}"
            ),
            Triple.of(
                "Пустое количество товаров у айтема",
                order -> order.setItems(List.of(createItem().count(null).build())),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"cost.itemsSum\"," +
                    "\"message\":\"must not be null\"},"
                    + "{\"propertyPath\":\"cost.total\"," +
                    "\"message\":\"must not be null\"},"
                    + "{\"propertyPath\":\"items[].count\"," +
                    "\"message\":\"must not be null\"}"
                    + "]}"
            ),
            Triple.of(
                "Корневой юнит без размеров",
                order -> order.setUnits(List.of(
                    createUnit("root", null, StorageUnitType.ROOT).dimensions(null).build(),
                    createUnit("child", "root", StorageUnitType.PLACE).dimensions(null).build()
                )),
                "{\"fieldErrors\":["
                    + "{\"propertyPath\":\"units[]\"," +
                    "\"message\":\"root units must have dimensions\"}"
                    + "]}"
            )
        );
    }

    @Test
    @DisplayName("Все числа в order.cost и item.cost отрицательные")
    @DatabaseSetup("/controller/order/commit/before/all_cost_values_are_negative.xml")
    void allCostValuesAreNegative() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, 1L)
            .andExpect(status().isBadRequest())
            .andExpect(content().json(extractFileContent(
                "controller/order/commit/response/all_cost_values_are_negative.json"
            ), true));
    }

    @Nonnull
    private WaybillOrderRequestDto createRequestOrderBuilder() {
        LocalDate deliveryDate = LocalDate.now().plus(10, DAYS);
        WaybillOrderRequestDto orderDto = new WaybillOrderRequestDto()
            .setWaybill(List.of(createPickupWaybillSegment(createWaybillShipment(deliveryDate)).build()));
        orderDto
            .setPlatformClientId(3L)
            .setDeliveryType(DeliveryType.COURIER)
            .setPickupPointId(3L)
            .setDeliveryInterval(
                deliveryInterval()
                    .deliveryDateMin(deliveryDate)
                    .deliveryDateMax(deliveryDate)
                    .fromTime(LocalTime.of(10, 0))
                    .toTime(LocalTime.of(15, 0))
                    .build()
            )
            .setSenderId(4L)
            .setBalanceContractId(1001L)
            .setBalancePersonId(10001L)
            .setReturnSortingCenterId(10L)
            .setSenderBalanceClientId(200L)
            .setSenderBalanceProductId("product-200")
            .setMarketIdFrom(111L)
            .setRecipient(createRecipient(createAddress()))
            .setCredentials(createCredentials())
            .setCost(createCost().build())
            .setComment("order commit validation comment")
            .setItems(List.of(createItem().build()))
            .setContacts(List.of(createOrderContact()))
            .setReturnSortingCenterId(1L)
            .setUnits(List.of(
                createUnit("root", null, StorageUnitType.ROOT).build(),
                createUnit("child", "root", StorageUnitType.PLACE).build()
            ));
        return orderDto;
    }

    @Nonnull
    private static StorageUnitDtoBuilder createUnit(
        String externalId,
        @Nullable String parentExternalId,
        StorageUnitType type
    ) {
        return StorageUnitDto.builder()
            .externalId(externalId)
            .parentExternalId(parentExternalId)
            .type(type)
            .dimensions(createDimensions());
    }

    @Nonnull
    private static WaybillSegmentDtoBuilder createPickupWaybillSegment(
        @Nullable WaybillSegmentDto.ShipmentDto waybillShipment
    ) {
        return WaybillSegmentDto.builder()
            .options(List.of())
            .partnerId(10L)
            .externalId("external waybill segment id")
            .shipment(waybillShipment)
            .requisiteId(5L)
            .segmentType(ru.yandex.market.logistics.lom.model.enums.SegmentType.PICKUP)
            .rootStorageUnitExternalId("root");
    }

    @Nonnull
    private static WaybillSegmentDtoBuilder createWaybillSegment(
        ru.yandex.market.logistics.lom.model.enums.SegmentType type,
        long idFrom,
        long idTo
    ) {
        WaybillSegmentDto.ShipmentDto shipment = WaybillSegmentDto.ShipmentDto.builder()
            .type(null)
            .locationFrom(createLocation(idFrom))
            .locationTo(createLocation(idTo))
            .build();
        return createSegmentWithLocationTo(type, shipment);
    }

    @Nonnull
    private static WaybillSegmentDtoBuilder createSegmentWithLocationTo(
        ru.yandex.market.logistics.lom.model.enums.SegmentType type,
        WaybillSegmentDto.ShipmentDto shipment
    ) {

        return WaybillSegmentDto.builder()
            .options(List.of())
            .partnerId(10L)
            .requisiteId(3L)
            .externalId("external waybill segment id")
            .segmentType(type)
            .shipment(shipment)
            .rootStorageUnitExternalId("root");
    }

    @Nonnull
    private static WaybillSegmentDto.ShipmentDto createWaybillShipment(LocalDate deliveryDate) {
        return WaybillSegmentDto.ShipmentDto.builder()
            .type(ShipmentType.IMPORT)
            .date(deliveryDate)
            .locationFrom(createLocation(6L))
            .locationTo(createLocation(7L))
            .build();
    }

    @Nonnull
    private static LocationDto createLocation(long warehouseId) {
        return LocationDto.builder().type(LocationType.WAREHOUSE).warehouseId(warehouseId).build();
    }

    @Nonnull
    private static ItemDtoBuilder createItem() {
        return ItemDto.builder()
            .name("item name")
            .vendorId(12L)
            .article("item article")
            .count(2)
            .price(createMonetary(100))
            .assessedValue(createMonetary(10))
            .vatType(VatType.NO_VAT)
            .dimensions(createDimensions())
            .boxes(List.of(createBox()));
    }

    @Nonnull
    private static OrderItemBoxDto createBox() {
        return OrderItemBoxDto.builder()
            .dimensions(createDimensions())
            .storageUnitExternalIds(Set.of("child"))
            .build();
    }

    @Nonnull
    private static MonetaryDto createMonetary(int val) {
        return new MonetaryDto("RUB", BigDecimal.valueOf(val), BigDecimal.ONE);
    }

    @Nonnull
    private static RecipientDto createRecipient(@Nullable AddressDto address) {
        return new RecipientDto(
            "lastName",
            "firstName",
            "middleName",
            address,
            "recipient@order.mail",
            1234567890L,
            null,
            null,
            null,
            null
        );
    }

    @Nonnull
    private static AddressDto createAddress() {
        return AddressDto.builder()
            .building("address building")
            .country("address country")
            .federalDistrict("address federal district")
            .geoId(4)
            .house("address house")
            .housing("address housing")
            .intercom("address intercom")
            .latitude(BigDecimal.valueOf(323442))
            .locality("address locality")
            .longitude(BigDecimal.valueOf(31235123))
            .metro("address metro")
            .porch("address porch")
            .room("address room")
            .settlement("address settlement")
            .zipCode("address zip code")
            .street("address street")
            .floor(5)
            .region("address region")
            .subRegion("address sub region")
            .build();
    }

    @Nonnull
    private static KorobyteDto createDimensions() {
        return new KorobyteDto(10, 20, 30, BigDecimal.valueOf(5));
    }

    @Nonnull
    private OrderContactDto createOrderContact() {
        return new OrderContactDto(
            ContactType.RECIPIENT,
            "lastName",
            "firstName",
            "middleName",
            "+79998887766",
            "+78889997766",
            "contact comment",
            "personal-fullname-id",
            "personal-phone-id"
        );
    }

    @Nonnull
    private CredentialsDto createCredentials() {
        return new CredentialsDto(
            "credentials name",
            "credentials incorporation",
            "credentials url",
            "credentials legal form",
            "credentials ogrn",
            "credentials inn",
            "order commit validation address",
            "credentials taxation",
            "order@commit.mail",
            null
        );
    }

    @Nonnull
    private static CostDto.CostDtoBuilder createCost() {
        return CostDto.builder()
            .paymentMethod(PaymentMethod.CASH)
            .cashServicePercent(BigDecimal.valueOf(10))
            .assessedValue(BigDecimal.valueOf(100))
            .amountPrepaid(BigDecimal.ZERO)
            .itemsSum(BigDecimal.valueOf(200))
            .delivery(BigDecimal.valueOf(10))
            .deliveryForCustomer(BigDecimal.valueOf(20))
            .isFullyPrepaid(false)
            .total(BigDecimal.valueOf(220))
            .tariffId(4L)
            .services(List.of(createService(ShipmentOption.INSURANCE)));
    }

    @Nonnull
    private static OrderServiceDto createService(ShipmentOption shipmentOption) {
        return OrderServiceDto.builder()
            .code(shipmentOption)
            .cost(new BigDecimal("45.23"))
            .customerPay(false)
            .build();
    }

    @Nonnull
    private static DeliveryIntervalDto.DeliveryIntervalDtoBuilder deliveryInterval() {
        return DeliveryIntervalDto.builder().deliveryIntervalId(1L);
    }
}
