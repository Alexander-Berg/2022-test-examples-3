import {Component} from 'components/Component';
import {Button} from 'components/Button';

export class TestUser extends Component {
    readonly loginButton: Button;

    constructor(browser: WebdriverIO.Browser, qa?: QA) {
        super(browser, qa);

        this.loginButton = new Button(browser, {
            parent: this.qa,
            current: 'loginButton',
        });
    }
}
