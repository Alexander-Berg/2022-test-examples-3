specs({
    feature: 'Работа в каналах',
}, function () {
    it('Корректный экран "Информация о канале для админа"', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: '874df3c6-08c8-4118-8fa6-5806ab1673bd',
        });

        await browser.click(PO.chat.header.title());
        await browser.yaWaitForVisible(PO.chatInfo(), 'Карточка канала не появилась');
        await browser.yaWaitForVisible(PO.chatInfo.notifications(), 'В карточке нет блока с уведолмениямиы');
        await browser.yaWaitForVisible(PO.chatInfo.settings(), 'Настройки канала отсутствуют в карточке');
        await browser.yaWaitForVisible(PO.chatInfo.inviteLink(), 'В карточке канала отсутствует инвайт-ссылка');
        await browser.yaWaitForVisible(PO.chatInfo.search(), 'В карточке канала отсутствует поиск по записям');
        await browser.yaWaitForVisible(PO.chatInfo.members(), 'В карточке канала отсутствует блок с участниками');
        await browser.yaScrollIntoView(PO.chatInfo.leave(), 'В карточке канала отсутствует кнопка Отписаться');
        await browser.yaWaitForVisible(PO.chatInfo.assetsBrowser(), 'В карточке канала отсутствует блок медиабраузером');
        await browser.yaWaitForVisible(PO.chatInfo.report(), 'В карточке канала отсутствует кнопка Пожаловаться');
        await browser.yaWaitForVisible(PO.chatInfo.leave(), 'В карточке канала отсутствует кнопка Отписаться');
    });

    it('Корректный экран "Информация о канале для подписчика"', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: '4d87c4e9-1bc8-47b3-9591-9ef92262f0f1',
        });

        await browser.yaWaitForVisible(PO.chatActionSubscriber(), 'Отсутствует панель подписчика');
        await browser.yaWaitForVisible(PO.chatActionSubscriber.shareChannel(), 'Не появилась кнопка поделиться каналом');
        await browser.yaWaitForVisible(PO.chatActionSubscriber.disableNotification(), 'Не появилась кнопка отключения уведомлений в канале');

        await browser.click(PO.chat.header.title());
        await browser.yaWaitForVisible(PO.chatInfo(), 'Карточка канала не появилась');
        await browser.yaWaitForVisible(PO.chatInfo.notifications(), 'В карточке нет блока с уведолмениямиы');
        await browser.yaWaitForVisible(PO.chatInfo.inviteLink(), 'В карточке канала отсутствует инвайт-ссылка');
        await browser.yaWaitForVisible(PO.chatInfo.search(), 'В карточке канала отсутствует поиск по записям');
        await browser.yaScrollIntoView(PO.chatInfo.leave(), 'В карточке канала отсутствует кнопка Отписаться');
        await browser.yaWaitForVisible(PO.chatInfo.assetsBrowser(), 'В карточке канала отсутствует блок медиабраузером');
        await browser.yaWaitForVisible(PO.chatInfo.report(), 'В карточке канала отсутствует кнопка Пожаловаться');
        await browser.yaWaitForVisible(PO.chatInfo.leave(), 'В карточке канала отсутствует кнопка Отписаться');
    });
});
