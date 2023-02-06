package ru.yandex.market.checkout.checkouter.pay;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author : poluektov
 * date: 12.01.18.
 */

//Проверка вызовов баланса для синих заказов и обычного FF
@DirtiesContext
public class BluePaymentTest extends AbstractPaymentTestBase {

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Оплата синего заказа")
    @Test
    public void holdAndClearBlue() throws Exception {
        createUnpaidBlueOrder();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();

        paymentTestHelper.checkAgencyCommission();

        assertThat(order().getPayment().getPsContractExternalId(), is("test_external_id"));
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Оплата FF заказа - разные поставщики")
    @Test
    public void holdAndClearFF() throws Exception {
        createUnpaidFFOrder();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();

        assertThat(order().getPayment().getPsContractExternalId(), is("test_external_id"));
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Оплата FF заказа - один поставщик")
    @Test
    public void holdAndClearFFDiffShops() throws Exception {
        createUnpaidFFOrderWithDiffShop();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();

        assertThat(order().getPayment().getPsContractExternalId(), is("test_external_id"));
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Оплата FF заказа - 1P поставщик")
    @Test
    public void holdAndClearFF1P() throws Exception {
        orderServiceTestHelper.createUnpaidBlue1POrder();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();

        assertThat(order().getPayment().getPsContractExternalId(), is("test_external_id"));
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Оплата синего заказа с включеным спасибо")
    @Test
    public void holdAndClearBlueSpasibo() throws Exception {
        createUnpaidBlueOrder();
        paymentTestHelper.initAndHoldPayment();

        assertThat(order().getPayment().getPsContractExternalId(), is("test_external_id"));
    }
}
