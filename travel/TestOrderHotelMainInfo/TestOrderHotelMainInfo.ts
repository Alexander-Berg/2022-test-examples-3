import TestOrderHotelsPrice from 'helpers/project/account/pages/OrderPage/components/TestOrderHotelsPrice/TestOrderHotelsPrice';

import {Component} from 'components/Component';
import TestCheckinCheckoutDates from './components/TestCheckinCheckoutDates';

export default class TestOrderHotelMainInfo extends Component {
    hotelName: Component;
    hotelAddress: Component;
    checkinCheckoutDates: TestCheckinCheckoutDates;
    orderHotelsPrice: TestOrderHotelsPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.hotelName = new Component(this.browser, {
            parent: this.qa,
            current: 'hotelName',
        });
        this.hotelAddress = new Component(this.browser, {
            parent: this.qa,
            current: 'hotelAddress',
        });
        this.checkinCheckoutDates = new TestCheckinCheckoutDates(this.browser, {
            parent: this.qa,
            current: 'checkinCheckoutDates',
        });
        this.orderHotelsPrice = new TestOrderHotelsPrice(this.browser, {
            parent: this.qa,
            current: 'orderHotelsPrice',
        });
    }
}
