specs({
    feature: 'Закрепление и открепление чатов в списке чатов',
}, function () {
    beforeEach(async function () {
        await this.browser.yaOpenMessenger();
    });

    it('Закрепление чата', async function () {
        const browser = this.browser;
        await browser.yaWaitForHidden(PO.separator());
        // закрепляем чат
        await browser.rightClick(PO.chatListItem1());
        await browser.yaWaitForVisible(
            await browser.yaGetContainsSelector(PO.popup.menu.item2(), 'Закрепить чат'),
            'контекстное меню чата не открылось',
        );
        await browser.click(PO.popup.menu.item2());

        await browser.rightClick(PO.chatListItem1());

        await browser.yaWaitForVisible(
            await browser.yaGetContainsSelector(PO.popup.menu.item2(), 'Открепить чат'),
            'контекстное меню чата не открылось',
        );
    });
});
