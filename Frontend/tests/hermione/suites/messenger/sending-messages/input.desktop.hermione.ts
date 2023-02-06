specs({
    feature: 'Ввод сообщений',
}, function () {
    it('Перевод строки по нажатию "shift+enter"', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/e9515522-81ba-4e17-b56a-6e77fc48adeb',
        });

        await browser.click(PO.settingsButton());
        await browser.yaWaitForVisible(PO.modal(), 'модальное окно с настройками не открылось');
        const sendByEnter = await browser.execute(function () {
            // @ts-ignore
            return document.getElementsByName('sendByEnter')[0].checked;
        });
        if (!sendByEnter) {
            // Включаем отправку по Enter
            await browser.click(`[name=sendByEnter] + ${PO.tumbler()}`);
        }
        await browser.click(PO.modal.toolbar.closeButton());
        await browser.setValue(PO.compose.container.input(), 'строка');
        await browser.keys(['Shift', 'Enter']);
        await browser.keys('еще строка'.split(''));
        const value = await browser.getValue(PO.compose.container.input());
        assert(value.includes('\n'), 'Нет переноса в строке');
    });

    it('Перевод строки по нажатию "enter" в зависимости от настройки', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/e9515522-81ba-4e17-b56a-6e77fc48adeb',
        });

        await browser.click(PO.settingsButton());
        await browser.yaWaitForVisible(PO.modal(), 'модальное окно с настройками не открылось');
        await browser.pause(5000);
        const sendByEnter = await browser.execute(function () {
            // @ts-ignore
            return document.getElementsByName('sendByEnter')[0].checked;
        });
        if (sendByEnter) {
            // Выключаем отправку по Enter
            await browser.click(`[name=sendByEnter] + ${PO.tumbler()}`);
        }
        await browser.pause(5000);
        await browser.click(PO.modal.toolbar.closeButton());
        await browser.setValue(PO.compose.container.input(), 'строка');
        await browser.keys('Enter');
        await browser.keys('еще строка'.split(''));
        const value = await browser.getValue(PO.compose.container.input());
        assert(value.includes('\n'), 'Нет переноса в строке');
    });

    it('Перевод строки по нажатию "ctrl+enter" на Windows в зависимости от настройки', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/e9515522-81ba-4e17-b56a-6e77fc48adeb',
        });

        await browser.click(PO.settingsButton());
        await browser.yaWaitForVisible(PO.modal(), 'модальное окно с настройками не открылось');
        const sendByEnter = await browser.execute(function () {
            // @ts-ignore
            return document.getElementsByName('sendByEnter')[0].checked;
        });
        if (!sendByEnter) {
            // Включаем отправку по Enter
            await browser.click(`[name=sendByEnter] + ${PO.tumbler()}`);
        }
        await browser.click(PO.modal.toolbar.closeButton());
        await browser.setValue(PO.compose.container.input(), 'строка');
        await browser.keys(['Control', 'Enter']);
        await browser.keys('еще строка'.split(''));
        const value = await browser.getValue(PO.compose.container.input());
        assert(value.includes('\n'), 'Нет переноса в строке');
    });

    it('Перевод строки по нажатию "control+enter" и "command+enter" на Mac в зависимости от настройки', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/e9515522-81ba-4e17-b56a-6e77fc48adeb',
        });

        await browser.click(PO.settingsButton());
        await browser.yaWaitForVisible(PO.modal(), 'модальное окно с настройками не открылось');
        const sendByEnter = await browser.execute(function () {
            // @ts-ignore
            return document.getElementsByName('sendByEnter')[0].checked;
        });
        if (!sendByEnter) {
            // Включаем отправку по Enter
            await browser.click(`[name=sendByEnter] + ${PO.tumbler()}`);
        }
        await browser.click(PO.modal.toolbar.closeButton());
        await browser.setValue(PO.compose.container.input(), 'строка');
        await browser.keys(['Meta', 'Enter']);
        await browser.keys('еще строка'.split(''));
        const value = await browser.getValue(PO.compose.container.input());
        assert(value.includes('\n'), 'Нет переноса в строке');
    });

    it('Отправка сообщения по "shift+enter" в зависимости от настройки', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            chatId: '0/0/e9515522-81ba-4e17-b56a-6e77fc48adeb',
        });

        await browser.click(PO.settingsButton());
        await browser.yaWaitForVisible(PO.modal(), 'модальное окно с настройками не открылось');
        const sendByEnter = await browser.execute(function () {
            // @ts-ignore
            return document.getElementsByName('sendByEnter')[0].checked;
        });
        if (sendByEnter) {
            // Выключаем отправку по Enter
            await browser.click(`[name=sendByEnter] + ${PO.tumbler()}`);
        }
        await browser.click(PO.modal.toolbar.closeButton());
        await browser.setValue(PO.compose.container.input(), 'строка');
        await browser.keys(['Shift', 'Enter']);
        await browser.keys('еще строка'.split(''));

        await browser.assertView('empty-input', PO.compose());
    });
});
