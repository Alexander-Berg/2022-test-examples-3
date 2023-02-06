/**
 * Hover state for pricebar
 */

const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar, mainPopup } = require('../../src/utils/selector');
const { color, opacity } = require('../../src/utils/styles');
const getStyles = require('../../src/utils/get-styles');
const rgbToHex = require('../../src/utils/rgb-to-hex');
const { PRICE } = require('../../src/utils/url');

const PAGE_URL = PRICE.MARKET_SEARCH;

let browser;
let page;

describe('Pricebar: hover state', () => {
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

    test('should change color of elements when hovered', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });

        const infoButtonStyles = await page.evaluate(
            getStyles,
            bar.BUTTON.INFO,
        );
        const settingsButtonStyles = await page.evaluate(
            getStyles,
            bar.BUTTON.SETTINGS,
        );
        const closeButtonStyles = await page.evaluate(
            getStyles,
            bar.BUTTON.CLOSE,
        );

        await page.hover(bar.PRODUCT);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const barEl = await page.$(bar.PRODUCT);
        const barViewEl = await page.$(bar.BUTTON.VIEW);
        const barMoreEl = await page.$(bar.BUTTON.MORE);
        const infoButtonEl = await page.$(bar.BUTTON.INFO);
        const settingsButtonEl = await page.$(bar.BUTTON.SETTINGS);
        const closeButtonEl = await page.$(bar.BUTTON.CLOSE);

        const barHoverStyles = await page.evaluate(getStyles, bar.PRODUCT);

        const barViewHoverStyles = await page.evaluate(
            getStyles,
            bar.BUTTON.VIEW,
        );

        const barMoreHoverStyles = await page.evaluate(
            getStyles,
            bar.BUTTON.MORE,
        );

        /* Hover per button */

        await page.hover(bar.BUTTON.INFO);
        await page.waitForTimeout(300); // yes, this thing is too slow for hover in real time
        const infoButtonHoverStyles = await page.evaluate(
            getStyles,
            bar.BUTTON.INFO,
        );

        await page.hover(bar.BUTTON.SETTINGS);
        await page.waitForTimeout(300);
        const settingsButtonHoverStyles = await page.evaluate(
            getStyles,
            bar.BUTTON.SETTINGS,
        );

        await page.hover(bar.BUTTON.CLOSE);
        await page.waitForTimeout(300);
        const closeButtonHoverStyles = await page.evaluate(
            getStyles,
            bar.BUTTON.CLOSE,
        );

        expect(barEl).toBeDefined();
        expect(barViewEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
        expect(infoButtonEl).toBeDefined();
        expect(settingsButtonEl).toBeDefined();
        expect(closeButtonEl).toBeDefined();

        expect(rgbToHex(barHoverStyles.backgroundColor)).toBe(
            color.yellow.hover,
        );
        expect(barViewHoverStyles.borderColor).toBe(color.lightGray.hover);
        expect(barMoreHoverStyles.borderColor).toBe(color.lightGray.hover);

        expect(infoButtonStyles.opacity).toBe(opacity['30']);
        expect(settingsButtonStyles.opacity).toBe(opacity['30']);
        expect(closeButtonStyles.opacity).toBe(opacity['30']);

        expect(infoButtonHoverStyles.opacity).toBe(opacity['50']);
        expect(settingsButtonHoverStyles.opacity).toBe(opacity['50']);
        expect(closeButtonHoverStyles.opacity).toBe(opacity['50']);
    });
});
