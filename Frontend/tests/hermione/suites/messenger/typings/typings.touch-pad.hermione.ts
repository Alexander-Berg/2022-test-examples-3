specs({
    feature: 'Тайпинги',
}, () => {
    const typingChatList = '[data-test-tag="yamb-chat-list-item"] .yamb-typing';
    const typingChatPrivateList = '[data-test-tag="yamb-chat-list-item-private"] .yamb-typing';

    it('Проверка отсутствия тайпинга в карточке списка чатов у чата один на один', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.yaWaitForHidden(typingChatPrivateList, 'Показался тайпинг текущего чата в списке чатов');
    });

    it('Проверка тайпинга в карточке списка чатов у чата один на один', async function () {
        // Для снятия данного теста необходимо:
        // 1. залогиниться под yndx-mssngr-tst-2
        // 2. открыть chamb
        // 3. зайти в личный чат с yndx-mssngr-tst-1 ("yndx-mssngr-tst-1 a.")
        // 4. печатать в поле ввода во время снятия теста для воспроизведения тайпинга

        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        const typingText = 'печатает';

        await browser.waitForVisible(typingChatPrivateList, 'Не показался тайпинг текущего чата в списке чатов');
        assert.equal(await browser.getText(typingChatPrivateList), typingText, 'Неправильный текст тайпинга текущего чата в списке чатов');
    });

    it('Проверка отсутствия тайпинга в карточке списка чатов у группового чата', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });

        await browser.waitForVisible('.yamb-chat', 'Чат не открылся');

        await browser.yaWaitForHidden(typingChatList, 'Показался тайпинг текущего чата в списке чатов');
    });

    it('Проверка тайпинга в карточке списка чатов у группового чата', async function () {
        // Для снятия данного теста необходимо:
        // 1. залогиниться под yndx-mssngr-tst-2
        // 2. открыть chamb
        // 3. зайти в личный чат с yndx-mssngr-tst-1 ("Тест публичного чата")
        // 4. печатать в поле ввода во время снятия теста для воспроизведения тайпинга

        const { browser } = this;

        await browser.yaOpenMessenger({
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });

        await browser.waitForVisible('.yamb-chat', 'Чат не открылся');

        const name = 'Def-Имя-autotests D.';
        const typingText = `${name} печатает`;

        await browser.waitForVisible(typingChatList, 'Не показался тайпинг текущего чата в списке чатов');
        assert.equal(await browser.getText(typingChatList), typingText, 'Неправильный текст тайпинга текущего чата в списке чатов');
    });
});
