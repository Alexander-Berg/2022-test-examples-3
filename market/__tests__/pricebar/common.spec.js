const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar } = require('../../src/utils/selector');
const { PRICE, SEARCH } = require('../../src/utils/url');
const getTitle = require('../../src/utils/get-title');
const { getTextContent } = require('../../src/utils/get-text');

let browser;
let page;

describe('Pricebar', () => {
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

    describe('delivery', () => {
        describe('model', () => {
            const testCases = [
                {
                    title: 'affordable price',
                    link: PRICE.AFFORDABLE,
                },
                {
                    title: 'price on market',
                    link: PRICE.MARKET_MODEL,
                },
            ];

            testCases.forEach(tc => {
                const { title, link } = tc;

                test(`${title}`, async () => {
                    await page.goto(link, { waitUntil: 'networkidle2' });
                    await page.waitForSelector(bar.PRODUCT, { visible: true });

                    const barDeliveryEl = await page.$(bar.DELIVERY.EL);
                    const barDeliveryIcon = await page.$(bar.DELIVERY.ICON);
                    const deliveryTitle = await page.evaluate(
                        getTitle,
                        bar.DELIVERY.EL,
                    );

                    const deliveryTextContent = await page.evaluate(
                        getTextContent,
                        bar.DELIVERY.EL,
                    );

                    expect(barDeliveryEl).toBeDefined();
                    expect(barDeliveryIcon).toBeDefined();
                    expect(deliveryTitle).toBeDefined();
                    expect(deliveryTextContent).toBeDefined();
                });
            });
        });

        describe('search popup', () => {
            const testCases = [
                {
                    title: 'affordable price',
                    link: SEARCH,
                },
                {
                    title: 'price on market',
                    link: PRICE.MARKET_SEARCH,
                },
            ];

            testCases.forEach(tc => {
                const { title, link } = tc;

                test(`${title}`, async () => {
                    await page.goto(link, {
                        waitUntil: 'networkidle2',
                    });
                    await page.waitForSelector(bar.PRODUCT, { visible: true });
                    const barDeliveryEl = await page.$(bar.DELIVERY.EL);
                    const barDeliveryIcon = await page.$(bar.DELIVERY.ICON);
                    const deliveryTitle = await page.evaluate(
                        getTitle,
                        bar.DELIVERY.EL,
                    );

                    const deliveryTextContent = await page.evaluate(
                        getTextContent,
                        bar.DELIVERY.EL,
                    );

                    expect(barDeliveryEl).toBeDefined();
                    expect(barDeliveryIcon).toBeDefined();
                    expect(deliveryTitle).toBeDefined();
                    expect(deliveryTextContent).toBeDefined();
                });
            });
        });
    });
});
