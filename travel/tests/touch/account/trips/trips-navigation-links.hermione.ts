import {assert} from 'chai';
import {trips, burgerMenuActiveTrips} from 'suites/trips';

import TestApp from 'helpers/project/TestApp';

describe(trips.name, () => {
    it('Ссылки на активные поездки в меню-бургер', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {accountApp, aviaApp} = app;

        await accountApp.useTripsApiMock();

        await app.goAviaIndexPage();

        const header = aviaApp.indexPage.header;

        await header.navigationSideSheet.toggleButton.click();

        assert.isTrue(
            await header.navigationSideSheet.tripsLink.isVisible(),
            'В меню должен быть пункт "Мои поездки"',
        );

        assert.equal(
            await header.navigationSideSheet.tripsLink.getRelativePathName(),
            '/my/trips/',
            'Пункт "Мои поездки" должен вести на "/my/trips/"',
        );

        assert.equal(
            await header.navigationSideSheet.activeTripsList.activeTripListItems.count(),
            2,
            'В меню должны отображаться две активные поездки',
        );

        await header.navigationSideSheet.activeTripsList.activeTripListItems.forEach(
            async (trip, index) => {
                const expectedTrip = burgerMenuActiveTrips[index];
                const number = index + 1;

                assert.equal(
                    await trip.title.getText(),
                    expectedTrip.title,
                    `${number} поездка в меню должна содержать заголовок "${expectedTrip.title}"`,
                );

                if (expectedTrip.imageHref === null) {
                    assert.isTrue(
                        await trip.image.isStub(),
                        `${number} поездка в меню должна содержать картинку заглушку`,
                    );
                } else {
                    assert.equal(
                        await trip.image.image.getSrc(),
                        expectedTrip.imageHref,
                        `${number} поездка в меню должна содержать картинку "${expectedTrip.imageHref}"`,
                    );
                }

                assert.equal(
                    await trip.getRelativePathName(),
                    expectedTrip.link,
                    `${number} поездка в меню должна содержать ссылку "${expectedTrip.link}"`,
                );
            },
        );
    });
});
