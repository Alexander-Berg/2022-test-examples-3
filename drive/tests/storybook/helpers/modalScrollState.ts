export async function modalScrollState(browser: WebdriverIO.Browser, selector: string) {
    await browser.execute(() => document.body.classList.add('sb-hermione-modal-scroll'));

    return browser.assertView('plain', selector);
}
