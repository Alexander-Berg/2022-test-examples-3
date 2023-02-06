specs({
    feature: 'Отправка медиафайлов',
}, function () {
    const path = require('path');
    const faker = require('faker');
    const image1 = path.join(__dirname, 'test-data', 'img1.png');
    const image2 = path.join(__dirname, 'test-data', 'img2.jpg');

    hermione.skip.in(['chrome-pad', 'chrome-phone', 'firefox'],
        'upload file https://st.yandex-team.ru/MSSNGRFRONT-7924');
    it('Можно удалить отправляемые файлы в попапе отправки', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.chooseFile(PO.compose.file(), '/bin/ls');

        await browser.$(PO.modalUploadFiles.item.cancelHidden()).isExisting();

        await browser.chooseFile(PO.modalUploadFiles.file(), '/bin/bash');

        await browser.execute(function (selector) {
            if (document.querySelectorAll(selector).length !== 2) {
                throw new Error('Должно быть 2 файла');
            }
        }, PO.modalUploadFiles.item());

        await browser.moveToObject(PO.modalUploadFiles.item());

        await browser.waitForVisible(PO.modalUploadFiles.item.cancel());

        await browser.yaClick(PO.modalUploadFiles.item.cancel());

        await browser.execute(function (selector) {
            if (document.querySelectorAll(selector).length !== 1) {
                throw new Error('Должен быть 1 файл');
            }
        }, PO.modalUploadFiles.item());
    });

    hermione.skip.in(['chrome-pad', 'chrome-phone'], 'https://st.yandex-team.ru/MSSNGRFRONT-7658');
    it('Можно отправить изображение, добавив к нему подпись', async function () {
        const { browser } = this;
        const text1 = 'Изображение с текстом 1';

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });
        await browser.chooseFile(PO.compose.file(), image1);

        await browser.click(PO.modalUploadFiles.input());
        await browser.setValue(PO.modalUploadFiles.input(), text1);

        await browser.click(PO.modalUploadFiles.upload());
        await browser.yaWaitForNewGalleryMessage(text1, { type: 'send' });

        const text2 = 'Изображение с текстом 2';

        await browser.chooseFile(PO.compose.file(), image2);
        await browser.chooseFile(PO.modalUploadFiles.file(), image1);

        await browser.click(PO.modalUploadFiles.input());
        await browser.setValue(PO.modalUploadFiles.input(), text2);

        await browser.click(PO.modalUploadFiles.upload());
        await browser.yaWaitForNewGalleryMessage(text2, { type: 'send' });
    });

    hermione.skip.in(['chrome-pad', 'chrome-phone', 'firefox'],
        'firefox: https://st.yandex-team.ru/MSSNGRFRONT-7924');
    it('Можно отменить загрузку изобаржений / файлов закрыв попап', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.chooseFile(PO.compose.file(), '/bin/ls');

        await browser.execute(function (selector) {
            if (document.querySelectorAll(selector).length !== 1) {
                throw new Error('Должен быть 1 файл');
            }
        }, PO.modalUploadFiles.item());
        await browser.yaCloseModal(browser);

        await browser.yaWaitForHidden(PO.modalUploadFiles(), 'Модальное окно загрузки не закрылось');
    });

    hermione.skip.in(['chrome-pad', 'chrome-phone', 'firefox'],
        'firefox: https://st.yandex-team.ru/MSSNGRFRONT-7924');
    it('При отправке изображения есть троббер загрузки', async function () {
        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        // Загружаем 1 картинку
        await browser.chooseFile(PO.compose.file(), image1);

        await browser.click(PO.modalUploadFiles.upload());
        await browser.yaWaitForNewImageMessage({ status: 'progress', type: 'send' });
        await browser.yaWaitForNewImageMessage({ status: 'complete', type: 'send' });

        // Загружаем 2 картинки
        await browser.chooseFile(PO.compose.file(), image1);
        await browser.chooseFile(PO.modalUploadFiles.file(), image2);

        await browser.click(PO.modalUploadFiles.upload());

        await browser.execute(function (selector) {
            if (document.querySelectorAll(selector).length !== 2) {
                throw new Error('Должно загружаться 2 изображения в галлерее');
            }
        }, '.message:last-child [data-test-tag="gallery-image-status-progress"]');

        await new Promise((resolve) => setTimeout(resolve, 700));

        await browser.execute(function (selector) {
            if (document.querySelectorAll(selector).length !== 2) {
                throw new Error('Должно быть загружено 2 изображения в галлерее');
            }
        }, '.message:last-child [data-test-tag="gallery-image-status-complete"]');
    });

    hermione.skip.in(['chrome-pad', 'chrome-phone', 'firefox'],
        'firefox: https://st.yandex-team.ru/MSSNGRFRONT-7924');
    it('Файл размером менее 50МБ можно выбрать и отправить, добавив к нему подпись', async function () {
        const file = path.join(__dirname, 'test-data', 'some-file.txt');
        faker.seed(44);
        const text = faker.lorem.sentence(5);

        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.chooseFile(PO.compose.file(), file);

        await browser.click(PO.modalUploadFiles.input());
        await browser.setValue(PO.modalUploadFiles.input(), text);
        await browser.click(PO.modalUploadFiles.upload());

        await browser.yaWaitForHidden(PO.modalUploadFiles());

        await browser.yaWaitForNewTextMessage(text, {
            type: 'send',
            index: 1,
            waitForSend: true,
        });
        await browser.yaWaitForNewFileMessage({
            fileName: 'some-file.txt',
            type: 'send',
            waitForSend: true,
        });
    });

    hermione.skip.in(['chrome-pad', 'chrome-phone', 'firefox'],
        'firefox: https://st.yandex-team.ru/MSSNGRFRONT-7924');
    it('Можно отменить загрузку изображения', async function () {
        const { browser } = this;

        faker.seed(42);
        const text = faker.lorem.sentence(5);

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.chooseFile(PO.compose.file(), image1);
        await browser.click(PO.modalUploadFiles.upload());

        await browser.yaWaitForHidden(PO.modalUploadFiles());

        const messageSelector = await browser.yaGetContainsSelector(PO.messageImage.statusProgress(), undefined, text);

        if (!messageSelector) {
            throw new Error('No message selector');
        }

        await browser.yaWaitForVisible(messageSelector, 1000);

        await browser.click(PO.messageImage.statusProgress());
        await browser.yaWaitForHidden(messageSelector);
    });

    hermione.skip.in(['chrome-pad', 'chrome-phone', 'firefox'],
        'firefox: https://st.yandex-team.ru/MSSNGRFRONT-7924');
    it('Можно отменить загрузку видео/файла', async function () {
        const file = path.join(__dirname, 'test-data', 'some-file.txt');

        faker.seed(43);
        const text = faker.lorem.sentence(5);

        const { browser } = this;

        await browser.yaOpenMessenger({
            userAlias: 'user',
        });

        await browser.chooseFile(PO.compose.file(), file);
        await browser.click(PO.modalUploadFiles.upload());

        await browser.yaWaitForVisible(PO.messageFile.statusProgress());

        const messageSelector = await browser.yaGetContainsSelector(PO.messageFile.statusProgress(), undefined, text);

        await browser.yaWaitForVisible(messageSelector);

        await browser.click(PO.messageFile.statusProgress.button());

        await browser.yaWaitForHidden(messageSelector);
    });
});
