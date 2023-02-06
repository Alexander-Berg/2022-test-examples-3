import moment, {Moment} from 'moment';

import {SECOND} from 'helpers/constants/dates';

import dateFormats from 'helpers/utilities/date/formats';

import {Component} from 'components/Component';
import {TestHotelsCrossSaleMap} from 'components/TestHotelsCrossSaleMap';

interface ICheckinCheckoutDates {
    checkinDate: Moment;
    checkoutDate: Moment;
}

export default class TestHotelsCrossSaleBlock extends Component {
    title: Component;
    checkinDate: Component;
    checkoutDate: Component;
    map: TestHotelsCrossSaleMap;
    closeIcon: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });
        this.checkinDate = new Component(this.browser, {
            parent: this.qa,
            current: 'checkinDate',
        });
        this.checkoutDate = new Component(this.browser, {
            parent: this.qa,
            current: 'checkoutDate',
        });
        this.map = new TestHotelsCrossSaleMap(this.browser, {
            parent: this.qa,
            current: 'map',
        });
        this.closeIcon = new Component(this.browser, {
            parent: this.qa,
            current: 'closeIcon',
        });
    }

    async parseDates(): Promise<ICheckinCheckoutDates> {
        const [checkinDateText, checkoutDateText] = await Promise.all([
            this.checkinDate.getText(),
            this.checkoutDate.getText(),
        ]);

        const [checkinDateDateText, checkinDateMonthText] =
            checkinDateText.split(' ');
        const [, checkoutDateMonthText] = checkoutDateText.split(' ');

        return {
            checkinDate: moment(
                `${checkinDateDateText} ${
                    checkinDateMonthText ?? checkoutDateMonthText
                }`,
                dateFormats.HUMAN,
            ),
            checkoutDate: moment(checkoutDateText, dateFormats.HUMAN),
        };
    }

    async waitForLoading(): Promise<void> {
        await this.title.waitForVisible(10 * SECOND);
    }
}
