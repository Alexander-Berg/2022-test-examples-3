/**
 * Pricebar "Более выгодная цена"
 */

const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar } = require('../../src/utils/selector');
const { font, color } = require('../../src/utils/styles');
const rgbToHex = require('../../src/utils/rgb-to-hex');
const getStyles = require('../../src/utils/get-styles');
const { PRICE } = require('../../src/utils/url');

const PAGE_URL = PRICE.AFFORDABLE;

let browser;
let page;

describe('Pricebar: more affordable price', () => {
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

    test('should be clickable', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        const currentPageUrl = page.url();

        const currentPage = page.target();
        await page.click(bar.PRODUCT);
        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();

        expect(barEl).toBeDefined();
        expect(newPageUrl).not.toBe(currentPageUrl);
    });

    test('should have proper elements and styles', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });

        const barTextEl = await page.$(bar.TEXT);
        const barProductNameEl = await page.$(bar.PRODUCT_NAME);
        const barCurrencyEl = await page.$(bar.CURRENCY);
        const barDeliveryEl = await page.$(bar.DELIVERY.CONTAINER);
        const barPriceEl = await page.$(bar.PRICE);
        const barProfitEl = await page.$(bar.PROFIT);
        const barViewEl = await page.$(bar.BUTTON.VIEW);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        const barTextStyles = await page.evaluate(getStyles, bar.TEXT);
        const barProductNameStyles = await page.evaluate(
            getStyles,
            bar.PRODUCT_NAME,
        );
        const barProfitStyles = await page.evaluate(getStyles, bar.PROFIT);
        const barViewStyles = await page.evaluate(getStyles, bar.BUTTON.VIEW);
        const barMoreStyles = await page.evaluate(getStyles, bar.BUTTON.MORE);

        expect(barTextEl).toBeDefined();
        expect(barProductNameEl).toBeDefined();
        expect(barDeliveryEl).toBeDefined();
        expect(barCurrencyEl).toBeDefined();
        expect(barProfitEl).toBeDefined();
        expect(barViewEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
        expect(barPriceEl).toBeDefined();

        expect(barTextStyles.fontSize).toBe(font.size.body);
        expect(barTextStyles.fontFamily).toBe(font.family);
        expect(barTextStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(barTextStyles.color)).toBe(color.black.legacy);

        expect(barProductNameStyles.fontSize).toBe(font.size.body);
        expect(barProductNameStyles.fontFamily).toBe(font.family);
        expect(barProductNameStyles.fontWeight).toBe(font.weight.bold);
        expect(rgbToHex(barProductNameStyles.color)).toBe(color.black.legacy);

        expect(barProfitStyles.fontSize).toBe(font.size.bodySmall);
        expect(barProfitStyles.fontFamily).toBe(font.family);
        expect(barProfitStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(barProfitStyles.color)).toBe(color.white.default);
        expect(rgbToHex(barProfitStyles.backgroundColor)).toBe(
            color.red.legacy,
        );

        expect(barMoreStyles.fontSize).toBe(font.size.body);
        expect(barMoreStyles.fontFamily).toBe(font.family);
        expect(barMoreStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(barMoreStyles.color)).toBe(color.black.legacy);

        expect(barViewStyles.fontSize).toBe(font.size.body);
        expect(barViewStyles.fontFamily).toBe(font.family);
        expect(barViewStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(barViewStyles.color)).toBe(color.black.legacy);
    });
});
