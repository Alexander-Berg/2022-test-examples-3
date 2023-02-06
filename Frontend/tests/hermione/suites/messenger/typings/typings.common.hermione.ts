specs({
    feature: 'Тайпинги',
}, () => {
    const typingHeader = '.yamb-chat-header .yamb-typing';

    it('Проверка отсутствия тайпинга в шапке чата один на один', async function () {
        // Для снятия данного теста необходимо:
        // 1. залогиниться под yndx-mssngr-tst-2
        // 2. открыть chamb
        // 3. зайти в личный чат с yndx-mssngr-tst-1 ("yndx-mssngr-tst-1 a.")
        // 4. печатать в поле ввода во время снятия теста для воспроизведения тайпинга
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.yaWaitForHidden(typingHeader, 'Показался тайпинг в шапке чата');
    });

    it('Проверка тайпинга в шапке чата один на один', async function () {
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

        await browser.waitForVisible(typingHeader, 'Не показался тайпинг в шапке чата');
        assert.equal(await browser.getText(typingHeader), typingText, 'Неправильный текст тайпинга в шапке чата');
    });

    it('Проверка отсутствия тайпинга в шапке группового чата', async function () {
        // Для снятия данного теста необходимо:
        // 1. залогиниться под yndx-mssngr-tst-2
        // 2. зайти в чат https://renderer-chat-dev.hamster.yandex.ru/chat?build=yamb&config=development#/chats/0%2F0%2F8407b3f0-b7fc-4f5f-810d-56db4a703550
        // 3. печатать в поле ввода во время снятия теста для воспроизведения тайпинга
        const { browser } = this;

        await browser.yaOpenMessenger({
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });

        await browser.waitForVisible('.yamb-chat', 'Чат не открылся');

        await browser.yaWaitForHidden(typingHeader, 'Показался тайпинг в шапке чата');
    });

    it('Проверка тайпинга в шапке группового чата', async function () {
        // Для снятия данного теста необходимо:
        // 1. залогиниться под yndx-mssngr-tst-2
        // 2. зайти в чат https://renderer-chat-dev.hamster.yandex.ru/chat?build=yamb&config=development#/chats/0%2F0%2F8407b3f0-b7fc-4f5f-810d-56db4a703550
        // 3. печатать в поле ввода во время снятия теста для воспроизведения тайпинга

        const { browser } = this;

        await browser.yaOpenMessenger({
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });

        await browser.waitForVisible('.yamb-chat', 'Чат не открылся');

        const name = 'Def-Имя-autotests D.';
        const typingText = `${name} печатает`;

        await browser.waitForVisible(typingHeader, 'Не показался тайпинг в шапке чата');
        assert.equal(await browser.getText(typingHeader), typingText, 'Неправильный текст тайпинга в шапке чата');
    });
});
