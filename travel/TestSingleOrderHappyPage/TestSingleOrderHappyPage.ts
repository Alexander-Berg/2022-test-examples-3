import TestOrderMainInfo from 'helpers/project/trains/components/TestOrderMainInfo/TestOrderMainInfo';

import {TestHappyPage} from 'components/TestHappyPage/TestHappyPage';

export default class TestSingleOrderHappyPage extends TestHappyPage {
    orderMainInfo: TestOrderMainInfo;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'trainsHappyPage');

        this.orderMainInfo = new TestOrderMainInfo(browser);
    }
}
