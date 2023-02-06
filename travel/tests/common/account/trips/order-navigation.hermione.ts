import {assert} from 'chai';
import {
    exampleTestingAviaOrder,
    exampleTestingHotelOrder,
    exampleTestingTrainOrder,
    trip,
} from 'suites/trips';

import {SECOND} from 'helpers/constants/dates';

import TestApp from 'helpers/project/TestApp';

const ORDERS = [
    exampleTestingAviaOrder,
    exampleTestingTrainOrder,
    exampleTestingHotelOrder,
];

describe(trip.name, () => {
    it('Переход к заказу из поездки', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {
            accountApp,
            accountApp: {tripPage},
            aviaApp,
            trainsApp,
        } = app;

        await accountApp.useTripsApiMock();

        /**
         * Получаем доступ к заказам, нужно из-за замоканных данных
         */
        for (const {prettyOrderId, email} of ORDERS) {
            await accountApp.tripsPage.searchOrder.searchOrder(
                prettyOrderId,
                email,
                10 * SECOND,
            );
        }

        await tripPage.goToTrip();

        const firstAviaOrder = await tripPage.aviaOrdersBlock.orders.first();

        await firstAviaOrder.link.click();

        await aviaApp.accountOrderPage.waitForPageLoading();

        assert.isTrue(
            await aviaApp.accountOrderPage.isDisplayed(),
            'Произошел переход на страницу авиа заказа',
        );

        await this.browser.back();

        const firstTrainOrder = await tripPage.trainOrdersBlock.orders.first();

        await firstTrainOrder.link.click();

        await trainsApp.genericOrderPage.waitOrderLoaded();

        assert.isTrue(
            await trainsApp.genericOrderPage.isDisplayed(),
            'Произошел переход на страницу ж/д заказа',
        );

        await this.browser.back();

        const firstHotelOrder = await tripPage.hotelOrdersBlock.orders.first();

        await firstHotelOrder.link.click();

        await accountApp.hotelOrderPage.waitOrderLoaded();

        assert.isTrue(
            await accountApp.hotelOrderPage.isDisplayed(),
            'Произошел переход на страницу отельного заказа',
        );
    });
});
