import {assert} from 'chai';
import {order} from 'suites/trains';

import {MINUTE} from 'helpers/constants/dates';
import {TRAINS_SUCCESS_TEST_CONTEXT_PARAMS} from 'helpers/constants/testContext';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';

describe(order.steps.order, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Не удалось провести возврат', async function () {
        const app = new TestTrainsApp(this.browser);
        const {
            orderPlacesStepPage,
            orderPassengersStepPage,
            orderConfirmationStepPage,
            genericOrderPage,
            paymentPage,
            happyPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        await app.setTestContext({
            ...TRAINS_SUCCESS_TEST_CONTEXT_PARAMS,
            refundCheckoutOutcome: 'RCO_FAILURE',
        });
        await app.paymentTestContextHelper.setPaymentTestContext();

        await app.setFirstPassengerViaFields(PASSENGER, CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();
        await orderConfirmationStepPage.goNextStep();

        await paymentPage.waitUntilLoaded();

        await happyPage.waitUntilLoaded();
        await happyPage.orderActions.detailsLink.click();

        await genericOrderPage.waitOrderLoaded();

        const {orderOrchActionModal, errorModal} = genericOrderPage;

        const firstPassenger =
            await genericOrderPage.passengers.passengers.first();

        await firstPassenger.refundTicket();
        await orderOrchActionModal.loader.waitUntilLoaded();
        await orderOrchActionModal.submitButton.click();
        await orderOrchActionModal.loader.waitUntilLoaded();

        assert.equal(
            await errorModal.text.getText(),
            'Не удалось выполнить операцию',
            'Неверное описание в модале для ошибки',
        );
        assert.isTrue(
            await errorModal.retryButton.isDisplayed(),
            'Не отображается кнопка для перезагрузки страницы',
        );

        await errorModal.retryButton.click();
        await genericOrderPage.waitOrderLoaded();

        assert.isTrue(
            await firstPassenger.isRefundButtonDisplayed(),
            'Не отображается кнопка сдать билет',
        );
    });
});
