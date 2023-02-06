specs({
    feature: 'Название и описание групповго чата',
}, function () {
    const USER_GUID = 'd12f4a00-eb7b-9571-5a4e-a4732db3c8e3';

    async function testChatDescription(description: string, guid?: string) {
        const { browser } = this;
        await browser.yaOpenMessengerAndGroupChat();

        // Вводим название и описание
        await browser.yaWaitForVisible(PO.createGroupChatName());
        await browser.setValue(PO.createGroupChatName(), 'Название группового чата');
        await browser.yaScrollIntoView(PO.createGroupChatDescription());
        await browser.setValue(PO.createGroupChatDescription(), description);

        // Нажимаем на кнопку "Добавить участников"
        await browser.yaScrollIntoView(PO.createGroupChatAddMembers());
        await browser.click(PO.createGroupChatAddMembers());
        await browser.waitForVisible(PO.createChatSelectMembers(), 'Окно выбора участников не появилось');
        await browser.waitForVisible(PO.listItemUser(), 'Участники не появились');

        await browser.click(POUtils.createTestTagSelector(PO.listItemUser(), guid));
        await browser.waitForVisible(PO.usersPillsSelectedUser(), 'Участник не выбран');

        // Создаем групповой чат
        await browser.click(PO.createChatCreateBtn());

        // Ждем пока покажется новый созданный чат
        await browser.waitForVisible(PO.chatWindow(), 'Окно чата не показалось');

        // Пишем в новый чат
        await browser.setValue(PO.messageEditor(), 'Привет');
        await browser.yaWaitForHidden(PO.compose.sendMessageButtonDisabled(),
            'не скрыта неактивная кнопка отправки сообщения');
        await browser.yaWaitForVisible(PO.compose.sendMessageButtonEnabled(),
            'не найдена активная кнопка отправки сообщения');
        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForNewTextMessage('Привет', { type: 'send', waitForSend: true });

        // Открываем информацию о чате
        await browser.yaWaitForVisible(PO.chat.header.menuButton(),
            'кнопка открытия контекстного меню не отобразилась');
        await browser.click(PO.chat.header.menuButton());
        await browser.yaWaitForVisible(PO.popup.menu(), 'контекстное меню чата не открылось');
        await browser.click(PO.popup.menu.item1());
        await browser.yaWaitForVisible(PO.chatInfo(), 'Не появиласть информация о чате');

        // Проверяем описание чата
        await browser.yaWaitForVisible(PO.chatInfo.description(), 'Не появилось описание');
        assert.equal(await browser.getText(PO.chatInfo.description()), description);
    }

    /**
     * В фф сортировка пользователей на экране добавления участников отличается,
     * возможно из-за того что там 6 пользователей с одинаковыми никами.
     */
    it('Есть возможность дать описание чату[manual]', async function () {
        await testChatDescription.call(this, 'Описание группового чата', USER_GUID);
    });

    hermione.skip.in(['chrome-pad', 'chrome-phone', 'iphone']);
    it('Можно дать описание чату состоящее из 499 символов[manual]', async function () {
        await testChatDescription.call(this, '#'.repeat(499), USER_GUID);
    });

    it('Нельзя создать чат без названия[manual]', async function () {
        const { browser } = this;
        await browser.yaOpenMessengerAndGroupChat();
        // Ожидаем, что кнопка создания чата не появилась
        const isAddMemberButtonExist = await browser.isExisting(PO.createGroupChatAddMembers());
        assert.equal(isAddMemberButtonExist, false);
    });

    it('Нельзя создать чат, название которого состоит из одних пробелов[manual]', async function () {
        const { browser } = this;
        await browser.yaOpenMessengerAndGroupChat();

        // Проверяем возможность создать чат с названием из одного пробела
        await browser.waitForVisible(PO.createGroupChatName());
        await browser.setValue(PO.createGroupChatName(), ' ');
        let isAddMemberButtonExist = await browser.isExisting(PO.createGroupChatAddMembers());
        assert.equal(isAddMemberButtonExist, false);

        // Проверяем возможность создать чат с названием из трех пробелов
        await browser.setValue(PO.createGroupChatName(), '   ');
        isAddMemberButtonExist = await browser.isExisting(PO.createGroupChatAddMembers());
        assert.equal(isAddMemberButtonExist, false);
    });
});
