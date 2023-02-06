/**
 * Welcome page
 */

const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');

const PAGE_URL =
    'https://www.wildberries.ru/catalog/3631944/detail.aspx?targetUrl=BP';

let browser;
let page;

describe.skip('Welcome page', () => {
    beforeAll(async () => {
        browser = await extension.launch({ headless: false });
        page = await browser.newPage();

        await page.setUserAgent(desktop.CHROME);
        await page.setViewport(viewPort.CUSTOM);
    });

    afterAll(async () => {
        if (page) {
            await page.close();
        }
        if (browser) {
            await browser.close();
        }
    });

    test('should have proper clid and pathname', async () => {
        const url = PAGE_URL;
        await page.goto(url, { waitUntil: 'networkidle2' });

        const pages = await browser.pages();
        const welcome = pages[pages.length - 1];
        const welcomeTitle = await welcome.title();
        const welcomeUrl = welcome.url();
        const parsed = new URL(welcomeUrl);

        const { searchParams, protocol, hostname, pathname } = parsed;
        const clid = searchParams.get('clid');

        expect(welcomeTitle).toBe('Яндекс.Советник');
        expect(protocol).toBe('https:');
        expect(hostname).toBe('sovetnik.yandex.ru');
        expect(pathname).toBe('/welcome/');
        expect(welcomeTitle).toBe('Яндекс.Советник');
        expect(clid).toBe('2210590');
    });
});
