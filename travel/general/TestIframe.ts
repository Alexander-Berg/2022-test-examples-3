import {Component} from 'components/Component';

export class TestIframe extends Component {
    async workInFrame(func: () => Promise<void>): Promise<void> {
        await this.waitForExist();

        const frame = await this.browser.$(this.selector);

        await this.browser.switchToFrame(frame);
        await func();

        await this.browser.switchToFrame(null);
    }
}
