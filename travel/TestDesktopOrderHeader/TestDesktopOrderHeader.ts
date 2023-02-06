import {Component} from 'components/Component';
import TestSuccessText from 'components/TestHappyPage/TestSuccessText/TestSuccessText';

export default class TestDesktopOrderHeader extends Component {
    successText: TestSuccessText;
    orderId: Component;
    supportPhone: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.successText = new TestSuccessText(this.browser, {
            parent: this.qa,
            current: 'successText',
        });
        this.orderId = new Component(this.browser, {
            parent: this.qa,
            current: 'orderId',
        });
        this.supportPhone = new Component(this.browser, {
            parent: this.qa,
            current: 'supportPhone',
        });
    }
}
