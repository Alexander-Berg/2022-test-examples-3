import TestPassengerTicketActions from 'helpers/project/trains/pages/TestTrainsGenericOrderPage/components/TestPassengers/components/components/TestPassengerTicketActions';
import TestPassengerTicketPlaces from 'helpers/project/trains/pages/TestTrainsGenericOrderPage/components/TestPassengers/components/components/TestPassengerTicketPlaces';
import TestPassengerTicketTariff from 'helpers/project/trains/pages/TestTrainsGenericOrderPage/components/TestPassengers/components/components/TestPassengerTicketTariff';
import TestPassengerTicketInsurance from 'helpers/project/trains/pages/TestTrainsGenericOrderPage/components/TestPassengers/components/components/TestPassengerTicketInsurance';
import TestPassengerTicketRemoteCheckinStatus from 'helpers/project/trains/pages/TestTrainsGenericOrderPage/components/TestPassengers/components/components/TestPassengerTicketRemoteCheckinStatus';

import {Component} from 'components/Component';

import TestPassengerTicketRefund from './TestPassengerTicketRefund';

export default class TestPassengerTicket extends Component {
    routeTitle: Component;
    refund: TestPassengerTicketRefund;
    actions: TestPassengerTicketActions;
    places: TestPassengerTicketPlaces;
    tariff: TestPassengerTicketTariff;
    insurance: TestPassengerTicketInsurance;
    remoteCheckinStatus: TestPassengerTicketRemoteCheckinStatus;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.routeTitle = new Component(browser, {
            parent: this.qa,
            current: 'routeTitle',
        });

        this.places = new TestPassengerTicketPlaces(browser, {
            parent: this.qa,
            current: 'places',
        });

        this.tariff = new TestPassengerTicketTariff(browser, {
            parent: this.qa,
            current: 'tariff',
        });

        this.insurance = new TestPassengerTicketInsurance(browser, {
            parent: this.qa,
            current: 'insurance',
        });

        this.remoteCheckinStatus = new TestPassengerTicketRemoteCheckinStatus(
            browser,
            {parent: this.qa, current: 'remoteCheckinStatus'},
        );

        this.refund = new TestPassengerTicketRefund(browser, {
            parent: this.qa,
            current: 'refund',
        });

        this.actions = new TestPassengerTicketActions(browser, {
            parent: this.qa,
            current: 'actions',
        });
    }
}
