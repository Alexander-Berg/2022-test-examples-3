import TestPartnerOffers from 'helpers/project/hotels/pages/HotelPage/components/TestOffersInfo/components/TestPartnerOffers/TestPartnerOffers';
import TestMainOffers from 'helpers/project/hotels/pages/HotelPage/components/TestOffersInfo/components/TestMainOffers/TestMainOffers';
import {HotelsSearchForm} from 'helpers/project/hotels/components/HotelsSearchForm';
import TestRooms from 'helpers/project/hotels/pages/HotelPage/components/TestOffersInfo/components/TestRooms/TestRooms';

import {Component} from 'components/Component';
import {Button} from 'components/Button';

export default class TestOffersInfo extends Component {
    allRoomsLink: Button;
    partnerOffers: TestPartnerOffers;
    mainOffers: TestMainOffers;
    mainOffersTitle: Component;
    partnerOffersTitle: Component;
    filteredOffersEmpty: Component;
    emptyOffers: Component;
    hotelPageSearchForm: HotelsSearchForm;
    rooms: TestRooms;
    offersTitle: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.allRoomsLink = new Button(browser, {
            parent: this.qa,
            current: 'allRoomsLink',
        });
        this.partnerOffers = new TestPartnerOffers(browser, {
            parent: this.qa,
            current: 'partnerOffers',
        });
        this.mainOffers = new TestMainOffers(browser, {
            parent: this.qa,
            current: 'mainOffers',
        });
        this.mainOffersTitle = new Component(browser, {
            parent: this.qa,
            current: 'mainOffersTitle',
        });
        this.partnerOffersTitle = new Component(browser, {
            parent: this.qa,
            current: 'partnerOffersTitle',
        });
        this.filteredOffersEmpty = new Component(browser, {
            parent: this.qa,
            current: 'filteredOffersEmpty',
        });
        this.emptyOffers = new Component(browser, {
            parent: this.qa,
            current: 'emptyOffers',
        });
        this.hotelPageSearchForm = new HotelsSearchForm(browser, {
            parent: this.qa,
            current: 'hotelPageSearchForm',
        });
        this.rooms = new TestRooms(browser, {
            parent: this.qa,
            current: 'rooms',
        });
        this.offersTitle = new Component(browser, {
            current: 'offersTitle',
        });
    }
}
