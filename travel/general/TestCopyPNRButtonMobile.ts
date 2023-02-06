import {Component} from 'components/Component';
import {Button} from 'components/Button';
import {TestLink} from 'components/TestLink';

export default class TestCopyPNRButtonMobile extends Component {
    readonly copyButton: Button;
    readonly actionButton: TestLink;
    readonly description: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.copyButton = new Button(browser, {
            parent: this.qa,
            current: 'copyButton',
        });

        this.description = new Component(browser, {
            parent: this.qa,
            current: 'description',
        });

        this.actionButton = new TestLink(browser, {
            parent: this.qa,
            current: 'actionButton',
        });
    }
}
