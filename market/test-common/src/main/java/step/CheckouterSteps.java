package step;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import client.CheckouterClient;
import client.TrustClient;
import com.fasterxml.jackson.databind.JsonNode;
import dto.requests.checkouter.Address;
import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.DeliveryDtoRequest;
import dto.responses.checkouter.ReturnResponse;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;
import toolkit.Pair;
import toolkit.Retrier;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryOption;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.pay.BankDetails;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.viewmodel.DeliveryVerificationPart;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderDeliveryViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderViewModel;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;

import static ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest.newDeliveryEditRequest;
import static toolkit.PredicateUtils.loggingPredicate;

/**
 * Шаги для работы с заказом.
 * Для работы со статусами заказов см. OrderStatusSteps.
 */
@Slf4j
public class CheckouterSteps {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private static CheckouterClient checkouterClient = new CheckouterClient();
    private static final TrustClient TRUST_CLIENT = new TrustClient();

    public void setCheckouterClient(CheckouterClient newCheckouterClient) {
        this.checkouterClient = newCheckouterClient;
    }

    @Step("Получение заказа")
    public Order getOrder(Long orderId) {
        return Retrier.clientRetry(() -> checkouterClient.getOrder(orderId));
    }

    public Order createOrder(CreateOrderParameters params) {
        return createOrders(params).get(0);
    }

    @Step("Создание заказа")
    public List<Order> createOrders(CreateOrderParameters params) {
        return checkout(params, cartWithRetry(params));
    }

    @Step("Дергаем cart с ретраями")
    public Delivery cartWithRetry(CreateOrderParameters params) {
        //Этот костыль нужно убрать, как только станет понятно почему не всегда приходят опции доставки
        return Retrier.clientRetry(() -> {
            List<? extends Delivery> deliveries = cart(params);

            Delivery deliveryOption = deliveries
                .stream()
                .filter(loggingPredicate(
                    delOption -> delOption.getType().equals(params.getDeliveryType()),
                    delOption -> log.debug("Skipping deliveryOption: {} as it doesn't have deliveryType = {}",
                        delOption, params.getDeliveryType())))
                .filter(loggingPredicate(
                    delOption -> delOption.getPaymentOptions().stream()
                        .anyMatch(po -> po.equals(params.getPaymentMethod())),
                    delOption -> log.debug("Skipping deliveryOption: {} as it doesn't have paymentMethod = {}",
                        delOption, params.getPaymentMethod())))
                .filter(loggingPredicate(
                    delOption -> params.getDeliveryPredicate().test(delOption),
                    delOption -> log.debug("Skipping deliveryOption: {} as it doesn't match the condition", delOption)))
                .findAny()
                .orElseThrow(() -> new AssertionError("Отсутствует в cart нужный deliveryOption"));

            Assertions.assertNotNull(deliveryOption.getHash(), "Отсутствует hash у deliveryOption");
            return deliveryOption;
        });
    }

    @Step("Дергаем cart")
    public List<? extends Delivery> cart(CreateOrderParameters params) {
        return Retrier.clientRetry(() -> {

            MultiCart multiCart = checkouterClient.cart(
                params.getItems(),
                params.getRegionId(),
                params.getPaymentType(),
                params.getPaymentMethod(),
                params.getExperiment(),
                params.getForceDeliveryId(),
                params.getAddress(),
                params.getRgb()
            );
            Assertions.assertNotNull(multiCart.getCarts(), "Пустой список carts");

            Order order = multiCart.getCarts()
                .stream()
                .findAny()
                .orElseThrow(() -> new AssertionError("Отсутствует order в cart"));
            Assertions.assertNotNull(order.getDeliveryOptions(), "Отсутствуют delivery options");

            return order.getDeliveryOptions();
        });

    }

    @Step("Дергаем checkout")
    public List<Order> checkout(CreateOrderParameters params, Delivery delivery) {
        MultiOrder multiOrder = Retrier.clientRetry(() -> {
            DeliveryDtoRequest deliveryParam = createDeliveryRequestParam(
                params.getRegionId(),
                delivery,
                params.getDeliveryInterval(),
                params.getAddress(),
                params.getDeliveryType(),
                params.getRgb()
            );

            MultiOrder multiOrderResp = checkouterClient.checkout(
                params.getItems(),
                params.getRegionId(),
                deliveryParam,
                params.getPaymentType(),
                params.getPaymentMethod(),
                params.getExperiment(),
                params.getForceDeliveryId(),
                params.getFake(),
                params.getComment(),
                params.getBuyer(),
                params.getRgb()
            );

            Assertions.assertTrue(multiOrderResp.isValid(), "Поле checkedOut не true после checkout " + delivery);
            Assertions.assertNotNull(multiOrderResp.getOrders(), "Пустое поле заказов при чекауте");
            Assertions.assertTrue(multiOrderResp.getOrders().size() > 0, "Не создался заказ после checkout");
            return multiOrderResp;
        });
        return multiOrder.getOrders();
    }

    private DeliveryDtoRequest createDeliveryRequestParam(
        Long regionId,
        Delivery delivery,
        Pair<LocalTime, LocalTime> interval,
        Address address,
        DeliveryType deliveryType,
        Color rgb
    ) {
        DeliveryDtoRequest.DeliveryDtoRequestBuilder deliveryDtoRequestBuilder = DeliveryDtoRequest.builder()
            .hash(delivery.getHash())
            .regionId(regionId);
        if (deliveryType.equals(DeliveryType.DELIVERY)) {
            if (rgb.equals(Color.WHITE)) {
                return deliveryDtoRequestBuilder.address(address.getAddress())
                    .dates(DeliveryDtoRequest.DeliveryDates.builder()
                        .fromDate(DATE_FORMAT.format(delivery.getDeliveryDates().getFromDate()))
                        .build())
                    .build();
            } else {
                Assertions.assertFalse(delivery.getRawDeliveryIntervals().isEmpty(), "Интервалы доставки пусты");
                Assertions.assertFalse(
                    delivery.getRawDeliveryIntervals().getForJson().isEmpty(),
                    "Отсутствуют интервалы доставки"
                );
                Stream<RawDeliveryInterval> stream = delivery.getRawDeliveryIntervals()
                    .getForJson()
                    .stream()
                    .flatMap(json -> json.getIntervals().stream());
                if (interval != null) {
                    stream = stream.filter(in ->
                        in.getFromTime().equals(interval.getFirst()) &&
                            in.getToTime().equals(interval.getSecond())
                    );
                }
                RawDeliveryInterval rawDeliveryInterval = stream.findAny()
                    .orElseThrow(() -> new AssertionError(
                        "Отсутствуют интервалы у " + (interval == null ? delivery : interval))
                    );
                return deliveryDtoRequestBuilder.address(address.getAddress())
                    .dates(DeliveryDtoRequest.DeliveryDates.builder()
                        .fromDate(DATE_FORMAT.format(delivery.getDeliveryDates().getFromDate()))
                        .fromTime(rawDeliveryInterval.getFromTime().toString())
                        .toTime(rawDeliveryInterval.getToTime().toString())
                        .build())
                    .build();
            }
        } else {
            Assertions.assertNotNull(delivery.getOutlets(), "Пустые outlet в опции доставки");
            Assertions.assertFalse(delivery.getOutlets().isEmpty(), "Пустые outlet в опции доставки");
            return deliveryDtoRequestBuilder
                .outletId(delivery.getOutlets().get(0).getId())
                .dates(DeliveryDtoRequest.DeliveryDates.builder()
                    .fromDate(DATE_FORMAT.format(delivery.getDeliveryDates().getFromDate()))
                    .build()
                )
                .build();
        }
    }

    public void payOrder(Order order) {
        payOrder(order, OrderStatus.PROCESSING);
    }

    @Step("Оплата заказа")
    public void payOrder(Order order, OrderStatus status) {
        Retrier.retry(() -> {
            String purchaseToken = checkouterClient.pay(order);
            if (order.getPaymentType().equals(PaymentType.PREPAID)) {
                checkouterClient.notifyFake(purchaseToken);
            } else {
                TRUST_CLIENT.supplyPaymentData(purchaseToken);
            }
        });
        Retrier.retry(
            () -> Assertions.assertEquals(status, checkouterClient.getOrder(order.getId()).getStatus(),
                "Статус не PROCESSING после оплаты заказа " + order.getId())
        );
    }

    @Step("Отмена заказа")
    public void cancelOrder(Order order) {
        if (order != null) {
            checkouterClient.cancellationRequestByCallCenterOperator(order.getId(), order.getShopId());
        }
    }

    @Step("Отмена заказа, если доступно")
    public void cancelOrderIfAllowed(Order order) {
        if (order == null) {
            return;
        }
        try {
            checkouterClient.cancellationRequestByCallCenterOperator(order.getId(), order.getShopId());
        } catch (Exception ignored) {
        }
    }

    @Step("Отмена заказа пользователем")
    public void cancelOrderByUser(Order order) {
        if (order == null) {
            return;
        }
        try {
            checkouterClient.cancellationRequestByUser(order.getId(), order.getShopId());
        } catch (AssertionFailedError afe) {
            Retrier.clientRetry(() -> checkouterClient.checkCancellationRequestExits(order.getId()));
        }
    }

    public void tearDownOrders(List<Order> orders) {
        for (Order order : orders) {
            cancelOrderIfAllowed(order);
        }
    }

    @Step("Переводим заказ в статус Готов к отправке")
    public void shipDropshipOrder(Order order) {
        Retrier.clientRetry(() -> checkouterClient.changeOrderStatus(
            order.getId(),
            null,
            OrderStatus.PROCESSING,
            OrderSubstatus.READY_TO_SHIP
        ));
    }

    @Step("Переводим заказ в статус {status} и подстатус {subStatus}")
    public void changeOrderStatusAndSubStatus(
        long orderId,
        Long shopId,
        OrderStatus status,
        OrderSubstatus subStatus
    ) {
        Retrier.clientRetry(() -> checkouterClient.changeOrderStatus(
            orderId,
            shopId,
            status,
            subStatus
        ));
    }

    @Step("Переводим заказ в статус {status} и подстатус {subStatus} под ролью SHOP - магазин через API")
    public void changeOrderStatusAndSubStatusByShop(
        Order order,
        OrderStatus status,
        OrderSubstatus subStatus
    ) {
        Retrier.clientRetry(() -> checkouterClient.changeOrderStatus(
            ClientRole.SHOP.name(),
            order.getShopId(),
            order.getId(),
            order.getShopId(),
            status,
            subStatus
        ));
    }

    @Step("Переводим заказ в статус {status} и подстатус {subStatus} под ролью SHOP_USER - магазин через ПИ")
    public void changeOrderStatusAndSubStatusByShopUser(
        Order order,
        OrderStatus status,
        OrderSubstatus subStatus
    ) {
        Retrier.clientRetry(() -> checkouterClient.changeOrderStatus(
            ClientRole.SHOP_USER.name(),
            order.getShopId(),
            order.getId(),
            order.getShopId(),
            status,
            subStatus
        ));
    }

    @Step("Получаем дату отгрузки для заказа")
    public LocalDate getOrderShipmentDate(Order order) {
        log.debug("Get order shipment date");
        return Retrier.retry(() -> {
            LocalDate shipmentDate = checkouterClient.getOrder(order.getId())
                .getDelivery()
                .getParcels()
                .stream()
                .findAny()
                .orElseThrow(() -> new AssertionError("Нет ни одной посылки у заказа"))
                .getShipmentDate();
            Assertions.assertNotNull(shipmentDate, "Пустая дата отправки у заказа " + order.getId());
            return shipmentDate;
        }, Retrier.RETRIES_SMALL);
    }

    //Получаем поле shipmentDateTimeBySupplier из заказа
    @Step("Получаем дату отгрузки поставщиком для заказа")
    public LocalDateTime getOrderShipmentDateTimeBySupplier(Order order) {
        log.debug("Get order shipment date time by supplier");
        return Retrier.retry(() -> {
                long orderId = order.getId();
                LocalDateTime shipmentDateTimeBySupplier = checkouterClient.getOrder(orderId)
                    .getDelivery()
                    .getParcels()
                    .stream()
                    .findAny()
                    .orElseThrow(() -> new AssertionError(String.format("Нет ни одной посылки у заказа %d", orderId)))
                    .getShipmentDateTimeBySupplier();
                Assertions.assertNotNull(
                    shipmentDateTimeBySupplier,
                    String.format("Пустая дата отправки у заказа %d", orderId)
                );
                return shipmentDateTimeBySupplier;
            },
            Retrier.RETRIES_SMALL
        );
    }

    @Step("Получаем дату отгрузки для доставки")
    public Delivery getEarliestDeliveryOption(CreateOrderParameters params) {
        log.debug("Get delivery date");
        return Retrier.retry(() -> cart(params)
            .stream()
            .filter(loggingPredicate(
                delOption -> delOption.getType().equals(params.getDeliveryType()),
                delOption -> log.debug("Skipping deliveryOption: {} as it doesn't have deliveryType = {}",
                    delOption, params.getDeliveryType())))
            .filter(loggingPredicate(
                delOption -> delOption.getPaymentOptions().stream()
                    .anyMatch(po -> po.equals(params.getPaymentMethod())),
                delOption -> log.debug(
                    "Skipping deliveryOption: {} as it doesn't have paymentMethod = {}",
                    delOption,
                    params.getPaymentMethod()
                )
            ))
            .min(Comparator.comparing(delivery -> delivery.getDeliveryDates().getFromDate()))
            .orElseThrow(() -> new AssertionError("Отсутствует дата доставки")), Retrier.RETRIES_SMALL);
    }

    public DeliveryDates verifyDeliveryShipmentDateChanged(
        DeliveryDates oldDeliveryDates,
        CreateOrderParameters params,
        boolean isAfter
    ) {
        return Retrier.retry(() -> {
            DeliveryDates actualDate = getEarliestDeliveryOption(params).getDeliveryDates();
            Assertions.assertEquals(
                isAfter,
                actualDate.getToDate().after(oldDeliveryDates.getToDate()),
                "" +
                    "Не изменилась дата доставки не стала " +
                    (isAfter ? "после" : "раньше") +
                    " даты\n" +
                    oldDeliveryDates.getToDate() +
                    "\nтекущая дата\n" +
                    actualDate.getToDate()
            );
            return actualDate;
        });
    }

    @Step("Получаем id ПВЗ для заказа {order.orderId}")
    public String getOutletId(Order order) {
        log.debug("Receive outletId");
        return Retrier.retry(
            () -> checkouterClient.getOrder(order.getId()).getDelivery().getOutletId().toString(),
            Retrier.RETRIES_SMALL);
    }

    @Step("Получаем КИЗ для первого товара из заказа")
    public String getCis(Order order) {
        log.debug("Receive CIS");
        return Retrier.retry(() -> {
                Order getOrder = checkouterClient.getOrder(order.getId());
                Assertions.assertNotNull(getOrder.getItems(), "Нет итемов у заказа " + order.getId());
                OrderItem item = getOrder
                    .getItems()
                    .stream()
                    .findAny()
                    .orElseThrow(() -> new AssertionError("Нет итемов у заказа " + order.getId()));
                Assertions.assertNotNull(item.getInstances(), "Нет instances у заказа " + order.getId());
                JsonNode cisValue = item.getInstances().findValue(OrderItemInstance.InstanceType.CIS.getName());
                Assertions.assertNotNull(cisValue, "Отсутствует сis у заказа " + order.getId());
                return cisValue.asText();
            },
            Retrier.RETRIES_SMALL
        );
    }

    @Step("Получаем UIT для i-го товара из заказа")
    public String getUit(Order order, int i) {
        log.debug("Receive UIT");
        return Retrier.retry(() -> {
                ArrayList<OrderItem> orderItems = new ArrayList<>(checkouterClient.getOrder(order.getId())
                    .getItems());
                Assertions.assertTrue(
                    orderItems.size() >= i,
                    "Товаров в заказе " + order.getId() + " меньше, чем " + i
                );
                Assertions.assertNotNull(orderItems.get(i), "Нет " + i + "-го товара в заказе " + order.getId());
                Assertions.assertNotNull(orderItems.get(i).getInstances(), "Нет instances у заказа " + order.getId());
                JsonNode uitValue = orderItems.get(i)
                    .getInstances()
                    .findValue(OrderItemInstance.InstanceType.UIT.getName());
                Assertions.assertNotNull(uitValue, "Нет UIT у заказа " + order.getId());
                return uitValue.asText();
            },
            Retrier.RETRIES_SMALL
        );
    }

    @Step("Получаем товары из заказа")
    public List<OrderItem> getItems(Order order) {
        log.debug("Receive items");

        return Retrier.retry(
            () -> checkouterClient.getOrder(order.getId())
                .getItems()
                .stream()
                .sorted(Comparator.comparing(OrderItem::getId))
                .collect(Collectors.toList()),
            Retrier.RETRIES_SMALL
        );
    }

    @Step("Получаем трек номера заказа от СД и/или от СЦ для заказа")
    public Track getTrackNumbers(Order order, DeliveryServiceType deliveryTrackType) {
        List<Parcel> parcels = Retrier.retry(() -> {
            Order getOrder = getOrder(order.getId());
            Assertions.assertNotNull(getOrder.getDelivery(), "Пустой объект delivery у заказа " + getOrder.getId());
            List<Parcel> getParcels = getOrder.getDelivery().getParcels();
            Assertions.assertFalse(getParcels.isEmpty(), "Список посылок пустой у заказа " + getOrder.getId());
            Assertions.assertTrue(getParcels.stream().allMatch(p -> p.getTracks() != null),
                "Пустые треки у посылок у заказа " + getOrder.getId());
            return getParcels;
        });

        return parcels
            .stream()
            .flatMap(parcel -> parcel.getTracks().stream())
            .filter(tr -> tr != null && tr.getDeliveryServiceType().equals(deliveryTrackType))
            .findAny()
            .orElseThrow(() -> new AssertionError("У посылок отсутствует трек доставки с типом " + deliveryTrackType +
                " заказ = " + order.getId()));
    }

    @Step("Ждем, что заказ перешёл в статус {status}")
    public void verifyForOrderStatus(Order order, OrderStatus status) {
        Retrier.retry(() -> Assertions.assertEquals(status, checkouterClient.getOrder(order.getId()).getStatus(),
            "Заказ не перешел в статус " + status));
    }

    @Step("Ждем, что заказ перешёл в подстатус {subStatus}")
    public void verifyForOrderSubStatus(Order order, OrderSubstatus subStatus) {
        Retrier.retry(() -> Assertions.assertEquals(subStatus, checkouterClient.getOrder(order.getId()).getSubstatus(),
            "Заказ не перешел в subStatus " + subStatus));
    }

    @Step("Ждем, что заказ перешёл в статус {status} и подстатус {substatus}")
    public void verifyOrderStatusAndSubstatus(Order order, OrderStatus status, OrderSubstatus substatus) {
        Retrier.retry(() -> {
            long orderId = order.getId();
            Order updatedOrder = checkouterClient.getOrder(orderId);
            Assertions.assertEquals(
                status,
                updatedOrder.getStatus(),
                String.format("Заказ %d не перешел в status %s", orderId, status)
            );
            Assertions.assertEquals(
                substatus,
                updatedOrder.getSubstatus(),
                String.format("Заказ %d не перешел в substatus %s", orderId, substatus)
            );
        });
    }

    @Step("Ждем, что заказ получит новые даты доставки")
    public void verifyForOrderHasNewDates(Long orderId, LocalDate date) {
        Retrier.retry(() -> {
                Order order = checkouterClient.getOrder(orderId);

                Assertions.assertNotNull(order.getDelivery(), "Доставка пустая у заказа id = " + orderId);
                DeliveryDates deliveryDates = order.getDelivery().getDeliveryDates();
                Assertions.assertNotNull(deliveryDates, "Пустые даты доставки у заказа id = " + orderId);
                Assertions.assertNotNull(
                    deliveryDates.getFromDate(),
                    "Пустая дата доставки ОТ у заказа id = " + orderId
                );
                Assertions.assertNotNull(
                    deliveryDates.getToDate(),
                    "Пустая дата доставки ДО у заказа id = " + orderId
                );
                Assertions.assertEquals(date, deliveryDates.getFromDate().toInstant().atZone(ZoneId.systemDefault())
                        .toLocalDate(),
                    "Некорректная fromDate у заказа " + order.getId());
                Assertions.assertEquals(date, deliveryDates.getToDate().toInstant().atZone(ZoneId.systemDefault())
                        .toLocalDate(),
                    "Некорректная toDate у заказа " + order.getId());
            }
        );
    }

    @Step("Ждем, когда заказ получит трек от СЦ и он создастся в Трекере")
    public void verifyFFTrackCreated(Order order) {
        Retrier.retry(() -> {
            Track track = verifyTrack(order.getId(), DeliveryServiceType.FULFILLMENT);
            Assertions.assertNotNull(
                track.getTrackerId(),
                "Пустой trackerId у посылки заказа " + order.getId() + " с типом " + DeliveryServiceType.FULFILLMENT
            );
        });
    }

    public void verifySDTracksCreated(Order order) {
        verifySDTracksCreated(order, true);
    }

    @Step("Ждем, когда заказ получит трек от СД и он создастся в Трекере")
    public void verifySDTracksCreated(Order order, boolean withTrackerId) {
        Retrier.retry(
            () -> {
                Track track = verifyTrack(order.getId(), DeliveryServiceType.CARRIER);
                Assertions.assertEquals(order.getDelivery().getDeliveryServiceId(), track.getDeliveryServiceId(),
                    "Некорректный deliveryServiceId посылки заказа " + order.getId());
                if (withTrackerId) {
                    Assertions.assertNotNull(
                        track.getTrackerId(),
                        "" +
                            "Пустой trackerId у посылки заказа " +
                            order.getId() +
                            " с типом " +
                            DeliveryServiceType.CARRIER
                    );
                }
            },
            Retrier.RETRIES_BIG
        );
    }

    @Step("Ждем, что у заказа появится определенный чекпоинт в Чекаутере")
    public void verifyForCheckpointReceived(
        Order order,
        OrderDeliveryCheckpointStatus checkpoint,
        DeliveryServiceType deliveryTrackType
    ) {
        Retrier.retry(
            () -> {
                log.debug("Checking if checkpoint {} is recieved by checkouter", checkpoint.getId());
                Order orderGet = checkouterClient.getOrder(order.getId());
                Track track = getTrackNumbers(orderGet, deliveryTrackType);
                track.getCheckpoints()
                    .stream()
                    .filter(cp -> cp.getDeliveryCheckpointStatus().equals(checkpoint.getId()))
                    .findAny()
                    .orElseThrow(() -> new AssertionError(
                        "Отсутствует чекпоинт с нужным статусом " +
                            checkpoint.getId() +
                            " номер заказа "
                            + order.getId())
                    );
            }
        );
    }

    @Step("Проверяем количество товаров в заказе")
    public void verifyItemsCount(Long orderId, int expectedCount) {
        Retrier.retry(() -> {
            Order order = checkouterClient.getOrder(orderId);
            Assertions.assertEquals(expectedCount, checkouterClient.getOrder(order.getId()).getItems().size(),
                "Количество товаров в заказе не соответствует ожидаемому. Заказ " + order.getId()
            );
        });
    }

    @Step("Проверяем количество заявок на изменение в заказе")
    public Order verifyChangeRequests(Long orderId, ChangeRequestType type, int expectedCount) {
        return Retrier.retry(() -> {
            Order order = checkouterClient.getOrder(
                orderId,
                new OptionalOrderPart[]{OptionalOrderPart.CHANGE_REQUEST}
            );
            long actualCount = Optional.ofNullable(order.getChangeRequests()).stream()
                .flatMap(List::stream)
                .filter(changeRequest -> changeRequest.getType() == type)
                .count();
            Assertions.assertEquals(
                expectedCount, actualCount,
                "Количество заявок на изменение в заказе не соответствует ожидаемому. Заказ " + orderId
            );

            return order;
        });
    }

    @Step("Проверяем deliveryFeature у заказа")
    public void verifyOrderHasDeliveryFeature(Order order, DeliveryFeature deliveryFeature) {
        Retrier.retry(() -> {
                Order getOrder = checkouterClient.getOrder(order.getId());
                Assertions.assertNotNull(
                    getOrder.getDelivery().getFeatures(),
                    "Пустая delivery features у заказа " + order.getId()
                );
                Assertions.assertTrue(getOrder.getDelivery().getFeatures().contains(deliveryFeature),
                    "Отсутствует delivery feature " + deliveryFeature + " у заказа id = " + order.getId());
            }

        );
    }

    @Step("Обновляем DeliveryService в заказе")
    public Order updateDeliveryServiceId(Order order, long deliveryServiceId) {
        return Retrier.clientRetry(() -> checkouterClient.updateDeliveryService(
            order.getId(),
            order.getShopId(),
            deliveryServiceId,
            order.getRgb()
        ));
    }

    @Step("Удаляем товары в заказе")
    public void removeItems(Order order) {
        List<ItemInfo> remainedItems = order.getItems().stream()
            .map(item -> new ItemInfo(item.getId(), item.getCount(), null))
            .collect(Collectors.toList());
        MissingItemsNotification missingItemsNotification = new MissingItemsNotification(
            true,
            remainedItems,
            HistoryEventReason.ITEMS_NOT_FOUND,
            true
        );

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setMissingItemsNotification(missingItemsNotification);
        Retrier.clientRetry(() -> checkouterClient.edit(
            order.getId(),
            Color.BLUE,
            null,
            null,
            orderEditRequest
        ));
    }

    public void changeDeliveryDate(Order order) {
        changeDeliveryDateAndTime(order, 0);
    }

    @Step(
        "Изменяем дату и время доставки в чекаутере на следующую доступную дату и время, " +
            "пропуская первые доступные временные интервалы ({numberSkippedIntervals} шт.)"
    )
    public void changeDeliveryDateAndTime(Order order, int numberSkippedIntervals) {
        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        //получаем опции изменения заказа
        OrderEditOptions orderEditOptions = checkouterClient.getEditOptions(
            order.getId(),
            Color.BLUE,
            null,
            null,
            orderEditOptionsRequest
        );

        DeliveryOption deliveryOption = orderEditOptions.getDeliveryOptions().stream().skip(1).findFirst()
            .orElse(orderEditOptions.getDeliveryOptions().stream().iterator().next());

        DeliveryEditRequest deliveryEditRequest = newDeliveryEditRequest()
            .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
            .deliveryServiceId(deliveryOption.getDeliveryServiceId())
            .fromDate(deliveryOption.getFromDate())
            .toDate(deliveryOption.getToDate())
            .shipmentDate(deliveryOption.getShipmentDate())
            .timeInterval(CollectionUtils.emptyIfNull(deliveryOption.getTimeIntervalOptions()).stream()
                .skip(numberSkippedIntervals)
                .findFirst()
                .orElse(null))
            .build();

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(deliveryEditRequest);

        Retrier.clientRetry(() -> checkouterClient.edit(
            order.getId(),
            Color.BLUE,
            null,
            null,
            orderEditRequest
        ));
    }

    private Track verifyTrack(Long orderId, DeliveryServiceType type) {
        Order order = checkouterClient.getOrder(orderId);
        Track track = getTrackNumbers(order, type);
        Assertions.assertNotNull(track, "Пустой трек у посылки заказа " + orderId);
        Assertions.assertNotNull(track.getTrackCode(), "Пустой трек код у посылки заказа " + orderId);
        return track;
    }

    @Step("Проверяем изменение даты заказа в чекаутер")
    public void verifyChangeDeliveryDate(Order order, LocalDate cpaDeliveryDateBefore) {
        Retrier.retry(() -> {
            Order cpaOrder = getOrder(order.getId());
            LocalDate cpaDeliveryDateAfter = cpaOrder.getDelivery().getDeliveryDates().getToDate()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

            Assertions.assertNotEquals(
                cpaDeliveryDateBefore,
                cpaDeliveryDateAfter,
                "Дата в заказе в CPA не изменилась"
            );
        });
    }

    @Step("Проверка кода подтверждения передачи заказа в чекаутере")
    public void verifyCode(Long orderId, String code) {
        Retrier.clientRetry(() -> {
            OrderViewModel cpaOrder = checkouterClient.getOrderViewModel(
                orderId,
                new OptionalOrderPart[]{OptionalOrderPart.DELIVERY_VERIFICATION_CODE}
            );
            Optional.ofNullable(cpaOrder.getDelivery())
                .map(OrderDeliveryViewModel::getVerificationPart)
                .map(DeliveryVerificationPart::getVerificationCode)
                .ifPresentOrElse(verificationCode -> Assertions.assertEquals(
                    code,
                    verificationCode,
                    "Код верификации в заказе в ЛОМе и в чекаутере не совпадает"
                    ),
                    () -> {
                        throw new AssertionError("Код верификации не найден в заказе чекаутера");
                    }
                );
        });
    }

    @Step("Получаем offerId всех товаров в заказе в чекаутере")
    public List<String> getItemsFromCheckouterOfferIds(Order order) {
        return order.getItems().stream()
            .map(OrderItem::getOfferId)
            .sorted()
            .collect(Collectors.toList());
    }

    @Step("Формируем айтемы для возврата")
    public List<ReturnItem> initReturnItems(Order order, boolean isFull) {
        //Берем айтемы из заказа и собираем из них айтемы для возврата

        Collection<OrderItem> itemFromOrder = order.getItems();

        //полный возврат
        if (isFull) {
            return itemFromOrder.stream().map(item -> {
                    ReturnItem returnItem = new ReturnItem();
                    returnItem.setItemId(item.getId());
                    returnItem.setCount(item.getCount());
                    returnItem.setReturnReason("Не хочу");
                    returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
                    return returnItem;
                }
            ).collect(Collectors.toList());
        }

        //частичный возврат
        return itemFromOrder.stream().findAny().map(item -> {
                ReturnItem returnItem = new ReturnItem();
                returnItem.setItemId(item.getId());
                returnItem.setCount(1);
                returnItem.setReturnReason("Не хочу");
                returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
                return returnItem;
            }
        ).stream().collect(Collectors.toList());
    }

    @Step("Формируем и вызываем возврат заказа из чекаутера")
    public ReturnResponse initReturn(
        Order order,
        long dSForReturn,
        long outletIdForReturn,
        List<ReturnItem> returnItem
    ) {

        //Собираем delivery для возврата
        ReturnDelivery delivery = new ReturnDelivery();
        delivery.setDeliveryServiceId(dSForReturn);
        delivery.setOutletId(outletIdForReturn);
        delivery.setType(DeliveryType.PICKUP);
        delivery.setRegionId(order.getDelivery().getRegionId());

        //скопировано из тестов чекаутера
        BankDetails bankDetails = new BankDetails(
            "00000000000000000009",
            "0123456789",
            "000000009",
            "bank",
            "bankCity",
            "firstName",
            "lastName",
            "middleName",
            null
        );

        //собираем тело запроса return
        Return returnOrder = new Return();
        returnOrder.setItems(returnItem);
        returnOrder.setDelivery(delivery);
        returnOrder.setBankDetails(bankDetails);
        returnOrder.setUserId(order.getBuyer().getUid());

        return Retrier.clientRetry(() -> checkouterClient.initReturn(returnOrder, order.getId()));
    }

    @Step("Проверить, что дата отгрузки поставщиком изменилась")
    public void verifyShipmentDateTimeBySupplierChanged(
        Order order,
        LocalDateTime orderShipmentDateTimeBySupplierBefore
    ) {
        Retrier.retry(() -> {
            LocalDateTime actualShipmentDateTimeBySupplier = getOrderShipmentDateTimeBySupplier(order);
            Assertions.assertTrue(
                actualShipmentDateTimeBySupplier.isAfter(orderShipmentDateTimeBySupplierBefore),
                "Дата отгрузки не изменилась"
            );
        });
    }
}
