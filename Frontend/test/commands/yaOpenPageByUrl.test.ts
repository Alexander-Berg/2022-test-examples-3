import { yaOpenPageByUrl } from '../../src/commands';
import { MockBrowser } from '../typings';

describe('commands/yaOpenPage', () => {
    let browser: MockBrowser;
    const hermioneConfig = { baseUrl: 'http://host.com' };

    beforeEach(() => {
        browser = {} as unknown as MockBrowser;
        browser.addCommand = (handler): void => {
            browser.yaOpenPage = handler;
        };
        browser.url = jest.fn(function(): MockBrowser {
            return browser;
        });
        browser.yaWaitForPageLoad = jest.fn(function(): MockBrowser {
            return browser;
        });
    });

    it('should correctly handle string url', async() => {
        browser.addCommand(yaOpenPageByUrl(hermioneConfig));
        await browser.yaOpenPage('/some/url?param=1#hash');

        expect(browser.url).toHaveBeenCalledWith('/some/url?param=1#hash');
        expect(browser.yaWaitForPageLoad).toHaveBeenCalledTimes(1);
    });

    it('should correctly handle object url', async() => {
        const urlObject = {
            protocol: 'http:',
            host: 'host.com',
            pathname: '/p/a/t/h',
            search: {
                param: '1',
            },
            hash: '123',
        };

        browser.addCommand(yaOpenPageByUrl(hermioneConfig));
        await browser.yaOpenPage(urlObject);

        expect(browser.url).toHaveBeenCalledWith('http://host.com/p/a/t/h?param=1#123');
        expect(browser.yaWaitForPageLoad).toHaveBeenCalledTimes(1);
    });

    it('should wait for the block with selector to load', async() => {
        browser.yaShouldSomeBeVisible = jest.fn(selector => {
            return selector === '#some-id' ? Promise.resolve() : Promise.reject();
        });

        browser.addCommand(yaOpenPageByUrl(hermioneConfig));
        const result = browser.yaOpenPage('host.com/page?param=1', '#some-id');

        await expect(result).resolves.toEqual(undefined);

        expect(browser.url).toHaveBeenCalledWith('host.com/page?param=1');
        expect(browser.yaWaitForPageLoad).toHaveBeenCalledTimes(1);
        expect(browser.yaShouldSomeBeVisible).toHaveBeenCalledWith('#some-id');
    });

    it('should reject if the block with specified selector wasn\'t loaded', async() => {
        browser.yaShouldSomeBeVisible = jest.fn(() => Promise.reject());

        browser.addCommand(yaOpenPageByUrl(hermioneConfig));
        const result = browser.yaOpenPage('host.com/page?param=1', '#another-id');

        await expect(result).rejects.toEqual(undefined);

        expect(browser.url).toHaveBeenCalledWith('host.com/page?param=1');
        expect(browser.yaWaitForPageLoad).toHaveBeenCalledTimes(1);
        expect(browser.yaShouldSomeBeVisible).toHaveBeenCalledWith('#another-id');
    });
});
