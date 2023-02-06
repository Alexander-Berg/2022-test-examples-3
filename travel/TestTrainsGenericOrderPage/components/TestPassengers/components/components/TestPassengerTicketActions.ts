import TestActionsDialog from 'helpers/project/trains/pages/TestTrainsGenericOrderPage/components/TestActionsDialog';

import {Component} from 'components/Component';
import {Button} from 'components/Button';

export default class TestPassengerTicketActions extends Component {
    moreButton: Button;
    actionsDialog: TestActionsDialog;
    /**
     * Кнопка возврата в десктопе лежит отдельно от TestActionsDialog
     */
    refundTicketButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.moreButton = new Button(browser, {
            parent: this.qa,
            current: 'moreButton',
        });

        this.refundTicketButton = new Button(browser, {
            parent: this.qa,
            current: 'refundTicket',
        });

        this.actionsDialog = new TestActionsDialog(browser, {
            parent: this.qa,
            current: 'actionsDialog',
        });
    }

    async openMoreActionsDialog(): Promise<void> {
        await this.moreButton.scrollIntoView();
        await this.moreButton.click();
    }

    async refundTicketButtonIsDisplayed(): Promise<boolean> {
        return this.refundTicketButton.isDisplayed();
    }

    async cancelAllCheckinIsDisplayed(): Promise<boolean> {
        if (!(await this.moreButton.isDisplayed())) {
            return false;
        }

        await this.moreButton.scrollIntoView();
        await this.moreButton.click();

        const isDisplayed =
            await this.actionsDialog.cancelTicketCheckin.isDisplayed();

        if (this.isTouch) {
            await this.actionsDialog.bottomSheet.close();
        }

        return isDisplayed;
    }

    async refundTicket(): Promise<void> {
        await this.refundTicketButton.click();
    }
}
