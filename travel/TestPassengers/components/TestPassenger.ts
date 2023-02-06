import TestPassengerInfo from 'helpers/project/trains/pages/TestTrainsGenericOrderPage/components/TestPassengers/components/components/TestPassengerInfo';
import TestPassengerTicketActions from 'helpers/project/trains/pages/TestTrainsGenericOrderPage/components/TestPassengers/components/components/TestPassengerTicketActions';
import TestPassengerTickets from 'helpers/project/trains/pages/TestTrainsGenericOrderPage/components/TestPassengers/components/components/TestPassengerTickets';

import {Component} from 'components/Component';

export default class TestPassenger extends Component {
    info: TestPassengerInfo;
    babyInfo: TestPassengerInfo;
    tickets: TestPassengerTickets;

    /**
     * Появляется только для простых заказов
     */
    actions: TestPassengerTicketActions;

    /**
     * Появляется только для простых заказов
     */
    refundTicketStatus: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.info = new TestPassengerInfo(browser, {
            parent: this.qa,
            current: 'info',
        });

        this.babyInfo = new TestPassengerInfo(browser, {
            parent: this.qa,
            current: 'babyInfo',
        });

        this.refundTicketStatus = new Component(browser, {
            parent: this.qa,
            current: 'refundTicketStatus',
        });

        this.tickets = new TestPassengerTickets(browser, {
            parent: this.qa,
            current: 'tickets',
        });

        this.actions = new TestPassengerTicketActions(browser, {
            parent: this.qa,
            current: 'actions',
        });
    }

    async refundTicket(
        ticketIndex: number = 0,
        withMultipleTickets: boolean = false,
    ): Promise<void> {
        if (this.isTouch || withMultipleTickets) {
            const ticket = await this.tickets.tickets.at(ticketIndex);

            await ticket.actions.refundTicket();
        } else {
            await this.actions.refundTicket();
        }
    }

    async isRefundButtonDisplayed(
        ticketIndex: number = 0,
        withMultipleTickets: boolean = false,
    ): Promise<boolean> {
        if (this.isTouch || withMultipleTickets) {
            const ticket = await this.tickets.tickets.at(ticketIndex);

            return ticket.actions.refundTicketButtonIsDisplayed();
        }

        return this.actions.refundTicketButtonIsDisplayed();
    }
}
