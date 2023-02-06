import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';
import {TestCheckbox} from 'components/TestCheckbox';

export default class TestInsurance extends Component {
    price: TestPrice;
    checkbox: TestCheckbox;
    description: Component;
    logo: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.checkbox = new TestCheckbox(browser, {
            parent: this.qa,
            current: 'checkbox',
        });

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });

        this.description = new Component(browser, {
            parent: this.qa,
            current: 'description',
        });

        this.logo = new Component(browser, {
            parent: this.qa,
            current: 'logo',
        });
    }
}
