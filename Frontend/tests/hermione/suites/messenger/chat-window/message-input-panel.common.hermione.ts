specs({
    feature: 'Поле ввода',
}, function () {
    it('Кнопка "Backspace" удаляет ранее введенный текст', async function () {
        const { browser } = this;
        const text = 'text';
        const modifiedText = 'te';
        const noText = '';

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/41ac4ff9-573d-4d2c-8f44-853bac54ae28',
        });

        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), text);
        await browser.yaWaitForHidden(PO.compose.sendMessageButtonDisabled(), 'не скрыта неактивная кнопка отправки сообщения');
        await browser.yaWaitForVisible(PO.compose.sendMessageButtonEnabled(), 'не найдена активная кнопка отправки сообщения');

        const value = await browser.getValue(PO.compose.input());
        assert.equal(value, text, 'Текст не появился в инпуте');

        await browser.keys(['Backspace', 'Backspace']);

        const value1 = await browser.getValue(PO.compose.input());
        assert.equal(value1, modifiedText, 'В импуте отображается не тот текст');

        await browser.keys(['Backspace', 'Backspace']);

        const value2 = await browser.getValue(PO.compose.input());
        assert.equal(value2, noText, 'В импуте были удалены не все символы');
    });

    it('Нельзя отправить сообщение, состоящее из пробелов', async function () {
        const { browser } = this;
        const text = 'text';

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/41ac4ff9-573d-4d2c-8f44-853bac54ae28',
        });
        await browser.click(PO.compose.container.input());
        await browser.keys(['Space', 'Space', 'Space']);
        await browser.assertView('compose-with-spaces', PO.compose());
        await browser.keys(['Backspace', 'Backspace', 'Backspace']);
        await browser.assertView('compose-empty', PO.compose());
        await browser.setValue(PO.compose.container.input(), text);
        await browser.assertView('compose-with-text', PO.compose());
        await browser.click(PO.compose.sendMessageButtonEnabled());
        await browser.yaWaitForNewTextMessage(text, { type: 'send', waitForSend: true });
    });
});
