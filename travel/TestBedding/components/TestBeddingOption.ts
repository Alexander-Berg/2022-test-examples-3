import {Component} from 'components/Component';
import {TestCheckbox} from 'components/TestCheckbox';
import {TestPrice} from 'components/TestPrice';

export class TestBeddingOption extends Component {
    readOnly: Component;
    checkbox: TestCheckbox;
    price: TestPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.readOnly = new Component(browser, {
            parent: this.qa,
            current: 'readOnly',
        });

        this.checkbox = new TestCheckbox(browser, {
            parent: this.qa,
            current: 'checkbox',
        });

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
    }
}
