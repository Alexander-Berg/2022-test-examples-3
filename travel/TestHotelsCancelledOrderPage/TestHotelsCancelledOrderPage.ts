import {Page} from 'components/Page';
import {Loader} from 'components/Loader';
import {Component} from 'components/Component';
import {TestHotelsBookHotelInfo} from '../../components/TestHotelsBookHotelInfo';
import {TestBookPartnerHotelInfo} from '../../components/TestBookPartnerHotelInfo';

/** @see CancelledOrderPage */
export class TestHotelsCancelledOrderPage extends Page {
    loader: Loader;
    errorSectionTitle: Component;
    errorOrderId: Component;
    bookHotelInfo: TestHotelsBookHotelInfo;
    bookPartnerHotelInfo: TestBookPartnerHotelInfo;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'hotelsCancelledOrderPage');

        this.loader = new Loader(browser, {
            path: [this.qa],
            current: 'loader',
        });
        this.errorSectionTitle = new Component(browser, {
            path: [this.qa],
            current: 'errorSectionTitle',
        });
        this.errorOrderId = new Component(browser, {
            path: [this.qa],
            current: 'errorOrderId',
        });
        this.bookHotelInfo = new TestHotelsBookHotelInfo(browser, {
            path: [this.qa],
            current: 'bookHotelInfo',
        });
        this.bookPartnerHotelInfo = new TestBookPartnerHotelInfo(browser);
    }
}
