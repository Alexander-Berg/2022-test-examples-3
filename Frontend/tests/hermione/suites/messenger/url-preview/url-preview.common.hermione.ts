specs({
    feature: 'Url-превью',
}, function () {
    hermione.skip.in([], 'https://st.yandex-team.ru/MSSNGRFRONT-7204');
    it('Превью при наборе сообщения', async function () {
        const { browser } = this;
        const text = 'Hello http://ya.ru';

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/e9515522-81ba-4e17-b56a-6e77fc48adeb',
        });

        await browser.yaWaitForVisible(PO.compose.container.input());
        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), text);

        await browser.yaWaitForVisible(PO.compose.quote(), 'Превью не открылось');

        await browser.click(PO.compose.quote.close());
        await browser.yaWaitForHidden(PO.compose.quote(), 'Превью не скрылось');

        await browser.setValue(PO.compose.container.input(), text.slice(0, -1));
        await browser.setValue(PO.compose.container.input(), text);

        await browser.yaWaitForVisible(PO.compose.quote(), 'Превью не переоткрылось');
    });

    it('Сообщение с включенным превью', async function () {
        const { browser } = this;
        const text = 'With preview http://ya.ru';

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/e9515522-81ba-4e17-b56a-6e77fc48adeb',
        });

        await browser.yaWaitForVisible(PO.compose.container.input());
        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), text);
        await browser.click(PO.compose.sendMessageButtonEnabled());

        await browser.yaWaitForNewTextMessage(text, { type: 'send', waitForSend: true });
        await browser.yaWaitForVisible(PO.lastMessage.urlPreview());
    });

    it('Сообщение с отключенным превью', async function () {
        const { browser } = this;
        const text = 'Without preview http://ya.ru';

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/e9515522-81ba-4e17-b56a-6e77fc48adeb',
        });

        await browser.yaWaitForVisible(PO.compose.container.input());
        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), text);

        await browser.yaWaitForVisible(PO.compose.quote.close());
        await browser.click(PO.compose.quote.close());
        await browser.yaWaitForHidden(PO.compose.quote.close());

        await browser.click(PO.compose.sendMessageButtonEnabled());

        await browser.yaWaitForNewTextMessage(text, { type: 'send', waitForSend: true });
        await browser.yaWaitForHidden(PO.lastMessage.urlPreview());
    });
});
