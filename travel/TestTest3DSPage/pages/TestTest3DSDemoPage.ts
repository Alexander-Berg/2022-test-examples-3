import {SECOND} from 'helpers/constants/dates';

import TestMessageList from 'helpers/project/testControlPanel/pages/TestTest3DSPage/components/TestMessageList';

import {Component} from 'components/Component';
import {TestSecureIframeProxy} from 'components/TestSecureIframeProxy';

export default class TestTest3DSDemoPage extends Component {
    iframe: TestSecureIframeProxy;
    messageList: TestMessageList;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'test3DSDemoPage');

        this.iframe = new TestSecureIframeProxy(browser, {
            parent: this.qa,
            current: 'iframe',
        });

        this.messageList = new TestMessageList(browser, {
            parent: this.qa,
            current: 'messageList',
        });
    }

    async waitUntilLoaded(): Promise<void> {
        await this.iframe.waitForVisible(10 * SECOND);
    }
}
