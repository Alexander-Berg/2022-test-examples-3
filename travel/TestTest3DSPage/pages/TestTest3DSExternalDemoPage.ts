import {SECOND} from 'helpers/constants/dates';

import {Component} from 'components/Component';
import {TestSecureIframeProxy} from 'components/TestSecureIframeProxy';

export default class TestTest3DSExternalDemoPage extends Component {
    iframe: TestSecureIframeProxy;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'test3DSExternalDemoPage');

        this.iframe = new TestSecureIframeProxy(browser, {
            parent: this.qa,
            current: 'iframe',
        });
    }

    async waitUntilLoaded(): Promise<void> {
        await this.iframe.waitForVisible(10 * SECOND);
    }
}
