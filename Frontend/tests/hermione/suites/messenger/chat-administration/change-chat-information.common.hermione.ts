specs({
    feature: 'Изменение информации чата',
}, function () {
    it('Пользователь без администраторских прав не может редактировать чат', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });

        await browser.yaWaitForVisible(PO.chat.header.title(), 'Отсутствует шапка чата');

        await browser.click(PO.chat.header.title());
        await browser.yaWaitForVisible(PO.chatInfo(), 'Карточка чата не появилась');
        await browser.yaWaitForHidden(PO.chatInfo.settings(), 'Отображается раздел с настройками чата');
    });

    it('Пользователь без администраторских прав не может назначить администратором другого участника', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });

        await browser.yaWaitForVisible(PO.chat.header.title(), 'Отсутствует шапка чата');

        await browser.click(PO.chat.header.title());
        await browser.yaWaitForVisible(PO.chatInfo(), 'Карточка чата не появилась');
        await browser.yaWaitForVisible(PO.chatInfo.members(), 'В карточке чата отсутствует блок с участниками');

        await browser.click(PO.chatInfo.members());
        await browser.yaWaitForVisible(PO.chatMembersList(), 'Карточка участников не открылась');
        await browser.yaWaitForHidden(PO.chatMembersList.memberAdmin.menu(), 'Есть возможность вызвать меню');
        await browser.yaWaitForHidden(PO.chatMembersList.member.menu(), 'Есть возможность вызвать меню');
    });
});
