package ru.yandex.market.api.cpa;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopActualDeliveryRegionalSettings;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DbUnitDataSet(before = "ShopMetaDataCreatorTest.before.csv")
class ShopMetaDataCreatorTest extends FunctionalTest {

    @Autowired
    private PartnerMetaDataCreator partnerMetaDataCreator;

    private static Stream<Arguments> params() {
        return Stream.of(
                Arguments.of("Магазин имеет незаапрувленную заявку", 1L, Optional.of(ShopMetaDataBuilder.of(1L)
                        .withBusninessId(1111)
                        .withAgencyCommission(1)
                        .withSandboxPaymentClass(PaymentClass.YANDEX)
                        .withProdPaymentClass(PaymentClass.OFF)
                        .withClientId(0L)
                        .withPrepayType(PrepayType.YANDEX_MARKET)
                        .withInn("2000")
                        .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                        .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                        .withPhoneNumber("+7(999)999-99-99")
                        .withOrderAutoAcceptEnabled(false)
                        .build())),
                Arguments.of("Магазин имеет заапрувленную заявку", 2L, Optional.of(ShopMetaDataBuilder.of(2L)
                        .withBusninessId(1111)
                        .withAgencyCommission(1)
                        .withSandboxPaymentClass(PaymentClass.YANDEX)
                        .withProdPaymentClass(PaymentClass.YANDEX)
                        .withClientId(2L)
                        .withPrepayType(PrepayType.YANDEX_MARKET)
                        .withInn("3000")
                        .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                        .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                        .withPhoneNumber("+7(999)999-99-99")
                        .withOrderAutoAcceptEnabled(true)
                        .build())),
                Arguments.of("У магазина нет заявки", 3L, Optional.empty()),
                Arguments.of("Магазин имеет заапрувленную заявку, но отключил предоплату", 4L, Optional.of(ShopMetaDataBuilder.of(4L)
                        .withBusninessId(1111)
                        .withAgencyCommission(1)
                        .withSandboxPaymentClass(PaymentClass.OFF)
                        .withProdPaymentClass(PaymentClass.OFF)
                        .withClientId(4L)
                        .withPrepayType(PrepayType.YANDEX_MARKET)
                        .withInn("3000")
                        .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                        .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                        .withPhoneNumber("+7(999)999-99-99")
                        .withActualDeliveryRegionalSettings(new ShopActualDeliveryRegionalSettings[]{})
                        .withActualDeliveryRegionalCalculationRules(new ArrayList<>())
                        .withOrderAutoAcceptEnabled(false)
                        .build())),
                Arguments.of("Проверка актуализации доставки. Магазин работает через API.", 5L, Optional.of(ShopMetaDataBuilder.of(5L)
                        .withBusninessId(1111)
                        .withAgencyCommission(1)
                        .withSandboxPaymentClass(PaymentClass.YANDEX)
                        .withProdPaymentClass(PaymentClass.YANDEX)
                        .withClientId(5L)
                        .withPrepayType(PrepayType.YANDEX_MARKET)
                        .withInn("3000")
                        .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                        .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                        .withPhoneNumber("+7(999)999-99-99")
                        .withActualDeliveryRegionalSettings(new ShopActualDeliveryRegionalSettings[] {})
                        .withActualDeliveryRegionalCalculationRules(new ArrayList<>())
                        .withOrderAutoAcceptEnabled(false)
                        .build())),
                Arguments.of("Проверка актуализации доставки. Магазин работает через PI. " +
                        "Актуализация через push-api (по типу доставки).", 6L, Optional.of(ShopMetaDataBuilder.of(6L)
                        .withBusninessId(1111)
                        .withAgencyCommission(1)
                        .withSandboxPaymentClass(PaymentClass.YANDEX)
                        .withProdPaymentClass(PaymentClass.YANDEX)
                        .withClientId(6L)
                        .withPrepayType(PrepayType.YANDEX_MARKET)
                        .withInn("3000")
                        .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                        .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                        .withPhoneNumber("+7(999)999-99-99")
                        .withActualDeliveryRegionalSettings(new ShopActualDeliveryRegionalSettings[]{})
                        .withActualDeliveryRegionalCalculationRules(new ArrayList<>())
                        .withOrderAutoAcceptEnabled(false)
                        .build())),
                Arguments.of("Проверка актуализации доставки. Магазин работает через PI. " +
                        "Актуализация через actual-delivery (по типу доставки).", 7L, Optional.of(ShopMetaDataBuilder.of(7L)
                        .withBusninessId(1112)
                        .withAgencyCommission(1)
                        .withSandboxPaymentClass(PaymentClass.YANDEX)
                        .withProdPaymentClass(PaymentClass.YANDEX)
                        .withClientId(7L)
                        .withPrepayType(PrepayType.YANDEX_MARKET)
                        .withInn("3000")
                        .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                        .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                        .withPhoneNumber("+7(999)999-99-99")
                        .withActualDeliveryRegionalSettings(new ShopActualDeliveryRegionalSettings[]{})
                        .withActualDeliveryRegionalCalculationRules(new ArrayList<>())
                        .withOrderAutoAcceptEnabled(false)
                        .build())),
                Arguments.of("Проверка актуализации доставки. Магазин работает через PI. " +
                        "Актуализация локального региона через push-api (по фиду).", 8L, Optional.of(ShopMetaDataBuilder.of(8L)
                        .withBusninessId(1112)
                        .withAgencyCommission(1)
                        .withSandboxPaymentClass(PaymentClass.YANDEX)
                        .withProdPaymentClass(PaymentClass.YANDEX)
                        .withClientId(8L)
                        .withPrepayType(PrepayType.YANDEX_MARKET)
                        .withInn("3000")
                        .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                        .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                        .withPhoneNumber("+7(999)999-99-99")
                        .withActualDeliveryRegionalSettings(new ShopActualDeliveryRegionalSettings[]{})
                        .withActualDeliveryRegionalCalculationRules(new ArrayList<>())
                        .withOrderAutoAcceptEnabled(false)
                        .build())),
                Arguments.of("Проверка актуализации доставки. Магазин работает через PI. " +
                        "Актуализация локального региона через push-api (по типу доставки).", 9L, Optional.of(ShopMetaDataBuilder.of(9L)
                        .withBusninessId(1112)
                        .withAgencyCommission(1)
                        .withSandboxPaymentClass(PaymentClass.YANDEX)
                        .withProdPaymentClass(PaymentClass.YANDEX)
                        .withClientId(9L)
                        .withPrepayType(PrepayType.YANDEX_MARKET)
                        .withInn("3000")
                        .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                        .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                        .withPhoneNumber("+7(999)999-99-99")
                        .withActualDeliveryRegionalSettings(new ShopActualDeliveryRegionalSettings[]{})
                        .withActualDeliveryRegionalCalculationRules(new ArrayList<>())
                        .withOrderAutoAcceptEnabled(false)
                        .build())),
                Arguments.of("Проверка актуализации доставки. Магазин работает через PI. " +
                        "Актуализация локального региона через actual-delivery (по типу доставки).", 10L, Optional.of(ShopMetaDataBuilder.of(10L)
                        .withBusninessId(1112)
                        .withAgencyCommission(1)
                        .withSandboxPaymentClass(PaymentClass.YANDEX)
                        .withProdPaymentClass(PaymentClass.YANDEX)
                        .withClientId(10L)
                        .withPrepayType(PrepayType.YANDEX_MARKET)
                        .withInn("3000")
                        .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                        .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                        .withPhoneNumber("+7(999)999-99-99")
                        .withActualDeliveryRegionalSettings(new ShopActualDeliveryRegionalSettings[]{})
                        .withActualDeliveryRegionalCalculationRules(new ArrayList<>())
                        .withOrderAutoAcceptEnabled(false)
                        .build())),
                Arguments.of("Проверка актуализации доставки. " +
                                "Актуализация локального региона через push-api (по типу доставки). Субрегионы Москвы", 11L,
                        Optional.of(ShopMetaDataBuilder.of(11L)
                                .withBusninessId(1112)
                                .withAgencyCommission(1)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(11L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("3000")
                                .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                                .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                                .withPhoneNumber("+7(999)999-99-99")
                                .withActualDeliveryRegionalSettings(new ShopActualDeliveryRegionalSettings[]{})
                                .withActualDeliveryRegionalCalculationRules(new ArrayList<>())
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("Проверка актуализации доставки. " +
                                "Актуализация локального региона через push-api (по типу доставки). Субрегионы Москвы", 11L,
                        Optional.of(ShopMetaDataBuilder.of(11L)
                                .withBusninessId(1112)
                                .withAgencyCommission(1)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(11L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("3000")
                                .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                                .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                                .withPhoneNumber("+7(999)999-99-99")
                                .withActualDeliveryRegionalSettings(new ShopActualDeliveryRegionalSettings[]{})
                                .withActualDeliveryRegionalCalculationRules(new ArrayList<>())
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("Проверка опции бесплатного подъема КГТ", 12L,
                        Optional.of(ShopMetaDataBuilder.of(12L)
                                .withBusninessId(1112)
                                .withAgencyCommission(1)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(12L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("3000")
                                .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                                .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                                .withFreeLiftingEnabled(true)
                                .withPhoneNumber("+7(999)999-99-99")
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("Проверка опции выключенного метода /cart", 13L,
                        Optional.of(ShopMetaDataBuilder.of(13L)
                                .withBusninessId(1112)
                                .withAgencyCommission(1)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(13L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("3000")
                                .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                                .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                                .withCartRequestTurnedOff(true)
                                .withPhoneNumber("+7(999)999-99-99")
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("Проверка опции необходимости чека доставки", 14L,
                        Optional.of(ShopMetaDataBuilder.of(14L)
                                .withBusninessId(1112)
                                .withAgencyCommission(1)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(14L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("2000")
                                .withVisibilityRule(OrderVisibility.BUYER, true)
                                .withVisibilityRule(OrderVisibility.BUYER_EMAIL, true)
                                .withVisibilityRule(OrderVisibility.BUYER_PHONE, true)
                                .withVisibilityRule(OrderVisibility.IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true)
                                .withDeliveryReceiptNeedType(2)
                                .withPhoneNumber("+7(999)999-99-99")
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("Проверка опции нового флоу выплат", 15L,
                        Optional.of(ShopMetaDataBuilder.of(15L)
                                .withBusninessId(1112)
                                .withAgencyCommission(1)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(15L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("2000")
                                .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                                .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                                .withPaymentControlEnabled(true)
                                .withPhoneNumber("+7(999)999-99-99")
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("У магазина нет номера телефона", 16L, Optional.empty()),
                Arguments.of("Проверка флага возможности доставки рецептурных препаратов", 17L,
                        Optional.of(ShopMetaDataBuilder.of(17L)
                                .withBusninessId(1112)
                                .withAgencyCommission(1)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(17L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("4000")
                                .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                                .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                                .withPrescriptionManagementSystem(1)
                                .withPhoneNumber("+7(999)999-99-99")
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("Проверка наличия медицинской лицензии", 18L,
                        Optional.of(ShopMetaDataBuilder.of(18L)
                                .withBusninessId(1112)
                                .withAgencyCommission(1)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(18L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("4000")
                                .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                                .withVisibilityRule(OrderVisibility.BUYER_PHONE, false)
                                .withMedicineLicense("LICENSE")
                                .withPhoneNumber("+7(999)999-99-99")
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                //Тестирование работы параметра PUSHAPI_ENABLE_BUYER_INFO
                Arguments.of("У магазина есть информация о покупателе при включённом параметре", 19L,
                        Optional.of(ShopMetaDataBuilder.of(19L)
                                .withBusninessId(1112)
                                .withAgencyCommission(1)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(19L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("4000")
                                .withVisibilityRule(OrderVisibility.BUYER, true)
                                .withVisibilityRule(OrderVisibility.BUYER_NAME, true)
                                .withVisibilityRule(OrderVisibility.BUYER_PHONE, true)
                                .withVisibilityRule(OrderVisibility.BUYER_FOR_EARLY_STATUSES, true)
                                .withVisibilityRule(OrderVisibility.BUYER_EMAIL, false)
                                .withPhoneNumber("+7(999)999-99-99")
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("У магазина нет информации о покупателе при выключенном параметре", 20L,
                        Optional.of(ShopMetaDataBuilder.of(20L)
                                .withBusninessId(1112)
                                .withAgencyCommission(1)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(20L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("4000")
                                .withPhoneNumber("+7(999)999-99-99")
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("У магазина нет информации о покупателе при отсутстующем параметре", 21L,
                        Optional.of(ShopMetaDataBuilder.of(21L)
                                .withBusninessId(1112)
                                .withAgencyCommission(1)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(21L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("4000")
                                .withPhoneNumber("+7(999)999-99-99")
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("У поставщика есть информация о покупателе при включённом параметре", 22L,
                        Optional.of(ShopMetaDataBuilder.of(22L)
                                .withBusninessId(1112)
                                .withAgencyCommission(200)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(22L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("4000")
                                .withVisibilityRule(OrderVisibility.BUYER, true)
                                .withVisibilityRule(OrderVisibility.BUYER_NAME, true)
                                .withVisibilityRule(OrderVisibility.BUYER_PHONE, true)
                                .withVisibilityRule(OrderVisibility.BUYER_FOR_EARLY_STATUSES, true)
                                .withPhoneNumber("+7(999)999-99-99")
                                .withSupplierFastReturnEnabled(true)
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("У поставщика нет информации о покупателе при выключенном параметре", 23L,
                        Optional.of(ShopMetaDataBuilder.of(23L)
                                .withBusninessId(1112)
                                .withAgencyCommission(200)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(23L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("4000")
                                .withPhoneNumber("+7(999)999-99-99")
                                .withSupplierFastReturnEnabled(true)
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("У поставщика нет информации о покупателе при отсутстующем параметре", 24L,
                        Optional.of(ShopMetaDataBuilder.of(24L)
                                .withBusninessId(1112)
                                .withAgencyCommission(200)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(24L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("4000")
                                .withPhoneNumber("+7(999)999-99-99")
                                .withSupplierFastReturnEnabled(true)
                                .withOrderAutoAcceptEnabled(false)
                                .build())),
                Arguments.of("Проверка разрешения на видимость email", 25L,
                        Optional.of(ShopMetaDataBuilder.of(25L)
                                .withBusninessId(1112)
                                .withAgencyCommission(200)
                                .withSandboxPaymentClass(PaymentClass.YANDEX)
                                .withProdPaymentClass(PaymentClass.YANDEX)
                                .withClientId(25L)
                                .withPrepayType(PrepayType.YANDEX_MARKET)
                                .withInn("2000")
                                .withVisibilityRule(OrderVisibility.BUYER, true)
                                .withVisibilityRule(OrderVisibility.BUYER_EMAIL, true)
                                .withVisibilityRule(OrderVisibility.BUYER_PHONE, true)
                                .withVisibilityRule(OrderVisibility.IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true)
                                .withPhoneNumber("+7(999)999-99-99")
                                .withSupplierFastReturnEnabled(true)
                                .withOrderAutoAcceptEnabled(false)
                                .build()))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("params")
    void checkMetaDataCreation(String testName, long shopId, Optional<ShopMetaData> expected) {
        assertEquals(expected, partnerMetaDataCreator.createMetadata(shopId));
    }
}
