specs({
    feature: 'Отправка сообщений',
}, function () {
    it('По кнопке отправить', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });
        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), 'some text');
        await browser.yaWaitForHidden(PO.compose.sendMessageButtonDisabled(), 'не скрыта неактивная кнопка отправки сообщения');
        await browser.yaWaitForVisible(PO.compose.sendMessageButtonEnabled(), 'не найдена активная кнопка отправки сообщения');
        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForNewTextMessage('some text', { type: 'send', waitForSend: true });
    });
});
