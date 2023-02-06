const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar } = require('../../src/utils/selector');
const { ECONOMY_BADGE } = require('../../src/utils/url');

const PAGE_URL = ECONOMY_BADGE;

let browser;
let page;

describe('Pricebar: economy badge click', () => {
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

    test('badge is clickable', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.ECONOMY_BADGE, { visible: true });

        const element = await page.$(bar.BUTTON.ECONOMY_BADGE);

        const currentPageUrl = page.url();

        const currentPage = page.target();
        await page.click(bar.BUTTON.ECONOMY_BADGE);
        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();

        expect(element).toBeDefined();
        expect(newPageUrl).not.toBe(currentPageUrl);
    });
});
