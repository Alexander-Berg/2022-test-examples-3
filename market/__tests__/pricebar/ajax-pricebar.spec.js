/**
 * Pricebar "измененине информации на прайсбаре"
 */

const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar, ajaxPage } = require('../../src/utils/selector');
const { getInnerText } = require('../../src/utils/get-text');
const { AJAX_PAGE_PRICEBAR } = require('../../src/utils/url');

const PAGE_URL = AJAX_PAGE_PRICEBAR;

let browser;
let page;

describe('ajax', () => {
    beforeAll(async () => {
        browser = await extension.launch({ headless: false });
    });

    beforeEach(async () => {
        page = await browser.newPage();

        await page.setUserAgent(desktop.CHROME);
        await page.setViewport(viewPort.CUSTOM);
    });

    afterEach(async () => {
        if (page) {
            await page.close();
        }
    });

    afterAll(async () => {
        if (browser) {
            await browser.close();
        }
    });

    test('should change pricebar text', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.TEXT, { visible: true });

        const textBefore = await page.evaluate(getInnerText, bar.TEXT);

        await page.waitForSelector(ajaxPage.BUTTON.FILTER, {
            visible: true,
        });
        const filters = await page.$$(ajaxPage.BUTTON.FILTER);
        await filters[1].click();
        await page.waitForTimeout(5000);
        await page.waitForSelector(bar.TEXT, { visible: true });

        const textAfter = await page.evaluate(getInnerText, bar.TEXT);

        expect(textBefore).not.toBe(textAfter);
    });
});
