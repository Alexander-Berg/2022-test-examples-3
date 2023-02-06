const clientPopups = require('../page-objects/client-popups');
const clientNavigation = require('../page-objects/client-navigation');
const clientPageObjects = require('../page-objects/client');
const clientContentListing = require('../page-objects/client-content-listing');

const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { retriable } = require('@ps-int/ufo-hermione/helpers/retries');
const listing = require('../page-objects/client-content-listing').common;
const { NAVIGATION } = require('../config').consts;
const constant = require('../config').consts;
const { assert } = require('chai');
const { consts } = require('../config');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

const DEFAULT_FOLDER_NAME = 'Новая папка';

/**
 * @param {string} touchUser
 * @param {string} desktopUser
 * @param {Browser} bro
 * @returns {string} folder name
 */
async function loginAndCreateFolder(touchUser, desktopUser, bro) {
    const user = await bro.yaIsMobile() ? touchUser : desktopUser;
    const folderName = await bro.yaGetUniqResourceName();

    await bro.yaClientLoginFast(user);
    await bro.url(consts.NAVIGATION.disk.url);
    await bro.yaCreateFolder(folderName);
    await bro.yaAssertListingHas(folderName);
    await bro.yaWaitActionBarDisplayed();

    return folderName;
}

describe('Действия с папкой -> ', () => {
    afterEach(async function() {
        const listingResources = this.currentTest.ctx.listingResources || [];

        if (listingResources.length) {
            const bro = this.browser;

            if (await bro.yaIsActionBarDisplayed()) {
                await bro.yaCloseActionBar();
            }

            const createDialog = await bro.$(clientPopups.common.createDialog());
            if (await createDialog.isDisplayed()) {
                const createDialogCloseButton = await bro.$(clientPopups.common.createDialog.closeButton());

                await createDialogCloseButton.click();
            }

            await bro.yaOpenSection('disk');
            await bro.yaDeleteCompletely(listingResources, { safe: true, fast: true });
        }
    });

    describe('Смоуки', () => {
        it('diskclient-1396, 1487: Смоук: создание папки', async function() {
            const bro = this.browser;
            const folderName = await bro.yaGetUniqResourceName();
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-1396' : 'diskclient-1487';

            await bro.yaClientLoginFast(isMobile ? 'yndx-ufo-test-50' : 'yndx-ufo-test-00');
            await bro.url(consts.NAVIGATION.disk.url);
            this.currentTest.ctx.listingResources = [folderName];

            await bro.yaOpenCreateDirectoryDialog();

            const defaultFolderName = await bro.getValue(clientPopups.common.createDialog.nameInput());
            assert.equal(
                defaultFolderName,
                DEFAULT_FOLDER_NAME,
                `Should have default '${DEFAULT_FOLDER_NAME}' placeholder`
            );

            await bro.yaSetResourceNameAndApply(folderName);
            await bro.yaWaitNotificationForResource(folderName, consts.TEXT_NOTIFICATION_FOLDER_CREATED);
            await bro.yaAssertListingHas(folderName);
            await bro.yaWaitActionBarDisplayed();

            await bro.yaCloseActionBar();
            await bro.yaAssertListingHas(folderName);
        });

        it('diskclient-1597, 1488: Смоук: копирование папки', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-1597' : 'diskclient-1488';

            const folderName = await loginAndCreateFolder('yndx-ufo-test-61', 'yndx-ufo-test-11', bro);
            const newFolderName = folderName + '_copy';
            this.currentTest.ctx.listingResources = [folderName, newFolderName];

            await bro.yaCallActionInActionBar('copy');
            await bro.yaWaitForVisible(clientPopups.common.selectFolderDialog());
            await bro.yaSelectFolderInDialogAndApply();
            await bro.yaSetValue(clientPopups.common.confirmationPopup.nameInput(), newFolderName);
            await bro.yaWaitForVisible(clientPopups.common.confirmationPopup.acceptButton());
            await bro.click(clientPopups.common.confirmationPopup.acceptButton());

            await bro.yaWaitNotificationForResource(newFolderName, consts.TEXT_NOTIFICATION_FOLDER_COPIED);

            await bro.yaAssertListingHas(newFolderName);
            await bro.yaAssertListingHas(folderName);
        });

        it('diskclient-1598, 1490: Смоук: переименование папки по клику на "Сохранить"', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-1598' : 'diskclient-1490';

            const folderName = await loginAndCreateFolder('yndx-ufo-test-62', 'yndx-ufo-test-12', bro);
            const newFolderName = folderName + '_rename';
            this.currentTest.ctx.listingResources = [folderName, newFolderName];

            await bro.yaRenameSelected(newFolderName);
            await bro.yaAssertListingHas(newFolderName);
            await bro.yaAssertListingHasNot(folderName);
        });

        it('diskclient-1016, 1489: Смоук: перемещение папки', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = await isMobile ? 'diskclient-1016' : 'diskclient-1489';

            const folderName = await loginAndCreateFolder('yndx-ufo-test-20', 'yndx-ufo-test-63', bro);
            const newFolderName = folderName + '_moved';
            this.currentTest.ctx.listingResources = [folderName, newFolderName];

            await bro.yaCreateFolder(newFolderName);
            await bro.yaMoveSelected(folderName);
            await bro.yaWaitNotificationForResource(
                { name: newFolderName, folder: folderName },
                consts.TEXT_NOTIFICATION_FOLDER_MOVED_TO_FOLDER
            );

            await bro.yaAssertListingHasNot(newFolderName);
            await bro.yaOpenListingElement(folderName);
            await bro.yaAssertFolderOpened(folderName);
            await bro.yaAssertListingHas(newFolderName);
        });
    });

    describe('Переименование', () => {
        it('diskclient-4723, 1273: Переименование папки по клавише Enter/Return', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-4723' : 'diskclient-1273';

            const folderName = await loginAndCreateFolder('yndx-ufo-test-150', 'yndx-ufo-test-148', bro);
            const newFolderName = folderName + '_renamed';
            this.currentTest.ctx.listingResources = [folderName, newFolderName];

            await bro.yaCallActionInActionBar('rename');
            await bro.yaSetValue(clientPopups.common.createDialog.nameInput(), newFolderName);
            await bro.keys(isMobile ? 'Return' : 'Enter');

            await bro.yaAssertListingHas(newFolderName);
            await bro.yaAssertListingHasNot(folderName);

            await bro.refresh();
            await bro.yaAssertListingHas(newFolderName);
        });

        hermione.auth.createAndLogin({ language: 'ru', tus_consumer: 'disk-front-client' });
        it('diskclient-6195, 586: Переименование папки Загрузки', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-6195' : 'diskclient-586';

            const folderName = 'Загрузки';
            const newFolderName = folderName + '_renamed';
            await bro.yaSkipWelcomePopup();

            await bro.yaCreateFolder(folderName);

            await bro.yaAssertListingHas(folderName);
            await bro.yaSelectResource(folderName);
            await bro.yaRenameSelected(newFolderName);

            await bro.yaAssertListingHasNot(folderName);
            await bro.yaAssertListingHas(newFolderName);
        });
    });

    describe('Создание', () => {
        /**
         * @param {string} folderName
         * @returns {Promise<void>}
         */
        const createAndOpenFolder = async function(folderName) {
            await this.browser.yaCreateFolder(folderName);
            await this.browser.yaAssertListingHas(folderName);
            await this.browser.yaOpenListingElement(folderName);
            await this.browser.yaWaitActionBarHidden();
            await this.browser.yaAssertFolderOpened(folderName);
        };

        /**
         * @param {Object} opts
         * @param {string} [opts.folderName]
         * @param {string} [opts.renameErrorTextTemplate]
         * @param {string} [opts.notificationErrorTextTemplate]
         * @param {boolean} [opts.shouldSaveButtonBeEnabled]
         * @returns {Promise<void>}
         */
        const wrongFolderNameTest = async function(opts) {
            const bro = this.browser;
            const {
                folderName,
                renameErrorTextTemplate,
                notificationErrorTextTemplate,
                shouldSaveButtonBeEnabled
            } = opts;

            await bro.yaClientLoginFast('yndx-ufo-test-472');
            await bro.url(consts.NAVIGATION.folder(['0', '1']).url);

            await retriable(async() => {
                await bro.yaOpenCreateDirectoryDialog();
                await bro.yaSetResourceNameAndApply(
                    folderName,
                    {
                        isDialogWillBeHidden: false,
                        needToClick: shouldSaveButtonBeEnabled
                    });
            });

            // проверка задизейбленности кнопки "сохранить"
            if (!shouldSaveButtonBeEnabled) {
                const isSaveButtonEnabled = await bro.isEnabled(clientPopups.common.renameDialog.submitButton());
                assert.isFalse(isSaveButtonEnabled);
            }

            // проверка текста нотифайки
            if (notificationErrorTextTemplate) {
                await bro.yaWaitNotificationForResource(folderName, notificationErrorTextTemplate);
            }

            // проверка текста ошибки в окне создания папки
            const expectedErrorText = renameErrorTextTemplate.replace(':name', folderName);
            await bro.yaAssertFolderPopupError(expectedErrorText);
        };

        it('diskclient-816, 1184: Создание вложенных папок', async function() {
            const bro = this.browser;
            this.currentTest.ctx.listingResources = [];

            await bro.yaClientLoginFast('yndx-ufo-test-468');

            for (let i = 0; i < 2; ++i) {
                const folderName = await bro.yaGetUniqResourceName();
                this.currentTest.ctx.listingResources.push(folderName);
                await createAndOpenFolder.call(this, folderName);
            }
        });

        it('diskclient-650, 1183: Создание папки с существующим именем', async function() {
            const bro = this.browser;
            const folderName = 'test-folder';

            await bro.yaClientLoginFast('yndx-ufo-test-469');

            await retriable(async() => {
                await bro.yaOpenCreateDirectoryDialog();
                await bro.yaSetResourceNameAndApply(folderName, { isDialogWillBeHidden: false });
            });

            await bro.yaWaitNotificationForResource(folderName, consts.TEXT_NOTIFICATION_FOLDER_CAN_NOT_CREATE);

            const expectedErrorText = consts.TEXT_NOTIFICATION_FOLDER_EXISTS.replace(':name', folderName);
            await bro.yaAssertFolderPopupError(expectedErrorText);

            await bro.click(clientPopups.common.renameDialog.closeButton());
            await bro.yaWaitForHidden(clientPopups.common.renameDialog());
        });

        it('diskclient-5158, 5738: Создание папки после удаления символов', async function() {
            const bro = this.browser;
            const folderName = await bro.yaGetUniqResourceName();
            const fakeFolderName = await bro.yaGetUniqResourceName();
            this.currentTest.ctx.listingResources = folderName;

            await bro.yaClientLoginFast('yndx-ufo-test-470');

            await bro.yaOpenCreateDirectoryDialog();
            await bro.yaSetValue(clientPopups.common.createDialog.nameInput(), fakeFolderName);

            // удаляем символы из поля ввода
            for (let i = 0; i < fakeFolderName.length; ++i) {
                await bro.addValue(clientPopups.common.createDialog.nameInput(), '\uE003');
            }

            await bro.yaSetResourceNameAndApply(folderName);
            await bro.yaAssertListingHas(folderName);
        });

        it('diskclient-4477, 4478: Создание вложенных папок после рефреша', async function() {
            const bro = this.browser;
            this.currentTest.ctx.listingResources = [];

            await bro.yaClientLoginFast('yndx-ufo-test-471');

            for (let i = 0; i < 3; ++i) {
                await bro.refresh();
                const folderName = await bro.yaGetUniqResourceName();
                this.currentTest.ctx.listingResources.push(folderName);
                await createAndOpenFolder.call(this, folderName);
            }
        });

        it('diskclient-3810, 3608: [Спецсимволы] Попытка создания вложенной папки с символом ".."', async function() {
            const opts = {
                folderName: '..',
                renameErrorTextTemplate: consts.TEXT_NOTIFICATION_FOLDER_CAN_NOT_CREATE,
                notificationErrorTextTemplate: consts.TEXT_NOTIFICATION_FOLDER_CAN_NOT_CREATE,
                shouldSaveButtonBeEnabled: true
            };
            await wrongFolderNameTest.call(this, opts);
        });

        it('diskclient-3807, 3607: [Спецсимволы] Попытка создания вложенной папки с символом "."', async function() {
            const opts = {
                folderName: '.',
                renameErrorTextTemplate: consts.TEXT_NOTIFICATION_FOLDER_CAN_NOT_CREATE,
                notificationErrorTextTemplate: consts.TEXT_NOTIFICATION_FOLDER_CAN_NOT_CREATE,
                shouldSaveButtonBeEnabled: true
            };
            await wrongFolderNameTest.call(this, opts);
        });

        it('diskclient-3804, 3606: [Спецсимволы] Попытка создания вложенной папки с символом "/"', async function() {
            const opts = {
                folderName: '/',
                renameErrorTextTemplate: consts.TEXT_RENAME_ERROR_FOLDER_NAME_CAN_NOT_INCLUDE,
                shouldSaveButtonBeEnabled: false
            };
            await wrongFolderNameTest.call(this, opts);
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-5411: Создание папки при выделенном безлимитном файле', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-5411';
            const photoName = 'photo.JPG';

            await bro.yaClientLoginFast('yndx-ufo-test-473');

            await bro.yaOpenSection('photo');
            await bro.yaSelectPhotoItemByName(photoName, true);
            await bro.yaOpenCreateDirectoryDialog(false);

            await bro.yaWaitForHidden(clientPopups.common.selectFolderDialog.notification());
        });
    });

    describe('Удаление', () => {
        hermione.only.in(clientDesktopBrowsersList);
        beforeEach(async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-539');
            await bro.yaRestoreAllFromTrash();
        });

        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-79884');
        it('diskclient-5035: Удаление папки, файл в которой открыт в редакторе', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-5035';

            await bro.url(NAVIGATION.disk.url + '/deleteFolder');
            await bro.yaOpenListingElement('doc.docx');
            await bro.pause(3000); //ждем, пока документ откроется
            const tabs = await bro.getTabIds();
            await bro.window(tabs[0]);
            await assert.equal(tabs.length, 2);

            await bro.click(listing.listing.head.header());
            await bro.yaWaitForVisible(clientPopups.common.actionPopup());
            await bro.click(clientPopups.common.actionPopup.deleteButton());
            await bro.yaWaitNotificationWithText(constant.TEXT_NOTIFICATION_FILE_WITHIN_FOLDER_EDITED);
        });
    });

    describe('Удаление', () => {
        describe('Заполненный пользователь', () => {
            const FILE = {
                name: '1-mb-file.db',
                newName: 'tmp-1-mb-file.db',
                size: 1048576
            };

            afterEach(async function() {
                const bro = this.browser;

                await bro.url(consts.NAVIGATION.disk.url);
                await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

                const allFileNames = await bro.yaGetListingElementsTitles();
                const fileNamesToDelete = allFileNames.filter((fileName) => {
                    return !['Загрузки', FILE.name, 'Корзина'].includes(fileName);
                });

                if (fileNamesToDelete.length) {
                    await bro.yaDeleteResources(fileNamesToDelete, { safe: true, fast: true });
                }
                await bro.yaCleanTrash();
            });

            /**
             *
             * @returns {Promise<void>}
             */
            const fillUser = async function() {
                const bro = this.browser;

                await bro.yaSelectResource(FILE.name);
                await bro.yaCopySelected();
                await bro.yaSetResourceNameAndApply(FILE.newName);

                await retriable(async () => {
                    await bro.refresh();
                    await bro.yaWaitForVisible(clientContentListing.common.listing());
                    await bro.yaFreeSpaceIsEqual(0);
                }, 5, 1000);

                if (!await bro.yaIsMobile()) {
                    await bro.yaAssertMemoryIndicatorText(consts.TEXT_NO_FREE_SPACE);
                }
            };

            hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-619', 'diskclient-5410'] });
            it('diskclient-619, 5410: Очистка Корзины заполненным юзером', async function() {
                const bro = this.browser;
                await fillUser.call(this);

                await bro.yaSelectResource(FILE.newName);
                await bro.yaDeleteSelected();
                await bro.yaAssertProgressBarAppeared();
                await bro.yaWaitNotificationForResource(FILE.newName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
                if (!await bro.yaIsMobile()) {
                    await bro.yaAssertMemoryIndicatorText(consts.TEXT_NO_FREE_SPACE);
                }
                await bro.yaAssertListingHasNot(FILE.newName);

                await bro.yaCleanTrash();
                if (!await bro.yaIsMobile()) {
                    await bro.yaAssertMemoryIndicatorText(`Остался ${FILE.size / 1024 / 1024} МБ из 10 ГБ`);
                }

                await retriable(async () => {
                    await bro.refresh();
                    await bro.yaWaitForVisible(clientContentListing.common.listing());
                    await bro.yaFreeSpaceIsEqual(FILE.size);
                }, 5, 1000);
            });

            hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-594', 'diskclient-1192'] });
            hermione.skip.notIn('', 'Мигает - https://st.yandex-team.ru/CHEMODAN-79453');
            it('diskclient-594, 1192: Удаление файла заполненным юзером', async function() {
                const bro = this.browser;
                await fillUser.call(this);

                await bro.yaSelectResource(FILE.newName);
                await bro.yaDeleteSelected();
                await bro.yaAssertProgressBarAppeared();
                await bro.yaWaitNotificationForResource(FILE.newName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
                if (!await bro.yaIsMobile()) {
                    await bro.yaAssertMemoryIndicatorText(consts.TEXT_NO_FREE_SPACE);
                }
                await bro.yaAssertListingHasNot(FILE.newName);
            });
        });

        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-588', 'diskclient-5408'] });
        it('diskclient-588, 5408: Папка Загрузки. Удаление', async function() {
            const bro = this.browser;
            const folder = 'Загрузки';
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-5408' : 'diskclient-588';
            this.currentTest.ctx.listingResources = [folder];

            await bro.yaCreateFolder(folder);
            await bro.yaSelectResource(folder);
            await bro.yaDeleteSelected();

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                folder,
                consts.TEXT_NOTIFICATION_FOLDER_MOVED_TO_TRASH
            );
            await bro.yaAssertListingHasNot(folder);

            if (isMobile) {
                await bro.url('/client/disk/Загрузки');
            } else {
                await bro.click(clientNavigation.desktop.navigationItemDownloads());
            }
            await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
            await bro.yaWaitForVisible(clientPageObjects.stub.background());
        });
    });
});
