import {assert} from 'chai';
import {trip} from 'suites/trips';

import TestApp from 'helpers/project/TestApp';

describe(trip.name, () => {
    it('Блок PNR на странице моей поездки', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {
            accountApp,
            accountApp: {tripPage},
        } = app;

        await accountApp.useTripsApiMock();

        await tripPage.goToTrip();

        const firstAviaOrder = await tripPage.aviaOrdersBlock.orders.first();

        const {pnr, copyButton, copyModal} = firstAviaOrder.pnr;

        const pnrValue = await pnr.getText();

        await copyButton.scrollIntoView();

        await copyButton.click();

        const expectedTitle = app.isTouch
            ? 'Код бронирования'
            : 'Код скопирован';

        assert.isTrue(
            await copyModal.modalContent.isVisible(),
            'Открылся модал',
        );
        assert.equal(
            await copyModal.title.getText(),
            expectedTitle,
            `Заголовок модала должен быть "${expectedTitle}"`,
        );
        assert.equal(
            await copyModal.pnr.getText(),
            pnrValue,
            `PNR должен совпадать со значением на странице поездки`,
        );
        assert.equal(
            await copyModal.description.getText(),
            'На сайте авиакомпании с ним можно управлять бронированием и оформить регистрацию на рейс',
            'Должен быть текст "На сайте авиакомпании с ним можно управлять бронированием и оформить регистрацию на рейс"',
        );
        assert.equal(
            await copyModal.actionButton.getText(),
            'Перейти на сайт авиакомпании',
            'Должна быть кнопка "Перейти на сайт авиакомпании"',
        );

        const registrationUrl = await copyModal.actionButton.getUrl();

        assert.equal(
            registrationUrl?.origin,
            'https://www.aeroflot.ru',
            'Кнопка должна вести на aeroflot.ru',
        );
    });
});
