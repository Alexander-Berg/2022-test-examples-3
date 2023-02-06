specs({
    feature: 'Контекстное меню (Выбрать)',
}, function () {
    const textMessageSelector = '.message:nth-last-child(1) .yamb-message-row';

    const active = 'yamb-message-row_active';

    const actionPanelSelector = '.yamb-messages-action-panel';
    const chosenNumberIndicatorSelector = PO.messagesActionPanel.count();

    const actionMenuSelector = '.ui-menu';

    function getClassList(selector) {
        const node = document.querySelector(selector);
        return node.classList;
    }

    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            userAlias: 'user',
        });
    });

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7489');
    it('Отмена выбора сообщения с помощью клавиши Esc', async function () {
        const { browser, currentPlatform } = this;

        await browser.waitForVisible(textMessageSelector, 'В чате нет текстовых сообщений');

        await browser.yaOpenMessageContextMenu(textMessageSelector, currentPlatform);

        await browser.waitForVisible(actionMenuSelector, 'Не появилось контекстное меню');

        await browser.click(PO.messageMenuSelect());

        await browser.yaWaitForHidden(actionMenuSelector);

        await browser.waitForVisible(textMessageSelector);

        let classes = await browser.execute(getClassList, textMessageSelector);
        assert(classes.indexOf(active) !== -1, 'Выделение не было произведено');

        await browser.waitForVisible(actionPanelSelector, 'Не появилось информационное сообщение');
        const infoText = await browser.getText(chosenNumberIndicatorSelector);

        assert.equal(infoText, 'Выбрано 1 сообщение', 'Неправильный текст о выбранных сообщениях');

        await browser.keys('Escape');

        classes = await browser.execute(getClassList, textMessageSelector);
        assert(classes.indexOf(active) === -1, 'Выделение не было снято');

        await browser.yaWaitForHidden(actionPanelSelector, 'Не скрылось информационное сообщение');
    });
});
