import {Component} from 'components/Component';

import {TestTrainSegmentTime} from '../TestTrainSegmentTime';

export class TestTrainsOrderSegmentTimeAndStations extends Component {
    departure: TestTrainSegmentTime;
    arrival: TestTrainSegmentTime;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.departure = new TestTrainSegmentTime(browser, {
            parent: this.qa,
            current: 'departure',
        });
        this.arrival = new TestTrainSegmentTime(browser, {
            parent: this.qa,
            current: 'arrival',
        });
    }
}
