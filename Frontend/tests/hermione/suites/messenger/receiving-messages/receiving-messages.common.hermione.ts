specs({
    feature: 'Получение сообщений',
}, function () {
    /*
     * Для записи теста необходимо зайти из другого браузера в соответствующий чат пользователем yndx-mssngr-tst-2
     * Пароли тут: https://yav.yandex-team.ru/secret/sec-01cx07hseqt3gwj5g77vnfse5k/explore/versions
     * Заходить тут: https://renderer-chat-dev.hamster.yandex.ru/chat?config=development#/
     */

    it('Получение сообщений в личном чате один на один', async function () {
        const { browser } = this;

        // https://renderer-chat-dev.hamster.yandex.ru/chat?config=development#/chats/84e2c95f-7a61-42d4-a546-c3c9c3ae0397_c3ac9c9b-853e-4b3b-ac8f-ac5aef28168f
        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.yaWaitForNewTextMessage('Полученное сообщение 2');
    });

    it('Получение сообщений в групповом чате', async function () {
        const { browser } = this;

        // https://renderer-chat-dev.hamster.yandex.ru/chat?config=development#/chats/0%2F0%2F8407b3f0-b7fc-4f5f-810d-56db4a703550
        await browser.yaOpenMessenger({
            chatId: '0/0/8407b3f0-b7fc-4f5f-810d-56db4a703550',
        });

        await browser.yaWaitForNewTextMessage('Полученное сообщение 2');
    });

    it('Получение файла', async function () {
        const { browser } = this;

        // https://renderer-chat-dev.hamster.yandex.ru/chat?config=development#/chats/84e2c95f-7a61-42d4-a546-c3c9c3ae0397_c3ac9c9b-853e-4b3b-ac8f-ac5aef28168f
        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.yaWaitForNewFileMessage();
    });

    it('Получение изображения', async function () {
        const { browser } = this;

        // https://renderer-chat-dev.hamster.yandex.ru/chat?config=development#/chats/84e2c95f-7a61-42d4-a546-c3c9c3ae0397_c3ac9c9b-853e-4b3b-ac8f-ac5aef28168f
        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.yaWaitForNewImageMessage();
    });

    it('Получение стикера', async function () {
        const { browser } = this;

        // https://renderer-chat-dev.hamster.yandex.ru/chat?config=development#/chats/84e2c95f-7a61-42d4-a546-c3c9c3ae0397_c3ac9c9b-853e-4b3b-ac8f-ac5aef28168f
        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.yaWaitForNewStickerMessage();
    });
});
