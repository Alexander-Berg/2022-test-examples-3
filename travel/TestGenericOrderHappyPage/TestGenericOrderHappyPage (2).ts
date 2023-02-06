import {TestGenericOrderInfo} from 'helpers/project/trains/pages/TestGenericOrderHappyPage/components/TestGenericOrderInfo/TestGenericOrderInfo';
import TestCrossSales from 'helpers/project/happyPage/components/TestCrossSales/TestCrossSales';

import {Component} from 'components/Component';
import {TestOrderActions} from 'components/TestOrderActions';
import {Loader} from 'components/Loader';

export class TestGenericOrderHappyPage extends Component {
    readonly loader: Loader;
    readonly orderInfo: TestGenericOrderInfo;
    readonly orderActions: TestOrderActions;
    readonly crossSales: TestCrossSales;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'happyPage') {
        super(browser, qa);

        this.loader = new Loader(browser);

        this.orderInfo = new TestGenericOrderInfo(browser, {
            parent: this.qa,
            current: 'orderInfo',
        });
        this.orderActions = new TestOrderActions(browser, {
            parent: this.qa,
            current: 'orderActions',
        });
        this.crossSales = new TestCrossSales(browser, {
            parent: this.qa,
            current: 'crossSales',
        });
    }

    async waitOrderLoaded(): Promise<void> {
        await this.loader.waitUntilLoaded();
    }
}
