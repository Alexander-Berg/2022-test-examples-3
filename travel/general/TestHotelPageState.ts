import {Component} from 'helpers/project/common/components';

const HOTEL_INFO_LOAD_DEFAULT_TIMEOUT = 25000;

export class TestHotelPageState extends Component {
    private hotelCard: Component;

    constructor(browser: WebdriverIO.Browser, qa: string) {
        super(browser, qa);

        this.hotelCard = new Component(browser, {
            parent: this,
            current: 'hotelCard',
        });
    }

    waitForLoadingFinished(
        timeout = HOTEL_INFO_LOAD_DEFAULT_TIMEOUT,
    ): Promise<void | boolean> {
        return this.hotelCard.waitForVisible(timeout);
    }
}
