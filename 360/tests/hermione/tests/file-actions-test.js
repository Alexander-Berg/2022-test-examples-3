const { basename } = require('path');
const clientPopups = require('../page-objects/client-popups');
const clientContentListing = require('../page-objects/client-content-listing');
const clientNavigation = require('../page-objects/client-navigation');
const popups = require('../page-objects/client-popups');

const consts = require('../config').consts;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { assert } = require('chai');

const getTmpFolderName = (prefix = 'work-folder') => `tmp-${prefix}-${Date.now()}`;

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 * @typedef { import('@ps-int/ufo-hermione/types').Resource } Resource
 */

describe('Действия с файлом -> ', () => {
    /**
     * @param {string} touchUser
     * @param {string} desktopUser
     * @param {Browser} bro
     * @returns {Promise<string|string[]>} filenames
     */
    async function loginAndUploadFiles(touchUser, desktopUser, bro) {
        const user = await bro.yaIsMobile() ? touchUser : desktopUser;

        await bro.yaClientLoginFast(user);

        return bro.yaUploadFiles('test-file.txt', { uniq: true });
    }

    /**
     * @param {string} originalName
     * @param {number} length
     * @returns {*}
     */
    const getLongFileName = (originalName, length = 255) => {
        const prefixSample = 'very';
        return originalName.padStart(length, prefixSample);
    };

    /**
     * Функция, копирующая файл в ту же папку. Переиспользуется в других тестах, поэтому
     * в контекст выносятся следующие переменные:
     * 1. this.currentTest.ctx.newTestFileName - новое имя файла
     * 2. this.currentTest.ctx.items - список временных файлов для удаления
     *
     * @param {string} user
     * @param {string} resourceName
     * @param {boolean} isFolder
     * @returns {Promise<void>}
     */
    async function copyResourceToTheSameFolder(user, resourceName, isFolder) {
        const bro = this.browser;

        this.currentTest.ctx.newTestFileName = `tmp-${Date.now()}`;
        this.currentTest.ctx.items = this.currentTest.ctx.newTestFileName;

        await bro.yaClientLoginFast(user);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await bro.yaSelectResource(resourceName);
        await bro.yaCopySelected();

        await bro.yaWaitForVisible(clientPopups.common.confirmationPopup());
        await bro.yaSetValue(clientPopups.common.confirmationPopup.nameInput(), this.currentTest.ctx.newTestFileName);
        await bro.click(clientPopups.common.confirmationPopup.acceptButton());

        await bro.yaAssertProgressBarAppeared();
        await bro.yaWaitNotificationForResource(
            this.currentTest.ctx.newTestFileName,
            isFolder ?
                consts.TEXT_NOTIFICATION_FOLDER_COPIED :
                consts.TEXT_NOTIFICATION_FILE_COPIED,
            { timeout: consts.FILE_OPERATIONS_TIMEOUT }
        );
        await bro.yaAssertListingHas(this.currentTest.ctx.newTestFileName);
    }

    describe('Смоуки', () => {
        afterEach(async function() {
            const listingResources = this.currentTest.ctx.listingResources || [];

            if (listingResources.length) {
                const bro = this.browser;

                await bro.url(consts.NAVIGATION.disk.url);
                await bro.yaWaitForVisible(clientContentListing.common.listing());

                await bro.yaDeleteCompletely(listingResources, { safe: true, fast: true });
            }
        });

        it('diskclient-1409, 1603: Смоук: переименование файла по клику на "Сохранить"', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-1409' : 'diskclient-1603';

            const testFileName = await loginAndUploadFiles('yndx-ufo-test-52', 'yndx-ufo-test-02', bro);
            const newFileName = 'rename-' + testFileName;
            this.currentTest.ctx.listingResources = [testFileName, newFileName];

            await bro.yaSelectResource(testFileName);
            await bro.yaRenameSelected(newFileName);

            await bro.yaAssertListingHas(newFileName);
            await bro.yaAssertListingHasNot(testFileName);
        });

        it('diskclient-1410, 1483: Смоук: копирование файла', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-1410' : 'diskclient-1483';

            const testFolderName = `tmp-${Date.now()}-test-folder`;
            const testFileName = await loginAndUploadFiles('yndx-ufo-test-53', 'yndx-ufo-test-03', bro);
            this.currentTest.ctx.listingResources = [testFileName, testFolderName];

            await bro.yaCreateFolder(testFolderName);

            await bro.yaSelectResource(testFileName);
            await bro.yaCopySelected(testFolderName);

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                {
                    name: testFileName,
                    folder: testFolderName
                },
                consts.TEXT_NOTIFICATION_FILE_COPIED_TO_FOLDER
            );
            await bro.yaAssertProgressBarDisappeared();

            await bro.yaAssertListingHas(testFileName);
            await bro.yaGoToFolderAndWaitForListingSpinnerHide(testFolderName);
            await bro.yaAssertListingHas(testFileName);
        });

        it('diskclient-1606, 1486: Смоук: удаление файла из корзины', async function() {
            const bro = this.browser;
            this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1606' : 'diskclient-1486';
            const testFileName = await loginAndUploadFiles('yndx-ufo-test-54', 'yndx-ufo-test-04', bro);
            this.currentTest.ctx.listingResources = [testFileName];
            await bro.yaDeleteCompletely(testFileName);
            this.currentTest.ctx.listingResources = [];
        });

        it('diskclient-1408, 1484: Смоук: удаление файла', async function() {
            const bro = this.browser;
            this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1408' : 'diskclient-1484';

            const testFileName = await loginAndUploadFiles('yndx-ufo-test-18', 'yndx-ufo-test-68', bro);
            this.currentTest.ctx.listingResources = [testFileName];

            await bro.yaScrollToEnd();
            await bro.yaSelectResource(testFileName);
            await bro.yaDeleteSelected();
            await bro.yaAssertListingHasNot(testFileName);
            await bro.yaWaitNotificationForResource(testFileName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);

            await bro.yaWaitActionBarHidden();
            await bro.yaAssertListingHasNot(testFileName);
            await bro.refresh();
            await bro.yaWaitForVisible(clientContentListing.common.listing());
            await bro.yaAssertListingHasNot(testFileName);
        });

        it('diskclient-1605, 1485: Смоук: восстановление файла', async function() {
            const bro = this.browser;
            this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1605' : 'diskclient-1485';
            const env = await bro.yaIsMobile() ? 'touch' : 'desktop';

            const testFileName = await loginAndUploadFiles(
                'yndx-ufo-test-55',
                'yndx-ufo-test-05',
                bro);
            this.currentTest.ctx.listingResources = [testFileName];

            await bro.yaDeleteResource(testFileName);

            await bro.yaOpenSection('trash');
            await bro.yaSelectResource(testFileName);
            await bro.yaWaitActionBarDisplayed();
            await bro.click(clientPopups[env].actionBar.restoreFromTrashButton());
            await bro.yaWaitNotificationForResource(testFileName, consts.TEXT_NOTIFICATION_FILE_RESTORE);

            await bro.url(consts.NAVIGATION.disk.url);
            await bro.yaAssertListingHas(testFileName);
        });

        it('diskclient-1411, 1482: Смоук: перемещение файла', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-1411' : 'diskclient-1482';

            const testFileName = await loginAndUploadFiles(
                'yndx-ufo-test-56',
                'yndx-ufo-test-06',
                bro);
            const testFolderName = `tmp-${Date.now()}`;

            this.currentTest.ctx.listingResources = [testFileName, testFolderName];
            await bro.yaCreateFolder(testFolderName);

            await bro.yaSelectResource(testFileName);
            await bro.yaMoveSelected(testFolderName);
            await bro.yaWaitNotificationForResource(
                { name: testFileName, folder: testFolderName },
                consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
            );

            await bro.yaAssertListingHasNot(testFileName);
            await bro.yaOpenListingElement(testFolderName);
            await bro.yaAssertListingHas(testFileName);
            await bro.yaOpenSection('disk');
        });
    });

    describe('Перемещение файлов', () => {
        afterEach(async function() {
            const items = this.currentTest.ctx.items;
            if (items) {
                await this.browser.yaOpenSection('disk');
                await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
            }
        });

        /**
         * @param {string} firstFolderName
         * @param {string} secondFolderName
         * @param {string|string[]} filesToUpload
         * @param {Object} opts
         * @returns {Promise<*|string|string[]>}
         */
        const getReady = async function(firstFolderName, secondFolderName, filesToUpload, opts) {
            await this.yaCreateFolder(firstFolderName);
            await this.yaGoToFolderAndWaitForListingSpinnerHide(firstFolderName);
            if (Array.isArray(secondFolderName)) {
                await this.yaCreateFolders(secondFolderName, null);
            } else {
                await this.yaCreateFolder(secondFolderName, null);
            }
            return this.yaUploadFiles(filesToUpload, opts);
        };

        it('diskclient-592, diskclient-1190: Перемещение файла заполненным юзером', async function() {
            const bro = this.browser;
            const firstFolderName = 'Earth';
            const secondFolderName = 'Moon';
            let folderNameForMoving = secondFolderName;

            await bro.yaClientLoginFast('yndx-ufo-test-224');

            await bro.yaGoToFolderAndWaitForListingSpinnerHide(firstFolderName);
            let fileNames = await bro.yaGetListingElementsTitles();

            // Проверяем, что в папке не меньше 90 файлов.
            // Сделано для того, чтобы в папках было примерно одинаковое кол-во файлов для перемещения.
            if (fileNames.length < 90) {
                await bro.url(consts.NAVIGATION.folder(secondFolderName).url);
                fileNames = await bro.yaGetListingElementsTitles();
                folderNameForMoving = firstFolderName;
            }
            const fileNameToBeMoved = fileNames[Math.floor(Math.random() * fileNames.length)];

            await bro.yaSelectResource(fileNameToBeMoved);
            await bro.yaMoveSelected(folderNameForMoving);

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                { name: fileNameToBeMoved, folder: folderNameForMoving },
                consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
            );

            await bro.url(consts.NAVIGATION.folder(folderNameForMoving).url);
            await bro.yaScrollToEnd();
            await bro.yaAssertListingHas(fileNameToBeMoved);
        });

        it('diskclient-723, diskclient-5070: Перемещение файла с публичной ссылкой', async function() {
            const workFolderName = getTmpFolderName();
            const folderNameForMoving = getTmpFolderName('move-folder');
            const testFileName = 'test-file.jpg';
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            this.currentTest.ctx.items = [workFolderName, folderNameForMoving];

            await bro.yaClientLoginFast('yndx-ufo-test-474');
            await getReady.call(bro, workFolderName, folderNameForMoving, testFileName, { closeUploader: true });

            await bro.yaSelectResource(testFileName);
            await bro.yaShareSelected();

            if (isMobile) {
                await bro.yaWaitForVisible(clientPopups.touch.mobilePaneVisible());
                await bro.yaExecuteClick(clientNavigation.touch.modalCell());
                await bro.yaWaitForHidden(clientPopups.touch.mobilePaneVisible());
            } else {
                await bro.click(clientPopups.common.shareDialog.closeButton());
                await bro.yaWaitForHidden(clientPopups.common.shareDialog());
            }

            await bro.yaMoveSelected(workFolderName, folderNameForMoving);

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                { name: testFileName, folder: folderNameForMoving },
                consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
            );

            await bro.yaGoToFolderAndWaitForListingSpinnerHide(folderNameForMoving);
            await bro.yaAssertListingHas(testFileName);
            await bro.yaWaitForVisible(clientContentListing[isMobile ? 'touch' : 'desktop']
                .listingItemPublicLinkIcon()
            );
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-637: Перемещение файла драг-энд-дропом', async function() {
            const workFolderName = getTmpFolderName();
            const folderNameForMoving = getTmpFolderName('move-folder');
            const testFileName = 'test-file.jpg';
            const bro = this.browser;

            this.currentTest.ctx.items = [workFolderName, folderNameForMoving];

            await bro.yaClientLoginFast('yndx-ufo-test-475');
            await getReady.call(bro, workFolderName, folderNameForMoving, testFileName, { closeUploader: true });

            await bro.yaDragAndDrop(
                clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, testFileName),
                clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, folderNameForMoving),
            );

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                { name: testFileName, folder: folderNameForMoving },
                consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
            );

            await bro.yaCloseActionBar();
            await bro.yaGoToFolderAndWaitForListingSpinnerHide(folderNameForMoving);
            await bro.yaAssertListingHas(testFileName);
        });

        it('diskclient-613, diskclient-5075: Перемещение нескольких файлов', async function() {
            const workFolderName = getTmpFolderName();
            const folderNameForMoving = getTmpFolderName('move-folder');
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            this.currentTest.ctx.items = [workFolderName, folderNameForMoving];

            await bro.yaClientLoginFast('yndx-ufo-test-476');

            const [firstTestFileName, secondTestFileName] = await getReady.call(
                bro,
                workFolderName,
                folderNameForMoving,
                ['test-file.jpg', 'test-file.jpg'],
                { uniq: true }
            );

            if (!isMobile) {
                await bro.yaCloseActionBar();
            }

            await bro.yaSelectResources([firstTestFileName, secondTestFileName]);
            await bro.yaMoveSelected(workFolderName, folderNameForMoving);

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                folderNameForMoving,
                consts.TEXT_NOTIFICATION_OBJECTS_MOVED_TO_FOLDER(2)
            );

            await bro.yaGoToFolderAndWaitForListingSpinnerHide(folderNameForMoving);
            await bro.yaAssertListingHas(firstTestFileName);
            await bro.yaAssertListingHas(secondTestFileName);
        });

        it('diskclient-3444, diskclient-5074: Перемещение группы файлов из раздела Последние', async function() {
            const workFolderName = getTmpFolderName();
            const folderNameForMoving = getTmpFolderName('first-folder');
            const bro = this.browser;

            this.currentTest.ctx.items = [workFolderName, folderNameForMoving];

            await bro.yaClientLoginFast('yndx-ufo-test-218');
            await bro.yaOpenSection('disk');

            const testFileNames = await getReady.call(
                bro,
                workFolderName,
                folderNameForMoving,
                ['test-file.jpg', 'test-file.jpg'],
                { uniq: true }
            );

            await bro.yaOpenSection('recent');

            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            await bro.yaSelectResources(testFileNames);

            await bro.yaMoveSelected(workFolderName, folderNameForMoving);

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationWithText(
                folderNameForMoving,
                consts.TEXT_NOTIFICATION_OBJECTS_MOVED_TO_FOLDER(2)
            );

            for (const testFileName of testFileNames) {
                await bro.yaAssertListingHas(testFileName);
            }
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-3421: Проверка в днд разделе "Файлы": Перемещение', async function() {
            this.testpalmId = 'diskclient-3421';

            const testFileName = 'test-file.jpg';
            const workFolderName = getTmpFolderName('first-folder');
            const folderNameForMoving = getTmpFolderName('second-folder');
            const folderNameToBeMoved = getTmpFolderName('test-folder');
            const bro = this.browser;

            this.currentTest.ctx.items = [workFolderName, folderNameForMoving, folderNameToBeMoved];

            await bro.yaClientLoginFast('yndx-ufo-test-477');
            await getReady.call(bro, workFolderName, [folderNameForMoving, folderNameToBeMoved], testFileName);

            // перенос папки
            await bro.yaDragAndDrop(
                clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, folderNameToBeMoved),
                clientContentListing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, folderNameForMoving),
                async() => {
                    await bro.yaAssertView(`${this.testpalmId}-1`, clientNavigation.desktop.listingCrumbs());
                }
            );

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                { name: folderNameToBeMoved, folder: folderNameForMoving },
                consts.TEXT_NOTIFICATION_FOLDER_MOVED_TO_FOLDER
            );

            // перенос файла
            await bro.yaDragAndDrop(
                clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, testFileName),
                clientContentListing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, folderNameForMoving),
                async() => {
                    await bro.yaAssertView(`${this.testpalmId}-2`, clientNavigation.desktop.listingCrumbs());
                }
            );

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                { name: testFileName, folder: folderNameForMoving },
                consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
            );

            await bro.yaGoToFolderAndWaitForListingSpinnerHide(folderNameForMoving);
            await bro.yaAssertListingHas(testFileName);
            await bro.yaAssertListingHas(folderNameToBeMoved);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-3416: Проверка днд в разделе "Последние": Перемещение', async function() {
            const bro = this.browser;
            const workFolderName = getTmpFolderName();
            this.currentTest.ctx.items = [workFolderName];

            await bro.yaClientLoginFast('yndx-ufo-test-218');
            await bro.yaCreateFolder(workFolderName);
            await bro.yaGoToFolderAndWaitForListingSpinnerHide(workFolderName);

            const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true, closeUploader: true });

            this.currentTest.ctx.items.push(testFileName);

            await bro.yaOpenSection('recent');
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

            await bro.yaDragAndDrop(
                clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, testFileName),
                clientNavigation.desktop.navigationItemDisk(),
                async() => {
                    const isDraggingElementIsExisting = await bro.waitForExist(
                        clientContentListing.desktop.listingDraggingElement_active()
                    );
                    assert.isOk(isDraggingElementIsExisting, 'Перетаскиваемый элемент не отобразился под курсором.');
                }
            );

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(testFileName, consts.TEXT_NOTIFICATION_FILE_MOVED);
            const sectionName = await bro.getText(clientContentListing.common.listingBodyItemsXpath().replace(
                /:titleText/g, testFileName) + '/preceding-sibling::h2'
            );
            assert.equal(sectionName, 'Сегодня');
        });

        hermione.auth.createAndLogin();
        it('diskclient-4454, diskclient-6687: Перемещение файла по пушу', async function() {
            const bro = this.browser;
            await bro.yaSkipWelcomePopup();

            const firstTab = await bro.getWindowHandle();

            const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });
            const testFolderName = `tmp-${Date.now()}`;
            await bro.yaCreateFolder(testFolderName);

            const secondTab = await bro.yaOpenNewTab();
            await bro.yaAssertListingHas(testFileName);

            await bro.switchTab(firstTab);

            await bro.yaSelectResource(testFileName);
            await bro.yaMoveSelected(testFolderName);
            await bro.yaWaitNotificationForResource(
                { name: testFileName, folder: testFolderName },
                consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
            );

            await bro.switchTab(secondTab);

            await bro.yaAssertListingHasNot(testFileName);
            await bro.yaOpenListingElement(testFolderName);
            await bro.yaAssertListingHas(testFileName);
        });
    });

    describe('Удаление файлов', () => {
        afterEach(async function() {
            const items = this.currentTest.ctx.items || [];
            if (items.length) {
                await this.browser.url(consts.NAVIGATION.disk.url);
                await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
            }
        });

        /**
         * @param {string} user
         * @param {boolean} group
         * @param {boolean} fromRecent
         * @param {boolean} startFromDisk
         * @returns {Promise<void>}
         */
        const deleteButtonTest = async function(user, group, fromRecent = false, startFromDisk = true) {
            const bro = this.browser;

            await bro.yaClientLoginFast(user);
            if (startFromDisk) {
                await bro.yaOpenSection('disk');
            }
            const testFileNames = await bro.yaUploadFiles(
                group ? ['test-file.jpg', 'test-file.jpg'] : ['test-file.jpg'],
                { uniq: true }
            );
            this.currentTest.ctx.items = testFileNames;

            if (fromRecent) {
                await bro.yaOpenSection('recent');
                await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            }

            await bro.yaSelectResources(testFileNames);
            await bro.yaDeleteSelected();

            await bro.yaAssertProgressBarAppeared();
            if (group) {
                await bro.yaWaitNotificationWithText('2 файла перемещены в Корзину');
            } else {
                await bro.yaWaitNotificationForResource(testFileNames[0], consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
            }
            await bro.yaAssertProgressBarDisappeared();
            for (const fileName of testFileNames) {
                await bro.yaAssertListingHasNot(fileName);
            }

            return testFileNames;
        };

        /**
         * @param {boolean} group
         * @returns {Promise<void>}
         */
        const testFunctionForDeletingFromTrash = async function(group) {
            const bro = this.browser;
            const platform = await bro.yaIsMobile() ? 'touch' : 'desktop';

            const testFileNames = await deleteButtonTest.call(this, 'yndx-ufo-test-232', group, false, false);

            await bro.yaOpenSection('trash');

            await bro.yaSelectResources(testFileNames);
            await bro.click(clientPopups[platform].actionBar.deleteFromTrashButton());
            await bro.yaAssertProgressBarAppeared();
            if (group) {
                await bro.yaWaitNotificationWithText('2 файла удалены');
            } else {
                await bro.yaWaitNotificationForResource(testFileNames[0], consts.TEXT_NOTIFICATION_FILE_DELETED);
            }
        };

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-923: Проверка днд на иконку Корзины', async function() {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-424');

            const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });
            this.currentTest.ctx.items = testFileName;

            await bro.yaDragAndDrop(
                clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, testFileName),
                clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, 'Корзина')
            );

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(testFileName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
            await bro.yaAssertListingHasNot(testFileName);
        });

        it('diskclient-668, 5188: Кнопка Удалить', async function() {
            await deleteButtonTest.call(this, 'yndx-ufo-test-218', false, true);
        });

        it('diskclient-621, 5189: Удаление файла в корне Диска из топбара', async function() {
            await deleteButtonTest.call(this, 'yndx-ufo-test-218', false);
        });

        it('diskclient-618, 1182: Удаление файла из Корзины', async function() {
            await testFunctionForDeletingFromTrash.call(this, false);
        });

        it('diskclient-614, 5190: Удаление нескольких файлов', async function() {
            await deleteButtonTest.call(this, 'yndx-ufo-test-218', true);
        });

        it('diskclient-3445, 5192: Удаление группы файлов из раздела "Последние"', async function() {
            await deleteButtonTest.call(this, 'yndx-ufo-test-218', true, true);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-3428: Проверка днд в папках: Удаление', async function() {
            const workFolderName = getTmpFolderName();
            const testFileName = 'test-file.jpg';
            this.currentTest.ctx.items = workFolderName;
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-425');

            await bro.yaCreateFolder(workFolderName);
            await bro.yaOpenListingElement(workFolderName);

            await bro.yaUploadFiles(testFileName);

            await bro.yaDragAndDrop(
                clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, testFileName),
                clientNavigation.desktop.navigationItemTrash()
            );

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(testFileName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
            await bro.yaAssertListingHasNot(testFileName);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-3419: Проверка в днд разделе "Файлы": Удаление', async function() {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-426');

            const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });
            this.currentTest.ctx.items = testFileName;

            await bro.yaDragAndDrop(
                clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, testFileName),
                clientNavigation.desktop.navigationItemTrash()
            );

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(testFileName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
            await bro.yaAssertListingHasNot(testFileName);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-3415: Проверка днд в разделе "Последние": Удаление', async function() {
            const workFolderName = getTmpFolderName();
            const bro = this.browser;
            this.currentTest.ctx.items = workFolderName;

            await bro.yaClientLoginFast('yndx-ufo-test-218');
            await bro.yaOpenSection('disk');

            await bro.yaCreateFolder(workFolderName);
            await bro.yaOpenListingElement(workFolderName);

            const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });

            await bro.yaOpenSection('recent');
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

            await bro.yaDragAndDrop(
                clientContentListing.common.listingBodyItemsIconXpath().replace(/:titleText/g, testFileName),
                clientNavigation.desktop.navigationItemTrash()
            );

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(testFileName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
            await bro.yaAssertListingHasNot(testFileName);
        });

        it('diskclient-3277, 5193: Удаление нескольких файлов из Корзины', async function() {
            await testFunctionForDeletingFromTrash.call(this, true);
        });

        it('diskclient-3275, 5194: Удаление папки', async function() {
            const workFolderName = getTmpFolderName();
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-427');

            await bro.yaCreateFolder(workFolderName);
            this.currentTest.ctx.items = workFolderName;

            await bro.yaSelectResource(workFolderName);
            await bro.yaDeleteSelected();

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(workFolderName, consts.TEXT_NOTIFICATION_FOLDER_MOVED_TO_TRASH);
            await bro.yaAssertListingHasNot(workFolderName);
        });

        it('diskclient-1359, 5195: Корзина. Удаление большого количества файлов', async function() {
            this.browser.executionContext.timeout(100000);

            const bro = this.browser;

            await copyResourceToTheSameFolder.call(this, 'yndx-ufo-test-239', 'testFiles', true);

            await bro.yaSelectResource(this.currentTest.ctx.newTestFileName);
            await bro.yaDeleteSelected();

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                this.currentTest.ctx.newTestFileName,
                consts.TEXT_NOTIFICATION_FOLDER_MOVED_TO_TRASH,
                { timeout: consts.FILE_OPERATIONS_TIMEOUT }
            );

            try {
                await bro.yaCleanTrash();
            } catch (error) {}
        });

        it('diskclient-6131, 1580: Длинное название. Удаление файла', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-6131' : 'diskclient-1580';

            const testFileName = await loginAndUploadFiles(
                'yndx-ufo-test-80',
                'yndx-ufo-test-33',
                bro
            );
            const newTestFileName = basename(getLongFileName(testFileName), '.txt');
            this.currentTest.ctx.items = [testFileName, newTestFileName];

            await bro.yaSelectResource(testFileName);
            await bro.yaRenameSelected(newTestFileName);
            // confirm extension change
            await bro.click(clientPopups.common.confirmRenameDialog.submitButton());
            await bro.yaAssertListingHas(newTestFileName);
            await bro.yaAssertListingHasNot(testFileName);
            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await bro.yaSelectResource(newTestFileName);
            await bro.yaDeleteSelected();

            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();
            await bro.yaAssertListingHasNot(newTestFileName);
        });

        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-763', 'diskclient-5409'] });
        it('diskclient-763, 5409: Удалить все файлы из корня Диска', async function() {
            const bro = this.browser;
            this.testpalmId = await bro.yaIsMobile() ? 'diskclient-5409' : 'diskclient-763';

            this.currentTest.ctx.listingResources = await bro.yaUploadFiles(
                ['test-file.txt', 'test-file.txt', 'test-file.txt'],
                { uniq: true }
            );

            await bro.yaSelectAll();
            await bro.yaDeleteSelected();
            await bro.yaWaitForVisible(clientContentListing.common.listingBody.items.trashIconFull());

            assert(await bro.yaListingNotEmpty() === false);
        });

        hermione.auth.createAndLogin();
        it('diskclient-4445, diskclient-4467: Очистка Корзины по пушу', async function() {
            const bro = this.browser;
            await bro.yaSkipWelcomePopup();

            const firstTab = await bro.getWindowHandle();

            const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });

            const secondTab = await bro.yaOpenNewTab();
            await bro.yaAssertListingHas(testFileName);

            await bro.switchTab(firstTab);
            await bro.yaCleanTrash();
            await bro.yaAssertProgressBarDisappeared();
            await bro.yaAssertListingHasNot(testFileName);

            await bro.switchTab(secondTab);
            await bro.yaOpenSection('trash');
            await bro.yaAssertListingHasNot(testFileName);
        });
    });

    describe('Восстановление файлов', () => {
        afterEach(async function() {
            const items = this.currentTest.ctx.items || [];
            if (items.length) {
                await this.browser.url(consts.NAVIGATION.disk.url);
                await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
            }
        });

        const uploadAndDeleteFiles = async function(user) {
            const bro = this.browser;
            await bro.yaClientLoginFast(user);
            await bro.yaOpenSection('disk');

            const testFileNames = await bro.yaUploadFiles(
                ['test-file.jpg', 'test-file.jpg'],
                { uniq: true }
            );
            this.currentTest.ctx.items = testFileNames;

            await bro.yaSelectResources(testFileNames);
            await bro.yaDeleteSelected();

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationWithText('2 файла перемещены в Корзину');
            await bro.yaAssertProgressBarDisappeared();

            for (const fileName of testFileNames) {
                await bro.yaAssertListingHasNot(fileName);
            }

            return testFileNames;
        };

        it('diskclient-6121, 641: Восстановление группы файлов из Корзины', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-6121' : 'diskclient-641';
            const env = await bro.yaIsMobile() ? 'touch' : 'desktop';
            const user = await bro.yaIsMobile() ? 'yndx-ufo-test-521' : 'yndx-ufo-test-522';

            const testFileNames = await uploadAndDeleteFiles.call(this, user);

            await bro.yaOpenSection('trash');
            await bro.yaSelectResources(testFileNames);

            await bro.yaWaitActionBarDisplayed();
            await bro.click(clientPopups[env].actionBar.restoreFromTrashButton());

            await bro.yaWaitNotificationWithText('2 файла восстановлены');
            await bro.url(consts.NAVIGATION.disk.url);

            for (const fileName of testFileNames) {
                await bro.yaAssertListingHas(fileName);
            }
        });
    });

    describe('Переименование файлов', () => {
        afterEach(async function() {
            const items = this.currentTest.ctx.items || [];
            if (items.length) {
                await this.browser.url(consts.NAVIGATION.disk.url);
                await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
            }
        });

        it('diskclient-675, 5198: Переименование файла в разделе Последние', async function() {
            const bro = this.browser;

            await bro.yaClientLoginFast('yndx-ufo-test-218');
            await bro.yaOpenSection('disk');

            const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });
            const splitTestFileName = testFileName.split('.');
            splitTestFileName[splitTestFileName.length - 2] += '-renamed';
            const newTestFileName = splitTestFileName.join('.');
            this.currentTest.ctx.items = [testFileName, newTestFileName];

            await bro.yaOpenSection('recent');
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

            await bro.yaSelectResource(testFileName);
            await bro.yaRenameSelected(newTestFileName);

            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await bro.yaAssertListingHasNot(testFileName);
            await bro.yaAssertListingHas(newTestFileName);
        });

        it('diskclient-5122, 5121: Переименование файла c изменением расширения', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmid = isMobile ? 'diskclient-5122' : 'diskclient-5121';

            const testFileName = await loginAndUploadFiles('yndx-ufo-test-52', 'yndx-ufo-test-02', bro);
            const newFileName = 'rename-' + basename(testFileName, '.txt');
            this.currentTest.ctx.items = [testFileName, newFileName];

            await bro.yaSelectResource(testFileName);
            await bro.yaRenameSelected(newFileName);
            await bro.click(clientPopups.common.confirmRenameDialog.submitButton());
            await bro.yaAssertListingHas(newFileName);
            await bro.yaAssertListingHasNot(testFileName);
        });

        it('diskclient-5125, diskclient-5124: Отмена переименования файла c изменением расширения', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmid = isMobile ? 'diskclient-5125' : 'diskclient-5124';

            /**
             * @param {Object} bro
             * @param {boolean} isMobile
             */
            const alternativeCancelAction = async(bro, isMobile) => {
                if (isMobile) {
                    await bro.yaTapOnScreen(1, 1);
                } else {
                    await bro.keys('Escape');
                }
            };

            await bro.yaClientLoginFast('yndx-ufo-test-250');
            const testFileName = 'Горы.jpg';
            const newFileName = basename(testFileName, '.jpg') + '.jpg2';

            await bro.yaSelectResource(testFileName);
            await bro.yaCallActionInActionBar('rename');
            await bro.yaWaitForVisible(clientPopups.common.createDialog());
            await bro.yaSetValue(clientPopups.common.createDialog.nameInput(), newFileName);
            await bro.click(clientPopups.common.createDialog.submitButton());
            await bro.yaWaitForVisible(clientPopups.common.confirmRenameDialog());

            await alternativeCancelAction(bro, isMobile);

            await bro.yaWaitForHidden(clientPopups.common.confirmRenameDialog());

            const isCreateDialogVisible = await bro.isVisible(clientPopups.common.createDialog());
            assert(isCreateDialogVisible);

            await alternativeCancelAction(bro, isMobile);

            await bro.yaWaitForHidden(clientPopups.common.createDialog());
        });

        it('diskclient-5123, diskclient-4997: Переименование файла c изменением расширения с оставленным расширением', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmid = isMobile ? 'diskclient-5123' : 'diskclient-4197';

            // 1. Заливаем тестовый файл с расширением .txt
            const testFileName = await loginAndUploadFiles('yndx-ufo-test-265', 'yndx-ufo-test-250', bro);

            // 2. Задаем новое имя файла, убираем расширение файла .txt
            const newFileName = basename(testFileName, '.txt') + (isMobile ? 'mobile-' : 'desktop-');
            this.currentTest.ctx.items = [testFileName, newFileName + '.txt'];

            // 3. Выбираем ресурс и нажимаем на "переименовать"
            await bro.yaSelectResource(testFileName);
            await bro.yaCallActionInActionBar('rename');
            await bro.yaWaitForVisible(clientPopups.common.createDialog());

            // 4. Задаем новое имя файла и нажимаем на "сохранить"
            await bro.yaSetValue(clientPopups.common.createDialog.nameInput(), newFileName);
            await bro.click(clientPopups.common.createDialog.submitButton());

            // 5. Ожидаем увидеть диалог подтверждения смены расширения
            const confirmationDialog = await bro.$(clientPopups.common.confirmationDialog());

            await confirmationDialog.waitForExist({ timeout: 5000 });

            // 6. Отменяем смену расширения
            await bro.click(clientPopups.common.confirmationDialog.cancelButton());
            await bro.yaWaitForHidden(clientPopups.common.confirmationDialog());
            // 7. Проверяем, что в листинге есть файл с новым именем и изначальным расширением
            await bro.yaAssertListingHas(newFileName + '.txt');
            await bro.yaAssertListingHasNot(newFileName);
        });
    });

    describe('Копирование файлов', () => {
        afterEach(async function() {
            const { items } = this.currentTest.ctx;
            if (items) {
                await this.browser.url(consts.NAVIGATION.disk.url);
                await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
            }
        });

        /**
         * @param {boolean} isGroup
         * @param {boolean} fromRecent
         * @returns {Promise<void>}
         */
        async function copyResourceTest(isGroup, fromRecent = false) {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-277');
            const workFolderName = getTmpFolderName();
            this.currentTest.ctx.items = [workFolderName];

            const testFileNames = await bro.yaUploadFiles(isGroup ?
                ['test-file.txt', 'test-file.txt'] :
                ['test-file.txt'],
            { uniq: true }
            );
            this.currentTest.ctx.items.push(...testFileNames);

            await bro.yaCreateFolder(workFolderName);
            await bro.yaCloseActionBar();

            if (fromRecent) {
                await bro.yaOpenSection('recent');
            }

            await bro.yaSelectResources(testFileNames);
            await bro.yaCopySelected(workFolderName);

            await bro.yaAssertProgressBarAppeared();
            if (isGroup) {
                await bro.yaWaitNotificationForResource(
                    workFolderName,
                    consts.TEXT_NOTIFICATION_OBJECTS_COPIED_TO_FOLDER(2)
                );
            } else {
                await bro.yaWaitNotificationForResource(
                    {
                        name: testFileNames[0],
                        folder: workFolderName
                    },
                    consts.TEXT_NOTIFICATION_FILE_COPIED_TO_FOLDER
                );
            }
            await bro.yaAssertProgressBarDisappeared();

            for (const fileName of testFileNames) {
                await bro.yaAssertListingHas(fileName);
            }
            if (fromRecent) {
                for (const testFileName of testFileNames) {
                    const { length: countOfFilesWithTheSameName } = await bro.$$(
                        clientContentListing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, testFileName)
                    );
                    assert.equal(countOfFilesWithTheSameName, 2);
                }
            } else {
                await bro.yaGoToFolderAndWaitForListingSpinnerHide(workFolderName);
                for (const fileName of testFileNames) {
                    await bro.yaAssertListingHas(fileName);
                }
            }
        }

        it('diskclient-1191, 593: Копирование файла заполненным юзером', async function() {
            const bro = this.browser;
            const testFileName = 'test-file';
            const testFolder = 'Sunshine';

            await bro.yaClientLoginFast('yndx-ufo-test-439');
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

            await bro.yaSelectResource(testFileName);
            await bro.yaCopySelected(testFolder);

            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await bro.yaWaitNotificationWithText(consts.TEXT_NOT_ENOUGH_FREE_SPACE);
        });

        it('diskclient-1328, diskclient-6135: Копирование папки заполненным юзером', async function() {
            const bro = this.browser;
            const testData = {
                user: 'yndx-ufo-test-529',
                srsFolderName: 'srsFolder'
            };

            await bro.yaClientLoginFast(testData.user);
            await bro.yaOpenSection('disk');

            await bro.yaFreeSpaceIsEqual(0);
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

            const dstFolderName = await bro.yaGetUniqResourceName();
            this.currentTest.ctx.listingResources = [dstFolderName];
            await bro.yaOpenCreateDirectoryDialog();
            await bro.yaSetResourceNameAndApply(dstFolderName);

            await bro.yaSelectResource(testData.srsFolderName);
            await bro.yaCopySelected(dstFolderName);

            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertProgressBarDisappeared();

            await bro.yaWaitNotificationWithText(consts.TEXT_NOT_ENOUGH_FREE_SPACE);
        });

        const openSelectFolderDialog = async function(fileName) {
            const bro = this.browser;
            await bro.yaSelectResource(fileName);
            await bro.yaCallActionInActionBar('copy');
            await bro.yaWaitForVisible(popups.common.selectFolderDialog(), 1000);
        };

        /**
         * @param {string} action
         * @returns {Promise<void>}
         */
        const closeSelectFolderDialog = async function(action) {
            const bro = this.browser;
            switch (action) {
                case 'по кнопке "Отменить"':
                    await bro.click(popups.common.selectFolderDialog.cancelButton());
                    break;
                case 'по крестику закрытия':
                    await bro.click(popups.common.selectFolderDialog.closeButton());
                    break;
                case 'по клавише Esc':
                    await bro.keys('Escape');
                    break;
            }
            await bro.yaWaitForHidden(popups.common.selectFolderDialog());
        };

        it('diskclient-6137, diskclient-6141, diskclient-6139, diskclient-6142, diskclient-6140: Закрытие диалога выбора папки', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            const testData = {
                user: 'yndx-ufo-test-533',
                fileName: 'Горы.jpg'
            };

            await bro.yaClientLoginFast(testData.user);
            await bro.yaOpenSection('disk');

            await openSelectFolderDialog.call(this, testData.fileName);
            await closeSelectFolderDialog.call(this, 'по кнопке "Отменить"');

            await openSelectFolderDialog.call(this, testData.fileName);
            await closeSelectFolderDialog.call(this, 'по крестику закрытия');

            if (!isMobile) {
                await openSelectFolderDialog.call(this, testData.fileName);
                await closeSelectFolderDialog.call(this, 'по клавише Esc');
            }
        });

        it('diskclient-6037, diskclient-6146: Ограничение имени файла при копировании в ту же директорию', async function() {
            const bro = this.browser;

            const testData = {
                user: 'yndx-ufo-test-534',
                fileName: 'Горы.jpg'
            };

            await bro.yaClientLoginFast(testData.user);

            await openSelectFolderDialog.call(this, testData.fileName);

            await bro.yaSelectFolderInDialogAndApply();

            await bro.yaSetValue(popups.common.renameDialog.nameInput(), consts.TEST_256_CHAR_NAME);
            assert.equal(
                await bro.getText(popups.common.renameDialog.renameError()),
                consts.TEXT_NOTIFICATION_FILE_TITlE_TOO_LONG
            );
            assert.equal(await bro.isEnabled(popups.common.renameDialog.submitButton()), false);

            await bro.keys('Backspace');
            assert.equal(
                await bro.getText(popups.common.renameDialog.renameError()),
                ''
            );
            assert.equal(await bro.isEnabled(popups.common.renameDialog.submitButton()), true);
        });

        it('diskclient-665, 5613: Кнопка Копировать', async function() {
            await copyResourceTest.call(this, false, true);
        });

        it('diskclient-636, 5419: Копирование файлов с заменой', async function() {
            const bro = this.browser;
            const workFolderName = getTmpFolderName();
            const imageTestFileName = 'test-file.jpg';
            const textTestFileName = 'test-file.txt';
            const testFileNames = [imageTestFileName, textTestFileName];
            this.currentTest.ctx.items = workFolderName;
            await bro.yaClientLoginFast('yndx-ufo-test-275');

            await bro.yaCreateFolder(workFolderName);
            await bro.yaCloseActionBar();

            await bro.yaGoToFolderAndWaitForListingSpinnerHide(workFolderName);

            await bro.yaUploadFiles(testFileNames);

            await bro.yaSelectResources(testFileNames);
            await bro.yaCopySelected(workFolderName);

            await bro.yaWaitForVisible(clientPopups.common.renameDialog());
            await bro.click(clientPopups.common.renameDialog.closeButton());

            await bro.yaWaitForVisible(clientPopups.common.renameDialog());
            const newTestFileName = await bro.getValue(clientPopups.common.renameDialog.nameInput());
            await bro.click(clientPopups.common.renameDialog.submitButton());

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                {
                    name: newTestFileName,
                    folder: workFolderName
                },
                consts.TEXT_NOTIFICATION_FILE_COPIED_TO_FOLDER
            );
            await bro.yaAssertProgressBarDisappeared();

            for (const fileName of [newTestFileName, ...testFileNames]) {
                await bro.yaAssertListingHas(fileName);
            }
        });

        it('diskclient-612, 5614: Копирование нескольких файлов', async function() {
            await copyResourceTest.call(this, true);
        });

        it('diskclient-3443, 5615: Кнопка Копировать: Группа файлов', async function() {
            await copyResourceTest.call(this, true, true);
        });

        it('diskclient-1360, 5420: Копирование большого количества файлов', async function() {
            this.browser.executionContext.timeout(100000);
            await copyResourceToTheSameFolder.call(this, 'yndx-ufo-test-407', 'test-files', true);
        });

        it('diskclient-1613, 5616: Копирование файла в ту же директорию', async function() {
            await copyResourceToTheSameFolder.call(this, 'yndx-ufo-test-409', 'dont-touch-me.jpg', false);
        });

        hermione.auth.createAndLogin();
        it('diskclient-4458, diskclient-6689: Копирование ресурса по пушу', async function() {
            const bro = this.browser;
            await bro.yaSkipWelcomePopup();

            const firstTab = await bro.getWindowHandle();

            const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });
            const testFolderName = 'folder';
            await bro.yaCreateFolder(testFolderName);

            await bro.newWindow(await bro.getUrl());
            const secondTab = await bro.getWindowHandle();
            await bro.yaAssertListingHas(testFileName);

            await bro.switchToWindow(firstTab);

            await bro.yaSelectResource(testFileName);
            await bro.yaCopySelected(testFolderName);
            await bro.yaWaitNotificationForResource(
                { name: testFileName, folder: testFolderName },
                consts.TEXT_NOTIFICATION_FILE_COPIED_TO_FOLDER
            );

            await bro.switchToWindow(secondTab);

            await bro.yaAssertListingHas(testFileName);
            await bro.yaOpenListingElement(testFolderName);
            await bro.yaAssertListingHas(testFileName);
        });
    });
});
