/**
 * Pricebar with disclaimer
 */

const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar } = require('../../src/utils/selector');
const { font, color } = require('../../src/utils/styles');
const rgbToHex = require('../../src/utils/rgb-to-hex');
const getStyles = require('../../src/utils/get-styles');
const { DISCLAIMER } = require('../../src/utils/url');

const BOOK_URL = DISCLAIMER.BOOK;
const GUN_URL = DISCLAIMER.GUN;

let browser;
let page;

describe('Pricebar: with disclaimer', () => {
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

    test('should have books disclaimer with proper styles', async () => {
        await page.goto(BOOK_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const disclaimerEl = await page.$(bar.DISCLAIMER.EL);
        const disclaimerTextEl = await page.$(bar.DISCLAIMER.TEXT);

        const disclaimerTextStyles = await page.evaluate(
            getStyles,
            bar.DISCLAIMER.TEXT,
        );

        expect(disclaimerEl).toBeDefined();
        expect(disclaimerTextEl).toBeDefined();

        expect(disclaimerTextStyles.fontSize).toBe(font.size.bodySmall);
        expect(disclaimerTextStyles.fontFamily).toBe(font.family);
        expect(disclaimerTextStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(disclaimerTextStyles.color)).toBe(color.white.default);

        const expected = expect.stringMatching(
            /^В[оo]з[pр][аa]стн[оo][eе] [оo]г[рp][аa]нич[eе]ни[eе]$/,
        );
        expect(disclaimerTextStyles.text).toStrictEqual(expected);
    });

    test('should have guns disclaimer with proper styles', async () => {
        await page.goto(GUN_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const disclaimerEl = await page.$(bar.DISCLAIMER.EL);
        const disclaimerTextEl = await page.$(bar.DISCLAIMER.TEXT);

        const disclaimerTextStyles = await page.evaluate(
            getStyles,
            bar.DISCLAIMER.TEXT,
        );

        expect(disclaimerEl).toBeDefined();
        expect(disclaimerTextEl).toBeDefined();

        expect(disclaimerTextStyles.fontSize).toBe(font.size.bodySmall);
        expect(disclaimerTextStyles.fontFamily).toBe(font.family);
        expect(disclaimerTextStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(disclaimerTextStyles.color)).toBe(color.white.default);

        const expected = expect.stringMatching(
            /^К[оo]н[cс]т[pр]уктивн[оo] [cс]х[оo]дные [cс] [оo][pр]ужи[eе]м изд[eе]лия.?$/,
        );
        expect(disclaimerTextStyles.text).toStrictEqual(expected);
    });
});
