specs({
    feature: 'Возможности админа канала',
}, function () {
    it('Администратор может отправлять текстовые  сообщения в канал', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: 'ff5a3b68-8c4b-4852-9714-9f419a8d343b',
        });
        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), 'some text');
        await browser.yaWaitForHidden(PO.compose.sendMessageButtonDisabled(), 'не скрыта неактивная кнопка отправки сообщения');
        await browser.yaWaitForVisible(PO.compose.sendMessageButtonEnabled(), 'не найдена активная кнопка отправки сообщения');
        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForNewTextMessage('some text', { type: 'send', waitForSend: true });
    });

    it('Администратор может сделать поиск сообщения по каналу', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: '874df3c6-08c8-4118-8fa6-5806ab1673bd',
        });

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
