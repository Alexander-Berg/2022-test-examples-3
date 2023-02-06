const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar } = require('../../src/utils/selector');
const { getTextContent, getInnerText } = require('../../src/utils/get-text');
const { CV, DISCLAIMER, PRICE } = require('../../src/utils/url');
const {
    barText: {
        cv,
        disclaimer,
        affordablePrice: ap,
        lowest,
        marketPrice: mp,
        newItem: ni,
    },
} = require('../../src/utils/text-constants');

let browser;
let page;

describe('Pricebar: randomize', () => {
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

    test('price for a new item', async () => {
        await page.goto(PRICE.NEW, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });

        const barText = await page.evaluate(getInnerText, bar.TEXT);
        const barMoreText = await page.evaluate(
            getInnerText,
            bar.BUTTON.MORE_TEXT,
        );
        const barViewText = await page.evaluate(getInnerText, bar.BUTTON.VIEW);

        const splitted = barText.split(' ');

        const marketPriceText = `${splitted[0]} ${splitted[1]} ${splitted[2]}`;

        expect(barText).toBeDefined();
        expect(marketPriceText).toStrictEqual(
            expect.stringMatching(ni.main.regex),
        );
        expect(barViewText).toStrictEqual(
            expect.stringMatching(ni.button.view.regex),
        );
        expect(barMoreText).toStrictEqual(
            expect.stringMatching(ni.button.moreOffers.regex),
        );
    });

    describe('market price', () => {
        const testCases = [
            {
                title: 'search popup',
                link: PRICE.MARKET_SEARCH,
            },
            {
                title: 'model popup',
                link: PRICE.MARKET_MODEL,
            },
        ];

        testCases.forEach(tc => {
            const { title, link } = tc;

            test(`${title}`, async () => {
                await page.goto(link, { waitUntil: 'networkidle2' });
                await page.waitForSelector(bar.PRODUCT, { visible: true });

                const barText = await page.evaluate(getInnerText, bar.TEXT);
                const barMoreText = await page.evaluate(
                    getInnerText,
                    bar.BUTTON.MORE_TEXT,
                );
                const barViewText = await page.evaluate(
                    getInnerText,
                    bar.BUTTON.VIEW,
                );

                const splitted = barText.split(' ');

                const marketPriceText = `${splitted[0]} ${splitted[1]} ${splitted[2]} ${splitted[3]}`;

                expect(barText).toBeDefined();

                /**
                 * Flaky expect
                 * TODO: ensure that string is always randomized from backend
                 */
                // expect(marketPriceText).not.toBe(mp.main.origin);
                expect(marketPriceText).toStrictEqual(
                    expect.stringMatching(mp.main.regex),
                );

                /**
                 * Flaky expect
                 * TODO: ensure that string is always randomized from backend
                 */
                // expect(barViewText).not.toBe(mp.button.view.origin);
                expect(barViewText).toStrictEqual(
                    expect.stringMatching(mp.button.view.regex),
                );

                /**
                 * Flaky expect
                 * TODO: ensure that string is always randomized from backend
                 */
                // expect(barMoreText).not.toBe(mp.button.moreOffers.origin);
                expect(barMoreText).toStrictEqual(
                    expect.stringMatching(mp.button.moreOffers.regex),
                );
            });
        });
    });

    test('lowest price', async () => {
        await page.goto(PRICE.LOWEST, { waitUntil: 'networkidle2' });

        await page.waitForSelector(bar.PRODUCT, { visible: true });

        const barText = await page.evaluate(getInnerText, bar.TEXT);
        const barMoreText = await page.evaluate(
            getInnerText,
            bar.BUTTON.MORE_TEXT,
        );
        const barSourceText = await page.evaluate(getInnerText, bar.SOURCE);

        const splitted = barText.split(' ');
        const lowestPriceText = `${splitted[0]} ${splitted[1]} ${splitted[2]} ${splitted[3]} ${splitted[4]} ${splitted[5]} ${splitted[6]}`;

        expect(barText).toBeDefined();
        expect(lowestPriceText).toStrictEqual(
            expect.stringMatching(lowest.main.regex),
        );
        expect(barMoreText).toStrictEqual(
            expect.stringMatching(lowest.button.moreOffers.regex),
        );
        expect(barSourceText).toStrictEqual(
            expect.stringMatching(lowest.source.regex),
        );
    });

    test('affordable price', async () => {
        await page.goto(PRICE.AFFORDABLE, { waitUntil: 'networkidle2' });

        await page.waitForSelector(bar.PRODUCT, { visible: true });
        await page.waitForSelector(bar.BUTTON.ECONOMY_BADGE, { visible: true });

        const barText = await page.evaluate(getInnerText, bar.TEXT);
        const barMoreText = await page.evaluate(
            getInnerText,
            bar.BUTTON.MORE_TEXT,
        );
        const barViewText = await page.evaluate(getInnerText, bar.BUTTON.VIEW);
        const barProfitInnerText = await page.evaluate(
            getInnerText,
            bar.BUTTON.ECONOMY_BADGE,
        );

        const splitted = barText.split(' ');
        const moreAffordablePriceText = `${splitted[0]} ${splitted[1]} ${splitted[2]} ${splitted[3]}`;
        const barProfitText = barProfitInnerText.split(' ')[0];

        expect(barText).toBeDefined();

        expect(moreAffordablePriceText).toStrictEqual(
            expect.stringMatching(ap.main.regex),
        );

        expect(barProfitText).toStrictEqual(
            expect.stringMatching(ap.button.economy.regex),
        );
        expect(barMoreText).toStrictEqual(
            expect.stringMatching(ap.button.moreOffers.regex),
        );
        expect(barViewText).toStrictEqual(
            expect.stringMatching(ap.button.view.regex),
        );
    });

    test('cv', async () => {
        await page.goto(CV, { waitUntil: 'networkidle2' });

        await page.waitForSelector(bar.PRODUCT, { visible: true });
        await page.waitForSelector(bar.BUTTON.ECONOMY_BADGE, { visible: true });

        const barText = await page.evaluate(getInnerText, bar.TEXT);
        const barMoreText = await page.evaluate(
            getInnerText,
            bar.BUTTON.MORE_TEXT,
        );
        const barPriceFromInnerText = await page.evaluate(
            getInnerText,
            bar.BUTTON.ECONOMY_BADGE,
        );

        const splitted = barText.split(' ');
        const foundText = splitted[0];
        const categoryText = `${splitted[splitted.length - 3]} ${
            splitted[splitted.length - 2]
        }`;

        const splittedPriceFrom = barPriceFromInnerText.split(' ');
        const priceFromText = `${splittedPriceFrom[0]} ${splittedPriceFrom[1]}`;

        expect(barText).toBeDefined();
        expect(foundText).toStrictEqual(expect.stringMatching(cv.found.regex));
        expect(categoryText).toStrictEqual(
            expect.stringMatching(cv.fromCategory.regex),
        );
        expect(barMoreText).toStrictEqual(
            expect.stringMatching(cv.button.moreOffers.regex),
        );
        expect(priceFromText).toStrictEqual(
            expect.stringMatching(cv.button.priceFrom.regex),
        );
    });

    describe('disclaimer', () => {
        const testCases = [
            {
                title: 'medicine',
                link: DISCLAIMER.MEDICINE,
                expected: expect.stringMatching(disclaimer.medicine.regex),
                origin: disclaimer.medicine.origin,
            },
            {
                title: 'gun',
                link: DISCLAIMER.GUN,
                expected: expect.stringMatching(disclaimer.gun.regex),
                origin: disclaimer.gun.origin,
            },
            {
                title: 'age',
                link: DISCLAIMER.BOOK,
                expected: expect.stringMatching(disclaimer.age.regex),
                origin: disclaimer.age.origin,
            },
        ];

        testCases.forEach(tc => {
            const { title, link, expected, origin } = tc;

            test(`${title}`, async () => {
                await page.goto(link, { waitUntil: 'networkidle2' });
                await page.waitForSelector(bar.PRODUCT, { visible: true });

                const disclaimerText = await page.evaluate(
                    getTextContent,
                    bar.DISCLAIMER.TEXT,
                );

                expect(disclaimerText).toBeDefined();
                expect(disclaimerText).toStrictEqual(expected);
                expect(disclaimerText).toBe(origin);
            });
        });
    });
});
