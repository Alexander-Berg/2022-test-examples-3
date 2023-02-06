import {assert} from 'chai';
import {trip, trips, tripMocks} from 'suites/trips';

import TestApp from 'helpers/project/TestApp';

describe(trip.name, () => {
    it('Общий вид страницы Моя поездка', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {
            accountApp,
            accountApp: {tripPage},
        } = app;

        await accountApp.useTripsApiMock();

        await tripPage.goToTrip();

        assert.isTrue(
            await tripPage.backLink.isVisible(),
            'На странице должна быть хлебная крошка "Мои поездки"',
        );
        assert.equal(
            await tripPage.backLink.getRelativePathName(),
            trips.url,
            'Хлебная крошка должна вести на страницу со списком поездок',
        );

        assert.isTrue(
            await tripPage.isSupportPhoneVisible(),
            'На странице должен быть текст "Поможем с заказом" с номером телефона',
        );
        assert.isTrue(
            await tripPage.tripImage.isVisible(),
            'На странице должно быть фото поездки',
        );

        // forecast block

        assert.isTrue(
            await tripPage.forecastBlock.isVisible(),
            'Должен отображаться блок с прогнозом погоды',
        );
        assert.isAbove(
            await tripPage.forecastBlock.items.count(),
            1,
            'Должно быть несколько элементов прогноза (> 1)',
        );
        await tripPage.forecastBlock.items.forEach(async (forecastItem, i) => {
            assert.isTrue(
                await forecastItem.image.isVisible(),
                `В ${i} элементе погоды должна быть иллюстрация погоды`,
            );
            assert.isNotEmpty(
                await forecastItem.title.getText(),
                `В ${i} элементе погоды должен быть указан период прогноза`,
            );
            assert.isNotEmpty(
                await forecastItem.description.getText(),
                `В ${i} элементе погоды должна быть указана температура`,
            );
            assert.isNotNull(
                await forecastItem.link.getUrl(),
                `В ${i} элементе погоды должна быть указана ссылка на Яндекс.Погоду`,
            );
        });

        // avia block

        assert.equal(
            await tripPage.aviaOrdersBlock.title.getText(),
            'Авиабилеты',
            'На странице должен быть блок "Авиабилеты"',
        );
        assert.equal(
            await tripPage.aviaOrdersBlock.orders.count(),
            tripMocks.aviaOrders.length,
            `В блоке авиабилетов должно быть ${tripMocks.aviaOrders.length} заказов`,
        );

        await tripPage.aviaOrdersBlock.orders.forEach(
            async (aviaOrder, index) => {
                const expectedAviaOrder = tripMocks.aviaOrders[index];
                const number = index + 1;

                if (!expectedAviaOrder) {
                    return;
                }

                assert.equal(
                    await aviaOrder.orderMainInfo.title.getText(),
                    expectedAviaOrder.title,
                    `Должен быть заголовок "${expectedAviaOrder.title}" в авиа заказе #${number}`,
                );
                assert.equal(
                    await aviaOrder.orderMainInfo.dateForward.getText(),
                    expectedAviaOrder.dateForward,
                    `Должна быть дата туда "${expectedAviaOrder.dateForward}" в авиа заказе #${number}`,
                );

                if (expectedAviaOrder.dateBackward) {
                    assert.equal(
                        await aviaOrder.orderMainInfo.dateBackward.getText(),
                        expectedAviaOrder.dateBackward,
                        `Должна быть дата обратно "${expectedAviaOrder.dateForward}" в авиа заказе #${number}`,
                    );
                } else {
                    assert.isFalse(
                        await aviaOrder.orderMainInfo.dateBackward.isVisible(),
                        `Не должно быть даты обратно в авиа заказе #${number}`,
                    );
                }

                assert.equal(
                    await aviaOrder.orderMainInfo.areDirectionsVisible(),
                    expectedAviaOrder.directionsShown,
                    expectedAviaOrder.directionsShown
                        ? `Должны отображаться надписи "туда" и "обратно" в авиа заказе #${number}`
                        : `Не должны отображаться надписи "туда" и "обратно" в авиа заказе #${number}`,
                );
                assert.equal(
                    await aviaOrder.logo.airlines.count(),
                    expectedAviaOrder.logosCount,
                    `Должно отображаться ${expectedAviaOrder.logosCount} иконок авиакомпаний в авиа заказе #${number}`,
                );
                assert.equal(
                    await aviaOrder.pnr.description.getText(),
                    'Код бронирования (PNR)',
                    `Должен отображаться текст "Код бронирования (PNR)" в авиа заказе #${number}`,
                );
                assert.equal(
                    await aviaOrder.pnr.pnr.getText(),
                    expectedAviaOrder.pnr,
                    `Должен отображаться pnr "${expectedAviaOrder.pnr}" в авиа заказе #${number}`,
                );
                assert.isTrue(
                    await aviaOrder.pnr.copyButton.isVisible(),
                    `Должна быть кнопка копирования в авиа заказе #${number}`,
                );
                assert.equal(
                    await aviaOrder.link.getRelativePathName(),
                    expectedAviaOrder.link,
                    `Ссылка должна вести на страницу этого заказа в авиа заказе #${number}`,
                );
            },
        );

        assert.equal(
            await tripPage.trainOrdersBlock.title.getText(),
            'Ж/д билеты',
            'На странице должен быть блок "Ж/д билеты"',
        );
        assert.equal(
            await tripPage.trainOrdersBlock.orders.count(),
            tripMocks.trainOrders.length,
            `В блоке ж/д билетов должно быть ${tripMocks.trainOrders.length} заказов`,
        );

        // trains block

        await tripPage.trainOrdersBlock.orders.forEach(
            async (trainOrder, index) => {
                const expectedTrainOrder = tripMocks.trainOrders[index];
                const number = index + 1;

                if (!expectedTrainOrder) {
                    return;
                }

                assert.equal(
                    await trainOrder.orderMainInfo.title.getText(),
                    expectedTrainOrder.title,
                    `Должен быть заголовок "${expectedTrainOrder.title}" в ж/д заказе #${number}`,
                );
                assert.equal(
                    await trainOrder.orderMainInfo.dateForward.getText(),
                    expectedTrainOrder.dateForward,
                    `Должна быть дата туда "${expectedTrainOrder.dateForward}" в ж/д заказе #${number}`,
                );

                if (expectedTrainOrder.dateBackward) {
                    assert.equal(
                        await trainOrder.orderMainInfo.dateBackward.getText(),
                        expectedTrainOrder.dateBackward,
                        `Должна быть дата обратно "${expectedTrainOrder.dateForward}" в ж/д заказе #${number}`,
                    );
                } else {
                    assert.isFalse(
                        await trainOrder.orderMainInfo.dateBackward.isVisible(),
                        `Не должно быть даты обратно в ж/д заказе #${number}`,
                    );
                }

                assert.equal(
                    await trainOrder.orderMainInfo.areDirectionsVisible(),
                    expectedTrainOrder.directionsShown,
                    expectedTrainOrder.directionsShown
                        ? `Должны отображаться надписи "туда" и "обратно" в ж/д заказе #${number}`
                        : `Не должны отображаться надписи "туда" и "обратно" в ж/д заказе #${number}`,
                );

                if (expectedTrainOrder.isCancelled) {
                    assert.isFalse(
                        await trainOrder.descriptionAndActions.isVisible(),
                        `Не должно быть описаний поездов в ж/д заказе #${number}`,
                    );
                    assert.isFalse(
                        await trainOrder.descriptionAndActions.downloadButton.isVisible(),
                        `Не должно быть кнопки скачивания в ж/д заказе #${number}`,
                    );
                    assert.isFalse(
                        await trainOrder.descriptionAndActions.printButton.isVisible(),
                        `Не должно быть кнопки печати в ж/д заказе #${number}`,
                    );
                    assert.equal(
                        await trainOrder.orderMainInfo.cancelCaption.getText(),
                        'Оформлен полный возврат',
                        `Должна отображаться надпись "Оформлен полный возврат" в ж/д заказе #${number}`,
                    );
                } else {
                    if (expectedTrainOrder.hasTransferWithStationChange) {
                        assert.equal(
                            await trainOrder.descriptionAndActions.trainDescription.combinedDescription.getText(),
                            `Поезда ${expectedTrainOrder.trains[0].number} и ${expectedTrainOrder.trains[1].number}`,
                            `Должны быть верные номера поездов в описании в ж/д заказе #${number}`,
                        );
                        assert.equal(
                            await trainOrder.descriptionAndActions.trainDescription.transferText.getText(),
                            'Пересадка со сменой вокзала',
                            `Должна быть надпись "Пересадка со сменой вокзала" в ж/д заказе #${number}`,
                        );
                    } else {
                        assert.equal(
                            await trainOrder.descriptionAndActions.trainDescription.trainDescriptions.count(),
                            expectedTrainOrder.trains.length,
                            `Должно быть ${expectedTrainOrder.trains.length} описаний поездов в ж/д заказе #${number}`,
                        );

                        await trainOrder.descriptionAndActions.trainDescription.trainDescriptions.forEach(
                            async (trainDescription, trainIndex) => {
                                const expectedTrain =
                                    expectedTrainOrder.trains[trainIndex];

                                assert.equal(
                                    await trainDescription.getText(),
                                    `Поезд ${expectedTrain.number} ${
                                        expectedTrain.from
                                    } — ${expectedTrain.to}${
                                        expectedTrain.firmName
                                            ? `, «${expectedTrain.firmName}»`
                                            : ''
                                    }`,
                                    `Должны быть верные данные поезда в описании поезда #${
                                        trainIndex + 1
                                    } в ж/д заказе #${number}`,
                                );
                            },
                        );
                    }

                    assert.isTrue(
                        await trainOrder.descriptionAndActions.downloadButton.isVisible(),
                        `Должна быть кнопка скачивания в ж/д заказе #${number}`,
                    );

                    if (app.isDesktop) {
                        assert.isTrue(
                            await trainOrder.descriptionAndActions.printButton.isVisible(),
                            `На десктопе должна быть кнопка печати в ж/д заказе #${number}`,
                        );
                    }
                }

                assert.equal(
                    await trainOrder.link.getRelativePathName(),
                    expectedTrainOrder.link,
                    `Ссылка должна вести на страницу этого заказа в ж/д заказе #${number}`,
                );
            },
        );

        // hotels block

        assert.equal(
            await tripPage.hotelOrdersBlock.title.getText(),
            'Отели',
            'На странице должен быть блок "Отели"',
        );
        assert.equal(
            await tripPage.hotelOrdersBlock.orders.count(),
            tripMocks.hotelOrders.length,
            `В блоке отелей должно быть ${tripMocks.hotelOrders.length} заказов`,
        );

        await tripPage.hotelOrdersBlock.orders.forEach(
            async (hotelOrder, index) => {
                const expectedHotelOrder = tripMocks.hotelOrders[index];
                const number = index + 1;

                if (!expectedHotelOrder) {
                    return;
                }

                assert.equal(
                    await hotelOrder.orderMainInfo.title.getText(),
                    expectedHotelOrder.title,
                    `Должен быть заголовок "${expectedHotelOrder.title}" в отельном заказе #${number}`,
                );
                assert.equal(
                    await hotelOrder.orderMainInfo.dateForward.getText(),
                    expectedHotelOrder.dates,
                    `Должны быть даты "${expectedHotelOrder.dates}" в отельном заказе #${number}`,
                );

                if (expectedHotelOrder.isCancelled) {
                    assert.equal(
                        await hotelOrder.orderMainInfo.cancelCaption.getText(),
                        'Бронирование отменено',
                        `Должна быть надпись "Бронирование отменено" в отельном заказе #${number}`,
                    );
                }

                assert.equal(
                    await hotelOrder.link.getRelativePathName(),
                    expectedHotelOrder.link,
                    `Ссылка должна вести на страницу этого заказа в отельном заказе #${number}`,
                );
            },
        );

        // hotels cross sale block

        assert.equal(
            await tripPage.hotelsCrossSaleBlock.title.getText(),
            `Выберите подходящий отель в Санкт–Петербурге`,
            'Должен быть блок отельного кросс-сейла с заголовком "Выберите подходящий отель в Санкт–Петербурге"',
        );
    });
});
