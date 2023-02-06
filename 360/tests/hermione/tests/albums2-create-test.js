const { NAVIGATION } = require('../config').consts;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { setNewAlbumName } = require('../helpers/albums');
const listing = require('../page-objects/client-content-listing');
const popups = require('../page-objects/client-popups');
const navigation = require('../page-objects/client-navigation');
const consts = require('../config').consts;
const albums = require('../page-objects/client-albums-page');
const slider = require('../page-objects/slider');
const photos = require('../page-objects/client-photo2-page');
const { assert } = require('chai');

/**
 * @returns {Promise<void>}
 */
async function createAlbumFromSidebar() {
    const bro = this.browser;
    if (await bro.yaIsMobile()) {
        await bro.yaWaitForVisible(navigation.touch.touchListingSettings.plus());
        await bro.click(navigation.touch.touchListingSettings.plus());
        await bro.yaWaitForVisible(popups.touch.createPopup.createAlbum());
        await bro.pause(200);
        await bro.yaWaitForVisible(popups.touch.createPopup());
        await bro.click(popups.touch.createPopup.createAlbum());
        await bro.yaWaitForHidden(popups.touch.createPopup());
    } else {
        await bro.yaOpenCreatePopup();
        await bro.click(popups.desktop.createPopup.createAlbum());
    }

    await bro.yaWaitForVisible(popups.common.albumTitleDialog());
}

/**
 * @returns {Promise<string>}
 */
async function setNewAlbumRandomName() {
    return setNewAlbumName.call(this);
}

/**
 * @param {string[]} fileNames
 * @returns {Promise<void>}
 */
async function selectPhotosAndCreateAlbum(fileNames) {
    const bro = this.browser;
    for (const photo of fileNames) {
        await bro.click(photos.common.photo.itemByName().replace(':title', photo));
    }
    await bro.click(photos.common.addToAlbumBar.submitButton());
}

/**
 * @param {string} albumTitle
 * @param {string[]} fileNames
 * @returns {Promise<void>}
 */
async function goToAlbumAndAssertPhotos(albumTitle, fileNames) {
    const bro = this.browser;
    await bro.yaOpenSection('albums');
    const albumSelector = albums.albumByName().replace(':titleText', albumTitle);
    await bro.yaWaitForVisible(albumSelector);
    await bro.click(albumSelector);

    await bro.yaAssertPhotosInAlbum(fileNames);
}

/**
 * Проверка на странице альбома и удаление альбома
 *
 * @param {string} albumTitle
 * @param {Array<string>} files
 * @returns {Promise<void>}
 */
async function checkPageAlbumAndCleanup(albumTitle, files) {
    const bro = this.browser;

    await bro.yaWaitAlbumItemsInViewportLoad();

    await bro.yaAssertPhotosInAlbum(files);
    assert.strictEqual(await bro.getText(albums.album2.title()), albumTitle);

    await bro.yaCallActionInAlbumActionsDropdown('delete');
    await bro.yaWaitForVisible(popups.common.confirmationDialog.submitButton());
    await bro.click(popups.common.confirmationDialog.submitButton());
    await bro.yaWaitForHidden(popups.common.confirmationDialog.submitButton());
}

/**
 * Получить id-ник созданного альбома из нотификации про создание альбома
 *
 * @returns {string}
 */
async function getCreatedAlbumIdFromNotification() {
    const bro = this.browser;

    const linkAlbumValue = await bro.getAttribute(popups.common.notifications.link(), 'href');
    return linkAlbumValue.match(/albums\/([\w]+)/)[1];
}

/**
 * Переход в альбом с нотификации о создании альбома
 *
 * @param {string} albumTitle
 * @returns {string} - id созданного альбома
 */
async function goToAlbumFromCreateNotification(albumTitle) {
    const bro = this.browser;

    await bro.yaWaitNotificationForResource(
        albumTitle,
        consts.TEXT_NOTIFICATION_ALBUM_CREATED,
        { close: false }
    );

    const albumId = await getCreatedAlbumIdFromNotification.call(this);

    await bro.yaClickNotificationForResource(albumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);
    await bro.yaCloseNotificationForResource(albumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);

    return albumId;
}

/**
 * Проверка создания публичного альбома
 *
 * @returns {Promise<void>}
 */
async function checkCreatePublicAlbum() {
    const bro = this.browser;

    await bro.yaAssertProgressBarAppeared();
    await bro.yaAssertProgressBarDisappeared();

    await bro.yaWaitForVisible(popups.common.shareDialog());
}

/**
 * Проверка создания альбома и его удаление
 *
 * @param {string} albumTitle
 * @param {Array<string>} files
 * @returns {Promise<void>}
 */
async function checkCreateAlbumAndCleanup(albumTitle, files) {
    await checkCreatePublicAlbum.call(this);
    await goToAlbumFromCreateNotification.call(this, albumTitle);
    await checkPageAlbumAndCleanup.call(this, albumTitle, files);
}

/**
 * Выделение файлов
 *
 * @param {Array<string>} files
 * @param {boolean} [inPhoto]
 * @returns {Promise<void>}
 */
async function selectFiles(files, inPhoto = false) {
    const bro = this.browser;

    if (inPhoto) {
        await bro.yaSelectPhotosRange(files);
    } else {
        await bro.yaSelectResources(files);
    }
}

/**
 * Публикация через топбар
 *
 * @param {Array<string>} files
 * @param {boolean} isMobile
 * @param {boolean} [inPhoto]
 * @returns {Promise<void>}
 */
async function selectAndPublishByTopbar(files, isMobile, inPhoto = false) {
    const bro = this.browser;

    await selectFiles.call(this, files, inPhoto);

    await bro.yaWaitActionBarDisplayed();
    await bro.click(isMobile ?
        popups.touch.actionBar.publishButton() :
        popups.desktop.actionBar.publishButton()
    );
}

/**
 * Публикация через КМ
 *
 * @param {Array<string>} files
 * @param {boolean} [inPhoto]
 * @returns {Promise<void>}
 */
async function selecteAndPublishByContextMenu(files, inPhoto = false) {
    const bro = this.browser;

    await selectFiles.call(this, files, inPhoto);

    await bro.yaWaitActionBarDisplayed();
    if (inPhoto) {
        await bro.yaOpenActionPopupPhoto(files[0]);
    } else {
        await bro.yaOpenActionPopup(files[0]);
    }
    await bro.click(popups.common.actionPopup.publishButton());
}

describe('Альбомы ->', () => {
    describe('создание ->', () => {
        afterEach(async function() {
            try {
                await this.browser.yaDeleteAlbumByName(this.currentTest.ctx.albumName);
            } catch (error) { }
        });

        it('diskclient-5649, diskclient-5840: Создание личного альбома из диалога добавления в альбом', async function() {
            const bro = this.browser;
            const folder = 'photos';
            const pics = ['10-14.jpg', '10-4.jpg'];

            await bro.yaClientLoginFast('yndx-ufo-test-387');
            await bro.url(`/client/disk/${folder}`);
            await bro.yaWaitForHidden(listing.common.listingSpinner(), 10000);
            await bro.yaSelectResources(pics);

            await bro.yaCallActionInActionBar('addToAlbum', false);
            await bro.yaWaitForVisible(popups.common.selectAlbumDialog.createAlbum(), 30000);
            await bro.click(popups.common.selectAlbumDialog.createAlbum());

            const newAlbumTitle = await setNewAlbumRandomName.call(this);
            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();
            await bro.yaCloseActionBar();
            await bro.yaWaitNotificationForResource(
                newAlbumTitle,
                consts.TEXT_NOTIFICATION_ALBUM_CREATED,
                { close: false }
            );
            await bro.yaClickNotificationForResource(newAlbumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);
            await bro.yaCloseNotificationForResource(newAlbumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);
            await bro.yaWaitAlbumItemsInViewportLoad();

            await bro.yaAssertPhotosInAlbum(pics);
            assert.strictEqual(await bro.getText(albums.album2.title()), newAlbumTitle);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-5986: Создание личного альбома из диалога добавления в альбом из контекстного меню', async function() {
            const bro = this.browser;
            const pics = ['10-14.jpg', '10-4.jpg'];

            await bro.yaClientLoginFast('yndx-ufo-test-331');
            await bro.url('/client/disk/create_album_photos');
            await bro.yaWaitForHidden(listing.common.listingSpinner());
            await bro.yaSelectResources(pics);
            await bro.rightClick(listing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, pics[0]));
            await bro.yaWaitForVisible(popups.common.actionPopup());
            await bro.pause(200);

            await bro.click(popups.common.actionPopup.addToAlbumButton());
            await bro.yaWaitForVisible(popups.common.selectAlbumDialog.createAlbum(), 30000);
            await bro.click(popups.common.selectAlbumDialog.createAlbum());

            const newAlbumTitle = await setNewAlbumRandomName.call(this);
            await bro.pause(200);
            await bro.yaCloseActionBar();
            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();
            await bro.yaWaitNotificationForResource(
                newAlbumTitle,
                consts.TEXT_NOTIFICATION_ALBUM_CREATED,
                { close: false }
            );
            await bro.yaClickNotificationForResource(newAlbumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);
            await bro.yaCloseNotificationForResource(newAlbumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);
            await bro.yaWaitAlbumItemsInViewportLoad();

            await bro.yaAssertPhotosInAlbum(pics);
            assert.strictEqual(await bro.getText(albums.album2.title()), newAlbumTitle);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-5985: Создание личного альбома из контекстного меню листинга', async function() {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-392');
            await bro.url('/client/disk/photos');
            await bro.yaWaitForHidden(listing.common.listingSpinner());

            await bro.rightClick(listing.common.listing.head());
            await bro.yaWaitForVisible(popups.desktop.contextMenuCreatePopup());
            await bro.click(popups.desktop.contextMenuCreatePopup.createAlbum());

            const newAlbumTitle = await setNewAlbumRandomName.call(this);

            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.yaWaitPhotosliceWithToolbarOpened();

            const fileNames = ['10-50.jpg', '10-37.jpg', '1-44.jpg'];
            await selectPhotosAndCreateAlbum.call(this, fileNames);

            await bro.yaAssertFolderOpened('photos');

            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await goToAlbumAndAssertPhotos.call(this, newAlbumTitle, fileNames);
        });

        it('diskclient-5988, diskclient-5989: Создание личного альбома из диалога добавления в альбом из слайдера', async function() {
            const bro = this.browser;
            const folder = 'create_album_photos';
            const pic = '10-14.jpg';

            await bro.yaClientLoginFast('yndx-ufo-test-331');
            await bro.url(`/client/disk/${folder}`);
            await bro.yaWaitForHidden(listing.common.listingSpinner());
            await bro.yaOpenListingElement(pic);
            await bro.waitForVisible(slider.common.contentSlider.previewImage());
            await bro.yaCallActionInSliderToolbar('more');
            await bro.yaCallActionInMoreButtonPopup('addToAlbum');
            await bro.yaWaitForVisible(popups.common.selectAlbumDialog.createAlbum(), 30000);
            await bro.click(popups.common.selectAlbumDialog.createAlbum());

            const newAlbumTitle = await setNewAlbumRandomName.call(this);
            await bro.yaWaitNotificationForResource(
                newAlbumTitle,
                consts.TEXT_NOTIFICATION_ALBUM_CREATED,
                { close: false }
            );
            await bro.yaClickNotificationForResource(newAlbumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);
            await bro.yaCloseNotificationForResource(newAlbumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);
            await bro.yaWaitAlbumItemsInViewportLoad();

            await bro.yaAssertPhotosInAlbum([pic]);
            assert.strictEqual(await bro.getText(albums.album2.title()), newAlbumTitle);
        });

        it('diskclient-5737, diskclient-5841: Создание пустого личного альбома', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-5841' : 'diskclient-5737';

            await bro.yaClientLoginFast('yndx-ufo-test-385');
            await bro.url('/client/albums');
            await bro.yaWaitForHidden(albums.albums2.shimmer());
            await bro.yaWaitForVisible(albums.albums2.personal());
            await bro.yaWaitForVisible(albums.albums2.personal.createAlbumButton());
            await bro.click(albums.albums2.personal.createAlbumButton());

            const newAlbumTitle = await setNewAlbumRandomName.call(this);

            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.yaWaitPhotosliceWithToolbarOpened();

            await bro.yaAssertView(this.testpalmId, 'body', {
                ignoreElements: navigation.desktop.spaceInfoSection.infoSpaceButton()
            });
            await bro.click(photos.common.addToAlbumBar.submitButton());
            await bro.yaWaitNotificationForResource(
                newAlbumTitle,
                consts.TEXT_NOTIFICATION_ALBUM_CREATED,
                { close: false }
            );
            await bro.yaWaitForVisible(albums.albums2.personal.createAlbumButton());
            await bro.yaClickNotificationForResource(newAlbumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);
            await bro.yaCloseNotificationForResource(newAlbumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);
            await bro.yaWaitForVisible(albums.album2.stub());
            await bro.yaWaitForHidden(albums.album2.spin());

            assert.strictEqual(await bro.getText(albums.album2.title()), newAlbumTitle);
        });

        it('diskclient-5648, diskclient-5839: Создание личного альбома по обложке Новый альбом', async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-386');

            await bro.url('/client/albums');
            await bro.yaWaitForHidden(albums.albums2.shimmer());

            await bro.yaWaitForVisible(albums.albums2.personal.createAlbumButton());
            await bro.click(albums.albums2.personal.createAlbumButton());
            await bro.yaWaitForVisible(popups.common.albumTitleDialog());

            const newAlbumTitle = await setNewAlbumRandomName.call(this);

            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.yaWaitPhotosliceWithToolbarOpened();

            const fileNames = ['10-32.jpg', '10-17.jpg', '10-50.jpg', '10-37.jpg'];
            for (const photo of fileNames) {
                await bro.click(photos.common.photo.itemByName().replace(':title', photo));
            }

            await bro.click(photos.common.addToAlbumBar.submitButton());

            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            const albumSelector = albums.albumByName().replace(':titleText', newAlbumTitle);
            await bro.yaWaitForVisible(albumSelector);
            // личные альбомы должны быть приватными по умолчанию
            await bro.yaWaitForHidden(albumSelector + ' ' + albums.albumPublishButton());

            await bro.click(albumSelector);

            await bro.yaAssertPhotosInAlbum(fileNames);
        });

        it('diskclient-5647, diskclient-5838: Создание личного альбома по кнопке Создать', async function() {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-384');

            await bro.url('/client/disk');

            await createAlbumFromSidebar.call(this);
            const newAlbumTitle = await setNewAlbumRandomName.call(this);

            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.yaWaitPhotosliceWithToolbarOpened();

            const fileNames = ['10-32.jpg', '10-17.jpg'];
            await selectPhotosAndCreateAlbum.call(this, fileNames);

            await bro.yaAssertSectionOpened('disk');

            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await goToAlbumAndAssertPhotos.call(this, newAlbumTitle, fileNames);
        });

        it('diskclient-6169: Создание альбома с кодом в имени', async function() {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-760');

            await bro.url('/client/disk');

            await createAlbumFromSidebar.call(this);

            const xssAlbumName = '<script>document.write("xss attack")</script>';

            await setNewAlbumName.call(this, xssAlbumName);

            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.yaWaitPhotosliceWithToolbarOpened();

            const fileNames = ['10-32.jpg', '10-17.jpg'];
            await selectPhotosAndCreateAlbum.call(this, fileNames);

            await bro.yaAssertSectionOpened('disk');

            await goToAlbumFromCreateNotification.call(this, xssAlbumName);
        });

        hermione.only.in(clientDesktopBrowsersList);
        hermione.auth.tus({ login: 'yndx-ufo-test-517', tus_consumer: 'disk-front-client' });
        it('diskclient-6022: Повторное создание личного альбома по кнопке Создать', async function() {
            const bro = this.browser;
            await bro.url(NAVIGATION.disk.url);
            await createAlbumFromSidebar.call(this);
            const newAlbumTitle = await setNewAlbumRandomName.call(this);
            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.yaWaitPhotosliceWithToolbarOpened();

            const fileNames = ['2013-02-26 21-38-03.JPG', '2013-02-26 21-32-29.JPG'];
            await selectPhotosAndCreateAlbum.call(this, fileNames);

            await bro.yaAssertSectionOpened('disk');

            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await goToAlbumAndAssertPhotos.call(this, newAlbumTitle, fileNames);

            /**
             * Возвращает рандомный раздел Диска за исключением разделов из neverInclude
             *
             * @param {Array} neverInclude
             * @returns {string}
             */
            const getRandomPath = async(neverInclude) => {
                const allPaths = Object.keys(NAVIGATION).filter((item) => !neverInclude.includes(item));
                return allPaths[Math.floor(Math.random() * allPaths.length)];
            };

            const randomPath = await getRandomPath(['disk', 'tuning', 'folder']);
            await bro.url(NAVIGATION[randomPath].url);
            await createAlbumFromSidebar.call(this);
        });

        it('diskclient-5802, diskclient-5842: Создание личного альбома с названием больше 255 символов', async function() {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-384');

            await bro.yaOpenSection('disk');
            await createAlbumFromSidebar.call(this);

            await bro.yaWaitForVisible(popups.common.albumTitleDialog());
            await bro.yaSetValue(popups.common.albumTitleDialog.nameInput(), consts.TEST_256_CHAR_NAME);

            await bro.yaWaitForVisible(popups.common.albumTitleDialog.errorText());

            assert.equal(
                await bro.getText(popups.common.albumTitleDialog.errorText()),
                consts.TEXT_NOTIFICATION_TITlE_TOO_LONG
            );

            assert.equal(await bro.isEnabled(popups.common.albumTitleDialog.submitButton()), false);
        });

        hermione.skip.in('chrome-phone', 'https://st.yandex-team.ru/CHEMODAN-84263');
        it('diskclient-5975, diskclient-5976: Отмена создания личного альбома', async function() {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-384');

            await bro.url('/client/disk/photos');

            await bro.yaWaitForHidden(listing.common.listingSpinner());
            await bro.yaSelectResource('1-17.jpg');

            await bro.yaCallActionInActionBar('addToAlbum', false);

            await bro.yaWaitForVisible(popups.common.selectAlbumDialog.createAlbum());
            await bro.click(popups.common.selectAlbumDialog.createAlbum());
            await bro.yaWaitForVisible(popups.common.albumTitleDialog());
            await bro.pause(200);

            await bro.click(popups.common.albumTitleDialog.closeButton());
            await bro.yaWaitForHidden(popups.common.albumTitleDialog());
            await bro.yaWaitForVisible(popups.common.selectAlbumDialog());

            await bro.click(popups.common.selectAlbumDialog.closeButton());
            await bro.yaWaitForHidden(popups.common.selectAlbumDialog());

            await bro.yaCallActionInActionBar('addToAlbum', false); // диалог должен вызваться после отмены
            await bro.yaWaitForVisible(popups.common.selectAlbumDialog.createAlbum());
        });
    });

    it('diskclient-5971,  diskclient-5972: Ограничение при создании личного альбома', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5972' : 'diskclient-5971';

        await bro.yaClientLoginFast('yndx-ufo-test-388');

        await bro.url('/client/disk');

        await createAlbumFromSidebar.call(this);

        await bro.click(popups.common.albumTitleDialog.submitButton());
        await bro.yaWaitPhotoSliceItemsInViewportLoad();
        await bro.yaWaitPhotosliceWithToolbarOpened();

        await bro.click(photos.common.photo.itemByName().replace(':title', '30-25.jpg')); // первое фото

        await bro.yaScrollToEnd();
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        // последнее фото
        await bro.yaClickWithPressedKey(photos.common.photo.itemByName().replace(':title', '76-7.jpg'),
            consts.KEY_SHIFT);

        await bro.yaWaitForVisible(popups.common.selectionInfoLimitTooltip());
        await bro.pause(200);

        await bro.yaAssertView(this.testpalmId, [
            photos.common.addToAlbumBar(), popups.common.selectionInfoLimitTooltip()
        ]);
    });

    describe('публикация нескольких фото ->', () => {
        beforeEach(async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-750');
        });

        it('diskclient-6042, diskclient-6041: Создание публичного альбома через топбар через выделение ресурсов в Корне', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-6042' : 'diskclient-6041';

            await bro.url(NAVIGATION.disk.url);
            const files = ['2015-07-06 18-38-01.JPG', '2015-07-06 18-38-45.JPG'];

            await selectAndPublishByTopbar.call(this, files, isMobile);
            await checkCreateAlbumAndCleanup.call(this, '6 июля 2015', files);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6043: Создание публичного альбома через КМ через выделение ресурсов в Корне', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-6043';

            await bro.url(NAVIGATION.disk.url);
            const files = ['2015-07-06 18-39-30.JPG', '2015-07-06 18-39-36.JPG'];

            await selecteAndPublishByContextMenu.call(this, files);
            await checkCreateAlbumAndCleanup.call(this, '6 июля 2015', files);
        });

        it('diskclient-6047, diskclient-6045: Создание публичного альбома через топбар через выделение ресурсов в разделе Последние', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-6047' : 'diskclient-6045';

            await bro.url(NAVIGATION.recent.url);
            const files = ['2015-07-06 18-38-01.JPG', '2015-07-06 18-38-45.JPG'];

            await selectAndPublishByTopbar.call(this, files, isMobile);
            await checkCreateAlbumAndCleanup.call(this, '6 июля 2015', files);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6046: Создание публичного альбома через КМ через выделение ресурсов в разделе Последние', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-6046';

            await bro.url(NAVIGATION.disk.url);
            const files = ['2015-07-06 18-39-30.JPG', '2015-07-06 18-39-36.JPG'];

            await selecteAndPublishByContextMenu.call(this, files);
            await checkCreateAlbumAndCleanup.call(this, '6 июля 2015', files);
        });

        it('diskclient-6051, diskclient-6049: Создание публичного альбома через топбар через выделение ресурсов в Фотосрезе', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-6051' : 'diskclient-6049';

            await bro.url(NAVIGATION.photo.url);
            const files = ['2015-07-06 18-38-01.JPG', '2015-07-06 18-38-45.JPG'];

            await selectAndPublishByTopbar.call(this, files, isMobile, true);
            await checkCreateAlbumAndCleanup.call(this, '6 июля 2015', files);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6050: Создание публичного альбома через КМ через выделение ресурсов в Фотосрезе', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-6050';

            await bro.url(NAVIGATION.photo.url);
            const files = ['2019-11-20 08-27-00.JPG', '2019-11-20 08-26-55.JPG'];

            await selecteAndPublishByContextMenu.call(this, files, true);
            await checkCreateAlbumAndCleanup.call(this, '20 ноября 2019', files);
        });

        it('diskclient-6057, diskclient-6058: Создание публичного альбома через топбар через выделение ресурсов в Авто-альбоме', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-6057' : 'diskclient-6058';

            await bro.url(`${NAVIGATION.photo.url}?filter=beautiful`);
            const files = ['2019-11-20 08-27-00.JPG', '2019-11-20 08-26-55.JPG'];

            await selectAndPublishByTopbar.call(this, files, isMobile, true);
            await checkCreateAlbumAndCleanup.call(this, '20 ноября 2019', files);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6059: Создание публичного альбома через КМ через выделение ресурсов в Авто-альбоме', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-6059';

            await bro.url(`${NAVIGATION.photo.url}?filter=beautiful`);
            const files = ['2019-11-20 08-27-00.JPG', '2019-11-20 08-26-55.JPG'];

            await selecteAndPublishByContextMenu.call(this, files, true);
            await checkCreateAlbumAndCleanup.call(this, '20 ноября 2019', files);
        });

        it('diskclient-6055, diskclient-6053: Создание публичного альбома через топбар через выделение ресурсов в Личном альбоме', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-6055' : 'diskclient-6053';

            await bro.url(NAVIGATION.disk.url);
            const files = ['2015-07-06 18-38-01.JPG', '2015-07-06 18-38-45.JPG'];

            await selectAndPublishByTopbar.call(this, files, isMobile);

            const albumTitle = '6 июля 2015';

            await checkCreatePublicAlbum.call(this);
            const personalAlbumId = await goToAlbumFromCreateNotification.call(this, albumTitle);
            await bro.yaWaitAlbumItemsInViewportLoad();

            await selectAndPublishByTopbar.call(this, files, isMobile, true);
            await checkCreateAlbumAndCleanup.call(this, albumTitle, files);

            await bro.url(`${NAVIGATION.albums.url}/${personalAlbumId}`);
            await bro.yaWaitAlbumItemsInViewportLoad();
            await checkPageAlbumAndCleanup.call(this, albumTitle, files);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6054: Создание публичного альбома через КМ через выделение ресурсов в Личном альбоме', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-6054';

            await bro.url(NAVIGATION.disk.url);
            const files = ['2015-07-06 18-38-01.JPG', '2015-07-06 18-38-45.JPG'];

            await selecteAndPublishByContextMenu.call(this, files);

            const albumTitle = '6 июля 2015';

            await checkCreatePublicAlbum.call(this);
            const personalAlbumId = await goToAlbumFromCreateNotification.call(this, albumTitle);
            await bro.yaWaitAlbumItemsInViewportLoad();

            await selecteAndPublishByContextMenu.call(this, files, true);
            await checkCreateAlbumAndCleanup.call(this, albumTitle, files);

            await bro.url(`${NAVIGATION.albums.url}/${personalAlbumId}`);
            await bro.yaWaitAlbumItemsInViewportLoad();
            await checkPageAlbumAndCleanup.call(this, albumTitle, files);
        });

        it('diskclient-6063, diskclient-6061: Отсутствие кнопки Поделения в топбаре при выделении ресурсов, отличных от изображения и видео', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-6063' : 'diskclient-6061';

            await bro.url(NAVIGATION.disk.url);
            const files = ['2015-07-06 18-39-30.JPG', 'no_name'];

            await selectFiles.call(this, files);

            await bro.yaWaitActionBarDisplayed();
            await bro.yaWaitForHidden(isMobile ?
                popups.touch.actionBar.publishButton() :
                popups.desktop.actionBar.publishButton()
            );
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6062: Отсутствие кнопки Поделения в КМ при выделении ресурсов, отличных от изображения и видео', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-6062';

            await bro.url(NAVIGATION.disk.url);
            const files = ['2015-07-06 18-39-30.JPG', 'no_name'];

            await selectFiles.call(this, files);

            await bro.yaOpenActionPopup(files[0]);
            await bro.yaWaitForHidden(popups.common.actionPopup.publishButton());
        });

        it('diskclient-6106, diskclient-6105: Появление нотификации после дизактивации публичного альбома', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-6106' : 'diskclient-6105';

            await bro.url(NAVIGATION.disk.url);
            const files = ['2015-07-06 18-38-01.JPG', '2015-07-06 18-38-45.JPG'];

            await selectAndPublishByTopbar.call(this, files, isMobile);

            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await bro.yaWaitForVisible(popups.common.shareDialog.textInput());

            const albumTitle = '6 июля 2015';

            await bro.yaWaitNotificationForResource(
                albumTitle,
                consts.TEXT_NOTIFICATION_ALBUM_CREATED,
                { close: false }
            );

            const albumId = await getCreatedAlbumIdFromNotification.call(this);

            await bro.yaDeletePublicLinkInShareDialog();

            await bro.yaCloseNotificationForResource(albumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);

            await bro.url(`${NAVIGATION.albums.url}/${albumId}`);

            await checkPageAlbumAndCleanup.call(this, albumTitle, files);
        });
    });
});
