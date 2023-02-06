import {every} from 'p-iteration';

import {Component} from 'components/Component';

export default class TestBusesAdvantage extends Component {
    icon: Component;
    title: Component;
    description: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.icon = new Component(browser, {
            parent: this.qa,
            current: 'icon',
        });

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.description = new Component(browser, {
            parent: this.qa,
            current: 'description',
        });
    }

    async isDisplayed(): Promise<boolean> {
        return every([this.icon, this.title, this.description], elem =>
            elem.isDisplayed(),
        );
    }
}
