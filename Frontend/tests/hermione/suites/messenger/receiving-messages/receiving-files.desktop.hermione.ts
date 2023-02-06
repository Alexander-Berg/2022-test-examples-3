hermione.skip.in(/.*/, 'https://st.yandex-team.ru/MSSNGRFRONT-5011');
specs({
    feature: 'Получение файлов и изображений в q',
}, function () {
    it('Получение файла', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.yaWaitForNewFileMessage();

        await browser.waitUntil(async function () {
            const fileUrl = await browser.getAttribute(PO.lastMessage.file.control(), 'href');

            return fileUrl.length > 0;
        }, 5000, 'Файл не загрузился');

        await browser.assertView('message-with-file', PO.lastMessage.file());
    });

    it('Получение изображения', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.yaWaitForNewImageMessage();
        await browser.waitUntil(() => browser.isVisible(PO.lastMessage.image.picture()), 5000, 'Изображение не загрузилось');

        await browser.moveToObject(PO.lastMessage.image());
        await browser.isVisible(PO.lastMessage.balloonInfo.time());

        await browser.click(PO.lastMessage.image());
        await browser.waitUntil(async function () {
            const imageOpened = await browser.isVisible(PO.lightbox());
            const spinnerIsVisible = await browser.isVisible(PO.lightbox.loader.spinner());

            return imageOpened && !spinnerIsVisible;
        }, 5000, 'Изображение не прогрузилось');

        await browser.assertView('full-screen-image', PO.lightbox(), { invisibleElements: [PO.lightbox.toolbar.summary()] });

        await browser.click(PO.lightbox.close());

        const isVisible = await browser.isVisible(PO.lightbox());
        assert.isFalse(isVisible);
    });
});
