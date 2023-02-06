describe('Настройки - оплата и история операций', () => {
    it('Без привязанных карт и покупок', async function() {
        const { browser, PO } = this;

        await browser
            .authOnRecord('user')
            .yaOpenPage('account/history')
            .waitForVisible(PO.CardSelect(), 10_000)
            .assertView('plain', 'body');
    });

    it('С картой и покупками', async function() {
        const { browser, PO } = this;

        await browser
            // Пользователь с одной картой, одной покупкой и одной арендой
            .authOnRecord('with-card')
            .yaOpenPage('account/history')
            .waitForVisible(PO.CardSelect(), 10_000)
            .assertView('plain', 'body');
    });
});
