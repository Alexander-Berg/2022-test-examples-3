specs({
    feature: 'Выбор участников группового чата',
}, function () {
    const getUserSelector = (nth) => `${PO.listItemUser()}:nth-child(${nth})`;

    async function initChatAndOpenSelectMembersList() {
        const { browser } = this;
        await browser.yaOpenMessengerAndGroupChat();

        // Вводим название и описание
        await browser.click(PO.createGroupChatName());
        await browser.setValue(PO.createGroupChatName(), 'Название группового чата не появилось');

        await browser.yaHideDeviceKeyboard(this);

        await browser.waitForVisible(PO.createGroupChat(), 'Окно создания группового чата не появилось');

        // Нажимаем на кнопку "Добавить участников"
        await browser.yaScrollIntoView(PO.createGroupChatAddMembers());
        await browser.click(PO.createGroupChatAddMembers());
        await browser.waitForVisible(PO.createChatSelectMembers(), 'Окно выбора участников не появилось');
        await browser.waitForVisible(PO.listItemUser(), 'Участники не появились');
    }

    it('Можно удалить выбранного пользователя из карусели добавляемых участников[manual]', async function () {
        const { browser } = this;

        await initChatAndOpenSelectMembersList.call(this);

        await browser.waitForVisible(PO.firstListItemUser(), 'Первый участник в списке контактов остался невидим');
        await browser.yaClick(PO.firstListItemUser());

        await browser.waitForVisible(PO.usersPillsSelectedUser(), 'Первый участник в карусели не появился');
        await browser.moveToObject(PO.usersPillsSelectedUser());

        await browser.waitForVisible(PO.usersPillsSelectedUser.removeMemberButton(), 'Кнопка удаления из карусели не появилась');
        await browser.yaClick(PO.usersPillsSelectedUser.removeMemberButton());

        const isUserInCarouselExist = await browser.isExisting(PO.usersPillsSelectedUser());

        assert.equal(isUserInCarouselExist, false);
    });

    hermione.skip.in(['firefox'], 'Бесконечная загрузка при создании чата');
    it('Можно создать чат с N собеседниками[manual]', async function () {
        const members = 5; // N > 1
        const { browser } = this;

        await initChatAndOpenSelectMembersList.call(this);

        // Добавляем участников
        for (let userPosition = 1; userPosition <= members; userPosition++) {
            const userSelector = getUserSelector(userPosition);
            await browser.yaScrollIntoView(userSelector);
            await browser.waitForVisible(userSelector, 'Участник в списке контактов остался невидим');
            await browser.yaClick(userSelector);
            await browser.waitForVisible(PO.usersPillsSelectedUser(), 'Участник в карусели не появился');
        }

        // Отображается в карусели ровно столько участиников, сколько было выбрано
        // TODO: make it work after (https://st.yandex-team.ru/MSSNGRFRONT-7924)
        // assert.equal((await browser.getText(PO.usersPillsSelectedUser())).length === members, true);

        // Создаем групповой чат
        await browser.click(PO.createChatCreateBtn());

        // Ждем пока покажется новый созданный чат
        await browser.waitForVisible(PO.chatWindow(), 'Окно чата не показалось');
    });

    it('Можно закрыть окно создания нового чата нажатием на "Крестик"[manual]', async function () {
        const { browser } = this;
        const chatName = 'Тестовый групповой чат';
        await browser.yaOpenMessengerAndGroupChat();

        await browser.waitForVisible(PO.createGroupChatName(), 'Поле ввода наименованя чата отсутсвует');
        await browser.waitForVisible(PO.createGroupChatDescription(), 'Поле ввода описания чата отсутсвует');
        await browser.waitForVisible(PO.editableAvatar(), 'Отсутсвует возможность загрузить аватар чата');

        await browser.click(PO.createGroupChatName());
        await browser.setValue(PO.createGroupChatName(), chatName);

        await browser.yaHideDeviceKeyboard(this);

        await browser.yaScrollIntoView(PO.createGroupChatAddMembers());
        await browser.click(PO.createGroupChatAddMembers());

        await browser.waitForVisible(PO.createChatSelectMembers(), 'Окно выбора участников не появилось');
        await browser.waitForVisible(PO.listItemUser(), 'Участники не появились');

        await browser.waitForVisible(PO.firstListItemUser(), 'Первый участник в списке контактов остался невидим');
        await browser.click(PO.firstListItemUser());
        await browser.waitForVisible(PO.usersPillsSelectedUser(), 'Первый участник в карусели не появился');

        await browser.yaScrollIntoView(PO.modal.toolbar.closeButton());
        await browser.click(PO.modal.toolbar.closeButton());
        await browser.waitForVisible(PO.popup.confirm(), 'Попап не появился');
        await browser.click(PO.popup.confirm.cancelButton());
        await browser.yaWaitForHidden(PO.popup.confirm(), 'Попап не закрылся');

        await browser.click(PO.modal.toolbar.closeButton());
        await browser.waitForVisible(PO.popup.confirm(), 'Попап не появился');
        await browser.click(PO.popup.confirm.submitButton());
        await browser.yaWaitForHidden(PO.popup.confirm(), 'Попап не закрылся');
        await browser.yaWaitForHidden(PO.createChatSelectMembers(), 'Модальное окно создания групового чата не закрылось');
    });
});
