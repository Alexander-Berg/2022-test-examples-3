describe('Настройки - оплата и история операций', () => {
    it('Без привязанных карт и покупок', async function() {
        const { browser, PO } = this;

        await browser.yaLoginReadonly();
        await browser.yaOpenPage('account/history', PO.CardSelect());
        await browser.yaAssertViewBottomSheet('plain', 'body');
    });

    it('С картой и покупками', async function() {
        const { browser, PO } = this;

        // Пользователь с одной картой, одной покупкой и одной арендой
        await browser.yaLoginReadonly('with-card');

        await browser.yaOpenPage('account/history', PO.CardSelect());
        await browser.yaAssertViewBottomSheet('plain', 'body');
    });
});
