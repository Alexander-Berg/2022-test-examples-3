const { extension } = require('../../src');
const viewPort = require('../../src/utils/viewport');
const { desktop } = require('../../src/utils/user-agent');
const { bar, mainPopup, mod } = require('../../src/utils/selector');
const { DISCLAIMER } = require('../../src/utils/url');

const PAGE_URL = DISCLAIMER.BOOK;

let browser;
let page;

describe('Pricebar: button "More"', () => {
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

    test('more button click: show and hide main popup, change caret sign', async () => {
        await page.goto(PAGE_URL, { waitUntil: 'networkidle2' });

        await page.waitForSelector(bar.BUTTON.MORE, { visible: true });
        await page.waitForSelector(bar.BUTTON.CARET.DOWN, { visible: true });
        await page.waitForSelector(bar.BUTTON.CARET.UP, { hidden: true });

        const moreButton = await page.$(bar.BUTTON.MORE);
        const caretDown = await page.$(bar.BUTTON.CARET.DOWN);
        const caretUp = await page.$(bar.BUTTON.CARET.DOWN);

        await page.click(bar.BUTTON.MORE);

        await page.waitForSelector(bar.BUTTON.CARET.DOWN, { hidden: true });
        await page.waitForSelector(bar.BUTTON.CARET.UP, { visible: true });

        const popupElVisible = await page.$(mainPopup.COMMON, {
            visible: true,
        });

        const moreButtonExpandedClassProp = await moreButton.getProperty(
            'className',
        );
        const moreButtonExpandedClassesRaw = await moreButtonExpandedClassProp.jsonValue();
        const moreButtonExpandedClasses = moreButtonExpandedClassesRaw.split(
            ' ',
        );
        const haveExpandedClass = moreButtonExpandedClasses.includes(
            mod.MORE_EXPANDED,
        );

        await page.click(bar.BUTTON.MORE);
        await page.waitForSelector(bar.BUTTON.CARET.DOWN, { visible: true });
        await page.waitForSelector(bar.BUTTON.CARET.UP, { hidden: true });

        const popupElInvisible = await page.$(mainPopup.COMMON, {
            hidden: true,
        });

        const moreButtonCollapsedClassProp = await moreButton.getProperty(
            'className',
        );
        const moreButtonCollapsedClassesRaw = await moreButtonCollapsedClassProp.jsonValue();
        const moreButtonCollapsedClasses = moreButtonCollapsedClassesRaw.split(
            ' ',
        );
        const dontHaveExpandedClass = !moreButtonCollapsedClasses.includes(
            mod.MORE_EXPANDED,
        );

        expect(moreButton).toBeDefined();
        expect(caretDown).toBeDefined();
        expect(caretUp).toBeDefined();
        expect(popupElVisible).toBeDefined();
        expect(popupElInvisible).toBeDefined();
        expect(haveExpandedClass).toBeTruthy();
        expect(dontHaveExpandedClass).toBeTruthy();
    });
});
