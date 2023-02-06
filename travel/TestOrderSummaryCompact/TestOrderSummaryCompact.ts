import {TestOrderSummary} from 'helpers/project/trains/components/TestOrderSummary/TestOrderSummary';

import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';
import {Button} from 'components/Button';
import TestBottomSheet from 'components/TestBottomSheet';

/**
 * Мини-корзинка на выборе мест, появляется под выбранным вагоном
 */
export default class TestOrderSummaryCompact extends Component {
    orderSummary: TestOrderSummary;
    price: TestPrice;
    orderButton: Button;
    places: Component;

    private bottomSheet: TestBottomSheet;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.orderSummary = new TestOrderSummary(browser, {
            parent: this.qa,
            current: 'orderSummary',
        });

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });

        this.orderButton = new Button(browser, {
            parent: this.qa,
            current: 'orderButton',
        });

        this.places = new Component(browser, {
            parent: this.qa,
            current: 'places',
        });

        this.bottomSheet = new TestBottomSheet(browser);
    }

    async openOrderSummary(): Promise<void> {
        await this.price.click();
    }

    async closeOrderSummary(): Promise<void> {
        await this.bottomSheet.close();
    }
}
