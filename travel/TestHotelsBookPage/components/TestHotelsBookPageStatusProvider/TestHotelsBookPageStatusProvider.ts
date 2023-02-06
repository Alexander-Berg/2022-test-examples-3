import {Component} from 'components/Component';

const PAGE_LOAD_DEFAULT_TIMEOUT = 5000;
const FETCH_OFFER_DEFAULT_TIMEOUT = 15000;

export class TestHotelsBookPageStatusProvider extends Component {
    readonly offerPending: Component;
    readonly offerFetched: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.offerPending = new Component(browser, {
            parent: this.qa,
            current: 'isOfferPending',
        });

        this.offerFetched = new Component(browser, {
            parent: this.qa,
            current: 'isOfferFetched',
        });
    }

    isOfferPending(
        timeout = PAGE_LOAD_DEFAULT_TIMEOUT,
    ): Promise<void | boolean> {
        return this.offerPending.waitForVisible(timeout);
    }

    isOfferFetched(
        timeout = FETCH_OFFER_DEFAULT_TIMEOUT,
    ): Promise<void | boolean> {
        return this.offerFetched.waitForVisible(timeout);
    }
}
