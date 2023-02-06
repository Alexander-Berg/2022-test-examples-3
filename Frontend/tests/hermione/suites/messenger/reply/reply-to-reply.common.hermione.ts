specs({
    feature: 'Reply на Reply',
}, function () {
    hermione.skip.in(['chrome-phone', 'iphone', 'ipad'],
        'openMessageContextMenu осуществляет тап по элементу, а не области его и как результат не вызвается КМ');
    it('На Reply c текстом можно еще раз ответить', async function () {
        const { browser, currentPlatform } = this;
        const attachedQuoteSelector = '.yamb-compose .yamb-quote';
        const textMessage = 'test message';
        const replyText = 'reply message';
        const replyToReplyText = 'reply to reply message';

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

        await browser.click(PO.messageMenuReply());

        await browser.yaWaitForVisible(attachedQuoteSelector, 'Сообщение с ответом не прикрепилось');
        await browser.setValue(PO.compose.container.input(), replyText);

        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForHidden(attachedQuoteSelector);
        await browser.yaWaitForNewReplyMessage(replyText, { type: 'send' });

        await browser.yaOpenMessageContextMenu(PO.lastMessage.message(), currentPlatform);

        if (currentPlatform === 'desktop') {
            await browser.waitForVisible(PO.popup.menu(), 'Не открылось меню сообщения');
        } else {
            await browser.waitForVisible(PO.popup(), 'Не открылось меню сообщения');
        }

        await browser.click(PO.messageMenuReply());

        await browser.yaWaitForVisible(attachedQuoteSelector, 'Сообщение с ответом не прикрепилось');
        await browser.setValue(PO.compose.container.input(), replyToReplyText);

        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForHidden(attachedQuoteSelector);
        await browser.yaWaitForNewReplyMessage(replyToReplyText, { type: 'send' });
    });
});
