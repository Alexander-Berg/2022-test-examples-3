specs({
    feature: 'Стикеры',
}, function () {
    const clickStickerButton = async function () {
        await this.browser.click(PO.compose.stickerButton());
        await this.browser.yaWaitForVisible(PO.popup.stickers(), 'Окно со стикерами не открылось');
        await this.browser.yaWaitForHidden(PO.popup.stickers.loader(), 'Иконка загрузки не исчезла');
    };

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-5119');
    it('Добавление и удаление стикеров', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });
        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await clickStickerButton.call(this);

        const { value: stickerTabsBeforeDelete } = await browser.elements(PO.popup.stickers.tabs.sticker());

        await browser.assertView('stickers-open', [PO.popup.stickers(), PO.compose.stickerButton()]);

        await browser.click(PO.popup.stickers.firstSticker());

        await browser.yaWaitForNewStickerMessage({ type: 'send', waitForSend: true });

        await browser.click(PO.popupWrapper());

        await browser.click(PO.lastMessage.sticker.image());
        await browser.yaWaitForVisible(PO.modal.stickerPack(), 'Модальное окно со стикер паком не открылось');

        await browser.assertView('modal-stickers-pack', PO.modal(), { allowViewportOverflow: true });

        let lastButtonText = await browser.getText(PO.modal.buttonsRow.lastButton());
        assert.equal(lastButtonText, 'Удалить стикеры');

        await browser.click(PO.modal.buttonsRow.lastButton());
        await browser.click(PO.modal.buttonsRow.cancelButton());
        await browser.yaWaitForHidden(PO.modal.stickerPack(), 'Модальное окно со стикер паком не закрылось');

        await clickStickerButton.call(this);

        const { value: stickerTabsAfterDelete } = await browser.elements(PO.popup.stickers.tabs.sticker());

        assert.isBelow(stickerTabsAfterDelete.length, stickerTabsBeforeDelete.length, 'Стикер пак не удалился');

        await browser.keys('Escape');
        await browser.click(PO.lastMessage.sticker());
        await browser.yaWaitForVisible(PO.modal.stickerPack(), 'Модальное окно со стикер паком не открылось');

        lastButtonText = await browser.getText(PO.modal.buttonsRow.lastButton());
        assert.equal(lastButtonText, 'Добавить стикеры');

        await browser.click(PO.modal.buttonsRow.lastButton());
        await browser.click(PO.modal.buttonsRow.cancelButton());
        await browser.yaWaitForHidden(PO.modal.stickerPack(), 'Модальное окно со стикер паком не закрылось');

        await clickStickerButton.call(this);

        const { value: stickerTabsAfterAdd } = await browser.elements(PO.popup.stickers.tabs.sticker());
        assert.equal(stickerTabsAfterAdd.length, stickerTabsBeforeDelete.length, 'Стикер пак не добавился');
    });
});
