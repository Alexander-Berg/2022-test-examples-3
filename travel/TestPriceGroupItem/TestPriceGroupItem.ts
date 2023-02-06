import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export default class TestPriceGroupItem extends Component {
    readonly price: TestPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
    }
}
