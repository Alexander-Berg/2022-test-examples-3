import {Component} from 'components/Component';
import {Button} from 'components/Button';
import TestBottomSheet from 'components/TestBottomSheet';

/**
 * Меню с дополнительными действиями над заказом
 */
export default class TestActionsDialog extends Component {
    refundTicketButton: Button;
    cancelTicketCheckin: Button;
    restoreTicketCheckin: Button;

    bottomSheet: TestBottomSheet;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.bottomSheet = new TestBottomSheet(browser);

        this.refundTicketButton = new Button(browser, {
            parent: this.qa,
            current: 'refundTicket',
        });
        this.cancelTicketCheckin = new Button(browser, {
            parent: this.qa,
            current: 'cancelTicketCheckin',
        });
        this.restoreTicketCheckin = new Button(browser, {
            parent: this.qa,
            current: 'restoreTicketCheckin',
        });
    }
}
