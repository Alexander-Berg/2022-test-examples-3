import {assert} from 'chai';
import {trip} from 'suites/trips';

import TestApp from 'helpers/project/TestApp';
import TestTripPage from 'helpers/project/account/pages/TripPage/TestTripPage';

describe(trip.name, () => {
    it('Отели, Авиа. Бронирование отменено', async function () {
        const tripPage = await prepareTripPage(this.browser);

        const ordersBlocks = [
            [tripPage.hotelOrdersBlock, 'отель'],
            [tripPage.aviaOrdersBlock, 'авиа'],
        ] as const;

        for (const [ordersBlock, orderType] of ordersBlocks) {
            const cancelledOrder = await ordersBlock.orders.find(order =>
                order.orderMainInfo.cancelCaption.isDisplayed(),
            );

            assert.exists(
                cancelledOrder,
                `В списке ${orderType} заказов должен быть отмененный заказ`,
            );

            await cancelledOrder.scrollIntoView();

            assert.isTrue(
                await cancelledOrder.orderMainInfo.isDisplayed(),
                `Должна отображаться основная информация о ${orderType} заказе`,
            );

            if ('pnr' in cancelledOrder) {
                assert.isFalse(
                    await cancelledOrder.pnr.isDisplayed(),
                    `Должна отсутствовать информация о PNR в ${orderType} заказе`,
                );
            }

            assert.equal(
                await cancelledOrder.orderMainInfo.cancelCaption.getText(),
                'Бронирование отменено',
                `Должна отображаться серая надпись "Бронирование отменено" на ${orderType} заказе`,
            );
        }
    });

    it('Отели. Чат поддержки для отменённого и активного заказов', async function () {
        const tripPage = await prepareTripPage(this.browser);

        const cancelledOrder = await tripPage.hotelOrdersBlock.orders.find(
            order => order.orderMainInfo.cancelCaption.isDisplayed(),
        );

        const notCancelledOrder = await tripPage.hotelOrdersBlock.orders.find(
            async order => {
                const isDisplayed =
                    await order.orderMainInfo.cancelCaption.isDisplayed();

                return !isDisplayed;
            },
        );

        const orders = [
            [notCancelledOrder, 'активный'],
            [cancelledOrder, 'отменённый'],
        ] as const;

        for (const [order, orderType] of orders) {
            assert.exists(
                order,
                `В списке отельных заказов должен быть ${orderType} заказ`,
            );

            await order.scrollIntoView();

            assert.isTrue(
                await order.supportAction.isDisplayed(),
                `${orderType} заказ. Должен отображаться чат поддержки`,
            );
        }
    });

    it('ЖД, Автобусы. Полный возврат', async function () {
        const tripPage = await prepareTripPage(this.browser);

        const orderBlocks = [
            [tripPage.trainOrdersBlock, 'ЖД'],
            [tripPage.busOrdersBlock, 'Автобусы'],
        ] as const;

        for (const [orderBlock, orderType] of orderBlocks) {
            const cancelledOrder = await orderBlock.orders.find(
                async order =>
                    (await order.orderMainInfo.cancelCaption.getText()) ===
                    'Оформлен полный возврат',
            );

            assert.exists(
                cancelledOrder,
                `В списке заказов ${orderType} должен быть заказ с полным возвратом`,
            );

            await cancelledOrder.scrollIntoView();

            assert.isTrue(
                await cancelledOrder.orderMainInfo.isDisplayed(),
                `Должна отображаться основная информация о ${orderType} заказе`,
            );

            assert.isFalse(
                await cancelledOrder.descriptionAndActions.isDisplayed(),
                `Должна отсутствовать дополнительная информация о ${orderType} заказе и действия`,
            );

            assert.equal(
                await cancelledOrder.orderMainInfo.cancelCaption.getText(),
                'Оформлен полный возврат',
                `Должна отображаться серая надпись "Оформлен полный возврат" на ${orderType} заказе`,
            );
        }
    });

    it('ЖД, Автобусы. Частичный возврат', async function () {
        const tripPage = await prepareTripPage(this.browser);

        const orderBlocks = [
            [tripPage.trainOrdersBlock, 'ЖД'],
            [tripPage.busOrdersBlock, 'Автобусы'],
        ] as const;

        const partialRefundRegExp = /Возврат .+ билет(а|ов)/;

        for (const [orderBlock, orderType] of orderBlocks) {
            const cancelledOrder = await orderBlock.orders.find(async order =>
                partialRefundRegExp.test(
                    await order.orderMainInfo.cancelCaption.getText(),
                ),
            );

            assert.exists(
                cancelledOrder,
                `В списке заказов ${orderType} должен быть заказ с частичным возвратом`,
            );

            await cancelledOrder.scrollIntoView();

            assert.isTrue(
                await cancelledOrder.orderMainInfo.isDisplayed(),
                `Должна отображаться основная информация о ${orderType} заказе`,
            );

            assert.isTrue(
                await cancelledOrder.descriptionAndActions.isDisplayed(),
                `Должна отображаться дополнительная информация о ${orderType} заказе и действия`,
            );

            assert.match(
                await cancelledOrder.orderMainInfo.cancelCaption.getText(),
                partialRefundRegExp,
                `Должна отображаться серая надпись "Возврат N билетов" на ${orderType} заказе`,
            );
        }
    });
});

async function prepareTripPage(
    browser: WebdriverIO.Browser,
): Promise<TestTripPage> {
    const app = new TestApp(browser);

    await app.loginRandomAccount();

    const {
        accountApp,
        accountApp: {tripPage},
    } = app;

    await accountApp.useTripsApiMock();

    await tripPage.goToTrip();

    return tripPage;
}
