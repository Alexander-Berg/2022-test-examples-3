import TestHotelOperator from 'helpers/project/hotels/components/TestHotelOperator';
import {TestHotelOfferLabels} from 'helpers/project/hotels/pages/HotelPage/components/TestHotelOfferLabels';
import TestOffer from 'helpers/project/hotels/pages/HotelPage/components/TestOffer';

export default class TestPartnerOffer extends TestOffer {
    hotelOperator: TestHotelOperator;
    offerLabels: TestHotelOfferLabels;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.hotelOperator = new TestHotelOperator(browser, {
            parent: this.qa,
            current: 'hotelOperator',
        });
        this.offerLabels = new TestHotelOfferLabels(browser, {
            parent: this.qa,
            current: 'offerLabels',
        });
    }
}
