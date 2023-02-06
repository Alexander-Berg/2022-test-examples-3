/* eslint-disable no-await-in-loop */
const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const {
    bar,
    infoPopup,
    settingsPopup,
} = require('../../../src/utils/selector');
const { PRICE, SEARCH } = require('../../../src/utils/url');
const { color, font } = require('../../../src/utils/styles');
const getStyles = require('../../../src/utils/get-styles');
const rgbToHex = require('../../../src/utils/rgb-to-hex');

let browser;
let page;

describe('Secondary popups', () => {
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

    const testCases = [
        {
            title: 'affordable price',
            link: PRICE.AFFORDABLE,
        },
        {
            title: 'lowest price',
            link: PRICE.LOWEST,
        },
        {
            title: 'market search price',
            link: PRICE.MARKET_SEARCH,
        },
        {
            title: 'market model price',
            link: PRICE.MARKET_MODEL,
        },
        {
            title: 'new item price',
            link: PRICE.NEW,
        },
        {
            title: 'search popup',
            link: SEARCH,
        },
    ];

    describe('Settings popup', () => {
        testCases.forEach(({ title, link }) => {
            test(`${title}: show and hide by second click on settings button`, async () => {
                await page.goto(link, { waitUntil: 'networkidle2' });

                await page.waitForSelector(bar.BUTTON.SETTINGS, {
                    visible: true,
                });

                const settingsButton = await page.$(bar.BUTTON.SETTINGS);

                await page.click(bar.BUTTON.SETTINGS);

                await page.waitForSelector(settingsPopup.COMMON, {
                    visible: true,
                });

                const settingsPopupElVisible = await page.$(
                    settingsPopup.COMMON,
                    {
                        visible: true,
                    },
                );

                await page.click(bar.BUTTON.SETTINGS);

                const settingsPopupElInvisible = await page.$(
                    settingsPopup.COMMON,
                    {
                        hidden: true,
                    },
                );

                expect(settingsButton).toBeDefined();
                expect(settingsPopupElVisible).toBeDefined();
                expect(settingsPopupElInvisible).toBeDefined();
            });
        });

        testCases.forEach(({ title, link }) => {
            test(`${title}: show and hide by click on close button`, async () => {
                await page.goto(link, { waitUntil: 'networkidle2' });

                await page.waitForSelector(bar.BUTTON.SETTINGS, {
                    visible: true,
                });

                const settingsButton = await page.$(bar.BUTTON.SETTINGS);

                await page.click(bar.BUTTON.SETTINGS);

                await page.waitForSelector(settingsPopup.COMMON, {
                    visible: true,
                });

                const settingsPopupElVisible = await page.$(
                    settingsPopup.COMMON,
                    {
                        visible: true,
                    },
                );

                await page.waitForSelector(settingsPopup.BUTTON.CLOSE, {
                    visible: true,
                });
                await page.click(settingsPopup.BUTTON.CLOSE);

                const settingsPopupElHideByCloseBtn = await page.$(
                    settingsPopup.COMMON,
                    {
                        hidden: true,
                    },
                );

                expect(settingsButton).toBeDefined();
                expect(settingsPopupElVisible).toBeDefined();
                expect(settingsPopupElHideByCloseBtn).toBeDefined();
            });
        });

        test('change region', async () => {
            await page.goto(PRICE.LOWEST, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.BUTTON.SETTINGS, { visible: true });

            const settingsButton = await page.$(bar.BUTTON.SETTINGS);

            await page.click(bar.BUTTON.SETTINGS);
            await page.waitForSelector(settingsPopup.COMMON, { visible: true });
            await page.click(bar.BUTTON.SETTINGS);

            await page.waitForSelector(settingsPopup.BUTTON.CHANGE_REGION, {
                visible: true,
            });

            const currentPageUrl = page.url();
            const currentPage = page.target();

            await page.click(settingsPopup.BUTTON.CHANGE_REGION);

            const newTarget = await browser.waitForTarget(
                target => target.opener() === currentPage,
            );

            const newPage = await newTarget.page();
            const newPageUrl = newPage.url();
            const url = new URL(newPageUrl);

            expect(settingsButton).toBeDefined();
            expect(newPageUrl).not.toBe(currentPageUrl);
            expect(url.protocol).toBe('https:');
            expect(url.hostname).toBe('sovetnik.market.yandex.ru');
            expect(url.pathname).toBe('/app/settings/');
        });

        test('change settings button', async () => {
            await page.goto(PRICE.LOWEST, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.BUTTON.SETTINGS, { visible: true });

            const settingsButton = await page.$(bar.BUTTON.SETTINGS);

            await page.click(bar.BUTTON.SETTINGS);
            await page.waitForSelector(settingsPopup.COMMON, { visible: true });
            await page.click(bar.BUTTON.SETTINGS);

            await page.waitForSelector(settingsPopup.BUTTON.CHANGE_SETTINGS, {
                visible: true,
            });

            const currentPageUrl = page.url();
            const currentPage = page.target();

            await page.click(settingsPopup.BUTTON.CHANGE_SETTINGS);

            const newTarget = await browser.waitForTarget(
                target => target.opener() === currentPage,
            );

            const newPage = await newTarget.page();
            const newPageUrl = newPage.url();
            const url = new URL(newPageUrl);

            expect(settingsButton).toBeDefined();
            expect(newPageUrl).not.toBe(currentPageUrl);
            expect(url.protocol).toBe('https:');
            expect(url.hostname).toBe('sovetnik.market.yandex.ru');
            expect(url.pathname).toBe('/app/settings/');
        });

        test('turn off on this website', async () => {
            await page.goto(PRICE.LOWEST, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.BUTTON.SETTINGS, { visible: true });

            const settingsButton = await page.$(bar.BUTTON.SETTINGS);

            await page.click(bar.BUTTON.SETTINGS);
            await page.waitForSelector(settingsPopup.COMMON, { visible: true });
            await page.click(bar.BUTTON.SETTINGS);

            await page.waitForSelector(settingsPopup.BUTTON.TURN_OFF, {
                visible: true,
            });

            const currentPageUrl = page.url();
            const currentPage = page.target();
            const currentUrl = new URL(currentPageUrl);

            await page.click(settingsPopup.BUTTON.TURN_OFF);

            const newTarget = await browser.waitForTarget(
                target => target.opener() === currentPage,
            );

            const newPage = await newTarget.page();
            const newPageUrl = newPage.url();
            const url = new URL(newPageUrl);

            const expectedHash = `#domain=${currentUrl.hostname}`;

            expect(settingsButton).toBeDefined();
            expect(newPageUrl).not.toBe(currentPageUrl);
            expect(url.protocol).toBe('https:');
            expect(url.hostname).toBe('sovetnik.market.yandex.ru');
            expect(url.pathname).toBe('/app/settings/');
            expect(url.pathname).toBe('/app/settings/');
            expect(url.hash).toBe(expectedHash);
        });
    });

    describe('Info popup', () => {
        testCases.forEach(({ title, link }) => {
            test(`${title}: show and hide popup by second click on info button`, async () => {
                await page.goto(link, { waitUntil: 'networkidle2' });

                await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

                const infoButton = await page.$(bar.BUTTON.INFO);

                await page.click(bar.BUTTON.INFO);

                await page.waitForSelector(infoPopup.PRODUCT, {
                    visible: true,
                });

                const infoPopupElVisible = await page.$(infoPopup.PRODUCT, {
                    visible: true,
                });

                await page.click(bar.BUTTON.INFO);

                const infoPopupElInvisible = await page.$(infoPopup.PRODUCT, {
                    hidden: true,
                });

                expect(infoButton).toBeDefined();
                expect(infoPopupElVisible).toBeDefined();
                expect(infoPopupElInvisible).toBeDefined();
            });
        });

        testCases.forEach(({ title, link }) => {
            test(`${title}: show and hide popup by second click on close button`, async () => {
                await page.goto(link, { waitUntil: 'networkidle2' });

                await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

                const infoButton = await page.$(bar.BUTTON.INFO);

                await page.click(bar.BUTTON.INFO);

                await page.waitForSelector(infoPopup.PRODUCT, {
                    visible: true,
                });

                const infoPopupElVisible = await page.$(infoPopup.PRODUCT, {
                    visible: true,
                });

                await page.waitForSelector(infoPopup.BUTTON.CLOSE, {
                    visible: true,
                });
                await page.click(infoPopup.BUTTON.CLOSE);

                const infoPopupElHideByCloseBtn = await page.$(
                    infoPopup.PRODUCT,
                    {
                        hidden: true,
                    },
                );

                expect(infoButton).toBeDefined();
                expect(infoPopupElVisible).toBeDefined();
                expect(infoPopupElHideByCloseBtn).toBeDefined();
            });
        });

        test('features button', async () => {
            await page.goto(PRICE.LOWEST, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

            const infoButton = await page.$(bar.BUTTON.INFO);

            await page.click(bar.BUTTON.INFO);
            await page.waitForSelector(infoPopup.PRODUCT, { visible: true });

            await page.waitForSelector(infoPopup.BUTTON.FEATURES, {
                visible: true,
            });

            const currentPageUrl = page.url();
            const currentPage = page.target();

            await page.click(infoPopup.BUTTON.FEATURES);

            const newTarget = await browser.waitForTarget(
                target => target.opener() === currentPage,
            );

            const newPage = await newTarget.page();
            const newPageUrl = newPage.url();
            const url = new URL(newPageUrl);

            expect(infoButton).toBeDefined();
            expect(newPageUrl).not.toBe(currentPageUrl);
            expect(url.protocol).toBe('https:');
            expect(url.hostname).toBe('sovetnik.yandex.ru');
            expect(url.pathname).toBe('/info/');
        });

        test('help link', async () => {
            await page.goto(PRICE.LOWEST, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

            const infoButton = await page.$(bar.BUTTON.INFO);

            await page.click(bar.BUTTON.INFO);
            await page.waitForSelector(infoPopup.PRODUCT, { visible: true });

            await page.waitForSelector(infoPopup.LINK.HELP, {
                visible: true,
            });

            const currentPageUrl = page.url();
            const currentPage = page.target();

            await page.click(infoPopup.LINK.HELP);

            const newTarget = await browser.waitForTarget(
                target => target.opener() === currentPage,
            );

            const newPage = await newTarget.page();
            const newPageUrl = newPage.url();
            const url = new URL(newPageUrl);

            expect(infoButton).toBeDefined();
            expect(newPageUrl).not.toBe(currentPageUrl);
            expect(url.protocol).toBe('https:');
            expect(url.hostname).toBe('yandex.ru');
            expect(url.pathname).toBe(
                '/support/market/personal-services/sovetnik.html',
            );
        });

        test('feedback link', async () => {
            await page.goto(PRICE.LOWEST, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

            const infoButton = await page.$(bar.BUTTON.INFO);

            await page.click(bar.BUTTON.INFO);
            await page.waitForSelector(infoPopup.PRODUCT, { visible: true });

            await page.waitForSelector(infoPopup.LINK.FEEDBACK, {
                visible: true,
            });

            const currentPageUrl = page.url();
            const currentPage = page.target();

            await page.click(infoPopup.LINK.FEEDBACK);

            const newTarget = await browser.waitForTarget(
                target => target.opener() === currentPage,
            );

            const newPage = await newTarget.page();
            const newPageUrl = newPage.url();
            const url = new URL(newPageUrl);

            expect(infoButton).toBeDefined();
            expect(newPageUrl).not.toBe(currentPageUrl);
            expect(url.protocol).toBe('https:');
            expect(url.hostname).toBe('www.yandex.ru');
            expect(url.pathname).toBe(
                '/support/market/personal-services/sovetnik.html',
            );
            expect(url.hash).toBe('#faq__other-problem');
        });

        test('turn off link', async () => {
            await page.goto(PRICE.LOWEST, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

            const infoButton = await page.$(bar.BUTTON.INFO);

            await page.click(bar.BUTTON.INFO);
            await page.waitForSelector(infoPopup.PRODUCT, { visible: true });

            await page.waitForSelector(infoPopup.LINK.TURN_OFF, {
                visible: true,
            });

            const currentPageUrl = page.url();
            const currentPage = page.target();

            await page.click(infoPopup.LINK.TURN_OFF);

            const newTarget = await browser.waitForTarget(
                target => target.opener() === currentPage,
            );

            const newPage = await newTarget.page();
            const newPageUrl = newPage.url();
            const url = new URL(newPageUrl);

            expect(infoButton).toBeDefined();
            expect(newPageUrl).not.toBe(currentPageUrl);
            expect(url.protocol).toBe('https:');
            expect(url.hostname).toBe('yandex.ru');
            expect(url.pathname).toBe(
                '/support/market/personal-services/sovetnik.html',
            );
            expect(url.hash).toBe('#settings');
        });

        test('license link', async () => {
            await page.goto(PRICE.LOWEST, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

            const infoButton = await page.$(bar.BUTTON.INFO);

            await page.click(bar.BUTTON.INFO);
            await page.waitForSelector(infoPopup.PRODUCT, { visible: true });

            await page.waitForSelector(infoPopup.LINK.LICENSE, {
                visible: true,
            });

            const currentPageUrl = page.url();
            const currentPage = page.target();

            await page.click(infoPopup.LINK.LICENSE);

            const newTarget = await browser.waitForTarget(
                target => target.opener() === currentPage,
            );

            const newPage = await newTarget.page();
            const newPageUrl = newPage.url();
            const url = new URL(newPageUrl);

            expect(infoButton).toBeDefined();
            expect(newPageUrl).not.toBe(currentPageUrl);
            expect(url.protocol).toBe('https:');
            expect(url.hostname).toBe('www.yandex.ru');
            expect(url.pathname).toBe('/legal/advisor_agreement/');
        });
    });

    describe('hover', () => {
        describe('info popup', () => {
            test('links', async () => {
                await page.goto(PRICE.AFFORDABLE, {
                    waitUntil: 'networkidle2',
                });
                await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

                await page.click(bar.BUTTON.INFO);
                await page.waitForSelector(infoPopup.PRODUCT, {
                    visible: true,
                });

                const links = [
                    infoPopup.LINK.HELP,
                    infoPopup.LINK.FEEDBACK,
                    infoPopup.LINK.TURN_OFF,
                ];

                // eslint-disable-next-line no-restricted-syntax
                for (const link of links) {
                    const linkStyles = await page.evaluate(getStyles, link);
                    await page.hover(link);
                    const hoveredLinkStyles = await page.evaluate(
                        getStyles,
                        link,
                    );

                    expect(rgbToHex(linkStyles.color)).toBe(
                        color.purple.legacy,
                    );
                    expect(linkStyles.fontFamily).toBe(font.family);
                    expect(linkStyles.fontSize).toBe(font.size.bodyBig);
                    expect(linkStyles.fontWeight).toBe(font.weight.normal);

                    expect(rgbToHex(hoveredLinkStyles.color)).toBe(
                        color.red.legacy,
                    );
                    expect(hoveredLinkStyles.fontFamily).toBe(font.family);
                    expect(hoveredLinkStyles.fontSize).toBe(font.size.bodyBig);
                    expect(hoveredLinkStyles.fontWeight).toBe(
                        font.weight.normal,
                    );
                }
            });

            test('license link', async () => {
                await page.goto(PRICE.AFFORDABLE, {
                    waitUntil: 'networkidle2',
                });
                await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

                await page.click(bar.BUTTON.INFO);
                await page.waitForSelector(infoPopup.PRODUCT, {
                    visible: true,
                });

                const linkStyles = await page.evaluate(
                    getStyles,
                    infoPopup.LINK.LICENSE,
                );

                await page.hover(infoPopup.LINK.LICENSE);

                const hoveredLinkStyles = await page.evaluate(
                    getStyles,
                    infoPopup.LINK.LICENSE,
                );

                expect(rgbToHex(linkStyles.color)).toBe(color.purple.legacy);
                expect(linkStyles.fontFamily).toBe(font.family);
                expect(linkStyles.fontSize).toBe(font.size.bodySmall);
                expect(linkStyles.fontWeight).toBe(font.weight.normal);

                expect(rgbToHex(hoveredLinkStyles.color)).toBe(
                    color.red.legacy,
                );
                expect(hoveredLinkStyles.fontFamily).toBe(font.family);
                expect(hoveredLinkStyles.fontSize).toBe(font.size.bodySmall);
                expect(hoveredLinkStyles.fontWeight).toBe(font.weight.normal);
            });

            test('features button', async () => {
                await page.goto(PRICE.AFFORDABLE, {
                    waitUntil: 'networkidle2',
                });
                await page.waitForSelector(bar.BUTTON.INFO, { visible: true });

                await page.click(bar.BUTTON.INFO);
                await page.waitForSelector(infoPopup.PRODUCT, {
                    visible: true,
                });

                const linkStyles = await page.evaluate(
                    getStyles,
                    infoPopup.BUTTON.FEATURES,
                );

                await page.hover(infoPopup.BUTTON.FEATURES);

                const hoveredLinkStyles = await page.evaluate(
                    getStyles,
                    infoPopup.BUTTON.FEATURES,
                );

                expect(rgbToHex(linkStyles.color)).toBe(color.black.legacy);
                expect(linkStyles.fontFamily).toBe(font.family);
                expect(linkStyles.fontSize).toBe(font.size.body);
                expect(linkStyles.fontWeight).toBe(font.weight.normal);
                expect(rgbToHex(linkStyles.backgroundColor)).toBe(
                    color.yellow.legacy,
                );

                expect(rgbToHex(hoveredLinkStyles.color)).toBe(
                    color.black.legacy,
                );
                expect(hoveredLinkStyles.fontFamily).toBe(font.family);
                expect(hoveredLinkStyles.fontSize).toBe(font.size.body);
                expect(hoveredLinkStyles.fontWeight).toBe(font.weight.normal);
                expect(rgbToHex(linkStyles.backgroundColor)).toBe(
                    color.yellow.legacy,
                );
            });
        });
    });
});
