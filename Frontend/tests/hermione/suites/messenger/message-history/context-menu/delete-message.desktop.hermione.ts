specs({
    feature: 'Контекстное меню (Удалить)',
}, function () {
    const actionMenu = '.ui-menu';
    const confirmDialog = '.yamb-confirm';

    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            userAlias: 'user',
        });
    });

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7489');
    it('Отмена удаления сообщения с помощью клавиши Esc', async function () {
        const { browser, currentPlatform } = this;

        await browser.waitForVisible(PO.lastMessage2(), 'В чате нет текстовых сообщений');

        // await browser.assertView(PO.lastMessage2(), 'some test');

        await browser.yaOpenMessageContextMenu(PO.lastMessage2(), currentPlatform);
        await browser.waitForVisible(actionMenu, 'Не появилось контекстное меню');
        await browser.click(PO.messageMenuDelete());
        await browser.waitForVisible(confirmDialog, 'Не появился диалог подтверждения');
        await browser.keys('Escape');
        await browser.yaWaitForHidden(confirmDialog);
    });
});
