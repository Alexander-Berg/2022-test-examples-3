import {Component} from 'components/Component';
import {Button} from 'components/Button';

export default class TestAviaFlightPageBookBlock extends Component {
    button: Button;
    text: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.button = new Button(this.browser, {
            parent: this.qa,
            current: 'button',
        });

        this.text = new Component(this.browser, {
            parent: this.qa,
            current: 'text',
        });
    }

    async getDate(): Promise<string> {
        const text = await this.text.getText();
        const [date] = text.split(',');

        return date;
    }
}
