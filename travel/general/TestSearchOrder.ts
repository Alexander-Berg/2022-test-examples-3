import {tripsSearchOrder} from 'suites/trips';

import TestOrderSearchForm from 'helpers/project/account/pages/OrdersSearch/components/TestOrdersSearchForm/TestOrderSearchForm';

import {TestModal} from 'components/TestModal';

export default class TestSearchOrder extends TestModal {
    readonly orderSearchForm: TestOrderSearchForm;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'searchOrderModal');

        this.orderSearchForm = new TestOrderSearchForm(browser, {
            parent: this.qa,
            current: 'orderSearchForm',
        });
    }

    async searchOrder(
        prettyOrderId: string,
        phoneOrEmail: string,
        timeout?: number,
    ): Promise<void> {
        await this.goToSearchOrder();

        await this.orderSearchForm.fillForm(prettyOrderId, phoneOrEmail);

        await this.orderSearchForm.submit(timeout);
    }

    private async goToSearchOrder(): Promise<void> {
        await this.browser.url(tripsSearchOrder.url);

        await this.isOpened();
    }
}
