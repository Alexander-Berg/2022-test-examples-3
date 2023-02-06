import {MINUTE} from 'helpers/constants/dates';

import {TestTrustForm} from 'helpers/project/common/TestTrustForm/TestTrustForm';
import TestOrderDetails from 'helpers/project/trains/pages/TestTrainsPaymentPage/components/TestOrderDetails';

import {Component} from 'components/Component';
import {Loader} from 'components/Loader';
import {TestIframe} from 'components/TestIframe';
import {TestErrorModal} from 'components/TestErrorModal';

export class TestTrainsPaymentPage extends Component {
    readonly loader: Loader;
    readonly iframe: TestIframe;
    readonly orderError: TestErrorModal;
    readonly orderDetails: TestOrderDetails;

    private readonly trustForm: TestTrustForm;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'trainsPaymentPage');

        this.loader = new Loader(browser, {
            parent: this.qa,
            current: 'loader',
        });

        this.iframe = new TestIframe(browser, {
            parent: this.qa,
            current: 'iframe',
        });

        this.orderDetails = new TestOrderDetails(browser, {
            parent: this.qa,
            current: 'orderDetails',
        });

        this.trustForm = new TestTrustForm(browser);

        this.orderError = new TestErrorModal(browser, 'orderError');
    }

    async pay(): Promise<void> {
        await this.iframe.workInFrame(async () => {
            await this.trustForm.pay();
        });
    }

    waitUntilLoaded(): Promise<void> {
        return this.loader.waitUntilLoaded(2 * MINUTE);
    }
}
