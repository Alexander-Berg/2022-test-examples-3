import {order} from 'suites/trains';
import {passengers} from 'suites/account';
import {assert} from 'chai';

import {MINUTE} from 'helpers/constants/dates';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import Account from 'helpers/project/common/passport/Account';
import PassengersPage from 'helpers/project/account/pages/PassengersPage/PassengersPage';
import passengerCyrillic from 'helpers/project/account/pages/PassengersPage/data/addPassengerCyrillic';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';

import {checkPassengerInfo, checkTrainInfo} from './utillities';

const {url: accountUrl} = passengers;

describe(order.steps.pay, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Основной флоу бронирования залогин', async function () {
        const accountManager = new Account();
        const {
            account: {login, password},
        } = await accountManager.getOrCreate();

        const accountPage = new PassengersPage(this.browser);

        await this.browser.login(login, password);
        await this.browser.url(accountUrl);

        await accountPage.removeAllPassengers();
        await accountPage.addPassenger(passengerCyrillic, true);

        const app = new TestTrainsApp(this.browser);

        const {
            orderPlacesStepPage,
            orderPassengersStepPage,
            orderConfirmationStepPage,
            genericOrderPage,
            happyPage,
            paymentPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        assert.isTrue(
            await orderPlacesStepPage.beddingIsChecked(),
            'Галка "Постельное белье" стоит или белье включено в стоимость',
        );

        const totalPrice = await orderPlacesStepPage.getTotalPrice();

        assert.isTrue(await totalPrice.isVisible(), 'Указана общая стоимость');
        await orderPlacesStepPage.goNextStep();

        await app.setTestContext();
        await app.paymentTestContextHelper.setPaymentTestContext();

        await app.setFirstPassengerViaFirstIntent(CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();

        const dataFromConfirmation = await app.getDataFromConfirmationPage();

        await orderConfirmationStepPage.goNextStep();

        await paymentPage.waitUntilLoaded();

        await happyPage.waitUntilLoaded();
        await happyPage.orderActions.detailsLink.click();

        await genericOrderPage.waitOrderLoaded();

        await checkTrainInfo(app, dataFromConfirmation);
        await checkPassengerInfo(app, PASSENGER);
    });
});
