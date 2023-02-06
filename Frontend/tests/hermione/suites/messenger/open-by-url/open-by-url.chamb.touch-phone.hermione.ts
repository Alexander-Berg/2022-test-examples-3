specs({
    feature: 'Открытие чата по ссылке',
}, function () {
    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            guid: 'ef212763-afac-488b-b099-f0e1c23cba3d',
        });
    });

    it('Открывается чат по ссылке', async function () {
        const { browser } = this;
        await browser.waitForVisible('.yamb-chat', 'Чат не открылся');

        return this;
    });
});
