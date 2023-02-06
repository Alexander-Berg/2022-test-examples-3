export async function hoverState(browser: WebdriverIO.Browser, selector: string) {
    await browser.$(selector).moveTo({ xOffset: 10, yOffset: 10 });

    return browser.assertView('hover', selector);
}
