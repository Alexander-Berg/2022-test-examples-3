specs({
    feature: 'История сообщений',
}, function () {
    const messageSelector = '.yamb-user-message';
    const messageTextSelector = '.yamb-message-text';
    const sentMessageClass = 'yamb-user-message_own';

    // Получение истории текстовых собщений в формате массива объектов
    // {
    //     sent: true для исхрдящего
    //     text: текст сообщения
    // }
    const getHistory = (ctx) => {
        // eslint-disable-next-line no-shadow
        return ctx.browser.execute(function (messageSelector, messageTextSelector, sentMessageClass) {
            var messages = document.querySelectorAll(messageSelector);
            return Array.prototype.slice.call(messages).map(function (message) {
                return {
                    sent: message.classList.contains(sentMessageClass),
                    text: message.querySelector(messageTextSelector).innerText,
                };
            });
        }, messageSelector, messageTextSelector, sentMessageClass);
    };

    // сравнение историй сообщений
    const compareHistories = (history1, history2) => {
        const history1Len = history1.length;
        const history2Len = history2.length;
        const len = Math.min(history1Len, history2Len);
        // убедиться, что истории прогружены на одну глубину

        const normalisedHistory1 = history1Len > len ? history1.slice(history1Len - len) : history1;
        const normalisedHistory2 = history2Len > len ? history2.slice(history2Len - len) : history2;

        return normalisedHistory1.every((item, index) => {
            return item.sent === normalisedHistory2[index].sent &&
                item.text === normalisedHistory2[index].text;
        });
    };

    it('После переоткрытия мессенджера история сообщений сохраняется', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            guid: '28e2a15d-6fcd-4a12-98f4-2afaf498a095',
        });
        await browser.waitForVisible('.yamb-chat', 'Чат не открылся');

        // набивается вручную под аккантом тестов и YNDX_MSSNGR_TST_5_LOGIN
        const expectedHistory = [{
            text: 'Входящее сообщение 1',
            sent: false,
        }, {
            text: 'Входящее сообщение 2',
            sent: false,
        }, {
            text: 'Исходящее сообщение 1',
            sent: true,
        }, {
            text: 'Входящее сообщение 3',
            sent: false,
        }, {
            text: 'Исходящее сообщение 2',
            sent: true,
        }, {
            text: 'Исходящее сообщение 3',
            sent: true,
        }, {
            text: 'Входящее сообщение 4',
            sent: false,
        }, {
            text: 'Исходящее сообщение 4',
            sent: true,
        }, {
            text: 'Исходящее сообщение 5',
            sent: true,
        }, {
            text: 'Входящее сообщение 5',
            sent: false,
        }];

        const loadedHistory = await getHistory(this);
        assert(compareHistories(loadedHistory, expectedHistory), 'Переписка в чате с ботом не сохранилась');
    });
});
