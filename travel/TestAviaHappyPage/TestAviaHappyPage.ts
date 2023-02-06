import {MINUTE} from 'helpers/constants/dates';

import {TestHappyPage} from 'components/TestHappyPage/TestHappyPage';
import {Component} from 'components/Component';

export class TestAviaHappyPage extends TestHappyPage {
    readonly errorPage: Component;
    readonly errorModal: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'aviaHappyPage') {
        super(browser, qa);

        this.errorPage = new Component(browser, {
            parent: this.qa,
            current: 'errorPage',
        });

        this.errorModal = new Component(browser, {
            parent: this.qa,
            current: 'errorModal',
        });
    }

    async waitForPageLoading(): Promise<void> {
        await this.waitForVisible(MINUTE);
    }

    async isOrderSuccessful(): Promise<boolean> {
        return this.successText.isVisible();
    }

    async isOrderHasError(): Promise<boolean> {
        return Promise.race([
            this.errorModal.isDisplayed(MINUTE),
            this.errorPage.isDisplayed(MINUTE),
        ]);
    }

    async forwardToDetailedPage(): Promise<void> {
        await this.orderActions.detailsLink.click();
    }
}
