import TestBusesAdvantage from 'helpers/project/buses/pages/TestBusesIndexPage/components/TestBusesAdvantages/components/TestBusesAdvantage/TestBusesAdvantage';

import {ComponentArray} from 'components/ComponentArray';

export default class TestBusesAdvantages extends ComponentArray<TestBusesAdvantage> {
    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa, TestBusesAdvantage);
    }

    // В таче единовременно отображается только одно преимущество
    async isDisplayed(): Promise<boolean> {
        return this.isTouch
            ? this.some(advantage => advantage.isDisplayed())
            : this.every(advantage => advantage.isDisplayed());
    }
}
