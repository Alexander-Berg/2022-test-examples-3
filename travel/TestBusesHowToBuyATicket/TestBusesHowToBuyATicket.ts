import TestBusesHowToBuyATicketItem from 'helpers/project/buses/pages/TestBusesIndexPage/components/TestBusesHowToBuyATicket/components/TestBusesHowToBuyATicketItem/TestBusesHowToBuyATicketItem';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestBusesHowToBuyATicket extends Component {
    title: Component;
    steps: ComponentArray<TestBusesHowToBuyATicketItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
        this.steps = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'step',
            },
            TestBusesHowToBuyATicketItem,
        );
    }

    async isDisplayed(): Promise<boolean> {
        return (
            (await this.title.isDisplayed()) &&
            (await this.steps.items).length === 4 &&
            (await this.steps.every(step => step.isDisplayed()))
        );
    }
}
