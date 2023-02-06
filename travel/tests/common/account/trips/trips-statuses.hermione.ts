import {assert} from 'chai';
import {trips} from 'suites/trips';

import TestApp from 'helpers/project/TestApp';

describe(trips.name, () => {
    it('Поездка отменена', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {accountApp} = app;

        await accountApp.useTripsApiMock();

        await accountApp.goTripsPage();

        const {tripsPage} = accountApp;

        await tripsPage.waitUntilLoaded();

        const cancelledPastTrip =
            await tripsPage.content.pastTrips.pastTrips.find(async trip =>
                trip.tripCancelled.isDisplayed(),
            );

        assert.exists(
            cancelledPastTrip,
            'В списке прошлых и отмененных поездок должна быть отмененная поездка',
        );

        assert.equal(
            await cancelledPastTrip.tripCancelled.getText(),
            'Поездка отменена',
            'У поездки должна отображаться серая надпись "Поездка отменена"',
        );
    });
});
