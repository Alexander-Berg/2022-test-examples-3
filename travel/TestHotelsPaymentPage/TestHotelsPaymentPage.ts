import {MINUTE} from 'helpers/constants/dates';

import {ITestCard} from 'helpers/project/common/TestTrustForm/card';
import {TestTrustForm} from 'helpers/project/common/TestTrustForm/TestTrustForm';

import {Loader} from 'components/Loader';
import {TestIframe} from 'components/TestIframe';
import {Page} from 'components/Page';
import {TestPrice} from 'components/TestPrice';
import {TestPriceErrorModal} from 'components/TestPriceErrorModal';
import {Component} from 'components/Component';

const PAYMENT_PAGE_SELECTOR = 'hotels-payment-page';

export class TestHotelsPaymentPage extends Page {
    readonly loader: Loader;
    readonly pageLoader: Loader;
    readonly iframe: TestIframe;
    readonly errorModal: TestPriceErrorModal;
    readonly paymentEndsAt: Component;
    readonly nextPaymentPrice: TestPrice;
    readonly hotelName: Component;
    readonly datesAndGuests: Component;
    readonly deferredPaymentLabel: Component;
    private readonly trustForm: TestTrustForm;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, PAYMENT_PAGE_SELECTOR);

        this.loader = new Loader(browser, {
            parent: this.qa,
            current: 'loader',
        });
        this.pageLoader = new Loader(browser, {
            parent: this.qa,
            current: 'pageLoader',
        });
        this.iframe = new TestIframe(browser, {
            parent: this.qa,
            current: 'iframe',
        });
        this.errorModal = new TestPriceErrorModal(browser, {
            parent: this.qa,
            current: 'errorModal',
        });
        this.hotelName = new Component(browser, {
            parent: this.qa,
            current: 'hotelName',
        });
        this.deferredPaymentLabel = new Component(browser, {
            parent: this.qa,
            current: 'deferredPaymentLabel',
        });
        this.datesAndGuests = new Component(browser, {
            parent: this.qa,
            current: 'datesAndGuests',
        });
        this.paymentEndsAt = new Component(browser, {
            parent: this.qa,
            current: 'paymentEndsAt',
        });
        this.nextPaymentPrice = new TestPrice(browser, {
            parent: this.qa,
            current: 'nextPaymentPrice',
        });
        this.trustForm = new TestTrustForm(browser);
    }

    async waitUntilLoaded(): Promise<void> {
        await this.loader.waitUntilLoaded(2 * MINUTE);
    }

    async pay(card: ITestCard): Promise<void> {
        await this.iframe.workInFrame(async () => {
            await this.trustForm.pay(card);
        });
    }
}
