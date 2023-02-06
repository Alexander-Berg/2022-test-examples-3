specs({
    feature: 'Медиабраузер',
}, function () {
    it('В блоке картинок можно листать картинки вправо от самой свежей картинки[manual]', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            build: 'yamb',
            userAlias: 'user',
        });

        await browser.yaOpenContactCardFromTitle();

        await browser.yaWaitForVisible(PO.chatInfo.assetsBrowser());
        await browser.yaClick(PO.chatInfo.assetsBrowser());
        await browser.yaWaitForVisible(PO.mediaItem());

        // Ожидаем подгрузки картинок
        await browser.yaWaitForVisible(PO.mediaItem.imageContainer());

        // Проверим, что картинка есть (можно убрать после решения проблемы ниже)
        const hasImageSrc = await browser.execute((selector) => {
            const image = document.querySelector(selector);
            return Boolean(image.getAttribute('src'));
        }, PO.mediaItem.image());

        assert.equal(hasImageSrc, true);

        // Кликаем на первую картинку
        await browser.yaClick(PO.mediaItem.imageContainer());
        await browser.yaWaitForVisible(PO.lightbox());

        // TODO: (https://st.yandex-team.ru/MSSNGRFRONT-8106)
        // await browser.waitUntil(async function () {
        //     const imageOpened = await browser.isVisible(PO.lightbox());
        //     const spinnerIsVisible = await browser.isVisible(PO.lightbox.loader.spinner());
        //
        //     return imageOpened && !spinnerIsVisible;
        // }, 5000, 'Изображение не прогрузилось');

        // Ожидаем информацию о картинке
        await browser.yaWaitForVisible(PO.lightbox.toolbar.summary());
        await browser.yaWaitForVisible(PO.lightbox.toolbar.download());
        const download = await browser.$(PO.lightbox.toolbar.download());
        assert.equal(Boolean(await download.getAttribute('href')), true);

        const left = await browser.$(PO.lightbox.left());
        assert.equal(await left.isExisting(), false);

        // Листаем вправо
        for (let i = 0; i < 5; i++) {
            await browser.yaWaitForVisible(PO.lightbox.right());
            await browser.yaClick(PO.lightbox.right());
            await browser.yaWaitForVisible(PO.lightbox.toolbar.summary());
        }

        assert.equal(await left.isExisting(), true);

        // Листаем влево
        for (let i = 0; i < 5; i++) {
            await browser.yaWaitForVisible(PO.lightbox.left());
            await browser.yaClick(PO.lightbox.left());
            await browser.yaWaitForVisible(PO.lightbox.toolbar.summary());
        }

        assert.equal(await left.isExisting(), false);
    });
});
