import {Component} from 'components/Component';

export class TestHotelsCancellationInfo extends Component {
    trigger: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.trigger = new Component(browser, {
            parent: this.qa,
            current: 'trigger',
        });
    }

    getTriggerText(): Promise<string> {
        return this.trigger.getText();
    }
}
