const { extension } = require('../../../src');
const viewPort = require('../../../src/utils/viewport');
const { desktop } = require('../../../src/utils/user-agent');
const {
    bar,
    mod,
    mainPopup,
    feedbackPopup,
    thanksPopup,
} = require('../../../src/utils/selector');
const { AUTO } = require('../../../src/utils/url');

let browser;
let page;

describe('Autobar', () => {
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

    test('feedback form', async () => {
        await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.AUTO, { visible: true });
        const barEl = await page.$(bar.AUTO);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        await page.click(bar.BUTTON.MORE);
        await page.click(mainPopup.LINK.TRACK_ERROR);

        const popupTrackErrorEl = await page.$(mainPopup.LINK.TRACK_ERROR);
        const popupEl = await page.$(feedbackPopup.COMMON);
        const popupClassProp = await popupEl.getProperty('className');
        const popupClassesRaw = await popupClassProp.jsonValue();
        const popupClasses = popupClassesRaw.split(' ');

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
        expect(popupTrackErrorEl).toBeDefined();
        expect(popupClasses.includes(mod.FEEDBACK_VISIBLE)).toBeTruthy();
    });

    test('send feedback without a comment', async () => {
        await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.AUTO, { visible: true });
        const barEl = await page.$(bar.AUTO);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        await page.click(bar.BUTTON.MORE);
        await page.click(mainPopup.LINK.TRACK_ERROR);
        await page.click(feedbackPopup.BUTTON.SEND);

        await page.waitForSelector(thanksPopup.COMMON, { visible: true });
        await page.waitForTimeout(3000);

        await page.waitForSelector(bar.AUTO, { hidden: true });
        await page.waitForSelector(feedbackPopup.COMMON, { hidden: true });

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
    });

    test('send feedback with a comment', async () => {
        await page.goto(AUTO.MORE_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.AUTO, { visible: true });
        const barEl = await page.$(bar.AUTO);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        await page.click(bar.BUTTON.MORE);
        await page.click(mainPopup.LINK.TRACK_ERROR);

        await page.$eval(feedbackPopup.TEXT_AREA, el => {
            // eslint-disable-next-line no-param-reassign
            el.value = 'test';
        });
        await page.click(feedbackPopup.BUTTON.SEND);

        await page.waitForSelector(thanksPopup.COMMON, { visible: true });
        await page.waitForTimeout(3000);

        await page.waitForSelector(bar.AUTO, { hidden: true });
        await page.waitForSelector(feedbackPopup.COMMON, { hidden: true });

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
    });

    test('close feedback by button', async () => {
        await page.goto(AUTO.FOUND_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.AUTO, { visible: true });
        const barEl = await page.$(bar.AUTO);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        await page.click(bar.BUTTON.MORE);
        await page.click(mainPopup.LINK.TRACK_ERROR);
        await page.click(feedbackPopup.BUTTON.CLOSE);

        await page.waitForTimeout(1000);

        await page.waitForSelector(bar.AUTO, { hidden: true });
        await page.waitForSelector(feedbackPopup.COMMON, { hidden: true });

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
    });

    test(`close feedback by icon 'close'`, async () => {
        await page.goto(AUTO.FOUND_OFFERS, { waitUntil: 'networkidle2' });
        await page.waitForSelector(bar.AUTO, { visible: true });
        const barEl = await page.$(bar.AUTO);
        const barMoreEl = await page.$(bar.BUTTON.MORE);

        await page.click(bar.BUTTON.MORE);
        await page.click(mainPopup.LINK.TRACK_ERROR);
        await page.click(feedbackPopup.CLOSE);

        await page.waitForTimeout(1000);

        await page.waitForSelector(bar.AUTO, { hidden: true });
        await page.waitForSelector(feedbackPopup.COMMON, { hidden: true });

        expect(barEl).toBeDefined();
        expect(barMoreEl).toBeDefined();
    });
});
