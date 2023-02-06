import TestFlightFareAvailabilityTerm from 'helpers/project/avia/components/TestTariffsModal/components/TestFlightFareAvailabilityTerm';

import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';
import {Button} from 'components/Button';

export default class TestTariffItem extends Component {
    name: Component;
    price: TestPrice;
    button: Button;

    refundable: TestFlightFareAvailabilityTerm;
    changingCarriage: TestFlightFareAvailabilityTerm;
    carryOn: Component;
    baggage: Component;
    miles: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.name = new Component(browser, {parent: this.qa, current: 'name'});
        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
        this.button = new Button(browser, {parent: this.qa, current: 'button'});

        this.refundable = new TestFlightFareAvailabilityTerm(browser, {
            parent: this.qa,
            current: 'refundable',
        });
        this.changingCarriage = new TestFlightFareAvailabilityTerm(browser, {
            parent: this.qa,
            current: 'changingCarriage',
        });
        this.carryOn = new Component(browser, {
            parent: this.qa,
            current: 'carryOn',
        });
        this.baggage = new Component(browser, {
            parent: this.qa,
            current: 'baggage',
        });
        this.miles = new Component(browser, {
            parent: this.qa,
            current: 'miles',
        });
    }
}
