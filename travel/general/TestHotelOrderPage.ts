import {
    Button,
    Component,
    ComponentArray,
} from 'helpers/project/common/components';
import {Loader} from 'helpers/project/common/components/Loader';
import {Page} from 'helpers/project/common/components/Page';
import {TestHotelsBookSearchParams} from 'helpers/project/hotels/pages/TestHotelsBookPage/components/TestHotelsBookSearchParams/TestHotelsBookSearchParams';
import {TestHotelsCancelOrderModal} from 'helpers/project/hotels/pages/TestHotelsHappyPage/components/TestHotelsCancelOrderModal';

import TestHotelFullPrice from './components/TestHotelFullPrice';
import TestHotelOrderPageError from './components/TestHotelOrderPageError';
import {TestPrice} from 'components/TestPrice';

export default class TestHotelOrderPage extends Page {
    hotelName: Component;
    hotelAddress: Component;
    searchParams: TestHotelsBookSearchParams;
    roomName: Component;
    mealInfo: Component;
    bedGroups: Component;
    hotelImage: Component;
    partnerHotelName: Component;
    hotelFullPrice: TestHotelFullPrice;
    guests: ComponentArray;
    cancelButton: Button;
    cancelOrderModal: TestHotelsCancelOrderModal;
    error: TestHotelOrderPageError;
    loader: Loader;
    paymentEndsAt: Component;
    nextPaymentLink: Component;
    nextPaymentPrice: TestPrice;
    deferredPaymentPrepayPrice: TestPrice;
    deferredPaymentFullPrice: TestPrice;
    fiscalReceipts: ComponentArray;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'hotelOrder');

        this.hotelName = new Component(browser, 'hotelName');
        this.hotelAddress = new Component(browser, 'hotelAddress');
        this.searchParams = new TestHotelsBookSearchParams(browser);
        this.roomName = new Component(browser, 'roomName');
        this.mealInfo = new Component(browser, 'mealInfo');
        this.bedGroups = new Component(browser, 'bedGroups');
        this.hotelImage = new Component(browser, 'hotelImage');
        this.partnerHotelName = new Component(browser, 'partnerHotelName');
        this.hotelFullPrice = new TestHotelFullPrice(browser);
        this.guests = new ComponentArray(browser, 'guest', Component);
        this.cancelButton = new Button(browser, 'cancelButton');
        this.cancelOrderModal = new TestHotelsCancelOrderModal(browser);
        this.error = new TestHotelOrderPageError(browser);
        this.loader = new Loader(browser);
        this.deferredPaymentPrepayPrice = new TestPrice(
            browser,
            'deferredPaymentPrepayPrice',
        );
        this.nextPaymentPrice = new TestPrice(browser, 'nextPaymentPrice');
        this.nextPaymentLink = new Component(browser, 'nextPaymentLink');
        this.deferredPaymentFullPrice = new TestPrice(
            browser,
            'deferredPaymentFullPrice',
        );
        this.paymentEndsAt = new Component(browser, 'paymentEndsAt');
        this.fiscalReceipts = new ComponentArray<Component>(
            browser,
            'fiscalReceipt',
            Component,
        );
    }
}
