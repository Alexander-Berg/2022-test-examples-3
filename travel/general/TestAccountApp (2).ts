import {trips} from 'suites/trips';

import PassengersPage from 'helpers/project/account/pages/PassengersPage/PassengersPage';
import TestHotelOrderPage from 'helpers/project/account/pages/OrderPage/TestHotelOrderPage';
import TestTripsPage from 'helpers/project/account/pages/TripsPage/TestTripsPage';
import TestTripPage from 'helpers/project/account/pages/TripPage/TestTripPage';
import {setAbExperiment} from 'helpers/utilities/experiment/setAbExperiment';

import {Component} from 'components/Component';

export default class TestAccountApp extends Component {
    readonly passengersPage: PassengersPage;
    readonly hotelOrderPage: TestHotelOrderPage;
    readonly tripsPage: TestTripsPage;
    readonly tripPage: TestTripPage;

    constructor(browser: WebdriverIO.Browser) {
        super(browser);

        this.passengersPage = new PassengersPage(this.browser);
        this.hotelOrderPage = new TestHotelOrderPage(this.browser);
        this.tripsPage = new TestTripsPage(this.browser);
        this.tripPage = new TestTripPage(this.browser);
    }

    async enableTripsExperiment(): Promise<void> {
        await setAbExperiment(this.browser, 'KOMOD_trip_page', 'enabled');
    }

    async useTripsApiMock(
        activeTripsCount?: number,
        pastTripsCount?: number,
    ): Promise<void> {
        await this.browser.setCookie({
            name: 'use_trips_mock',
            value: 'true',
        });

        if (activeTripsCount !== undefined) {
            await this.browser.setCookie({
                name: 'active_trips_count',
                value: String(activeTripsCount),
            });
        }

        if (pastTripsCount !== undefined) {
            await this.browser.setCookie({
                name: 'past_trips_count',
                value: String(pastTripsCount),
            });
        }
    }

    goTripsPage(): Promise<string> {
        return this.browser.url(trips.url);
    }
}
