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

        await browser.waitForVisible('.yamb-sidebar', 'Не показался список чатов');
        await browser.waitForVisible('[data-test-tag="yamb-chat-list-item"]', 'В списке чатов нет ни одного чата');
        await browser.waitForVisible('.yamb-chat', 'Чат не открылся');
        await browser.setValue('.ui-textarea__control', 'text');
        await browser.click('.yamb-compose-submit-button_type_button');
        await browser.waitForVisible('.yamb-message-text');
    });
});
