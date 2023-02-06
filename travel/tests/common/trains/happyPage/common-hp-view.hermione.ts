import {assert} from 'chai';
import {order} from 'suites/trains';

import {MINUTE} from 'helpers/constants/dates';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';

describe(order.steps.happyPage, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Общий вид HP', async function () {
        const app = new TestTrainsApp(this.browser);
        const {
            orderPlacesStepPage,
            orderPassengersStepPage,
            orderConfirmationStepPage,
            paymentPage,
            happyPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await app.setTestContext();
        await app.paymentTestContextHelper.setPaymentTestContext();

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        await app.setFirstPassengerViaFields(PASSENGER, CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();
        await orderConfirmationStepPage.goNextStep();

        await paymentPage.loader.waitUntilLoaded(120000);

        await happyPage.waitUntilLoaded();
        await happyPage.test();

        assert(
            await happyPage.orderActions.downloadButton.isVisible(),
            'На Happy page должна отображаться кнопка "Скачать"',
        );

        if (happyPage.isDesktop) {
            assert(
                await happyPage.orderActions.printButton.isVisible(),
                'На Happy page должна отображаться кнопка "Распечатать"',
            );
        }
    });
});
