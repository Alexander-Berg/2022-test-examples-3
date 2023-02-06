/**
 * Popup "кликабельность оффера"
 */

const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const { bar, mainPopup } = require('../../../src/utils/selector');
const offerClick = require('../../../src/utils/offer-click');
const getStyles = require('../../../src/utils/get-styles');
const { color, popupHeight } = require('../../../src/utils/styles');
const offersCheck = require('../../../src/utils/offer-check');
const { SEARCH } = require('../../../src/utils/url');
const rgbToHex = require('../../../src/utils/rgb-to-hex');

const { MORE_OFFERS, LESS_OFFERS } = SEARCH;
const PAGE_URL = MORE_OFFERS;

let browser;
let page;

describe('Product: search popup', () => {
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

    test('check if image fallback is working properly', async () => {
        await page.setRequestInterception(true);
        page.on('request', interceptedRequest => {
            if (
                interceptedRequest
                    .url()
                    .startsWith('https://avatars.mds.yandex.net')
            ) {
                interceptedRequest.abort();
            } else {
                interceptedRequest.continue();
            }
        });

        await page.goto(PAGE_URL, {
            waitUntil: 'networkidle2',
        });

        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });
        await page.waitForSelector(mainPopup.OFFER.IMAGE_FALLBACK, {
            visible: true,
        });
    });

    test('check if height of popups with different offers count is equal', async () => {
        await page.goto(LESS_OFFERS, {
            waitUntil: 'networkidle2',
        });

        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });

        await page.click(bar.BUTTON.MORE);

        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const lessOffersPopupStyles = await page.evaluate(
            getStyles,
            mainPopup.COMMON,
        );
        const lessOffersPopupHeight = lessOffersPopupStyles.height;

        await page.goto(MORE_OFFERS, {
            waitUntil: 'networkidle2',
        });

        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });

        await page.click(bar.BUTTON.MORE);

        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const moreOffersPopupStyles = await page.evaluate(
            getStyles,
            mainPopup.COMMON,
        );
        const moreOffersPopupHeight = moreOffersPopupStyles.height;

        expect(lessOffersPopupHeight).toEqual(popupHeight.MAIN);
        expect(moreOffersPopupHeight).toEqual(popupHeight.MAIN);
    });

    test('hover on offer elements', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });
        await page.waitForSelector(mainPopup.OFFER.SEARCH, { visible: true });

        // проверяем цвет бордера оффера до/при ховере
        const offerDefaultStyles = await page.evaluate(
            getStyles,
            mainPopup.OFFER.SEARCH,
        );
        const offerDefaultBorder = rgbToHex(offerDefaultStyles.borderColor);

        await page.hover(mainPopup.OFFER.SEARCH);

        const offerHoverStyles = await page.evaluate(
            getStyles,
            mainPopup.OFFER.SEARCH,
        );
        const offerHoverBorder = rgbToHex(offerHoverStyles.borderColor);

        expect(offerDefaultBorder).toEqual(color.black.default);
        expect(offerHoverBorder).toEqual(color.yellow.searchOfferHover);

        // проверяем цвет текста заголовка до/при ховере на заголовок
        const offerTitleDefaultStyles = await page.evaluate(
            getStyles,
            mainPopup.OFFER.TITLE,
        );
        const offerTitleDefaultColor = rgbToHex(offerTitleDefaultStyles.color);

        await page.hover(mainPopup.OFFER.TITLE);

        const offerTitleHoverStyles = await page.evaluate(
            getStyles,
            mainPopup.OFFER.TITLE,
        );
        const offerTitleHoverColor = rgbToHex(offerTitleHoverStyles.color);

        expect(offerTitleDefaultColor).toEqual(color.black.coal);
        expect(offerTitleHoverColor).toEqual(color.red.legacy);

        // проверяем цвет текста цены до/при ховере на текст
        const offerPriceDefaultStyles = await page.evaluate(
            getStyles,
            mainPopup.OFFER.PRICE,
        );
        const offerPriceDefaultColor = rgbToHex(offerPriceDefaultStyles.color);

        await page.hover(mainPopup.OFFER.PRICE);

        const offerPriceHoverStyles = await page.evaluate(
            getStyles,
            mainPopup.OFFER.PRICE,
        );
        const offerPriceHoverColor = rgbToHex(offerPriceHoverStyles.color);

        expect(offerPriceDefaultColor).toEqual(color.black.coal);
        expect(offerPriceHoverColor).toEqual(color.red.legacy);
    });

    test('click on offer', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
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
        expect(url.searchParams.get('utm_campaign')).toBe(
            'search_category_title',
        );
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
        expect(url.searchParams.get('utm_source')).toBe('sovetnik');
        expect(url.searchParams.get('utm_medium')).toBe('cpc');
        expect(url.searchParams.get('utm_campaign')).toBe('all-search-results');
        expect(url.searchParams.has('clid')).toBeTruthy();
        expect(url.searchParams.has('distr_type')).toBeTruthy();
        expect(url.searchParams.has('cvredirect')).toBeTruthy();
        expect(url.searchParams.has('req_id')).toBeTruthy();
    });
});
