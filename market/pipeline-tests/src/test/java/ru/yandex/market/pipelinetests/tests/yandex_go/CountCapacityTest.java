package ru.yandex.market.pipelinetests.tests.yandex_go;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import dto.responses.combinator.YcomboParameters;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.market.logistics4go.client.model.ContactDto;
import ru.yandex.market.logistics4go.client.model.Cost;
import ru.yandex.market.logistics4go.client.model.CreateOrderRequest;
import ru.yandex.market.logistics4go.client.model.DeliveryOption;
import ru.yandex.market.logistics4go.client.model.Dimensions;
import ru.yandex.market.logistics4go.client.model.Handing;
import ru.yandex.market.logistics4go.client.model.Inbound;
import ru.yandex.market.logistics4go.client.model.Interval;
import ru.yandex.market.logistics4go.client.model.Item;
import ru.yandex.market.logistics4go.client.model.ItemSupplier;
import ru.yandex.market.logistics4go.client.model.OrderAddressDto;
import ru.yandex.market.logistics4go.client.model.PersonName;
import ru.yandex.market.logistics4go.client.model.Phone;
import ru.yandex.market.logistics4go.client.model.ShipmentType;
import ru.yandex.market.logistics4go.client.model.VatRate;

@DisplayName("Изменение счетчиков капасити в Capacity-Storage")
@Epic("Yandex Go")
@Tag("YandexGoOrderTest")
public class CountCapacityTest extends AbstractYandexGoTest {

    private static final long SORTING_CENTER_ID = 65913;
    private static final long SENDER_WAREHOUSE_ID = 10001090477L;
    private static final long SORTING_CENTER_SEGMENT_ID = 854150L;
    private static final long SENDER_ID = 600000005L;
    private static final int RECIPIENT_GEO_ID = 21642;
    private static final int HEIGHT = 30;
    private static final int LENGTH = 40;
    private static final int WIDTH = 50;
    private static final double WEIGHT_KG = 1.0;
    private static final int WEIGHT_G = 1000;

    @Test
    @DisplayName("Недоступные по капасити даты доезжают до комбинатора")
    void disableDatesInCombinator() {
        // Создаем настройки капасити на СЦ - 1 заказ в день по всей России.
        createCapacityIfNeeded();

        // Создаем заказ.
        OrderDto lomOrder = createOrder();

        // Достаем дату отгрузки с сегмента СЦ.
        LocalDate shipmentDate = lomOrder.getWaybill().stream()
            .filter(segment -> segment.getSegmentType() == SegmentType.SORTING_CENTER)
            .findFirst()
            .map(WaybillSegmentDto::getShipment)
            .map(WaybillSegmentDto.ShipmentDto::getDate)
            .orElseThrow(IllegalStateException::new);

        // Дейофф из CS проставился в Комбинаторе.
        COMBINATOR_STEPS.verifyDateIsDisabledInRoute(
            YcomboParameters.builder()
                .warehouse(SORTING_CENTER_ID)
                .region(RECIPIENT_GEO_ID)
                .weight(WEIGHT_G)
                .dimensions(new Integer[]{LENGTH, HEIGHT, WIDTH})
                .build(),
            shipmentDate
        );

        // Отменяем заказ.
        L4G_STEPS.cancelOrder(lomOrder.getId());

        // Дейофф из CS снялся в Комбинаторе.
        COMBINATOR_STEPS.verifyDateIsEnabledInRoute(
            YcomboParameters.builder()
                .warehouse(SORTING_CENTER_ID)
                .region(RECIPIENT_GEO_ID)
                .weight(WEIGHT_G)
                .dimensions(new Integer[]{LENGTH, HEIGHT, WIDTH})
                .build(),
            shipmentDate
        );
    }

    private void createCapacityIfNeeded() {
        LMS_STEPS.createCapacityInLmsIfItIsNotAvailable(
            CapacityService.SHIPMENT,
            CountingType.ORDER,
            225,
            225,
            SORTING_CENTER_ID,
            null,
            1L,
            null
        );
        CAPACITY_STORAGE_STEPS.snapshot();
    }

    @Nonnull
    private OrderDto createOrder() {
        long lomOrderId = L4G_STEPS.createOrder(createOrderRequest());
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);
        return LOM_ORDER_STEPS.verifyAllSegmentsAreCreated(lomOrderId);
    }

    @Nonnull
    private CreateOrderRequest createOrderRequest() {
        OffsetDateTime inboundDateTime = middayTomorrow();
        LocalDate deliveryDate = inboundDateTime.toLocalDate().plusDays(1);
        return new CreateOrderRequest()
            .senderId(SENDER_ID)
            .externalId(UUID.randomUUID().toString())
            .cost(
                new Cost()
                    .assessedValue(250.0)
                    .deliveryForCustomer(100.0)
                    .paymentMethod(ru.yandex.market.logistics4go.client.model.PaymentMethod.CARD)
            )
            .recipient(
                new ContactDto()
                    .name(
                        new PersonName()
                            .lastName("recipient.lastName")
                            .firstName("recipient.firstName")
                            .middleName("recipient.middleName")
                    )
                    .email("recipient@email.com")
                    .phone(new Phone().number("+7 999 888 7766"))
            )
            .items(List.of(
                new Item()
                    .externalId("item[0].externalId")
                    .name("item[0].name")
                    .count(1)
                    .price(250.0)
                    .assessedValue(250.0)
                    .tax(VatRate.VAT_20)
                    .supplier(new ItemSupplier().inn("7736207543"))
                    .dimensions(
                        new Dimensions()
                            .height(HEIGHT)
                            .length(LENGTH)
                            .width(WIDTH)
                            .weight(WEIGHT_KG)
                    )
            ))
            .deliveryOption(
                new DeliveryOption()
                    .inbound(
                        new Inbound()
                            .type(ShipmentType.IMPORT)
                            .fromLogisticsPointId(SENDER_WAREHOUSE_ID)
                            .toSegmentId(SORTING_CENTER_SEGMENT_ID)
                            .dateTime(inboundDateTime)
                    )
                    .handing(
                        new Handing()
                            .deliveryType(ru.yandex.market.logistics4go.client.model.DeliveryType.COURIER)
                            .address(
                                (OrderAddressDto) new OrderAddressDto()
                                    .geoId(RECIPIENT_GEO_ID)
                                    .latitude(55.715312)
                                    .longitude(38.220477)
                                    .country("Россия")
                                    .region("Московская область")
                                    .locality("Электроугли")
                                    .street("Советская")
                                    .house("13")
                                    .postalCode("142455")
                            )
                            .interval(
                                new Interval()
                                    .start(LocalTime.of(10, 0))
                                    .end(LocalTime.of(19, 0))
                            )
                            .dateMin(deliveryDate)
                            .dateMax(deliveryDate)
                    )
            );
    }

    @Nonnull
    private OffsetDateTime middayTomorrow() {
        return LocalDate.now()
            .plusDays(1)
            .atTime(12, 0)
            .atOffset(ZoneOffset.ofHours(3));
    }
}
