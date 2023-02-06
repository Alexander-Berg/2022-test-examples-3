import {Component} from 'components/Component';

export class TestBookingOrderPNR extends Component {
    number: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.number = new Component(browser, {
            parent: this.qa,
            current: 'number',
        });
    }
}
