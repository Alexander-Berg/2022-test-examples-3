import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export class TestTariffListItem extends Component {
    title: Component;
    seats: Component;
    price: TestPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
        this.seats = new Component(browser, {
            parent: this.qa,
            current: 'seats',
        });
        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
    }
}
