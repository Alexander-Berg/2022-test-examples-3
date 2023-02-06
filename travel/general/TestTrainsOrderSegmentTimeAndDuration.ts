import {Component} from 'components/Component';

import {TestTrainSegmentTime} from './TestTrainSegmentTime';

export class TestTrainsOrderSegmentTimeAndDuration extends Component {
    departure: TestTrainSegmentTime;
    arrival: TestTrainSegmentTime;
    duration: Component;
    timeMessage: Component;

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
        this.duration = new Component(browser, {
            parent: this.qa,
            current: 'duration',
        });
        this.timeMessage = new Component(browser, {
            parent: this.qa,
            current: 'timeMessage',
        });
    }
}
