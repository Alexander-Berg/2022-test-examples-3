const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const { bar, mainPopup } = require('../../../src/utils/selector');
const { font, color } = require('../../../src/utils/styles');
const getStyles = require('../../../src/utils/get-styles');
const rgbToHex = require('../../../src/utils/rgb-to-hex');
const offerClick = require('../../../src/utils/offer-click');
const offersCheck = require('../../../src/utils/offer-check');
const { CV } = require('../../../src/utils/url');

let browser;
let page;

const PAGE_URL = CV;

describe('Product: cv popup', () => {
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

    test('category title styles', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const categoryTitle = await page.$(mainPopup.CATEGORY.TITLE, {
            visible: true,
        });

        const categoryTitleStyles = await page.evaluate(
            getStyles,
            mainPopup.CATEGORY.LINK,
        );

        await page.hover(mainPopup.CATEGORY.LINK);

        const categoryTitleStylesHover = await page.evaluate(
            getStyles,
            mainPopup.CATEGORY.LINK,
        );

        expect(categoryTitle).toBeDefined();

        expect(categoryTitleStyles.fontFamily).toBe(font.family);
        expect(categoryTitleStyles.fontSize).toBe(font.size.title);
        expect(categoryTitleStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(categoryTitleStyles.color)).toBe(color.black.legacy);

        expect(categoryTitleStylesHover.fontFamily).toBe(font.family);
        expect(categoryTitleStylesHover.fontSize).toBe(font.size.title);
        expect(categoryTitleStylesHover.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(categoryTitleStylesHover.color)).toBe(color.red.legacy);
    });

    test('category title should be clickable', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });

        const currentPageUrl = page.url();
        const currentPage = page.target();

        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        await page.click(mainPopup.CATEGORY.LINK);

        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();

        const url = new URL(newPageUrl);

        expect(newPageUrl).not.toBe(currentPageUrl);
        expect(url.protocol).toBe('https:');
        expect(url.hostname).toBe('market.yandex.ru');
        expect(url.searchParams.get('utm_source')).toBe('sovetnik');
        expect(url.searchParams.get('utm_medium')).toBe('cpc');
        expect(url.searchParams.get('utm_campaign')).toBe('clothes-category');
        expect(url.searchParams.has('hid')).toBeTruthy();
        expect(url.searchParams.has('clid')).toBeTruthy();
        expect(url.searchParams.has('distr_type')).toBeTruthy();
    });

    test('`view all offers` button should be clickable', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });

        const currentPageUrl = page.url();
        const currentPage = page.target();

        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        await page.click(mainPopup.BUTTON.MORE);

        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();

        const url = new URL(newPageUrl);

        expect(newPageUrl).not.toBe(currentPageUrl);
        expect(url.protocol).toBe('https:');
        expect(url.hostname).toBe('market.yandex.ru');
        expect(url.pathname).toBe('/search');
        expect(url.searchParams.get('utm_source')).toBe('sovetnik');
        expect(url.searchParams.get('utm_medium')).toBe('cpc');
        expect(url.searchParams.get('utm_campaign')).toBe('all-search-results');
        expect(url.searchParams.has('clid')).toBeTruthy();
        expect(url.searchParams.has('distr_type')).toBeTruthy();
        expect(url.searchParams.has('cvredirect')).toBeTruthy();
        expect(url.searchParams.has('req_id')).toBeTruthy();
    });

    test('`view all offers` button styles', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });

        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const buttonAllOffers = await page.$(mainPopup.BUTTON.MORE, {
            visible: true,
        });
        const buttonAllOffersStyles = await page.evaluate(
            getStyles,
            mainPopup.BUTTON.MORE,
        );

        await page.hover(mainPopup.BUTTON.MORE);

        const buttonAllOffersHover = await page.evaluate(
            getStyles,
            mainPopup.BUTTON.MORE,
        );

        const buttonHeight = '24px';
        const buttonWidth = '678px';

        expect(buttonAllOffers).toBeDefined();

        expect(buttonAllOffersStyles.fontFamily).toBe(font.family);
        expect(buttonAllOffersStyles.fontSize).toBe(font.size.bodySmall);
        expect(buttonAllOffersStyles.fontWeight).toBe(font.weight.normal);
        expect(buttonAllOffersStyles.height).toBe(buttonHeight);
        expect(buttonAllOffersStyles.width).toBe(buttonWidth);
        expect(rgbToHex(buttonAllOffersStyles.color)).toBe(color.black.legacy);

        expect(buttonAllOffersHover.fontFamily).toBe(font.family);
        expect(buttonAllOffersHover.fontSize).toBe(font.size.bodySmall);
        expect(buttonAllOffersHover.fontWeight).toBe(font.weight.normal);
        expect(buttonAllOffersHover.height).toBe(buttonHeight);
        expect(buttonAllOffersHover.width).toBe(buttonWidth);
        expect(rgbToHex(buttonAllOffersHover.color)).toBe(color.black.legacy);
    });

    test('click on offer', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const offers = await page.$$(mainPopup.OFFER.SEARCH);
        const isClickable = await offerClick(offers, browser, page);

        expect(isClickable).toBeTruthy();
    });

    test('all elements should be defined', async () => {
        await page.goto(PAGE_URL, {
            waitUntil: 'networkidle2',
        });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const category = await page.$(mainPopup.CATEGORY.TITLE);
        const offers = await page.$$(mainPopup.OFFER.SEARCH);

        const isCorrect = await offersCheck(offers);

        const moreButton = await page.$(mainPopup.BUTTON.MORE);
        const location = await page.$(mainPopup.FOOTER.LOCATION);
        const info = await page.$(mainPopup.FOOTER.INFO);
        const feedback = await page.$(mainPopup.FOOTER.FEEDBACK);
        const source = await page.$(mainPopup.FOOTER.SOURCE);

        expect(category).toBeDefined();
        expect(moreButton).toBeDefined();
        expect(location).toBeDefined();
        expect(info).toBeDefined();
        expect(feedback).toBeDefined();
        expect(source).toBeDefined();
        expect(isCorrect).toBeTruthy();
    });
});
