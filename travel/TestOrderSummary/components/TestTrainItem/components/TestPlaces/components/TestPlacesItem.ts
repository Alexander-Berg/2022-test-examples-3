import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export default class TestPlacesItem extends Component {
    title: Component;
    places: Component;
    price: TestPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.places = new Component(browser, {
            parent: this.qa,
            current: 'places',
        });

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
    }
}
