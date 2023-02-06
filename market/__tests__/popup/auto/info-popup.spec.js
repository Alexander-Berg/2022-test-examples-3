/* eslint-disable no-await-in-loop */
const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const { bar, infoPopup, mod } = require('../../../src/utils/selector');
const { AUTO } = require('../../../src/utils/url');
const { color, font } = require('../../../src/utils/styles');
const getStyles = require('../../../src/utils/get-styles');
const rgbToHex = require('../../../src/utils/rgb-to-hex');

let browser;
let page;

describe('Auto: info popup', () => {
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

    const testCases = [
        {
            title: 'more offers',
            link: AUTO.MORE_OFFERS,
        },
        {
            title: 'found offers',
            link: AUTO.FOUND_OFFERS,
        },
    ];

    testCases.forEach(({ title, link }) => {
        test(`${title}: show and hide popup by click on info button`, async () => {
            await page.goto(link, { waitUntil: 'networkidle2' });

            await page.waitForSelector(bar.AUTO, { visible: true });
            await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

            const infoButton = await page.$(bar.BUTTON.INFO);
            await page.click(bar.BUTTON.INFO);
            await page.waitForSelector(infoPopup.AUTO, { visible: true });

            const infoPopupElVisible = await page.$(infoPopup.AUTO, {
                visible: true,
            });

            const popupEl = await page.$(infoPopup.AUTO);
            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            await page.click(bar.BUTTON.INFO);

            const infoPopupElHidden = await page.$(infoPopup.AUTO, {
                hidden: true,
            });

            const popupElHidden = await page.$(infoPopup.AUTO);
            const popupHiddenClassProp = await popupElHidden.getProperty(
                'className',
            );
            const popupClassesRawHidden = await popupHiddenClassProp.jsonValue();
            const popupClassesHidden = popupClassesRawHidden.split(' ');

            expect(infoButton).toBeDefined();
            expect(infoPopupElVisible).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeTruthy();
            expect(infoPopupElHidden).toBeDefined();
            expect(popupClassesHidden.includes(mod.VISIBLE)).toBeFalsy();
        });
    });

    testCases.forEach(({ title, link }) => {
        test(`${title}: hide popup by click on close button`, async () => {
            await page.goto(link, { waitUntil: 'networkidle2' });

            await page.waitForSelector(bar.AUTO, { visible: true });
            await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

            const infoButton = await page.$(bar.BUTTON.INFO);
            await page.click(bar.BUTTON.INFO);
            await page.waitForSelector(infoPopup.AUTO, { visible: true });

            const infoPopupElVisible = await page.$(infoPopup.AUTO, {
                visible: true,
            });

            const popupEl = await page.$(infoPopup.AUTO);
            const popupClassProp = await popupEl.getProperty('className');
            const popupClassesRaw = await popupClassProp.jsonValue();
            const popupClasses = popupClassesRaw.split(' ');

            await page.click(infoPopup.BUTTON.CLOSE_AUTO);

            const infoPopupElHidden = await page.$(infoPopup.AUTO, {
                hidden: true,
            });

            const popupElHidden = await page.$(infoPopup.AUTO);
            const popupHiddenClassProp = await popupElHidden.getProperty(
                'className',
            );
            const popupClassesRawHidden = await popupHiddenClassProp.jsonValue();
            const popupClassesHidden = popupClassesRawHidden.split(' ');

            expect(infoButton).toBeDefined();
            expect(infoPopupElVisible).toBeDefined();
            expect(popupClasses.includes(mod.VISIBLE)).toBeTruthy();
            expect(infoPopupElHidden).toBeDefined();
            expect(popupClassesHidden.includes(mod.VISIBLE)).toBeFalsy();
        });
    });

    test('features button', async () => {
        await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

        const infoButton = await page.$(bar.BUTTON.INFO);

        await page.click(bar.BUTTON.INFO);
        await page.waitForSelector(infoPopup.AUTO, { visible: true });

        await page.waitForSelector(infoPopup.BUTTON.FEATURES, {
            visible: true,
        });

        const currentPageUrl = page.url();
        const currentPage = page.target();

        await page.click(infoPopup.BUTTON.FEATURES);

        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();
        const url = new URL(newPageUrl);

        expect(infoButton).toBeDefined();
        expect(newPageUrl).not.toBe(currentPageUrl);
        expect(url.protocol).toBe('https:');
        expect(url.hostname).toBe('sovetnik.yandex.ru');
        expect(url.pathname).toBe('/info/');
    });

    test('help link', async () => {
        await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

        const infoButton = await page.$(bar.BUTTON.INFO);

        await page.click(bar.BUTTON.INFO);
        await page.waitForSelector(infoPopup.AUTO, { visible: true });

        await page.waitForSelector(infoPopup.LINK.HELP, {
            visible: true,
        });

        const currentPageUrl = page.url();
        const currentPage = page.target();

        await page.click(infoPopup.LINK.HELP);

        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();
        const url = new URL(newPageUrl);

        expect(infoButton).toBeDefined();
        expect(newPageUrl).not.toBe(currentPageUrl);
        expect(url.protocol).toBe('https:');
        expect(url.hostname).toBe('yandex.ru');
        expect(url.pathname).toBe(
            '/support/market/personal-services/sovetnik.html',
        );
    });

    test('feedback link', async () => {
        await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

        const infoButton = await page.$(bar.BUTTON.INFO);

        await page.click(bar.BUTTON.INFO);
        await page.waitForSelector(infoPopup.AUTO, { visible: true });

        await page.waitForSelector(infoPopup.LINK.FEEDBACK, {
            visible: true,
        });

        const currentPageUrl = page.url();
        const currentPage = page.target();

        await page.click(infoPopup.LINK.FEEDBACK);

        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();
        const url = new URL(newPageUrl);

        expect(infoButton).toBeDefined();
        expect(newPageUrl).not.toBe(currentPageUrl);
        expect(url.protocol).toBe('https:');
        expect(url.hostname).toBe('www.yandex.ru');
        expect(url.pathname).toBe(
            '/support/market/personal-services/sovetnik.html',
        );
        expect(url.hash).toBe('#faq__other-problem');
    });

    test('turn off link', async () => {
        await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

        const infoButton = await page.$(bar.BUTTON.INFO);

        await page.click(bar.BUTTON.INFO);
        await page.waitForSelector(infoPopup.AUTO, { visible: true });

        await page.waitForSelector(infoPopup.LINK.TURN_OFF, {
            visible: true,
        });

        const currentPageUrl = page.url();
        const currentPage = page.target();

        await page.click(infoPopup.LINK.TURN_OFF);

        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();
        const url = new URL(newPageUrl);

        expect(infoButton).toBeDefined();
        expect(newPageUrl).not.toBe(currentPageUrl);
        expect(url.protocol).toBe('https:');
        expect(url.hostname).toBe('yandex.ru');
        expect(url.pathname).toBe(
            '/support/market/personal-services/sovetnik.html',
        );
        expect(url.hash).toBe('#settings');
    });

    test('license link', async () => {
        await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

        const infoButton = await page.$(bar.BUTTON.INFO);

        await page.click(bar.BUTTON.INFO);
        await page.waitForSelector(infoPopup.AUTO, { visible: true });

        await page.waitForSelector(infoPopup.LINK.LICENSE, {
            visible: true,
        });

        const currentPageUrl = page.url();
        const currentPage = page.target();

        await page.click(infoPopup.LINK.LICENSE);

        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();
        const url = new URL(newPageUrl);

        expect(infoButton).toBeDefined();
        expect(newPageUrl).not.toBe(currentPageUrl);
        expect(url.protocol).toBe('https:');
        expect(url.hostname).toBe('www.yandex.ru');
        expect(url.pathname).toBe('/legal/advisor_agreement/');
    });

    describe('hover', () => {
        test('links', async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

            await page.click(bar.BUTTON.INFO);
            await page.waitForSelector(infoPopup.AUTO, { visible: true });

            const links = [
                infoPopup.LINK.HELP,
                infoPopup.LINK.FEEDBACK,
                infoPopup.LINK.TURN_OFF,
            ];

            // eslint-disable-next-line no-restricted-syntax
            for (const link of links) {
                const linkStyles = await page.evaluate(getStyles, link);
                await page.hover(link);
                const hoveredLinkStyles = await page.evaluate(getStyles, link);

                expect(rgbToHex(linkStyles.color)).toBe(color.purple.legacy);
                expect(linkStyles.fontFamily).toBe(font.family);
                expect(linkStyles.fontSize).toBe(font.size.bodyBig);
                expect(linkStyles.fontWeight).toBe(font.weight.normal);

                expect(rgbToHex(hoveredLinkStyles.color)).toBe(
                    color.red.legacy,
                );
                expect(hoveredLinkStyles.fontFamily).toBe(font.family);
                expect(hoveredLinkStyles.fontSize).toBe(font.size.bodyBig);
                expect(hoveredLinkStyles.fontWeight).toBe(font.weight.normal);
            }
        });

        test('license link', async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

            await page.click(bar.BUTTON.INFO);
            await page.waitForSelector(infoPopup.AUTO, { visible: true });

            const linkStyles = await page.evaluate(
                getStyles,
                infoPopup.LINK.LICENSE,
            );

            await page.hover(infoPopup.LINK.LICENSE);

            const hoveredLinkStyles = await page.evaluate(
                getStyles,
                infoPopup.LINK.LICENSE,
            );

            expect(rgbToHex(linkStyles.color)).toBe(color.purple.legacy);
            expect(linkStyles.fontFamily).toBe(font.family);
            expect(linkStyles.fontSize).toBe(font.size.bodySmall);
            expect(linkStyles.fontWeight).toBe(font.weight.normal);

            expect(rgbToHex(hoveredLinkStyles.color)).toBe(color.red.legacy);
            expect(hoveredLinkStyles.fontFamily).toBe(font.family);
            expect(hoveredLinkStyles.fontSize).toBe(font.size.bodySmall);
            expect(hoveredLinkStyles.fontWeight).toBe(font.weight.normal);
        });

        test('features button', async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

            await page.click(bar.BUTTON.INFO);
            await page.waitForSelector(infoPopup.AUTO, { visible: true });

            const linkStyles = await page.evaluate(
                getStyles,
                infoPopup.BUTTON.FEATURES,
            );

            await page.hover(infoPopup.BUTTON.FEATURES);

            const hoveredLinkStyles = await page.evaluate(
                getStyles,
                infoPopup.BUTTON.FEATURES,
            );

            expect(rgbToHex(linkStyles.color)).toBe(color.black.legacy);
            expect(linkStyles.fontFamily).toBe(font.family);
            expect(linkStyles.fontSize).toBe(font.size.body);
            expect(linkStyles.fontWeight).toBe(font.weight.normal);
            expect(rgbToHex(linkStyles.backgroundColor)).toBe(
                color.yellow.legacy,
            );

            expect(rgbToHex(hoveredLinkStyles.color)).toBe(color.black.legacy);
            expect(hoveredLinkStyles.fontFamily).toBe(font.family);
            expect(hoveredLinkStyles.fontSize).toBe(font.size.body);
            expect(hoveredLinkStyles.fontWeight).toBe(font.weight.normal);
            expect(rgbToHex(linkStyles.backgroundColor)).toBe(
                color.yellow.legacy,
            );
        });
    });
});
