import TestOrder from 'helpers/project/account/pages/TripPage/components/TestOrder';
import TestTitleAndDates from 'helpers/project/account/pages/TripPage/components/TestTitleAndDates';
import TestLocationAndActions from 'helpers/project/account/pages/TripPage/components/TestHotelOrder/components/TestLocationAndActions';

import {Component} from 'components/Component';

export default class TestHotelOrder extends TestOrder {
    titleAndDates: TestTitleAndDates;
    locationAndActions: TestLocationAndActions;
    cancelCaption: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.titleAndDates = new TestTitleAndDates(this.browser, {
            parent: this.qa,
            current: 'titleAndDates',
        });
        this.locationAndActions = new TestLocationAndActions(this.browser, {
            parent: this.qa,
            current: 'locationAndActions',
        });
        this.cancelCaption = new Component(this.browser, {
            parent: this.qa,
            current: 'cancelCaption',
        });
    }
}
