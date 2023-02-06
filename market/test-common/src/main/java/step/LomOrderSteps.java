package step;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import client.LomClient;
import dto.Item;
import dto.requests.report.OfferItem;
import dto.responses.lom.admin.business_process.BusinessProcess;
import dto.responses.lom.admin.order.AdminLomOrderResponse;
import dto.responses.lom.admin.order.LomAdminOrderTag;
import dto.responses.lom.admin.order.OrderTags;
import dto.responses.lom.admin.order.OrdersResponse;
import dto.responses.lom.admin.order.Route;
import dto.responses.lom.admin.order.Values;
import dto.responses.lom.admin.order.route.RouteResponse;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;
import toolkit.exceptions.BreakRetryError;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;

import static toolkit.Retrier.RETRIES_SMALL;

@Slf4j
public class LomOrderSteps {
    private static final Integer WAITING_FOR_CHECKPOINTS_RETRIES = 30;
    private static final Integer WAITING_FOR_CHECKPOINTS_DELAY = 60;

    private static final LomClient LOM_CLIENT = new LomClient();

    @Step("Создание заказа в ломе")
    public OrderDto createLomOrder(OrderDto orderDto) {
        log.debug("Create LOM order");
        WaybillOrderRequestDto waybillOrderRequestDto = new WaybillOrderRequestDto();
        waybillOrderRequestDto.setPlatformClientId(orderDto.getPlatformClientId());
        waybillOrderRequestDto.setSenderId(orderDto.getSenderId());
        waybillOrderRequestDto.setWaybill(orderDto.getWaybill());
        return LOM_CLIENT.createOrder(waybillOrderRequestDto);
    }

    @Step("Ждём, когда заказ создастся в LOM и получаем его data")
    public OrderDto getLomOrderData(Order order) {
        return getLomOrderData(order.getId().toString());
    }

    @Step("Получение информации о заказе из лома по внешнему идентификатору заказа")
    public OrderDto getLomOrderData(String externalOrderId) {
        log.debug("Get LOM order data");
        OrdersResponse orderResult = Retrier.retry(() -> {
            OrdersResponse orderResponse = LOM_CLIENT.orderSearch(externalOrderId);
            Assertions.assertNotNull(
                orderResponse.getData(),
                "Не пришел объект data из лома по externalID = " + externalOrderId
            );
            Assertions.assertFalse(
                orderResponse.getData().isEmpty(),
                "Пришел пустой объект data из лома по externalID = " + externalOrderId
            );
            return orderResponse;
        });

        Assertions.assertEquals(
            orderResult.getData().size(), 1,
            "Количество полученных заказов из LOM не равно 1 для заказа " + externalOrderId
        );

        return orderResult.getData().get(0);
    }

    @Step("Получение информации о заказе из лома по фильтру")
    public OrderDto getLomOrderData(OrderSearchFilter filter) {
        log.debug("Get LOM order data");
        OrdersResponse orderResult = Retrier.retry(() -> {
            OrdersResponse orderResponse = LOM_CLIENT.orderSearch(filter);
            Assertions.assertNotNull(
                orderResponse.getData(),
                "Не пришел объект data из лома по фильтру " + filter
            );
            Assertions.assertFalse(
                orderResponse.getData().isEmpty(),
                "Пришел пустой объект data из лома по фильтру " + filter
            );
            return orderResponse;
        });

        Assertions.assertEquals(
            orderResult.getData().size(), 1,
            "Количество полученных заказов из LOM не равно 1 для фильтра " + filter
        );

        return orderResult.getData().get(0);
    }

    @Step("Получаем lom id по orderId чекаутера")
    public Long getLomOrderId(Order order) {
        log.debug("Get LOM orderId by OrderId {}", order);
        return getLomOrderData(order).getId();
    }

    @Step("Получаем роут по orderId")
    public Route getOrderRoute(Long lomOrderId) {
        log.debug("Wait For Admin LOM Route by LomOrderId {}", lomOrderId);
        return Retrier.retry(() -> {
            log.debug("Get Admin LOM route by LomOrderId {}", lomOrderId);
            RouteResponse routeResponse = LOM_CLIENT.getAdminLomRoute(lomOrderId);
            Route route = routeResponse.getItem().getValues().getRoute();
            Assertions.assertNotNull(route, "Route пустой у заказа " + lomOrderId);
            return route;
        }, RETRIES_SMALL);
    }

    @Step("Получаем вейбилл заказа")
    public List<WaybillSegmentDto> getWaybillSegments(Long lomOrderId) {
        List<WaybillSegmentDto> waybillSegments = Retrier.retry(
            () -> {
                List<WaybillSegmentDto> waybill = LOM_CLIENT.getOrder(lomOrderId).getWaybill();
                Assertions.assertNotNull(waybill, "Пустой waybill у заказа " + lomOrderId);
                Assertions.assertFalse(waybill.isEmpty(), "Пустой waybill у заказа " + lomOrderId);
                if (!LOM_CLIENT.getOrder(lomOrderId).getStatus().equals(OrderStatus.CANCELLED)) {
                    Assertions.assertTrue(
                        waybill.stream()
                            .allMatch(waybillSegmentDto -> waybillSegmentDto.getTrackerId() != null),
                        "Не у всех сегментов проставились trackerId lomId = " + lomOrderId
                    );
                }
                return waybill;
            });
        log.debug("Get waybills {}", waybillSegments.size());
        return waybillSegments;
    }

    @Step("Получаем вейбилл сегменты заказа без проверки наличия trackerId")
    public List<WaybillSegmentDto> getWaybillSegmentsWithoutTrackerId(Long lomOrderId) {
        List<WaybillSegmentDto> waybillSegments = Retrier.retry(
            () -> {
                List<WaybillSegmentDto> waybill = LOM_CLIENT.getOrder(lomOrderId).getWaybill();
                Assertions.assertNotNull(waybill, "Пустой waybill у заказа " + lomOrderId);
                Assertions.assertFalse(waybill.isEmpty(), "Пустой waybill у заказа " + lomOrderId);
                return waybill;
            });
        log.debug("Get waybills {}", waybillSegments.size());
        return waybillSegments;
    }

    @Step("Получаем вейбилл по партнеру")
    public WaybillSegmentDto getWaybillSegmentForPartner(Long lomOrderId, long partnerId) {
        List<WaybillSegmentDto> segments = getWaybillSegments(lomOrderId);
        return segments.stream()
            .filter(w -> w.getPartnerId() == partnerId)
            .findAny()
            .orElseThrow(() -> new AssertionError(
                "Не найден сегмент в заказе " +
                    lomOrderId +
                    " для партнера " +
                    partnerId
            ));
    }

    @Step("Получаем ПВЗ сегмент в схеме onDemand")
    public WaybillSegmentDto getOnDemandPickupWaybillSegment(Long lomOrderId) {
        List<WaybillSegmentDto> segments = getWaybillSegments(lomOrderId);
        return segments.stream()
            .filter(
                waybillSegmentDto -> waybillSegmentDto.getWaybillSegmentTags().contains(WaybillSegmentTag.ON_DEMAND)
            )
            .filter(waybillSegmentDto -> waybillSegmentDto.getSegmentType() == SegmentType.PICKUP)
            .findAny()
            .orElseThrow(() -> new AssertionError("Не найден ПВЗ сегмент в схеме onDemand в заказе " + lomOrderId));
    }

    @Step("Получаем все сегменты заказа в LOM по партнеру")
    public List<WaybillSegmentStatusHistoryDto> getSegmentStatusHistoryByPartner(Long lomOrderId, long partnerId) {
        log.debug("Get lom segment status history by partnerId {}", partnerId);
        WaybillSegmentDto waybillSegment = getWaybillSegmentForPartner(lomOrderId, partnerId);
        return waybillSegment.getWaybillSegmentStatusHistory().stream()
            .sorted(Comparator.comparing(WaybillSegmentStatusHistoryDto::getCreated))
            .collect(Collectors.toList());
    }

    @Step("Получаем все фоновые бизнес процессы по заказу {lomOrderId}")
    public List<BusinessProcess> getAdminBusinessProcesses(Long lomOrderId) {
        return LOM_CLIENT.getAdminBusinessProcesses(lomOrderId).getItems();
    }

    @Step("Получаем фоновый бизнес процесс у заказа {lomOrderId} по типу {queueType}")
    public List<BusinessProcess> getAdminBusinessProcessesWithType(Long lomOrderId, String queueType) {
        return Retrier.retry(() -> {
            List<BusinessProcess> processes = getAdminBusinessProcesses(lomOrderId).stream()
                .filter(
                    process -> process.getValues().getQueueTypes().stream().anyMatch(type -> type.equals(queueType))
                )
                .collect(Collectors.toList());
            Assertions.assertFalse(processes.isEmpty(), "Отсутствуют процессы с типом " + queueType);
            return processes;
        });
    }

    @Step("Проверяем успешное завершение синхронного бизнес процесса типа {queueType} у заказа {lomOrderId}")
    public void verifySyncBusinessProcessSuccess(Long lomOrderId, String queueType) {
        Retrier.retry(() -> {
            List<BusinessProcess> processes = getAdminBusinessProcesses(lomOrderId).stream()
                .filter(
                    process -> process.getValues().getQueueTypes().stream().anyMatch(type -> type.equals(queueType))
                )
                .collect(Collectors.toList());

            Assertions.assertTrue(
                processes.stream().anyMatch(
                    process -> process.getValues().getStatuses().contains("SYNC_PROCESS_SUCCEEDED")
                ),
                "Отсутствуют успешно завершенные процессы с типом " + queueType
            );
        });
    }

    @Step("Перевыставить бизнесс процесс {processId}")
    public void retryBusinessProcess(Long lomOrderId, String queueType) {
        Retrier.retry(() -> {
            List<BusinessProcess> adminBusinessProcessesWithType = getAdminBusinessProcessesWithType(
                lomOrderId,
                queueType
            );
            if (!adminBusinessProcessesWithType
                .stream()
                .allMatch(process -> process.getValues()
                    .getStatuses()
                    .stream()
                    .anyMatch(status -> status.contains("SUCCEEDED"))
                )) {
                LOM_CLIENT.retryBusinessProcess(adminBusinessProcessesWithType.get(0).getId());
            }
        });
    }

    @Step("Проверяем историю статусов сегмента по партнеру {partnerId} и номеру заказа {lomOrderId}")
    public void verifySegmentStatusHistory(Long lomOrderId, long partnerId, List<SegmentStatus> expectedStatuses) {
        Retrier.retry(() -> {
            List<SegmentStatus> actualStatuses = getSegmentStatusHistoryByPartner(lomOrderId, partnerId)
                .stream()
                .map(WaybillSegmentStatusHistoryDto::getStatus)
                .collect(Collectors.toList());
            for (int i = 0; i < actualStatuses.size(); i++) {
                if (!actualStatuses.get(i).equals(expectedStatuses.get(i))) {
                    throw new BreakRetryError(
                        new AssertionError(
                            "Некорректные статусы у заказа " + lomOrderId + " в ломе\n " +
                                "ожидаемые " + expectedStatuses +
                                "\nактуальные " + actualStatuses
                        )
                    );
                }
            }
            Assertions.assertEquals(
                expectedStatuses,
                actualStatuses,
                "Некорректные статусы у заказа " + lomOrderId + " в ломе"
            );
        });
    }

    @Step("Проверяем историю статусов сегмента по партнеру {partnerId} и номеру заказа {lomOrderId}")
    public void verifySegmentStatusCount(
        Long lomOrderId,
        long partnerId,
        SegmentStatus expectedStatus,
        int expectedCount
    ) {
        Retrier.retry(() -> {
            List<SegmentStatus> actualStatuses = getSegmentStatusHistoryByPartner(lomOrderId, partnerId)
                .stream()
                .map(WaybillSegmentStatusHistoryDto::getStatus)
                .collect(Collectors.toList());
            long count = actualStatuses.stream().filter(status -> status.equals(expectedStatus)).count();
            Assertions.assertEquals(
                expectedCount,
                count,
                "Статус " +
                    expectedStatus +
                    " не присутствует в истории заказа " +
                    lomOrderId +
                    " нужное количество раз " +
                    actualStatuses
            );
        });
    }

    @Step("Сравниваем тип сегмента LOM по PartnerId")
    public WaybillSegmentDto verifyWaybillSegmentTypeByPartnerId(
        Long lomOrderId,
        long partnerId,
        SegmentType lomSegmentType
    ) {
        WaybillSegmentDto waybillSegment = getWaybillSegmentForPartner(lomOrderId, partnerId);
        Assertions.assertEquals(lomSegmentType, waybillSegment.getSegmentType(),
            "Некорректный сегмент у заказа " + lomOrderId + " по партнеру " + partnerId);
        return waybillSegment;
    }

    @Step("Ждем, что заказ перешёл в статус {expectedStatus} в LOM")
    public OrderDto verifyOrderStatus(Long lomOrderId, OrderStatus expectedStatus) {
        return Retrier.retry(() -> {
            OrderDto order = LOM_CLIENT.getOrder(lomOrderId);
            OrderStatus status = order.getStatus();
            Assertions.assertEquals(
                expectedStatus,
                status,
                "Статус у заказа " + lomOrderId + " не соответствует ожидаемому"
            );
            return order;
        });
    }

    @Step("Ждем, что заказ отменился с причиной {reason}")
    public OrderDto verifyOrderCancelled(Long lomOrderId, CancellationOrderReason reason) {
        return Retrier.retry(() -> {
            OrderDto order = LOM_CLIENT.getOrder(lomOrderId, Set.of(OptionalOrderPart.CANCELLATION_REQUESTS));
            OrderStatus status = order.getStatus();
            Assertions.assertEquals(
                OrderStatus.CANCELLED,
                status,
                "Статус у заказа " + lomOrderId + " не соответствует ожидаемому"
            );
            Assertions.assertTrue(
                order.getCancellationOrderRequests().stream()
                    .anyMatch(request ->
                        request.getCancellationOrderReason().equals(reason)
                    ),
                "Заказ " + lomOrderId + " отменился не по причине " + reason
            );
            return order;
        });
    }

    //  В метод добавлен костыль для кейса, когда доставка COURIER, а сегмент GO_PLATFORM, подробности в DELIVERY-31091
    @Step("Ждем, что статус сегмента в LOM поменялся на {expectedStatus}")
    public void verifyOrderSegmentStatus(Long lomOrderId, Long partnerId, SegmentStatus expectedStatus) {
        Retrier.retry(
            () -> {
                OrderDto order = LOM_CLIENT.getOrder(lomOrderId);
                WaybillSegmentDto waybillSegment = order.getWaybill().stream()
                    .filter(wbs -> wbs.getPartnerId().equals(partnerId) &&
                        (wbs.getSegmentType().name().equals(order.getDeliveryType().name()) ||
                            (wbs.getSegmentType().name().equals("GO_PLATFORM") &&
                                order.getDeliveryType().name().equals("COURIER"))))
                    .findAny()
                    .orElseThrow(() -> new AssertionError(
                        "Отсутствует сегмент доставки у заказа " + lomOrderId + " по партнеру " + partnerId)
                    );
                Assertions.assertEquals(
                    expectedStatus,
                    waybillSegment.getSegmentStatus(),
                    "Статус сегмента не соответствует ожидаемому"
                );
            },
            WAITING_FOR_CHECKPOINTS_RETRIES,
            WAITING_FOR_CHECKPOINTS_DELAY,
            TimeUnit.SECONDS
        );
    }

    @Step("Ждем, что в истории статусов сегмента партнёра {partnerId} в LOM появился статус {expectedStatus}")
    public void verifyOrderSegmentHasStatusInHistory(Long lomOrderId, Long partnerId, SegmentStatus expectedStatus) {
        Retrier.retry(
            () -> {
                List<WaybillSegmentStatusHistoryDto> segmentStatusHistory = getSegmentStatusHistoryByPartner(
                    lomOrderId,
                    partnerId
                );

                Assertions.assertTrue(
                    segmentStatusHistory.stream().anyMatch(entry -> entry.getStatus() == expectedStatus),
                    "В истории статусов сегмента не найден ожидаемый статус " + expectedStatus
                );
            }
        );
    }

    @Step("Ждем, что статус сегмента партнёра {partnerId} в LOM поменялся на {expectedStatus}")
    public void verifyOrderAnyMileSegmentStatus(Long lomOrderId, Long partnerId, SegmentStatus expectedStatus) {
        Retrier.retry(() -> {
            OrderDto order = LOM_CLIENT.getOrder(lomOrderId);
            WaybillSegmentDto waybillSegment = order.getWaybill().stream()
                .filter(wbs -> wbs.getPartnerId().equals(partnerId))
                .findAny()
                .orElseThrow(() -> new AssertionError(
                    "У заказа " + lomOrderId + " отсутствует сегмент по партнеру " + partnerId)
                );
            Assertions.assertEquals(
                expectedStatus,
                waybillSegment.getSegmentStatus(),
                "Статус сегмента не соответствует ожидаемому"
            );
        });
    }

    @Step("Ждем, что заказ разобьется на 2 коробки в LOM")
    public void verifyOrderHasTwoBoxes(Long lomOrderId) {
        Retrier.retry(() -> {
                List<StorageUnitDto> units = LOM_CLIENT.getOrder(lomOrderId).getUnits();
                Assertions.assertNotNull(units, "В заказе отсутствуют юниты");
                Assertions.assertEquals(3, units.size(), "В заказе " + units.size() + " юнитов");
            }
        );
    }

    @Step("Проверяем наличие заявки на изменение в заказе")
    public OrderDto verifyChangeRequest(
        Long orderId,
        ChangeOrderRequestType type,
        Collection<ChangeOrderRequestStatus> statuses
    ) {
        return Retrier.retry(() -> {
            OrderDto order = LOM_CLIENT.getOrder(orderId, Set.of(OptionalOrderPart.CHANGE_REQUESTS));
            boolean changeRequestExist = Optional.ofNullable(order.getChangeOrderRequests()).stream()
                .flatMap(List::stream)
                .anyMatch(
                    changeRequest -> changeRequest.getRequestType() == type
                        && statuses.contains(changeRequest.getStatus())
                );
            Assertions.assertTrue(
                changeRequestExist,
                "Заявка на изменение заказа не найдена. Заказ " + orderId
            );

            return order;
        });
    }

    @Step("Проверяем наличие trackerId во всех сегментах заказа в LOM")
    public void verifyTrackerIds(long orderId) {
        Retrier.retry(() -> {
            OrderDto order = LOM_CLIENT.getOrder(orderId);
            order.getWaybill().forEach(s -> {
                Assertions.assertNotNull(
                    s.getTrackerId(),
                    "Не найден trackerId для сегмента партнера " + s.getPartnerId()
                );
            });
        });
    }

    @Step("Проверяем изменение даты на последней миле заказа в LOM")
    public void verifyChangeLastMileDeliveryDate(Order order, LocalDate lomDeliveryDateBefore) {
        Retrier.retry(() -> {
            LocalDate lomDeliveryDateAfter = getLomOrderData(order).getWaybill().stream()
                .filter(ws -> ws.getSegmentType() == SegmentType.PICKUP)
                .findAny()
                .orElseGet(() -> Assertions.fail("Не найден сегмент с типом PICKUP"))
                .getShipment().getDate();

            Assertions.assertNotEquals(
                lomDeliveryDateBefore,
                lomDeliveryDateAfter,
                "Дата в сегменте заказа в LOM не изменилась"
            );
        });
    }

    @Step("Проверяем изменение даты заказа в LOM")
    public void verifyChangeLomDeliveryDate(
        Order order,
        LocalDate lomDeliveryDateMinBefore,
        LocalDate lomDeliveryDateMaxBefore
    ) {
        Retrier.retry(() -> {
            LocalDate lomDeliveryDateMinAfter = getLomOrderData(order).getDeliveryInterval().getDeliveryDateMin();
            LocalDate lomDeliveryDateMaxAfter = getLomOrderData(order).getDeliveryInterval().getDeliveryDateMax();

            Assertions.assertNotEquals(
                lomDeliveryDateMaxBefore,
                lomDeliveryDateMaxAfter,
                "dateMax заказа в LOM не изменилась"
            );
            Assertions.assertNotEquals(
                lomDeliveryDateMinBefore,
                lomDeliveryDateMinAfter,
                "dateMin заказа в LOM не изменилась"
            );
        });
    }

    @Step("Проверяем, что время доставки заказа в LOM не изменилось")
    public void verifyDontChangeLomDeliveryTime(
        Order order,
        LocalTime lomDeliveryStartTimeBefore,
        LocalTime lomDeliveryEndTimeBefore
    ) {
        Retrier.retry(() -> {
            LocalTime lomDeliveryStartTimeAfter = getLomOrderData(order).getDeliveryInterval().getFromTime();
            LocalTime lomDeliveryEndTimeAfter = getLomOrderData(order).getDeliveryInterval().getToTime();

            Assertions.assertEquals(
                lomDeliveryEndTimeBefore,
                lomDeliveryEndTimeAfter,
                "endTime заказа в LOM изменилась"
            );
            Assertions.assertEquals(
                lomDeliveryStartTimeBefore,
                lomDeliveryStartTimeAfter,
                "startTime заказа в LOM изменилась"
            );
        });
    }

    @Step("Проверяем, что время доставки заказа в LOM изменилось")
    public void verifyChangeLomDeliveryTime(
        Order order,
        LocalTime lomDeliveryStartTimeBefore,
        LocalTime lomDeliveryEndTimeBefore
    ) {
        Retrier.retry(() -> {
            LocalTime lomDeliveryStartTimeAfter = getLomOrderData(order).getDeliveryInterval().getFromTime();
            LocalTime lomDeliveryEndTimeAfter = getLomOrderData(order).getDeliveryInterval().getToTime();

            Assertions.assertNotEquals(
                lomDeliveryEndTimeBefore,
                lomDeliveryEndTimeAfter,
                "endTime заказа в LOM не изменилась"
            );
            Assertions.assertNotEquals(
                lomDeliveryStartTimeBefore,
                lomDeliveryStartTimeAfter,
                "startTime заказа в LOM не изменилась"
            );
        });
    }

    @Step("Проверяем изменение рута в LOM")
    public void verifyChangeOrderRoute(long orderId, String routeBefore) {
        Retrier.retry(() -> {
            String routeAfter = getOrderRoute(orderId).getRawText();

            Assertions.assertNotEquals(routeBefore, routeAfter, "Рут в заказе в LOM не изменился");
        });
    }

    @Step("Ждем, когда придут instances для всех товаров в заказе")
    public OrderDto verifyInstances(Order order) {
        return Retrier.retry(() -> {
            OrderDto lomOrderData = getLomOrderData(order);
            Assertions.assertTrue(
                lomOrderData
                    .getItems()
                    .stream()
                    .allMatch(item -> item.getInstances() != null),
                "Не пришли instances для товаров в заказе " + order.getId()
            );
            return lomOrderData;
        });
    }

    @Step("Получаем список offerId из ЛОМ для всех товаров в заказе ")
    public List<String> getItemsOfferIds(List<OfferItem> items) {
        return items.stream()
            .flatMap(item -> item.getItems().stream())
            .map(Item::getOfferId)
            .sorted()
            .collect(Collectors.toList());
    }

    @Step("Проверка, что заказ создан на всех сегментах")
    public OrderDto verifyAllSegmentsAreCreated(Long orderId) {
        return Retrier.retry(() -> {
            OrderDto lomOrder = LOM_CLIENT.getOrder(orderId);
            Assertions.assertTrue(
                CollectionUtils.isNonEmpty(lomOrder.getWaybill()),
                String.format("У заказа %d не сгенерирован waybill", orderId)
            );
            Assertions.assertTrue(
                lomOrder
                    .getWaybill()
                    .stream()
                    .filter(segment -> segment.getSegmentType() != SegmentType.NO_OPERATION)
                    .allMatch(segment -> segment.getExternalId() != null && segment.getTrackerId() != null),
                String.format("Заказ %d создан не на всех сегментах", orderId)
            );
            return lomOrder;
        });
    }

    @Step("Проверка, что заказ создан в системе партнера на указанном сегменте")
    public OrderDto verifyOrderCreatedAtPartnerOnWaybillSegment(Order order, Long waybillSegmentId) {
        return Retrier.retry(() -> {
            OrderDto orderDto = getLomOrderData(order);
            Assertions.assertTrue(
                orderDto
                    .getWaybill()
                    .stream()
                    .filter(ws -> ws.getId().equals(waybillSegmentId))
                    .allMatch(ws -> ws.getExternalId() != null && ws.getTrackerId() != null),
                String.format("Заказ %d не создан на сегменте %d", order.getId(), waybillSegmentId)
            );
            return orderDto;
        });
    }

    @Step("Проверка, что у заказа указан код верификации")
    public OrderDto verifyVerificationCodePresent(Long lomOrderId) {
        return Retrier.clientRetry(() -> {
            OrderDto orderDto = LOM_CLIENT.getOrder(lomOrderId);
            Assertions.assertNotNull(orderDto.getRecipientVerificationCode());
            return orderDto;
        });
    }

    @Step("Проверка, что у заказа присутствуют указанные тэги")
    public void verifyOrderTags(Long lomOrderId, Set<LomAdminOrderTag> tags) {
        Retrier.clientRetry(() -> {
            AdminLomOrderResponse orderDto = LOM_CLIENT.getAdminLomOrder(lomOrderId);
            String orderTags = Optional.ofNullable(orderDto)
                .map(AdminLomOrderResponse::getItem)
                .map(dto.responses.lom.admin.order.Item::getValues)
                .map(Values::getTags)
                .map(OrderTags::getText)
                .orElseThrow(() -> new AssertionError("Не найдены тэги для заказа " + lomOrderId));
            tags.forEach(tag -> Assertions.assertTrue(orderTags.contains(tag.getValue())));
        });
    }

    @Step("Проверка, что маршрут заказа поменялся")
    public void verifyRouteChanged(Long lomOrderId, UUID oldRouteUuid) {
        Retrier.retry(() -> {
            UUID actualUuid = LOM_CLIENT.getOrder(lomOrderId)
                .getRouteUuid();
            Assertions.assertNotEquals(oldRouteUuid, actualUuid, "Маршрут заказа не изменился");
        });
    }

    @Step("Проверить, что схема, по которой едет заказ, совпадает с ожидаемой")
    public void verifyOrderSchema(OrderDto order, List<Pair<PartnerType, SegmentType>> expectedSchema) {
        List<Pair<PartnerType, SegmentType>> actualSchema = order.getWaybill().stream()
            .map(waybillSegment -> Pair.of(waybillSegment.getPartnerType(), waybillSegment.getSegmentType()))
            .collect(Collectors.toList());
        Assertions.assertIterableEquals(expectedSchema, actualSchema);
    }

}
