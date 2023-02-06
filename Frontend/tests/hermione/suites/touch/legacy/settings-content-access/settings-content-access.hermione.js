describe('Настройки - режим поиска', () => {
    it('Страница открывается', async function() {
        const { browser, PO } = this;

        await browser.yaLoginReadonly();
        await browser.yaOpenPage('account/content-access', PO.ContentAccessPage());
        await browser.yaAssertViewBottomSheet('plain', 'body');
    });

    it('Перещелкивается таб детскости', async function() {
        const { browser, PO } = this;

        // Открываем страницу
        await browser.yaLoginReadonly();

        await browser.yaDeleteUserStorage();

        await browser.yaOpenPage('account/content-access', PO.ContentAccessPage());

        // Кликаем на первый элемент в списке
        await browser.click(PO.ContentAccessVoiceTabChild());

        await browser.pause(1_000);

        // Кликаем на второй элемент в списке
        await browser.waitForVisible(PO.ContentAccessPage(), 20_000);

        await browser.yaAssertViewBottomSheet('plain', 'body');
    });
});
