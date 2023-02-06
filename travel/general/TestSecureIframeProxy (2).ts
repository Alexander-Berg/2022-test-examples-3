import {Component} from 'components/Component';

export class TestSecureIframeProxy extends Component {
    async workInFrame(func: () => Promise<void>): Promise<void> {
        await this.fallIntoFrame(this.selector);

        await this.fallIntoFrame('iframe');

        await func();

        await this.browser.switchToFrame(null);
    }

    private async fallIntoFrame(selector: string): Promise<void> {
        await this.browser.waitForExist(selector);

        const frame = await this.browser.$(selector);

        await this.browser.switchToFrame(frame);
    }
}
