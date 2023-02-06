package ru.yandex.market.pharmatestshop.domain.order.status;


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

import static ru.yandex.market.pharmatestshop.domain.order.status.OrderStatus.CANCELLED;
import static ru.yandex.market.pharmatestshop.domain.order.status.OrderStatus.DELIVERED;
import static ru.yandex.market.pharmatestshop.domain.order.status.OrderStatus.DELIVERY;
import static ru.yandex.market.pharmatestshop.domain.order.status.OrderStatus.PENDING;
import static ru.yandex.market.pharmatestshop.domain.order.status.OrderStatus.PICKUP;
import static ru.yandex.market.pharmatestshop.domain.order.status.OrderStatus.PROCESSING;
import static ru.yandex.market.pharmatestshop.domain.order.status.OrderStatus.UNPAID;

public enum OrderSubstatus {
    // Whatever you do, DO NOT CHANGE THE ORDER!
    //      "Покупатель не завершил вовремя оформление зарезервированного заказа; заказ не виден пользователю"

    RESERVATION_EXPIRED(0, CANCELLED),


    //          "Покупатель не оплатил заказ"
    USER_NOT_PAID(1, CANCELLED),

    //          "Невозможно связаться с покупателем"

    USER_UNREACHABLE(2, CANCELLED),


    //   "Покупатель передумал"

    USER_CHANGED_MIND(3, CANCELLED),

    //        "Покупателя не устраивают условия доставки"
    USER_REFUSED_DELIVERY(4, CANCELLED),

    //        "Покупателю не подошел товар"
    USER_REFUSED_PRODUCT(5, CANCELLED),

    //         "Магазин не может выполнить заказ"
    SHOP_FAILED(6, CANCELLED),

    //         "Покупателя не устраивает качество товара"
    USER_REFUSED_QUALITY(7, CANCELLED),

    //         "Изменяется состав заказа"
    // REPLACING_ORDER(8, CANCELLED),

    //       "Магазин не обработал заказ вовремя"
    PROCESSING_EXPIRED(9, CANCELLED),

    //         "Заказ не был обработан магазином в установленный срок"
    PENDING_EXPIRED(10, CANCELLED),

    //         "Магазин отменил заказ из статуса PENDING"
    SHOP_PENDING_CANCELLED(11, CANCELLED),

    //         "Пользователь отменил заказ, находящийся в статусе PENDING"
    PENDING_CANCELLED(12, CANCELLED),

    //        "Заказ отменен в результате срабатывания антифрода как реакция на фрод со стороны покупателя"
    USER_FRAUD(13, CANCELLED),

    //         "Ошибка при оформлении заказа. Заказ не виден пользователю."
    RESERVATION_FAILED(14, CANCELLED),

    //          "Пользователь сделал другой заказ."
    USER_PLACED_OTHER_ORDER(15, CANCELLED),

    //         "Пользователь нашел дешевле."
    USER_BOUGHT_CHEAPER(16, CANCELLED),

    //        @ApiModelProperty(
//                "Товара не оказалось в наличии."
//        )
    MISSING_ITEM(17, CANCELLED),

    //        @ApiModelProperty(
//                "Товар оказался бракованным."
//        )
    BROKEN_ITEM(18, CANCELLED),

    //        @ApiModelProperty(
//                "Доставили не тот товар."
//        )
    WRONG_ITEM(19, CANCELLED),

    //        @ApiModelProperty(
//                "Не успел забрать товар из пункта самовывоза."
//        )
    PICKUP_EXPIRED(20, CANCELLED),

    //        @ApiModelProperty(
//                "Возникли проблемы во время доставки."
//        )
    DELIVERY_PROBLEMS(21, CANCELLED),

    //        @ApiModelProperty(
//                "С пользователем связались слишком поздно."
//        )
    LATE_CONTACT(22, CANCELLED),

    //        @ApiModelProperty(
//                "Причина отмены заказа в свободной форме, " +
//                        "описание ожидается в поле order.cancellationRequest.notes ."
//        )
    CUSTOM(23, CANCELLED),

    //        @ApiModelProperty(
//                "Заказ отменен по вине службы доставки (утеря, брак транспортировки)."
//        )
    DELIVERY_SERVICE_FAILED(24, CANCELLED),

    //        @ApiModelProperty(
//                "Склад не отгрузил заказ."
//        )
    WAREHOUSE_FAILED_TO_SHIP(25, CANCELLED),

    @Deprecated
//        @ApiModelProperty(
//                "Заказ не доставлен по данным от СД. " +
//                        "Выставляется если пришел возвратный чекпоинт и не было запроса отмены." +
//                        "Название с опечаткой, в процессе вырезания."
//        )
    DELIVERY_SERIVCE_UNDELIVERED(26, CANCELLED),

    //        @ApiModelProperty(
//                "Заказ не доставлен по данным от СД. " +
//                        "Выставляется если пришел возвратный чекпоинт и не было запроса отмены."
//        )
    DELIVERY_SERVICE_UNDELIVERED(26, CANCELLED),

    //        @ApiModelProperty(
//                "Заказ не готов к обзвону, начальный статус для предзаказа."
//        )
    PREORDER(27, PENDING),

    //        @ApiModelProperty(
//                "Заказ готов к обзвону, " +
//                        "начальный статус для обычного заказа или следующий после INIT для предзаказа."
//        )
    AWAIT_CONFIRMATION(28, PENDING),

    //        @ApiModelProperty(
//                "Заказ только перешел в статус PROCESSING."
//        )
    STARTED(29, PROCESSING),

    //        @ApiModelProperty(
//                "Заказ комплектуется на складе."
//        )
    PACKAGING(30, PROCESSING),

    //        @ApiModelProperty(
//                "Заказ готов к отгрузке."
//        )
    READY_TO_SHIP(31, PROCESSING),

    //        @ApiModelProperty(
//                "Заказ отгружен в службу доставки."
//        )
    SHIPPED(32, PROCESSING),

    //        @ApiModelProperty(
//                "Ожидается асинхронная обработка заказа после создания (пока что только антифрод)"
//        )
    ASYNC_PROCESSING(33, PENDING),

    @Deprecated
//        @ApiModelProperty(
//                "Пользователь отказался предоставить персональные данные для трансграничного заказа"
//        )
    USER_REFUSED_TO_PROVIDE_PERSONAL_DATA(34, CANCELLED),

    //        @ApiModelProperty("Ожидаем пока пользователь введет данные. Для оплаты картой - данные карты, для
    //        оплаты кредитом" +
//                " - оформит заявку в сбере/кредитном брокере")
    WAITING_USER_INPUT(35, UNPAID),
    //        @ApiModelProperty("Ожидаем решения банка по кредиту. В случае кредита сберовского мы через ~полчаса
    //        переводим " +
//                "заказ в этот статус и через ивент и нотифаер шлем письмо с предложением пользователю 'проверить
//                свои " +
//                "данные в личном кабинете, заявка в работе'. В случае кредитного брокера мы будем по его
//                нотификации " +
//                "переводить в этот подстатус.")
    WAITING_BANK_DECISION(36, UNPAID),

    //  @ApiModelProperty("Банк отказал в оформлении кредита")
    BANK_REJECT_CREDIT_OFFER(37, CANCELLED),
    //  @ApiModelProperty("Клиент отказался от кредита")
    CUSTOMER_REJECT_CREDIT_OFFER(38, CANCELLED),

    //   @ApiModelProperty("Ошибка при оформлении кредита")
    CREDIT_OFFER_FAILED(39, CANCELLED),

    //  @ApiModelProperty("Ожидает согласования дат доставки в CRM")
    AWAIT_DELIVERY_DATES_CONFIRMATION(40, PENDING),

    //  @ApiModelProperty("Отмена по вине сервиса")
    SERVICE_FAULT(41, CANCELLED),

    //    @ApiModelProperty("Передано в службу доставки")
    DELIVERY_SERVICE_RECEIVED(42, DELIVERY),

    //  @ApiModelProperty("Вручено пользователю")
    USER_RECEIVED(43, DELIVERY),

    //   @ApiModelProperty("Ожидает резервирования заказа на складе")
    WAITING_FOR_STOCKS(44, PENDING),

    //  @ApiModelProperty("Как часть мульти заказа")
    AS_PART_OF_MULTI_ORDER(45, CANCELLED),

    //    @ApiModelProperty("Заказ готов к вручению пользователю. Ожидаем от пользователя запроса на вручение.")
    READY_FOR_LAST_MILE(46, DELIVERY),

    //    @ApiModelProperty("Заказ отправлен пользователю и скоро будет вручен.")
    LAST_MILE_STARTED(47, DELIVERY),

    //   @ApiModelProperty("Заказ требует проверки антифрода")
    ANTIFRAUD(48, PENDING),

    //    @ApiModelProperty("Пользователь не получил заказ")
    DELIVERY_USER_NOT_RECEIVED(49, DELIVERY),

    //    @ApiModelProperty("Служба сообщила, что доставила заказ")
    DELIVERY_SERVICE_DELIVERED(50, DELIVERED),

    //    @ApiModelProperty("Пользователь не получил заказ")
    DELIVERED_USER_NOT_RECEIVED(51, DELIVERED),

    //    @ApiModelProperty("Хочу оплатить другим способом")
    USER_WANTED_ANOTHER_PAYMENT_METHOD(52, CANCELLED),

    //    @ApiModelProperty("Возникла техническая ошибка при оформлении")
    USER_RECEIVED_TECHNICAL_ERROR(53, CANCELLED),

    //    @ApiModelProperty("Пользователь забыл использовать Бонус")
    USER_FORGOT_TO_USE_BONUS(54, CANCELLED),

    //    @ApiModelProperty("Заказ не поступил в СД СЦ")
    DELIVERY_SERVICE_NOT_RECEIVED(55, CANCELLED),

    //   @ApiModelProperty("Заказ утрачен службой доставки")
    DELIVERY_SERVICE_LOST(56, CANCELLED),

    //    @ApiModelProperty("Заказ был передан в другую СД")
    SHIPPED_TO_WRONG_DELIVERY_SERVICE(57, CANCELLED),

    //   @ApiModelProperty("Пользователь подтвердил, что получил заказ")
    DELIVERED_USER_RECEIVED(58, DELIVERED),

    //    @ApiModelProperty("Пользователь ожидает открытия кредитного счета")
    WAITING_TINKOFF_DECISION(59, UNPAID),

    // Подстатусы 60 - 68 для экспресс-заказов
    //    @ApiModelProperty("Поиск курьера")
    COURIER_SEARCH(60, DELIVERY),

    //    @ApiModelProperty("Курьер назначен")
    COURIER_FOUND(61, DELIVERY),

    //    @ApiModelProperty("Курьер едет за заказом")
    COURIER_IN_TRANSIT_TO_SENDER(62, DELIVERY),

    //        @ApiModelProperty("Курьер приехал на точку получения заказа")
    COURIER_ARRIVED_TO_SENDER(63, DELIVERY),

    //        @ApiModelProperty("Курьер забрал заказ у партнера")
    COURIER_RECEIVED(64, DELIVERY),

    //        @ApiModelProperty("Курьер не найден")
    COURIER_NOT_FOUND(65, DELIVERY),

    //        @ApiModelProperty("Курьер не доставил ваш заказ")
    COURIER_NOT_DELIVER_ORDER(66, CANCELLED),

    //        @ApiModelProperty("Курьер едет к партнеру с невыкупом (клиент не купил, курьер везет обратно)")
    COURIER_RETURNS_ORDER(67, CANCELLED),

    //        @ApiModelProperty("Курьер передал невыкупленный заказ обратно партнеру")
    COURIER_RETURNED_ORDER(68, CANCELLED),

    //        @ApiModelProperty("Ожидаем пока пользователь выберет новую опцию доставки")
    WAITING_USER_DELIVERY_INPUT(69, UNPAID),

    //        @ApiModelProperty("Заказ прибыл в пвз/постомат")
    PICKUP_SERVICE_RECEIVED(70, PICKUP),

    //Пользователь забрал заказ из пвз/постомата(49й чп),
    //но эта информация не от пользователя и она может быть ошибочная
    //поэтому статус остается в pickup и может вернуться обратно в delivery/pickup
//        @ApiModelProperty("Пользователь забрал заказ из пвз/постомата")
    PICKUP_USER_RECEIVED(71, PICKUP),

    //отдельный подстатус COURIER_NOT_FOUND для статуса CANCELLED
//        @ApiModelProperty("Курьер не найден, отмена заказа")
    CANCELLED_COURIER_NOT_FOUND(72, CANCELLED),

    //        @ApiModelProperty("Курьер не приехал за заказом")
    COURIER_NOT_COME_FOR_ORDER(73, CANCELLED),

    //        @ApiModelProperty("Необслуживаемый регион СД")
    DELIVERY_NOT_MANAGED_REGION(74, CANCELLED),

    //        @ApiModelProperty("Некорректные весогабариты")
    INAPPROPRIATE_WEIGHT_SIZE(75, CANCELLED),

    //        @ApiModelProperty("Неполная контактная информация по заказу (адрес/телефон)")
    INCOMPLETE_CONTACT_INFORMATION(76, CANCELLED),

    //        @ApiModelProperty("Заказ не полный (многоместный)")
    INCOMPLETE_MULTI_ORDER(77, CANCELLED),

    //        @ApiModelProperty("Техническая ошибка")
    TECHNICAL_ERROR(78, CANCELLED),

    //        @ApiModelProperty("Заказ утерян на СЦ")
    SORTING_CENTER_LOST(79, CANCELLED),

    //        @ApiModelProperty("Не был запущен поиск курьера")
    COURIER_SEARCH_NOT_STARTED(80, CANCELLED),

    //        @ApiModelProperty("Заказ утерян")
    LOST(81, CANCELLED),

    //        @ApiModelProperty("Заказ ожидает процессинга платежа по заказу. Устанавливается в случае деградации
    //        траста")
    AWAIT_PAYMENT(82, UNPAID),

    @JsonEnumDefaultValue
    UNKNOWN(-1, OrderStatus.UNKNOWN);

    // ----------------------------------------------------------------------------------------------------------------
    private static final Map<OrderStatus, Set<OrderSubstatus>> STATUS_TO_SUBSTATUSES = new HashMap<>();
    private static final Map<Integer, OrderSubstatus> ORDER_SUBSTATUS_MAP =
            Arrays.stream(OrderSubstatus.values())
                    .filter(orderSubstatus -> !orderSubstatus.equals(DELIVERY_SERIVCE_UNDELIVERED))
                    .collect(Collectors.toMap(OrderSubstatus::getId, Function.identity()));

    private final int id;
    private final OrderStatus status;

    OrderSubstatus(int id, OrderStatus status) {
        this.id = id;
        this.status = status;
    }


    public OrderStatus getStatus() {
        return status;
    }


    public int getId() {
        return id;
    }


    public boolean isUnknown() {
        return this == UNKNOWN;
    }


    public String getPrimaryDescription() {
        return getStatus().toString();
    }

    // ----------------------------------------------------------------------------------------------------------------

    static {
        for (OrderSubstatus substatus : values()) {
            final OrderStatus status = substatus.status;
            Set<OrderSubstatus> substatuses = STATUS_TO_SUBSTATUSES.computeIfAbsent(status, k -> new HashSet<>());
            substatuses.add(substatus);
        }
    }

    public static Set<OrderSubstatus> fromStatus(OrderStatus status) {
        return STATUS_TO_SUBSTATUSES.get(status);
    }

    public static OrderSubstatus getByIdOrUnknown(Integer id) {
        return id == null ? null :
                id == DELIVERY_SERIVCE_UNDELIVERED.id ? DELIVERY_SERIVCE_UNDELIVERED :
                        ORDER_SUBSTATUS_MAP.getOrDefault(id, UNKNOWN);
    }
}
