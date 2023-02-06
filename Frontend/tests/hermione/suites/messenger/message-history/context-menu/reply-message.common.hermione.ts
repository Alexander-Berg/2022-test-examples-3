hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7489');
specs({
    feature: 'Контекстное меню (Ответить)',
}, function () {
    const { sendTextMessage, sendStickerMessage } = require('../../shared-steps/send-message.hermione');
    const messageSelector = '.message:nth-last-child(1) .yamb-message-content';
    const quoteDescriptionSelector = '.message:nth-last-child(1) .yamb-message-reply__description';
    const attachedQuoteSelector = '.yamb-compose .yamb-quote';
    const quoteCancelSelector = '.yamb-quote__cancel';
    const composeTextareaSelector = 'textarea.ui-textarea__control';
    const actionMenuSelector = '.ui-menu';
    const sendBtnSelector = '.yamb-compose-submit-button';

    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            userAlias: 'user',
            chatId: '0/0/0dbe0e4a-b5e6-4775-86b9-ce72484788d9',
        });
    });

    it('Пустой ответ на сообщение', async function () {
        const { browser, currentPlatform } = this;

        await browser.yaWaitForVisible(messageSelector, 'В чате нет текстовых сообщений');
        await sendTextMessage.call(this, 'Текст сообщения для теста');

        await browser.yaOpenMessageContextMenu(messageSelector, currentPlatform);
        await browser.yaWaitForVisible(actionMenuSelector, 'Не появилось контекстное меню');

        await browser.click(PO.messageMenuReply());
        await browser.yaWaitForHidden(actionMenuSelector);

        await browser.yaWaitForVisible(attachedQuoteSelector, 'Сообщение с ответом не прикрепилось');

        await browser.click(sendBtnSelector);
        await browser.yaWaitForHidden(attachedQuoteSelector);
        await browser.yaWaitForNewForwardMessage('', { type: 'send', reply: true });
    });

    it('Ответ с текстом на сообщение', async function () {
        const { browser, currentPlatform } = this;
        const replyText = 'Привет';

        await browser.yaWaitForVisible(messageSelector, 'В чате нет текстовых сообщений');
        await sendTextMessage.call(this, 'Текст сообщения для теста');

        await browser.yaOpenMessageContextMenu(messageSelector, currentPlatform);
        await browser.yaWaitForVisible(actionMenuSelector, 'Не появилось контекстное меню');

        await browser.click(PO.messageMenuReply());
        await browser.yaWaitForHidden(actionMenuSelector);

        await browser.yaWaitForVisible(attachedQuoteSelector, 'Сообщение с ответом не прикрепилось');

        await browser.setValue(composeTextareaSelector, replyText);
        await browser.click(sendBtnSelector);
        await browser.yaWaitForHidden(attachedQuoteSelector);

        await browser.yaWaitForNewReplyMessage(replyText, { type: 'send' });
    });

    hermione.skip.in([], 'https://st.yandex-team.ru/MSSNGRFRONT-7204');
    it('Ответ на отправленный стикер', async function () {
        const { browser, currentPlatform } = this;

        const replyText = 'Привет';

        await browser.yaWaitForVisible(PO.lastMessage.balloonContent(), 'В чате нет текстовых сообщений');
        await sendStickerMessage.call(this);

        await browser.yaOpenLastMessageContextMenu(currentPlatform);
        await browser.yaWaitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');

        await browser.click(PO.messageMenuReply());
        await browser.yaWaitForHidden(PO.popup.menu());

        await browser.yaWaitForVisible(PO.quote(), 'Сообщение с ответом не прикрепилось');

        await browser.setValue(PO.compose.container.input(), replyText);
        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForHidden(PO.quote());

        await browser.yaWaitForNewReplyMessage(replyText, { type: 'send' });
        assert.equal(await browser.getText(quoteDescriptionSelector), 'Стикер',
            'Нет корректной надписи "Стикер"');
    });

    it('Отмена ответа с введенным текстом в поле ввода', async function () {
        const { browser, currentPlatform } = this;
        const text = 'Привет';

        await browser.yaWaitForVisible(messageSelector, 'В чате нет текстовых сообщений');
        await sendTextMessage.call(this, 'Текст сообщения для теста');

        await browser.yaOpenMessageContextMenu(messageSelector, currentPlatform);
        await browser.yaWaitForVisible(actionMenuSelector, 'Не появилось контекстное меню');

        await browser.click(PO.messageMenuReply());
        await browser.yaWaitForHidden(actionMenuSelector);

        await browser.yaWaitForVisible(attachedQuoteSelector, 'Сообщение с ответом не прикрепилось');

        assert(!(await browser.getValue(composeTextareaSelector)), 'Поле с вводом сообщения не пустое');
        await browser.setValue(composeTextareaSelector, text);

        await browser.click(quoteCancelSelector);

        await browser.yaWaitForHidden(attachedQuoteSelector, 'Сообщение с ответом не открепилось');
        assert.equal(await browser.getValue(composeTextareaSelector), text, 'Поле с вводом сообщения не сохранилось');
    });
});
