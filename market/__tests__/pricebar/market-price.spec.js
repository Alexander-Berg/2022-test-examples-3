/**
 * Pricebar "Цена на Яндекс.Маркете"
 */

const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar } = require('../../src/utils/selector');
const { color, font } = require('../../src/utils/styles');
const rgbToHex = require('../../src/utils/rgb-to-hex');
const getStyles = require('../../src/utils/get-styles');
const { PRICE } = require('../../src/utils/url');

const PAGE_URL = PRICE.MARKET_SEARCH;

let browser;
let page;

describe('Pricebar: market price', () => {
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

    test('should be closeable', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        await page.click(bar.BUTTON.CLOSE);

        await page.waitForTimeout(300);
        const element = await page.$(bar.PRODUCT);

        expect(element).toBeNull();
    });

    test('styles', async () => {
        const pageUrl = PRICE.MARKET_MODEL;

        await page.goto(pageUrl, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        await page.waitForSelector(bar.TEXT, { visible: true });

        const pricebar = await page.$(bar.TEXT);

        const productName = await page.$(bar.PRODUCT_NAME);
        const productNameStyles = await page.evaluate(
            getStyles,
            bar.PRODUCT_NAME,
        );

        const price = await page.$(bar.PRICE);
        const priceStyles = await page.evaluate(getStyles, bar.PRICE);

        const delivery = await page.$(bar.DELIVERY.CONTAINER);

        const barButtonView = await page.$(bar.BUTTON.VIEW);
        const barButtonViewStyles = await page.evaluate(
            getStyles,
            bar.BUTTON.VIEW,
        );

        const barButtonMore = await page.$(bar.BUTTON.MORE);
        const barButtonMoreStyles = await page.evaluate(
            getStyles,
            bar.BUTTON.MORE,
        );

        const text = await page.evaluate(
            elSelector => document.querySelector(elSelector).innerText,
            bar.TEXT,
        );
        const result = text.match(/в м[аa]г[аa]зин[eе]/);
        const shopName = text.slice(result.index + result[0].length).trim();

        expect(pricebar).toBeDefined();
        expect(delivery).toBeDefined();

        expect(productName).toBeDefined();
        expect(productNameStyles.fontWeight).toBe(font.weight.bold);

        expect(price).toBeDefined();
        expect(priceStyles.fontWeight).toBe(font.weight.bold);

        expect(barButtonView).toBeDefined();
        expect(rgbToHex(barButtonViewStyles.backgroundColor)).toBe(
            color.white.default,
        );

        expect(barButtonMore).toBeDefined();
        expect(rgbToHex(barButtonMoreStyles.backgroundColor)).toBe(
            color.black.default,
        );

        expect(shopName).toBeTruthy();
    });
});
