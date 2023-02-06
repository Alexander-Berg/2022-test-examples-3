/**
 * Popup "наличие дисклеймера лекарств"
 */

const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const { bar, mainPopup } = require('../../../src/utils/selector');
const { DISCLAIMER } = require('../../../src/utils/url');
const { font, color } = require('../../../src/utils/styles');
const rgbToHex = require('../../../src/utils/rgb-to-hex');
const getStyles = require('../../../src/utils/get-styles');

const PAGE_URL = DISCLAIMER.MEDICINE;

let browser;
let page;

describe('Product: with disclaimer', () => {
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

    test('click on offer', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const disclaimer = await page.$(mainPopup.DISCLAIMER.TEXT);

        const disclaimerStyles = await page.evaluate(
            getStyles,
            mainPopup.DISCLAIMER.TEXT,
        );

        expect(disclaimer).toBeDefined();
        expect(rgbToHex(disclaimerStyles.color)).toBe(color.gray.default);
        expect(disclaimerStyles.fontSize).toBe(font.size.bodyTiny);
    });
});
