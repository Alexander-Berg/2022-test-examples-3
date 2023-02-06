import {Component} from 'components/Component';

const VISIBLE_TIMEOUT = 1000;

export class TestHotelsBookHotelCovidInfo extends Component {
    readonly hotelCovidInfo: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.hotelCovidInfo = new Component(browser, {
            parent: this.qa,
            current: 'hotel-covid-info',
        });
    }

    isVisibleInfo(): Promise<void> {
        return this.hotelCovidInfo.waitForVisible(VISIBLE_TIMEOUT);
    }
}
