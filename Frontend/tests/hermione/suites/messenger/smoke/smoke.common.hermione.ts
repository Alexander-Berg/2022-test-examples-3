specs({
    feature: 'Основные проверки смоука',
}, function () {
    // 'https://st.yandex-team.ru/MSSNGRFRONT-6441#5f61d98138d0aa6215478c32');
    hermione.skip.in(['chrome-pad', 'chrome-phone', 'firefox'],
        'upload files: https://st.yandex-team.ru/MSSNGRFRONT-7924');
    it('Можно создать "Групповой чат" через окно создания чата', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
        });

        // Открываем окно с визардом создания чата
        await browser.waitForVisible(PO.chatListWriteBtn());
        await browser.click(PO.chatListWriteBtn());
        await browser.waitForVisible(PO.createChatWizard(), 'Окно создания чата не появилось');

        // Выбираем пункт "Групповой чат"
        await browser.click(PO.createGroupChatBtn());
        await browser.waitForVisible(PO.createGroupChat(), 'Окно создания группового чата не появилось');

        // Вводим название и описание
        await browser.click(PO.createGroupChatName());
        await browser.setValue(PO.createGroupChatName(), 'Название группового чата');
        await browser.yaScrollIntoView(PO.createGroupChatDescription());
        await browser.click(PO.createGroupChatDescription());
        await browser.setValue(PO.createGroupChatDescription(), 'Описание группового чата');

        // Выбираем аватар
        await browser.yaScrollIntoView(PO.editableAvatar());
        await browser.chooseFile(PO.editableAvatarInput(),
            './tests/hermione/suites/messenger/smoke/test-data/group-chat-test-avatar.jpg');
        await browser.waitForVisible(PO.imageCrop(), 'Окно обрезки картинки не появилось');
        await browser.click(PO.imageCropSave());

        await browser.waitForVisible(PO.createGroupChat(), 'Окно создания группового чата не появилось');

        // Нажимаем на кнопку "Добавить участников"
        await browser.yaScrollIntoView(PO.createGroupChatAddMembers());
        await browser.click(PO.createGroupChatAddMembers());
        await browser.waitForVisible(PO.createChatSelectMembers(), 'Окно выбора участников');
        await browser.waitForVisible(PO.listItemUser(), 'Участники не появились');
        await browser.click(PO.listItemUser());
        await browser.waitForVisible(PO.usersPillsSelectedUser(), 'Участник не выбран');

        // Создаем групповой чат
        await browser.click(PO.createChatCreateBtn());

        // Ждем пока покажется новый созданный чат
        await browser.yaWaitForVisible(PO.chatWindow(), 5000, 'Окно чата не показалось');

        // Пишем в новый чат
        await browser.setValue(PO.messageEditor(), 'Привет');
        await browser.yaWaitForHidden(PO.compose.sendMessageButtonDisabled(),
            'не скрыта неактивная кнопка отправки сообщения');
        await browser.yaWaitForVisible(PO.compose.sendMessageButtonEnabled(),
            'не найдена активная кнопка отправки сообщения');
        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForNewTextMessage('Привет', { type: 'send', waitForSend: true });
    });

    /**
     * В этом тесте создается новый пользователь и создает новый чат с существующим
     * тестовым пользователем
     *
     * Как записать данный тест:
     * 1) Убрать пост месседж в инлайн скрипте .config/kotik/middlewares/inject-dynamic-hermione-asset.js
     * 2) Не удалять заголовок Cookies .config/kotik/middlewares/messenger-api.js
     * 3) Выставить флаг csrfTokenEnabled: true src/server/core/params/config.js
     * 4) Вернуть true в методе isCsrfEnabled() src/client/services/Api/requestApi.js
     * 5) Инкрементировать yndx-mssngr-test-N чтобы создался новый юзер для теста
     * 6) Запустить npm run build:dev
     */
    hermione.skip.in('chrome-pad', 'https://st.yandex-team.ru/MSSNGRFRONT-6612');
    it('Можно создать "Приватный чат" через окно создания чата (собеседник в ЛАК)', async function () {
        const { browser } = this;

        // TODO: make it work after (https://st.yandex-team.ru/MSSNGRFRONT-7924)
        // await browser.authOnRecord('yndx-mssngr-test-5');

        await browser.yaOpenMessenger({
            build: 'yamb',
            waitToken: false,
        });

        // Открываем окно с визардом создания чата
        await browser.waitForVisible(PO.chatListWriteBtn(), 'Кнопка создания чата не появилась');
        await browser.click(PO.chatListWriteBtn());
        await browser.waitForVisible(PO.createChatWizard(), 'Окно создания чата не появилось');

        // Поиск по пользователям
        await browser.click(PO.selectMembersInput());
        await browser.setValue(PO.selectMembersInput(), 'yndx-mssngr-tst-1');
        await new Promise((resolve) => setTimeout(resolve, 200));
        await browser.yaWaitForHidden('.ui-spinner', 'Спиннер не скрылся');
        await browser.waitForVisible(PO.listItemUser(), 'Пользователь не появился');

        // Выбираем пользователя
        await browser.yaScrollIntoView(PO.listItemUser());
        await browser.click(PO.listItemUser());

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
    });

    // Если надо переснять тест - нужно выйти из чата.
    it('Присоединение к чату по inviteHash', async function () {
        const INVITE_HASH = '0e71565a-3105-4f68-ac7f-bab92f9c990a';

        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: INVITE_HASH,
        });

        await browser.waitForVisible(PO.chatActionActionPanel.join(), 'Кнопка присоединения к чату не найдена');

        await browser.click(PO.chatActionActionPanel.join());

        await browser.waitForVisible(PO.compose.container.input(), 'Не удалось присоединиться к чату');
    });

    it('Удалить одного собеседника из группового чата может администратор [manual]', async function () {
        const { browser } = this;
        const INVITE_HASH = '2d809d33-37db-44e7-9d8b-b60860fda719';

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: INVITE_HASH,
        });

        await browser.click(PO.chat.header());
        await browser.waitForVisible(PO.chatInfo(), 'Окно информации о чате не открылось');

        await browser.click(PO.chatInfo.members());
        await browser.waitForVisible(PO.chatMembersList(), 'Окно списка участников не открылось');

        await browser.waitForVisible(PO.chatMembersList.member.menu(), 'Меню участника не показалось');
        await browser.click(PO.chatMembersList.member.menu());
        await browser.waitForVisible(PO.chatMemberMenuRemove(), 'Не открылось меню редактирования пользователя');

        await browser.click(PO.chatMemberMenuRemove());

        await browser.waitForVisible(PO.removeMemberConfirm(), 'Не открылось окно подтверждения удаления');

        await browser.click(PO.removeMemberConfirm());

        await browser.yaWaitForHidden(PO.removeMemberConfirm(), 'Окно подверждения не скрылось');

        await browser.yaWaitForHidden(PO.chatMembersList.member.menu(), 'Пользователь не удален');
    });

    hermione.skip.in([], 'https://st.yandex-team.ru/MSSNGRFRONT-7204');
    it('Собеседника можно заблокировать через карточку контакта', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
        });

        // Кликаем по приватному чату
        await browser.waitForVisible(PO.chatListItemPrivate(), 'Приватный чат не найден');

        await browser.click(PO.chatListItemPrivate());
        await browser.waitForVisible(PO.chatWindow(), 'Окно чата не показалось');

        await browser.click(PO.chat.header());
        await browser.waitForVisible(PO.chatInfo(), 'Информация о чате не открылась');

        await browser.yaScrollIntoView(PO.chatInfo.blockUser());

        await browser.click(PO.chatInfo.blockUser());
        await browser.waitForVisible(PO.popup(), 'Попап с подтверждением блокировки не появился');

        await browser.click(PO.confirmDialogOk());
        await browser.yaWaitForHidden(PO.modal(), 'Модальное окно не исчезло');

        await browser.click(PO.settingsButton());
        await browser.waitForVisible(PO.settingsModal(), 'Настройки не открылись');

        await browser.yaScrollIntoView(PO.settingsModal.blacklist());
        await browser.click(PO.settingsModal.blacklist());
        await browser.waitForVisible(PO.blacklistModal(), 'Список заблокированных пользователей не открылся');
        await browser.waitForVisible(PO.chatMember(), 'Не удалось заблокировать пользователя');
    });

    it('Поиск работает для запросов от 1 символа', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
        });

        await browser.yaWaitForVisible(PO.globalSearch(), 'Поле поиска не появилось');
        await browser.click(PO.globalSearch());
        await browser.setValue(PO.globalSearch(), 'D');

        await new Promise((resolve) => setTimeout(resolve, 500));
        await browser.yaWaitForVisible(PO.searchItemUser(), 'Результаты поиска не появились');

        await browser.click(PO.globalSearchClear());
        await new Promise((resolve) => setTimeout(resolve, 200));

        await browser.yaWaitForVisible(PO.chatListItem(), 'Поиск не очистился');

        await browser.setValue(PO.globalSearch(), 'De');
        await new Promise((resolve) => setTimeout(resolve, 500));
        await browser.yaWaitForVisible(PO.searchItemUser(), 'Результаты поиска 2 не появились');
    });

    hermione.skip.in(['chrome-pad', 'chrome-phone', 'firefox'],
        'firefox: https://st.yandex-team.ru/MSSNGRFRONT-7489, chrome: https://st.yandex-team.ru/MSSNGRFRONT-7924');
    it('Нельзя удалить сообщение собеседника', async function () {
        const { browser, currentPlatform } = this;

        const messageSelectors = [
            ['0889f1b2-3dd4-4dae-ac60-d8561abeeffa', PO.messageText()],
            ['953afdd9-737d-4add-a4de-b2b3f1a40148', PO.messageSticker()],
            ['4cd69567-00ec-435d-9c6d-832f99a6f4e2', PO.messageUrl()],
            ['57462bbf-245a-48c9-9cd6-362de17c2fcb', PO.messageUrl()],
            ['38f0055b-c096-4864-a25d-5e156fa39521', PO.messageForward()],
            ['7c801733-9f4a-4772-b599-55fc727e118e', PO.messageReply()],
            ['c80fd04b-6e50-4977-a358-fe8c3b2ca58c', PO.messageImage()],
            ['b2bea701-4d58-4e05-9cdc-74276f50cb63', PO.messageFile()],
            ['578d6fc0-f692-4587-afee-c8f669c7f864', PO.messageGallery()],
            ['18715ff9-c5e3-4786-b710-125a7ae9fd1c', PO.messageVoice()],
        ];

        for (const [inviteHash, messageSelector] of messageSelectors) {
            await browser.yaOpenMessenger({
                build: 'yamb',
                inviteHash,
            });
            await browser.yaOpenMessageContextMenu(messageSelector, currentPlatform);
            await browser.yaWaitForVisible(PO.popup.menu(), 'не открылось меню сообщения');
            await browser.yaWaitForHidden(PO.messageMenuDelete(), 'Пункт удалить виден');
        }
    });

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7489');
    it('Поиск по чату. Поиск работает по тексту сообщений', async function () {
        const INVITE_HASH = '61282f9f-1d9f-45fc-8107-3634682b197a';
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: INVITE_HASH,
        });

        await browser.yaWaitForVisible(PO.lastMessage());

        await browser.click(PO.chat.header.searchButton(), 'Не найдена кнопка поиска по чату');
        await browser.yaWaitForVisible(PO.chat.search.input(), 'Не появилось поле ввода запроса');

        await browser.setValue(PO.chat.search.input(), 'message 2');

        await browser.yaWaitForVisibleWithContent(
            PO.messageText.text(),
            'message 2',
            10000,
            'Сообщение message 2 не найдено',
        );

        await browser.setValue(PO.chat.search.input(), 'message 1');

        await browser.yaWaitForVisibleWithContent(
            PO.messageText.text(),
            'message 11',
            10000,
            'Сообщение message 11 не найдено',
        );

        await browser.click(PO.chat.search.prev(), 'Не найдена кнопка предыдущий результат');

        await browser.yaWaitForVisibleWithContent(
            PO.messageText.text(),
            'message 10',
            10000,
            'Сообщение message 10 не найдено',
        );

        await browser.click(PO.chat.search.prev(), 'Не найдена кнопка предыдущий результат');

        await browser.yaWaitForVisibleWithContent(
            PO.messageText.text(),
            'message 1',
            10000,
            'Сообщение message 1 не найдено',
        );

        await browser.click(PO.chat.search.next(), 'Не найдена кнопка предыдущий результат');

        await browser.yaWaitForVisibleWithContent(
            PO.messageText.text(),
            'message 10',
            10000,
            'Сообщение message 10 не найдено',
        );
    });

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7489');
    it('При клике по текстовому сообщению в Reply нас скроллит до цитируемого текстового сообщения', async function () {
        const inviteHash = '61282f9f-1d9f-45fc-8107-3634682b197a';
        const timestamp = '1610616759257009';
        const messageText = 'reply';
        const { browser, currentPlatform } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash,
            timestamp,
        });

        const messageSelector = await browser.yaGetContainsSelector(PO.messageText.text(), 'message 8');

        await browser.yaWaitForVisible(messageSelector, 'Сообщение message 8 не найдено');

        await browser.yaOpenMessageContextMenu(messageSelector, currentPlatform);
        await browser.yaWaitForVisible(PO.popup.menu(), 'не открылось меню сообщения');

        await browser.click(PO.messageMenuReply(), 'Пункт ответить не найден');

        await browser.yaWaitForVisible(PO.compose.quote(), 'Реплай не прикрепился');

        await browser.setValue(PO.compose.container.input(), messageText);
        await browser.click(PO.compose.sendMessageButtonEnabled());

        await browser.yaWaitForNewTextMessage(messageText, { type: 'send', waitForSend: true });

        await browser.click(PO.lastMessage.reply(), 'Не найден реплай в последнем сообщении');

        await browser.yaWaitForVisibleWithContent(
            PO.messageText.text(),
            'message 8',
            10000,
            'Сообщение message 8 не найдено',
        );
    });
});
