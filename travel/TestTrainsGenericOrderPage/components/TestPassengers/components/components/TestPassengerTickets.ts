import TestPassengerTicket from 'helpers/project/trains/pages/TestTrainsGenericOrderPage/components/TestPassengers/components/components/TestPassengerTicket';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestPassengerTickets extends Component {
    tickets: ComponentArray<TestPassengerTicket>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.tickets = new ComponentArray<TestPassengerTicket>(
            browser,
            {parent: this.qa, current: 'ticket'},
            TestPassengerTicket,
        );
    }
}
