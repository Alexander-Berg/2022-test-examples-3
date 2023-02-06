import TestCopyPNRButtonMobile from 'helpers/project/account/pages/TripPage/components/TestCopyPNRButtonMobile';

import {Component} from 'components/Component';
import {TestLink} from 'components/TestLink';

export default class TestCopyPNRModal extends Component {
    readonly modalContent: Component;
    readonly title: Component;
    readonly pnr: Component;

    readonly copyPNRButtonMobile: TestCopyPNRButtonMobile;

    private readonly _description: Component;
    private readonly _actionButton: TestLink;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.modalContent = new Component(browser, {
            parent: this.qa,
            current: 'modalContent',
        });
        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
        this.pnr = new Component(browser, {
            parent: this.qa,
            current: 'pnr',
        });
        this._description = new Component(browser, {
            parent: this.qa,
            current: 'description',
        });
        this._actionButton = new TestLink(browser, {
            parent: this.qa,
            current: 'actionButton',
        });
        this.copyPNRButtonMobile = new TestCopyPNRButtonMobile(browser, {
            parent: this.qa,
            current: 'copyPNRButtonMobile',
        });
    }

    get actionButton(): TestLink {
        return this.isTouch
            ? this.copyPNRButtonMobile.actionButton
            : this._actionButton;
    }

    get description(): Component {
        return this.isTouch
            ? this.copyPNRButtonMobile.description
            : this._description;
    }
}
