package ru.yandex.market.deliveryintegrationtests.delivery.tests.manual_orders_doubles;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import client.CheckouterClient;
import dto.requests.checkouter.Address;
import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import dto.requests.checkouter.checkout.Buyer;
import factory.OfferItems;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;

public class VoronezhDeliveryTest extends AbstractManualOrderDoubleTest {
    private Buyer voronezhBuyer;
    private CheckouterClient checkouterClient;

    private final List<Long> outletId = Collections.singletonList(10001090594L);
    private final List<Long> lockerOutletId = Collections.singletonList(10001087772L);

    @BeforeEach
    void setUp() {
        // Задать паспорт, в который есть доступ
        voronezhBuyer = new Buyer("+79998889900","");
        voronezhBuyer.setFirstName("Иван");
        voronezhBuyer.setLastName("Анастасиев");
        voronezhBuyer.setMiddleName(null);
        voronezhBuyer.setEmail("yandex-team-63765.51231@yandex.ru");
        checkouterClient = new CheckouterClient();
        checkouterClient.setUid(userUid);
        ORDER_STEPS.setCheckouterClient(checkouterClient);
    }

    @Test
    @DisplayName("Курьерская доставка в Воронеж")
    public void createCourierOrderToVoronezh() {
        // Создать заказ
        params = CreateOrderParameters
            .newBuilder(193L, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
            .address(Address.MANUAL_COURIER_VORONEZH)
            .buyer(voronezhBuyer)
            .build();
        order = ORDER_STEPS.createOrder(params);

        // Проверит, что доставка создалась через 63119 : СЦ МК Сестрица Сорока
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        CombinatorRoute combinatorRoute = LOM_ORDER_STEPS.getOrderRoute(lomOrderId).getText();
        combinatorRoute.getRoute().getPoints().stream()
            .filter(
                point -> point.getPartnerType() == PartnerType.SORTING_CENTER && Objects.equals(
                    point.getPartnerName(),
                    "СЦ МК Сестрица Сорока"
                )
            )
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "В маршруте отсутствует СЦ МК Сестрица Сорока"
            ));

        // Проверить, что доставляется МК Сорокой
        long deliveryServiceId = order.getDelivery().getDeliveryServiceId();
        Assertions.assertEquals(
            63132L, deliveryServiceId,
            "Заказ везет не МК Сестрица Сорока"
        );
    }

    @Test
    @DisplayName("Доставка в Воронеж в ПВЗ")
    public void createPVZOrderToVoronezh() {
        // Создать заказ
        params = CreateOrderParameters
            .newBuilder(193L, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.PICKUP)
            .paymentType(PaymentType.POSTPAID)
            .paymentMethod(PaymentMethod.CARD_ON_DELIVERY)
            .experiment(EnumSet.noneOf(RearrFactor.class))
            .outletId(outletId)
            .buyer(voronezhBuyer)
            .build();
        order = ORDER_STEPS.createOrder(params);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        CombinatorRoute combinatorRoute = LOM_ORDER_STEPS.getOrderRoute(lomOrderId).getText();
        List<CombinatorRoute.Point> routePoints = combinatorRoute.getRoute().getPoints();
        // Проверит, что доставка создалась через 63119 : СЦ МК Сестрица Сорока
        routePoints.stream()
            .filter(
                point -> point.getPartnerType() == PartnerType.SORTING_CENTER && Objects.equals(
                    point.getPartnerName(),
                    "СЦ МК Сестрица Сорока"
                )
            )
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "В маршруте отсутствует СЦ МК Сестрица Сорока"
            ));

        // Проверит, что доставка создалась через 63132 : МК Сестрица Сорока
        routePoints.stream()
            .filter(
                point -> point.getPartnerType() == PartnerType.DELIVERY && Objects.equals(
                    point.getPartnerName(),
                    "МК Сестрица Сорока"
                )
            )
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "В маршруте отсутствует МК Сестрица Сорока"
            ));

        // Проверит, что доставка создалась в 64514 : Партнёрский ПВЗ ООО Посоветуйте аниме
        routePoints.stream()
            .filter(
                point -> point.getPartnerType() == PartnerType.DELIVERY && Objects.equals(
                    point.getPartnerName(),
                    "Партнёрский ПВЗ ООО Посоветуйте аниме"
                )
            )
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "В маршруте отсутствует Партнёрский ПВЗ ООО Посоветуйте аниме"
            ));
    }

    @Test
    @DisplayName("Доставка в Воронеж в локер")
    public void createLockerOrderToVoronezh() {
        // Создать заказ
        params = CreateOrderParameters
            .newBuilder(193L, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.PICKUP)
            .paymentMethod(PaymentMethod.YANDEX)
            .paymentType(PaymentType.PREPAID)
            .experiment(EnumSet.noneOf(RearrFactor.class))
            .outletId(lockerOutletId)
            .buyer(voronezhBuyer)
            .build();
        order = ORDER_STEPS.createOrder(params);
        ORDER_STEPS.payOrder(order);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        CombinatorRoute combinatorRoute = LOM_ORDER_STEPS.getOrderRoute(lomOrderId).getText();
        List<CombinatorRoute.Point> routePoints = combinatorRoute.getRoute().getPoints();
        // Проверит, что доставка создалась через 63119 : СЦ МК Сестрица Сорока
        routePoints.stream()
            .filter(
                point -> point.getPartnerType() == PartnerType.SORTING_CENTER && Objects.equals(
                    point.getPartnerName(),
                    "СЦ МК Сестрица Сорока"
                )
            )
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "В маршруте отсутствует СЦ МК Сестрица Сорока"
            ));

        // Проверит, что доставка создалась через 63132 : МК Сестрица Сорока
        routePoints.stream()
            .filter(
                point -> point.getPartnerType() == PartnerType.DELIVERY && Objects.equals(
                    point.getPartnerName(),
                    "МК Сестрица Сорока"
                )
            )
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "В маршруте отсутствует МК Сестрица Сорока"
            ));

        // Проверит, что доставка создалась в 53058 : МП Мск Комб Тест
        routePoints.stream()
            .filter(
                point -> point.getPartnerType() == PartnerType.DELIVERY && Objects.equals(
                    point.getPartnerName(),
                    "МП Мск Комб Тест"
                )
            )
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "В маршруте отсутствует МП Мск Комб Тест"
            ));
    }
}
