import {TestHotelsCancellationInfo} from 'helpers/project/hotels/components/TestHotelsCancellationInfo/TestHotelsCancellationInfo';

import {Component} from 'components/Component';

export default class TestHotelOfferLabels extends Component {
    offerMealInfo: Component;
    hotelsCancellationInfo: TestHotelsCancellationInfo;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.offerMealInfo = new Component(browser, {
            parent: this.qa,
            current: 'offerMealInfo',
        });
        this.hotelsCancellationInfo = new TestHotelsCancellationInfo(browser, {
            parent: this.qa,
            current: 'hotelsCancellationInfo',
        });
    }
}
