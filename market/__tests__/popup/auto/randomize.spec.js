const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const { bar, mainPopup } = require('../../../src/utils/selector');
const { getInnerText } = require('../../../src/utils/get-text');
const { AUTO } = require('../../../src/utils/url');
const {
    mainPopupText: { auto },
} = require('../../../src/utils/text-constants');

let browser;
let page;

describe('Auto popup: randomize', () => {
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
            title: 'more offers',
            link: AUTO.MORE_OFFERS,
        },
        {
            title: 'found offers',
            link: AUTO.FOUND_OFFERS,
        },
    ];

    testCases.forEach(tc => {
        const { title, link } = tc;

        test(`${title}`, async () => {
            await page.goto(link, { waitUntil: 'networkidle2' });
            await page.waitForSelector(bar.AUTO, { visible: true });

            await page.click(bar.BUTTON.MORE);
            await page.waitForSelector(mainPopup.COMMON, { visible: true });

            const tabText = await page.evaluate(
                getInnerText,
                mainPopup.TAB.ACTIVE,
            );
            const averagePriceText = await page.evaluate(
                getInnerText,
                mainPopup.LINK.AUTO.AVERAGE_PRICE,
            );

            const allOffersBtn = await page.evaluate(
                getInnerText,
                mainPopup.BUTTON.AUTO.ALL_OFFERS,
            );

            const wrongProductText = await page.evaluate(
                getInnerText,
                mainPopup.FOOTER.WRONG_PRODUCT,
            );

            const sourceText = await page.evaluate(
                getInnerText,
                mainPopup.FOOTER.SOURCE,
            );

            const splitted = allOffersBtn.split(' ');
            const allOffersText = `${splitted[0]} ${splitted[1]} ${splitted[2]}`;

            expect(averagePriceText).toBe(auto.header.averagePrice.origin);
            expect(tabText).toBe(auto.tab.origin);
            expect(allOffersText).toBe(auto.button.allOffers.origin);
            expect(wrongProductText).toBe(auto.footer.feedback.origin);
            expect(sourceText).toBe(auto.footer.source.origin);
        });
    });
});
