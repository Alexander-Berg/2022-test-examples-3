specs({
    feature: 'Интерфейс публичной ссылки',
}, function () {
    it('В карточке приватного чата нет секции для инвайт-линки', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.yaWaitForVisible(PO.chatHeader(), 'Отсутствует шапка чата');
        await browser.click(PO.chatHeader());
        await browser.yaWaitForVisible(PO.chatInfo(), 'Карточка контакта не открылась');
        await browser.yaWaitForHidden(PO.chatInfo.inviteLink(), 'В карточке контакта есть инвайт-ссылка');
    });
});
