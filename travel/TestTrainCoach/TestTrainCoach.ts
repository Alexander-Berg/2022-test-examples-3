import {Component} from 'components/Component';
import TestTrainCoachHeader from './components/TestTrainCoachHeader';
import TestTrainCoachTransportSchema from './components/TestTrainCoachTransportSchema';
import TestTrainCoachAutoSeat from './components/TestTrainCoachAutoSeat';

export default class TestTrainCoach extends Component {
    coachHeader: TestTrainCoachHeader;
    transportSchema: TestTrainCoachTransportSchema;
    autoSeat: TestTrainCoachAutoSeat;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.coachHeader = new TestTrainCoachHeader(browser, {
            parent: this.qa,
            current: 'coachHeader',
        });

        this.transportSchema = new TestTrainCoachTransportSchema(browser, {
            parent: this.qa,
            current: 'transportSchema',
        });

        this.autoSeat = new TestTrainCoachAutoSeat(browser, this.qa);
    }
}
