package ru.yandex.market.checkout.checkouter.checkout;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_EMAIL_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_FULL_NAME_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_PHONE_ID;

/**
 * Тесты обратной совместимости создания заказов с использованием открытых персональных данных.
 * Удалить после MARKETCHECKOUT-27094 и MARKETCHECKOUT-27942
 */
public class CreateOrderPersonalDataCompatibilityTest extends AbstractWebTestBase {

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @Test
    public void createOrderWithoutPersonalIds() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, false);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_EMAIL_ID, false);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_FULL_NAME_ID, false);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getBuyer().setPersonalPhoneId(null);
        parameters.getBuyer().setPersonalEmailId(null);
        parameters.getBuyer().setPersonalFullNameId(null);
        ((AddressImpl) parameters.getOrder().getDelivery().getBuyerAddress()).setPersonalPhoneId(null);
        ((AddressImpl) parameters.getOrder().getDelivery().getBuyerAddress()).setPersonalEmailId(null);
        ((AddressImpl) parameters.getOrder().getDelivery().getBuyerAddress()).setPersonalFullNameId(null);
        ((AddressImpl) parameters.getOrder().getDelivery().getBuyerAddress()).setPersonalAddressId(null);
        ((AddressImpl) parameters.getOrder().getDelivery().getBuyerAddress()).setPersonalGpsId(null);

        assertNotNull(parameters.getBuyer().getPhone());
        assertNotNull(parameters.getBuyer().getEmail());
        assertNotNull(parameters.getBuyer().getFirstName());
        assertNotNull(parameters.getBuyer().getLastName());
        assertNotNull(parameters.getOrder().getDelivery().getBuyerAddress().getPhone());
        assertNotNull(parameters.getOrder().getDelivery().getBuyerAddress().getRecipientEmail());
        assertNotNull(parameters.getOrder().getDelivery().getBuyerAddress().getRecipientPerson());

        Order createdOrder = orderCreateHelper.createOrder(parameters);

        assertNotNull(createdOrder.getId());
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @Test
    public void createOrderWithoutPersonalData() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_EMAIL_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_FULL_NAME_ID, true);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getBuyer().setPhone(null);
        parameters.getBuyer().setEmail(null);
        parameters.getBuyer().setFirstName(null);
        parameters.getBuyer().setMiddleName(null);
        parameters.getBuyer().setLastName(null);
        ((AddressImpl) parameters.getOrder().getDelivery().getBuyerAddress()).setPhone(null);
        ((AddressImpl) parameters.getOrder().getDelivery().getBuyerAddress()).setRecipientEmail(null);
        ((AddressImpl) parameters.getOrder().getDelivery().getBuyerAddress()).setRecipientPerson(null);
        ((AddressImpl) parameters.getOrder().getDelivery().getBuyerAddress()).setRecipient(null);

        assertNotNull(parameters.getBuyer().getPersonalPhoneId());
        assertNotNull(parameters.getBuyer().getPersonalEmailId());
        assertNotNull(parameters.getBuyer().getPersonalFullNameId());
        assertNotNull(parameters.getOrder().getDelivery().getBuyerAddress().getPersonalPhoneId());
        assertNotNull(parameters.getOrder().getDelivery().getBuyerAddress().getPersonalEmailId());
        assertNotNull(parameters.getOrder().getDelivery().getBuyerAddress().getPersonalFullNameId());

        Order createdOrder = orderCreateHelper.createOrder(parameters);

        assertNotNull(createdOrder.getId());
    }
}
