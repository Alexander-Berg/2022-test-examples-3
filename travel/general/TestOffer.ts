import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';
import {Button} from 'components/Button';

export default class TestOffer extends Component {
    offerName: Component;
    labels: Component;
    price: TestPrice;
    strikethroughPrice: TestPrice;
    nightsCount: Component;
    bookButton: Button;
    plusInfo: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.offerName = new Component(browser, {
            parent: this.qa,
            current: 'offerName',
        });
        this.labels = new Component(browser, {
            parent: this.qa,
            current: 'labels',
        });
        this.nightsCount = new Component(browser, {
            parent: this.qa,
            current: 'nightsCount',
        });
        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
        this.strikethroughPrice = new TestPrice(browser, {
            parent: this.qa,
            current: 'strikethroughPrice',
        });
        this.bookButton = new Button(browser, {
            parent: this.qa,
            current: 'bookButton',
        });
        this.plusInfo = new Button(browser, {
            parent: this.qa,
            current: 'plusInfo',
        });
    }
}
