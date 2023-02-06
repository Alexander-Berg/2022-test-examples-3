const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const { bar, mainPopup, mod } = require('../../../src/utils/selector');
const { CV, PRICE, SEARCH } = require('../../../src/utils/url');

let browser;
let page;

describe('Product: close popup by close button', () => {
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

    test('cv popup', async () => {
        await page.goto(CV, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const popupEl = await page.$(mainPopup.COMMON, { visible: true });

        await page.click(mainPopup.BUTTON.CLOSE);
        await page.$(mainPopup.COMMON, { visible: false });

        const popupClassProp = await popupEl.getProperty('className');
        const popupClassesRaw = await popupClassProp.jsonValue();
        const popupClasses = popupClassesRaw.split(' ');

        expect(popupEl).toBeDefined();
        expect(popupClasses.includes(mod.VISIBLE)).toBeFalsy();
    });

    describe('model popup', () => {
        test('affordable price', async () => {
            await page.goto(PRICE.AFFORDABLE, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.PRODUCT, { visible: true });
            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const popupEl = await page.$(mainPopup.COMMON, {
                visible: true,
            });

            await page.click(mainPopup.BUTTON.CLOSE);
            await page.$(mainPopup.COMMON, { visible: false });

            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(popupEl).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeFalsy();
        });

        test('lowest price', async () => {
            await page.goto(PRICE.LOWEST, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.PRODUCT, { visible: true });
            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const popupEl = await page.$(mainPopup.COMMON, {
                visible: true,
            });

            await page.click(mainPopup.BUTTON.CLOSE);
            await page.$(mainPopup.COMMON, { visible: false });

            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(popupEl).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeFalsy();
        });

        test('price on market', async () => {
            await page.goto(PRICE.MARKET_MODEL, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.PRODUCT, { visible: true });
            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const popupEl = await page.$(mainPopup.COMMON, {
                visible: true,
            });

            await page.click(mainPopup.BUTTON.CLOSE);
            await page.$(mainPopup.COMMON, { visible: false });

            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(popupEl).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeFalsy();
        });

        test('price for a new item', async () => {
            await page.goto(PRICE.NEW, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.PRODUCT, { visible: true });
            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const popupEl = await page.$(mainPopup.COMMON, {
                visible: true,
            });

            await page.click(mainPopup.BUTTON.CLOSE);
            await page.$(mainPopup.COMMON, { visible: false });

            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(popupEl).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeFalsy();
        });
    });

    describe('search popup', () => {
        test('price on market', async () => {
            await page.goto(PRICE.MARKET_SEARCH, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.PRODUCT, { visible: true });
            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const popupEl = await page.$(mainPopup.COMMON, {
                visible: true,
            });

            await page.click(mainPopup.BUTTON.CLOSE);
            await page.$(mainPopup.COMMON, { visible: false });

            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(popupEl).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeFalsy();
        });

        test('affordable price', async () => {
            await page.goto(SEARCH, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.PRODUCT, { visible: true });
            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const popupEl = await page.$(mainPopup.COMMON, {
                visible: true,
            });

            await page.click(mainPopup.BUTTON.CLOSE);
            await page.$(mainPopup.COMMON, { visible: false });

            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(popupEl).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeFalsy();
        });
    });
});
