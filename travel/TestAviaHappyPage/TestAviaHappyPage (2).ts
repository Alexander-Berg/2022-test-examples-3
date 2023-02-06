import {MINUTE} from 'helpers/constants/dates';

import {TestHappyPage} from 'components/TestHappyPage';
import {Component} from 'components/Component';

export class TestAviaHappyPage extends TestHappyPage {
    readonly errorPage: Component;
    readonly errorModal: Component;
    readonly email: Component;
    readonly phone: Component;

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

        this.email = new Component(browser, {
            parent: this.qa,
            current: 'email',
        });

        this.phone = new Component(browser, {
            parent: this.qa,
            current: 'phone',
        });
    }

    async waitForPageLoading(): Promise<void> {
        await this.waitForVisible(MINUTE);
    }

    async isOrderSuccessful(): Promise<boolean> {
        return this.orderHeader.successBadge.isDisplayed();
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
