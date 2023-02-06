specs({
    feature: 'Маркер администратора',
}, function () {
    it('Маркер отображается рядом с именем администратора', async function () {
        const { browser } = this;
        const textMessage = 'Текстовое сообщение собеседника';
        const tokenAdministrator = 'Администратор';

        await browser.yaOpenMessenger({
            chatId: '0/0/41ac4ff9-573d-4d2c-8f44-853bac54ae28',
        });

        await browser.yaWaitForVisibleWithContent(PO.lastMessage.message(), textMessage, 10000, 'Отсутствует сообщение');
        await browser.yaWaitForVisibleWithContent(PO.lastMessage.message.messagesUser.role(), tokenAdministrator, 10000, 'Отсутствует маркер Администратор');
    });

    it('Маркер не отображается у участника группы, который не является администратором', async function () {
        const { browser } = this;
        const textMessage = 'Текстовое сообщение собеседника';
        const tokenAdministrator = '';

        await browser.yaOpenMessenger({
            chatId: '0/0/305d02ed-f5e5-4a20-a1d5-eb60a8ba5a60',
        });

        await browser.yaWaitForVisibleWithContent(PO.lastMessage.message(), textMessage, 10000, 'Отсутствует сообщение');
        await browser.yaWaitForVisibleWithContent(PO.lastMessage.message.messagesUser.role(), tokenAdministrator, 10000, 'Есть маркер Администратор');
    });
});
