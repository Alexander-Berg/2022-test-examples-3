import TestActionsDialog from 'helpers/project/common/components/TestActionsDialog';

import {TestOrderActions as CommonTestOrderActions} from 'components/TestOrderActions';

export default class TestOrderActions extends CommonTestOrderActions {
    actionsDialog: TestActionsDialog;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.actionsDialog = new TestActionsDialog(browser, {
            parent: this.qa,
            current: 'actionsDialog',
        });
    }
}
