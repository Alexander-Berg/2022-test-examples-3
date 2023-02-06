import {trips} from 'suites/trips';

import ITripsMockParams from 'helpers/project/account/types/ITripsMockParams';

import PassengersPage from 'helpers/project/account/pages/PassengersPage/PassengersPage';
import TestOrderHotels from 'helpers/project/account/pages/OrderPage/TestOrderHotels';
import TestTripsPage from 'helpers/project/account/pages/TripsPage/TestTripsPage';
import TestTripPage from 'helpers/project/account/pages/TripPage/TestTripPage';

import {Component} from 'components/Component';

export default class TestAccountApp extends Component {
    readonly passengersPage: PassengersPage;
    readonly hotelOrderPage: TestOrderHotels;
    readonly tripsPage: TestTripsPage;
    readonly tripPage: TestTripPage;

    constructor(browser: WebdriverIO.Browser) {
        super(browser);

        this.passengersPage = new PassengersPage(this.browser);
        this.hotelOrderPage = new TestOrderHotels(this.browser);
        this.tripsPage = new TestTripsPage(this.browser);
        this.tripPage = new TestTripPage(this.browser);
    }

    async useTripsApiMock({
        activeTripsCount,
        pastTripsCount,
        filterActivityTypes,
    }: ITripsMockParams = {}): Promise<void> {
        await this.browser.setCookies({
            name: 'use_trips_mock',
            value: 'true',
        });

        if (activeTripsCount !== undefined) {
            await this.browser.setCookies({
                name: 'active_trips_count',
                value: String(activeTripsCount),
            });
        }

        if (pastTripsCount !== undefined) {
            await this.browser.setCookies({
                name: 'past_trips_count',
                value: String(pastTripsCount),
            });
        }

        if (filterActivityTypes !== undefined) {
            await this.browser.setCookies({
                name: 'mock_activity_types',
                value: JSON.stringify(filterActivityTypes),
            });
        }
    }

    goTripsPage(): Promise<string> {
        return this.browser.url(trips.url);
    }
}
