import {ITestFormContacts} from './types';

import {Input} from 'components/Input';
import {Component} from 'components/Component';

export class TestBookingContactsForm extends Component {
    readonly email: Input;
    readonly phone: Input;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.email = new Input(browser, {
            parent: this.qa,
            current: 'email',
        });

        this.phone = new Input(browser, {
            parent: this.qa,
            current: 'phone',
        });
    }

    async fill(contacts: ITestFormContacts): Promise<void> {
        if (contacts.email) {
            await this.email.setValue(contacts.email);
        }

        if (contacts.phone) {
            await this.phone.setValue(contacts.phone);
        }
    }
}
