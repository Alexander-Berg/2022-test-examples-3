import TestOrder from 'helpers/project/account/pages/TripPage/components/TestOrder';
import TestOrderMainInfo from 'helpers/project/account/pages/TripPage/components/TestOrderMainInfo';

import {Component} from 'components/Component';

export default class TestHotelOrder extends TestOrder {
    orderMainInfo: TestOrderMainInfo;
    supportAction: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.orderMainInfo = new TestOrderMainInfo(this.browser, {
            parent: this.qa,
            current: 'orderMainInfo',
        });

        this.supportAction = new Component(this.browser, {
            parent: this.qa,
            current: 'supportAction',
        });
    }
}
