hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7489');
specs({
    feature: 'Контекстное меню (Выбрать)',
}, function () {
    const fileMessageSelector = '.message:nth-last-child(1) .yamb-message-row';
    const imageMessageSelector = '.message:nth-last-child(2) .yamb-message-row';
    const stickerMessageSelector = '.message:nth-last-child(3) .yamb-message-row';
    const textMessageSelector = '.message:nth-last-child(4) .yamb-message-row';

    const active = 'yamb-message-row_active';

    const forwardedMessageSelector = '.yamb-forwarded-messages';

    const actionPanelSelector = '.yamb-messages-action-panel';
    const closePanelButtonSelector = PO.messagesActionPanel.cancel();
    const chosenNumberIndicatorSelector = PO.messagesActionPanel.count();

    const submitButtonSelector = '.yamb-compose-submit-button_type_button';

    const actionMenuSelector = '.ui-menu';

    const textarea = '.ui-textarea__control';

    function getClassList(selector) {
        const node = document.querySelector(selector);

        if (!node) {
            throw new Error(`Can not get class list for "${selector}"`);
        }

        return node.classList;
    }

    function scrollToElement(selector) {
        const event = new WheelEvent('wheel', { deltaY: -200, bubbles: true });
        const list = document.querySelector('.ui-InfiniteList-Container');

        if (!list) {
            return;
        }

        let message = document.querySelector(selector);

        while (!message) {
            list.dispatchEvent(event);
            message = document.querySelector(selector);
        }

        const top = list.getBoundingClientRect().top;

        while (message.getBoundingClientRect().top < top) {
            list.dispatchEvent(event);
        }

        list.dispatchEvent(event);
    }

    function getSelectorForward() {
        return PO.messagesActionPanel.forward();
    }

    function getCount(text) {
        // @ts-ignore
        const [_, count] = text.match(/(\d+)/);

        return count;
    }

    beforeEach(function () {
        return this.browser.yaOpenMessenger({
            userAlias: 'user',
        });
    });

    // Отправить в чат 4 сообщения от yndx-mssngr-tst-2: стикер, картинка, файл, текст
    // Используем их для тестов

    it('Выбор сообщения', async function () {
        const { browser, currentPlatform } = this;

        await browser.execute(scrollToElement, textMessageSelector);
        await browser.waitForVisible(textMessageSelector, 'В чате нет текстовых сообщений');

        await browser.yaOpenMessageContextMenu(textMessageSelector, currentPlatform);

        await browser.waitForVisible(actionMenuSelector, 'Не появилось контекстное меню');
        await browser.clickTo(PO.messageMenuSelect());

        await browser.yaWaitForHidden(actionMenuSelector);

        await browser.waitForVisible(textMessageSelector);

        const classes = await browser.execute(getClassList, textMessageSelector);
        assert(classes.indexOf(active) !== -1, 'Выделение не было произведено');

        await browser.waitForVisible(actionPanelSelector, 'Не появилось информационное сообщение');
        const infoText = await browser.getText(chosenNumberIndicatorSelector);

        assert.equal(getCount(infoText), '1', 'Неправильный текст о выбранных сообщениях');
    });

    it('Выбор и пересылка нескольких сообщений', async function () {
        const { browser, currentPlatform } = this;

        await browser.execute(scrollToElement, textMessageSelector);
        await browser.waitForVisible(textMessageSelector, 'В чате нет текстовых сообщений');

        await browser.yaOpenMessageContextMenu(textMessageSelector, currentPlatform);

        await browser.waitForVisible(actionMenuSelector, 'Не появилось контекстное меню');
        await browser.clickTo(PO.messageMenuSelect());

        await browser.yaWaitForHidden(actionMenuSelector);

        let classes = await browser.execute(getClassList, textMessageSelector);
        assert(classes.indexOf(active) !== -1, 'Выделение текста не было произведено');

        await browser.waitForVisible(actionPanelSelector, 'Не появилось информационное сообщение');

        let infoText = await browser.getText(chosenNumberIndicatorSelector);

        assert.equal(getCount(infoText), '1', 'Неправильный текст о выбранных сообщениях');

        await browser.waitForEnabled(fileMessageSelector);
        await browser.clickTo(fileMessageSelector, 5, 5);

        await browser.waitForVisible(fileMessageSelector);

        classes = await browser.execute(getClassList, fileMessageSelector);
        assert(classes.indexOf(active) !== -1, 'Выделение файла не было произведено');

        infoText = await browser.getText(chosenNumberIndicatorSelector);

        assert.equal(getCount(infoText), '2', 'Неправильный текст о выбранных сообщениях');

        await browser.waitForEnabled(imageMessageSelector);
        await browser.clickTo(imageMessageSelector, 5, 5);

        classes = await browser.execute(getClassList, imageMessageSelector);
        assert(classes.indexOf(active) !== -1, 'Выделение картинки не было произведено');

        infoText = await browser.getText(chosenNumberIndicatorSelector);

        assert.equal(getCount(infoText), '3', 'Неправильный текст о выбранных сообщениях');

        await browser.waitForEnabled(stickerMessageSelector);
        await browser.execute(scrollToElement, stickerMessageSelector);
        await browser.click(stickerMessageSelector);

        classes = await browser.execute(getClassList, stickerMessageSelector);
        assert(classes.indexOf(active) !== -1, 'Выделение стикера не было произведено');

        infoText = await browser.getText(chosenNumberIndicatorSelector);

        assert.equal(getCount(infoText), '4', 'Неправильный текст о выбранных сообщениях');

        // @ts-ignore
        await browser.click(getSelectorForward(currentPlatform));
        await browser.waitForEnabled(PO.forwardDialog.firstListItem());

        await browser.yaWaitForHidden('.ui-spinner');

        await browser.clickTo(PO.forwardDialog.firstListItem(), 5, 5);

        await browser.yaWaitForHidden('.yamb-modal.yamb-modal_fullscreen');

        await browser.waitForVisible('.yamb-quote');

        await browser.clickTo(textarea, 5, 5);
        await browser.setValue(textarea, 'text');

        await browser.click(submitButtonSelector);
        await browser.waitForVisible(forwardedMessageSelector);
    });

    it('Отмена выбора сообщения с помощью кнопки в виде крестика', async function () {
        const { browser, currentPlatform } = this;

        await browser.execute(scrollToElement, textMessageSelector);
        await browser.waitForVisible(textMessageSelector, 'В чате нет текстовых сообщений');

        await browser.yaOpenMessageContextMenu(textMessageSelector, currentPlatform);

        await browser.waitForVisible(actionMenuSelector, 'Не появилось контекстное меню');

        await browser.clickTo(PO.messageMenuSelect());

        await browser.yaWaitForHidden(actionMenuSelector);

        await browser.waitForVisible(actionPanelSelector, 'Не появилось информационное сообщение');

        let classes = await browser.execute(getClassList, textMessageSelector);

        assert(classes.indexOf(active) !== -1, 'Выделение не было произведено');

        const infoText = await browser.getText(chosenNumberIndicatorSelector);

        assert.equal(getCount(infoText), '1', 'Неправильный текст о выбранных сообщениях');

        await browser.click(closePanelButtonSelector);

        classes = await browser.execute(getClassList, textMessageSelector);
        assert(classes.indexOf(active) === -1, 'Выделение не было снято');
    });

    it('Отмена выбора сообщения с помощью нажатия на выделенное сообщение', async function () {
        const { browser, currentPlatform } = this;

        await browser.execute(scrollToElement, textMessageSelector);
        await browser.waitForVisible(textMessageSelector, 'В чате нет текстовых сообщений');

        await browser.yaOpenMessageContextMenu(textMessageSelector, currentPlatform);

        await browser.waitForVisible(actionMenuSelector, 'Не появилось контекстное меню');

        await browser.clickTo(PO.messageMenuSelect());

        await browser.yaWaitForHidden(actionMenuSelector);

        await browser.waitForVisible(actionPanelSelector, 'Не появилось информационное сообщение');

        let classes = await browser.execute(getClassList, textMessageSelector);

        assert(classes.indexOf(active) !== -1, 'Выделение не было произведено');

        const infoText = await browser.getText(chosenNumberIndicatorSelector);

        assert.equal(getCount(infoText), '1', 'Неправильный текст о выбранных сообщениях');

        await browser.click(textMessageSelector);

        classes = await browser.execute(getClassList, textMessageSelector);
        assert(classes.indexOf(active) === -1, 'Выделение не было снято');
    });
});
