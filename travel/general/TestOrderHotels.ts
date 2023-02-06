import {Loader} from 'helpers/project/common/components/Loader';
import {Page} from 'helpers/project/common/components/Page';
import {TestHotelsCancelOrderModal} from 'helpers/project/hotels/pages/TestHotelsHappyPage/components/TestHotelsCancelOrderModal';
import TestOrderHotelsGuests from 'helpers/project/account/pages/OrderPage/components/TestOrderHotelsGuests/TestOrderHotelsGuests';
import TestOrderHotelMainInfo from 'helpers/project/account/pages/OrderPage/components/TestOrderHotelMainInfo/TestOrderHotelMainInfo';
import TestOrderHotelsError from 'helpers/project/account/pages/OrderPage/components/TestOrderHotelsError/TestOrderHotelsError';
import TestOrderHotelsDeferredPayment from 'helpers/project/account/pages/OrderPage/components/TestOrderHotelsDeferredPayment/TestOrderHotelsDeferredPayment';
import TestOrderHotelsInfo from 'helpers/project/account/pages/OrderPage/components/TestOrderHotelsInfo/TestOrderHotelsInfo';
import TestOrderHotelsActions from 'helpers/project/account/pages/OrderPage/components/TestOrderHotelsActions/TestOrderHotelsActions';

export default class TestOrderHotels extends Page {
    guests: TestOrderHotelsGuests;
    cancelOrderModal: TestHotelsCancelOrderModal;
    error: TestOrderHotelsError;
    loader: Loader;
    mainInfo: TestOrderHotelMainInfo;
    deferredPayment: TestOrderHotelsDeferredPayment;
    hotelInfo: TestOrderHotelsInfo;
    hotelActions: TestOrderHotelsActions;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'hotelOrder');

        this.mainInfo = new TestOrderHotelMainInfo(this.browser, {
            parent: this.qa,
            current: 'mainInfo',
        });
        this.hotelInfo = new TestOrderHotelsInfo(this.browser, {
            parent: this.qa,
            current: 'hotelInfo',
        });
        this.guests = new TestOrderHotelsGuests(this.browser, {
            parent: this.qa,
            current: 'guests',
        });
        this.hotelActions = new TestOrderHotelsActions(this.browser, {
            parent: this.qa,
            current: 'hotelActions',
        });
        this.cancelOrderModal = new TestHotelsCancelOrderModal(this.browser);
        this.error = new TestOrderHotelsError(this.browser, {
            parent: this.qa,
            current: 'error',
        });
        this.loader = new Loader(this.browser);

        this.deferredPayment = new TestOrderHotelsDeferredPayment(
            this.browser,
            {
                parent: this.qa,
                current: 'deferredPayment',
            },
        );
    }

    waitOrderLoaded(): Promise<void> {
        return this.loader.waitUntilLoaded();
    }
}
