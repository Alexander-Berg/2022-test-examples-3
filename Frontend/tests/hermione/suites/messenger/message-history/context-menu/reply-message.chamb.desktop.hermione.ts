specs({
    feature: 'Контекстное меню (Ответить)',
}, function () {
    const textMessageSelector = '.message:nth-last-child(1)';
    const menuSelector = '.ui-menu';
    const quoteSelector = '.yamb-quote';

    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            userAlias: 'user',
        });
    });

    afterEach(async function () {
        await this.browser.yaCheckClientErrors();
    });

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-7489');
    it('Отмена ответа с помощью клавиши Esc', async function () {
        const { browser, currentPlatform } = this;

        await browser.waitForVisible(textMessageSelector, 'В чате нет текстовых сообщений');
        await browser.yaOpenMessageContextMenu(textMessageSelector, currentPlatform);
        await browser.waitForVisible(menuSelector, 'Не появилось контекстное меню');

        await browser.click(PO.messageMenuReply());
        await browser.yaWaitForHidden(menuSelector);
        await browser.waitForVisible(textMessageSelector);
        await browser.waitForVisible(quoteSelector, 'Сообщение не прикрепилось');
        await browser.keys('Escape');
        await browser.yaWaitForHidden(quoteSelector, 'Не скрылось информационное сообщение');
    });
});
