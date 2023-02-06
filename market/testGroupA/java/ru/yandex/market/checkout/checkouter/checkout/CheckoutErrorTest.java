package ru.yandex.market.checkout.checkouter.checkout;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Stream;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.actions.MultiCartActions;
import ru.yandex.market.checkout.util.matching.CheckoutErrorMatchers;

import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_EMAIL_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_FULL_NAME_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_PHONE_ID;

/**
 * @author : poluektov
 * date: 16.08.17.
 */
public class CheckoutErrorTest extends AbstractWebTestBase {

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(new Object[][]{
                //https://testpalm.yandex-team.ru/testcase/checkouter-5
                {missingAddress(), "Создание заказа с ошибкой 'Missing address'"},
                //https://testpalm.yandex-team.ru/testcase/checkouter-6
                {emptyDeliveryOutlet(), "Создание заказа с ошибкой 'Missing address'"},
                //https://testpalm.yandex-team.ru/testcase/checkouter-7
                {pickupAddressAndOutlet(), "Создание заказа с ошибкой 'Buyer delivery address is not appropriate for " +
                        "delivery type PICKUP'"},
                //https://testpalm.yandex-team.ru/testcase/checkouter-8
                {deliveryAddressAndOutlet(), "Создание заказа с ошибкой 'message': 'Delivery outlet is not " +
                        "appropriate for delivery type DELIVERY'"},
                //https://testpalm.yandex-team.ru/testcase/checkouter-9
                {postAddressAndOutlet(), "Создание заказа с ошибкой 'message': 'Delivery outlet is not appropriate " +
                        "for delivery type POST'"},
                //https://testpalm.yandex-team.ru/testcase/checkouter-10
                {wrongRegionId(), "Создание заказа с ошибкой 'Unknown delivery region id: n'"},
                {withEmptyPersonalEmailId(), "Отсутствует идентификатор email в сервисе Personal"},
                {withWrongPersonalEmailId(), "Невалидный идентификатор email в сервисе Personal"},
                {withEmptyPersonalPhoneId(), "Отсутствует идентификатор телефона в сервисе Personal"},
                {withWrongPersonalPhoneId(), "Невалидный идентификатор телефона в сервисе Personal"},
                // checkouter-38, step 5
                {withDeliveryIntervalAndMissingFromDate(), "fromDate отсустствует"},
                //https://testpalm.yandex-team.ru/testcase/checkouter-77
                {missingPaymentMethod(), "Не указан метод оплаты"},
                // https://st.yandex-team.ru/MARKETCHECKOUT-6803
                {missingCarts(), "Не указана корзина"},
                {withEmptyPersonalFullNameId(), "Отсутствует идентификатор полного имени в сервисе Personal"},
                {withWrongPersonalFullNameId(), "Невалидный идентификатор полного имени в сервисе Personal"}
        }).stream().map(Arguments::of);
    }

    private static Parameters missingAddress() {
        Parameters missingAddress = new Parameters();
        missingAddress.setDeliveryType(DeliveryType.DELIVERY);
        missingAddress.setMultiCartAction(MultiCartActions.setDeliveryBuyerAddressNull);
        missingAddress.setErrorMatcher(CheckoutErrorMatchers.missingAddress);
        missingAddress.setExpectedCheckoutReturnCode(400);
        return missingAddress;
    }

    private static Parameters missingCarts() {
        Parameters missingCarts = new Parameters();
        missingCarts.setMultiCartAction(MultiCartActions.setCartsNull);
        missingCarts.setErrorMatcher(CheckoutErrorMatchers.missingCarts);
        missingCarts.setExpectedCheckoutReturnCode(400);
        return missingCarts;
    }

    private static Parameters missingPaymentMethod() {
        Parameters missingPaymentMethod = new Parameters();
        missingPaymentMethod.setMultiCartAction(MultiCartActions.setPaymentMethodNull);
        missingPaymentMethod.setErrorMatcher(CheckoutErrorMatchers.missingPaymentMethod);
        missingPaymentMethod.setExpectedCheckoutReturnCode(400);
        return missingPaymentMethod;
    }

    private static Parameters emptyDeliveryOutlet() {
        Parameters emptyDeliveryOutlet = new Parameters();
        emptyDeliveryOutlet.setDeliveryType(DeliveryType.PICKUP);
        emptyDeliveryOutlet.setMultiCartAction(MultiCartActions.setDeliveryOutletIdNull);
        emptyDeliveryOutlet.setErrorMatcher(CheckoutErrorMatchers.deliveryOutletIdEmpty);
        emptyDeliveryOutlet.setExpectedCheckoutReturnCode(400);
        return emptyDeliveryOutlet;
    }

    private static Parameters pickupAddressAndOutlet() {
        Parameters pickupAddressAndOutlet = new Parameters();
        pickupAddressAndOutlet.setDeliveryType(DeliveryType.PICKUP);
        pickupAddressAndOutlet.setMultiCartAction(MultiCartActions.setDeliveryBuyerAddressDefault);
        pickupAddressAndOutlet.setErrorMatcher(CheckoutErrorMatchers.buyerAddressIsNotAppropriate);
        pickupAddressAndOutlet.setExpectedCheckoutReturnCode(400);
        return pickupAddressAndOutlet;
    }

    private static Parameters deliveryAddressAndOutlet() {
        Parameters deliveryAddressAndOutlet = new Parameters();
        deliveryAddressAndOutlet.setDeliveryType(DeliveryType.DELIVERY);
        deliveryAddressAndOutlet.setMultiCartAction(MultiCartActions.setDeliveryOutletIdDefault);
        deliveryAddressAndOutlet.setErrorMatcher(CheckoutErrorMatchers.outletIdIsNotAppropriate);
        deliveryAddressAndOutlet.setExpectedCheckoutReturnCode(400);
        return deliveryAddressAndOutlet;
    }

    private static Parameters postAddressAndOutlet() {
        Parameters postAddressAndOutlet = new Parameters();
        postAddressAndOutlet.setColor(Color.WHITE);
        postAddressAndOutlet.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        postAddressAndOutlet.setDeliveryType(DeliveryType.POST);
        postAddressAndOutlet.setMultiCartAction(MultiCartActions.setDeliveryOutletIdDefault);
        postAddressAndOutlet.setErrorMatcher(CheckoutErrorMatchers.outletIdIsNotAppropriateForPost);
        postAddressAndOutlet.setExpectedCheckoutReturnCode(400);
        return postAddressAndOutlet;
    }

    private static Parameters wrongRegionId() {
        long regionId = 99999999L;
        Parameters params = new Parameters();
        params.setDeliveryType(DeliveryType.DELIVERY);
        params.setMultiCartAction(MultiCartActions.setRegionId(regionId));
        params.setErrorMatcher(CheckoutErrorMatchers.wrongRegionId(regionId));
        params.setExpectedCheckoutReturnCode(400);
        return params;
    }

    private static Parameters withEmptyEmail() {
        Parameters params = new Parameters();
        params.setDeliveryType(DeliveryType.DELIVERY);
        params.getBuyer().setEmail("");
        params.getBuyer().setPersonalEmailId(null);
        params.setErrorMatcher(CheckoutErrorMatchers.emptyEmail);
        params.setExpectedCheckoutReturnCode(400);
        return params;
    }

    private static Parameters withEmptyPersonalPhoneId() {
        Parameters params = new Parameters();
        params.setDeliveryType(DeliveryType.DELIVERY);
        params.getBuyer().setPersonalPhoneId("");
        params.setErrorMatcher(CheckoutErrorMatchers.emptyPersonalPhoneId);
        params.setExpectedCheckoutReturnCode(400);
        return params;
    }

    private static Parameters withWrongPersonalPhoneId() {
        Parameters params = new Parameters();
        params.setDeliveryType(DeliveryType.DELIVERY);
        params.getBuyer().setPersonalPhoneId("a".repeat(33));
        params.setErrorMatcher(CheckoutErrorMatchers.wrongPersonalPhoneId);
        params.setExpectedCheckoutReturnCode(400);
        return params;
    }

    private static Parameters withEmptyPersonalEmailId() {
        Parameters params = new Parameters();
        params.setDeliveryType(DeliveryType.DELIVERY);
        params.getBuyer().setPersonalEmailId("");
        params.setErrorMatcher(CheckoutErrorMatchers.emptyPersonalEmailId);
        params.setExpectedCheckoutReturnCode(400);
        return params;
    }

    private static Parameters withWrongPersonalEmailId() {
        Parameters params = new Parameters();
        params.setDeliveryType(DeliveryType.DELIVERY);
        params.getBuyer().setPersonalEmailId("a".repeat(33));
        params.setErrorMatcher(CheckoutErrorMatchers.wrongPersonalEmailId);
        params.setExpectedCheckoutReturnCode(400);
        return params;
    }

    private static Parameters withEmptyPersonalFullNameId() {
        Parameters params = new Parameters();
        params.setDeliveryType(DeliveryType.DELIVERY);
        params.getBuyer().setPersonalFullNameId("");
        params.setErrorMatcher(CheckoutErrorMatchers.emptyPersonalFullNameId);
        params.setExpectedCheckoutReturnCode(400);
        return params;
    }

    private static Parameters withWrongPersonalFullNameId() {
        Parameters params = new Parameters();
        params.setDeliveryType(DeliveryType.DELIVERY);
        params.getBuyer().setPersonalFullNameId("a".repeat(33));
        params.setErrorMatcher(CheckoutErrorMatchers.wrongPersonalFullNameId);
        params.setExpectedCheckoutReturnCode(400);
        return params;
    }

    @Deprecated
    private static Parameters withBuyerPhone(String phone) {
        Parameters params = new Parameters();
        params.setDeliveryType(DeliveryType.DELIVERY);
        params.getBuyer().setPhone(phone);
        params.getBuyer().setPersonalPhoneId(null);
        params.setErrorMatcher(CheckoutErrorMatchers.wrongPhone);
        params.setExpectedCheckoutReturnCode(400);
        return params;
    }

    private static Parameters withWrongBuyerPhoneAndValidPersonalPhoneId() {
        Parameters params = new Parameters();
        params.setDeliveryType(DeliveryType.DELIVERY);
        params.getBuyer().setPhone("0");
        params.setErrorMatcher(CheckoutErrorMatchers.wrongPhone);
        params.setExpectedCheckoutReturnCode(400);
        return params;
    }

    private static Parameters withDeliveryIntervalAndMissingFromDate() {
        LocalTime fromTime = LocalTime.of(8, 0);
        LocalTime toTime = LocalTime.of(12, 0);
        Parameters parameters = new Parameters();
        configureDeliveryInterval(fromTime, toTime, parameters, null);
        parameters.setExpectedCheckoutReturnCode(400);
        parameters.setErrorMatcher(CheckoutErrorMatchers.MISSING_DELIVERY_FROM_DATE);
        return parameters;
    }

    private static Parameters withNullFirstName() {

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addOrder(BlueParametersProvider.defaultBlueOrderParameters());
        parameters.setExpectedCheckoutReturnCode(400);
        parameters.setErrorMatcher(CheckoutErrorMatchers.MISSING_ERROR_NAME);
        parameters.setMultiCartAction(mc -> {
            Buyer buyerWithoutFirstName = BuyerProvider.getBuyer();
            buyerWithoutFirstName.setFirstName(null);
            buyerWithoutFirstName.setPersonalFullNameId(null);

            mc.getCarts().get(0).setBuyer(buyerWithoutFirstName);
        });
        return parameters;
    }

    private static void configureDeliveryInterval(LocalTime fromTime, LocalTime toTime, Parameters parameters) {
        configureDeliveryInterval(fromTime, toTime, parameters, DateUtil.addDay(new Date(), 1));
    }

    private static void configureDeliveryInterval(LocalTime fromTime, LocalTime toTime, Parameters parameters,
                                                  Date fromDate) {
        parameters.setMultiCartAction(mc -> {
            mc.getCarts().forEach(o -> {
                DeliveryDates deliveryDates = o.getDelivery().getDeliveryDates();
                deliveryDates.setFromDate(fromDate);
                deliveryDates.setToDate(DateUtil.addDay(new Date(), 1));
                deliveryDates.setFromTime(fromTime);
                deliveryDates.setToTime(toTime);
            });
        });
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @ParameterizedTest(name = "{1}")
    @MethodSource("parameterizedTestData")
    public void testCheckoutError(Parameters parameters, String caseName) {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_EMAIL_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_FULL_NAME_ID, true);
        orderCreateHelper.createMultiOrder(parameters);
    }

    /**
     * Тест обратной совместимости создания заказов с использованием открытого номера телефона.
     * Удалить в MARKETCHECKOUT-27094
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @Test
    @DisplayName("Невалидный телефон покупателя")
    public void testCheckoutWrongPhoneError() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, false);
        orderCreateHelper.createMultiOrder(withBuyerPhone("0"));
    }

    /**
     * Удалить в MARKETCHECKOUT-27094
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @Test
    @DisplayName("Невалидный телефон покупателя при наличии идентификатора Personal")
    public void testCheckoutWrongPhoneError2() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, false);
        orderCreateHelper.createMultiOrder(withWrongBuyerPhoneAndValidPersonalPhoneId());
    }

    /**
     * Тест обратной совместимости создания заказов с использованием открытого email.
     * Удалить в MARKETCHECKOUT-27094
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @Test
    @DisplayName("Пустой email покупателя")
    public void testCheckoutEmptyEmailError() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_EMAIL_ID, false);
        orderCreateHelper.createMultiOrder(withEmptyEmail());
    }

    /**
     * Тест обратной совместимости создания заказов с использованием открытого имени пользователя.
     * Удалить в MARKETCHECKOUT-27094
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @Test
    @DisplayName("Пустое имя покупателя")
    public void testCheckoutEmptyFullNameError() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_FULL_NAME_ID, false);
        orderCreateHelper.createMultiOrder(withNullFirstName());
    }
}
