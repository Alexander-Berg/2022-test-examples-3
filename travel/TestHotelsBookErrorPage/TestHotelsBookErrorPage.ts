import {TestHotelsBookHotelInfo} from 'helpers/project/hotels/components/TestHotelsBookHotelInfo';
import {TestBookPartnerHotelInfo} from 'helpers/project/hotels/components/TestBookPartnerHotelInfo';

import {Page} from 'components/Page';
import {Loader} from 'components/Loader';
import {Component} from 'components/Component';

/** @see BookErrorPage */
export class TestHotelsBookErrorPage extends Page {
    loader: Loader;
    bookHotelInfo: TestHotelsBookHotelInfo;
    errorTitle: Component;
    errorSectionTitle: Component;
    searchHotelsButton: Component;
    searchHotelButton: Component;
    bookPartnerHotelInfo: TestBookPartnerHotelInfo;
    supportLink: Component;
    errorOrderId: Component;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'hotelsBookErrorPage');

        this.loader = new Loader(browser, {
            path: [this.qa],
            current: 'loader',
        });
        this.bookHotelInfo = new TestHotelsBookHotelInfo(browser, {
            path: [this.qa],
            current: 'bookHotelInfo',
        });
        this.errorTitle = new Component(browser, {
            path: [this.qa],
            current: 'errorTitle',
        });
        this.errorSectionTitle = new Component(browser, {
            path: [this.qa],
            current: 'errorSectionTitle',
        });
        this.searchHotelsButton = new Component(browser, {
            path: [this.qa],
            current: 'searchHotelsButton',
        });
        this.searchHotelButton = new Component(browser, {
            path: [this.qa],
            current: 'searchHotelButton',
        });
        this.supportLink = new Component(browser, {
            path: [this.qa],
            current: 'supportLink',
        });
        this.bookPartnerHotelInfo = new TestBookPartnerHotelInfo(browser);
        this.errorOrderId = new Component(browser, {
            path: [this.qa],
            current: 'errorOrderId',
        });
    }
}
