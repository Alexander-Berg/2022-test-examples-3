const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar } = require('../../src/utils/selector');
const { color, font } = require('../../src/utils/styles');
const getStyles = require('../../src/utils/get-styles');
const rgbToHex = require('../../src/utils/rgb-to-hex');

const { ALI_FAST_DELIVERY } = require('../../src/utils/url');

let browser;
let page;

describe('Pricebar: bar with fast delivery', () => {
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

    test('is clickable', async () => {
        await page.goto(ALI_FAST_DELIVERY, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });

        const element = await page.$(bar.PRODUCT);

        const currentPageUrl = page.url();

        const currentPage = page.target();
        await page.click(bar.PRODUCT);
        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();

        expect(element).toBeDefined();
        expect(newPageUrl).not.toBe(currentPageUrl);
    });

    test('has right appearances', async () => {
        await page.goto(ALI_FAST_DELIVERY, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });

        const pricebarText = await page.evaluate(
            elSelector => document.querySelector(elSelector).innerText,
            bar.TEXT,
        );

        const expected = expect.stringMatching(/бы[cс]т[pр][oо]й/);
        expect(pricebarText).toStrictEqual(expected);

        const productName = await page.$(bar.PRODUCT_NAME);
        const productNameStyles = await page.evaluate(
            getStyles,
            bar.PRODUCT_NAME,
        );

        expect(productName).toBeDefined();
        expect(productNameStyles.fontWeight).toBe(font.weight.bold);

        const pricebarBoldTextStyles = await page.evaluate(
            getStyles,
            bar.TEXT_BOLD,
        );
        expect(pricebarBoldTextStyles.fontWeight).toBe(font.weight.bold);

        const pricebarTextStyles = await page.evaluate(getStyles, bar.TEXT);
        expect(pricebarTextStyles.fontWeight).toBe(font.weight.normal);

        const barMoreStyles = await page.evaluate(getStyles, bar.BUTTON.MORE);

        expect(barMoreStyles.fontSize).toBe(font.size.body);
        expect(barMoreStyles.fontFamily).toBe(font.family);
        expect(barMoreStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(barMoreStyles.color)).toBe(color.black.legacy);

        const barViewStyles = await page.evaluate(getStyles, bar.BUTTON.VIEW);
        expect(barViewStyles.fontSize).toBe(font.size.body);
        expect(barViewStyles.fontFamily).toBe(font.family);
        expect(barViewStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(barViewStyles.color)).toBe(color.black.legacy);
        expect(rgbToHex(barViewStyles.backgroundColor)).toBe(
            color.white.default,
        );
    });
});
