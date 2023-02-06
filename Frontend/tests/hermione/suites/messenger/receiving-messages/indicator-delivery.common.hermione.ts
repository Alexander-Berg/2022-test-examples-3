specs({
    feature: 'Индикатор доставки',
}, function () {
    const { sendTextMessage } = require('../shared-steps/send-message.hermione');

    const checkIndicator = (browser, name) => (async function () {
        const iconCheckName = await browser.getAttribute(`${PO.lastMessage.balloonInfo()} use`, 'xlink:href');

        return iconCheckName.slice(6) === name;
    });

    it('Проверка индикатора отправки сообщения', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await sendTextMessage.call(this, 'Привет');
        await browser.waitUntil(checkIndicator(browser, 'check'), 5000, 'Индикатор отправки сообщения не появился');
    });

    it('Проверка индикатора просмотра сообщения', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await sendTextMessage.call(this, 'Привет');
        await browser.waitUntil(checkIndicator(browser, 'double-check'), 10000, 'Индикатор просмотра сообщения не появился');
    });
});
