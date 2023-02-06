import {assert} from 'chai';
import {order} from 'suites/trains';

import {
    MOCK_PAYMENT_URL,
    TRAINS_SUCCESS_TEST_CONTEXT_PARAMS,
} from 'helpers/constants/testContext';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';
import skipBecauseProblemWithIM from 'helpers/skips/skipBecauseProblemWithIM';

describe(order.steps.confirmation, () => {
    skipBecauseProblemWithIM();
    it('ТК ЖД Ошибка при бронировании', async function () {
        const app = new TestTrainsApp(this.browser);
        const {orderPlacesStepPage, orderPassengersStepPage, paymentPage} = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        await app.setTestContext({
            ...TRAINS_SUCCESS_TEST_CONTEXT_PARAMS,
            createReservationOutcome: 'RCRO_FAILURE',
        });
        await app.paymentTestContextHelper.setPaymentTestContext({
            minUserActionDelay:
                app.paymentTestContextHelper.getProgressiveDelay(
                    10,
                    this.retryCount,
                ),
            paymentOutcome: 'PO_SUCCESS',
            paymentUrl: MOCK_PAYMENT_URL,
        });

        await app.setFirstPassengerViaFields(PASSENGER, CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        assert.isTrue(
            await paymentPage.orderError.isDisplayed(25000),
            'Отображается ошибка оплаты',
        );

        // Текст ошибки не провреяем, т.к. в тестовом контексте приходит замоканое значение

        await paymentPage.orderError.primaryActionButton.click();

        await orderPlacesStepPage.waitTrainDetailsLoaded();

        assert.isTrue(
            await orderPlacesStepPage.layout.orderSteps.searchStep.isDisplayed(
                4000,
            ),
            'Отображается кнопка "Выбор поезда"',
        );
    });
});
