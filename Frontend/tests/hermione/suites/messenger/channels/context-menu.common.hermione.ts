specs({
    feature: 'Контекстное меню в канале',
}, function () {
    it('У администртора канала в меню есть пункт Выбрать', async function () {
        const { browser, currentPlatform } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: 'ff5a3b68-8c4b-4852-9714-9f419a8d343b',
        });

        await browser.yaOpenMessageContextMenu(PO.messageText(), currentPlatform);

        if (currentPlatform === 'desktop') {
            await browser.waitForVisible(PO.popup.menu(), 'Не открылось меню сообщения');
        } else {
            await browser.waitForVisible(PO.popup(), 'Не открылось меню сообщения');
        }

        await browser.yaWaitForVisible(PO.messageMenuSelect(), 'Пункта Выбрать нет в меню');
        await browser.click(PO.messageMenuSelect());

        await browser.yaWaitForVisible(PO.messagesActionPanel.copy(), 'Кнопка копировать отсутствует в панели мультивыбора');
        await browser.yaWaitForVisible(PO.messagesActionPanel.cancel(), 'Кнопка закрытия режима мультивыбора не появилась');
        await browser.yaWaitForVisible(PO.messagesActionPanel.count(), 'Не появился счетчик выделенных сообщений в панели мультивыбора');
        await browser.yaWaitForHidden(PO.compose.container.input(), 'Отображается поле ввода');
    });

    it('У администртора канала в меню есть пункт Редактировать', async function () {
        const { browser, currentPlatform } = this;
        const incorrectText = 'original message';
        const correctText = 'modified message';

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: '874df3c6-08c8-4118-8fa6-5806ab1673bd',
        });

        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), incorrectText);
        await browser.yaWaitForHidden(PO.compose.sendMessageButtonDisabled(), 'не скрыта неактивная кнопка отправки сообщения');
        await browser.yaWaitForVisible(PO.compose.sendMessageButtonEnabled(), 'не найдена активная кнопка отправки сообщения');
        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForNewTextMessage(incorrectText, { type: 'send', waitForSend: true });

        await browser.yaOpenMessageContextMenu(PO.lastMessage(), currentPlatform);

        if (currentPlatform === 'desktop') {
            await browser.waitForVisible(PO.popup.menu(), 'Не открылось меню сообщения');
        } else {
            await browser.waitForVisible(PO.popup(), 'Не открылось меню сообщения');
        }

        await browser.yaWaitForVisible(PO.messageMenuEdit(), 'Пункта Редактировать нет в меню');
        await browser.click(PO.messageMenuEdit());

        await browser.waitForVisible(PO.quote(), 'Не появилась плашка с редактируемым сообщением');
        assert.equal(incorrectText, await browser.getText(PO.quoteDescription()), 'В редактируемом сообщении неверный текст');

        await browser.setValue(PO.compose.input(), correctText);
        await browser.yaWaitForVisible(PO.compose.sendMessageButtonEnabled(), 'Кнопка отправки неактивна');

        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForNewTextMessage(correctText, { type: 'send', waitForSend: true });
    });

    it('У подписчика канала в меню есть пункт Выбрать', async function () {
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

        await browser.yaWaitForVisible(PO.messageMenuSelect(), 'Пункта Выбрать нет в меню');
        await browser.click(PO.messageMenuSelect());

        await browser.yaWaitForVisible(PO.messagesActionPanel.copy(), 'Кнопка копировать отсутствует в панели мультивыбора');
        await browser.yaWaitForVisible(PO.messagesActionPanel.cancel(), 'Кнопка закрытия режима мультивыбора не появилась');
        await browser.yaWaitForVisible(PO.messagesActionPanel.count(), 'Не появился счетчик выделенных сообщений в панели мультивыбора');
        await browser.yaWaitForHidden(PO.chatActionSubscriber(), 'Отображается панель пописчика');
    });

    it('У администртора канала в меню есть пункт Закрепить сообщение', async function () {
        const { browser, currentPlatform } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: 'ff5a3b68-8c4b-4852-9714-9f419a8d343b',
        });
        await browser.yaOpenMessageContextMenu(PO.messageText(), currentPlatform);

        if (currentPlatform === 'desktop') {
            await browser.waitForVisible(PO.popup.menu(), 'Не открылось меню сообщения');
        } else {
            await browser.waitForVisible(PO.popup(), 'Не открылось меню сообщения');
        }
        await browser.yaWaitForVisible(PO.messageMenuPin(), 'Пункта закрепить нет в меню');
        await browser.click(PO.messageMenuPin());

        await browser.yaWaitForVisible(PO.pinnedMessage(), 'Сообщение не было закреплено');
        await browser.yaWaitForVisible(PO.pinnedMessage.remove(), 'Нет пункта открепить в панеле закрепленных сообщений');
    });
});
