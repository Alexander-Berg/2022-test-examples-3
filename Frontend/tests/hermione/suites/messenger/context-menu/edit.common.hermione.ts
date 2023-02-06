specs({
    feature: 'Пункт Редактировать',
}, function () {
    hermione.skip.in(['chrome-phone', 'iphone', 'ipad', 'chrome-pad'],
        'openMessageContextMenu осуществляет тап по элементу, а не области его и как результат не вызвается КМ');
    it('Нельзя отредактировать сообщение собеседника любого типа', async function () {
        const { browser, currentPlatform } = this;

        const messageSelectors = [
            ['819695a9-7d66-43af-a63e-c6a3b0dbc76f', PO.messageText()],
            ['819695a9-7d66-43af-a63e-c6a3b0dbc76f', PO.messageSticker()],
            ['819695a9-7d66-43af-a63e-c6a3b0dbc76f', PO.messageUrl()],
            ['819695a9-7d66-43af-a63e-c6a3b0dbc76f', PO.messageReply()],
            ['352f880b-c47c-4564-ae76-e8b921f8d443', PO.messageUrl()],
            ['352f880b-c47c-4564-ae76-e8b921f8d443', PO.messageForward()],
            ['352f880b-c47c-4564-ae76-e8b921f8d443', PO.messageGallery()],
            ['d44853fd-5872-4c72-b0f3-681da3561223', PO.messageImage()],
            ['d44853fd-5872-4c72-b0f3-681da3561223', PO.messageFile()],
            ['d44853fd-5872-4c72-b0f3-681da3561223', PO.messageVoice()],
        ];

        for (const [inviteHash, messageSelector] of messageSelectors) {
            await browser.yaOpenMessenger({
                build: 'yamb',
                inviteHash,
            });
            await browser.yaOpenMessageContextMenu(messageSelector, currentPlatform);

            if (currentPlatform === 'desktop') {
                await browser.waitForVisible(PO.popup.menu(), 'Не открылось меню сообщения');
            } else {
                await browser.waitForVisible(PO.popup(), 'Не открылось меню сообщения');
            }

            await browser.yaWaitForHidden(PO.messageMenuEdit(), 'Пункт редактировать виден');
        }
    });

    hermione.skip.in(['chrome-phone', 'iphone'],
        'openMessageContextMenu осуществляет тап по элементу, а не области его и как результат не вызвается КМ');
    it('Нельзя отредактировать собственное сообщение со стикером', async function () {
        const { browser, currentPlatform } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            inviteHash: 'b7825f25-a242-4d01-801a-498f4df5e6be',
        });

        await browser.yaOpenMessageContextMenu(PO.messageSticker(), currentPlatform);

        if (currentPlatform === 'desktop') {
            await browser.waitForVisible(PO.popup.menu(), 'Не открылось меню сообщения');
        } else {
            await browser.waitForVisible(PO.popup(), 'Не открылось меню сообщения');
        }

        await browser.yaWaitForHidden(PO.messageMenuEdit(), 'Пункт редактировать виден');
    });
});
