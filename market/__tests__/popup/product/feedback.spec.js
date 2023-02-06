const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const {
    bar,
    footer,
    feedbackPopup,
    thanksPopup,
} = require('../../../src/utils/selector');
const { PRICE } = require('../../../src/utils/url');
const getStyles = require('../../../src/utils/get-styles');
const { font, color } = require('../../../src/utils/styles');
const rgbToHex = require('../../../src/utils/rgb-to-hex');
const {
    mainPopupText: {
        product: { feedback },
    },
} = require('../../../src/utils/text-constants');

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

    test('send feedback without a comment', async () => {
        await page.goto(PRICE.AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        await page.click(bar.BUTTON.MORE);
        await page.click(footer.TRACK_ERROR);

        await page.click(feedbackPopup.BUTTON.SEND);
        await page.waitForSelector(thanksPopup.COMMON, { visible: true });
        await page.waitForTimeout(3000);

        await page.waitForSelector(bar.PRODUCT, { hidden: true });
        await page.waitForSelector(feedbackPopup.COMMON, { hidden: true });

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
    });

    test('send feedback with a comment', async () => {
        await page.goto(PRICE.AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        await page.click(bar.BUTTON.MORE);
        await page.click(footer.TRACK_ERROR);

        await page.$eval(feedbackPopup.TEXT_AREA, el => {
            // eslint-disable-next-line no-param-reassign
            el.value = 'test';
        });
        await page.click(feedbackPopup.BUTTON.SEND);
        await page.waitForSelector(thanksPopup.COMMON, { visible: true });
        await page.waitForTimeout(3000);

        await page.waitForSelector(bar.PRODUCT, { hidden: true });
        await page.waitForSelector(feedbackPopup.COMMON, { hidden: true });

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
    });

    test(`close feedback by button 'close'`, async () => {
        await page.goto(PRICE.AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        await page.click(bar.BUTTON.MORE);
        await page.click(footer.TRACK_ERROR);

        await page.click(feedbackPopup.BUTTON.SEND);
        await page.waitForTimeout(1000);

        await page.waitForSelector(bar.AUTO, { hidden: true });
        await page.waitForSelector(feedbackPopup.COMMON, { hidden: true });

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
    });

    test(`close feedback by icon 'icon'`, async () => {
        await page.goto(PRICE.AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        await page.click(bar.BUTTON.MORE);
        await page.click(footer.TRACK_ERROR);

        await page.click(feedbackPopup.BUTTON.SEND);
        await page.waitForTimeout(1000);

        await page.waitForSelector(bar.AUTO, { hidden: true });
        await page.waitForSelector(feedbackPopup.COMMON, { hidden: true });

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
    });

    test(`options styles`, async () => {
        await page.goto(PRICE.AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        await page.click(bar.BUTTON.MORE);
        await page.click(footer.TRACK_ERROR);

        const optionOneStyles = await page.evaluate(
            getStyles,
            feedbackPopup.OPTION.WRONG_PRODUCT,
        );

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();

        expect(optionOneStyles.fontFamily).toBe(font.family);
        expect(optionOneStyles.fontSize).toBe(font.size.bodyBig);
        expect(optionOneStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(optionOneStyles.color)).toBe(color.black.legacy);
    });

    test(`options styles - click`, async () => {
        await page.goto(PRICE.AFFORDABLE, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        await page.click(bar.BUTTON.MORE);
        await page.click(footer.TRACK_ERROR);

        await page.click(feedbackPopup.OPTION.WRONG_PRODUCT);

        const optionOneStyles = await page.evaluate(
            getStyles,
            feedbackPopup.OPTION.WRONG_PRODUCT,
        );

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();

        expect(optionOneStyles.fontFamily).toBe(font.family);
        expect(optionOneStyles.fontSize).toBe(font.size.bodyBig);
        expect(optionOneStyles.fontWeight).toBe(font.weight.normal);
        expect(rgbToHex(optionOneStyles.color)).toBe(color.black.legacy);
        expect(rgbToHex(optionOneStyles.backgroundColor)).toBe(
            color.yellow.feedback,
        );
    });

    describe('send feedback with an option', () => {
        const testCases = [
            {
                title: feedback.wrongProduct.origin,
                regex: feedback.wrongProduct.regex,
                element: feedbackPopup.OPTION.WRONG_PRODUCT,
            },
            {
                title: feedback.wrongSpecs.origin,
                regex: feedback.wrongSpecs.regex,
                element: feedbackPopup.OPTION.WRONG_SPECS,
            },
            {
                title: feedback.moreExpensive.origin,
                regex: feedback.moreExpensive.regex,
                element: feedbackPopup.OPTION.MORE_EXPENSIVE,
            },
            {
                title: feedback.multipleProducts.origin,
                regex: feedback.multipleProducts.regex,
                element: feedbackPopup.OPTION.MULTIPLE_PRODUCTS,
            },
            {
                title: feedback.noProducts.origin,
                regex: feedback.noProducts.regex,
                element: feedbackPopup.OPTION.NO_PRODUCTS,
            },
        ];

        testCases.forEach(({ title, regex, element }) => {
            test(`${title}`, async () => {
                let feedbackRequest;
                await page.setRequestInterception(true);
                page.on('request', interceptedRequest => {
                    if (
                        interceptedRequest.url() ===
                        'https://sovetnik.market.yandex.ru/feedback'
                    ) {
                        feedbackRequest = interceptedRequest.postData();
                        interceptedRequest.continue();
                    } else {
                        interceptedRequest.continue();
                    }
                });

                await page.goto(PRICE.AFFORDABLE, {
                    waitUntil: 'networkidle2',
                });
                await page.waitForSelector(bar.PRODUCT, { visible: true });
                const barEl = await page.$(bar.PRODUCT);
                const barMoreEl = await page.$(bar.BUTTON.MORE);

                await page.click(bar.BUTTON.MORE);
                await page.click(footer.TRACK_ERROR);

                await page.click(element);

                await page.click(feedbackPopup.BUTTON.SEND);
                await page.waitForSelector(thanksPopup.COMMON, {
                    visible: true,
                });
                await page.waitForTimeout(3000);

                await page.waitForSelector(bar.PRODUCT, { hidden: true });
                await page.waitForSelector(feedbackPopup.COMMON, {
                    hidden: true,
                });

                expect(barEl).toBeDefined();
                expect(barMoreEl).toBeDefined();

                const { feedbackData } = JSON.parse(feedbackRequest);

                expect(feedbackData.message).toBe('');
                expect(feedbackData.options).toEqual(
                    expect.stringMatching(regex),
                );
            });
        });
    });
});
