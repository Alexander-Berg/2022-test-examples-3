import {assert} from 'chai';
import {trips, exampleTestingHotelOrder} from 'suites/trips';

import TestApp from 'helpers/project/TestApp';

describe(trips.name, () => {
    it('Поиск заказа (залогин)', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {accountApp} = app;

        await accountApp.useTripsApiMock();

        await accountApp.goTripsPage();

        const {tripsPage} = accountApp;

        await tripsPage.waitUntilLoaded();

        await tripsPage.content.searchOrderLink.click();

        assert.isTrue(
            await tripsPage.searchOrder.isOpened(),
            'Должен открыться попап для поиска заказа',
        );

        assert.equal(
            await tripsPage.searchOrder.orderSearchForm.content.title.getText(),
            'Поиск заказа',
            'Заголовок в попапе должен быть "Поиск заказа"',
        );

        await tripsPage.searchOrder.orderSearchForm.fillForm(
            exampleTestingHotelOrder.prettyOrderId,
            exampleTestingHotelOrder.email,
        );

        await tripsPage.searchOrder.orderSearchForm.submit();

        assert.equal(
            await app.getPagePathname(),
            `/my/order/${exampleTestingHotelOrder.orderId}`,
            `Открылась страница заказа "/my/order/${exampleTestingHotelOrder.orderId}"`,
        );
    });

    it('Поиск заказа (незалогин)', async function () {
        const app = new TestApp(this.browser);

        const {accountApp} = app;

        await accountApp.useTripsApiMock();

        await accountApp.goTripsPage();

        const {tripsPage} = accountApp;

        await tripsPage.waitUntilLoaded();

        assert.isTrue(
            await tripsPage.content.noAuthTripsPage.isVisible(),
            'Должна открыться страница с поиском заказа',
        );

        assert.equal(
            await tripsPage.content.noAuthTripsPage.searchFormTitle.getText(),
            'Поиск заказа',
            'Заголовок на странице должен быть "Поиск заказа"',
        );

        assert.isTrue(
            await tripsPage.content.noAuthTripsPage.orderSearchForm.content.enterAccountLink.isVisible(),
            'Должна быть ссылка "Войдите в аккаунт"',
        );

        await tripsPage.content.noAuthTripsPage.orderSearchForm.fillForm(
            exampleTestingHotelOrder.prettyOrderId,
            exampleTestingHotelOrder.email,
        );

        await tripsPage.content.noAuthTripsPage.orderSearchForm.submit();

        assert.equal(
            await app.getPagePathname(),
            `/my/order/${exampleTestingHotelOrder.orderId}`,
            `Открылась страница заказа "/my/order/${exampleTestingHotelOrder.orderId}"`,
        );
    });
});
