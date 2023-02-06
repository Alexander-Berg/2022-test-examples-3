specs({
    feature: 'Контекстное меню (Переслать)',
}, function () {
    const { sendTextMessage } = require('../../shared-steps/send-message.hermione');
    const messageSelector = '.message:nth-last-child(1) .yamb-message-content';
    const actionMenuSelector = '.ui-menu';
    const forwardChatListModal = PO.modal.panel();

    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            userAlias: 'user',
            chatId: '0/0/0dbe0e4a-b5e6-4775-86b9-ce72484788d9',
        });
    });

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7489');
    it('Отмена пересылки сообщения с помощью клавиши Esc', async function () {
        const { browser, currentPlatform } = this;

        await browser.yaWaitForVisible(messageSelector, 'В чате нет текстовых сообщений');
        await sendTextMessage.call(this, 'Текст сообщения для теста');

        await browser.yaOpenMessageContextMenu(PO.lastMessage(), currentPlatform);
        await browser.yaWaitForVisible(actionMenuSelector, 'Не появилось контекстное меню');

        await browser.click(PO.messageMenuForward());
        await browser.yaWaitForHidden(actionMenuSelector);

        await browser.yaWaitForVisible(forwardChatListModal, 'Окно с выбором чатов не открылось');
        await browser.keys('Escape');
        await browser.yaWaitForHidden(forwardChatListModal, 'Окно с выбором чатов не закрылось');
    });
});
