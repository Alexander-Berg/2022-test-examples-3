import {TestModal} from './TestModal';
import {Button} from './Button';

export class TestModalWithBackButton extends TestModal {
    backButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'modal') {
        super(browser, qa);

        this.backButton = new Button(browser, {
            parent: this.qa,
            current: 'backButton',
        });
    }
}
