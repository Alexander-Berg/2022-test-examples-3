const { NAVIGATION } = require('../config').consts;
const albums = require('../page-objects/client-albums-page');
const slider = require('../page-objects/slider').common;
const popups = require('../page-objects/client-popups');
const { retriable } = require('@ps-int/ufo-hermione/helpers/retries');
const { photoItemByName } = require('../page-objects/client');
const { addResourcesForCleanup, cleanUpAlbums, createAlbumFromTestImages } = require('../helpers/albums');
const { consts } = require('../config');
const _ = require('lodash');
const { assert } = require('chai');

/**
 * @param {boolean} [openSection]
 * @returns {Promise<void>}
 */
async function openFavorites(openSection) {
    const bro = this.browser;
    if (openSection) {
        await bro.yaOpenSection('albums');
    } else {
        await bro.url(NAVIGATION.albums.url);
    }
    await waitForShimmersToHide(bro);
    await bro.yaWaitForVisible(albums.albums2.favorites());
    await bro.click(albums.albums2.favorites());
    await bro.yaWaitAlbumItemsInViewportLoad();
}

/**
 * @param {string} selector
 * @returns {Array} itemsinAlbum
 */
async function getItemsInAlbum(selector) {
    const bro = this.browser;
    const array = await bro.execute((selector) => {
        return Array.from(document.querySelectorAll(selector), (element) => element.title);
    }, selector);
    return array;
}

/**
 * @param {Object} bro
 */
async function waitForShimmersToHide(bro) {
    await retriable(async () => {
        const shimmers = await bro.$$(albums.albums2.shimmer());

        assert(!shimmers.length, 'Шиммеры все еще видны');
    }, 5, 1000);
}

/**
 * @param {Object} bro
 */
async function checkPreviewDisplayed(bro) {
    await retriable(async () => {
        const previews = await bro.$$(slider.contentSlider.previewImage());
        const displayed = await Promise.all(previews.map((preview) => preview.isDisplayed()));

        assert(displayed.some(Boolean), 'Картинка не показалась в слайдере');
    }, 5, 1000);
}

describe('Избранное ->', () => {
    describe('Отображение альбома избранные', () => {
        it('diskclient-5898, 5948, 5897, 5947: Отображение избранного альбома и подгрузка ресурсов в нем', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-5898, 5948' : 'diskclient-5897, 5947';

            await bro.yaClientLoginFast('yndx-ufo-test-734');
            await bro.url(NAVIGATION.disk.url);
            await bro.yaOpenListingElement('1.jpg');
            await checkPreviewDisplayed(bro);
            await bro.click(slider.sliderButtons.closeButton());
            await bro.yaOpenSection('albums');
            await waitForShimmersToHide(bro);
            await bro.yaWaitForVisible(albums.albums2.personal());
            await bro.yaWaitPreviewsLoaded(albums.albums2.item.preview());
            await bro.yaAssertView(isMobile ? 'diskclient-5898' : 'diskclient-5897',
                isMobile ? albums.albums2RootContent() : albums.albums2());
            await bro.click(albums.albums2.favorites());
            await bro.yaWaitPreviewsLoaded(albums.album2.item.preview());
            await bro.yaAssertView(isMobile ? 'diskclient-5948' : 'diskclient-5947', albums.album2());
        });

        hermione.only.in('chrome-phone', 'На десктопе мигает положение меню ... CHEMODAN-73618');
        it('diskclient-5951, 5950: Отображение попапа за тремя точками в избранном альбоме', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-5951' : 'diskclient-5950';
            await bro.yaClientLoginFast('yndx-ufo-test-734');
            await bro.url(`${NAVIGATION.albums.url}/1a70c43c362d770b1de02291`);

            await bro.yaWaitPreviewsLoaded(albums.album2.item.preview());

            await bro.click(albums.album2.actionsMoreButton());
            await bro.yaWaitForVisible(albums.album2ActionsDropdown());

            await bro.pause(500);
            await bro.yaAssertView(this.testpalmId, albums.album2ActionsDropdown());
        });

        it('diskclient-5953, 5952: Переход в слайдер ресурса в избранном альбоме', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-5953' : 'diskclient-5952';
            await bro.yaClientLoginFast('yndx-ufo-test-734');
            await bro.url('/client/disk?dialog=slider&idDialog=%2Fdisk%2F1.jpg');

            await checkPreviewDisplayed(bro);
            await bro.yaWaitForVisible(slider.sliderButtons.favoriteButtonOn());
        });

        describe('Отображение в слайдере', () => {
            it('diskclient-5978, 5977: Отображение признака избранности ресурса в слайдере', async function() {
                const bro = this.browser;
                const isMobile = await bro.yaIsMobile();
                this.testpalmId = isMobile ? 'diskclient-5978' : 'diskclient-5977';
                await bro.yaClientLoginFast('yndx-ufo-test-733');
                await bro.url(NAVIGATION.disk.url);

                await bro.yaOpenListingElement('01_picture.jpg');
                await checkPreviewDisplayed(bro);
                await bro.yaAssertView(`${this.testpalmId}-not-in-favorite`, slider.sliderButtons());
                await bro.yaChangeSliderActiveImage();
                await checkPreviewDisplayed(bro);
                await bro.yaAssertView(`${this.testpalmId}-in-favorite`, slider.sliderButtons());
            });
        });

        describe('Добавление и исключение ресурсов из избранного альбома', () => {
            beforeEach(async function() {
                const bro = this.browser;
                await bro.yaSkipWelcomePopup();
            });

            afterEach(cleanUpAlbums);

            hermione.auth.createAndLogin({
                language: 'ru',
                country: 'ru',
                tus_consumer: 'disk-front-client',
                tags: 'diskclient-favorites-show-hide'
            });
            hermione.skip.notIn('', 'Мигает – https://st.yandex-team.ru/CHEMODAN-74013');
            it('diskclient-5906, 5946, 5905, 5945: Добавление и исключение ресурсов из слайдера вне избранного альбома', async function() {
                const bro = this.browser;
                const isMobile = await bro.yaIsMobile();
                this.testpalmId = isMobile ? 'diskclient-5906, 5946' : 'diskclient-5905, 5945';

                //Проверяем, что избранного альбома нет
                await bro.url(NAVIGATION.albums.url);
                await bro.yaWaitForVisible(albums.albums.stub());

                //Идем в корень Диска и загружаем ресурс
                await bro.url(NAVIGATION.disk.url);
                const fileName = await bro.yaUploadFiles('test-image1.jpg', { uniq: true });
                addResourcesForCleanup.call(this, fileName);

                await bro.yaOpenListingElement(fileName);
                await bro.yaAssertView(`${this.testpalmId}-before-insert-in-favorite`,
                    slider.sliderButtons.favoriteButton());

                //Добавляем в Избранное
                await bro.click(slider.sliderButtons.favoriteButton());

                await bro.yaAssertView(`${this.testpalmId}-after-insert-in-favorite`,
                    slider.sliderButtons.favoriteButton());
                await bro.click(slider.sliderButtons.closeButton());

                //Проверяем, что альбом Избранное появился
                await bro.yaOpenSection('albums', true);
                await waitForShimmersToHide(bro);
                await bro.yaWaitForVisible(albums.albums2.favorites());

                //Возвращаемся в корень Диска
                await bro.yaOpenSection('disk');
                await bro.yaOpenListingElement(fileName);
                await bro.yaAssertView(`${this.testpalmId}-after-insert-in-favorite`,
                    slider.sliderButtons.favoriteButton());

                //Исключаем из избранного
                await bro.click(slider.sliderButtons.favoriteButton());

                await bro.yaAssertView(`${this.testpalmId}-after-remove-from-favorite`,
                    slider.sliderButtons.favoriteButton());
                await bro.click(slider.sliderButtons.closeButton());

                //Проверяем, что альбом Избранное скрылся
                await bro.yaOpenSection('albums', true);
                await waitForShimmersToHide(bro);
                await bro.yaWaitForHidden(albums.albums2.favorites());
                await bro.yaWaitForVisible(albums.albums.stub());
            });

            const addAndAssertFavorites = async function(file, expectedItems, tespalmId) {
                const bro = this.browser;
                await bro.url(NAVIGATION.disk.url);

                await bro.yaOpenListingElement(file);
                await bro.click(slider.sliderButtons.favoriteButton());
                await bro.yaWaitForVisible(slider.sliderButtons.favoriteButtonOn());

                // проверка количества и порядка файлов в альбоме Избранные
                await openFavorites.call(this);
                const currentItems = await getItemsInAlbum.call(this, albums.album2.item());
                await assert.deepEqual(
                    currentItems, expectedItems,
                    'Количество или порядок файлов в альбоме "Избранные" отличается от ожидаемого'
                );

                // проверка обложки альбома Избранные
                await bro.url(NAVIGATION.albums.url);
                await bro.yaWaitPreviewsLoaded(albums.albums2.item.preview());
                await bro.yaAssertView(tespalmId, albums.albums2.item());
            };

            hermione.auth.createAndLogin();
            it('diskclient-5903, diskclient-5904: Добавление файла в непустой альбом из слайдера', async function() {
                const bro = this.browser;
                await bro.yaSkipWelcomePopup();
                const isMobile = await bro.yaIsMobile();
                this.testpalmId = isMobile ? 'diskclient-5904' : 'diskclient-5903';

                const firstFile = await bro.yaUploadFiles('test-image1.jpg', { uniq: true });
                const secondFile = await bro.yaUploadFiles('test-image2.jpg', { uniq: true });

                await addAndAssertFavorites.call(this, firstFile, [firstFile], `${this.testpalmId}-first`);
                await addAndAssertFavorites
                    .call(this, secondFile, [secondFile, firstFile], `${this.testpalmId}-second`);
            });

            it('diskclient-5902, 5901: Отображение избранного альбома в диалоге добавления в альбом', async function() {
                const bro = this.browser;
                const isMobile = await bro.yaIsMobile();
                this.testpalmId = isMobile ? 'diskclient-5902' : 'diskclient-5901';
                await bro.yaClientLoginFast('yndx-ufo-test-738');
                await bro.url(NAVIGATION.disk.url);

                await bro.yaSelectResource('1.jpg');
                await bro.yaWaitActionBarDisplayed();
                await bro.click(popups.common.actionBar.addToAlbumButton());
                await bro.yaWaitForVisible(popups.common.selectAlbumDialog());

                await bro.yaWaitForVisible(albums.albums2.item.preview());

                await bro.yaAssertView(
                    this.testpalmId,
                    `${popups.common.selectAlbumDialog()} .dialog__body`
                );
            });

            hermione.auth.createAndLogin({ language: 'ru', tus_consumer: 'disk-front-client' });
            it('diskclient-5980, 5979: Исключение ресурса из избранного альбома (убрать из альбома)', async function() {
                const bro = this.browser;
                const isMobile = await bro.yaIsMobile();
                this.testpalmId = isMobile ? 'diskclient-5980' : 'diskclient-5979';
                const { testImageFiles } = await createAlbumFromTestImages.call(this, {
                    goToAlbum: false,
                    isFavorites: true
                });

                await openFavorites.call(this);

                const photoSelector = photoItemByName().replace(':title', testImageFiles[0]);
                await bro.yaSelectPhotoItem(photoSelector, true);

                await bro.yaCallActionInActionBar('excludeFromPersonalAlbum', false);

                await bro.yaWaitForHidden(photoSelector);
                await bro.yaWaitNotificationWithText(consts.TEXT_NOTIFICATION_FILE_EXCLUDED);
            });

            hermione.auth.createAndLogin({ language: 'ru', tus_consumer: 'disk-front-client' });
            it('diskclient-5944, 5943: Исключение всех ресурсов из избранного альбома (убрать из альбома)', async function() {
                const bro = this.browser;
                const isMobile = await bro.yaIsMobile();
                this.testpalmId = isMobile ? 'diskclient-5944' : 'diskclient-5943';

                await createAlbumFromTestImages.call(this, { goToAlbum: false, isFavorites: true });

                await openFavorites.call(this);

                const fileNames = await bro.yaGetPhotosNames();
                await bro.yaSelectPhotosRange(fileNames);

                await bro.yaCallActionInActionBar('excludeFromPersonalAlbum', false);
                await bro.yaAssertProgressBarAppeared();
                await bro.yaAssertProgressBarDisappeared();

                await bro.yaWaitForVisible(albums.album2.stub());
            });

            hermione.auth.createAndLogin({ language: 'ru', tus_consumer: 'disk-front-client' });
            it('diskclient-5982, 5981: Исключение ресурса из избранного альбома из раздела Последние', async function() {
                const bro = this.browser;
                const isMobile = await bro.yaIsMobile();
                this.testpalmId = isMobile ? 'diskclient-5982' : 'diskclient-5981';

                const { testImageFiles } = await createAlbumFromTestImages.call(this, {
                    goToAlbum: false,
                    isFavorites: true
                });

                await openFavorites.call(this);

                const photoSelector = photoItemByName().replace(':title', testImageFiles[0]);
                await bro.yaWaitForVisible(photoSelector);

                await bro.yaOpenSection('recent');
                await bro.yaOpenListingElement(testImageFiles[0]);

                await bro.yaWaitForVisible(slider.sliderButtons.favoriteButtonOn());
                await bro.click(slider.sliderButtons.favoriteButtonOn());
                await bro.yaWaitForVisible(slider.sliderButtons.favoriteButtonOff());

                await bro.click(slider.sliderButtons.closeButton());
                await bro.yaWaitForHidden(slider.contentSlider());

                await openFavorites.call(this, true);
                await bro.yaWaitForHidden(photoSelector);
            });

            hermione.auth.createAndLogin({ language: 'ru', tus_consumer: 'disk-front-client' });
            it('diskclient-5940, 5942, 5939, 5941: Исключение ресурса из слайдера избранного альбома, листание и закрытие слайдера при снятии признака избранности', async function() {
                const bro = this.browser;
                const isMobile = await bro.yaIsMobile();
                this.testpalmId = isMobile ? 'diskclient-5940, 5942' : 'diskclient-5939, 5941';

                await createAlbumFromTestImages.call(this, {
                    goToAlbum: false,
                    isFavorites: true
                });
                await openFavorites.call(this);

                const fileNames = await bro.yaGetPhotosNames();

                const photoSelectors = fileNames.map((fileName) => photoItemByName().replace(':title', fileName));
                await bro.click(photoSelectors[1]);

                await bro.yaWaitForVisible(slider.sliderButtons.favoriteButtonOn());
                // гонка, нужный класс ненадолго появится после клика
                bro.yaWaitForVisible(slider.sliderButtons.favoriteButtonOff(), 5000).then();
                await bro.click(slider.sliderButtons.favoriteButtonOn());
                await bro.yaWaitForVisible(slider.sliderButtons.favoriteButtonOn());

                assert.equal(await bro.yaGetActiveSliderImageName(), fileNames[2]);

                await bro.yaWaitForVisible(slider.sliderButtons.favoriteButtonOn());
                await bro.click(slider.sliderButtons.favoriteButtonOn());

                await bro.yaWaitForHidden(slider.contentSlider());

                await bro.yaWaitForHidden(photoSelectors[1]);
                await bro.yaWaitForHidden(photoSelectors[2]);
                await bro.yaWaitForVisible(photoSelectors[0]);
            });

            /**
             * @param {string} user
             * @param {string} albumId
             * @param {Function} addFunction
             */
            async function testAddToAlbum(user, albumId, addFunction) {
                const bro = this.browser;
                await bro.yaClientLoginFast(user);
                await bro.url(`/client/albums/${albumId}`);

                await bro.yaWaitAlbumLoaded();

                const fileNamesInAlbums = await bro.yaGetPhotosNames();

                await addFunction();

                await bro.yaWaitPhotoSliceItemsInViewportLoad();

                const fileNamesInPhotoslice = await bro.yaGetPhotosNames();
                const fileName = _.shuffle(_.difference(fileNamesInPhotoslice, fileNamesInAlbums))[0];
                this.currentTest.ctx.excludePhotosFromFavorites = [`/disk/photos/${fileName}`];

                await bro.yaSelectPhotosAndCreateAlbum([fileName], true);
                await bro.yaWaitNotificationForResource(
                    'Избранные',
                    consts.TEXT_NOTIFICATION_FILE_ADDED_TO_ALBUM,
                    { close: false }
                );

                await bro.yaWaitForVisible(albums.album2.title());
                assert.strictEqual(await bro.getText(albums.album2.title()), 'Избранные');

                await bro.yaAssertPhotosInAlbum([fileName]);
            }

            it('diskclient-5912, 5911: Добавление файла в избранном альбоме по кнопке-иконке', async function() {
                await testAddToAlbum.call(this, 'yndx-ufo-test-743', '7a5089809f947fe7056498b7', async() => {
                    await this.browser.click(albums.album2.addToFavoritesAlbumButton());
                });
            });

            it('diskclient-5914, 5913: Добавление файла в избранном альбоме по кнопке за тремя точками', async function() {
                await testAddToAlbum.call(this, 'yndx-ufo-test-744', 'f37ea4887d82d4253bc33031', async() => {
                    await this.browser.yaCallActionInAlbumActionsDropdown('addItems');
                });
            });

            /**
             * @param {string[]} testImageFiles
             * @returns {Promise<void>}
             */
            async function testAddResourceToFavoritesFromDialog(testImageFiles) {
                const bro = this.browser;
                await bro.yaSelectResources(testImageFiles);
                await bro.yaCallActionInActionBar('addToAlbum', false);
                await bro.yaWaitForVisible(popups.common.selectAlbumDialog.favoritesAlbum());
                await bro.click(popups.common.selectAlbumDialog.favoritesAlbum());

                await bro.yaWaitForHidden(popups.common.selectAlbumDialog.createAlbum());

                await bro.yaWaitNotificationForResource(
                    'Избранные',
                    testImageFiles.length === 1 ?
                        consts.TEXT_NOTIFICATION_FILE_ADDED_TO_ALBUM :
                        consts.TEXT_NOTIFICATION_FILES_ADDED_TO_ALBUM.replace(':count', testImageFiles.length),
                    { close: false }
                );

                await bro.yaOpenSection('albums', true);

                await waitForShimmersToHide(bro);
                await bro.yaWaitForVisible(albums.albums2.favorites());
                await bro.click(albums.albums2.favorites());
                await bro.yaWaitAlbumItemsInViewportLoad();

                await bro.yaAssertPhotosInAlbum(testImageFiles);
            }

            hermione.auth.createAndLogin({ language: 'ru', tus_consumer: 'disk-front-client' });
            hermione.only.notIn('firefox-desktop', 'https://st.yandex-team.ru/CHEMODAN-73958');
            it('diskclient-5910, 5909: Добавление файла в пустой альбом из диалога добавления в альбом', async function() {
                const bro = this.browser;

                await bro.url(NAVIGATION.disk.url);
                const testImageFiles = await bro.yaUploadFiles(['test-image1.jpg'], { uniq: true });

                await testAddResourceToFavoritesFromDialog.call(this, testImageFiles);
            });

            hermione.auth.createAndLogin({ language: 'ru', tus_consumer: 'disk-front-client' });
            hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-73958');
            it('diskclient-5908, 5907: Добавление файла в непустой альбом из диалога добавления в альбом', async function() {
                const bro = this.browser;

                await createAlbumFromTestImages.call(this, { goToAlbum: false, isFavorites: true });

                await bro.url(NAVIGATION.disk.url);
                const testImageFiles = await bro.yaUploadFiles(['test-file.jpg'], { uniq: true });

                await testAddResourceToFavoritesFromDialog.call(this, testImageFiles);
            });

            hermione.auth.createAndLogin({ language: 'ru', tus_consumer: 'disk-front-client' });
            hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-73958');
            it('diskclient-5984, 5983: Добавление нескольких файлов в альбом из диалога добавления в альбом', async function() {
                const bro = this.browser;

                await bro.url(NAVIGATION.disk.url);
                const testImageFiles = await bro.yaUploadFiles(
                    ['test-image1.jpg', 'test-image2.jpg', 'test-image3.jpg'],
                    { uniq: true }
                );

                await testAddResourceToFavoritesFromDialog.call(this, testImageFiles);
            });
        });
    });
});
