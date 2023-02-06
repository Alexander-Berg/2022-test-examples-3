import {trip} from 'suites/trips';

import TestTripImage from 'helpers/project/trips/components/TestTripImage';
import TestOrdersBlock from 'helpers/project/account/pages/TripPage/components/TestOrdersBlock';
import TestAviaOrder from 'helpers/project/account/pages/TripPage/components/TestAviaOrder/TestAviaOrder';
import TestTrainOrder from 'helpers/project/account/pages/TripPage/components/TestTrainOrder/TestTrainOrder';
import TestHotelOrder from 'helpers/project/account/pages/TripPage/components/TestHotelOrder/TestHotelOrder';
import TestHotelsCrossSaleBlock from 'helpers/project/account/pages/TripPage/components/TestHotelsCrossSaleBlock';

import {Component} from 'components/Component';
import {TestLink} from 'components/TestLink';
import {TestFooter} from 'components/TestFooter';

export default class TestTripPage extends Component {
    backLink: TestLink;
    desktopSupportPhone: Component;
    tripImage: TestTripImage;
    aviaOrdersBlock: TestOrdersBlock<TestAviaOrder>;
    trainOrdersBlock: TestOrdersBlock<TestTrainOrder>;
    hotelOrdersBlock: TestOrdersBlock<TestHotelOrder>;
    hotelsCrossSaleBlock: TestHotelsCrossSaleBlock;
    footer: TestFooter;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'tripPage');

        this.backLink = new TestLink(this.browser, {
            parent: this.qa,
            current: 'backLink',
        });
        this.desktopSupportPhone = new Component(this.browser, {
            parent: this.qa,
            current: 'desktopSupportPhone',
        });
        this.tripImage = new TestTripImage(this.browser, {
            parent: this.qa,
            current: 'tripImage',
        });
        this.aviaOrdersBlock = new TestOrdersBlock(
            this.browser,
            {
                parent: this.qa,
                current: 'aviaOrdersBlock',
            },
            TestAviaOrder,
        );
        this.trainOrdersBlock = new TestOrdersBlock(
            this.browser,
            {
                parent: this.qa,
                current: 'trainOrdersBlock',
            },
            TestTrainOrder,
        );
        this.hotelOrdersBlock = new TestOrdersBlock(
            this.browser,
            {
                parent: this.qa,
                current: 'hotelOrdersBlock',
            },
            TestHotelOrder,
        );
        this.hotelsCrossSaleBlock = new TestHotelsCrossSaleBlock(this.browser, {
            parent: this.qa,
            current: 'hotelsCrossSaleBlock',
        });
        this.footer = new TestFooter(browser);
    }

    /**
     * @default '1' - для моков не важен идентификатор заказа
     * @param tripId
     */
    async goToTrip(tripId: string = '1'): Promise<void> {
        await this.browser.url(trip.url(tripId));
    }

    async isSupportPhoneVisible(): Promise<boolean> {
        if (this.isTouch) {
            return this.footer.isSupportPhoneVisible();
        }

        return this.desktopSupportPhone.isVisible();
    }
}
