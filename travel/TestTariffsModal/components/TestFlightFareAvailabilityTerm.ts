import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export default class TestFlightFareAvailabilityTerm extends Component {
    price: TestPrice;
    chargeIcon: Component;
    availableIcon: Component;
    notAvailableIcon: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });

        this.chargeIcon = new Component(browser, {
            parent: this.qa,
            current: 'chargeIcon',
        });

        this.availableIcon = new Component(browser, {
            parent: this.qa,
            current: 'availableIcon',
        });

        this.notAvailableIcon = new Component(browser, {
            parent: this.qa,
            current: 'notAvailableIcon',
        });
    }
}
