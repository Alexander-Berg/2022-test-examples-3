/**
 * Pricebar "Компьютерное Зрение"
 */

const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar, mainPopup } = require('../../src/utils/selector');
const { color, font } = require('../../src/utils/styles');
const rgbToHex = require('../../src/utils/rgb-to-hex');
const getStyles = require('../../src/utils/get-styles');
const { CV } = require('../../src/utils/url');

const PAGE_URL = CV;

let browser;
let page;

describe('Pricebar: CV', () => {
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

    test('shows main popup on click', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        await page.click(bar.PRODUCT);

        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const mainPopupEl = await page.$(mainPopup.COMMON);
        const mainPopupClassProp = await mainPopupEl.getProperty('className');
        const mainPopupClasses = await mainPopupClassProp.jsonValue();

        expect(barEl).toBeDefined();
        expect(mainPopupEl).toBeDefined();
        expect(
            mainPopupClasses.split(' ').includes('popup_visible'),
        ).toBeTruthy();
    });

    test('should have proper elements and styles', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        await page.waitForSelector(bar.TEXT, { visible: true });
        const pricebar = await page.$(bar.TEXT);
        const pricebarStyles = await page.evaluate(getStyles, bar.TEXT);

        const productName = await page.$(bar.PRODUCT_NAME);
        const productNameStyles = await page.evaluate(
            getStyles,
            bar.PRODUCT_NAME,
        );

        const barProfitEl = await page.$(bar.PROFIT);
        const barProfitStyles = await page.evaluate(getStyles, bar.PROFIT);

        const barButtonMore = await page.$(bar.BUTTON.MORE);
        const barButtonMoreStyles = await page.evaluate(
            getStyles,
            bar.BUTTON.MORE,
        );

        const cvText = pricebarStyles.text.match(/п[оo][xх][оo]жи[xхй]/);
        expect(cvText).toBeDefined();

        expect(pricebar).toBeDefined();

        expect(productName).toBeDefined();
        expect(productNameStyles.fontWeight).toBe(font.weight.bold);

        expect(barProfitEl).toBeDefined();
        expect(barProfitStyles.fontSize).toBe(font.size.bodySmall);
        expect(barProfitStyles.fontFamily).toBe(font.family);
        expect(barProfitStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(barProfitStyles.color)).toBe(color.white.default);
        expect(rgbToHex(barProfitStyles.backgroundColor)).toBe(
            color.red.legacy,
        );

        expect(barButtonMore).toBeDefined();
        expect(rgbToHex(barButtonMoreStyles.backgroundColor)).toBe(
            color.white.default,
        );
    });
});
