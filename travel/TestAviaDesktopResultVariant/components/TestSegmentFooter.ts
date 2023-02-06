import {Component} from 'components/Component';
import {Button} from 'components/Button';
import {TestPrice} from 'components/TestPrice';

export default class TestSegmentFooter extends Component {
    buyButton: Component;
    price: TestPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.buyButton = new Button(browser, {
            parent: this.qa,
            current: 'buyButton',
        });

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
    }
}
