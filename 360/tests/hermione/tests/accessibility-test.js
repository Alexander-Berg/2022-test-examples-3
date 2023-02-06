const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { assert } = require('chai');
const { NAVIGATION, PASSPORT_URL } = require('../config').consts;
const clientPageObjects = require('../page-objects/client');
const clientContentListing = require('../page-objects/client-content-listing');
const popups = require('../page-objects/client-popups');
const { setNewAlbumName } = require('../helpers/albums');
const navigation = require('../page-objects/client-navigation');

const PHOTO_NAME = 'photo_bbt.jpeg';

const goToTmpFolder = async (bro) => {
    await bro.url(NAVIGATION.disk.url);
    await bro.yaWaitForVisible(clientContentListing.common.listingBody.items());
    await bro.keys('Right arrow');
    await bro.keys('Right arrow');
    await bro.keys('Enter');
    await bro.yaAssertFolderOpened('FolderForTmp');
};

const checkNotificationAlertWithText = async (bro, text) => {
    await bro.yaWaitForVisible(popups.common.notifications());
    const notificationElement = await bro.element(popups.common.notifications());
    const ariaLiveText = await notificationElement.getAttribute('aria-live');
    assert.equal(ariaLiveText, 'assertive');
    await bro.yaWaitNotificationWithText(text);
};

const enterAddToAlbumDialog = async (bro) => {
    await bro.url(NAVIGATION.disk.url);
    await bro.yaWaitForVisible(clientContentListing.common.listingBody.items());
    for (let i = 0; i < 6; i++) {
        await bro.keys('Right arrow');
    }
    const activeElementText = await bro.execute(() => document.activeElement.textContent);
    assert.equal(activeElementText, PHOTO_NAME);

    await bro.rightClick(clientContentListing.common.listingBodyItemsXpath().replace(/:titleText/g, PHOTO_NAME));
    await bro.keys('Tab');
    const menuActiveElementText = await bro.execute(() => document.activeElement.textContent);
    assert.equal(menuActiveElementText, 'Добавить в альбом');

    await bro.keys('Enter');
    await bro.yaWaitForVisible(popups.common.selectAlbumDialog.createAlbum());
    // Ждём автофокус
    let newElementText = await bro.execute(() => document.activeElement.textContent);
    if (newElementText !== 'Новый альбом') {
        // В FF и хроме фокусы встают на разные элементы изначално.
        // Может быть и 'Новый альбом' и крестик. И та и та ситуация валидная.
        await bro.keys('Tab');
    }
    newElementText = await bro.execute(() => document.activeElement.textContent);
    assert.equal(newElementText, 'Новый альбом');
};

hermione.only.in(clientDesktopBrowsersList);
describe('Доступность', () => {
    beforeEach(async function() {
        await this.browser.yaClientLoginFast('yndx-ufo-test-639');
    });

    it('diskclient-7112: Разлогин из Диска', async function() {
        const bro = this.browser;
        await bro.url(NAVIGATION.disk.url);

        await bro.yaFocus(clientPageObjects.psHeader.suggest.input());
        await bro.pause(100);

        await bro.keys('Tab');
        // Ждём завершение анимации серча
        await bro.pause(1000);

        for (let i = 0; i < 5; i++) {
            await bro.keys('Tab');
        }

        const moreClassName = await bro.execute(() => document.activeElement.className);
        assert.include(moreClassName, 'PSHeader-ServiceList-More');
        await bro.keys('Tab');
        await bro.keys('Tab');
        await bro.keys('Enter');

        for (let i = 0; i < 7; i++) {
            await bro.keys('Tab');
        }
        await bro.yaAssertView('diskclient-7112-1', clientPageObjects.psHeader.legoUser.popup(), {
            hideElements: clientPageObjects.psHeader.legoUser.popup.unreadCounter()
        });

        await bro.keys('Enter');
        await bro.yaAssertUrlInclude(PASSPORT_URL);
    });

    it('diskclient-7140: Копирование файла из контекстного меню', async function() {
        const bro = this.browser;
        await bro.url(NAVIGATION.disk.url);
        await bro.yaWaitForVisible(clientContentListing.common.listingBody.items());
        await bro.keys('Right arrow');
        await bro.keys('Right arrow');
        await bro.yaAssertView(
            'diskclient-7140-1',
            clientContentListing.common.listingBodyItemsXpath().replace(/:titleText/g, 'FolderForTmp')
        );
        await bro.keys('Enter');
        await bro.yaAssertFolderOpened('FolderForTmp');
        const testFileName = await bro.yaUploadFiles('test-file.txt', { uniq: true });

        await bro.yaSelectResource(testFileName);
        await bro.rightClick(clientContentListing.common.listingBodyItemsXpath().replace(/:titleText/g, testFileName));
        // Появляется поповер контекстного меню
        // Идём до пункта "Копировать"
        for (let i = 0; i < 5; i++) {
            await bro.keys('Tab');
        }
        await bro.keys('Enter');
        // Появляется поповер копирования, ждём дерево папок и фокус
        await bro.yaWaitForVisible(popups.common.confirmationDialog.content.tree.item.inner());
        // Доходим до целевой папки
        for (let i = 0; i < 6; i++) {
            await bro.keys('Tab');
        }
        // Выбираем папку
        await bro.keys('Enter');
        const rowElementText = await bro.execute(() => document.activeElement.textContent);
        assert.equal(rowElementText, 'FolderForTmpCopy');
        // Идём до кнопки "Копировать"
        for (let i = 0; i < 3; i++) {
            await bro.keys('Tab');
        }
        await bro.keys('Enter');
        // Запустили копирование. Ждём и проверяем нотификацию
        await checkNotificationAlertWithText(bro, `Файл «${testFileName}» скопирован в папку «FolderForTmpCopy»`);
    });

    it('diskclient-7143: Перемещение файла из контекстного меню', async function() {
        const bro = this.browser;
        await goToTmpFolder(bro);
        const testFileName = await bro.yaUploadFiles('test-file.txt', { uniq: true });

        await bro.yaSelectResource(testFileName);
        await bro.rightClick(clientContentListing.common.listingBodyItemsXpath().replace(/:titleText/g, testFileName));
        // Появляется поповер контекстного меню
        // Идём до пункта "Переместить"
        for (let i = 0; i < 4; i++) {
            await bro.keys('Tab');
        }
        await bro.keys('Enter');
        // Появляется поповер перемещения, ждём дерево папок
        await bro.yaWaitForVisible(popups.common.confirmationDialog.content.tree.item.inner());
        // Доходим до целевой папки
        for (let i = 0; i < 7; i++) {
            await bro.keys('Tab');
        }
        // Выбираем папку
        await bro.keys('Enter');
        await bro.yaAssertView('diskclient-7143-1', popups.common.confirmationDialog.content.tree());
        // Идём до кнопки "Переместить"
        for (let i = 0; i < 2; i++) {
            await bro.keys('Tab');
        }
        await bro.keys('Enter');
        // Запустили копирование. Ждём и проверяем нотификацию
        await checkNotificationAlertWithText(bro, `Файл «${testFileName}» перемещен в «FolderForTmpMove»`);
    });

    it('diskclient-7156, diskclient-7173, diskclient-7174: Удаление файла из контекстного меню, Очистка корзины, Восстановление файла', async function() {
        const bro = this.browser;
        await goToTmpFolder(bro);
        const testFileName = await bro.yaUploadFiles('test-file.txt', { uniq: true });

        await bro.yaSelectResource(testFileName);
        await bro.rightClick(clientContentListing.common.listingBodyItemsXpath().replace(/:titleText/g, testFileName));
        // Доходим до кнопки удалить
        for (let i = 0; i < 7; i++) {
            await bro.keys('Tab');
        }

        const activeElementText = await bro.execute(() => document.activeElement.textContent);
        assert.equal(activeElementText, 'Удалить');
        await bro.keys('Enter');

        // Запустили удаление. Ждём и проверяем нотификацию
        await checkNotificationAlertWithText(bro, `Файл «${testFileName}» перемещён в Корзину`);

        await bro.yaFocus(navigation.desktop.sidebarButtons.upload.input());
        // Доходим до корзины в навигации
        for (let i = 0; i < 11; i++) {
            await bro.keys('Tab');
        }
        const trashElementText = await bro.execute(() => document.activeElement.textContent);
        assert.equal(trashElementText, 'Корзина');
        await bro.keys('Enter');
        await bro.yaWaitForVisible(clientContentListing.common.listingBody.items());
        await bro.pause(1000); // Очистить корзину раздизейбливается асинхронно
        await bro.keys(['Shift', 'Tab']);
        await bro.keys(['Shift', 'Tab']);
        const clearAllText = await bro.execute(() => document.activeElement.textContent);
        assert.include(clearAllText, 'Очистить Корзину');
        // Жмём кнопку, появляется confirmation диалог (и он в фокусе), для проверки доступности этого достаточно
        await bro.keys('Enter');
        await bro.yaWaitForVisible(popups.common.confirmationDialog());
        await bro.keys('Escape');
        await bro.yaWaitForHidden(popups.common.confirmationDialog());
        // Восстанавливаем файл
        await bro.yaSelectResource(testFileName);
        await bro.rightClick(clientContentListing.common.listingBodyItemsXpath().replace(/:titleText/g, testFileName));
        await bro.keys('Enter');
        await checkNotificationAlertWithText(bro, `Файл «${testFileName}» восстановлен`);
    });

    it('diskclient-7150: Поделение файлом из контекстного меню', async function() {
        const bro = this.browser;
        await goToTmpFolder(bro);
        const testFileName = await bro.yaUploadFiles('test-file.txt', { uniq: true });

        await bro.yaSelectResource(testFileName);
        await bro.rightClick(clientContentListing.common.listingBodyItemsXpath().replace(/:titleText/g, testFileName));
        // Первая кнопка в фокусе - поделиться
        const activeElementText = await bro.execute(() => document.activeElement.textContent);
        assert.equal(activeElementText, 'Поделиться');
        await bro.keys('Enter');

        await checkNotificationAlertWithText(bro, 'Ссылка скопирована');

        await bro.yaWaitForVisible(popups.common.shareDialog());
        await bro.keys('Tab');
        const closeElementText = await bro.execute(() => document.activeElement.getAttribute('aria-label'));
        assert.equal(closeElementText, 'Закрыть');

        await bro.keys('Tab');
        const removeElementText = await bro.execute(() => document.activeElement.getAttribute('title'));
        assert.equal(removeElementText, 'Удалить ссылку');

        await bro.keys('Tab');
        const copyElementText = await bro.execute(() => document.activeElement.textContent);
        assert.equal(copyElementText, 'Скопировать ссылку');

        await bro.keys('Tab');
        const mailElementText = await bro.execute(() => document.activeElement.getAttribute('aria-label'));
        assert.equal(mailElementText, 'Поделиться в письме');

        await bro.keys('Tab');
        const qrElementText = await bro.execute(() => document.activeElement.getAttribute('aria-label'));
        assert.equal(qrElementText, 'Через QR-код');
    });

    it('diskclient-7165, diskclient-7164: Создание нового альбома, Добавление в альбом фото из контекстного меню', async function() {
        const bro = this.browser;
        await enterAddToAlbumDialog(bro);
        await bro.keys('Enter');
        const albumName = await setNewAlbumName.call(this);
        await checkNotificationAlertWithText(bro, `Вы создали альбом «${albumName}»`);

        await enterAddToAlbumDialog(bro);
        await bro.keys('Tab');
        await bro.keys('Tab');
        let selectedAlbumName = await bro.execute(() => document.activeElement.textContent);
        for (let i = 0; i < 99999; i++) {
            if (selectedAlbumName === albumName) {
                break;
            }
            bro.keys('Tab');
            selectedAlbumName = await bro.execute(() => document.activeElement.textContent);
        }
        assert.equal(selectedAlbumName, albumName);
        await bro.keys('Enter');

        await checkNotificationAlertWithText(bro, 'Выбранные файлы уже находятся в альбоме');
    });

    it('Переименование файла', async function() {
        const bro = this.browser;
        await goToTmpFolder(bro);
        const testFileName = await bro.yaUploadFiles('test-file.txt', { uniq: true });
        const splitTestFileName = testFileName.split('.');
        splitTestFileName[splitTestFileName.length - 2] += '-renamed';
        const newTestFileName = splitTestFileName.join('.');

        await bro.yaSelectResource(testFileName);
        await bro.rightClick(clientContentListing.common.listingBodyItemsXpath().replace(/:titleText/g, testFileName));
        // Доходим до кнопки переименовать
        for (let i = 0; i < 3; i++) {
            await bro.keys('Tab');
        }

        const renameElementText = await bro.execute(() => document.activeElement.textContent);
        assert.equal(renameElementText, 'Переименовать');
        await bro.keys('Enter');
        await bro.yaWaitForVisible(popups.common.createDialog.nameInput());
        // Появляется поповер переименования, ждём фокуса на инпуте, он асинхронный
        await bro.pause(500);
        const renameInputValue = await bro.execute(() => document.activeElement.value);
        assert.equal(renameInputValue, testFileName);

        await bro.yaSetValue(popups.common.createDialog.nameInput(), newTestFileName);
        await bro.keys('Enter');
        await bro.yaAssertListingHas(newTestFileName);
    });

    it('Навигация по листингу используя стрелки, Enter, Backspace', async function() {
        const bro = this.browser;
        await bro.url(NAVIGATION.disk.url);
        await bro.yaWaitForVisible(clientContentListing.common.listingBody.items());
        // Выбираем папку и заходим в неё
        await bro.keys('Right arrow');
        await bro.keys('Right arrow');
        await bro.keys('Left arrow');
        await bro.keys('Enter');
        await bro.yaAssertFolderOpened('Folder2');
        // Возвращаемся обратно в файлы
        await bro.yaWaitForVisible(clientContentListing.common.listingBody.items());
        await bro.keys('Backspace');
        // Заходим в другую папку
        await bro.yaWaitForVisible(clientContentListing.common.listingBody.items());
        await bro.keys('Right arrow');
        await bro.keys('Up arrow');
        await bro.keys('Enter');
        await bro.yaAssertFolderOpened('Folder1');
    });
});
