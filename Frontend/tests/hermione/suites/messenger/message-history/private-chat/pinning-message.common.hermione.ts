specs({
    feature: 'Закрепление/открепление сообщений',
}, function () {
    const { sendTextMessage } = require('../../shared-steps/send-message.hermione');

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7489');
    it('Закрепление сообщения', async function () {
        const { browser, currentPlatform } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await sendTextMessage.call(this, 'hello');

        await browser.yaOpenMessageContextMenu(PO.lastMessage.message(), currentPlatform);
        await browser.yaWaitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');

        await browser.click(PO.messageMenuPin());
        await browser.yaWaitForHidden(PO.popup.menu(), 'Контекстное меню не скрылось');
        await browser.yaWaitForVisible(PO.pinnedMessage(), 'Сообщение не прикрепилось к шапке');

        await browser.yaWaitForVisible(PO.lastMessage.system(), 'Не отобразилось системное сообщение');
    });

    // Перед снятием теста, нужно закрепить сообщение в чате или снять тест с закреплением
    it('Открепление сообщения', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.click(PO.pinnedMessage.remove());
        await browser.yaWaitForVisible(PO.popup.confirm(), 'Не открылось модальное окно');

        await browser.click(await browser.yaGetContainsSelector(PO.popup.confirm.button(), 'ОК'));

        await browser.yaWaitForHidden(PO.popup.confirm(), 'Модальное окно не закрылось');
        await browser.yaWaitForHidden(PO.pinnedMessage(), 'Сообщение не открепилось');

        await browser.yaWaitForVisible(PO.lastMessage.system(), 'Не отобразилось системное сообщение');
    });
});
