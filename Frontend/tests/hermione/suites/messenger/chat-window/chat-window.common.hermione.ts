specs({
    feature: 'Окно чата',
}, function () {
    it('У сообщений пользователя нет аватарок в приватном чате', async function () {
        const { browser } = this;
        const textMessage = 'Собственное текстовое сообщение';

        await browser.yaOpenMessenger({
            chatId: '7ced3800-3de7-9754-67cd-33a88f8e5084_84e2c95f-7a61-42d4-a546-c3c9c3ae0397',
        });

        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), textMessage);
        await browser.yaWaitForHidden(PO.compose.sendMessageButtonDisabled(), 'не скрыта неактивная кнопка отправки сообщения');
        await browser.yaWaitForVisible(PO.compose.sendMessageButtonEnabled(), 'не найдена активная кнопка отправки сообщения');
        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForNewTextMessage(textMessage, { type: 'send', waitForSend: true });

        await browser.yaWaitForHidden(PO.lastMessage.user.avatar(), 'Есть аватар у пользователя');
    });

    it('У сообщений собеседника нет аватарок в приватном чате', async function () {
        const { browser } = this;
        const textMessage = 'Текстовое сообщение собеседника';

        await browser.yaOpenMessenger({
            chatId: '7ced3800-3de7-9754-67cd-33a88f8e5084_84e2c95f-7a61-42d4-a546-c3c9c3ae0397',
        });

        await browser.yaWaitForVisibleWithContent(PO.lastMessage.message(), textMessage, 5000, 'Отсутствует сообщение от другого пользователя');
        await browser.yaWaitForHidden(PO.lastMessage.user.avatar(), 'Есть аватар у пользователя');
    });

    it('У сообщений собеседника есть аватарка в групповом чате', async function () {
        const { browser } = this;
        const textMessage = 'Текстовое сообщение собеседника';

        await browser.yaOpenMessenger({
            chatId: '0/0/41ac4ff9-573d-4d2c-8f44-853bac54ae28',
        });

        await browser.yaWaitForVisibleWithContent(PO.lastMessage.message(), textMessage, 5000, 'Отсутствует сообщение');
        await browser.yaWaitForVisible(PO.lastMessage.user.avatar(), 'Отсутствует аватар у пользователя');
    });

    it('У сообщений пользователя нет аватарки в групповом чате', async function () {
        const { browser } = this;
        const textMessage = 'Текстовое сообщение';

        await browser.yaOpenMessenger({
            inviteHash: 'e81cf388-bdbe-47fd-9612-a991e6dbbf57',
        });

        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), textMessage);
        await browser.yaWaitForHidden(PO.compose.sendMessageButtonDisabled(), 'не скрыта неактивная кнопка отправки сообщения');
        await browser.yaWaitForVisible(PO.compose.sendMessageButtonEnabled(), 'не найдена активная кнопка отправки сообщения');
        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForNewTextMessage(textMessage, { type: 'send', waitForSend: true });

        await browser.yaWaitForHidden(PO.lastMessage.user.avatar(), 'Отсутствует аватар у пользователя');
    });
});
