package ru.yandex.market.deliveryintegrationtests.delivery.tests.checkouter;

import java.util.EnumSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import dto.requests.checkouter.Address;
import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.OrderComment;
import dto.requests.checkouter.RearrFactor;
import factory.OfferItems;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.deliveryintegrationtests.delivery.tests.AbstractTest;

import static org.junit.jupiter.params.provider.Arguments.arguments;


public abstract class AbstractCheckoutTest extends AbstractTest {

    private static final long mskRegionId = 213;
    private static final long spbRegionId = 2;
    private static final long ekbRegionId = 54;
    //4 кейса закомменчены, до нахождения причин их падения в тикете https://st.yandex-team.ru/DELIVERY-36223
    public static Stream<Arguments> getParams() {
        return Stream.of(
                arguments((Supplier<CreateOrderParameters>) AbstractCheckoutTest::ffToDelivery, "FF заказа со склада 172 в курьерскую доставку в МСК"),
                arguments((Supplier<CreateOrderParameters>) AbstractCheckoutTest::ff171ToPickup, "FF заказа со склада 171 в ПВЗ в СПб"),
                arguments((Supplier<CreateOrderParameters>) AbstractCheckoutTest::dshScToPickup, "дропшип через СЦ заказа в ПВЗ в МСК"),
                arguments((Supplier<CreateOrderParameters>) AbstractCheckoutTest::dshScToDelivery, "дропшип через СЦ заказа в курьерскую доставку в МСК"),
                arguments((Supplier<CreateOrderParameters>) AbstractCheckoutTest::dshToDelivery, "дропшип заказа в курьерскую доставку в МСК"),
//                arguments((Supplier<CreateOrderParameters>) AbstractCheckoutTest::dshToPickup, "дропшип заказа в ПВЗ в МСК"),
                arguments((Supplier<CreateOrderParameters>) AbstractCheckoutTest::dshDrToDelivery, "дропофф заказа в курьерскую доставку в МСК"),
                arguments((Supplier<CreateOrderParameters>) AbstractCheckoutTest::dshExpressToDelivery, "дропшип заказа с экспресс доставкой в МСК"),
//                arguments((Supplier<CreateOrderParameters>) AbstractCheckoutTest::darkstoreToDelivery, "FF со склада 300 (Даркстор) заказа в курьерскую доставку в ЕКБ"),
                arguments((Supplier<CreateOrderParameters>) AbstractCheckoutTest::lavkaToTaxi, "FF со склада 172 заказа в лавку в МСК")
        );
    }

    private static CreateOrderParameters ffToDelivery() {
        return CreateOrderParameters
                .newBuilder(mskRegionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY).build();
    }

    private static CreateOrderParameters ff171ToPickup() {
        return CreateOrderParameters
                .newBuilder(spbRegionId, OfferItems.FF_171_UNFAIR_STOCK.getItem(), DeliveryType.PICKUP).build();
    }

    private static CreateOrderParameters dshScToPickup() {
        return CreateOrderParameters
                .newBuilder(mskRegionId, OfferItems.DROPSHIP_SC.getItem(), DeliveryType.PICKUP).build();
    }

    private static CreateOrderParameters dshScToDelivery() {
        return CreateOrderParameters
                .newBuilder(mskRegionId, OfferItems.DROPSHIP_SC.getItem(), DeliveryType.DELIVERY).build();
    }

    private static CreateOrderParameters dshToDelivery() {
        return CreateOrderParameters
                .newBuilder(mskRegionId, OfferItems.DROPSHIP_SD_COURIER.getItem(), DeliveryType.DELIVERY).build();
    }

    private static CreateOrderParameters dshToPickup() {
        return CreateOrderParameters
                .newBuilder(mskRegionId, OfferItems.DROPSHIP_SD_PICKUP.getItem(), DeliveryType.PICKUP).build();
    }

    private static CreateOrderParameters dshDrToDelivery() {
        return CreateOrderParameters
                .newBuilder(mskRegionId, OfferItems.DROPSHIP_DR.getItem(), DeliveryType.DELIVERY).build();
    }

    private static CreateOrderParameters dshExpressToDelivery() {
        return CreateOrderParameters
                .newBuilder(mskRegionId, OfferItems.DROPSHIP_EXPRESS.getItem(), DeliveryType.DELIVERY)
                .paymentType(PaymentType.PREPAID)
                .paymentMethod(PaymentMethod.YANDEX)
                .experiment(EnumSet.of(RearrFactor.EXPRESS))
                .comment(OrderComment.FIND_COURIER_FASTER)
                .build();
    }

    private static CreateOrderParameters darkstoreToDelivery() {
        return CreateOrderParameters
                .newBuilder(ekbRegionId, OfferItems.FF_300_UNFAIR_STOCK_EXPRESS.getItem(), DeliveryType.DELIVERY)
                .paymentType(PaymentType.PREPAID)
                .paymentMethod(PaymentMethod.YANDEX)
                .address(Address.EKB_DARKSTORE)
                .experiment(EnumSet.of(RearrFactor.EXPRESS))
                .comment(OrderComment.FIND_COURIER_FASTER)
                .build();
    }

    private static CreateOrderParameters lavkaToTaxi() {
        return CreateOrderParameters
                .newBuilder(mskRegionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
                .address(Address.LAVKA)
                .paymentType(PaymentType.PREPAID)
                .paymentMethod(PaymentMethod.YANDEX)
                .experiment(EnumSet.of(RearrFactor.LAVKA, RearrFactor.COMBINATORONDEMAND))
                .forceDeliveryId(1005471L)
                .build();

    }


    @AfterEach
    public void tearDown() {
        ORDER_STEPS.cancelOrderIfAllowed(order);
    }
}
