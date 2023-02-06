const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar } = require('../../src/utils/selector');
const { getInnerText } = require('../../src/utils/get-text');
const { AUTO } = require('../../src/utils/url');
const {
    barText: { auto },
} = require('../../src/utils/text-constants');

let browser;
let page;

describe('Autobar: randomize', () => {
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

    test('more offers', async () => {
        await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.AUTO, { visible: true });

        const barText = await page.evaluate(getInnerText, bar.TEXT);
        const barMoreText = await page.evaluate(
            getInnerText,
            bar.BUTTON.MORE_TEXT,
        );

        expect(barText).toBeDefined();
        expect(barMoreText).toStrictEqual(
            expect.stringMatching(auto.moreOffers.button.view.regex),
        );
    });

    test('found offers', async () => {
        await page.goto(AUTO.FOUND_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.AUTO, { visible: true });

        const barText = await page.evaluate(getInnerText, bar.TEXT);
        const barMoreText = await page.evaluate(
            getInnerText,
            bar.BUTTON.MORE_TEXT,
        );
        const barViewText = await page.evaluate(getInnerText, bar.BUTTON.VIEW);

        const splitted = barText.split('\n');
        const foundOffersText = splitted[0];

        expect(barText).toBeDefined();
        expect(foundOffersText).toStrictEqual(
            expect.stringMatching(auto.foundOffers.main.regex),
        );
        expect(barViewText).toStrictEqual(
            expect.stringMatching(auto.foundOffers.button.view.regex),
        );
        expect(barMoreText).toStrictEqual(
            expect.stringMatching(auto.foundOffers.button.more.regex),
        );
    });
});
