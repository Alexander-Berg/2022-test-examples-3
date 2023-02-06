describe('Настройки - режим поиска', () => {
    it('Страница открывается', async function() {
        const { browser, PO } = this;

        await browser
            .authOnRecord('user')
            .yaOpenPage('account/content-access')
            .waitForVisible(PO.ContentAccessPage(), 20_000)
            .assertView('plain', 'body');
    });

    it('Перещелкивается таб детскости', async function() {
        const { browser, PO } = this;

        // Открываем страницу
        await browser
            .authOnRecord('user')
            .yaOpenPage('account/content-access')
            .waitForVisible(PO.ContentAccessPage(), 20_000)

            // Кликаем на первый элемент в списке
            .click(PO.ContentAccessVoiceTabChild())
            .pause(1_000)

            // Кликаем на второй элемент в списке
            .waitForVisible(PO.ContentAccessPage(), 20_000)
            .assertView('plain', 'body');
    });
});
