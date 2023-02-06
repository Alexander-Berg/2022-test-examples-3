specs({
    feature: 'Карточка контакта, переходы внутри',
}, function () {
    const notificationsPropertySelector = '[data-test-tag=chat-info-notifications]';
    const chatListItemBadgeSelector = '.yamb-chat-list-item-block:first-child .yamb-chat-list-item__badge';
    const sendMessageCardButtonSelector = '[data-test-tag=chat-info-send-message-button]';
    const selfMemberItemSelector = '[data-test-tag=chat-members-list-admin__name][title="yndx-mssngr-tst-1 a."]';
    const chatHeaderTitle = PO.chat.header.title();
    const followersButton = '[data-test-tag=chat-info-followers]';

    afterEach(async function () {
        await this.browser.yaCheckClientErrors();
    });

    it('Отключение уведомлений о сообщениях по тумблеру', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.yaOpenContactCardFromTitle();
        await browser.yaWaitForVisible(notificationsPropertySelector, 'Не виден переключатель уведомлений в карточке контакта');

        await browser.click(PO.tumbler());
        await browser.assertView('notifications', notificationsPropertySelector);

        await browser.keys('Escape');
        await browser.keys('Escape');
        await browser.assertView('dynamic', chatListItemBadgeSelector);
    });

    it('Переход через карточку контакта из группового чата в личный', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/1e534f22-99d3-49d0-b8cb-2eeaed958657',
        });

        await browser.click(PO.lastMessage.user());
        await browser.yaWaitForVisible('[data-test-tag=chat-info]', 'Не появилась карточка контакта');

        await browser.yaWaitForVisible(sendMessageCardButtonSelector, 'Не появилась кнопка отправки сообщения');
        await browser.click(sendMessageCardButtonSelector);

        await browser.yaWaitForVisible(chatHeaderTitle, 'Не появился заголовок чата с пользователем');
        const titleText = await browser.getText(chatHeaderTitle);
        assert.equal(titleText, 'Def-Имя-autotests D.');
    });

    it('Отправка сообщения из карточки контакта самому себе', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/1e534f22-99d3-49d0-b8cb-2eeaed958657',
        });

        await browser.click(PO.chat.header.menuButton());
        await browser.yaWaitForVisible(PO.popup.menu(), 'Не появилось контекстое меню чата');

        // Нажатие на Посмотреть информацию
        await browser.click(PO.popup.menu.item1());
        await browser.yaWaitForVisible(PO.modal(), 'Не появилось окно информации о чате');

        await browser.click(followersButton);

        await browser.yaWaitForVisible(selfMemberItemSelector, 'Не появился юзер в списке участников');
        await browser.click(selfMemberItemSelector);

        await browser.yaWaitForVisible(sendMessageCardButtonSelector, 'Не появилась кнопка отправки сообщения');
        await browser.click(sendMessageCardButtonSelector);

        const titleText = await browser.getText(chatHeaderTitle);
        assert.equal(titleText, 'Сохранённые сообщения');
    });
});
