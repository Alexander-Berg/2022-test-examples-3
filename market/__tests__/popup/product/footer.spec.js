const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const {
    bar,
    mainPopup,
    footer,
    feedbackPopup,
    mod,
} = require('../../../src/utils/selector');
const { PRICE } = require('../../../src/utils/url');

let browser;
let page;

describe('Product: footer elements', () => {
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

    test('location click', async () => {
        await page.goto(PRICE.AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        const currentPageUrl = page.url();
        const currentPage = page.target();

        await page.click(bar.BUTTON.MORE);

        const popupEl = await page.$(mainPopup.COMMON, { visible: true });

        await page.click(footer.LOCATION);

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
        expect(parsedUrl.protocol).toBe('https:');
        expect(parsedUrl.hostname).toBe('sovetnik.market.yandex.ru');
        expect(parsedUrl.pathname).toBe('/app/settings/');
        expect(parsedUrl.search).toHaveLength(0);
    });

    test('sellers info click', async () => {
        await page.goto(PRICE.AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        const currentPageUrl = page.url();
        const currentPage = page.target();

        await page.click(bar.BUTTON.MORE);

        const popupEl = await page.$(mainPopup.COMMON, { visible: true });

        await page.click(footer.INFO);

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
        expect(parsedUrl.protocol).toBe('https:');
        expect(parsedUrl.hostname).toBe('market.yandex.ru');
        expect(parsedUrl.searchParams.get('clid')).toBeDefined();
        expect(parsedUrl.searchParams.get('distr_type')).toBeDefined();
        expect(parsedUrl.searchParams.get('req_id')).toBeDefined();
        expect(parsedUrl.searchParams.get('shopIds')).toBeDefined();
        expect(parsedUrl.searchParams.get('utm_campaign')).toBe('shops-info');
        expect(parsedUrl.searchParams.get('utm_medium')).toBe('cpc');
        expect(parsedUrl.searchParams.get('utm_source')).toBe('sovetnik');
    });

    test('market data click', async () => {
        await page.goto(PRICE.AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        const currentPageUrl = page.url();
        const currentPage = page.target();

        await page.click(bar.BUTTON.MORE);

        const popupEl = await page.$(mainPopup.COMMON, { visible: true });

        await page.click(footer.MARKET_DATA);

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
        expect(parsedUrl.protocol).toBe('https:');
        expect(parsedUrl.hostname).toBe('market.yandex.ru');
        expect(parsedUrl.searchParams.get('clid')).toBeDefined();
        expect(parsedUrl.searchParams.get('distr_type')).toBeDefined();
        expect(parsedUrl.searchParams.get('req_id')).toBeDefined();
        expect(parsedUrl.searchParams.get('utm_campaign')).toBe('market-data');
        expect(parsedUrl.searchParams.get('utm_medium')).toBe('cpc');
        expect(parsedUrl.searchParams.get('utm_source')).toBe('sovetnik');
    });

    test('feedback click', async () => {
        const requestsData = [];
        await page.setRequestInterception(true);
        page.on('request', interceptedRequest => {
            if (interceptedRequest.url().endsWith('client')) {
                requestsData.push(interceptedRequest.postData());
                interceptedRequest.continue();
            } else {
                interceptedRequest.continue();
            }
        });
        await page.goto(PRICE.AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        await page.click(bar.BUTTON.MORE);

        const mainPopupEl = await page.$(mainPopup.COMMON, { visible: true });

        await page.click(footer.TRACK_ERROR);

        const title = await page.$(feedbackPopup.TITLE, { visible: true });
        const textArea = await page.$(feedbackPopup.TEXT_AREA, {
            visible: true,
        });
        const list = await page.$(feedbackPopup.LIST, { visible: true });
        const popupCloseBtn = await page.$(feedbackPopup.CLOSE, {
            visible: true,
        });
        const closeBtn = await page.$(feedbackPopup.BUTTON.CLOSE, {
            visible: true,
        });
        const sendBtn = await page.$(feedbackPopup.BUTTON.SEND, {
            visible: true,
        });

        const popupEl = await page.$(feedbackPopup.COMMON);
        const popupClassProp = await popupEl.getProperty('className');
        const popupClassesRaw = await popupClassProp.jsonValue();
        const popupClasses = popupClassesRaw.split(' ');

        const lastRequest = requestsData.pop();

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
        expect(mainPopupEl).toBeDefined();
        expect(popupEl).toBeDefined();
        expect(popupClasses.includes(mod.FEEDBACK_VISIBLE)).toBeTruthy();
        expect(title).toBeDefined();
        expect(textArea).toBeDefined();
        expect(list).toBeDefined();
        expect(popupCloseBtn).toBeDefined();
        expect(closeBtn).toBeDefined();
        expect(sendBtn).toBeDefined();

        expect(JSON.parse(lastRequest)).toEqual(
            expect.objectContaining({
                transaction_id: expect.any(String),
                interaction: 'wrong_product',
                type_view: 'desktop',
                settings: expect.objectContaining({
                    applicationName: 'Яндекс.Советник',
                    affId: expect.any(Number),
                    clid: expect.any(Number),
                    sovetnikExtension: expect.any(Boolean),
                    withButton: expect.any(Boolean),
                    extensionStorage: expect.any(Boolean),
                    notificationStatus: expect.any(String),
                    notificationPermissionGranted: true,
                    installId: expect.any(String),
                    installTime: expect.any(Number),
                }),
                v: expect.any(String),
                url: expect.any(String),
            }),
        );
    });
});
