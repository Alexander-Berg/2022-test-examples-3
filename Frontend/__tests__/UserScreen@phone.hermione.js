describe('UserScreen', function() {
    it('Внешний вид экрана пользователя', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'user',
            query: { patch: 'setBlackboxData' },
        });

        await browser.yaWaitForVisible('.UserScreen');
        await browser.assertView('plain', '.UserScreen');
    });
});
