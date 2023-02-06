import TestPriceExplanation from 'helpers/project/trains/components/TestPriceExplanation/TestPriceExplanation';

import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export class TestTicket extends Component {
    direction: Component;
    places: Component;
    tariff: Component;
    price: TestPrice;
    priceExplanation: TestPriceExplanation;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.direction = new Component(browser, {
            parent: this.qa,
            current: 'direction',
        });

        this.places = new Component(browser, {
            parent: this.qa,
            current: 'places',
        });

        this.tariff = new Component(browser, {
            parent: this.qa,
            current: 'tariff',
        });

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });

        this.priceExplanation = new TestPriceExplanation(browser, {
            parent: this.qa,
            current: 'priceExplanation',
        });
    }
}
