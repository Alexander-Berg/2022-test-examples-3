export async function minWidthState(browser: WebdriverIO.Browser, selector: string) {
    // eslint-disable-next-line @typescript-eslint/no-magic-numbers
    await browser.setWindowSize(1024, 670);
    await browser.pause(100);

    return browser.assertView('minWidth', selector, { allowViewportOverflow: true });
}
