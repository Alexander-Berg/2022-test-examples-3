import {assert} from 'chai';
import {trip, exampleTestingTrainOrder} from 'suites/trips';

import TestApp from 'helpers/project/TestApp';

describe(trip.name, () => {
    it('Печать и скачивание жд билетов на странице моей поездки', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {accountApp} = app;

        await accountApp.useTripsApiMock();

        /**
         * Получаем доступ к заказу
         */
        await accountApp.tripsPage.searchOrder.searchOrder(
            exampleTestingTrainOrder.prettyOrderId,
            exampleTestingTrainOrder.email,
        );

        const {tripPage} = accountApp;

        await tripPage.goToTrip();

        const trainOrder = await tripPage.trainOrdersBlock.orders.first();

        const {downloadButton} = trainOrder.descriptionAndActions;

        const expectedDownloadAttributes = {
            href: '/api/trains/downloadBlank/',
            download: `order-${exampleTestingTrainOrder.orderId}.pdf`,
        };

        assert.isTrue(
            await downloadButton.isVisible(),
            'У первого ж/д заказа должна отображаться ссылка на скачивание билетов',
        );
        assert.equal(
            await downloadButton.getRelativePathName(),
            expectedDownloadAttributes.href,
            `У первого ж/д заказа должна быть указана верная ссылка на скачивание "${expectedDownloadAttributes.href}"`,
        );
        assert.equal(
            await downloadButton.getAttribute('download'),
            expectedDownloadAttributes.download,
            `У первого ж/д заказа в ссылке на скачивание должен быть корректно указан аттрибут download "${expectedDownloadAttributes.download}"`,
        );
        assert.equal(
            await downloadButton.getRequestStatus(),
            200,
            `У первого ж/д заказа ссылка на скачивание должна корректно отрабатывать и возвращать статус 200`,
        );

        if (app.isTouch) {
            return;
        }

        const {printButton} = trainOrder.descriptionAndActions;

        assert.isTrue(
            await printButton.isVisible(),
            'У первого ж/д заказа должна отображаться кнопка для печати билетов (десктоп)',
        );

        await printButton.click();

        assert.isTrue(
            await app.printForm.isOpened(),
            'Открылась форма для печати билета (десктоп)',
        );
    });
});
