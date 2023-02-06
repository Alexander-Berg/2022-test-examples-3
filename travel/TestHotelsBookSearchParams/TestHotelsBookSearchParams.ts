import {Component} from 'components/Component';

const GET_TEXT_DEFAULT_TIMEOUT = 1000;
const SEARCH_PARAMS_QA = 'search-params';

export class TestHotelsBookSearchParams extends Component {
    checkinDate: Component;
    checkoutDate: Component;
    guests: Component;

    constructor(browser: WebdriverIO.Browser, qa?: QA) {
        super(browser, qa);

        this.checkinDate = new Component(browser, {
            parent: this.qa,
            current: `${SEARCH_PARAMS_QA}-checkin-date`,
        });
        this.checkoutDate = new Component(browser, {
            parent: this.qa,
            current: `${SEARCH_PARAMS_QA}-checkout-date`,
        });
        this.guests = new Component(browser, {
            parent: this.qa,
            current: `${SEARCH_PARAMS_QA}-guests`,
        });
    }

    getCheckinDate(timeout = GET_TEXT_DEFAULT_TIMEOUT): Promise<string> {
        return this.checkinDate.getText(timeout);
    }

    getCheckoutDate(timeout = GET_TEXT_DEFAULT_TIMEOUT): Promise<string> {
        return this.checkoutDate.getText(timeout);
    }

    getGuests(timeout = GET_TEXT_DEFAULT_TIMEOUT): Promise<string> {
        return this.guests.getText(timeout);
    }
}
