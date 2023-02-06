hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7489');
specs({
    feature: 'Контекстное меню (Переслать)',
}, function () {
    const { sendTextMessage, sendStickerMessage } = require('../../shared-steps/send-message.hermione');

    const messageSelector = '.message:nth-last-child(1) .yamb-message-balloon';
    const forwardMessageHeaderSelector = '.message:nth-last-child(1) .yamb-forwarded-messages__header';
    const chatListItemInPanelSelector = PO.forwardDialog.listItemName();
    const destinationChatName = 'ForwardTestChat';
    const closeModal = async (browser) => {
        const isVisibleBackBtn = await browser.isVisible(PO.modal.toolbar.backButton());
        if (isVisibleBackBtn) {
            await browser.click(PO.modal.toolbar.backButton());
        } else {
            await browser.click(PO.modal.toolbar.closeButton());
        }
    };

    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            userAlias: 'user',
            chatId: '0/0/0dbe0e4a-b5e6-4775-86b9-ce72484788d9',
        });
    });

    afterEach(async function () {
        await this.browser.yaCheckClientErrors();
    });

    it('Пересылка отправленного сообщения', async function () {
        const { browser, currentPlatform } = this;
        const forwardText = 'Привет';

        await browser.yaWaitForVisible(messageSelector, 'В чате нет текстовых сообщений');
        await sendTextMessage.call(this, 'Текст сообщения для теста');

        await browser.yaOpenMessageContextMenu(messageSelector, currentPlatform);
        await browser.yaWaitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');

        await browser.click(PO.messageMenuForward());
        await browser.yaWaitForHidden(PO.popup.menu());
        await browser.yaWaitForVisible(PO.modal.panel(), 'Окно с выбором чатов не открылось');

        await browser.setValue(PO.forwardDialog.search(), destinationChatName);

        await browser.pause(2000);

        const destinationChatSelector = await browser.yaGetContainsSelector(
            chatListItemInPanelSelector,
            destinationChatName,
        );

        // Ожидание закрытия клавиатуры для chrome-pad
        await browser.pause(2000);

        await browser.click(destinationChatSelector);
        await browser.yaWaitForHidden(PO.modal.panel(), 'Окно с выбором чатов не закрылось');

        await browser.yaWaitForVisible(PO.lastMessage(), 'В чате нет текстовых сообщений');
        assert.equal(await browser.getText(PO.chat.header.title()), destinationChatName, 'Чат для пересылки не открылся');
        await browser.yaWaitForVisible(PO.compose.quote(), 'Пересланное сообщение не прикрепилось');

        await browser.setValue(PO.compose.container.input(), forwardText);
        await browser.click(PO.compose.sendMessageButtonEnabled());

        await browser.yaWaitForHidden(PO.compose.quote());
        await browser.yaWaitForNewForwardMessage(forwardText, { type: 'send' });

        assert.equal(await browser.getText(forwardMessageHeaderSelector), 'Пересланное сообщение',
            'Нет корректной надписи "Пересланное сообщение"');
    });

    it('Пересылка стикера', async function () {
        const { browser, currentPlatform } = this;
        const forwardText = 'Привет';

        await browser.yaWaitForVisible(messageSelector, 'В чате нет текстовых сообщений');
        await sendStickerMessage.call(this);

        await browser.yaOpenMessageContextMenu(messageSelector, currentPlatform);
        await browser.yaWaitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');

        await browser.click(PO.messageMenuForward());
        await browser.yaWaitForHidden(PO.popup.menu());
        await browser.yaWaitForVisible(PO.modal.panel(), 'Окно с выбором чатов не открылось');

        await browser.setValue(PO.forwardDialog.search(), destinationChatName);

        const destinationChatSelector = await browser.yaGetContainsSelector(
            chatListItemInPanelSelector,
            destinationChatName,
        );
        await browser.yaWaitForVisible(destinationChatSelector, 'Не появился чат для пересылки в списке чатов');

        // Ожидание закрытия клавиатуры для chrome-pad
        await browser.pause(2000);

        await browser.click(destinationChatSelector);
        await browser.yaWaitForHidden(PO.modal.panel(), 'Окно с выбором чатов не закрылось');

        await browser.yaWaitForVisible(messageSelector, 'В чате нет текстовых сообщений');
        assert.equal(await browser.getText('.ui-toolbar__title'), destinationChatName, 'Чат для пересылки не открылся');
        await browser.yaWaitForVisible(PO.compose.quote(), 'Пересланное сообщение не прикрепилось');

        await browser.setValue(PO.compose.container.input(), forwardText);
        await browser.click(PO.compose.sendMessageButtonEnabled());

        await browser.yaWaitForHidden(PO.compose.quote());
        await browser.yaWaitForNewForwardMessage(forwardText, { type: 'send' });

        assert.equal(await browser.getText(forwardMessageHeaderSelector), 'Пересланное сообщение',
            'Нет корректной надписи "Пересланное сообщение"');
        await browser.yaWaitForVisible('.yamb-forwarded-messages__container .yamb-message-sticker',
            'Стикер не отображается в контенте пересланного сообщения');
    });

    it('Отмена пересылки сообщения', async function () {
        const { browser, currentPlatform } = this;

        await browser.yaWaitForVisible(messageSelector, 'В чате нет текстовых сообщений');
        await sendTextMessage.call(this, 'Текст сообщения для теста');

        await browser.yaOpenMessageContextMenu(messageSelector, currentPlatform);
        await browser.yaWaitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');

        await browser.click(PO.messageMenuForward());
        await browser.yaWaitForHidden(PO.popup.menu());

        await browser.yaWaitForVisible(PO.modal.panel(), 'Окно с выбором чатов не открылось');

        await closeModal(browser);
        await browser.yaWaitForHidden(PO.modal.panel(), 'Окно с выбором чатов не закрылось');
    });
});
