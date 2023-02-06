import {Component} from 'helpers/project/common/components';

/* Constants */
const MARKER_DEFAULT_TIMEOUT = 15000;

export class TestHotelPageMapMarker extends Component {
    marker: Component;
    hotelName: Component;

    constructor(browser: WebdriverIO.Browser, qa?: string) {
        super(browser, {parent: qa, current: 'mapMarker'});

        this.marker = new Component(browser, this.qa);

        this.hotelName = new Component(browser, {
            parent: this.qa,
            current: 'hotelNameWithStars',
        });
    }

    isMarkerDisplayed(
        timeout: number = MARKER_DEFAULT_TIMEOUT,
    ): Promise<boolean> {
        return this.isDisplayed(timeout);
    }
}
