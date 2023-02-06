import {every} from 'p-iteration';

import {Component} from 'components/Component';

export default class TestBusesHowToBuyATicketItem extends Component {
    title: Component;
    description: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

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
        return every([this.title, this.description], elem =>
            elem.isDisplayed(),
        );
    }
}
