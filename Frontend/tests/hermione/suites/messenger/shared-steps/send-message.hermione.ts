async function sendStickerMessage() {
    const { browser } = this;

    await browser.waitForVisible(PO.compose.stickerButton(), 'кнопка со стикерами не отобразилась');
    await browser.click(PO.compose.stickerButton());

    await browser.waitForVisible(PO.popup.stickers.firstRow.firstSticker(), 'окно со стикерами не отобразилось');
    await browser.yaWaitForHidden(PO.popup.stickers.loader());
    await browser.click(PO.popup.stickers.firstRow.firstSticker());

    await browser.yaWaitForNewStickerMessage({ type: 'send', waitForSend: true });
}

async function sendTextMessage(text) {
    const composeTextareaSelector = 'textarea.ui-textarea__control';
    const sendBtnSelector = '.yamb-compose-submit-button_type_button';

    const { browser } = this;

    await browser.setValue(composeTextareaSelector, text);
    await browser.click(sendBtnSelector);

    await browser.yaWaitForNewTextMessage(text, { type: 'send', waitForSend: true });
}

async function sendPreviewUrlMessage(text, waitForSend = true) {
    const composeTextareaSelector = 'textarea.ui-textarea__control';
    const sendBtnSelector = '.yamb-compose-submit-button_type_button';

    const { browser } = this;

    await browser.setValue(composeTextareaSelector, text);
    await browser.click(sendBtnSelector);

    await browser.yaWaitForNewPreviewUrlMessage({ type: 'send', waitForSend });
}

module.exports = {
    sendStickerMessage,
    sendTextMessage,
    sendPreviewUrlMessage,
};
