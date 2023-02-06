specs({
    feature: 'Шапка окна чата',
}, function () {
    it('Можно открыть "Поиск по чату", нажав на значок лупы в шапке приватного чата', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.yaWaitForVisible(PO.chat.header.searchButton(), 'В шапке приватного чата отсутствует иконка поиска');
        await browser.click(PO.chat.header.searchButton());
        await browser.yaWaitForVisible(PO.chat.search(), 'Режим поиска не был открыт');
    });

    it('Можно открыть "Поиск по чату", нажав на значок лупы в шапке группового чата', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/53a2ed75-1759-41fc-a6f4-b8012f77f372',
        });

        await browser.yaWaitForVisible(PO.chat.header.searchButton(), 'В шапке группового чата отсутствует иконка поиска');
        await browser.click(PO.chat.header.searchButton());
        await browser.yaWaitForVisible(PO.chat.search(), 'Режим поиска не был открыт');
    });
});
