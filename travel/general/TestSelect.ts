import {Component} from './Component';
import {ComponentArray} from './ComponentArray';

export class TestSelect extends Component {
    labelText: Component;
    trigger: Component;
    optionsPopup: Component;
    options: ComponentArray;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.labelText = new Component(browser, {
            parent: this.qa,
            current: 'labelText',
        });

        this.trigger = new Component(browser, {
            parent: this.qa,
            current: 'trigger',
        });

        this.optionsPopup = new Component(browser, {
            parent: this.qa,
            current: 'optionsPopup',
        });

        this.options = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'option',
            },
            Component,
        );
    }

    async getValue(timeout?: number): Promise<string> {
        return this.trigger.getText(timeout);
    }

    async selectByValue(value: string, timeout?: number): Promise<void> {
        await this.trigger.waitForVisible(timeout);
        await this.trigger.click();
        await this.optionsPopup.waitForVisible();

        const option = new Component(this.browser, {
            key: value,
            parent: this.qa,
            current: 'option',
        });

        await option.clickJS();
    }
}
