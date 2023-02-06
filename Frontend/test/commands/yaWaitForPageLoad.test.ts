import { yaWaitForPageLoad } from '../../src/commands';
import { MockBrowser } from '../typings';

describe('commands/yaWaitForPageLoad', () => {
    let browser: MockBrowser;

    beforeEach(() => {
        browser = {} as unknown as MockBrowser;
        browser.waitUntil = jest.fn();
        browser.addCommand = (handler: Function): void => {
            browser.yaWaitForPageLoad = handler;
        };
    });

    it('should call wait until once', async() => {
        browser.addCommand(yaWaitForPageLoad({ pageLoadTimeout: 100 }));
        await browser.yaWaitForPageLoad();

        expect(browser.waitUntil).toBeCalledTimes(1);
    });
});
