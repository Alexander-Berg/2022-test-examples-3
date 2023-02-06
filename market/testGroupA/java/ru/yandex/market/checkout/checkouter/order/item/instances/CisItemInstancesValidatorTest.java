package ru.yandex.market.checkout.checkouter.order.item.instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import ru.yandex.market.checkout.backbone.validation.order.item.instances.CisItemInstancesValidator;
import ru.yandex.market.checkout.backbone.validation.order.item.instances.DbOrderForInstancesValidation;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureFlagWrapper;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemCisesValidationException;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstances;
import ru.yandex.market.checkout.checkouter.order.OrderItemsException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.util.OrderItemInstancesUtil;
import ru.yandex.market.identifier.rules.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.order.OrderItemCisesValidationException.INVALID_CIS_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemCisesValidationException.TOO_FEW_CISES_FOR_ITEM_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemCisesValidationException.TOO_MANY_CISES_FOR_ITEM_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERED_USER_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PACKAGING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.READY_TO_SHIP;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.STARTED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;

//todo: Тест не делает моки для Validator, проверяет только поход с отключенным флагом, но в проде флаг давно включен
class CisItemInstancesValidatorTest {

    private final CisItemInstancesValidator cisValidator;
    private final String cis1 = "010465006531553121CtPoNqNB7qOd1";
    private final String cis2 = "010465006531553121CtPoNqNB7qOd2";
    private final String cis3 = "010465006531553121CtPoNqNB7qOd3";

    CisItemInstancesValidatorTest() {
        var flagWrapper = new CheckouterFeatureFlagWrapper(new CheckouterFeatureResolverStub());
        this.cisValidator = new CisItemInstancesValidator(Mockito.mock(Validator.class), flagWrapper);
    }

    private static Stream<Arguments> failedValidationForEditFFOrderByStatusesSource() {
        return Stream.of(
                Arguments.of(OrderType.FF, DELIVERED, OrderSubstatus.DELIVERED_USER_NOT_RECEIVED),
                Arguments.of(OrderType.FBS, PROCESSING, OrderSubstatus.SHIPPED),
                Arguments.of(OrderType.FBS, CANCELLED, OrderSubstatus.USER_BOUGHT_CHEAPER),
                Arguments.of(OrderType.DBS, CANCELLED, OrderSubstatus.USER_BOUGHT_CHEAPER)
        );
    }

    private static Stream<Arguments> successValidationForEditFFOrderByStatusesSource() {
        return Stream.of(
                Arguments.of(OrderType.FF, PROCESSING, OrderSubstatus.READY_TO_SHIP),
                Arguments.of(OrderType.FF, PICKUP, OrderSubstatus.PICKUP_SERVICE_RECEIVED),
                Arguments.of(OrderType.FBS, PROCESSING, OrderSubstatus.READY_TO_SHIP),
                Arguments.of(OrderType.FBS, PROCESSING, OrderSubstatus.STARTED),
                Arguments.of(OrderType.FBS, DELIVERY, OrderSubstatus.COURIER_RECEIVED),
                Arguments.of(OrderType.FBS, PICKUP, OrderSubstatus.PICKUP_SERVICE_RECEIVED),
                Arguments.of(OrderType.DBS, PROCESSING, OrderSubstatus.READY_TO_SHIP),
                Arguments.of(OrderType.DBS, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED)
        );
    }

    private static Stream<Arguments> errorValidationForEditFFOrderByStatusesSource() {
        return Stream.of(
                Arguments.of(OrderType.FBS, DELIVERY, OrderSubstatus.USER_RECEIVED),
                Arguments.of(OrderType.FBS, PICKUP, OrderSubstatus.PICKUP_USER_RECEIVED)
        );
    }

    private static Stream<Arguments> failedValidatingByCisCountSource() {
        return Stream.of(
                Arguments.of(OrderType.FBS, PROCESSING, OrderSubstatus.READY_TO_SHIP),
                Arguments.of(OrderType.DBS, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED),
                Arguments.of(OrderType.DBS, PICKUP, OrderSubstatus.PICKUP_SERVICE_RECEIVED)
        );
    }

    private static Stream<Arguments> successValidatingByCisCountSource() {
        return Stream.of(
                Arguments.of(OrderType.FF, PROCESSING, OrderSubstatus.STARTED),
                Arguments.of(OrderType.FBS, PROCESSING, OrderSubstatus.STARTED),
                Arguments.of(OrderType.DBS, PROCESSING, OrderSubstatus.STARTED)
        );
    }

    public static Stream<Arguments> statusValidateOnStatusesWithoutRequireValidateSource() {
        return Stream.of(
                Arguments.of(OrderType.FF, CANCELLED, USER_CHANGED_MIND),
                Arguments.of(OrderType.FBS, CANCELLED, USER_CHANGED_MIND),
                Arguments.of(OrderType.FBS, PROCESSING, null),
                Arguments.of(OrderType.FBS, PROCESSING, STARTED),
                Arguments.of(OrderType.DBS, CANCELLED, USER_CHANGED_MIND)
        );
    }

    @Test
    @DisplayName("Валидация ну дубли. Оба дубля добавляются для одного товара")
    public void duplicateValidationNegativeCase1() {
        OrderItem item1 = initRequiredCisesItem(1, 1, cis2);
        Order order = initOrder(item1);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis1, cis1)
        );

        mustThrowWithCode(order, orderItemInstances, OrderItemCisesValidationException.DUPLICATE_CISES);
    }

    @Test
    @DisplayName("Валидация ну дубли. Дубли добавляются для разных товаров, но 1 уже записан")
    public void duplicateValidationNegativeCase2() {
        OrderItem item1 = initRequiredCisesItem(1, 1, cis1);
        OrderItem item2 = initRequiredCisesItem(2, 1);
        Order order = initOrder(item1, item2);

        List<OrderItemInstances> orderItemInstances = List.of(initItemInstances(2, cis1));

        mustThrowWithCode(order, orderItemInstances, OrderItemCisesValidationException.DUPLICATE_CISES);
    }

    @Test
    @DisplayName("Валидация ну дубли. Оба дубля добавляются для разных товаров")
    public void duplicateValidationNegativeCase3() {
        OrderItem item1 = initRequiredCisesItem(1, 1, cis2);
        OrderItem item2 = initRequiredCisesItem(2, 1);
        Order order = initOrder(item1, item2);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis1),
                initItemInstances(2, cis1)
        );

        mustThrowWithCode(order, orderItemInstances, OrderItemCisesValidationException.DUPLICATE_CISES);
    }

    @Test
    @DisplayName("Валидация ну дубли. Дубли каким-то образом уже записаны в одном товаре")
    public void duplicateValidationNegativeCase4() {
        OrderItem item1 = initRequiredCisesItem(1, 2, cis1, cis1);
        OrderItem item2 = initRequiredCisesItem(2, 1);
        Order order = initOrder(item1, item2);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(2, cis2)
        );

        Assertions.assertThrows(IllegalStateException.class,
                () -> cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances));
    }

    @Test
    @DisplayName("Валидация ну дубли. Дубли каким-то образом уже записаны в разных товарах")
    public void duplicateValidationNegativeCase5() {
        OrderItem item1 = initRequiredCisesItem(1, 2, cis1);
        OrderItem item2 = initRequiredCisesItem(2, 1, cis1);
        OrderItem item3 = initRequiredCisesItem(3, 1);
        Order order = initOrder(item1, item2, item3);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(3, cis2)
        );

        Assertions.assertThrows(IllegalStateException.class,
                () -> cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances));
    }

    @Test
    @DisplayName("Валидация ну дубли. Кизы все разные")
    public void duplicateValidationPositiveCase1() {
        OrderItem item1 = initRequiredCisesItem(1, 2, cis1);
        Order order = initOrder(item1);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis2, cis3)
        );

        cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances);
    }

    @Test
    @DisplayName("Валидация ну дубли. Есть одинаковые кизы, но они перезапишутся")
    public void duplicateValidationPositiveCase2() {
        OrderItem item1 = initRequiredCisesItem(1, 2, cis1);
        Order order = initOrder(item1);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis1, cis2)
        );

        cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances);
    }

    @Test
    @DisplayName("Валидация ну дубли. Записываются в пустой товар")
    public void duplicateValidationPositiveCase3() {
        OrderItem item1 = initRequiredCisesItem(1, 2);
        Order order = initOrder(item1);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis1, cis2)
        );

        cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances);
    }

    @ParameterizedTest
    @DisplayName("Неудавшаяся валидация заказа для редактирования кизов. Статусы не позволяют")
    @MethodSource("failedValidationForEditFFOrderByStatusesSource")
    public void failedValidationForEditFFOrderByStatuses(OrderType orderType,
                                                         OrderStatus status,
                                                         OrderSubstatus substatus) {
        OrderItem item = initRequiredCisesItem(1, 2, cis1);
        Order order = initOrder(orderType, status, substatus);
        order.addItem(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis1, cis2)
        );

        Assertions.assertThrows(OrderStatusNotAllowedException.class,
                () -> cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances));
    }

    @ParameterizedTest
    @DisplayName("Удавшаяся валидация заказа для редактирования кизов. С разными статусами")
    @MethodSource("successValidationForEditFFOrderByStatusesSource")
    public void successValidationForEditFFOrderByStatuses(OrderType orderType,
                                                          OrderStatus status,
                                                          OrderSubstatus substatus) {
        OrderItem item = initRequiredCisesItem(1, 2, cis1);
        Order order = initOrder(orderType, status, substatus);
        order.addItem(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis1, cis2)
        );

        cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances);
    }

    @ParameterizedTest
    @DisplayName("")
    @MethodSource("errorValidationForEditFFOrderByStatusesSource")
    public void unavailableStatusTest(OrderType orderType,
                                      OrderStatus status,
                                      OrderSubstatus substatus) {
        OrderItem item = initRequiredCisesItem(1, 2, cis1);
        Order order = initOrder(orderType, status, substatus);
        order.addItem(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis1, cis2)
        );

        Assertions.assertThrows(OrderStatusNotAllowedException.class,
                () -> cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances));
    }

    @ParameterizedTest
    @DisplayName("Удавшаяся валидация с разным форматом кизов. Для заказов требующих кизов")
    @ValueSource(strings = {
            "010465006531553121CtPoNqNB7qOdc",
            "010465006531553121CtPoN.NB7;Odc",
            "010460707019002821gsmhXP", // Молочная продукция
            "010465006531290521p9YjRk;Ihdm6T2WItlgr" //Фотокамеры
    })
    public void successValidationByFormat(String cis) {
        OrderItem item = initRequiredCisesItem(1, 1);
        Order order = initOrder(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis)
        );

        cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances);
    }

    @ParameterizedTest
    @DisplayName("Удавшаяся валидация с разным форматом кизов. Для заказов НЕ требующие кизов")
    @ValueSource(strings = {
            ""
    })
    public void successValidationByFormatForNotRequiredCises(String cis) {
        OrderItem item = initNotMandatoryMarkingItem(1, 1);
        Order order = initOrder(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis)
        );

        cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances);
    }

    @ParameterizedTest
    @DisplayName("Ошибка валидации при разных форматов кизов в заказе с обязательным наличием кизов")
    @ValueSource(strings = {
            "",
            " ",
            "123",
            "10465006531553121CtPoNqNB7qOdc",
            "00465006531553121CtPoNqNB7qOdc",
            "01046500631553121CtPoNqNB7qOdc",
            "0104650065315531221CtPoNqNB7qOdc",
            "0104650A6531553121CtPoNqNB7qOdc",
            "0121CtPoNqNB7qOdc",
            "01046500653155311CtPoNqNB7qOdc",
            "010465006531553122CtPoNqNB7qOdc",
            "010465006531553121CtPoN",
            "010463013616870921?!@0|9106926506|+8610="
    })
    public void failedValidationByFormat(String cis) {
        OrderItem item = initRequiredCisesItem(1, 2);
        Order order = initOrder(OrderType.DBS, DELIVERY, null);
        order.addItem(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis)
        );

        Assertions.assertThrows(OrderItemCisesValidationException.class,
                () -> cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances));
    }

    @ParameterizedTest
    @DisplayName("Неудавшаяся валидация с разным форматом кизов в заказе БЕЗ обязательности наличия кизов")
    @ValueSource(strings = {
            " ",
            "123",
            "10465006531553121CtPoNqNB7qOdc",
            "00465006531553121CtPoNqNB7qOdc",
            "01046500631553121CtPoNqNB7qOdc",
            "0104650065315531221CtPoNqNB7qOdc",
            "0104650A6531553121CtPoNqNB7qOdc",
            "0121CtPoNqNB7qOdc",
            "01046500653155311CtPoNqNB7qOdc",
            "010465006531553122CtPoNqNB7qOdc",
            "010465006531553121CtPoN",
            "010463013616870921?!@0|9106926506|+8610="
    })
    public void failedValidationByFormatForNotRequiredCisItems(String cis) {
        OrderItem item = initNotMandatoryMarkingItem(1, 1);
        Order order = initOrder(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis)
        );

        Assertions.assertThrows(OrderItemCisesValidationException.class,
                () -> cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances));
    }

    @Test
    @DisplayName("Ошибка валидации при cis == null в заказе с обязательностью кизов")
    public void failedValidationWithNull() {
        OrderItem item = initRequiredCisesItem(1, 2);
        Order order = initOrder(OrderType.FBS, PROCESSING, OrderSubstatus.READY_TO_SHIP);
        order.addItem(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, (String) null)
        );

        Assertions.assertThrows(OrderItemCisesValidationException.class,
                () -> cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances));
    }

    @Test
    @DisplayName("ОК валидации при cis == null в заказе НЕ требующем кизов")
    public void okValidationWithNull() {
        OrderItem item = initNotMandatoryMarkingItem(1, 2);
        Order order = initOrder(OrderType.FBS, PROCESSING, OrderSubstatus.READY_TO_SHIP);
        order.addItem(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, (String) null)
        );

        cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances);
    }

    @ParameterizedTest
    @DisplayName("Неудавшаяся валидация по причине нехватки кизов. Для статусов где кизы должны быть указаны все")
    @MethodSource("failedValidatingByCisCountSource")
    public void failedValidatingByCisCount(OrderType type, OrderStatus status, OrderSubstatus substatus) {
        OrderItem item = initRequiredCisesItem(1, 2);
        Order order = initOrder(type, status, substatus);
        order.addItem(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis1)
        );
        OrderItemCisesValidationException exception = Assertions.assertThrows(OrderItemCisesValidationException.class,
                () -> cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances));
        assertThat(exception.getCode()).isEqualTo(TOO_FEW_CISES_FOR_ITEM_CODE);
    }

    @ParameterizedTest
    @DisplayName("Удавшаяся валидация. Кизов не хватает, но статусы позволяют их не указывать все")
    @MethodSource("successValidatingByCisCountSource")
    public void successValidatingByCisCount(OrderType type, OrderStatus status, OrderSubstatus substatus) {
        OrderItem item = initRequiredCisesItem(1, 2);
        Order order = initOrder(type, status, substatus);
        order.addItem(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis1)
        );
        cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances);
    }

    @Test
    @DisplayName("Валидация ок. Кизов меньше, но товар не требует их обязательного наличия")
    public void successValidatingByCisCountForNotMandatoryCisItem() {
        OrderItem item = initNotMandatoryMarkingItem(1, 2);
        Order order = initOrder(OrderType.DBS, DELIVERY, DELIVERY_SERVICE_RECEIVED);
        order.addItem(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis1)
        );
        cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances);
    }

    @Test
    @DisplayName("Добавляемых кизов больше чем штучек в товаре")
    public void shouldExceptionWhenTooManyCisesForItemAdded() {
        OrderItem item = initRequiredCisesItem(1, 1);
        Order order = initOrder(OrderType.FF, PROCESSING, STARTED);
        order.addItem(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(1, cis1, cis2)
        );
        OrderItemCisesValidationException exception = Assertions.assertThrows(OrderItemCisesValidationException.class,
                () -> cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances));
        assertThat(exception.getCode()).isEqualTo(TOO_MANY_CISES_FOR_ITEM_CODE);
    }

    @Test
    @DisplayName("Валидация на редактирование. Кизы будут добавляться в товар которого нет в заказе")
    public void shouldExceptionWhenValidatedItemNotFound() {
        OrderItem item = initRequiredCisesItem(1, 1);
        Order order = initOrder(OrderType.FF, PROCESSING, STARTED);
        order.addItem(item);

        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(2, cis1)
        );
        OrderItemsException exception = Assertions.assertThrows(OrderItemsException.class,
                () -> cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances));
        assertThat(exception.getCode()).isEqualTo(OrderItemsException.ITEM_NOT_FOUND_CODE);
    }

    @Test
    @DisplayName("Валидация на редактирование кизов. Несколько товаров")
    public void validateForEditWorkCase() {
        OrderItem item1 = initRequiredCisesItem(1, 1);
        OrderItem item2 = initRequiredCisesItem(2, 2);
        OrderItem item3 = initRequiredCisesItem(3, 3);
        Order order = initOrder(OrderType.FF, PROCESSING, PACKAGING);
        order.addItem(item1);
        order.addItem(item2);
        order.addItem(item3);
        List<OrderItemInstances> orderItemInstances = List.of(
                initItemInstances(item1.getId(), cis1),
                initItemInstances(item2.getId(), cis2, cis3)
        );

        cisValidator.validateForEdit(new DbOrderForInstancesValidation(order), orderItemInstances);
    }

    @Test
    @DisplayName("Валидация при смене статуса. Ок на валидации товаров без обязательного наличия кизов")
    void statusValidateNotRequiredCisItem() {
        OrderItem item1 = initNotMandatoryMarkingItem(1, 1);
        Order order = initOrder(OrderType.FBS);
        order.addItem(item1);

        cisValidator.validateForUpdateStatus(order, DELIVERED, DELIVERED_USER_RECEIVED, ClientRole.USER);
    }

    @Test
    @DisplayName("Валидация при смене статуса. " +
            "Ок на валидации товаров без обязательного наличия кизов, но кизы указаны")
    void statusValidateNotRequiredCisItemWithCis() {
        OrderItem item1 = initNotMandatoryMarkingItem(1, 2, cis1);
        Order order = initOrder(OrderType.FBS);
        order.addItem(item1);
        cisValidator.validateForUpdateStatus(order, DELIVERED, DELIVERED_USER_RECEIVED, ClientRole.USER);
    }

    @ParameterizedTest
    @DisplayName("Валидация при смене статуса. " +
            "Кизов не хватает, но статус позволяет не валидировать")
    @MethodSource("statusValidateOnStatusesWithoutRequireValidateSource")
    void statusValidateOnStatusesWithoutRequireValidate(OrderType type, OrderStatus status, OrderSubstatus substatus) {
        OrderItem item1 = initRequiredCisesItem(1, 2, cis1);
        Order order = initOrder(type);
        order.addItem(item1);
        cisValidator.validateForUpdateStatus(order, status, substatus, ClientRole.SHOP);
    }

    @Test
    @DisplayName("Валидация при смене статуса. " +
            "Ошибка из-за неверного количества")
    void statusValidateFailedForInvalidCisCount() {
        OrderItem item1 = initRequiredCisesItem(1, 2, cis1);
        Order order = initOrder(OrderType.FBS);
        order.addItem(item1);
        OrderItemCisesValidationException exception = Assertions.assertThrows(OrderItemCisesValidationException.class,
                () -> cisValidator.validateForUpdateStatus(order, PROCESSING, READY_TO_SHIP, ClientRole.SHOP_USER));
        assertThat(exception.getCode()).isEqualTo(TOO_FEW_CISES_FOR_ITEM_CODE);
    }

    @Test
    @DisplayName("Валидация при смене статуса. " +
            "Кизов нет, но роль позволяет перевод статуса без них")
    void statusValidateOKWithoutCisByRole() {
        OrderItem item1 = initRequiredCisesItem(1, 2);
        Order order = initOrder(OrderType.FBS);
        order.addItem(item1);

        cisValidator.validateForUpdateStatus(order, PROCESSING, READY_TO_SHIP, ClientRole.SYSTEM);
    }

    @Test
    @DisplayName("Валидация при смене статуса. " +
            "Ошибка из-за неверного формата")
    void statusValidateFailedForInvalidCisFormat() {
        OrderItem item1 = initRequiredCisesItem(1, 1, "invalidCis");
        Order order = initOrder(OrderType.DBS);
        order.addItem(item1);
        OrderItemCisesValidationException exception = Assertions.assertThrows(OrderItemCisesValidationException.class,
                () -> cisValidator.validateForUpdateStatus(order, DELIVERED, DELIVERED_USER_RECEIVED, ClientRole.SHOP));
        assertThat(exception.getCode()).isEqualTo(INVALID_CIS_CODE);
    }

    private void mustThrowWithCode(Order order, List<OrderItemInstances> orderItemInstances, String exceptionCode) {
        OrderItemCisesValidationException exception =
                Assertions.assertThrows(OrderItemCisesValidationException.class,
                        () -> cisValidator.validateForEdit(new DbOrderForInstancesValidation(order),
                                orderItemInstances));
        assertThat(exception.getCode()).isEqualTo(exceptionCode);
    }

    private OrderItem initRequiredCisesItem(long itemId, int count, String... cises) {
        OrderItem item = initNotMandatoryMarkingItem(itemId, count, cises);
        item.setCargoTypes(CisItemInstancesValidator.CIS_REQUIRED_CARGOTYPE_CODES);
        return item;
    }

    private OrderItem initNotMandatoryMarkingItem(long itemId, int count, String... cises) {
        OrderItem item = new OrderItem();
        item.setId(itemId);
        item.setCount(count);
        item.setFeedId(item.getId());
        item.setBundleId("bundleId" + item.getId());
        if (cises != null) {
            List<OrderItemInstance> itemInstances = new ArrayList<>();
            for (String cis : cises) {
                itemInstances.add(new OrderItemInstance(cis));
            }
            item.setInstances(OrderItemInstancesUtil.convertToNode(itemInstances));
        }
        return item;
    }

    private Order initOrder(OrderItem... items) {
        Order order = initOrder(OrderType.FF, PROCESSING, OrderSubstatus.STARTED);
        order.setItems(Arrays.asList(items));
        return order;
    }

    private Order initOrder(OrderType type) {
        return initOrder(type, PROCESSING, OrderSubstatus.STARTED);
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    private Order initOrder(OrderType orderType, OrderStatus status, OrderSubstatus substatus) {
        Order order = new Order();
        order.setId(100L);
        order.setStatus(status);
        order.setSubstatus(substatus);
        switch (orderType) {
            case FF:
                order.setFulfilment(true);
                order.setDelivery(initDeliveryWithType(DeliveryPartnerType.YANDEX_MARKET));
                break;
            case FBS:
                order.setFulfilment(false);
                order.setDelivery(initDeliveryWithType(DeliveryPartnerType.YANDEX_MARKET));
                break;
            case DBS:
                order.setFulfilment(false);
                order.setDelivery(initDeliveryWithType(DeliveryPartnerType.SHOP));
                break;
        }
        return order;
    }

    private Delivery initDeliveryWithType(DeliveryPartnerType deliveryType) {
        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(deliveryType);
        return delivery;
    }

    private OrderItemInstances initItemInstances(long itemId, String... cises) {
        List<OrderItemInstance> instances = new ArrayList<>();
        for (String cis : cises) {
            instances.add(new OrderItemInstance(cis));
        }
        return new OrderItemInstances(itemId, instances);
    }

    enum OrderType {
        FF, FBS, DBS
    }
}
