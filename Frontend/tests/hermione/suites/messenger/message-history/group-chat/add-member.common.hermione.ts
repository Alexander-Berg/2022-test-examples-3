specs({
    feature: 'Добавление участников в чат',
}, function () {
    const chatMenuButton = PO.chat.header.menuButton();
    const menuPopup = PO.popup.menu();
    const addMembersButton = PO.popup.menu.item3();
    const addMembersPopup = PO.selectMembers();
    const selectableMemberListItem = PO.selectMembers.membersList.firstSelectableUser();
    const addButton = '.ui-button_primary';
    const lastMessageText = '.message:last-child .text';
    const chatInfoButton = '.ui-menu-item:first-child';
    const followersButton = '[data-test-tag=chat-info-followers]';
    const lastChatMember = '[data-test-tag=chat-members-list-subscriber]:last-child';
    const modalPanel = PO.modal.panel();
    const popupAddMembersButton = '[data-test-tag=add-member-chat-button]';
    const input = PO.selectMembers.input();
    const addedUser = PO.selectMembers.pillsList.user1();

    afterEach(async function () {
        await this.browser.yaCheckClientErrors();
    });

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7487');
    it('Добавление участников в групповой чат из меню группового чата', async function () {
        const browser = this.browser;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/1e534f22-99d3-49d0-b8cb-2eeaed958657',
        });

        // открываем попап для добавления участников
        await browser.click(chatMenuButton);
        await browser.waitForVisible(menuPopup, 'меню чата не открылось');
        await browser.assertView('chat-menu', menuPopup, {
            allowViewportOverflow: true,
            tolerance: 5,
        });
        await browser.click(addMembersButton);
        await browser.waitForVisible(addMembersPopup, 'попап добавления участников не открылся');
        await browser.assertView('add-members-popup', modalPanel, {
            allowViewportOverflow: true,
        });

        // добавляем участника
        await browser.waitForVisible(selectableMemberListItem, 'участник не появился');
        await browser.yaScrollIntoView(selectableMemberListItem);
        await browser.click(selectableMemberListItem);
        await browser.assertView('selected-member', modalPanel, {
            allowViewportOverflow: true,
        });
        await browser.click(addButton);
        // нет специального селектора на сообщение о том, что участник добавлен в чат,
        // поэтому скриншотим последнее сообщение, для появления которого требуется некоторый таймаут
        await browser.pause(200);
        await browser.assertView('message-member-added', lastMessageText, {
            allowViewportOverflow: true,
        });

        // открываем карточку чата, проверяем, что участник добавлен
        await browser.click(chatMenuButton);
        await browser.waitForVisible(menuPopup, 'меню чата не открылось');
        await browser.click(chatInfoButton);
        await browser.waitForVisible(modalPanel, 'попап с карточкой чата не открылся');
        await browser.click(followersButton, 'список участников не открылся');
        await browser.yaScrollIntoView(lastChatMember);
        await browser.pause(200);
        await browser.assertView('chat-info', modalPanel, {
            allowViewportOverflow: true,
        });
    });

    hermione.skip.in(['firefox'], 'Бесконечная загрузка при получении участников');
    it('Добавление участников в групповой чат из модального окна информации о чате', async function () {
        const browser = this.browser;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/1e534f22-99d3-49d0-b8cb-2eeaed958657',
        });

        await browser.click(chatMenuButton);
        await browser.waitForVisible(menuPopup, 'меню чата не открылось');
        await browser.click(chatInfoButton);
        await browser.waitForVisible(modalPanel, 'попап с карточкой чата не открылся');
        await browser.click(followersButton);
        await browser.pause(200);
        await browser.waitForVisible(popupAddMembersButton);
        await browser.click(popupAddMembersButton);

        // добавляем участника
        await browser.waitForVisible(addMembersPopup, 'попап добавления участников не открылся');
        await browser.waitForVisible(selectableMemberListItem, 'участник не появился');
        await browser.yaScrollIntoView(selectableMemberListItem);
        await browser.click(selectableMemberListItem);
        await browser.waitForVisible(addedUser, 'участник не был добавлен в список добавленных');
        await browser.waitForVisible(addButton, 'Кнопка добавления участника не показалась');
        await browser.click(addButton);
        // нет специального селектора на сообщение о том, что участник добавлен в чат,
        // поэтому скриншотим последнее сообщение, для появления которого требуется некоторый таймаут
        await browser.pause(200);
        await browser.assertView('message-member-added-modal', lastMessageText);
    });

    hermione.skip.in(/.*/, 'Плавающий тест');
    it('Поиск пользователей в модальном окне добавления участников в групповой чат', async function () {
        const browser = this.browser;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/1e534f22-99d3-49d0-b8cb-2eeaed958657',
        });

        // открываем попап для добавления участников
        await browser.click(chatMenuButton);
        await browser.waitForVisible(menuPopup, 'меню чата не открылось');
        await browser.click(addMembersButton);
        await browser.waitForVisible(addMembersPopup, 'попап добавления участников не открылся');

        // вводим текст в поле поиска
        await browser.setValue(input, 'Дос');
        await browser.pause(650); // требуется время чтобы список участников отреагировал на введенный текс
        await browser.assertView('add-members-popup', modalPanel);
    });

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7488');
    it('Отмена добавления участников в чат', async function () {
        const browser = this.browser;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/53a2ed75-1759-41fc-a6f4-b8012f77f372',
        });

        // открываем попап для добавления участников
        await browser.click(chatMenuButton);
        await browser.waitForVisible(menuPopup, 'меню чата не открылось');
        await browser.click(addMembersButton);
        await browser.waitForVisible(addMembersPopup, 'попап добавления участников не открылся');

        // закрываем модалку
        await browser.yaCloseModal(browser);
        await browser.waitForVisible(addMembersPopup, 500, true);
    });

    hermione.skip.in([], 'https://st.yandex-team.ru/MSSNGRFRONT-7204');
    it('Отправление ссылки на публичный чат другому пользователю', async function () {
        const browser = this.browser;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/53a2ed75-1759-41fc-a6f4-b8012f77f372',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await browser.click(PO.chat.header.menuButton());
        await browser.yaWaitForVisible(PO.popup.menu(), 'Меню чата не открылось');

        await browser.click(PO.popup.menu.item1());
        await browser.yaWaitForVisible(PO.modal(), 'Модальное окно информации о чате не открылось');
        await browser.yaWaitForVisible(PO.chatInfo(), 'Информация о чате не появилась');

        await browser.click(PO.chatInfo.settings());
        await browser.yaWaitForVisible(PO.chatSettings(), 'Настройки чата не открылись');
        await browser.click(PO.chatSettings.publicSwitchOff());
        await browser.yaWaitForVisible(PO.chatSettings.publicSwitchOn(), 'Приватность чата не переключилась');
        await browser.click(PO.chatSettings.save());
        await new Promise((resolve) => setTimeout(resolve, 3000));
        await browser.yaWaitForVisible(PO.chatInfo.inviteLink(), 'Ссылка публичного доступа не появилась');

        const inviteLink = await browser.getText(PO.chatInfo.inviteLink.link());

        await browser.click(PO.chatInfo.inviteLink());
        await browser.yaWaitForVisible(PO.infoMessage.content(), 'Информационное сообщение не появилось');

        await browser.yaCloseModal(browser);

        const isVisibleSidebar = await browser.isVisible(PO.sidebar());
        if (!isVisibleSidebar) {
            await browser.click(PO.chat.header.backButton());
            await browser.yaWaitForVisible(PO.sidebar(), 'Сайдбар с контактами не появился');
        }

        await browser.click(PO.chatListItemPrivate());
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await browser.setValue(PO.compose.container.input(), inviteLink);
        await browser.click(PO.compose.sendMessageButtonEnabled());

        const selector = `[data-test-tag="url-preview-${inviteLink}"]`;
        await browser.yaWaitForVisible(selector, `Не найдено новое сообщение с ссылкой на чат "${inviteLink}"`);
    });

    it('Присоединение к чату по публичной ссылке', async function () {
        const browser = this.browser;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: 'e5ce1c2d-9b1a-413b-9f24-4e19c6308697',
        });

        await browser.waitForVisible(PO.chat.joinButton());
        const joinButtonText = await browser.getText(PO.chat.joinButton());
        assert.equal(joinButtonText, 'Присоединиться', 'Неверный текст на кнопке присоединения к чату');

        await browser.click(PO.chat.joinButton());
        await browser.yaWaitForVisible(PO.compose.container.input(), 'Поле ввода не появилось');
    });
});
