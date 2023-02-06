specs({
    feature: 'Отправка сообщений',
}, function () {
    it('По нажатию "enter" в зависимости от настройки', async function () {
        const { browser } = this;
        const text = 'some random text';

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
        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), text);
        await browser.keys('Enter');
        await browser.yaWaitForNewTextMessage(text, { type: 'send' });
    });

    it('По нажатию "ctrl+enter" на Windows в зависимости от настройки', async function () {
        const { browser } = this;
        const text = 'another random text';

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
        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), text);
        await browser.keys(['Control', 'Enter']);
        await browser.yaWaitForNewTextMessage(text, { type: 'send' });
    });

    it('По нажатию "control+enter" и "command+enter" на Mac в зависимости от настройки', async function () {
        // control+enter тестируется в тесте 'По нажатию "ctrl+enter" на Windows в зависимости от настройки'
        const { browser } = this;
        const text = 'yet another random text';

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
        await browser.click(PO.compose.container.input());
        await browser.setValue(PO.compose.container.input(), text);
        await browser.keys(['Meta', 'Enter']);
        await browser.yaWaitForNewTextMessage(text, { type: 'send' });
    });
});
