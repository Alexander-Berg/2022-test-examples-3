import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export default class TestDetailItem extends Component {
    title: Component;
    price: TestPrice;
    additional: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });

        this.price = new TestPrice(this.browser, {
            parent: this.qa,
            current: 'price',
        });

        this.additional = new Component(this.browser, {
            parent: this.qa,
            current: 'additional',
        });
    }
}
