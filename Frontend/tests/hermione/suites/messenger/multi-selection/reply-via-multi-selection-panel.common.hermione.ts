specs({
    feature: 'Reply через мультивыбор',
}, function () {
    it('Можно ответить на собственное текстовое сообщение', async function () {
        const { browser, currentPlatform } = this;
        const attachedQuoteSelector = '.yamb-compose .yamb-quote';
        const textMessage = 'test message';
        const replyText = 'reply message';

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: 'e81cf388-bdbe-47fd-9612-a991e6dbbf57',
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

        await browser.click(PO.messageMenuSelect());
        await browser.yaWaitForVisible(PO.messagesActionPanel.reply(), 'отсутствует кнопка ответа в панели мультивыбора');
        await browser.yaWaitForHidden(PO.compose.container.input(), 'отображается поле ввода');

        await browser.click(PO.messagesActionPanel.reply());
        await browser.yaWaitForVisible(attachedQuoteSelector, 'Сообщение с ответом не прикрепилось');
        await browser.setValue(PO.compose.container.input(), replyText);

        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForHidden(attachedQuoteSelector);
        await browser.yaWaitForNewReplyMessage(replyText, { type: 'send' });
    });

    it('Нельзя ответить на несколько выделенных сообщений', async function () {
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

        await browser.yaWaitForVisible(PO.messagesActionPanel.reply(), 'Кнопка reply отсутствует в панели мультивыбора');
        await browser.yaWaitForVisible(PO.messagesActionPanel.cancel(), 'Кнопка закрытия режима мультивыбора не появилась');
        await browser.yaWaitForVisible(PO.messagesActionPanel.count(), 'Не появился счетчик выделенных сообщений в панели мультивыбора');
        await browser.yaWaitForHidden(PO.compose.container.input(), 'Отображается поле ввода');

        await browser.click(PO.messageSticker());
        await browser.yaWaitForHidden(PO.messagesActionPanel.reply(), 'Кнопка reply есть в панели');
        await browser.yaWaitForVisible(PO.messagesActionPanel.cancel(), 'Кнопка закрытия режима мультивыбора не появилась');

        await browser.click(PO.messageSticker());
        await browser.yaWaitForVisible(PO.messagesActionPanel.reply(), 'Кнопка reply отсутствует в панели мультивыбора');
    });
});
