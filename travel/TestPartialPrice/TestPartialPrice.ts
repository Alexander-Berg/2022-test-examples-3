import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export default class TestPartialPrice extends Component {
    price: TestPrice;
    totalPrice: TestPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.price = new TestPrice(this.browser, {
            parent: this.qa,
            current: 'price',
        });
        this.totalPrice = new TestPrice(this.browser, {
            parent: this.qa,
            current: 'totalPrice',
        });
    }
}
