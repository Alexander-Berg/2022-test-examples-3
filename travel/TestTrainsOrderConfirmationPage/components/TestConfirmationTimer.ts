import {Component} from 'components/Component';

export default class TestConfirmationTimer extends Component {
    time: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'confirmationTimer') {
        super(browser, qa);

        this.time = new Component(browser, {parent: this.qa, current: 'time'});
    }

    async getMinutes(): Promise<number> {
        const value = await this.time.getText();

        const [minute] = value.split(':');

        return Number(minute);
    }
}
