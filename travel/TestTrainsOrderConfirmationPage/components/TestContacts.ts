import {Component} from 'components/Component';
import TestFieldLabel from 'components/TestFieldLabel';

export default class TestContacts extends Component {
    email: TestFieldLabel;
    phone: TestFieldLabel;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.email = new TestFieldLabel(browser, {
            parent: this.qa,
            current: 'email',
        });
        this.phone = new TestFieldLabel(browser, {
            parent: this.qa,
            current: 'phone',
        });
    }

    async getPhoneNumber(): Promise<string> {
        const phone = await this.phone.value.getText();

        return phone.replace(/[\s-]+/g, '');
    }
}
