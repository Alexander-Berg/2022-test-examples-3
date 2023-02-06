specs({
    feature: 'Карточка контакта открытие и закрытие',
}, function () {
    it('Вызов карточки из личного чата по клику на шапку', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await browser.click(PO.chat.header());
        await browser.yaWaitForVisible(PO.modal.panel(), 'Карточка не открылась');
        await browser.yaWaitForVisible(PO.chatInfo(), 'Информация о чате не появилась');
    });

    it('Вызов карточки из группового чата по клику на шапку аккаунта', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await browser.yaWaitForVisible(PO.lastMessage.user.avatar(), 'Сообщение не появилось');

        await browser.click(PO.lastMessage.user.avatar());
        await browser.yaWaitForVisible(PO.modal.panel(), 'Карточка не открылась');
        await browser.yaWaitForVisible(PO.chatInfo(), 'Информация о чате не появилась');
    });

    it('Вызов карточки из раздела Информация о чате по клику на шапку контакта', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await browser.click(PO.chat.header.menuButton());
        await browser.yaWaitForVisible(PO.popup(), 'Меню группового чата не открылось');

        await browser.click(PO.popup.menu.item1());
        await browser.yaWaitForVisible(PO.modal.panel(), 'Карточка не открылась');

        await browser.yaWaitForVisible(PO.chatInfo.members(), 'Кнопка участники не появилась');
        await browser.click(PO.chatInfo.members());
        await browser.yaWaitForVisible(PO.modal.panel(), 'Список участников не открылся');
        await browser.yaWaitForVisible(PO.modal.panel.lastNewMember(), 'Последний участник отобразился');

        const memberName = await browser.getText(PO.modal.panel.lastNewMember.title());

        await browser.click(PO.modal.panel.lastNewMember());

        await browser.waitUntil(async function () {
            const chatInfoTitle = await browser.getText(PO.chatInfo.title());

            return chatInfoTitle === memberName;
        }, 5000, 'Карточка контакта не открылась');
    });

    it('Вызов карточки контакта из меншена', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await browser.setValue(PO.compose.container.input(), '@');
        await browser.yaWaitForVisible(PO.compose.suggestsList(), 'Саджест с участниками чата не открылся');

        await browser.click(PO.compose.suggestsList.suggest3());
        const mentionText = (await browser.getText(PO.compose.container.input())).slice(1).trim();

        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForNewTextMessage(mentionText, { type: 'send' });

        await browser.click(PO.lastMessage.messageText() + ' [data-guid]');
        await browser.yaWaitForVisible(PO.modal.panel(), 'Карточка не открылась');
    });

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7490');
    it('Вызов карточки контакта по клику на добавленного собеседника', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            chatId: '0/0/461c199a-e402-4820-9b14-71714144b018',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await browser.click(PO.chat.header.menuButton());
        await browser.yaWaitForVisible(PO.popup(), 'Меню группового чата не открылось');

        await browser.click(PO.popup.menu.item3());
        await browser.yaWaitForVisible(PO.modal.addMembers(), 'Модальное окно с добавлением участников не открылось');

        const newMemberNames = await browser.getText(PO.modal.addMembers.notMembersFirst.nameMember());

        await browser.click(PO.modal.addMembers.notMembersFirst());
        await browser.yaWaitForVisible(PO.modal.addMembers.usersList(), 'Участник не выбрался');

        await browser.click(PO.modal.addMembers.joinButton());
        await browser.yaWaitForVisible(PO.lastMessage.system(), 'Не отобразилось системное сообщение');

        const lastMessage = await browser.getText(PO.lastMessage.system.text());
        assert.include(lastMessage, `добавил(а): ${newMemberNames}`);

        await browser.click(PO.lastMessage.system.text.lastLink());
        await browser.yaWaitForVisible(PO.modal.panel(), 'Карточка не открылась');
    });

    // В chrome-pad из-за большей ширины отображается крестик вместо стрелки
    hermione.only.in('chrome-pad');
    it('Закрытие карточки контакта по клику на кнопку с крестиком', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await browser.click(PO.chat.header());
        await browser.yaWaitForVisible(PO.modal.panel(), 'Карточка не открылась');

        await browser.click(PO.modal.toolbar.closeButton());
        await browser.yaWaitForHidden(PO.modal.panel(), 'Карточка не закрылась');
    });

    // На тачах карточка открывается на весь размер вьюпорта
    hermione.only.notIn(['iphone', 'chrome-phone', 'chrome-pad']);
    it('Закрытие карточки контакта при клике в сторону', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await browser.yaWaitForVisible(PO.lastMessage.userName(), 'Сообщение не появилось');

        await browser.click(PO.lastMessage.userName());
        await browser.yaWaitForVisible(PO.modal.panel(), 'Карточка не открылась');

        await browser.clickTo(PO.modal.panel(), -100, -10);
        await browser.yaWaitForHidden(PO.modal.panel(), 'Карточка не закрылась');
    });
});
