specs({
    feature: 'Закрепление/открепление сообщений',
}, function () {
    const { sendTextMessage } = require('../../shared-steps/send-message.hermione');

    hermione.skip.in(/.*/, 'https://st.yandex-team.ru/MSSNGRFRONT-4987');
    it('Закрепление сообщения администратором', async function () {
        const { browser, currentPlatform } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/53a2ed75-1759-41fc-a6f4-b8012f77f372',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await sendTextMessage.call(this, 'hello');

        await browser.yaOpenMessageContextMenu(PO.lastMessage(), currentPlatform);
        await browser.yaWaitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');

        await browser.click(await browser.yaGetContainsSelector(PO.popup.menu.item(), 'Закрепить сообщение'));
        await browser.yaWaitForHidden(PO.popup.menu(), 'Контекстное меню не скрылось');
        await browser.yaWaitForVisible(PO.pinnedMessage(), 'Сообщение не прикрепилось к шапке');

        const lastMessage = await browser.getText(PO.lastMessage.text());
        assert.include(lastMessage, 'обновил(а) информацию о чате');

        await browser.assertView('pinned-message', PO.pinnedMessage());
        await browser.assertView('info-message', PO.lastMessage.text());
    });

    it('Открепление сообщения администратором', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/53a2ed75-1759-41fc-a6f4-b8012f77f372',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');
        await browser.yaWaitForVisible(PO.pinnedMessage.remove(), 'В шапке отсутствует кнопка открепления сообщения');

        await browser.click(PO.pinnedMessage.remove());
        await browser.yaWaitForVisible(PO.popup.confirm(), 'Не открылось модальное окно');

        await browser.click(await browser.yaGetContainsSelector(PO.popup.confirm.button(), 'ОК'));

        await browser.yaWaitForHidden(PO.popup.confirm(), 'Модальное окно не закрылось');
        await browser.yaWaitForHidden(PO.pinnedMessage(), 'Сообщение не открепилось');

        const lastMessage = await browser.getText(PO.lastMessage.system());
        assert.include(lastMessage, 'обновил(а) информацию о чате');
    });

    hermione.skip.in(/.*/, 'https://st.yandex-team.ru/MSSNGRFRONT-4987');
    it('Закрепление сообщения не администратором', async function () {
        const { browser, currentPlatform } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await sendTextMessage.call(this, 'hello');

        await browser.yaOpenMessageContextMenu(PO.lastMessage(), currentPlatform);
        await browser.yaWaitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');

        const menuItems = await browser.getText(PO.popup.menu.item());
        assert.notInclude(menuItems, 'Закрепить сообщение');
    });

    it('Открепление сообщения не администратором', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await browser.yaWaitForVisible(PO.pinnedMessage(), 'В шапке отсутствует прикрепленное сообщение');
        await browser.yaWaitForHidden(PO.pinnedMessage.remove(), 'Кнопка открепления сообщения отобразилась');

        await browser.assertView('pinned-message-without-remove', PO.pinnedMessage());
    });
});
