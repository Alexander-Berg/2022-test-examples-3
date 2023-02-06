hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7489');
specs({
    feature: 'Контекстное меню(Редактировать)',
}, function () {
    const { sendTextMessage } = require('../../shared-steps/send-message.hermione');
    const deleteMessagePopupSelector = PO.popup.confirm();
    const deleteMessageCancelButtonSelector = PO.popup.confirm.cancelButton();

    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            userAlias: 'user',
        });
    });

    it('Редактирование сообщения', async function () {
        const { browser, currentPlatform } = this;

        await sendTextMessage.call(this, 'hello world');

        await browser.yaOpenMessageContextMenu(PO.lastMessage.message(), currentPlatform);
        await browser.waitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');
        await browser.click(PO.messageMenuEdit());

        await browser.waitForVisible(PO.quote(), 'Не появилась плашка с редактируемым сообщением');
        assert.equal('hello world', await browser.getText(PO.quoteDescription()), 'В редактируемом сообщении неверный текст');

        await sendTextMessage.call(this, 'hello worl');

        const lastMessage = await browser.getText(PO.lastMessage.messageText(), 'Исходный текст сообщения изменился');

        assert.equal('hello worl', lastMessage);
    });

    it('При редактировании сообщения не вносим изменений в текст', async function () {
        const { browser, currentPlatform } = this;

        await sendTextMessage.call(this, 'hello world');
        await browser.yaOpenMessageContextMenu(PO.lastMessage.message(), currentPlatform);
        await browser.waitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');
        await browser.click(PO.messageMenuEdit());

        await sendTextMessage.call(this, 'hello world');
        await browser.yaScrollIntoView(PO.lastMessage.messageText());
        const lastMessage = await browser.getText(PO.lastMessage.messageText(), 'Исходный текст сообщения изменился');
        assert.equal('hello world', lastMessage);
    });

    it('Редактирование сообщения с полным удалением текста', async function () {
        const { browser, currentPlatform } = this;

        await sendTextMessage.call(this, 'test');

        await browser.yaOpenMessageContextMenu(PO.lastMessage.message(), currentPlatform);
        await browser.yaWaitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');
        await browser.click(PO.messageMenuEdit());

        await browser.click(PO.compose.container.input());

        await browser.setValue(PO.compose.container.input(), ' ');

        await browser.click(PO.compose.sendMessageButtonEnabled());

        await browser.yaWaitForVisible(deleteMessagePopupSelector, 'Не появилось модальное окно удаления сообщения');
        await browser.click(deleteMessageCancelButtonSelector);

        await browser.yaWaitForHidden(deleteMessagePopupSelector, 'Не закрылось модальное окно');
        await browser.waitForVisible(PO.quote(), 'Не произошло возвращение к редактированию сообщения');
        assert.equal(' ', await browser.getText(PO.compose.container.input()), 'Текст редактирования не пустой');
    });

    it('Отмена редактирования', async function () {
        const { browser, currentPlatform } = this;

        await sendTextMessage.call(this, 'hello world');

        await browser.yaOpenMessageContextMenu(PO.lastMessage.message(), currentPlatform);
        await browser.waitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');
        await browser.click(PO.messageMenuEdit());

        await browser.waitForVisible(PO.quote(), 'Не появилась плашка с редактируемым сообщением');
        await browser.setValue(PO.compose.container.input(), 'hello worl');
        await browser.click(PO.compose.quote.close());

        await browser.yaWaitForHidden(PO.quote(), 'Не исчезла плашка редактирования сообщения');
        assert.equal('', await browser.getText(PO.compose.container.input()), 'Редактирование не прекратилось');
    });

    it('При отмене редактирования не вносим изменений в текст', async function () {
        const { browser, currentPlatform } = this;

        await sendTextMessage.call(this, 'hello world');

        await browser.yaOpenMessageContextMenu(PO.lastMessage.message(), currentPlatform);
        await browser.waitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');
        await browser.click(PO.messageMenuEdit());

        await browser.yaWaitForVisible(PO.quote(), 'Не появилась плашка редактирования сообщения');
        await browser.click(PO.compose.quote.close());

        await browser.yaWaitForHidden(PO.quote(), 'Не исчезла плашка редактирования сообщения');
        assert.equal('', await browser.getText(PO.compose.container.input()), 'Редактирование не прекратилось');
    });

    it('Повторное редактирование после отмены редактирования', async function () {
        const { browser, currentPlatform } = this;

        await sendTextMessage.call(this, 'hello world');

        await browser.yaOpenMessageContextMenu(PO.lastMessage.message(), currentPlatform);
        await browser.waitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');
        await browser.click(PO.messageMenuEdit());

        await browser.waitForVisible(PO.quote(), 'Не появилась плашка с редактируемым сообщением');
        await browser.setValue(PO.compose.container.input(), 'hello worl');
        await browser.click(PO.compose.quote.close());

        await browser.yaWaitForHidden(PO.quote(), 'Не исчезла плашка редактирования сообщения');
        assert.equal('', await browser.getText(PO.compose.container.input()), 'Редактирование не прекратилось');

        await sendTextMessage.call(this, 'txt');
        await browser.yaOpenMessageContextMenu(PO.lastMessage.message(), currentPlatform);
        await browser.yaWaitForVisible(PO.popup.menu(), 'Не появилось контекстное меню');
        await browser.click(PO.messageMenuEdit());

        browser.waitForVisible(PO.quote(), 'Не появилась плашка редактирования сообщения');
    });
});
