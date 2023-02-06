import {Component} from 'components/Component';

export class TestBookingOrderUserContacts extends Component {
    email: Component;
    phone: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.email = new Component(browser, {
            parent: this.qa,
            current: 'email',
        });

        this.phone = new Component(browser, {
            parent: this.qa,
            current: 'phone',
        });
    }
}
