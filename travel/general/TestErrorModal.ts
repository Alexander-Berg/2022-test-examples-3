import {Component} from 'components/Component';
import {Button} from 'components/Button';

export class TestErrorModal extends Component {
    text: Component;
    retryButton: Button;
    goToMainButton: Button;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'errorModal');

        this.text = new Component(browser, {
            parent: this.qa,
            current: 'text',
        });
        this.retryButton = new Component(browser, {
            parent: this.qa,
            current: 'retryButton',
        });
        this.goToMainButton = new Component(browser, {
            parent: this.qa,
            current: 'goToMainButton',
        });
    }
}
