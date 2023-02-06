import TestCopyPNRModal from 'helpers/project/account/pages/TripPage/components/TestCopyPNRModal';

import {Component} from 'components/Component';
import {Button} from 'components/Button';

export default class TestPNR extends Component {
    description: Component;
    pnr: Component;
    copyButton: Button;
    copyModal: TestCopyPNRModal;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.description = new Component(this.browser, {
            parent: this.qa,
            current: 'description',
        });
        this.pnr = new Component(this.browser, {
            parent: this.qa,
            current: 'pnr',
        });
        this.copyButton = new Button(this.browser, {
            parent: this.qa,
            current: 'copyButton',
        });
        this.copyModal = new TestCopyPNRModal(this.browser, {
            parent: this.qa,
            current: 'copyModal',
        });
    }
}
