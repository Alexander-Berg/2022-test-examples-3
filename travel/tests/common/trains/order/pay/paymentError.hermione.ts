import {assert} from 'chai';
import {order} from 'suites/trains';

import {TRAINS_SUCCESS_TEST_CONTEXT_PARAMS} from 'helpers/constants/testContext';
import {MINUTE} from 'helpers/constants/dates';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';

describe(order.steps.pay, () => {
    hermione.config.testTimeout(5 * MINUTE);
    it('Ошибка подтверждения брони в ИМ', async function () {
        const app = new TestTrainsApp(this.browser);
        const {
            orderPlacesStepPage,
            orderConfirmationStepPage,
            orderPassengersStepPage,
            paymentPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        await app.setTestContext({
            ...TRAINS_SUCCESS_TEST_CONTEXT_PARAMS,
            confirmReservationOutcome: 'RCOO_FAILURE',
        });
        await app.paymentTestContextHelper.setPaymentTestContext();

        await app.setFirstPassengerViaFields(PASSENGER, CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();
        await orderConfirmationStepPage.goNextStep();

        assert.isTrue(
            await paymentPage.orderError.isDisplayed(3 * MINUTE),
            'Отображается ошибка оплаты',
        );

        const content = await paymentPage.orderError.content.getText();

        assert.equal(
            content,
            'Не удалось выкупить билет. Попробуйте ещё раз. Не волнуйтесь, деньги не списались. Если проблема повторяется, свяжитесь со службой поддержки по телефону 8 800 511-71-04.',
            'Отображается текст ошибки "Не удалось выкупить билет..."',
        );

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
