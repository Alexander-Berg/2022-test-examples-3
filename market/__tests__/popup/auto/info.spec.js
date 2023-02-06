/**
 * Click on `question` autobar icon should open info popup
 */

const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const { bar, infoPopup, mod } = require('../../../src/utils/selector');
const { AUTO } = require('../../../src/utils/url');

const PAGE_URL = AUTO.MORE_OFFERS;

let browser;
let page;

describe('Autobar', () => {
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

    test('info popup opens on icon click', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.AUTO, { visible: true });
        const barEl = await page.$(bar.AUTO);
        const barInfoEl = await page.$(bar.BUTTON.INFO);

        await page.click(bar.BUTTON.INFO);
        await page.waitForSelector(infoPopup.AUTO, { visible: true });

        const infoPopupEl = await page.$(infoPopup.AUTO);
        const infoPopupClassProp = await infoPopupEl.getProperty('className');
        const infoPopupClassesRaw = await infoPopupClassProp.jsonValue();
        const infoPopupClasses = infoPopupClassesRaw.split(' ');

        expect(barEl).toBeDefined();
        expect(barInfoEl).toBeDefined();
        expect(infoPopupClasses.includes(mod.VISIBLE)).toBeTruthy();
    });
});
