import {SECOND} from 'helpers/constants/dates';

import TestMessageList from 'helpers/project/testControlPanel/pages/TestTest3DSPage/components/TestMessageList';

import {Component} from 'components/Component';

export default class TestTest3DSFramePage extends Component {
    messageList: TestMessageList;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'test3DSFramePage');

        this.messageList = new TestMessageList(browser, {
            parent: this.qa,
            current: 'messageList',
        });
    }

    async waitUntilLoaded(): Promise<void> {
        await this.messageList.sendButton.waitForVisible(10 * SECOND);
    }
}
