describe('Главный экран', () => {
    it('Открывается промостраница без залогина', async function() {
        await this.browser.yaOpenPage('');
        await this.browser.waitForVisible('.promo', 10_000);
        await this.browser.assertView('plain', 'body');
    });
});
