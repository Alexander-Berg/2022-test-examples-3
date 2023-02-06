import {Component} from 'components/Component';

export class TestTrainSegmentTime extends Component {
    date: Component;
    time: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.date = new Component(browser, {parent: this.qa, current: 'date'});
        this.time = new Component(browser, {parent: this.qa, current: 'time'});
    }

    async getDate() {
        return (await this.date.getText()).split(',')[0];
    }
}
