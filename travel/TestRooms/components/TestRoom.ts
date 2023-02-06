import TestMainOffers from 'helpers/project/hotels/pages/HotelPage/components/TestOffersInfo/components/TestMainOffers/TestMainOffers';
import TestOffer from 'helpers/project/hotels/pages/HotelPage/components/TestOffer';
import {TestHotelOfferLabels} from 'helpers/project/hotels/pages/HotelPage/components/TestHotelOfferLabels';
import TestAmenities from 'helpers/project/hotels/pages/HotelPage/components/TestOffersInfo/components/TestRooms/components/TestAmenities';

import {Component} from 'components/Component';

export default class TestRoom extends TestOffer {
    mainOffers: TestMainOffers;
    emptyOffer: Component;
    offer: TestOffer;
    offerLabels: TestHotelOfferLabels;
    name: Component;
    gallery: Component;
    amenities: TestAmenities;
    detailedInfoButton: Component;
    bedGroupsAndSize: Component;
    topName: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.mainOffers = new TestMainOffers(browser, {
            parent: this.qa,
            current: 'mainOffers',
        });
        this.emptyOffer = new Component(browser, {
            parent: this.qa,
            current: 'emptyOffer',
        });
        this.offer = new TestOffer(browser, {
            parent: this.qa,
            current: 'offer',
        });
        this.offerLabels = new TestHotelOfferLabels(browser, {
            parent: this.qa,
            current: 'offerLabels',
        });
        this.name = new Component(browser, {parent: this.qa, current: 'name'});
        this.gallery = new Component(browser, {
            parent: this.qa,
            current: 'gallery',
        });
        this.amenities = new TestAmenities(browser, {
            parent: this.qa,
            current: 'amenities',
        });
        this.detailedInfoButton = new Component(browser, {
            parent: this.qa,
            current: 'detailedInfoButton',
        });
        this.bedGroupsAndSize = new Component(browser, {
            parent: this.qa,
            current: 'bedGroupsAndSize',
        });
        this.topName = new Component(browser, {
            parent: this.qa,
            current: 'topName',
        });
    }
}
