import {Component} from 'components/Component';
import {Button} from 'components/Button';

export default class TestAviaFlightPageBookBlock extends Component {
    button: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);
        this.button = new Button(this.browser, {
            parent: this.qa,
            current: 'button',
        });
    }
}
