const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar, mainPopup } = require('../../src/utils/selector');

const { BOOKS_BY_AUTHOR } = require('../../src/utils/url');

let browser;
let page;

describe('Pricebar: bar with books by author', () => {
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

    test('appears and not clickable', async () => {
        await page.goto(BOOKS_BY_AUTHOR, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);

        const currentPageUrl = page.url();
        await page.click(bar.PRODUCT);
        const newPage = await page.target();
        const newPageUrl = newPage.url();

        expect(newPageUrl).toBe(currentPageUrl);

        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const mainPopupEl = await page.$(mainPopup.COMMON);
        const mainPopupClassProp = await mainPopupEl.getProperty('className');
        const mainPopupClasses = await mainPopupClassProp.jsonValue();

        expect(barEl).toBeDefined();
        expect(mainPopupEl).toBeDefined();
        expect(
            mainPopupClasses.split(' ').includes('popup_visible'),
        ).toBeTruthy();

        const pricebarText = await page.evaluate(
            elSelector => document.querySelector(elSelector).innerText,
            bar.TEXT,
        );

        const expected = expect.stringMatching(/Д[рp][уy]ги[еe] книги/);

        expect(pricebarText).toStrictEqual(expected);
    });
});
