specs({
    feature: 'Ввод сообщений',
}, function () {
    const textarea = PO.compose.input();
    const textareaContainer = PO.compose.input();

    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            guid: 'ef212763-afac-488b-b099-f0e1c23cba3d',
        });

        await this.browser.waitForVisible('.yamb-chat', 'Чат не открылся');
    });

    it('Ввод короткого текста', async function () {
        const { browser } = this;
        const text = 'text';

        await browser.click(textarea);
        await browser.setValue(textarea, text);
        const value = await browser.getValue(textarea);
        assert.equal(value, text, 'Текст не появился в инпуте');
    });

    it('Ввод длинного сообщения', async function () {
        const { browser } = this;
        // вводим 200 символов, пробел для корректного отображения скрина(чтобы текст не подчеркивался)
        const text = `${'text'.repeat(50)} `;

        await browser.click(textarea);
        await browser.setValue(textarea, text);
        const value = await browser.getValue(textarea);
        assert.equal(value, text, 'Текст не появился в инпуте');

        await browser.assertView('longMsg', textareaContainer);
    });

    it('Ввод символов, цифр и т.д.', async function () {
        const { browser } = this;
        let text = '1234567890';

        await browser.click(textarea);
        await browser.setValue(textarea, text);
        let value = await browser.getValue(textarea);
        assert.equal(value, text, 'Ошибка ввода цифр');

        await browser.assertView('digits', textareaContainer);

        text = '@#$%^&*!±<>?/\\';

        await browser.setValue(textarea, text);
        value = await browser.getValue(textarea);
        assert(value === text, 'Ошибка ввода специальных символов');

        await browser.assertView('symbols', textareaContainer);
    });

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7486');
    it('Ввод через копировать-вставить', async function () {
        const { browser } = this;
        const text = 'text';

        // Эмулируем копирование текста в буфер обмена
        await browser.setValue(textarea, text);
        await browser.keys(['Shift', 'Left arrow', 'Left arrow', 'Left arrow', 'Left arrow', 'NULL']);
        await browser.keys(['Control', 'x', 'NULL']);

        // Проверяем, весь ли текст удалился
        await browser.getValue(textarea, (err, res) => {
            assert(err === null, 'Упала проверка на пустоту инпута, после вырезании из нее текста');
            assert(res === '', `Из инпута удалился не весь текст при вырезании, остаток: ${res}`);
        });

        // Вставляем скопированный текст обратно
        // Не CTRL + V, т.к. https://twitter.com/webdriverio/status/812034986341789696
        await browser.keys(['Shift', 'Insert']);

        const value = await browser.getValue(textarea);

        assert(value === text, `Ошибка при вставке символов через Shift + Insert, текст в инпуте: ${value}`);
    });
});
