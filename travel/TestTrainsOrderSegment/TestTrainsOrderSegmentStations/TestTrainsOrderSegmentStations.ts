import {TestStation} from 'helpers/project/trains/components/TestTrainsOrderSegment/TestTrainsOrderSegmentStations/components/TestStation';

import {Component} from 'components/Component';

export class TestTrainsOrderSegmentStations extends Component {
    departure: TestStation;
    arrival: TestStation;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.departure = new TestStation(browser, {
            parent: this.qa,
            current: 'departure',
        });
        this.arrival = new TestStation(browser, {
            parent: this.qa,
            current: 'arrival',
        });
    }
}
