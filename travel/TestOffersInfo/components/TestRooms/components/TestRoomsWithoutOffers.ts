import {Component} from 'components/Component';

export default class TestRoomsWithoutOffers extends Component {
    trigger: Component;
    content: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.trigger = new Component(browser, {
            parent: this.qa,
            current: 'trigger',
        });
        this.content = new Component(browser, {
            parent: this.qa,
            current: 'content',
        });
    }

    async open(): Promise<void> {
        if (await this.content.isVisible()) {
            return;
        }

        await this.trigger.click();
    }
}
