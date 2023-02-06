specs({
    feature: 'Отправка файлов в q',
}, function () {
    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-5093');
    it('Отправка файла', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.chooseFile(PO.compose.file(), '/bin/ls');

        await browser.yaWaitForVisible(PO.lastMessage.file(), 'Файл не отправился в чат');
        await browser.waitUntil(async function () {
            const fileUrl = await browser.getAttribute(PO.lastMessage.file.control(), 'href');

            return fileUrl.length > 0;
        }, 5000, 'Файл не загрузился');

        await browser.assertView(
            'file-message', PO.lastMessage.file(),
            { ignoreElements: [PO.lastMessage.balloonInfo(), PO.lastMessage.file.description()] },
        );
    });

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-5093');
    it('Ошибка загрузки файла размером более чем максимальный лимит', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.chooseFile(PO.compose.file(), '/bin/bash');
        await browser.click(PO.modalUploadFiles.upload());
        await browser.yaWaitForVisible(PO.infoMessage.content(), 'Информационное сообщение не появилось');

        const infoMessage = await browser.getText(PO.infoMessage.content());
        assert.equal(infoMessage, 'Можно загружать непустые файлы, размер которых не превышает 500,0 КБ.');
    });
});
