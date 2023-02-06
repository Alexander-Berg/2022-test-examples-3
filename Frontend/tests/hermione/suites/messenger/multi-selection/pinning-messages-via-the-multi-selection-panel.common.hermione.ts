specs({
    feature: 'Закрепление сообщений через панель мультивыбора',
}, function () {
    it('Можно закрепить собственное текстовое сообщение', async function () {
        const { browser, currentPlatform } = this;
        const textMessage = 'test message';

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: '3c71ea22-8033-4de3-bfdd-9d7d165145ed',
        });

        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), textMessage);
        await browser.yaWaitForHidden(PO.compose.sendMessageButtonDisabled(), 'не скрыта неактивная кнопка отправки сообщения');
        await browser.yaWaitForVisible(PO.compose.sendMessageButtonEnabled(), 'не найдена активная кнопка отправки сообщения');
        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForNewTextMessage(textMessage, { type: 'send', waitForSend: true });

        await browser.yaOpenMessageContextMenu(PO.lastMessage.message(), currentPlatform);

        if (currentPlatform === 'desktop') {
            await browser.waitForVisible(PO.popup.menu(), 'Не открылось меню сообщения');
        } else {
            await browser.waitForVisible(PO.popup(), 'Не открылось меню сообщения');
        }

        await browser.yaWaitForVisible(PO.messageMenuSelect(), 'Пункта Выбрать нет в меню');
        await browser.click(PO.messageMenuSelect());

        await browser.yaWaitForVisible(PO.messagesActionPanel.pin(), 'Кнопка Закрепить отсутствует в панели мультивыбора');
        await browser.yaWaitForVisible(PO.messagesActionPanel.cancel(), 'Кнопка закрытия режима мультивыбора не появилась');
        await browser.yaWaitForVisible(PO.messagesActionPanel.count(), 'Не появился счетчик выделенных сообщений в панели мультивыбора');
        await browser.yaWaitForHidden(PO.compose.container.input(), 'Отображается поле ввода');

        await browser.click(PO.messagesActionPanel.pin());
        await browser.yaWaitForVisible(PO.pinnedMessage(), 'Сообщение не прикрепилось к шапке');
        await browser.yaWaitForVisible(PO.pinnedMessage.remove(), 'В шапке отсутствует кнопка открепления сообщения');
    });

    it('Нельзя закрепить несколько выделенных сообщений', async function () {
        const { browser, currentPlatform } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: '819695a9-7d66-43af-a63e-c6a3b0dbc76f',
        });

        await browser.yaOpenMessageContextMenu(PO.lastMessage.message(), currentPlatform);

        if (currentPlatform === 'desktop') {
            await browser.waitForVisible(PO.popup.menu(), 'Не открылось меню сообщения');
        } else {
            await browser.waitForVisible(PO.popup(), 'Не открылось меню сообщения');
        }

        await browser.yaWaitForVisible(PO.messageMenuSelect(), 'Пункта Выбрать нет в меню');
        await browser.click(PO.messageMenuSelect());

        await browser.yaWaitForVisible(PO.messagesActionPanel.pin(), 'Кнопка Закрепить отсутствует в панели мультивыбора');
        await browser.yaWaitForVisible(PO.messagesActionPanel.cancel(), 'Кнопка закрытия режима мультивыбора не появилась');
        await browser.yaWaitForVisible(PO.messagesActionPanel.count(), 'Не появился счетчик выделенных сообщений в панели мультивыбора');
        await browser.yaWaitForHidden(PO.compose.container.input(), 'Отображается поле ввода');

        await browser.click(PO.messageSticker());
        await browser.yaWaitForHidden(PO.messagesActionPanel.pin(), 'Кнопка Закрепить есть в панели');
        await browser.yaWaitForVisible(PO.messagesActionPanel.cancel(), 'Кнопка закрытия режима мультивыбора не появилась');

        await browser.click(PO.messageSticker());
        await browser.yaWaitForVisible(PO.messagesActionPanel.pin(), 'Кнопка Закрепить отсутствует в панели мультивыбора');
    });
});
