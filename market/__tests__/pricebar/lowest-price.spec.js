/**
 * Pricebar "На этой странице самая низкая цена"
 */

const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar, mainPopup } = require('../../src/utils/selector');
const { font, color } = require('../../src/utils/styles');
const rgbToHex = require('../../src/utils/rgb-to-hex');
const getStyles = require('../../src/utils/get-styles');
const { PRICE } = require('../../src/utils/url');

const PAGE_URL = PRICE.LOWEST;

let browser;
let page;

describe('Pricebar: the lowest price', () => {
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

    test('styles', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });

        const barTextEl = await page.$(bar.TEXT);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        const barTextStyles = await page.evaluate(getStyles, bar.TEXT);
        const barMoreStyles = await page.evaluate(getStyles, bar.BUTTON.MORE);

        expect(barTextEl).toBeDefined();
        expect(barMoreEl).toBeDefined();

        expect(barTextStyles.fontSize).toBe(font.size.body);
        expect(barTextStyles.fontFamily).toBe(font.family);
        expect(barTextStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(barTextStyles.color)).toBe(color.black.legacy);

        expect(barMoreStyles.fontSize).toBe(font.size.body);
        expect(barMoreStyles.fontFamily).toBe(font.family);
        expect(barMoreStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(barMoreStyles.color)).toBe(color.black.legacy);
        expect(rgbToHex(barMoreStyles.backgroundColor)).toBe(
            color.white.default,
        );
    });
});
