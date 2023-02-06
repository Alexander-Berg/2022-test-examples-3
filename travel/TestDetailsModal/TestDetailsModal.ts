import {TestModalWithBackButton} from 'helpers/project/common/components';

import TestDetails from './components/TestDetails/TestDetails';

export default class TestDetailsModal extends TestModalWithBackButton {
    details: TestDetails;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.details = new TestDetails(this.browser, {
            parent: this.qa,
            current: 'details',
        });
    }

    async close(): Promise<void> {
        if (this.isTouch) {
            return this.backButton.click();
        }

        return this.closeButton.click();
    }
}
