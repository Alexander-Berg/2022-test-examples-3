specs({
    feature: 'Внешний вид инпута',
}, () => {
    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            userAlias: 'user',
        });
        await this.browser.waitForVisible(PO.chat(), 'Чат не открылся');
    });

    it('Скролл в инпуте', async function () {
        const { browser } = this;

        const compose = PO.compose();
        const textareaContainer = PO.compose.container();
        const textarea = PO.compose.container.input();

        const text = 'text';

        await browser.click(textarea);
        await browser.setValue(textarea, text);
        await browser.keys(['Enter', 'NULL']);
        await browser.assertView('first-new-line', compose);

        for (let i = 0; i < 6; i++) {
            await browser.keys(['Enter', 'NULL']);
        }
        await browser.assertView('next-new-lines', compose);

        await this.browser.execute((selector) => {
            document.querySelector(selector).scrollTop = 0;
        }, textareaContainer);
        await browser.assertView('scroll-top', compose);
    });
});
