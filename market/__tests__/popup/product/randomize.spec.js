const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const { bar, mainPopup } = require('../../../src/utils/selector');
const { getInnerText } = require('../../../src/utils/get-text');
const { PRICE, SEARCH } = require('../../../src/utils/url');
const {
    mainPopupText: { product },
} = require('../../../src/utils/text-constants');

let browser;
let page;

describe('Product popup: randomize', () => {
    beforeAll(async () => {
        browser = await extension.launch({ headless: false, slowMo: 300 });
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

    describe('tabs', () => {
        const testCases = [
            {
                title: 'affordable price',
                link: PRICE.AFFORDABLE,
            },
            {
                title: 'market price',
                link: PRICE.MARKET_MODEL,
            },

            {
                title: 'price for a new item',
                link: PRICE.NEW,
            },
        ];

        testCases.forEach(({ title, link }) => {
            test(`${title}`, async () => {
                await page.goto(link, { waitUntil: 'networkidle2' });
                await page.waitForSelector(bar.PRODUCT, { visible: true });

                await page.click(bar.BUTTON.MORE);
                await page.waitForSelector(mainPopup.COMMON, {
                    visible: true,
                });
                await page.waitForSelector(mainPopup.FOOTER.INFO, {
                    visible: true,
                });
                await page.waitForSelector(mainPopup.FOOTER.WRONG_PRODUCT, {
                    visible: true,
                });

                const tabActiveText = await page.evaluate(
                    getInnerText,
                    mainPopup.TAB.ACTIVE,
                );

                expect(tabActiveText).toStrictEqual(
                    expect.stringMatching(product.tab.active.regex),
                );
            });
        });
    });

    describe('footer', () => {
        const cases = [
            {
                title: 'model popup: affordable price',
                link: PRICE.AFFORDABLE,
            },
            {
                title: 'model popup: market price',
                link: PRICE.MARKET_MODEL,
            },
            {
                title: 'search popup: market price',
                link: PRICE.MARKET_SEARCH,
            },
            {
                title: 'lowest price',
                link: PRICE.LOWEST,
            },
            {
                title: 'price for a new item',
                link: PRICE.NEW,
            },
            {
                title: 'search popup',
                link: SEARCH,
            },
        ];

        cases.forEach(({ title, link }) => {
            test(`${title}`, async () => {
                await page.goto(link, { waitUntil: 'networkidle2' });
                await page.waitForSelector(bar.PRODUCT, { visible: true });

                await page.click(bar.BUTTON.MORE);
                await page.waitForSelector(mainPopup.COMMON, {
                    visible: true,
                });
                await page.waitForSelector(mainPopup.FOOTER.INFO, {
                    visible: true,
                });
                await page.waitForSelector(mainPopup.FOOTER.WRONG_PRODUCT, {
                    visible: true,
                });

                const infoText = await page.evaluate(
                    getInnerText,
                    mainPopup.FOOTER.INFO,
                );

                const wrongProductText = await page.evaluate(
                    getInnerText,
                    mainPopup.FOOTER.WRONG_PRODUCT,
                );

                const sourceText = await page.evaluate(
                    getInnerText,
                    mainPopup.FOOTER.SOURCE,
                );

                expect(infoText).toStrictEqual(
                    expect.stringMatching(product.footer.info.regex),
                );
                expect(wrongProductText).toStrictEqual(
                    expect.stringMatching(product.footer.feedback.regex),
                );
                expect(sourceText).toStrictEqual(
                    expect.stringMatching(product.footer.source.regex),
                );
            });
        });
    });
});
