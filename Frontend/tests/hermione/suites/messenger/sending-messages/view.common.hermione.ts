specs({
    feature: 'Внешний вид инпута',
}, () => {
    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await this.browser.waitForVisible(PO.chat(), 'Чат не открылся');
    });

    it('Панель ввода сообщений', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await this.browser.assertView('compose', PO.compose());
    });
});
