hermione.skip.in([], 'https://st.yandex-team.ru/MSSNGRFRONT-7204');
specs({
    feature: 'Подскролл к сообщению',
}, function () {
    async function openAndAssert(paramsOpen, expectedAssert) {
        const { browser } = this;
        await browser.yaOpenMessenger(paramsOpen);
        await browser.yaWaitForVisible(PO.messageText(), 'Не появились сообщения');
        await browser.yaWaitForHidden(PO.floatingDateVisible(), 'Дата не исчезла');

        await browser.assertView(expectedAssert, PO.conversation());
    }

    it('Происходит переход к сообщению по линке, если оно не глубоко в истории и мы состоим в данном чате', async function () {
        const params = {
            inviteHash: '5e7b9e75-bd73-4480-8719-151505b1bf11',
            timestamp: '1580213541311016',
        };

        await openAndAssert.call(this, params, 'scrollToHere');
    });

    it('Происходит переход к сообщению по линке, если оно не глубоко в истории и мы не состоим в чате', async function () {
        const params = {
            // Инвайт хеш незаджоиненного чата, не потерять
            inviteHash: 'd275356e-47db-44d7-95e5-9ee7a72a8f53',
            timestamp: '1580224020609007',
        };

        await openAndAssert.call(this, params, 'scrollToHereUnjoined');
    });

    it('Происходит переход к сообщению по линке, если оно глубоко в истории и мы состоим в чате', async function () {
        const params = {
            inviteHash: '5e7b9e75-bd73-4480-8719-151505b1bf11',
            timestamp: '1580213533131016',
        };

        await openAndAssert.call(this, params, 'first');
    });

    it('Происходит переход к сообщению по линке, если оно глубоко в истории и мы не состоим в чате', async function () {
        const params = {
            inviteHash: 'd275356e-47db-44d7-95e5-9ee7a72a8f53',
            timestamp: '1580223973651007',
        };

        await openAndAssert.call(this, params, 'unjoinedFirst');
    });

    it('Происходит переход к сообщению по линке, если оно было удалено и мы состоим в данном чате', async function () {
        const params = {
            inviteHash: '5e7b9e75-bd73-4480-8719-151505b1bf11',
            timestamp: '1580213538053016',
        };

        await openAndAssert.call(this, params, 'deletedMessage');
    });

    it('Происходит переход к сообщению по линке, если оно было удалено и мы не состоим в данном чате', async function () {
        const params = {
            inviteHash: 'd275356e-47db-44d7-95e5-9ee7a72a8f53',
            timestamp: '1580224000164007',
        };

        await openAndAssert.call(this, params, 'deletedMessageUnjoined');
    });

    it('Подскролл к последнему сообщению', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            inviteHash: '5e7b9e75-bd73-4480-8719-151505b1bf11',
            timestamp: '1580213541311016',
        });

        await browser.yaWaitForVisible(PO.messageText(), 'Не появились сообщения');
        await browser.yaWaitForHidden(PO.floatingDateVisible(), 'Дата не исчезла');
        await browser.yaWaitForVisible(PO.scrollToBottom(), 'Не появилась кнопка подскролла к последнему сообщению');
        await browser.click(PO.scrollToBottom());
        await browser.yaWaitForHidden(PO.scrollToBottom(), 'Не исчезла кнопка подскролла');
        assert.equal(
            await browser.getText(PO.lastMessage.messageText()),
            'LAST_MESSAGE',
            'Не появилось последнее сообщение',
        );
    });
});
