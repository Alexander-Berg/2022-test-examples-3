import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';
import TestFieldLabel from 'components/TestFieldLabel';
import TestPartialPrice from 'components/TestPartialPrice/TestPartialPrice';

export default class TestTotalPrice extends Component {
    fieldLabel: TestFieldLabel;
    partialPrice: TestPartialPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.fieldLabel = new TestFieldLabel(this.browser, {
            parent: this.qa,
            current: 'fieldLabel',
        });

        this.partialPrice = new TestPartialPrice(this.browser, {
            parent: this.qa,
            current: 'partialPrice',
        });
    }

    get title(): Component {
        return this.fieldLabel.label;
    }

    get paidPrice(): TestPrice {
        return this.partialPrice.price;
    }

    get totalPrice(): TestPrice {
        return this.partialPrice.totalPrice;
    }
}
