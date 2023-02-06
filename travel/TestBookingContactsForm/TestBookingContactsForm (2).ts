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
        /*
         * Баг в Firefox, очистка предзаполненого поля через backspace не работает
         * Проваливается в бесконечный цикл, т.к. getValue возвращает для пустого поля начальное значение
         *
         * Есть подозрение на то, что ломает автозаполнение, но надо проверять
         */
        const isFirefox = (await this.browser.getBrowserName()) === 'firefox';

        if (contacts.email) {
            await this.email.setValue(contacts.email, {
                withoutBackspace: isFirefox,
            });
        }

        if (contacts.phone) {
            await this.phone.setValue(contacts.phone, {
                withoutBackspace: isFirefox,
            });
        }
    }
}
