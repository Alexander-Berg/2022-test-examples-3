import TestDetailItem from 'helpers/project/account/pages/OrderPage/components/TestOrderHotelsPrice/components/TestDetailsModal/components/TestDetails/components/TestDetailItem';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestDetails extends Component {
    totalNights: TestDetailItem;
    nights: ComponentArray;
    discount: TestDetailItem;
    taxesAndFeesSum: TestDetailItem;
    promoCodes: ComponentArray<TestDetailItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.totalNights = new TestDetailItem(this.browser, {
            parent: this.qa,
            current: 'totalNights',
        });

        this.nights = new ComponentArray(
            this.browser,
            {
                parent: this.qa,
                current: 'night',
            },
            TestDetailItem,
        );

        this.discount = new TestDetailItem(this.browser, {
            parent: this.qa,
            current: 'discount',
        });

        this.taxesAndFeesSum = new TestDetailItem(this.browser, {
            parent: this.qa,
            current: 'taxesAndFeesSum',
        });

        this.promoCodes = new ComponentArray(
            this.browser,
            {
                parent: this.qa,
                current: 'promoCode',
            },
            TestDetailItem,
        );
    }
}
