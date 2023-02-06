const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar } = require('../../src/utils/selector');

const { PRICE } = require('../../src/utils/url');

const PAGE_URL = PRICE.AFFORDABLE;

let browser;
let page;

describe('Pricebar: regular bar with view button', () => {
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

    test('view-button is clickable', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });

        const element = await page.$(bar.BUTTON.VIEW);

        const currentPageUrl = page.url();

        const currentPage = page.target();
        await page.click(bar.BUTTON.VIEW);
        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();

        expect(element).toBeDefined();
        expect(newPageUrl).not.toBe(currentPageUrl);
    });
});
