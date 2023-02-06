specs({
    feature: 'Проверка открытия тестового сапортового бота по ссылке',
}, function () {
    it('Открытие чата с сапортовым ботом с текстом в поле ввода', async function () {
        const { browser } = this;
        const textInInputField = 'text message';

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/53a2ed75-1759-41fc-a6f4-b8012f77f372',
            customField: {
                name: 'text',
                value: 'text message',
            }
        });

        const value = await browser.getValue(PO.compose.input());
        assert.equal(value, textInInputField, 'Текст не появился в инпуте');
    });
});
