import {MINUTE} from 'helpers/constants/dates';

import {Loader} from 'components/Loader';
import {Page} from 'components/Page';

const START_PAYMENT_PAGE_QA = 'startDeferredPaymentPage';

class TestHotelsStartDeferredPaymentPage extends Page {
    readonly pageLoader: Loader;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, START_PAYMENT_PAGE_QA);

        this.pageLoader = new Loader(browser, {
            parent: this.qa,
            current: 'pageLoader',
        });
    }

    async waitForPageLoading(): Promise<void> {
        await this.pageLoader.waitUntilLoaded(4 * MINUTE);
    }
}

export default TestHotelsStartDeferredPaymentPage;
