const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar, mainPopup, mod } = require('../../src/utils/selector');
const { color, font, opacity } = require('../../src/utils/styles');
const rgbToHex = require('../../src/utils/rgb-to-hex');
const getTitle = require('../../src/utils/get-title');
const getStyles = require('../../src/utils/get-styles');
const { AUTO } = require('../../src/utils/url');

let browser;
let page;

describe('Auto bar', () => {
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

    describe('more offers', () => {
        test('bar click', async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });
            const barEl = await page.$(bar.AUTO);

            await page.click(bar.AUTO);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const popupEl = await page.$(mainPopup.COMMON);
            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(barEl).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeTruthy();
        });

        test('"view offers" button click', async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });
            const barEl = await page.$(bar.AUTO);
            const moreEl = await page.$(bar.BUTTON.MORE);

            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const popupEl = await page.$(mainPopup.COMMON);
            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(barEl).toBeDefined();
            expect(moreEl).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeTruthy();
        });

        test('bar styles and hover', async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });

            const barStyles = await page.evaluate(getStyles, bar.AUTO);

            await page.hover(bar.AUTO);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const barEl = await page.$(bar.AUTO);
            const moreEl = await page.$(bar.BUTTON.MORE);
            const infoEl = await page.$(bar.BUTTON.INFO);
            const closeEl = await page.$(bar.BUTTON.CLOSE);

            const barHoverStyles = await page.evaluate(getStyles, bar.AUTO);

            const moreAttrs = await page.evaluate(getTitle, bar.BUTTON.MORE);
            const infoAttrs = await page.evaluate(getTitle, bar.BUTTON.INFO);
            const closeAttrs = await page.evaluate(getTitle, bar.BUTTON.CLOSE);

            const popupEl = await page.$(mainPopup.COMMON);
            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(barEl).toBeDefined();
            expect(moreEl).toBeDefined();
            expect(infoEl).toBeDefined();
            expect(closeEl).toBeDefined();

            expect(rgbToHex(barStyles.color)).toBe(color.black.legacy);
            expect(barStyles.fontFamily).toBe(font.family);
            expect(barStyles.fontSize).toBe(font.size.body);
            expect(barStyles.fontWeight).toBe(font.weight.normal);
            expect(rgbToHex(barStyles.backgroundColor)).toBe(
                color.yellow.default,
            );

            expect(rgbToHex(barHoverStyles.color)).toBe(color.black.legacy);
            expect(rgbToHex(barHoverStyles.backgroundColor)).toBe(
                color.yellow.hover,
            );

            expect(moreAttrs.title).toBe('Посмотреть предложения');
            expect(infoAttrs.title).toBe('О программе');
            expect(closeAttrs.title).toBe('Закрыть');

            expect(popupClasses.includes(mod.VISIBLE)).toBeTruthy();
        });

        test(`button 'more' hover`, async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });

            const moreStyles = await page.evaluate(getStyles, bar.BUTTON.MORE);

            await page.hover(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const barEl = await page.$(bar.BUTTON.MORE);
            const moreEl = await page.$(bar.BUTTON.MORE);

            const moreHoverStyles = await page.evaluate(
                getStyles,
                bar.BUTTON.MORE,
            );

            const popupEl = await page.$(mainPopup.COMMON);
            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(barEl).toBeDefined();
            expect(moreEl).toBeDefined();
            expect(rgbToHex(moreStyles.color)).toBe(color.black.legacy);
            expect(rgbToHex(moreStyles.backgroundColor)).toBe(
                color.white.default,
            );
            expect(rgbToHex(moreHoverStyles.color)).toBe(color.black.legacy);
            expect(rgbToHex(moreHoverStyles.backgroundColor)).toBe(
                color.white.default,
            );
            expect(popupClasses.includes(mod.VISIBLE)).toBeTruthy();
        });

        test(`button 'info' hover`, async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });

            const infoStyles = await page.evaluate(getStyles, bar.BUTTON.INFO);

            await page.hover(bar.BUTTON.INFO);
            await page.waitForTimeout(300);

            const infoEl = await page.$(bar.BUTTON.INFO);

            const infoHoverStyles = await page.evaluate(
                getStyles,
                bar.BUTTON.INFO,
            );

            expect(infoEl).toBeDefined();
            expect(rgbToHex(infoStyles.backgroundColor)).toBe(
                color.black.default,
            );
            expect(infoStyles.opacity).toBe(opacity[30]);
            expect(rgbToHex(infoHoverStyles.backgroundColor)).toBe(
                color.black.default,
            );
            expect(infoHoverStyles.opacity).toBe(opacity[50]);
        });

        test(`button 'close' hover`, async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });

            const closeStyles = await page.evaluate(
                getStyles,
                bar.BUTTON.CLOSE,
            );

            await page.hover(bar.BUTTON.CLOSE);
            await page.waitForTimeout(300);

            const closeEl = await page.$(bar.BUTTON.CLOSE);

            const closeHoverStyles = await page.evaluate(
                getStyles,
                bar.BUTTON.CLOSE,
            );

            expect(closeEl).toBeDefined();

            expect(rgbToHex(closeStyles.backgroundColor)).toBe(
                color.black.default,
            );
            expect(closeStyles.opacity).toBe(opacity[30]);

            expect(rgbToHex(closeHoverStyles.backgroundColor)).toBe(
                color.black.default,
            );
            expect(closeHoverStyles.opacity).toBe(opacity[50]);
        });

        test(`logo click`, async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });

            await page.click(bar.LOGO);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const logoEl = await page.$(bar.LOGO);

            const popupEl = await page.$(mainPopup.COMMON);
            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(logoEl).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeTruthy();
        });

        test(`logo hover`, async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });

            await page.hover(bar.LOGO);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const logoEl = await page.$(bar.LOGO);

            const popupEl = await page.$(mainPopup.COMMON);
            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(logoEl).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeTruthy();
        });

        test(`close`, async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });

            await page.click(bar.BUTTON.CLOSE);
            await page.waitForTimeout(300);

            const barEl = await page.$(bar.LOGO);

            expect(barEl).toBeNull();
        });

        test('should have proper elements', async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });

            const barEl = await page.$(bar.AUTO);
            const barLogoEl = await page.$(bar.LOGO);
            const barTextEl = await page.$(bar.TEXT);
            const buttonMoreEl = await page.$(bar.BUTTON.MORE);
            const buttonInfoEl = await page.$(bar.BUTTON.INFO);
            const buttonCloseEl = await page.$(bar.BUTTON.CLOSE);

            expect(barEl).toBeDefined();
            expect(barLogoEl).toBeDefined();
            expect(barTextEl).toBeDefined();
            expect(buttonMoreEl).toBeDefined();
            expect(buttonInfoEl).toBeDefined();
            expect(buttonCloseEl).toBeDefined();
        });
    });

    describe('found offers', () => {
        test('bar click', async () => {
            await page.goto(AUTO.FOUND_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });
            const barEl = await page.$(bar.AUTO);

            const currentPageUrl = page.url();
            const currentPage = page.target();

            await page.click(bar.AUTO);

            const newTarget = await browser.waitForTarget(
                target => target.opener() === currentPage,
            );

            const newPage = await newTarget.page();
            const newPageUrl = newPage.url();
            const parsedUrl = new URL(newPageUrl);

            expect(barEl).toBeDefined();
            expect(currentPageUrl).not.toBe(newPageUrl);
            expect(newPageUrl.includes('https://auto.ru/')).toBeTruthy();
            expect(parsedUrl.searchParams.get('utm_campaign')).toBeDefined();
            expect(parsedUrl.searchParams.get('utm_medium')).toBe('cpc');
            expect(parsedUrl.searchParams.get('utm_source')).toBe(
                'yandex_sovetnik',
            );
        });

        test('"view offers" button click', async () => {
            await page.goto(AUTO.FOUND_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });
            const barEl = await page.$(bar.AUTO);

            const currentPageUrl = page.url();
            const currentPage = page.target();

            await page.click(bar.BUTTON.VIEW);

            const newTarget = await browser.waitForTarget(
                target => target.opener() === currentPage,
            );

            const newPage = await newTarget.page();
            const newPageUrl = newPage.url();
            const parsedUrl = new URL(newPageUrl);

            expect(barEl).toBeDefined();
            expect(currentPageUrl).not.toBe(newPageUrl);
            expect(newPageUrl.includes('https://auto.ru/')).toBeTruthy();
            expect(parsedUrl.searchParams.get('utm_campaign')).toBeDefined();
            expect(parsedUrl.searchParams.get('utm_medium')).toBe('cpc');
            expect(parsedUrl.searchParams.get('utm_source')).toBe(
                'yandex_sovetnik',
            );
        });

        test('should have proper elements', async () => {
            await page.goto(AUTO.FOUND_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });

            const barEl = await page.$(bar.AUTO);
            const barLogoEl = await page.$(bar.LOGO);
            const barTextEl = await page.$(bar.TEXT);
            const buttonViewEl = await page.$(bar.BUTTON.VIEW);
            const buttonMoreEl = await page.$(bar.BUTTON.MORE);
            const buttonInfoEl = await page.$(bar.BUTTON.INFO);
            const buttonCloseEl = await page.$(bar.BUTTON.CLOSE);

            expect(barEl).toBeDefined();
            expect(barLogoEl).toBeDefined();
            expect(barTextEl).toBeDefined();
            expect(buttonMoreEl).toBeDefined();
            expect(buttonViewEl).toBeDefined();
            expect(buttonInfoEl).toBeDefined();
            expect(buttonCloseEl).toBeDefined();
        });

        test('"more offers" button click', async () => {
            await page.goto(AUTO.FOUND_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });
            const barEl = await page.$(bar.AUTO);
            const moreEl = await page.$(bar.BUTTON.MORE);

            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const popupEl = await page.$(mainPopup.COMMON);
            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(barEl).toBeDefined();
            expect(moreEl).toBeDefined();
            expect(popupEl).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeTruthy();
        });

        test('bar styles and hover', async () => {
            await page.goto(AUTO.FOUND_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });

            const barStyles = await page.evaluate(getStyles, bar.AUTO);

            await page.hover(bar.AUTO);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const barHoverStyles = await page.evaluate(getStyles, bar.AUTO);

            const viewAttrs = await page.evaluate(getTitle, bar.BUTTON.VIEW);
            const moreAttrs = await page.evaluate(getTitle, bar.BUTTON.MORE);
            const infoAttrs = await page.evaluate(getTitle, bar.BUTTON.INFO);
            const closeAttrs = await page.evaluate(getTitle, bar.BUTTON.CLOSE);

            const popupEl = await page.$(mainPopup.COMMON);
            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(rgbToHex(barStyles.color)).toBe(color.black.legacy);
            expect(barStyles.fontFamily).toBe(font.family);
            expect(barStyles.fontSize).toBe(font.size.body);
            expect(barStyles.fontWeight).toBe(font.weight.normal);
            expect(rgbToHex(barStyles.backgroundColor)).toBe(
                color.yellow.default,
            );

            expect(rgbToHex(barHoverStyles.color)).toBe(color.black.legacy);
            expect(rgbToHex(barHoverStyles.backgroundColor)).toBe(
                color.yellow.hover,
            );

            expect(viewAttrs.title).toBe('Посмотреть');
            expect(moreAttrs.title).toBe('Еще предложения');
            expect(infoAttrs.title).toBe('О программе');
            expect(closeAttrs.title).toBe('Закрыть');

            expect(popupClasses.includes(mod.VISIBLE)).toBeTruthy();
        });

        test('"more offers" button double click', async () => {
            await page.goto(AUTO.FOUND_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });
            const barEl = await page.$(bar.AUTO);
            const moreEl = await page.$(bar.BUTTON.MORE);

            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: false });

            const popupEl = await page.$(mainPopup.COMMON);
            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            expect(barEl).toBeDefined();
            expect(moreEl).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeFalsy();
        });
    });
});
