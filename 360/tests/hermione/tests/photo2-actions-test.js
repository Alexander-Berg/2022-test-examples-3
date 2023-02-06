const { NAVIGATION } = require('../config').consts;
const { photo } = require('../page-objects/client-photo2-page').common;
const { photoItemByName } = require('../page-objects/client');
const consts = require('../config').consts;
const popups = require('../page-objects/client-popups');
const listing = require('../page-objects/client-content-listing').common;
const PageObjects = require('../page-objects/public');
const { assert } = require('chai');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 * @param {string} photoSelector
 * @returns {Promise<string>}
 */

const selectPhoto = async function(photoSelector) {
    await this.yaWaitForVisible(photoSelector);
    await this.yaScrollIntoView(photoSelector);
    await this.yaSelectPhotoItem(photoSelector, true, true);
    return await this.yaGetPhotosliceItemName(photoSelector);
};

describe('Действия в фотосрезе', () => {
    describe('Копирование файлов', () => {
        afterEach(async function() {
            const listingResources = this.currentTest.ctx.listingResources || [];
            if (listingResources.length) {
                await this.browser.yaOpenSection('disk');
                await this.browser.yaDeleteCompletely(listingResources, { safe: true, fast: true });
            }
        });

        /**
         * @param {string} testpalmId
         * @param {string} user
         * @param {Object} opts
         * @param {boolean} [opts.isWowGrid=false]
         * @param {string} [opts.url='/client/photo']
         * @param {boolean} [opts.isWowGrid=false]
         * @returns {Promise<void>}
         */
        async function testCopy(testpalmId, user, opts) {
            // при прогоне большого кол-ва параллельных тестов времени может не хватать
            this.browser.executionContext.timeout(100000);
            const options = Object.assign({}, { isWowGrid: false, url: NAVIGATION.photo.url, isGroup: false }, opts);
            const folderName = `tmp-${Date.now()}-copy-target`;
            this.currentTest.ctx.listingResources = [folderName];

            const bro = this.browser;
            this.testpalmId = testpalmId;

            const selectRandomPhoto = async() => {
                const photoSelector = await bro.yaGetPhotosliceRandomPhoto();
                await bro.yaWaitForVisible(photoSelector);
                await bro.yaScrollIntoView(photoSelector);

                const photoName = await bro.yaGetPhotosliceItemName(photoSelector);
                await bro.yaSelectPhotoItem(photoSelector, true, true);

                return photoName;
            };

            await bro.yaClientLoginFast(user);
            await bro.yaOpenSection('disk');

            await bro.yaCreateFolder(folderName);
            await bro.yaAssertListingHas(folderName);

            await bro.url(options.url);

            await bro.yaWaitForVisible(options.isWowGrid ? photo.wow() : photo.tile());
            await bro.yaWaitForVisible(photo.item(), 10000);

            let secondPhotoName;
            const firstPhotoName = await selectRandomPhoto();
            if (options.isGroup) {
                secondPhotoName = await selectRandomPhoto();
            }

            await bro.yaCopySelected(folderName);

            if (options.isGroup) {
                await bro.yaWaitNotificationForResource(
                    folderName,
                    consts.TEXT_NOTIFICATION_OBJECTS_COPIED_TO_FOLDER(2)
                );
            } else {
                await bro.yaWaitNotificationForResource(
                    { name: firstPhotoName, folder: folderName },
                    consts.TEXT_NOTIFICATION_FILE_COPIED_TO_FOLDER
                );
            }
            await bro.url('/client/disk/' + folderName);

            await bro.yaWaitForHidden(listing.listingSpinner());
            await bro.yaAssertListingHas(firstPhotoName);
            if (options.isGroup) {
                await bro.yaAssertListingHas(secondPhotoName);
            }
        }

        it('diskclient-4315, 4264: Все фото. Копирование', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testCopy.call(this,
                isMobile ? 'diskfront-4315' : 'diskfront-4264',
                isMobile ? 'yndx-ufo-test-90' : 'yndx-ufo-test-147'
            );
        });

        it('diskclient-4619, 4620: [Вау-сетка] Копирование файла в вау-сетке через топбар', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testCopy.call(this,
                isMobile ? 'diskfront-4620' : 'diskfront-4619',
                isMobile ? 'yndx-ufo-test-195' : 'yndx-ufo-test-196',
                { isWowGrid: true }
            );
        });

        it('diskclient-4439, 4322: Все фото. Копирование безлимитного файла', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testCopy.call(this,
                isMobile ? 'diskclient-4322' : 'diskclient-4439',
                isMobile ? 'yndx-ufo-test-129' : 'yndx-ufo-test-130'
            );
        });

        it('diskclient-5280, diskclient-5574: Копирование одного файла в автоальбоме через топбар', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testCopy.call(this,
                isMobile ? 'diskclient-5574' : 'diskclient-5280',
                isMobile ? 'yndx-ufo-test-208' : 'yndx-ufo-test-209',
                { isWowGrid: true, url: NAVIGATION.photo.url + '?filter=unbeautiful' }
            );
        });

        it('diskclient-4291, 4321: Все фото. Копирование группы', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testCopy.call(this,
                isMobile ? 'diskclient-4321' : 'diskclient-4291',
                'yndx-ufo-test-408',
                { isGroup: true }
            );
        });

        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-4475'] });
        it('diskclient-4475: Все фото. Копирование группы по пушу', async function() {
            const bro = this.browser;

            const photoTitles = ['1970_1.jpg', '1970_2.jpg', '1970_3.jpg', '1970_4.jpg', '1970_5.jpg'];
            const photoSelectors = photoTitles.map((title) => photo.itemByName().replace(':title', title));

            const testFolderName = `tmp-${Date.now()}`;
            await bro.yaCreateFolder(testFolderName);
            this.currentTest.ctx.listingResources = [testFolderName];

            await bro.yaOpenSection('photo');
            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.newWindow(NAVIGATION.folder(testFolderName).url);

            const handles = await bro.getWindowHandles();

            await bro.switchToWindow(handles[0]);

            for (const selector of photoSelectors) {
                await selectPhoto.call(bro, selector);
            }

            await bro.yaCopySelected(testFolderName);
            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await bro.switchToWindow(handles[1]);

            for (const titles of photoTitles) {
                await bro.yaAssertListingHas(titles);
            }
        });
    });

    describe('Поделение файлами', () => {
        /**
         * @param {string} testpalmId
         * @param {string} fileName
         * @param {boolean} [isWowGrid]
         * @param {string} [url]
         * @returns {Promise<void>}
         */
        async function testShare(testpalmId, fileName, isWowGrid = false, url = NAVIGATION.photo.url) {
            const bro = this.browser;
            this.testpalmId = testpalmId;

            await bro.url(url);

            await bro.yaWaitForVisible(isWowGrid ? photo.wow() : photo.tile());

            await bro.yaSelectPhotoItemByName(fileName, true);
            const publicUrl = await bro.yaShareSelected();

            await bro.yaGetPublicUrlAndCloseTab();

            await bro.yaDeletePublicLinkInShareDialog();

            await bro.newWindow(publicUrl);
            await bro.yaWaitForVisible(PageObjects.error());
        }

        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-4604-4605'] });
        it('diskclient-4605, 4604: [Вау-сетка] Поделение файлом в вау-сетке через топбар', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testShare.call(this,
                isMobile ? 'diskclient-4605' : 'diskclient-4604',
                'IMG_20190829_153602.jpg',
                true
            );
        });

        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-5290-5571'] });
        it('diskclient-5290, diskclient-5571: Поделение файлом в автоальбоме через топбар', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testShare.call(this,
                isMobile ? 'diskclient-5571' : 'diskclient-5290',
                '18-46.jpg',
                true,
                NAVIGATION.photo.url + '?filter=beautiful'
            );
        });
    });

    describe('Удаление файлов', () => {
        afterEach(async function() {
            await this.browser.yaRestoreAllFromTrash();
        });
        /**
         * @param {string} testpalmId
         * @param {string} user
         * @param {boolean} [isWowGrid]
         * @param {string} [url]
         * @returns {Promise<void>}
         */
        async function testDelete(testpalmId, user, isWowGrid = false, url = NAVIGATION.photo.url) {
            this.testpalmId = testpalmId;
            const bro = this.browser;
            await bro.yaClientLoginFast(user);
            await bro.url(url);

            await bro.yaWaitForVisible(isWowGrid ? photo.wow() : photo.tile());
            await bro.yaWaitForVisible(photo.item(), 10000);

            const fileToDelete = await bro.yaGetPhotosliceRandomPhoto();
            await bro.yaWaitForVisible(fileToDelete);
            const fileName = await bro.yaGetPhotosliceItemName(fileToDelete);

            await bro.yaSelectPhotoItem(fileToDelete, true);

            await bro.yaDeleteSelected();
            await bro.yaWaitForHidden(photo.itemByName().replace(':title', fileName));
            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();
            await bro.yaWaitNotificationForResource(fileName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);

            await bro.yaWaitActionBarHidden();

            // проверим что фотосрез не развалился (есть какие-то видимые фотки)
            await bro.yaWaitForVisible(photo.item());

            await bro.refresh();

            await bro.yaWaitForVisible(photo.item(), 10000);
            await bro.yaWaitForHidden(photo.itemByName().replace(':title', fileName));
        }

        it('diskclient-4359, 4262: Все фото. Удаление', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testDelete.call(this,
                isMobile ? 'diskfront-4359' : 'diskfront-4262',
                isMobile ? 'yndx-ufo-test-45' : 'yndx-ufo-test-46'
            );
        });

        it('diskclient-5191, 4358: Все фото. Удаление группы', async function() {
            const bro = this.browser;
            const isMobile = await this.browser.yaIsMobile();

            await bro.yaClientLoginFast(isMobile ? 'yndx-ufo-test-510' : 'yndx-ufo-test-509');

            await bro.yaOpenSection('photo');
            await bro.yaWaitPhotoSliceItemsInViewportLoad();

            const firstPhotoSelector = await bro.yaGetPhotosliceRandomPhoto();
            await selectPhoto.call(bro, firstPhotoSelector);

            let secondPhotoSelector;
            do {
                secondPhotoSelector = await bro.yaGetPhotosliceRandomPhoto();
            } while (firstPhotoSelector === secondPhotoSelector);
            await selectPhoto.call(bro, secondPhotoSelector);

            await bro.yaDeleteSelected();
            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();
        });

        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-4466'] });
        it('diskclient-4466: Все фото. Удаление группы по пушу', async function() {
            const bro = this.browser;

            const photoTitles = ['1970_1.jpg', '1970_2.jpg', '1970_3.jpg', '1970_4.jpg', '1970_5.jpg'];
            const photoSelectors = photoTitles.map((title) => photo.itemByName().replace(':title', title));

            await bro.yaOpenSection('photo');
            await bro.yaWaitPhotoSliceItemsInViewportLoad();
            await bro.newWindow(await bro.getUrl());

            const handles = await bro.getWindowHandles();

            await bro.switchToWindow(handles[0]);

            for (const selector of photoSelectors) {
                await selectPhoto.call(bro, selector);
            }

            await bro.yaDeleteSelected();
            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await bro.switchToWindow(handles[1]);

            for (const selector of photoSelectors) {
                await bro.yaWaitForHidden(selector);
            }
        });

        it('diskclient-4623, 4624: [Вау-сетка] Удаление файла в вау-сетке через топбар', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testDelete.call(this,
                isMobile ? 'diskfront-4624' : 'diskfront-4623',
                isMobile ? 'yndx-ufo-test-193' : 'yndx-ufo-test-194',
                true
            );
        });

        it('diskclient-4360, 4284: Все фото. Удаление безлимитных файлов', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testDelete.call(this,
                isMobile ? 'diskclient-4360' : 'diskfront-4284',
                isMobile ? 'yndx-ufo-test-131' : 'yndx-ufo-test-132'
            );
        });

        it('diskclient-5546, 5287: Удаление файла в альбоме-срезе через топбар', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testDelete.call(this,
                isMobile ? 'diskclient-5546' : 'diskclient-5287',
                isMobile ? 'yndx-ufo-test-206' : 'yndx-ufo-test-207',
                true,
                NAVIGATION.photo.url + '?filter=beautiful'
            );
        });
    });

    describe('Перемещение файлов', () => {
        afterEach(async function() {
            const testFolderName = this.currentTest.ctx.testFolderName;
            if (testFolderName) {
                const bro = this.browser;

                await bro.url(consts.NAVIGATION.folder(testFolderName).url);
                await bro.yaMoveBackToPhoto();

                await bro.url(consts.NAVIGATION.disk.url);
                await bro.yaDeleteCompletely(testFolderName, { safe: true, fast: true });
            }
        });

        /**
         * @param {string} testpalmId
         * @param {string} user
         * @param {boolean} [isWowGrid]
         * @param {string} [url]
         * @returns {Promise<void>}
         */
        async function testMove(testpalmId, user, isWowGrid = false, url = NAVIGATION.photo.url) {
            const bro = this.browser;
            const folderName = `tmp-${Date.now()}`;
            this.currentTest.ctx.testFolderName = folderName;
            this.testpalmId = testpalmId;
            this.currentTest.ctx.listingResources = [folderName];

            await bro.yaClientLoginFast(user);

            await bro.yaCreateFolder(folderName);
            await bro.yaAssertListingHas(folderName);

            await bro.url(url);

            await bro.yaWaitForVisible(isWowGrid ? photo.wow() : photo.tile());

            await bro.waitUntil(async () => {
                const item = await bro.$(photo.item());
                return (await item.isDisplayed());
            });

            const fileToMove = await bro.yaGetPhotosliceRandomPhoto();
            await bro.yaWaitForVisible(fileToMove);
            await bro.yaScrollIntoView(fileToMove);

            const fileName = await bro.yaGetPhotosliceItemName(fileToMove);
            await bro.yaSelectPhotoItem(fileToMove, true);

            await bro.yaMoveSelected(folderName);

            await bro.yaWaitNotificationForResource(
                { name: fileName, folder: folderName },
                consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
            );

            const newFileName = await bro.yaGetPhotosliceItemName(fileToMove);
            assert(fileName === newFileName, 'Имя первого фото поменялось');

            await bro.url('/client/disk/' + folderName);
            await bro.yaWaitForHidden(listing.listingSpinner());
            await bro.yaAssertListingHas(fileName);
        }

        it('diskclient-4311, 4263: Все фото. Перемещение', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testMove.call(this,
                isMobile ? 'diskclient-4311' : 'diskfront-4263',
                isMobile ? 'yndx-ufo-test-92' : 'yndx-ufo-test-93'
            );
        });

        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-68670');
        it('diskclient-4616, 4617: [Вау-сетка] Перемещение файла в вау-сетке через топбар', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testMove.call(this,
                isMobile ? 'diskclient-4617' : 'diskfront-4616',
                isMobile ? 'yndx-ufo-test-197' : 'yndx-ufo-test-198',
                true
            );
        });

        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-68670');
        it('diskclient-5282, diskclient-5568: Перемещение файла в автоальбоме через топбар', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testMove.call(this,
                isMobile ? 'diskclient-5568' : 'diskfront-5282',
                isMobile ? 'yndx-ufo-test-210' : 'yndx-ufo-test-211',
                true,
                NAVIGATION.photo.url + '?filter=beautiful'
            );
        });

        it('diskclient-4320, diskclient-5071: Раздел фото. Перемещение группы', async function() {
            const bro = this.browser;
            const folderNameForMoving = `tmp-${Date.now()}-move-folder`;
            this.currentTest.ctx.testFolderName = folderNameForMoving;

            await bro.yaClientLoginFast('yndx-ufo-test-219');

            await bro.yaCreateFolder(folderNameForMoving);
            await bro.yaWaitActionBarDisplayed();
            await bro.yaCloseActionBar();

            await bro.yaOpenSection('photo');
            await bro.yaWaitPhotoSliceItemsInViewportLoad();

            const firstPhotoSelector = await bro.yaGetPhotosliceRandomPhoto();
            const firstPhotoName = await selectPhoto.call(bro, firstPhotoSelector);

            let secondPhotoSelector;
            do {
                secondPhotoSelector = await bro.yaGetPhotosliceRandomPhoto();
            } while (firstPhotoSelector === secondPhotoSelector);
            const secondPhotoName = await selectPhoto.call(bro, secondPhotoSelector);

            await bro.yaMoveSelected(folderNameForMoving);
            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await bro.url('/client/disk/' + folderNameForMoving);
            await bro.yaWaitForHidden(listing.listingSpinner());
            await bro.yaAssertListingHas(firstPhotoName);
            await bro.yaAssertListingHas(secondPhotoName);
        });
    });

    describe('Переименование файлов', () => {
        /**
         * @param {string} testpalmId
         * @param {string} user
         * @param {boolean} [isWowGrid]
         * @param {string} [url]
         * @returns {Promise<void>}
         */
        async function testRename(testpalmId, user, isWowGrid = false, url = NAVIGATION.photo.url) {
            const bro = this.browser;
            this.testpalmId = testpalmId;

            await bro.yaClientLoginFast(user);

            await bro.url(url);

            await bro.yaWaitForVisible(isWowGrid ? photo.wow() : photo.tile());
            await bro.yaWaitPhotoSliceItemsInViewportLoad();

            const fileToRename = await bro.yaGetPhotosliceRandomPhoto();

            await bro.yaWaitForVisible(fileToRename);

            const originalFileName = await bro.yaGetPhotosliceItemName(fileToRename);
            const newFileName = originalFileName.replace(/^([^.]+)(.*)$/, `$1_${Date.now()}$2`);

            await bro.yaSelectPhotoItem(fileToRename, true);

            await bro.yaCallActionInActionBar('rename');
            await bro.yaSetResourceNameAndApply(newFileName);
            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await bro.yaSelectPhotoItem(fileToRename, true);
            const fileNameAfterRename = await bro.yaGetResourceNameFromInfoDropdown(
                popups.common.actionBar.infoButton()
            );

            assert(fileNameAfterRename === newFileName, 'Имя в инфо не поменялось после переименования');
            await bro.yaCallActionInActionBar('rename');
            await bro.yaSetResourceNameAndApply(originalFileName);
            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();
        }

        it('diskclient-4621, 4627: Переименование обычного фото в фотосрезе', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testRename.call(this,
                isMobile ? 'diskclient-4627' : 'diskfront-4621',
                isMobile ? 'yndx-ufo-test-94' : 'yndx-ufo-test-95'
            );
        });

        it('diskclient-4614, 4615: [Вау-сетка] Переименование файла в вау-сетке через топбар', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testRename.call(this,
                isMobile ? 'diskclient-4615' : 'diskfront-4614',
                isMobile ? 'yndx-ufo-test-199' : 'yndx-ufo-test-200',
                true
            );
        });

        it('diskclient-5286, diskclient-6097: Переименование фото в автоальбоме', async function() {
            const isMobile = await this.browser.yaIsMobile();
            await testRename.call(this,
                isMobile ? 'diskclient-6097' : 'diskfront-5286',
                isMobile ? 'yndx-ufo-test-212' : 'yndx-ufo-test-213',
                true,
                NAVIGATION.photo.url + '?filter=unbeautiful'
            );
        });

        it('diskclient-4545, 5199: Переименование безлимитного файла', async function() {
            await testRename.call(this,
                await this.browser.yaIsMobile ? 'diskclient-5199' : 'diskfront-4545',
                'yndx-ufo-test-223',
                true
            );
        });
    });
    it('diskclient-4608, diskclient-4607: Скачивание файла в вау-сетке через топбар', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4608' : 'diskclient-4607';

        await bro.yaClientLoginFast('yndx-ufo-test-513');

        await bro.yaOpenSection('photo');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        const firstPhotoSelector = photoItemByName().replace(':title', '7-9.jpg');
        await bro.yaWaitForVisible(firstPhotoSelector);
        await bro.yaSelectPhotoItem(firstPhotoSelector, true);

        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            await bro.click(isMobile ?
                popups.touch.actionBar.downloadButton() : popups.desktop.actionBar.downloadButton());
        });

        assert.match(
            url,
            /downloader\.disk\.yandex\.ru\/disk\/.+&filename=7-9\.jpg/,
            'Некорректный url для скачивания'
        );
    });

    it('diskclient-4612, diskclient-4609: Скачивание группы файлов в вау-сетке через топбар', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4612' : 'diskclient-4609';

        await bro.yaClientLoginFast('yndx-ufo-test-513');

        await bro.yaOpenSection('photo');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        const firstPhotoSelector = await bro.yaGetPhotosliceRandomPhoto();
        await selectPhoto.call(bro, firstPhotoSelector);

        let secondPhotoSelector;
        do {
            secondPhotoSelector = await bro.yaGetPhotosliceRandomPhoto();
        } while (firstPhotoSelector === secondPhotoSelector);
        await selectPhoto.call(bro, secondPhotoSelector);

        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            await bro.click(isMobile ?
                popups.touch.actionBar.downloadButton() : popups.desktop.actionBar.downloadButton());
        });

        assert.match(
            url,
            /\/downloader\.disk\.yandex\.ru\/zip-files\/.+&filename=archive-(.*)\.zip.*/,
            'Некорректный url для скачивания'
        );
    });
});

describe('Действия в фотосрезе -> ', () => {
    afterEach(async function() {
        await this.browser.yaRestoreAllFromTrash();
    });

    hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-4358', 'diskclient-5191'] });
    it('diskclient-4358, 5191: Все фото. Удаление группы.', async function() {
        const bro = this.browser;
        const firstPhotoName = 'IMG_2372.JPG';
        const secondPhotoName = 'IMG_2373.JPG';

        await bro.yaOpenSection('photo');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaSelectPhotosliceItemByName(firstPhotoName, true);
        await bro.yaSelectPhotosliceItemByName(secondPhotoName, true, true);
        await bro.yaDeleteSelected();

        await bro.yaAssertProgressBarAppeared();
        await bro.yaAssertProgressBarDisappeared();

        await bro.yaWaitForHidden(firstPhotoName);
        await bro.yaWaitForHidden(secondPhotoName);
    });
});
