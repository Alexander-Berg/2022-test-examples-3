import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';
import {TestLink} from 'components/TestLink';

export default class TestPassengerTicketRefund extends Component {
    price: TestPrice;
    downloadTicket: TestLink;
    downloadReceipt: TestLink;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });

        this.downloadTicket = new TestLink(browser, {
            parent: this.qa,
            current: 'downloadTicket',
        });

        this.downloadReceipt = new TestLink(browser, {
            parent: this.qa,
            current: 'downloadReceipt',
        });
    }
}
