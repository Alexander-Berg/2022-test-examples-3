import TestOffer from 'helpers/project/hotels/pages/HotelPage/components/TestOffer';

import TestHotelOfferLabels from './TestHotelOfferLabels';

export default class TestMainOffer extends TestOffer {
    hotelOfferLabels: TestHotelOfferLabels;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.hotelOfferLabels = new TestHotelOfferLabels(browser, {
            parent: this.qa,
            current: 'hotelOfferLabels',
        });
    }
}
