specs({
    feature: 'Возможности подпиcчика канала',
}, function () {
    it('Подписчик канала не может отправлять сообщения в канал', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: '4d87c4e9-1bc8-47b3-9591-9ef92262f0f1',
        });

        await browser.yaWaitForVisible(PO.chatActionSubscriber(), 'Отсутствует панель подписчика');
        await browser.yaWaitForVisible(PO.chatActionSubscriber.shareChannel(), 'Не появилась кнопка поделиться каналом');
        await browser.yaWaitForVisible(PO.chatActionSubscriber.disableNotification(), 'Не появилась кнопка отключения уведомлений в канале');
        await browser.yaWaitForHidden(PO.compose.container.input(), 'Отображается поле ввода');
    });

    it('Участнику канала не доступен список участников канала', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: '4d87c4e9-1bc8-47b3-9591-9ef92262f0f1',
        });

        await browser.yaWaitForVisible(PO.chatActionSubscriber(), 'Отсутствует панель подписчика');
        await browser.yaWaitForVisible(PO.chatActionSubscriber.shareChannel(), 'Не появилась кнопка поделиться каналом');
        await browser.yaWaitForVisible(PO.chatActionSubscriber.disableNotification(), 'Не появилась кнопка отключения уведомлений в канале');
        await browser.yaWaitForHidden(PO.compose.container.input(), 'Отображается поле ввода');
        await browser.click(PO.chat.header.title());
        await browser.yaWaitForVisible(PO.chatInfo(), 'Карточка канала не появилась');
        await browser.yaWaitForHidden(PO.chatInfo.members(), 'Отображается раздел с участниками канала');
    });

    it('Подписчик канала  не может сделать Reply на сообщение через контекстное меню', async function () {
        const { browser, currentPlatform } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: '4d87c4e9-1bc8-47b3-9591-9ef92262f0f1',
        });

        await browser.yaWaitForVisible(PO.chatActionSubscriber(), 'Отсутствует панель подписчика');
        await browser.yaWaitForVisible(PO.chatActionSubscriber.shareChannel(), 'Не появилась кнопка поделиться каналом');
        await browser.yaWaitForVisible(PO.chatActionSubscriber.disableNotification(), 'Не появилась кнопка отключения уведомлений в канале');
        await browser.yaWaitForHidden(PO.compose.container.input(), 'Отображается поле ввода');

        await browser.yaOpenMessageContextMenu(PO.messageText(), currentPlatform);

        if (currentPlatform === 'desktop') {
            await browser.waitForVisible(PO.popup.menu(), 'Не открылось меню сообщения');
        } else {
            await browser.waitForVisible(PO.popup(), 'Не открылось меню сообщения');
        }

        await browser.yaWaitForHidden(PO.messageMenuReply(), 'Пункт Ответить есть в меню');
    });

    it('Подписчик может сделать поиск по каналу', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: 'f8333556-6011-4ee0-b05c-d528848dbe91',
        });

        await browser.yaWaitForVisible(PO.chatActionSubscriber(), 'Отсутствует панель подписчика');
        await browser.yaWaitForVisible(PO.chatActionSubscriber.shareChannel(), 'Не появилась кнопка поделиться каналом');
        await browser.yaWaitForVisible(PO.chatActionSubscriber.disableNotification(), 'Не появилась кнопка отключения уведомлений в канале');
        await browser.yaWaitForVisible(PO.chat.header.searchButton(), 'В шапке канала отсутствует иконка поиска');

        await browser.click(PO.chat.header.searchButton());
        await browser.yaWaitForVisible(PO.chat.search(), 'Режим поиска не был открыт');

        await browser.setValue(PO.chat.search.input(), 'some text');

        await browser.yaWaitForVisibleWithContent(
            PO.messageText.text(),
            'some text',
            10000,
            'Сообщение some text не найдено',
        );
    });
});
