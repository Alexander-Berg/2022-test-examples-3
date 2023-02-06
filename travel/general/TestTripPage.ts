import {trip} from 'suites/trips';

import TestTripImage from 'helpers/project/trips/components/TestTripImage';
import TestOrdersBlock from 'helpers/project/account/pages/TripPage/components/TestOrdersBlock';
import TestAviaOrder from 'helpers/project/account/pages/TripPage/components/TestAviaOrder/TestAviaOrder';
import TestTrainOrder from 'helpers/project/account/pages/TripPage/components/TestTrainOrder/TestTrainOrder';
import TestHotelOrder from 'helpers/project/account/pages/TripPage/components/TestHotelOrder/TestHotelOrder';
import TestHotelsCrossSaleBlock from 'helpers/project/account/pages/TripPage/components/TestHotelsCrossSaleBlock';
import TestActivitiesBlock from 'helpers/project/account/pages/TripPage/components/TestActivitiesBlock/TestActivitiesBlock';
import TestBusOrder from 'helpers/project/account/pages/TripPage/components/TestBusOrder/TestBusOrder';
import TestForecastBlock from 'helpers/project/account/pages/TripPage/components/TestForecastBlock/TestForecastBlock';

import {Component} from 'components/Component';
import {TestLink} from 'components/TestLink';
import TestLayoutDefault from 'components/TestLayoutDefault/TestLayoutDefault';

export default class TestTripPage extends TestLayoutDefault {
    skeleton: Component;
    backLink: TestLink;
    desktopSupportPhone: Component;
    tripImage: TestTripImage;
    aviaOrdersBlock: TestOrdersBlock<TestAviaOrder>;
    trainOrdersBlock: TestOrdersBlock<TestTrainOrder>;
    hotelOrdersBlock: TestOrdersBlock<TestHotelOrder>;
    busOrdersBlock: TestOrdersBlock<TestBusOrder>;
    hotelsCrossSaleBlock: TestHotelsCrossSaleBlock;
    activitiesBlock: TestActivitiesBlock;
    forecastBlock: TestForecastBlock;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'tripPage');

        this.skeleton = new Component(this.browser, {
            parent: this.qa,
            current: 'skeleton',
        });
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
        this.busOrdersBlock = new TestOrdersBlock(
            this.browser,
            {
                parent: this.qa,
                current: 'busOrdersBlock',
            },
            TestBusOrder,
        );
        this.hotelsCrossSaleBlock = new TestHotelsCrossSaleBlock(this.browser, {
            parent: this.qa,
            current: 'hotelsCrossSaleBlock',
        });
        this.activitiesBlock = new TestActivitiesBlock(this.browser, {
            parent: this.qa,
            current: 'activitiesBlock',
        });
        this.forecastBlock = new TestForecastBlock(this.browser, {
            parent: this.qa,
            current: 'forecastBlock',
        });
    }

    /**
     * @default '1' - для моков не важен идентификатор заказа
     * @param tripId
     */
    async goToTrip(tripId: string = '1'): Promise<void> {
        await this.browser.url(trip.url(tripId));
        await this.waitForLoading();
    }

    async isSupportPhoneVisible(): Promise<boolean> {
        if (this.isTouch) {
            return this.footer.isSupportPhoneVisible();
        }

        return this.desktopSupportPhone.isVisible();
    }

    async waitForLoading(): Promise<void> {
        await this.skeleton.waitForHidden();
    }
}
