import TestTotalPrice from 'helpers/project/account/pages/OrderPage/components/TestOrderHotelsPrice/components/TestTotalPrice';

import {Component} from 'components/Component';
import TestReceiptsAndDocs from './components/TestReceiptsAndDocs/TestReceiptsAndDocs';

export default class TestOrderHotelsPrice extends Component {
    totalPrice: TestTotalPrice;
    receiptsAndDocs: TestReceiptsAndDocs;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.totalPrice = new TestTotalPrice(this.browser, {
            parent: this.qa,
            current: 'totalPrice',
        });

        this.receiptsAndDocs = new TestReceiptsAndDocs(this.browser, {
            parent: this.qa,
            current: 'receiptsAndDocs',
        });
    }
}
