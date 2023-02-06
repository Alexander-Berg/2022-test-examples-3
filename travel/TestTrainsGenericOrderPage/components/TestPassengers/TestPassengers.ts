import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';
import TestPassenger from './components/TestPassenger';

import TestTotalPrice from '../TestTotalPrice';

export default class TestPassengers extends Component {
    totalPrice: TestTotalPrice;
    passengers: ComponentArray<TestPassenger>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.passengers = new ComponentArray<TestPassenger>(
            browser,
            {
                parent: this.qa,
                current: 'passenger',
            },
            TestPassenger,
        );

        this.totalPrice = new TestTotalPrice(browser, {
            parent: this.qa,
            current: 'totalPrice',
        });
    }
}
