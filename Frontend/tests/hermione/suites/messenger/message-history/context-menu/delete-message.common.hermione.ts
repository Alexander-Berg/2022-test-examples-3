hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7489');
specs({
    feature: 'Контекстное меню (Удалить)',
}, function () {
    const actionMenu = '.ui-menu';
    const menuActionsSelector = '.ui-menu > *';
    const confirmDialog = '.yamb-confirm';
    const okBtn = '.ui-action-bar .ui-button:last-child';
    const deletedMessage = '.message:last-child > .yamb-message-system:last-child';

    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            userAlias: 'user',
        });
    });

    it('Удаление сообщения', async function () {
        const { browser, currentPlatform } = this;
        // TODO: not defined
        // @ts-ignore
        await browser.waitForVisible(PO.textMessage(), 'В чате нет текстовых сообщений');
        // @ts-ignore
        await browser.yaOpenMessageContextMenu(PO.textMessage(), currentPlatform);
        await browser.waitForVisible(actionMenu, 'Не появилось контекстное меню');
        await browser.click(await browser.yaGetContainsSelector(menuActionsSelector, 'Удалить'));
        await browser.waitForVisible(confirmDialog, 'Не появился диалог подтверждения');
        await browser.click(okBtn);
        await browser.waitForVisible(deletedMessage, 'Не появилось системное сообщение');
    });
});
