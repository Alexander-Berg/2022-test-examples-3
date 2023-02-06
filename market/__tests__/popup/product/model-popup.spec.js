/**
 * Popup "кнопка все цены"
 */

const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const { bar, mainPopup, mod } = require('../../../src/utils/selector');
const getStyles = require('../../../src/utils/get-styles');
const { getInnerText } = require('../../../src/utils/get-text');
const { PRICE, MODEL } = require('../../../src/utils/url');
const { popupHeight } = require('../../../src/utils/styles');
const {
    mainPopupText: { product: { opinions = {}, offers = {} } = {} } = {},
} = require('../../../src/utils/text-constants');
const offerClick = require('../../../src/utils/offer-click');
const getClassList = require('../../../src/utils/get-class-list');

let browser;
let page;

async function offerClickModified(shopOffers) {
    const currentPageUrl = page.url();
    const currentPage = page.target();

    /* eslint-disable no-restricted-syntax */
    /* eslint-disable no-await-in-loop */
    for (const offer of shopOffers) {
        await offer.click();

        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();

        if (
            newPageUrl === currentPageUrl ||
            newPageUrl.includes('about:blank')
        ) {
            return false;
        }

        let pages = await browser.pages();
        let lastPage = pages[pages.length - 1];
        await lastPage.close();

        const popup = await page.$(mainPopup.OFFER.DELIVERY_POPUP);
        if (popup) {
            await page.click(mainPopup.OFFER.SHOP);
            await browser.waitForTarget(
                target => target.opener() === currentPage,
            );

            pages = await browser.pages();
            lastPage = pages[pages.length - 1];
            await lastPage.close();
        }
    }
    return true;
}

const { AFFORDABLE } = PRICE;

describe('Model popup', () => {
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

    test('delivery-sis test: sis is shown, tabs, map link click', async () => {
        await page.goto(PRICE.MODEL.MORE_OFFERS, {
            waitUntil: 'networkidle2',
        });

        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        await page.hover(mainPopup.DELIVERY_SIS.TRIGGER);

        await page.waitForSelector(mainPopup.DELIVERY_SIS.POPUP, {
            visible: true,
        });

        await page.waitForSelector(mainPopup.DELIVERY_SIS.TABS.CONTAINER, {
            visible: true,
        });

        await page.waitForSelector(mainPopup.DELIVERY_SIS.TABS.DELIVERY, {
            visible: true,
        });

        await page.waitForSelector(mainPopup.DELIVERY_SIS.TABS.PICKUP, {
            visible: true,
        });

        await page.waitForSelector(mainPopup.DELIVERY_SIS.TABS.SHOPS, {
            visible: true,
        });

        const deliveryTabClasses = await page.evaluate(
            getClassList,
            mainPopup.DELIVERY_SIS.TABS.DELIVERY,
        );
        const isDeliveryActive = Object.values(deliveryTabClasses).includes(
            mod.SIS_TAB_ACTIVE,
        );
        expect(isDeliveryActive).toBeTruthy();

        await page.click(mainPopup.DELIVERY_SIS.TABS.PICKUP);
        const pickupTabClasses = await page.evaluate(
            getClassList,
            mainPopup.DELIVERY_SIS.TABS.PICKUP,
        );
        const isPickupActive = Object.values(pickupTabClasses).includes(
            mod.SIS_TAB_ACTIVE,
        );
        expect(isPickupActive).toBeTruthy();

        await page.click(mainPopup.DELIVERY_SIS.TABS.SHOPS);
        const shopsTabClasses = await page.evaluate(
            getClassList,
            mainPopup.DELIVERY_SIS.TABS.SHOPS,
        );
        const isShopsActive = Object.values(shopsTabClasses).includes(
            mod.SIS_TAB_ACTIVE,
        );
        expect(isShopsActive).toBeTruthy();

        await page.waitForSelector(mainPopup.DELIVERY_SIS.MAP_BUTTON, {
            visible: true,
        });

        const currentPageUrl = page.url();
        const currentPage = page.target();
        await page.click(mainPopup.DELIVERY_SIS.MAP_BUTTON);

        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();
        const url = new URL(newPageUrl);

        expect(newPageUrl).not.toBe(currentPageUrl);
        expect(url.protocol).toBe('https:');
        expect(url.hostname).toBe('market.yandex.ru');
        expect(url.pathname).toBe('/geo');

        expect(url.searchParams.has('fesh')).toBeTruthy();
        expect(url.searchParams.has('offerid')).toBeTruthy();
        expect(url.searchParams.get('offer-shipping')).toBe('pickup');
    });

    test('check if height of popups with different offers count is equal', async () => {
        await page.goto(PRICE.MODEL.LESS_OFFERS, {
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

        await page.goto(PRICE.MODEL.MORE_OFFERS, {
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

        await page.goto(PRICE.MODEL.LESS_OFFERS, {
            waitUntil: 'networkidle2',
        });

        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });
        await page.waitForSelector(mainPopup.MODEL_INFO.IMAGE_FALLBACK, {
            visible: true,
        });
    });

    test('check height on opinions page', async () => {
        await page.goto(PRICE.MODEL.MORE_OFFERS, {
            waitUntil: 'networkidle2',
        });

        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });
        await page.waitForSelector(mainPopup.OPINIONS.TAB, { visible: true });

        const mainScreenStyles = await page.evaluate(
            getStyles,
            mainPopup.COMMON,
        );
        const mainScreenHeight = mainScreenStyles.height;

        await page.click(mainPopup.OPINIONS.TAB);
        await page.waitForSelector(mainPopup.MODEL_LINKS.PRICE.AVERAGE, {
            hidden: true,
        });

        const opinionsScreenStyles = await page.evaluate(
            getStyles,
            mainPopup.COMMON,
        );
        const opinionsScreenHeight = opinionsScreenStyles.height;

        expect(mainScreenHeight).toEqual(popupHeight.MAIN);
        expect(opinionsScreenHeight).toEqual(popupHeight.MAIN);
    });

    test('check opinion info', async () => {
        await page.goto(PRICE.MODEL.MORE_OFFERS, {
            waitUntil: 'networkidle2',
        });

        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });
        await page.waitForSelector(mainPopup.OPINIONS.TAB, { visible: true });
        await page.click(mainPopup.OPINIONS.TAB);

        await page.waitForSelector(mainPopup.OPINIONS.LIST, { visible: true });
        await page.waitForSelector(mainPopup.OPINION.GRADE, { visible: true });

        await page.waitForSelector(mainPopup.OPINION.RATING, { visible: true });
        await page.waitForSelector(mainPopup.OPINIONS.MORE_BLOCK, {
            visible: true,
        });

        const moreInnerText = await page.evaluate(
            getInnerText,
            mainPopup.OPINIONS.MORE_BLOCK,
        );
        const [moreText, count] = moreInnerText.split(' (');
        const moreValue = parseInt(count, 10);

        expect(moreValue).toBeDefined();
        expect(moreValue).not.toBeNaN();
        expect(moreText).toBeDefined();
        expect(moreText).not.toBeNull();
        expect(moreText).toEqual(
            expect.stringMatching(opinions.allOpinions.regex),
        );
    });

    test('check click of more opinions button', async () => {
        await page.goto(PRICE.MODEL.MORE_OFFERS, {
            waitUntil: 'networkidle2',
        });

        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });
        await page.waitForSelector(mainPopup.OPINIONS.TAB, { visible: true });
        await page.click(mainPopup.OPINIONS.TAB);

        await page.waitForSelector(mainPopup.OPINIONS.LIST, { visible: true });
        await page.waitForSelector(mainPopup.OPINIONS.MORE_BUTTON, {
            visible: true,
        });

        const currentPageUrl = page.url();
        const currentPage = page.target();

        await page.click(mainPopup.OPINIONS.MORE_BUTTON);
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
            'opinions-tab-all-button',
        );

        expect(url.searchParams.has('pp')).toBeTruthy();
        expect(url.searchParams.has('clid')).toBeTruthy();
        expect(url.searchParams.has('distr_type')).toBeTruthy();
        expect(url.searchParams.has('hid')).toBeTruthy();
        expect(url.searchParams.has('req_id')).toBeTruthy();
    });

    // скипаем тест, потому что ссылка для воспроизведения протухает крайне быстро
    test.skip('check if there is a message in popup with 0 offers in list', async () => {
        await page.goto(PRICE.MODEL.NO_OFFERS, {
            waitUntil: 'networkidle2',
        });

        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });
        await page.waitForSelector(mainPopup.MESSAGE, { visible: true });

        const messageText = await page.evaluate(
            getInnerText,
            mainPopup.MESSAGE,
        );

        expect(messageText).toBeDefined();
        expect(messageText).not.toBeNull();
        expect(messageText).toEqual(
            expect.stringMatching(offers.noOffers.regex),
        );
    });

    test('all prices link: should open new browser tab', async () => {
        await page.goto(AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const currentPageUrl = page.url();
        const currentPage = page.target();
        await page.click(mainPopup.MODEL_LINKS.PRICE.ALL);
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
        expect(url.searchParams.get('utm_campaign')).toBe('all-offers');

        expect(url.searchParams.has('pp')).toBeTruthy();
        expect(url.searchParams.has('clid')).toBeTruthy();
        expect(url.searchParams.has('distr_type')).toBeTruthy();
        expect(url.searchParams.has('hid')).toBeTruthy();
        expect(url.searchParams.has('req_id')).toBeTruthy();
    });

    test('average price link: should open new browser tab', async () => {
        await page.goto(AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const currentPageUrl = page.url();
        const currentPage = page.target();
        await page.click(mainPopup.MODEL_LINKS.PRICE.AVERAGE);
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
            'footer-average-price',
        );

        expect(url.searchParams.has('pp')).toBeTruthy();
        expect(url.searchParams.has('clid')).toBeTruthy();
        expect(url.searchParams.has('distr_type')).toBeTruthy();
        expect(url.searchParams.has('hid')).toBeTruthy();
        expect(url.searchParams.has('req_id')).toBeTruthy();
    });

    test('map link: should open new browser tab', async () => {
        await page.goto(AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const currentPageUrl = page.url();
        const currentPage = page.target();
        await page.click(mainPopup.MODEL_LINKS.MAP);
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
        expect(url.searchParams.get('utm_campaign')).toBe('offers-on-map');

        expect(url.searchParams.has('pp')).toBeTruthy();
        expect(url.searchParams.has('clid')).toBeTruthy();
        expect(url.searchParams.has('distr_type')).toBeTruthy();
        expect(url.searchParams.has('hid')).toBeTruthy();
        expect(url.searchParams.has('req_id')).toBeTruthy();
    });

    test('should open popup specs tab', async () => {
        await page.goto(AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const popupBeforeStyles = await page.evaluate(
            getStyles,
            mainPopup.COMMON,
        );

        await page.click(mainPopup.TABS.SPECS);
        await page.waitForSelector(mainPopup.SPECS.SECTION, { visible: true });

        const popupModelLinksStyles = await page.evaluate(
            getStyles,
            mainPopup.MODEL_LINKS.BLOCK,
        );
        const specsTab = await page.$(mainPopup.TABS.SPECS);
        const popupAfterStyles = await page.evaluate(
            getStyles,
            mainPopup.COMMON,
        );

        expect(popupAfterStyles.height).toBe(popupBeforeStyles.height);
        expect(popupModelLinksStyles.display).toBe('none');
        expect(specsTab).toBeDefined();
    });

    test('elements: should be defined', async () => {
        const PAGE_URL = MODEL.PREMIUM;

        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const headerOffer = await page.$(mainPopup.MODEL_INFO.PREMIUM);
        const headerOfferPhoto = await page.$(mainPopup.MODEL_INFO.PHOTO);
        const headerOfferInfo = await page.$(mainPopup.MODEL_INFO.INFO);
        const headerOfferPrice = await page.$(mainPopup.MODEL_INFO.PRICE);

        const specification = await page.$(mainPopup.TABS.BLOCK);
        const offersList = await page.$(mainPopup.TABS.OFFERS);
        const opinionsList = await page.$(mainPopup.TABS.OPINIONS);
        const specs = await page.$(mainPopup.TABS.SPECS);

        const additionalBlock = await page.$(mainPopup.OFFERS.LIST);
        const additionalOffers = await page.$(mainPopup.OFFER.EL);

        const modelLinks = await page.$(mainPopup.MODEL_LINKS.BLOCK);
        const modelLinksMap = await page.$(mainPopup.MODEL_LINKS.MAP);
        const modelLinksPriceAll = await page.$(
            mainPopup.MODEL_LINKS.PRICE.ALL,
        );
        const modelLinksPriceAverage = await page.$(
            mainPopup.MODEL_LINKS.PRICE.AVERAGE,
        );

        expect(headerOffer).toBeDefined();
        expect(headerOfferPhoto).toBeDefined();
        expect(headerOfferInfo).toBeDefined();
        expect(headerOfferPrice).toBeDefined();

        expect(specification).toBeDefined();
        expect(offersList).toBeDefined();
        expect(opinionsList).toBeDefined();
        expect(specs).toBeDefined();

        expect(additionalBlock).toBeDefined();
        expect(additionalOffers).toBeDefined();

        expect(modelLinks).toBeDefined();
        expect(modelLinksMap).toBeDefined();
        expect(modelLinksPriceAll).toBeDefined();
        expect(modelLinksPriceAverage).toBeDefined();
    });

    test('offers: should be less than 31', async () => {
        const PAGE_URL = MODEL.PREMIUM;

        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const shopOffers = await page.$$(mainPopup.BUTTON.OFFER.NON_PREMIUM);
        const offersCount = shopOffers.length + 1;

        expect(offersCount).toBeLessThan(31);
    });

    test('pharmacy offers: should be less than 6', async () => {
        const PAGE_URL = MODEL.MEDICINE;

        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const shopOffers = await page.$$(mainPopup.OFFER.PHARMACY);
        const offersCount = shopOffers.length + 1;

        expect(offersCount).toBeLessThan(6);
    });

    test('premium offer: should open new browser tab', async () => {
        const PAGE_URL = MODEL.PREMIUM;

        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.MODEL_INFO.PREMIUM, {
            visible: true,
        });

        const currentPageUrl = page.url();
        const currentPage = page.target();
        await page.click(mainPopup.MODEL_INFO.PREMIUM);
        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();

        expect(newPageUrl).not.toBe(currentPageUrl);
        expect(newPageUrl).not.toBe('about:blank');
    });

    test('premium offer button: should open new browser tab', async () => {
        const PAGE_URL = MODEL.PREMIUM;

        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.BUTTON.OFFER.PREMIUM, {
            visible: true,
        });

        const currentPageUrl = page.url();
        const currentPage = page.target();
        await page.click(mainPopup.BUTTON.OFFER.PREMIUM);
        const newTarget = await browser.waitForTarget(
            target => target.opener() === currentPage,
        );

        const newPage = await newTarget.page();
        const newPageUrl = newPage.url();

        expect(newPageUrl).not.toBe(currentPageUrl);
        expect(newPageUrl).not.toBe('about:blank');
    });

    test('non-premium offer: should open new browser tab', async () => {
        const PAGE_URL = MODEL.NON_PREMIUM;

        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const shopOffers = await page.$$(mainPopup.OFFER.EL);
        const isClickable = await offerClickModified(shopOffers, browser, page);
        expect(isClickable).toBeTruthy();
    });

    test('non-premium offer button: should open new browser tab', async () => {
        const PAGE_URL = MODEL.NON_PREMIUM;

        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(mainPopup.COMMON, { visible: true });

        const shopOffers = await page.$$(mainPopup.BUTTON.OFFER.NON_PREMIUM);
        const isClickable = await offerClick(shopOffers, browser, page);
        expect(isClickable).toBeTruthy();
    });
});
