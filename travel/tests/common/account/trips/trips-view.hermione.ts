import {assert} from 'chai';
import {trips, activeTripsMocks, pastTripsMocks} from 'suites/trips';

import TestApp from 'helpers/project/TestApp';

describe(trips.name, () => {
    it('Общий вид страницы (есть и будущие и прошедшие проездки)', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {accountApp, aviaApp} = app;

        await accountApp.useTripsApiMock();

        await app.goAviaIndexPage();

        await aviaApp.indexPage.header.clickTripsLink();

        const {tripsPage} = accountApp;

        await tripsPage.waitUntilLoaded();

        assert.isTrue(
            await tripsPage.isVisible(),
            'Должна открыться страница Мои поездки',
        );

        if (!app.isTouch) {
            assert.isTrue(
                await tripsPage.accountMenu.tripsTab.isVisible(),
                'На странице в десктопе должна быть вкладка "Мои поездки"',
            );
            assert.isTrue(
                await tripsPage.accountMenu.passengersTab.isVisible(),
                'На странице в десктопе должна быть вкладка "Пассажиры"',
            );
            assert.isTrue(
                await tripsPage.accountMenu.tripsTab.isActive(),
                'На странице в десктопе вкладка "Мои поездки" должна быть активна',
            );
        }

        assert.isTrue(
            await tripsPage.isSupportPhoneVisible(),
            'На странице должен быть текст "Поможем с заказом" и номер телефона',
        );

        assert.isTrue(
            await tripsPage.content.searchOrderLink.isVisible(),
            'На странице должна быть ссылка "Найти свой заказ"',
        );

        await tripsPage.content.activeTrips.forEach(
            async (activeTrip, index) => {
                const expectedActiveTrip = activeTripsMocks[index];

                const number = index + 1;

                assert.equal(
                    await activeTrip.title.getText(),
                    expectedActiveTrip.title,
                    `Должен отображаться правильный заголовок активной поездки номер ${number}`,
                );

                assert.equal(
                    await activeTrip.displayDate.getText(),
                    expectedActiveTrip.displayDate,
                    `Должна отображаться правильная дата активной поездки номер ${number}`,
                );

                if (expectedActiveTrip.imageHref === null) {
                    assert.isTrue(
                        await activeTrip.tripImage.isStub(),
                        `Должна отображаться заглушка при отсутствии картинки активной поездки номер ${number}`,
                    );
                } else {
                    assert.equal(
                        await activeTrip.tripImage.image.getSrc(),
                        expectedActiveTrip.imageHref,
                        `Должна отображаться верная картинка для активной поездки номер ${number}`,
                    );
                }

                assert.equal(
                    await activeTrip.link.getRelativePathName(),
                    expectedActiveTrip.link,
                    `Ссылка активной поездки должна вести в правильный раздел ${number}`,
                );
            },
        );

        await tripsPage.content.pastTrips.pastTrips.forEach(
            async (pastTrip, index) => {
                const expectedPastTrip = pastTripsMocks[index];

                const number = index + 1;

                assert.equal(
                    await pastTrip.title.getText(),
                    expectedPastTrip.title,
                    `Должен отображаться правильный заголовок прошедшей поездки номер ${number}`,
                );

                assert.equal(
                    await pastTrip.displayDate.getText(),
                    expectedPastTrip.displayDate,
                    `Должна отображаться правильная дата прошедшей поездки номер ${number}`,
                );

                if (expectedPastTrip.imageHref === null) {
                    assert.isTrue(
                        await pastTrip.tripImage.isStub(),
                        `Должна отображаться заглушка при отсутствии картинки прошедшей поездки номер ${number}`,
                    );
                } else {
                    assert.equal(
                        await pastTrip.tripImage.image.getSrc(),
                        expectedPastTrip.imageHref,
                        `Должна отображаться верная картинка для прошедшей поездки номер ${number}`,
                    );
                }

                assert.equal(
                    await pastTrip.getRelativePathName(),
                    expectedPastTrip.link,
                    `Ссылка прошедшей поездки должна вести в правильный раздел ${number}`,
                );
            },
        );
    });

    it('Общий вид страницы (есть только прошедшие проездки)', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {accountApp} = app;

        await accountApp.useTripsApiMock({activeTripsCount: 0});

        await accountApp.goTripsPage();

        const {tripsPage} = accountApp;

        await tripsPage.waitUntilLoaded();

        assert.isTrue(
            await tripsPage.content.emptyTripsPlaceholder.illustration.isVisible(),
            'Должна отображаться картинка-заглушка',
        );

        assert.equal(
            await tripsPage.content.emptyTripsPlaceholder.noOrdersDescription.getText(),
            'Никуда не собираетесь? Это стоит исправить',
            'Текст картинки-заглушки должен быть "Никуда не собираетесь? Это стоит исправить"',
        );

        assert.equal(
            await tripsPage.content.emptyTripsPlaceholder.noOrdersButton.getText(),
            'Подобрать билеты или отель',
            'Текст кнопки картинки-заглушки должен быть "Подобрать билеты или отель"',
        );

        assert.equal(
            await tripsPage.content.emptyTripsPlaceholder.noOrdersButton.getRelativePathName(),
            '/',
            'Кнопка с картинки-заглушки должна вести на главную',
        );

        assert.isTrue(
            await tripsPage.content.pastTripsTitle.isVisible(),
            'Должен отображаться заголовок "Прошлые поездки"',
        );

        assert.isNotEmpty(
            await tripsPage.content.pastTrips.pastTrips.items,
            'Должны отображаться прошлые поездки',
        );
    });

    it('Общий вид страницы (есть только активные поездки)', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {accountApp} = app;

        await accountApp.useTripsApiMock({
            activeTripsCount: 1,
            pastTripsCount: 0,
        });

        await accountApp.goTripsPage();

        const {tripsPage} = accountApp;

        await tripsPage.waitUntilLoaded();

        assert.equal(
            await tripsPage.content.activeTrips.count(),
            1,
            'Должна быть одна активная поездка',
        );

        const firstActiveTrip = await tripsPage.content.activeTrips.first();

        assert.equal(
            await firstActiveTrip.title.getText(),
            'Санкт–Петербург',
            'Заголовок активной поездки должен быть "Санкт–Петербург"',
        );

        assert.isFalse(
            await tripsPage.content.pastTripsTitle.isVisible(),
            'Должен отсутствовать заголовок "Прошлые поездки"',
        );

        assert.equal(
            await tripsPage.content.pastTrips.pastTrips.count(),
            0,
            'Должны отсутствовать прошлые поездки',
        );
    });
});
