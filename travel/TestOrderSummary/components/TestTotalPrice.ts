import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export default class TestTotalPrice extends Component {
    price: TestPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
    }
}
