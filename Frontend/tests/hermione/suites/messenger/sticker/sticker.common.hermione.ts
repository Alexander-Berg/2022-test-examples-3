hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-3514');
specs({
    feature: 'Стикеры',
}, function () {
    beforeEach(async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });
    });

    it('Окно со стикерами открывается и закрывается', async function () {
        const { browser } = this;

        await browser.waitForVisible(PO.compose.stickerButton(), 'кнопка со стикерами не отобразилась');

        await browser.click(PO.compose.stickerButton());
        await browser.yaWaitForVisible(PO.popup.stickers());
        await browser.assertView('stickers-open', PO.popup.stickers(), { allowViewportOverflow: true });

        await browser.click(PO.popupWrapper());
        await browser.yaWaitForHidden(PO.popup.stickers());
    });

    it('Вкладки со стикерами переключаются', async function () {
        const { browser } = this;

        await browser.waitForVisible(PO.compose.stickerButton(), 'кнопка со стикерами не отобразилась');
        await browser.click(PO.compose.stickerButton());
        await browser.waitForVisible(PO.popup.stickers(), 'Попап со стикерами не появился');
        await browser.assertView('stickers-not-switched', PO.popup.stickers(), {
            allowViewportOverflow: true,
            screenshotDelay: 666,
            tolerance: 20,
        });

        await browser.waitForVisible(PO.popup.stickers.tabs.secondTab(), 'панель с выбором стикерпаков не отобразилась');
        await browser.yaWaitForHidden(PO.chat.loader());
        await browser.click(PO.popup.stickers.tabs.secondTab());

        await browser.waitForVisible(PO.popup.stickers(), 'Попап со стикерами не появился');
        await browser.assertView('stickers-switched', PO.popup.stickers(), {
            allowViewportOverflow: true,
            screenshotDelay: 666,
            tolerance: 20,
        });
    });

    it('Отправляется сообщение со стикером', async function () {
        const { browser } = this;

        await browser.waitForVisible(PO.compose.stickerButton(), 'кнопка со стикерами не отобразилась');
        await browser.click(PO.compose.stickerButton());

        await browser.yaWaitForHidden(PO.chat.loader());
        await browser.waitForVisible(PO.popup.stickers(), 'окно со стикерами не отобразилось');
        await browser.click(PO.popup.stickers.firstRow.firstSticker());

        await browser.yaWaitForNewStickerMessage({ type: 'send' });
    });

    hermione.skip.in(['chrome-phone'], 'https://st.yandex-team.ru/MSSNGRFRONT-7924');
    it('Окно со стикерами скролится', async function () {
        const { browser } = this;

        await browser.waitForVisible(PO.compose.stickerButton(), 'кнопка со стикерами не отобразилась');
        await browser.click(PO.compose.stickerButton());

        await browser.waitForVisible(PO.popup.stickers.tabs.secondTab(), 'вторая вкладка окна со стикерами не отобразилось');
        await browser.yaWaitForHidden(PO.chat.loader());
        await browser.click(PO.popup.stickers.tabs.secondTab());

        await browser.execute((selector) => {
            const stickersElem = document.querySelector(selector);
            stickersElem.scrollBy(0, 300);
        }, PO.popup.stickers.virtualList.scroller());

        await browser.assertView('stickers-scrolled', PO.popup.stickers(), {
            allowViewportOverflow: true,
            ignoreElements: ['.ui-tab .yamb-sticker'],
        });
    });

    it('Табы со стикерами скролятся', async function () {
        const { browser } = this;

        await browser.waitForVisible(PO.compose.stickerButton(), 'кнопка со стикерами не отобразилась');
        await browser.click(PO.compose.stickerButton());

        await browser.waitForVisible(PO.popup.stickers.tabs(), 'Блок с табами не отобразился');
        await browser.yaWaitForHidden(PO.chat.loader());
        await browser.click(PO.popup.stickers.tabs());

        await browser.execute((selector) => {
            const stickersElem = document.querySelector(selector);
            stickersElem.scrollLeft = 1000;
        }, PO.popup.stickers.tabs());

        await browser.assertView('stickers-tabs-scrolled', PO.popup.stickers.tabs(), { allowViewportOverflow: true });
    });
});
