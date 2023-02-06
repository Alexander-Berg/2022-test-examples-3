import {TestGenericOrderInfo} from 'helpers/project/trains/pages/TestGenericOrderHappyPage/components/TestGenericOrderInfo/TestGenericOrderInfo';

import {TestHappyPage} from 'components/TestHappyPage/TestHappyPage';

export class TestGenericOrderHappyPage extends TestHappyPage {
    readonly orderInfo: TestGenericOrderInfo;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'happyPage') {
        super(browser, qa);

        this.orderInfo = new TestGenericOrderInfo(browser, {
            parent: this.qa,
            current: 'orderInfo',
        });
    }
}
