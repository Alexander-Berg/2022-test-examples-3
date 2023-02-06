import {SECOND} from 'helpers/constants/dates';

import TestOrderSearchFormContent from './components/TestOrderSearchFormContent';
import {Component} from 'components/Component';

export default class TestOrderSearchForm extends Component {
    readonly content: TestOrderSearchFormContent;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.content = new TestOrderSearchFormContent(this.browser, {
            parent: this.qa,
            current: 'content',
        });
    }

    async fillForm(prettyOrderId: string, phoneOrEmail: string): Promise<void> {
        await this.content.prettyOrderIdInput.type(prettyOrderId);
        await this.content.userPhoneOrEmailInput.type(phoneOrEmail);
    }

    async submit(timeout = 30 * SECOND): Promise<void> {
        await this.content.searchButton.click();

        await this.content.loader.waitUntilLoaded(timeout);
    }
}
