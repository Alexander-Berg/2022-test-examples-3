import {TestPassenger} from 'helpers/project/trains/pages/TestTrainsOrderConfirmationPage/components/TestPassengers/components/TestPassenger/TestPassenger';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export class TestPassengers extends Component {
    passengers: ComponentArray<TestPassenger>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.passengers = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'passenger',
            },
            TestPassenger,
        );
    }
}
