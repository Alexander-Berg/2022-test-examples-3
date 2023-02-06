import TestPastTripItem from 'helpers/project/account/pages/TripsPage/components/TestPastTripItem';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestPastTrips extends Component {
    readonly pastTrips: ComponentArray<TestPastTripItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.pastTrips = new ComponentArray(
            browser,
            {parent: this.qa, current: 'pastTrip'},
            TestPastTripItem,
        );
    }

    getTitles(): Promise<string[]> {
        return this.pastTrips.map(trip => trip.title.getText());
    }
}
