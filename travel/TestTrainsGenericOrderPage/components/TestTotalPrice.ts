import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';
import {TestLink} from 'components/TestLink';

export default class TestTotalPrice extends Component {
    total: TestPrice;
    refund: TestPrice;
    receipt: TestLink;
    refundReceipt: TestLink;
    insuranceDetails: TestLink;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.total = new TestPrice(browser, {
            parent: this.qa,
            current: 'total',
        });
        this.refund = new TestPrice(browser, {
            parent: this.qa,
            current: 'refund',
        });
        this.receipt = new TestLink(browser, {
            parent: this.qa,
            current: 'receipt',
        });
        this.refundReceipt = new TestLink(browser, {
            parent: this.qa,
            current: 'refundReceipt',
        });
        this.insuranceDetails = new TestLink(browser, {
            parent: this.qa,
            current: 'insuranceDetails',
        });
    }
}
