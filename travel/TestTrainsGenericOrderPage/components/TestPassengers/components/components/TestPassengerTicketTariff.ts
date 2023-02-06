import TestPriceExplanation from 'helpers/project/trains/components/TestPriceExplanation/TestPriceExplanation';

import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export default class TestPassengerTicketTariff extends Component {
    tariffBaby: Component;
    tariffName: Component;
    price: TestPrice;
    priceExplanation: TestPriceExplanation;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.tariffBaby = new Component(browser, {
            parent: this.qa,
            current: 'tariffBaby',
        });

        this.tariffName = new Component(browser, {
            parent: this.qa,
            current: 'tariffName',
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
