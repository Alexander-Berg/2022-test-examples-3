import {TestHotelsBookSearchParams} from 'helpers/project/hotels/pages/TestHotelsBookPage/components/TestHotelsBookSearchParams/TestHotelsBookSearchParams';

import {Component} from 'components/Component';

export class TestHotelsBookHotelInfo extends Component {
    hotelNameLink: Component;
    addressAndRating: Component;
    bookSearchParams: TestHotelsBookSearchParams;
    offerName: Component;
    mealInfo: Component;
    bedsGroups: Component;
    hotelImage: Component;
    partnerHotelName: Component;
    partnerDescriptionLink: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.hotelNameLink = new Component(browser, {
            path: [this.qa],
            current: 'hotelNameLink',
        });
        this.addressAndRating = new Component(browser, {
            path: [this.qa],
            current: 'addressAndRating',
        });
        this.bookSearchParams = new TestHotelsBookSearchParams(browser, {
            path: [this.qa],
            current: 'bookSearchParams',
        });
        this.offerName = new Component(browser, {
            path: [this.qa],
            current: 'offer-name',
        });
        this.mealInfo = new Component(browser, {
            path: [this.qa],
            current: 'mealInfo',
        });
        this.bedsGroups = new Component(browser, {
            path: [this.qa],
            current: 'bedsGroups',
        });
        this.hotelImage = new Component(browser, {
            path: [this.qa],
            current: 'hotelImage',
        });
        this.partnerHotelName = new Component(browser, {
            path: [this.qa],
            current: 'partnerHotelName',
        });
        this.partnerDescriptionLink = new Component(browser, {
            path: [this.qa],
            current: 'partnerDescriptionLink',
        });
    }
}
