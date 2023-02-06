const { extension } = require('../src');
const viewPort = require('../src/utils/viewport');
const { desktop } = require('../src/utils/user-agent');
const { bar } = require('../src/utils/selector');
const { PRICE } = require('../src/utils/url');

const PAGE_URL = PRICE.AFFORDABLE;

let browser;
let page;

describe.skip('Install', () => {
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

    test('should open browser', async () => {
        const targets = await browser.targets();
        const backgroundPageTarget = targets.find(
            target => target.type() === 'background_page',
        );
        const backgroundPage = await backgroundPageTarget.page();

        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.PRODUCT, { visible: true });
        const barEl = await page.$(bar.PRODUCT);
        const pages = await browser.pages();

        const welcomePage = pages[pages.length - 1];
        const welcomePageUrl = welcomePage.url();

        expect(browser).toBeDefined();
        expect(barEl).toBeDefined();
        expect(welcomePageUrl).toBeDefined();
        expect(
            welcomePageUrl.includes('sovetnik.yandex.ru/welcome'),
        ).toBeTruthy();
        expect(backgroundPage.url()).toBeDefined();
    });
});
