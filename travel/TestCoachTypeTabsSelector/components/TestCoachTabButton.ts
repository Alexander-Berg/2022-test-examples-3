import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export class TestCoachTabButton extends Component {
    title: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'title',
            },
            Component,
        );
    }

    async setActive(): Promise<void> {
        await this.scrollIntoView();
        await this.click();
    }
}
