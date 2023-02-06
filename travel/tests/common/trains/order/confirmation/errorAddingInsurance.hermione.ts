import {assert} from 'chai';
import {order} from 'suites/trains';

import {TRAINS_SUCCESS_TEST_CONTEXT_PARAMS} from 'helpers/constants/testContext';
import {MINUTE} from 'helpers/constants/dates';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';

describe(order.steps.confirmation, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Ошибка добавления страховки', async function () {
        const app = new TestTrainsApp(this.browser);
        const {
            orderPlacesStepPage,
            orderConfirmationStepPage,
            orderPassengersStepPage,
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
            insuranceCheckoutConfirmOutcome: 'ICCO_FAILURE',
        });
        await app.paymentTestContextHelper.setPaymentTestContext();

        await app.setFirstPassengerViaFields(PASSENGER, CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();

        const {price: priceFromConfirmation} =
            await app.getDataFromConfirmationPage();

        await orderConfirmationStepPage.addInsurance();

        await orderConfirmationStepPage.goNextStep();

        await paymentPage.waitUntilLoaded();

        await happyPage.waitUntilLoaded();
        await happyPage.orderActions.detailsLink.click();

        await genericOrderPage.waitOrderLoaded();

        const firstPassenger = await genericOrderPage.passengers.passengers.at(
            0,
        );
        const firstTicket = await firstPassenger.tickets.tickets.at(0);
        const firstWarning = await genericOrderPage.warnings.warnings.at(0);

        await firstPassenger.info.name.scrollIntoView();

        assert.equal(
            await firstWarning.getText(),
            'При покупке страховых полисов произошла ошибка, поэтому билеты оформлены без страховки. Деньги за нее вернутся в ближайшее время.',
            'Отображается некорректный сообщение в предупреждении',
        );
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
