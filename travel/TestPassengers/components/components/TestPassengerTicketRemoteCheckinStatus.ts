import {Component} from 'components/Component';

export default class TestPassengerTicketRemoteCheckinStatus extends Component {
    checkinCancelled: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.checkinCancelled = new Component(browser, {
            parent: this.qa,
            current: 'checkinCancelled',
        });
    }
}
