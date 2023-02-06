import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

import TestTrainCoach from '../TestTrainCoach/TestTrainCoach';

export class TestTrainCoaches extends Component {
    coaches: ComponentArray<TestTrainCoach>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.coaches = new ComponentArray(
            browser,
            {parent: this.qa, current: 'coach'},
            TestTrainCoach,
        );
    }
}
