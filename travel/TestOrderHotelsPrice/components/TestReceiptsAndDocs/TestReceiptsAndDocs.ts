import TestDetailsModal from 'helpers/project/account/pages/OrderPage/components/TestOrderHotelsPrice/components/TestDetailsModal/TestDetailsModal';

import {Component} from 'components/Component';
import {Button} from 'components/Button';
import TestActionsDialog from 'components/TestActionsDialog';

export default class TestReceiptsAndDocs extends Component {
    button: Button;
    actionsDialog: TestActionsDialog;
    detailsModal: TestDetailsModal;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.button = new Button(this.browser, {
            parent: this.qa,
            current: 'button',
        });

        this.actionsDialog = new TestActionsDialog(this.browser, {
            parent: this.qa,
            current: 'actionsDialog',
        });

        this.detailsModal = new TestDetailsModal(this.browser, {
            parent: this.qa,
            current: 'detailsModal',
        });
    }

    async openDetails(): Promise<void> {
        await this.button.scrollIntoView();
        await this.button.click();

        const orderDetailsItem = await this.actionsDialog.findItem(
            'orderDetails',
        );

        await orderDetailsItem.click();
    }
}
