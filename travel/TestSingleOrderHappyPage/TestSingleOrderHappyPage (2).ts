import TestCrossSales from 'helpers/project/happyPage/components/TestCrossSales/TestCrossSales';
import TestOrderMainInfo from 'helpers/project/trains/components/TestOrderMainInfo/TestOrderMainInfo';

import {TestHappyPage} from 'components/TestHappyPage';
import {TestOrderHeader} from 'components/TestOrderHeader';
import {TestOrderActions} from 'components/TestOrderActions';

export default class TestSingleOrderHappyPage extends TestHappyPage {
    crossSales: TestCrossSales;
    orderMainInfo: TestOrderMainInfo;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'trainsHappyPage');

        this.orderHeader = new TestOrderHeader(browser, {
            parent: this.qa,
            current: 'orderHeader',
        });
        this.orderMainInfo = new TestOrderMainInfo(browser);
        this.orderActions = new TestOrderActions(browser, {
            parent: this.qa,
            current: 'orderActions',
        });
        this.crossSales = new TestCrossSales(browser, {
            parent: this.qa,
            current: 'crossSales',
        });
    }
}
