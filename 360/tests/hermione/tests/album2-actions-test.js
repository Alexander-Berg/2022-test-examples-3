const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { consts } = require('../config');
const { photoItemByName } = require('../page-objects/client');
const albums = require('../page-objects/client-albums-page');
const popups = require('../page-objects/client-popups');
const listing = require('../page-objects/client-content-listing');
const slider = require('../page-objects/slider').common;
const { assert } = require('chai');
const { addResourcesForCleanup, cleanUpAlbums, createAlbumFromTestImages } = require('../helpers/albums');
const photo = require('../page-objects/client-photo2-page').common;
const { NAVIGATION } = require('../config').consts;

/**
 * @param {string} albumTitle
 * @returns {Promise<void>}
 */
async function openAlbumFromAlbumsList(albumTitle) {
    const bro = this.browser;
    await bro.yaOpenSection('albums');
    const albumSelector = albums.albumByName().replace(':titleText', albumTitle);
    await bro.yaWaitForVisible(albumSelector);
    await bro.click(albumSelector);
}

/**
 * @param {string} albumTitle
 * @returns {Promise<void>}
 */
async function assertPublishAndUnpublishAlbum(albumTitle) {
    const bro = this.browser;

    await bro.yaWaitNotificationForResource(albumTitle, consts.TEXT_NOTIFICATION_ALBUM_PUBLISHED, { close: false });

    await bro.yaWaitForVisible(popups.common.shareDialog.textInput());
    const url = await bro.getValue(popups.common.shareDialog.textInput());

    await bro.newWindow(url);

    await bro.yaWaitForVisible(albums.publicAlbum.avatar());
    const title = await bro.getText(albums.publicAlbum.title());

    assert.equal(title, albumTitle);

    await bro.close();
    await bro.yaWaitForVisible(popups.common.shareDialog());
    await bro.yaDeletePublicLinkInShareDialog();
}

/**
 * @param {number} amount
 * @param {string} folder
 * @returns {Promise<string[]>}
 */
async function getPhotosIds(amount, folder) {
    return (await this.browser.fetchResources(40, folder))
        .filter(({ type }) => type === 'file')
        .slice(0, amount)
        .map(({ id }) => id);
}

describe('Действия над ресурсами в альбоме ->', () => {
    afterEach(cleanUpAlbums);

    it('diskclient-5682, diskclient-5828: Удалить файл в личном альбоме', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-302');

        const { testImageFiles } = await createAlbumFromTestImages.call(this);

        const photoSelector = photoItemByName().replace(':title', testImageFiles[1]);
        await bro.yaSelectPhotoItem(photoSelector, true);
        await bro.yaDeleteSelected();

        await bro.yaWaitForHidden(photoSelector);
        await bro.yaAssertProgressBarAppeared();
        await bro.yaAssertProgressBarDisappeared();
        await bro.yaWaitNotificationForResource(testImageFiles[1], consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);

        addResourcesForCleanup.call(this, testImageFiles[1]);

        await bro.yaWaitForVisible(photoItemByName().replace(':title', testImageFiles[0]));
        await bro.yaWaitForVisible(photoItemByName().replace(':title', testImageFiles[2]));
    });

    it('diskclient-5688, diskclient-5830: Удалить файл в слайдере личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-377');

        await createAlbumFromTestImages.call(this);

        const fileNames = await bro.yaGetPhotosNames();

        await bro.click(photoItemByName().replace(':title', fileNames[1]));

        await bro.yaCallActionInSlider('delete');
        addResourcesForCleanup.call(this, fileNames[1]);

        assert.equal(await bro.yaGetActiveSliderImageName(), fileNames[2]);
        await bro.yaWaitNotificationForResource(fileNames[1], consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);

        await bro.yaCallActionInSlider('delete');
        addResourcesForCleanup.call(this, fileNames[2]);

        await bro.yaWaitForHidden(slider.contentSlider());
        await bro.yaWaitNotificationForResource(fileNames[2], consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
    });

    it('diskclient-5689, diskclient-5831: Удалить все файлы в личном альбоме', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-325');

        const { testImageFiles } = await createAlbumFromTestImages.call(this);
        const fileNames = await bro.yaGetPhotosNames();

        await bro.yaSelectPhotosRange(fileNames);
        await bro.yaDeleteSelected();

        await bro.yaAssertProgressBarAppeared();
        await bro.yaAssertProgressBarDisappeared();

        addResourcesForCleanup.call(this, ...testImageFiles);

        await bro.yaWaitForVisible(albums.album2.stub());
    });

    it('diskclient-5954, diskclient-5955: Удалить файл который есть в нескольких личных альбомах', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-502');

        const {
            testImageFiles,
            resourcesIds,
            albumData: firstAlbumData
        } = await createAlbumFromTestImages.call(this, { goToAlbum: false });
        const { albumData: secondAlbumData } = await createAlbumFromTestImages.call(this, { resourcesIds });

        await bro.click(albums.album2.backButton());
        const firstAlbumSelector = albums.albumByName().replace(':titleText', firstAlbumData.title);
        await bro.yaWaitForVisible(firstAlbumSelector);
        await bro.click(firstAlbumSelector);
        await bro.yaWaitAlbumItemsInViewportLoad();

        const photoSelector = photoItemByName().replace(':title', testImageFiles[0]);
        await bro.yaSelectPhotoItem(photoSelector, true);
        await bro.yaDeleteSelected();

        await bro.yaWaitForHidden(photoSelector);
        await bro.yaWaitNotificationForResource(testImageFiles[0], consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
        addResourcesForCleanup.call(this, testImageFiles[0]);

        await bro.click(albums.album2.backButton());
        const secondAlbumSelector = albums.albumByName().replace(':titleText', secondAlbumData.title);
        await bro.yaWaitForVisible(secondAlbumSelector);
        await bro.click(secondAlbumSelector);

        await bro.yaWaitAlbumItemsInViewportLoad();

        await bro.yaWaitForVisible(photoItemByName().replace(':title', testImageFiles[1]));
        await bro.yaWaitForVisible(photoItemByName().replace(':title', testImageFiles[2]));
        await bro.yaWaitForHidden(photoSelector);
    });

    it('diskclient-5695, diskclient-5887: Убрать файл в личном альбоме', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-326');

        const resourcesIds = await getPhotosIds.call(this, 3, '/disk/photos');
        const { testImageFiles } = await createAlbumFromTestImages.call(this, { resourcesIds });

        const photoSelector = photoItemByName().replace(':title', testImageFiles[1]);
        await bro.yaSelectPhotoItem(photoSelector, true);

        await bro.yaCallActionInActionBar('excludeFromPersonalAlbum', false);
        await bro.yaAssertProgressBarAppeared();
        await bro.yaAssertProgressBarDisappeared();

        await bro.yaWaitNotificationForResource('', consts.TEXT_NOTIFICATION_FILE_EXCLUDED);
        await bro.yaWaitForHidden(photoSelector);

        await bro.yaWaitForVisible(photoItemByName().replace(':title', testImageFiles[0]));
        await bro.yaWaitForVisible(photoItemByName().replace(':title', testImageFiles[2]));
    });

    it('diskclient-5774, diskclient-5956: Убрать файл в слайдере личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-378');

        const resourcesIds = await getPhotosIds.call(this, 3, '/disk/photos');
        await createAlbumFromTestImages.call(this, { resourcesIds });
        const fileNames = await bro.yaGetPhotosNames();

        await bro.click(photoItemByName().replace(':title', fileNames[1]));

        await bro.yaCallActionInSlider('excludeFromPersonalAlbum');

        assert.equal(await bro.yaGetActiveSliderImageName(), fileNames[2]);
        await bro.yaWaitNotificationWithText(consts.TEXT_NOTIFICATION_FILE_EXCLUDED);

        await bro.yaCallActionInSlider('excludeFromPersonalAlbum');

        await bro.yaWaitForHidden(slider.contentSlider());
        await bro.yaWaitNotificationWithText(consts.TEXT_NOTIFICATION_FILE_EXCLUDED);
    });

    it('diskclient-5697, diskclient-5891: Убрать все файлы в личном альбоме', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-326');

        const resourcesIds = await getPhotosIds.call(this, 3, '/disk/photos');
        await createAlbumFromTestImages.call(this, { resourcesIds });
        const fileNames = await bro.yaGetPhotosNames();

        await bro.yaSelectPhotosRange(fileNames);

        await bro.yaCallActionInActionBar('excludeFromPersonalAlbum', false);
        await bro.yaAssertProgressBarAppeared();
        await bro.yaAssertProgressBarDisappeared();

        await bro.yaWaitForVisible(albums.album2.stub());
    });

    it('diskclient-5719, diskclient-5866: Переименовать файл в личном альбоме', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-327');

        const { testImageFiles } = await createAlbumFromTestImages.call(this);

        const photoSelector = photoItemByName().replace(':title', testImageFiles[1]);
        await bro.yaSelectPhotoItem(photoSelector, true);

        const newFileName = 'RENAMED_' + testImageFiles[1];
        const newPhotoSelector = photoItemByName().replace(':title', newFileName);
        await bro.yaCallActionInActionBar('rename');
        await bro.yaSetResourceNameAndApply(newFileName);

        await bro.yaAssertProgressBarAppeared();
        await bro.yaAssertProgressBarDisappeared();

        await bro.yaWaitForHidden(photoSelector);
        await bro.yaWaitForVisible(newPhotoSelector);

        await bro.refresh();
        await bro.yaWaitForVisible(newPhotoSelector);
    });

    it('diskclient-5721, diskclient-5867: Переименовать файл в слайдере личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-379');

        const { testImageFiles } = await createAlbumFromTestImages.call(this);

        await bro.click(photoItemByName().replace(':title', testImageFiles[1]));

        const newFileName = 'RENAMED_' + testImageFiles[1];
        await bro.yaCallActionInSlider('rename');
        await bro.yaSetResourceNameAndApply(newFileName);

        await bro.yaWaitForVisible(slider.sliderButtons.infoButton());

        assert.equal(await bro.yaGetActiveSliderImageName(), newFileName);

        await bro.click(slider.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.contentSlider());
    });

    hermione.auth.tus({ login: 'yndx-ufo-test-516', tus_consumer: 'disk-front-client' });
    it('diskclient-5150, diskclient-5161: [Слайдер, альбом] Открытие инфопопапа', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5161' : 'diskclient-5150';

        const album = {
            id: '5e9d71226fcc2afd3c20c01d',
            name: 'Новый альбом',
            item: '/disk/1.jpg'
        };

        await bro.url(`/client/albums/${album.id}?dialog=slider&idDialog=${album.item}`);
        await bro.yaClick(slider.sliderButtons.infoButton());
        await bro.yaWaitForVisible(popups.common.resourceInfoDropdownContent());
        await bro.pause(1000);
        await bro.yaAssertView(this.testpalmId, popups.common.resourceInfoDropdownContent(),
            { ignoreElements: [popups.common.resourceInfoDropdownContent.viewCount()] });
    });

    hermione.auth.tus({ login: 'yndx-ufo-test-516', tus_consumer: 'disk-front-client' });
    it('diskclient-5156, diskclient-5167: [Слайдер, альбомы] Зум изображения', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5167' : 'diskclient-5156';

        const album = {
            id: '5e9d71226fcc2afd3c20c01d',
            name: 'Новый альбом',
            item: '/disk/1.jpg'
        };

        await bro.url(`/client/albums/${album.id}?dialog=slider&idDialog=${album.item}`);

        await bro.yaZoomIn();
        await bro.yaAssertView(`${this.testpalmId}-zoom_in`, slider.contentSlider());

        await bro.yaZoomOut();
        await bro.yaAssertView(`${this.testpalmId}-zoom_out`, slider.contentSlider());
    });

    hermione.auth.tus({ login: 'yndx-ufo-test-516', tus_consumer: 'disk-front-client' });
    it('diskclient-5723, diskclient-5848: Листание файлов в слайдере личного альбома', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5848' : 'diskclient-5723';

        const album = {
            id: '5e9f1c2f09693b2264ba7617',
            name: '51 файл',
            firstItem: '/disk/Загрузки/Красивые фото (не удалять!!!)/2013-01-18 19-13-43.JPG',
            lastItem: '/disk/Загрузки/Красивые фото (не удалять!!!)/2013-02-09 01-38-22.JPG'
        };

        await bro.url(`/client/albums/${album.id}?dialog=slider&idDialog=${album.firstItem}`);

        await bro.yaWaitForVisible(slider.contentSlider.previewImage());
        await bro.pause(500);

        await bro.yaChangeSliderActiveImage(50);
        await assert.include(decodeURIComponent(await bro.getUrl()), album.lastItem);
    });

    it('diskclient-5700, diskclient-5852: Переместить файл в личном альбоме', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-328');

        const { testImageFiles } = await createAlbumFromTestImages.call(this);

        const photoSelector = photoItemByName().replace(':title', testImageFiles[1]);
        await bro.yaSelectPhotoItem(photoSelector, true);

        await bro.yaMoveSelected();

        await bro.yaAssertProgressBarAppeared();
        await bro.yaAssertProgressBarDisappeared();

        addResourcesForCleanup.call(this, testImageFiles[1]);

        await bro.yaWaitForVisible(photoSelector);

        await bro.yaOpenSection('disk');
        await bro.yaAssertListingHas(testImageFiles[1]);
    });

    it('diskclient-5702, diskclient-5854: Переместить файл в слайдере личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-380');

        const { testImageFiles } = await createAlbumFromTestImages.call(this);

        await bro.click(photoItemByName().replace(':title', testImageFiles[1]));

        await bro.yaCallActionInSlider('move');

        await bro.yaSelectFolderInDialogAndApply();
        await bro.yaWaitForHidden(popups.common.selectFolderDialog());
        addResourcesForCleanup.call(this, testImageFiles[1]);

        await bro.yaWaitForVisible(slider.sliderButtons.infoButton());

        assert.equal(await bro.yaGetActiveSliderImageName(), testImageFiles[1]);

        await bro.click(slider.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.contentSlider());

        await bro.yaOpenSection('disk');
        await bro.yaAssertListingHas(testImageFiles[1]);
    });

    it('diskclient-5693, diskclient-5858: Опубликовать файл в личном альбоме', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-329');

        const { testImageFiles } = await createAlbumFromTestImages.call(this);

        const photoSelector = photoItemByName().replace(':title', testImageFiles[1]);
        await bro.yaSelectPhotoItem(photoSelector, true);

        await bro.yaShareSelected();
        await bro.yaGetPublicUrlAndCloseTab();
        await bro.keys('Escape'); // закрытие поделяшки на мобилах
    });

    it('diskclient-5679, diskclient-5861: Копировать файл в личном альбоме', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-330');

        const targetFolderName = 'tmp-target-' + Date.now();
        await bro.yaCreateFolder(targetFolderName);

        addResourcesForCleanup.call(this, targetFolderName);

        const resourcesIds = await getPhotosIds.call(this, 3, '/disk/photos');
        const { testImageFiles } = await createAlbumFromTestImages.call(this, { resourcesIds });

        const photoSelector = photoItemByName().replace(':title', testImageFiles[1]);
        await bro.yaSelectPhotoItem(photoSelector, true);

        await bro.yaCallActionInActionBar('copy');

        await bro.yaWaitForVisible(popups.common.selectFolderDialog());
        await bro.yaSelectFolderInDialogAndApply(targetFolderName);

        await bro.yaWaitNotificationForResource(
            { name: testImageFiles[1], folder: targetFolderName },
            consts.TEXT_NOTIFICATION_FILE_COPIED_TO_FOLDER
        );
    });

    it('diskclient-5659, diskclient-5871: Ограничение при выделении 501 файла в личном альбоме', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5871' : 'diskclient-5659';
        await bro.yaClientLoginFast('yndx-ufo-test-395');

        await bro.url('/client/albums/5e3f1c8b71a18a7813b5f033');

        const firstPhotoSelector = photoItemByName().replace(':title', '19-44.jpg');
        await bro.yaWaitForVisible(firstPhotoSelector);
        await bro.yaSelectPhotoItem(firstPhotoSelector, true);

        await bro.yaScrollToEnd();

        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaClickWithPressedKey(photoItemByName().replace(':title', '17-44.jpg'), consts.KEY_SHIFT);

        await bro.yaWaitForVisible(popups.common.selectionInfoLimitTooltip());
        await bro.pause(200);

        await bro.yaAssertView(this.testpalmId, [popups.common.actionBar(), popups.common.selectionInfoLimitTooltip()],
            {
                tolerance: 7 // для устойчивости к разным методам сглаживания пикселей
            });
    });

    it('diskclient-5676, diskclient-5881: Добавить в альбом один файл из личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-394');

        const resourcesIds = await getPhotosIds.call(this, 6, '/disk/photos');

        const firstAlbum = await createAlbumFromTestImages.call(this, {
            resourcesIds: resourcesIds.slice(0, 3),
            goToAlbum: false
        });
        const secondAlbum = await createAlbumFromTestImages.call(this, {
            resourcesIds: resourcesIds.slice(3, 6)
        });

        const fileName = secondAlbum.testImageFiles[0];
        await bro.click(photoItemByName().replace(':title', fileName));

        await bro.yaCallActionInSlider('addToAlbum');

        await bro.yaWaitForVisible(popups.common.selectAlbumDialog.createAlbum(), 30000);
        const albumSelector = popups.common.selectAlbumDialog.albumByName()
            .replace(':title', firstAlbum.albumData.title);
        await bro.yaWaitForVisible(albumSelector);
        await bro.click(albumSelector);

        await bro.yaWaitForHidden(popups.common.selectAlbumDialog.createAlbum());

        await bro.yaWaitNotificationForResource(
            firstAlbum.albumData.title,
            consts.TEXT_NOTIFICATION_FILE_ADDED_TO_ALBUM,
            { close: false }
        );

        await bro.url(`/client/albums/${firstAlbum.albumData.id}`);
        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaAssertPhotosInAlbum([fileName]);
    });
});

describe('Действия над альбомом ->', () => {
    afterEach(cleanUpAlbums);

    it('diskclient-5957, diskclient-5958: Добавить в альбом несколько файлов из Фото', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-429');

        const resourcesIds = await getPhotosIds.call(this, 1, '/disk/photos');
        const { albumData } = await createAlbumFromTestImages.call(this, { resourcesIds, goToAlbum: false });

        await bro.url('/client/disk/photos');

        await bro.yaWaitForHidden(listing.common.listingSpinner());
        const fileName = '1-18.jpg';
        await bro.yaSelectResource(fileName);

        await bro.yaCallActionInActionBar('addToAlbum', false);
        await bro.yaWaitForVisible(popups.common.selectAlbumDialog.createAlbum(), 30000);
        const albumSelector = popups.common.selectAlbumDialog.albumByName().replace(':title', albumData.title);
        await bro.yaWaitForVisible(albumSelector);
        await bro.click(albumSelector);

        await bro.yaWaitForHidden(popups.common.selectAlbumDialog.createAlbum());

        await bro.yaWaitNotificationForResource(
            albumData.title,
            consts.TEXT_NOTIFICATION_FILE_ADDED_TO_ALBUM,
            { close: false }
        );
        await bro.yaClickNotificationForResource(albumData.title, consts.TEXT_NOTIFICATION_FILE_ADDED_TO_ALBUM, 'a');

        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaAssertPhotosInAlbum([fileName]);
    });

    it('diskclient-680, diskclient-6147: Добавить в альбом несколько файлов из раздела Последние', async function() {
        const bro = this.browser;

        const testData = {
            user: 'yndx-ufo-test-535',
            folder: {
                path: '/disk/data',
                count: 7
            },
            fileName: 'Горы.jpg'
        };

        /**
         * @param {string} surceFolder
         * @param {Array} files
         * @returns {Object} album
         */
        const createTestAlbum = async function(surceFolder, files) {
            const resourcesIds = await getPhotosIds.call(this, files, surceFolder);
            const { albumData: album } = await createAlbumFromTestImages.call(this, { resourcesIds, goToAlbum: false });
            album.selector = popups.common.selectAlbumDialog.albumByName().replace(':title', album.title);
            return album;
        };

        await bro.yaClientLoginFast(testData.user);
        const album = await createTestAlbum.call(this, testData.folder.path, testData.folder.count);

        await bro.url(NAVIGATION.recent.url);
        await bro.yaSelectResource(testData.fileName);

        await bro.yaCallActionInActionBar('addToAlbum', false);
        await bro.yaWaitForVisible(album.selector);

        await bro.click(album.selector);
        await bro.yaWaitForHidden(popups.common.selectAlbumDialog.createAlbum());
        await bro.yaWaitNotificationForResource(
            album.title,
            consts.TEXT_NOTIFICATION_FILE_ADDED_TO_ALBUM,
            { close: false }
        );

        await bro.yaClickNotificationForResource(album.title, consts.TEXT_NOTIFICATION_FILE_ADDED_TO_ALBUM, 'a');
        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaAssertPhotosInAlbum([testData.fileName]);
    });

    it('diskclient-5959, diskclient-5960: Отображение контролов личного альбома', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5960' : 'diskclient-5959';
        await bro.yaClientLoginFast('yndx-ufo-test-433');
        await bro.url('/client/albums/5e52a538a828020d649cdceb');
        await bro.yaWaitAlbumItemsInViewportLoad();

        await bro.click(albums.album2.actionsMoreButton());
        await bro.yaWaitForVisible(albums.album2ActionsDropdown());
        await bro.yaResetPointerPosition();
        await bro.pause(500);

        await bro.yaAssertView(
            `${this.testpalmId}-1`,
            [albums.album2ActionsDropdown(), albums.album2.actionsMoreButton()],
            { hideElements: [albums.album2.grid()] }
        );

        await bro.click(albums.album2ActionsDropdown.setViewButton());
        await bro.yaResetPointerPosition();
        await bro.pause(500);

        await bro.yaAssertView(
            `${this.testpalmId}-2`,
            [albums.album2ActionsDropdown(), albums.album2.actionsMoreButton()],
            { hideElements: [albums.album2.grid()] }
        );
    });

    it('diskclient-5663, diskclient-5873: Добавить файлы в альбом со страницы личного альбома', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-417');

        const resourcesIds = (await this.browser.fetchResources(3, '/disk/photos')).map(({ id }) => id);
        const { albumData } = await createAlbumFromTestImages.call(this, { resourcesIds });

        await bro.click(albums.album2.addToAlbumButton());

        const fileNames = ['10-32.jpg', '10-17.jpg', '10-50.jpg', '10-37.jpg'];
        await bro.yaSelectPhotosAndCreateAlbum(fileNames);

        await bro.yaAssertProgressBarAppeared();
        await bro.yaAssertProgressBarDisappeared();

        await bro.yaWaitForVisible(albums.album2.title());
        assert.strictEqual(await bro.getText(albums.album2.title()), albumData.title);

        await bro.yaAssertPhotosInAlbum(fileNames);
    });

    it('diskclient-5666, diskclient-5855: Опубликовать альбом со страницы личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-421');

        const resourcesIds = await getPhotosIds.call(this, 3, '/disk/photos');
        const { albumData } = await createAlbumFromTestImages.call(this, { resourcesIds, goToAlbum: false });

        await bro.url('/client/albums');
        await openAlbumFromAlbumsList.call(this, albumData.title);

        await bro.yaWaitForVisible(albums.album2.publishButton());
        await bro.click(albums.album2.publishButton());

        await assertPublishAndUnpublishAlbum.call(this, albumData.title);

        if (!(await bro.yaIsMobile())) {
            await bro.click(albums.album2.backButton());
            const albumSelector = albums.albumByName().replace(':titleText', albumData.title);
            await bro.yaWaitForVisible(albumSelector);
            await bro.yaWaitForHidden(albumSelector + ' ' + albums.albumPublishButton());
        }
    });

    it('diskclient-5961, diskclient-5962: Добавить файлы в альбом со страницы личного альбома через меню Еще', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-418');

        const resourcesIds = await getPhotosIds.call(this, 3, '/disk/photos');
        await createAlbumFromTestImages.call(this, { resourcesIds });

        await bro.yaCallActionInAlbumActionsDropdown('addItems');

        const fileNames = ['10-32.jpg', '10-17.jpg', '10-50.jpg', '10-37.jpg'];
        await bro.yaSelectPhotosAndCreateAlbum(fileNames);

        await bro.yaAssertPhotosInAlbum(fileNames);
    });

    it('diskclient-5661, diskclient-5812: Переключение типа сетки в личном альбоме', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5812' : 'diskclient-5661';
        await bro.yaClientLoginFast('yndx-ufo-test-419');

        const resourcesIds = await getPhotosIds.call(this, 10, '/disk/photos');
        await createAlbumFromTestImages.call(this, { resourcesIds });

        /**
         * @param {('tile'|'wow')} view
         * @returns {Promise<void>}
         */
        const assertChangeView = async(view) => {
            await bro.yaCallActionInAlbumActionsDropdown('setView', false);
            await bro.yaWaitForVisible(albums.album2ActionsDropdown.setView());
            await bro.pause(200);
            await bro.click(albums.album2ActionsDropdown.setView[view]());
            await bro.yaWaitForHidden(albums.album2ActionsDropdown());
            await bro.refresh();
            await bro.yaWaitAlbumItemsInViewportLoad();
            await bro.yaResetPointerPosition();
            await bro.yaAssertView(`${this.testpalmId}-${view}`, albums.album2.grid());
        };

        await assertChangeView('tile');
        await assertChangeView('wow');
    });

    it('diskclient-5664, diskclient-5864: Переименовать альбом со страницы личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-399');
        await bro.url('/client/disk');

        const resourcesIds = await getPhotosIds.call(this, 3, '/disk/photos');
        const { albumData } = await createAlbumFromTestImages.call(this, { resourcesIds, goToAlbum: false });

        await openAlbumFromAlbumsList.call(this, albumData.title);

        await bro.yaCallActionInAlbumActionsDropdown('rename');

        const newAlbumTitle = albumData.title + '_RENAME';
        await bro.yaWaitForVisible(popups.common.albumTitleDialog());
        await bro.yaSetValue(popups.common.albumTitleDialog.nameInput(), newAlbumTitle);
        await bro.click(popups.common.albumTitleDialog.submitButton());
        await bro.yaWaitForHidden(popups.common.albumTitleDialog());

        assert.strictEqual(await bro.getText(albums.album2.title()), newAlbumTitle);

        await bro.click(albums.album2.backButton());
        await bro.yaWaitForVisible(albums.albumByName().replace(':titleText', newAlbumTitle));

        await bro.url(`/client/albums/${albumData.id}`);
        await bro.yaWaitForVisible(albums.album2.title());
        assert.strictEqual(await bro.getText(albums.album2.title()), newAlbumTitle);
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-5963: Переименовать альбом из обложки личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-432');

        const resourcesIds = await getPhotosIds.call(this, 3, '/disk/photos');
        const { albumData } = await createAlbumFromTestImages.call(this, { resourcesIds });

        await bro.click(albums.album2.backButton());
        const albumSelector = albums.albumByName().replace(':titleText', albumData.title);
        await bro.yaWaitForVisible(albumSelector);

        // ховер нужен, чтобы появилась кнопка с действиями
        await bro.moveToObject(albumSelector);
        await bro.click(albumSelector + ' ' + albums.albumSettingsButton());
        await bro.yaWaitForVisible(albums.albumSettingsPopup());
        await bro.pause(200);
        await bro.click(albums.albumSettingsPopup.rename());

        const newAlbumTitle = albumData.title + '_RENAME';

        await bro.yaWaitForVisible(popups.common.albumTitleDialog());
        await bro.yaSetValue(popups.common.albumTitleDialog.nameInput(), newAlbumTitle);
        await bro.click(popups.common.albumTitleDialog.submitButton());
        await bro.yaWaitForHidden(popups.common.albumTitleDialog());

        const renamedAlbumSelector = albums.albumByName().replace(':titleText', newAlbumTitle);
        await bro.yaWaitForVisible(renamedAlbumSelector);
        await bro.click(renamedAlbumSelector);

        await bro.yaWaitForVisible(albums.album2.title());
        assert.strictEqual(await bro.getText(albums.album2.title()), newAlbumTitle);
    });

    it('diskclient-5965, diskclient-5966: Опубликовать альбом со страницы личного альбома из меню Еще', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-435');

        const resourcesIds = await getPhotosIds.call(this, 3, '/disk/photos');
        const { albumData } = await createAlbumFromTestImages.call(this, { resourcesIds });

        await bro.yaCallActionInAlbumActionsDropdown('publish', false);

        await assertPublishAndUnpublishAlbum.call(this, albumData.title);
    });

    it('diskclient-5782, diskclient-5857: Удалить ссылку на альбом со страницы личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-434');

        const resourcesIds = await getPhotosIds.call(this, 3, '/disk/photos');
        const { albumData } = await createAlbumFromTestImages.call(this, { resourcesIds, isPublic: true });

        await bro.yaCallActionInAlbumActionsDropdown('unpublish', false);

        await bro.yaWaitNotificationForResource(albumData.title, consts.TEXT_NOTIFICATION_ALBUM_UNPUBLISHED);
    });

    it('diskclient-5665, diskclient-5824: Удалить альбом со страницы личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-420');

        await bro.url('/client/disk');
        const resourcesIds = await getPhotosIds.call(this, 3, '/disk/photos');
        const { albumData } = await createAlbumFromTestImages.call(this, { resourcesIds, goToAlbum: false });

        await openAlbumFromAlbumsList.call(this, albumData.title);

        await bro.yaCallActionInAlbumActionsDropdown('delete');

        await bro.yaWaitForVisible(popups.common.confirmationDialog.submitButton());
        await bro.click(popups.common.confirmationDialog.submitButton());
        await bro.yaWaitForHidden(popups.common.confirmationDialog.submitButton());

        await bro.yaWaitNotificationForResource(albumData.title, consts.TEXT_NOTIFICATION_ALBUM_REMOVED);

        await bro.yaWaitForVisible(albums.albums2.header());
        await bro.yaWaitForHidden(albums.albumByName().replace(':titleText', albumData.title));
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-5967: Удалить альбом из обложки личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-436');

        const resourcesIds = await getPhotosIds.call(this, 3, '/disk/photos');
        const { albumData } = await createAlbumFromTestImages.call(this, { resourcesIds, goToAlbum: false });

        await bro.url('/client/albums');

        const albumSelector = albums.albumByName().replace(':titleText', albumData.title);
        await bro.yaWaitForVisible(albumSelector);

        // ховер нужен, чтобы появилась кнопка с действиями
        await bro.moveToObject(albumSelector);
        await bro.click(albumSelector + ' ' + albums.albumSettingsButton());
        await bro.yaWaitForVisible(albums.albumSettingsPopup());
        await bro.pause(200);
        await bro.click(albums.albumSettingsPopup.delete());

        await bro.yaAcceptDeletePopup();

        await bro.yaWaitForHidden(albumSelector);
        await bro.yaWaitNotificationForResource(albumData.title, consts.TEXT_NOTIFICATION_ALBUM_REMOVED);
    });

    it('diskclient-5969, diskclient-5970: Скачать альбом со страницы личного альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-431');

        await bro.url('/client/albums/5e527d459fd889b95340e68c');
        await bro.yaWaitAlbumItemsInViewportLoad();

        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            await bro.yaCallActionInAlbumActionsDropdown('download');
        });

        assert.match(
            url,
            /downloader\.disk\.yandex\.ru\/zip-album\/.+&filename=Album_for_download\.zip/,
            'Некорректный url для скачивания'
        );
    });

    hermione.auth.tus({ login: 'yndx-ufo-test-516', tus_consumer: 'disk-front-client' });
    it('diskclient-6023, diskclient-6116: Переключение режима отображения при добавлении в альбом', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6116' : 'diskclient-6023';

        /**
         * Проверяется переключение вида на шаге выбора фото, топбар должен оставаться
         *
         * @param {('tile'|'wow')} view
         * @returns {Promise<void>}
         */
        const assertChangeView = async(view) => {
            await bro.yaSetPhotoSliceListingType(view);

            await bro.yaWaitForVisible(photo.addToAlbumBar());
            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.yaAssertView(`${this.testpalmId}-${view}`, photo.photo[view]());
        };

        const album = {
            id: '5e9d71226fcc2afd3c20c01d',
            name: 'Новый альбом'
        };

        await bro.url(`${NAVIGATION.photo.url}%7Cadd-to-album?targetAlbumId=${album.id}`);

        await assertChangeView('tile');
        await assertChangeView('wow');
    });
});
