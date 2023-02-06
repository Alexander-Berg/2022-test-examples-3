specs({
    feature: 'Контекстное меню для списка чатов',
}, function () {
    // При перезаписывании дампов к этому тесту убедитесь, что
    // чат Мебельбург является одним из первых чатов (или хотя бы
    // находится во вьюпорте на тачах), иначе будут падать тесты на chrome-phone,
    // потому что если элемент находится далеко за пределами вьюпорта, то
    // гермиона не находит его по селектору (по селекторам доступны только первые 13 чатов в списке)
    // и не может к нему подскроллиться
    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7495');
    it('Контекстное меню чата c бизнесом', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger();
        await browser.yaWaitForVisible(PO.chatListItemName(), 'Не дождались показа списка чатов');
        const chatSelector = await browser.yaGetContainsSelector(PO.chatListItemName(), 'Мебельбург');
        await browser.yaScrollIntoView(chatSelector);
        await browser.yaWaitForVisible(chatSelector, 'Не появился чат Мебельбург в списке чатов');
        await browser.rightClick(`${PO.chatListItemPrivate()} ${chatSelector}`);
        await browser.yaWaitForVisible(PO.popup.menu(), 'не открылось меню чата');
        await browser.assertView('chat-business-menu', PO.popup.menu());
    });

    it('Контекстное меню чата 1x1', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger();
        await browser.yaWaitForVisible(PO.chatListItemName(), 'Не долждались показа списка чатов');
        const chatSelector = await browser.yaGetContainsSelector(PO.chatListItemName(), 'Def-Имя-autotests D.');
        await browser.rightClick(`${PO.chatListItemPrivate()} ${chatSelector}`, 0, 0);
        await browser.yaWaitForVisible(PO.popup.menu(), 'не открылось меню чата');
        await browser.assertView('chat-private-menu', PO.popup.menu());
    });

    it('Контекстное меню группового чата', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger();
        await browser.yaWaitForVisible(PO.chatListItemName(), 'Не долждались показа списка чатов');
        const chatSelector = await browser.yaGetContainsSelector(PO.chatListItemName(), 'pin-message-not-admin');
        await browser.yaScrollIntoView(chatSelector);
        await browser.rightClick(`${PO.chatListItem()} ${chatSelector}`);
        await browser.yaWaitForVisible(PO.popup.menu(), 'не открылось меню чата');
        await browser.assertView('chat-group-menu', PO.popup.menu(), {
            invisibleElements: ['.yamb-root'],
            allowViewportOverflow: true,
        });
    });
});
