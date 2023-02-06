import {assert} from 'chai';
import {order} from 'suites/trains';

import {MINUTE} from 'helpers/constants/dates';
import {
    MOCK_PAYMENT_URL,
    TRAINS_SUCCESS_TEST_CONTEXT_PARAMS,
} from 'helpers/constants/testContext';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';

describe(order.steps.pay, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Не удалось подтвердить страховку', async function () {
        const app = new TestTrainsApp(this.browser);
        const {
            orderPlacesStepPage,
            orderConfirmationStepPage,
            orderPassengersStepPage,
            paymentPage,
            genericOrderPage,
            happyPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        await app.setTestContext({
            ...TRAINS_SUCCESS_TEST_CONTEXT_PARAMS,
            insuranceCheckoutOutcome: 'ICO_FAILURE',
        });
        await app.paymentTestContextHelper.setPaymentTestContext({
            minUserActionDelay: 'PT10S',
            paymentOutcome: 'PO_SUCCESS',
            paymentUrl: MOCK_PAYMENT_URL,
        });

        await app.setFirstPassengerViaFields(PASSENGER, CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();

        const {price: priceFromConfirmation} =
            await app.getDataFromConfirmationPage();

        await orderConfirmationStepPage.addInsurance();

        await orderConfirmationStepPage.goNextStep();

        await paymentPage.waitUntilLoaded();

        assert.equal(
            await paymentPage.orderError.title.getText(),
            'Не удалось купить страховой полис',
            'Отображается некорректный заголовок ошибки',
        );
        assert.equal(
            await paymentPage.orderError.content.getText(),
            'При покупке страхового полиса произошла ошибка',
            'Отображается некорректное описание ошибки',
        );

        await paymentPage.orderError.primaryActionButton.click();
        await paymentPage.waitUntilLoaded();

        await happyPage.waitUntilLoaded();
        await happyPage.orderActions.detailsLink.click();

        await genericOrderPage.waitOrderLoaded();

        const firstPassenger =
            await genericOrderPage.passengers.passengers.first();
        const firstTicket = await firstPassenger.tickets.tickets.first();

        await firstPassenger.info.name.scrollIntoView();

        assert.isFalse(
            await firstTicket.insurance.price.isDisplayed(),
            'Не должна отображаться страховка',
        );

        assert.equal(
            priceFromConfirmation,
            await genericOrderPage.passengers.totalPrice.total.getPriceValue(),
            'Различаются цены со страницы подтверждения (без страховки) и заказа',
        );
    });
});
