const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const { bar, mainPopup, footer } = require('../../../src/utils/selector');
const { popupHeight } = require('../../../src/utils/styles');
const offerClick = require('../../../src/utils/offer-click');
const getStyles = require('../../../src/utils/get-styles');
const { AUTO } = require('../../../src/utils/url');

let browser;
let page;

describe('Auto popup', () => {
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

    test('click on auto.ru link in footer', async () => {
        await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.AUTO, { visible: true });
        const barEl = await page.$(bar.AUTO);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        const currentPageUrl = page.url();
        const currentPage = page.target();

        await page.click(bar.BUTTON.MORE);

        const popupEl = await page.$(mainPopup.COMMON, { visible: true });

        await page.click(footer.LINK_AUTO);

        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();

        const parsedUrl = new URL(newPageUrl);

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
        expect(popupEl).toBeDefined();
        expect(currentPageUrl).not.toBe(newPageUrl);
        expect(newPageUrl.includes('https://auto.ru/')).toBeTruthy();
        expect(parsedUrl.searchParams.get('utm_campaign')).toBe('about');
        expect(parsedUrl.searchParams.get('utm_medium')).toBe('cpc');
        expect(parsedUrl.searchParams.get('utm_source')).toBe(
            'yandex_sovetnik',
        );
    });

    describe('found offers', () => {
        test('should have a proper popup height', async () => {
            await page.goto(AUTO.FOUND_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });
            const barEl = await page.$(bar.AUTO);
            const barMoreEl = await page.$(bar.BUTTON.MORE);

            await page.click(bar.BUTTON.MORE);

            const popupEl = await page.$(mainPopup.COMMON, { visible: true });
            const popupStyles = await page.evaluate(
                getStyles,
                mainPopup.COMMON,
            );

            expect(barEl).toBeDefined();
            expect(barMoreEl).toBeDefined();
            expect(popupEl).toBeDefined();
            expect(popupStyles.height).toBe(popupHeight.MAIN);
        });

        test('click on offer', async () => {
            await page.goto(AUTO.FOUND_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });
            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const offers = await page.$$(mainPopup.OFFER.AUTO);
            const isClickable = await offerClick(offers, browser, page);
            expect(isClickable).toBeTruthy();
        });
    });

    describe('more offers', () => {
        test('should have a proper popup height', async () => {
            await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });
            const barEl = await page.$(bar.AUTO);
            const barMoreEl = await page.$(bar.BUTTON.MORE);

            await page.click(bar.BUTTON.MORE);

            const popupEl = await page.$(mainPopup.COMMON, { visible: true });
            const popupStyles = await page.evaluate(
                getStyles,
                mainPopup.COMMON,
            );

            expect(barEl).toBeDefined();
            expect(barMoreEl).toBeDefined();
            expect(popupEl).toBeDefined();
            expect(popupStyles.height).toBe(popupHeight.MAIN);
        });

        test('click on offer', async () => {
            await page.goto(AUTO.FOUND_OFFERS, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });
            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const offers = await page.$$(mainPopup.OFFER.AUTO);
            const isClickable = await offerClick(offers, browser, page);

            expect(isClickable).toBeTruthy();
        });
    });
});
