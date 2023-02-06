import {Component} from 'components/Component';

export class TestHappyPageContacts extends Component {
    phone: Component;
    email: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.phone = new Component(browser, {
            parent: this.qa,
            current: 'phone',
        });

        this.email = new Component(browser, {
            parent: this.qa,
            current: 'email',
        });
    }
}
