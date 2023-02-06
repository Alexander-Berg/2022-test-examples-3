describe('Главный экран', () => {
    it('Открывается промостраница без залогина', async function() {
        await this.browser.yaOpenPage('', '.promo');
        await this.browser.yaAssertView('plain', 'body');
    });

    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Открывается промостраница без залогина 2', async function() {
        await this.browser.yaOpenPage('', '.promo');
        await this.browser.yaAssertView('plain', 'body');
    });
});
