import {SECOND} from 'helpers/constants/dates';

import {Button} from 'components/Button';
import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export default class TestGoToConfirmStep extends Component {
    price: TestPrice;
    button: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });

        this.button = new Component(browser, {
            parent: this.qa,
            current: 'button',
        });
    }

    async clickButtonAndAwaitAnimation(): Promise<void> {
        if (this.isTouch) {
            await this.button.click();

            await this.browser.pause(SECOND);
        }
    }
}
