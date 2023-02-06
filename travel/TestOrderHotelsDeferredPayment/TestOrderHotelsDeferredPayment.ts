import TestPartialPrice from 'helpers/project/common/components/TestPartialPrice/TestPartialPrice';

import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export default class TestOrderHotelsDeferredPayment extends Component {
    nextPaymentPrice: TestPrice;
    nextPaymentLink: Component;
    paymentEndsAt: Component;
    partialPrice: TestPartialPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.partialPrice = new TestPartialPrice(browser, {
            parent: this.qa,
            current: 'partialPrice',
        });
        this.nextPaymentPrice = new TestPrice(browser, {
            parent: this.qa,
            current: 'nextPaymentPrice',
        });
        this.nextPaymentLink = new Component(browser, {
            parent: this.qa,
            current: 'nextPaymentLink',
        });
        this.paymentEndsAt = new Component(browser, {
            parent: this.qa,
            current: 'paymentEndsAt',
        });
    }
}
