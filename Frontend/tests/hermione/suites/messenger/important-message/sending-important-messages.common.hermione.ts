specs({
    feature: 'Отправка важных сообщений',
}, function () {
    it('Пропадает кнопка важности, если ввели текст, а кнопку не тапнули', async function () {
        const { browser } = this;
        const text = 'te';
        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/41ac4ff9-573d-4d2c-8f44-853bac54ae28',
        });

        await browser.yaWaitForVisible(PO.compose.importantButton(), 'В поле ввода отсутствует кнопка выставления важности');
        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), text);
        await browser.yaWaitForHidden(PO.compose.importantButton(), 'В поле ввода есть кнопка выставления важности');

        await browser.keys(['Backspace', 'Backspace']);

        await browser.yaWaitForVisible(PO.compose.importantButton(), 'В поле ввода отсутствует кнопка выставления важности');
    });
});
