/**
 * Pricebar "Отображение прайсбара при разной ширине"
 */

const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar } = require('../../src/utils/selector');
const { PRICE, CV } = require('../../src/utils/url');

let browser;
let page;

describe('Pricebar: different width', () => {
    beforeAll(async () => {
        browser = await extension.launch({ headless: false });
    });

    beforeEach(async () => {
        page = await browser.newPage();

        await page.setUserAgent(desktop.CHROME);
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
            title: 'model affordable price',
            link: PRICE.AFFORDABLE,
        },
        {
            title: 'model lowest price',
            link: PRICE.LOWEST,
        },
        {
            title: 'CV',
            link: CV,
        },
        {
            title: 'model on market',
            link: PRICE.MARKET_MODEL,
        },
        {
            title: 'new item price',
            link: PRICE.NEW,
        },
    ];

    describe('1440px', () => {
        testCases.forEach(({ title, link }) => {
            test(`${title}`, async () => {
                await page.setViewport(viewPort.CUSTOM);
                await page.goto(link, { waitUntil: 'networkidle2' });
                await page.waitForSelector(bar.COMMON, { visible: true });

                const barEl = await page.$(bar.COMMON);
                const barProductLogoEl = await page.$(bar.LOGO);
                const barTextEl = await page.$(bar.TEXT);
                const barProductNameEl = await page.$(bar.PRODUCT_NAME);
                const barDeliveryEl = await page.$(bar.DELIVERY.CONTAINER);
                const barButtonMoreEl = await page.$(bar.BUTTON.MORE_TEXT);

                expect(barEl).toBeDefined();
                expect(barProductLogoEl).toBeDefined();
                expect(barTextEl).toBeDefined();
                expect(barProductNameEl).toBeDefined();
                expect(barDeliveryEl).toBeDefined();
                expect(barButtonMoreEl).toBeDefined();
            });
        });
    });

    describe('1200px', () => {
        testCases.forEach(({ title, link }) => {
            test(`${title}`, async () => {
                await page.setViewport(viewPort['1200']);
                await page.goto(link, { waitUntil: 'networkidle2' });
                await page.waitForSelector(bar.COMMON, { visible: true });

                const barEl = await page.$(bar.COMMON);
                const barProductLogoEl = await page.$(bar.LOGO);
                const barTextEl = await page.$(bar.TEXT);
                const barProductNameEl = await page.$(bar.PRODUCT_NAME);
                const barDeliveryEl = await page.$(bar.DELIVERY.CONTAINER);
                const barButtonMoreEl = await page.$(bar.BUTTON.MORE_TEXT);

                expect(barEl).toBeDefined();
                expect(barProductLogoEl).toBeDefined();
                expect(barTextEl).toBeDefined();
                expect(barProductNameEl).toBeDefined();
                expect(barDeliveryEl).toBeDefined();
                expect(barButtonMoreEl).toBeDefined();
            });
        });
    });

    describe('1100px', () => {
        testCases.forEach(({ title, link }) => {
            test(`${title}`, async () => {
                await page.setViewport(viewPort['1100']);
                await page.goto(link, { waitUntil: 'networkidle2' });
                await page.waitForSelector(bar.COMMON, { visible: true });

                const barEl = await page.$(bar.COMMON);
                const barProductLogoEl = await page.$(bar.LOGO);
                const barTextEl = await page.$(bar.TEXT);
                const barProductNameEl = await page.$(bar.PRODUCT_NAME);
                const barDeliveryEl = await page.$(bar.DELIVERY.CONTAINER);
                const barButtonMoreEl = await page.$(bar.BUTTON.MORE_TEXT);

                expect(barEl).toBeDefined();
                expect(barProductLogoEl).toBeDefined();
                expect(barTextEl).toBeDefined();
                expect(barProductNameEl).toBeDefined();
                expect(barDeliveryEl).toBeDefined();
                expect(barButtonMoreEl).toBeDefined();
            });
        });
    });

    describe('950px', () => {
        testCases.forEach(({ title, link }) => {
            test(`${title}`, async () => {
                await page.setViewport(viewPort['950']);
                await page.goto(link, { waitUntil: 'networkidle2' });
                await page.waitForSelector(bar.COMMON, { visible: true });

                const barEl = await page.$(bar.COMMON);
                const barProductLogoEl = await page.$(bar.LOGO);
                const barTextEl = await page.$(bar.TEXT);
                const barProductNameEl = await page.$(bar.PRODUCT_NAME);
                const barDeliveryEl = await page.$(bar.DELIVERY.CONTAINER);
                const barButtonMoreEl = await page.$(bar.BUTTON.MORE_TEXT);

                expect(barEl).toBeDefined();
                expect(barProductLogoEl).toBeDefined();
                expect(barTextEl).toBeDefined();
                expect(barProductNameEl).toBeDefined();
                expect(barDeliveryEl).toBeDefined();
                expect(barButtonMoreEl).toBeDefined();
            });
        });
    });

    describe('880px', () => {
        testCases.forEach(({ title, link }) => {
            test(`${title}`, async () => {
                await page.setViewport(viewPort['880']);
                await page.goto(link, { waitUntil: 'networkidle2' });
                await page.waitForSelector(bar.COMMON, { visible: true });

                const barEl = await page.$(bar.COMMON);
                const barProductLogoEl = await page.$(bar.LOGO);
                const barTextEl = await page.$(bar.TEXT);
                const barProductNameEl = await page.$(bar.PRODUCT_NAME);
                const barDeliveryEl = await page.$(bar.DELIVERY.CONTAINER);
                const barButtonMoreEl = await page.$(bar.BUTTON.MORE_TEXT);

                expect(barEl).toBeDefined();
                expect(barProductLogoEl).toBeDefined();
                expect(barTextEl).toBeDefined();
                expect(barProductNameEl).toBeDefined();
                expect(barDeliveryEl).toBeDefined();
                expect(barButtonMoreEl).toBeDefined();
            });
        });
    });
});
