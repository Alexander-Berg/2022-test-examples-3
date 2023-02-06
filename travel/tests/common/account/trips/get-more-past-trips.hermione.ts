import {assert} from 'chai';
import {trips, pastTripsMocks, morePastTrips} from 'suites/trips';

import TestApp from 'helpers/project/TestApp';

describe(trips.name, () => {
    it('Подгрузка прошедших поездок', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {accountApp} = app;

        await accountApp.useTripsApiMock();

        await accountApp.goTripsPage();

        const {tripsPage} = accountApp;

        await tripsPage.waitUntilLoaded();

        const expectedTripTitlesBeforeClick = pastTripsMocks.map(
            trip => trip.title,
        );

        assert.deepEqual(
            await tripsPage.content.pastTrips.getTitles(),
            expectedTripTitlesBeforeClick,
            `В блоке прошедших поездок отображается ${
                expectedTripTitlesBeforeClick.length
            } поездки ${expectedTripTitlesBeforeClick.join(', ')}`,
        );

        await tripsPage.content.moreTripsButton.click();

        await tripsPage.content.waitUntilMoreTripsLoaded();

        const expectedTripTitlesAfterClick = [
            ...pastTripsMocks,
            ...morePastTrips,
        ].map(trip => trip.title);

        assert.deepEqual(
            await tripsPage.content.pastTrips.getTitles(),
            expectedTripTitlesAfterClick,
            `В блоке прошедших поездок стало ${
                expectedTripTitlesAfterClick.length
            } поездки ${expectedTripTitlesAfterClick.join(', ')}`,
        );

        assert.isFalse(
            await tripsPage.content.moreTripsButton.isVisible(),
            'Кнопка "Показать еще" не должна отображаться',
        );
    });
});
