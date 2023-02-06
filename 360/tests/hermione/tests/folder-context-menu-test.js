const { TEST_FOLDER_NAME, TEST_255_CHAR_NAME } = require('../config').consts;
const popups = require('../page-objects/client-popups').common;
const listing = require('../page-objects/client-content-listing').common;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const navigation = require('../page-objects/client-navigation');

const consts = require('../config').consts;

describe('Контекстное меню папки -> ', () => {
    describe('Отображение контекстного меню', () => {
        beforeEach(async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-64');
        });

        it('diskclient-1594, 1589: Смоук: Открытие контекстного меню папки кликом по названию', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            const testpalmId = isMobile ? 'diskclient-1594' : 'diskclient-1589';
            this.testpalmId = testpalmId;

            await bro.yaWaitForVisible(listing.listing());
            await bro.yaOpenListingElement(TEST_FOLDER_NAME);
            await bro.yaAssertFolderOpened(TEST_FOLDER_NAME);
            await bro.click(listing.listing.head.header());
            await bro.yaWaitForVisible(popups.actionPopup());
            await bro.pause(1000); // анимация модалки
            await bro.yaAssertView(testpalmId, isMobile ? listing.listing() : listing.listing.inner());

            if (isMobile) {
                await bro.yaExecuteClick(navigation.touch.modalCell());
            } else {
                await bro.click(listing.listing.inner());
            }

            await bro.yaWaitForHidden(popups.actionPopup());
            await bro.yaWaitForVisible(listing.listing.head.header());
        });

        hermione.only.in(clientDesktopBrowsersList, 'Три точки для КМ папки есть только на десктопах');
        it('diskclient-1506: Контекстное меню в текущей папке из "трех точек"', async function() {
            const bro = this.browser;
            const testpalmId = 'diskclient-1506';
            this.testpalmId = testpalmId;

            await bro.yaWaitForVisible(listing.listing());
            await bro.yaOpenListingElement(TEST_FOLDER_NAME);
            await bro.yaAssertFolderOpened(TEST_FOLDER_NAME);
            await bro.click(listing.listing.head.actionButton());
            await bro.yaWaitForVisible(popups.actionPopup());
            await bro.yaAssertView(testpalmId, listing.listing.inner());
        });

        hermione.only.in(clientDesktopBrowsersList, 'Три точки для КМ папки есть только на десктопах');
        it('diskclient-3779: [Длинное название] Контекстное меню в текущей папке из "трех точек"', async function() {
            const bro = this.browser;
            const testpalmId = 'diskclient-3779';
            this.testpalmId = testpalmId;

            await bro.yaWaitForVisible(listing.listing());
            await bro.yaOpenListingElement(TEST_255_CHAR_NAME);
            await bro.yaAssertFolderOpened(TEST_255_CHAR_NAME);
            await bro.click(listing.listing.head.actionButton());
            await bro.yaWaitForVisible(popups.actionPopup());
            await bro.yaAssertView(testpalmId, listing.listing.inner());
        });

        it('diskclient-5610, 5603: Контекстное меню папки исчезает после выхода из папки', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-5610' : 'diskclient-5603';

            await bro.yaOpenListingElement(TEST_FOLDER_NAME);
            await bro.yaAssertFolderOpened(TEST_FOLDER_NAME);
            await bro.click(listing.listing.head.header());
            await bro.yaWaitForVisible(popups.actionPopup());

            await bro.back();
            await bro.yaOpenListingElement(TEST_FOLDER_NAME);
            await bro.yaAssertFolderOpened(TEST_FOLDER_NAME);

            await bro.yaWaitForHidden(popups.actionPopup());
        });
    });

    describe('Действия с папкой', () => {
        afterEach(async function() {
            const items = this.currentTest.ctx.items || [];
            if (items.length) {
                await this.browser.url(consts.NAVIGATION.disk.url);
                await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
            }
        });

        it('diskclient-3411, diskclient-931: Перемещение папки из контекстного меню по клику на наименование папки', async function() {
            const workFolderName = `tmp-${Date.now()}-work-folder`;
            const folderNameForMoving = `tmp-${Date.now()}-first-folder`;
            const folderNameToBeMoved = `tmp-${Date.now()}-test-folder`;
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();

            this.currentTest.ctx.items = [workFolderName, folderNameForMoving, folderNameToBeMoved];

            await bro.yaClientLoginFast('yndx-ufo-test-192');
            await bro.yaCreateFolder(workFolderName);
            await bro.yaGoToFolderAndWaitForListingSpinnerHide(workFolderName);
            await bro.yaCreateFolders([folderNameForMoving, folderNameToBeMoved], null);
            await bro.yaGoToFolderAndWaitForListingSpinnerHide(folderNameToBeMoved);

            if (isMobile) {
                await bro.yaWaitForVisible(listing.listing.head.header());
                await bro.click(listing.listing.head.header());
                await bro.yaWaitForVisible(popups.actionPopup.moveButton());
                await bro.click(popups.actionPopup.moveButton());
            } else {
                await bro.yaWaitForVisible(listing.listing.head.actionButton());
                await bro.click(listing.listing.head.actionButton());
                await bro.yaWaitForVisible(popups.actionPopup.moveButton());
                await bro.click(popups.actionPopup.moveButton());
            }

            await bro.yaSelectFolderInDialogAndApply(workFolderName, folderNameForMoving);

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource({
                name: folderNameToBeMoved,
                folder: folderNameForMoving
            }, consts.TEXT_NOTIFICATION_FOLDER_MOVED_TO_FOLDER);

            await bro.url(`/client/disk/${workFolderName}/${folderNameForMoving}`);
            await bro.yaWaitForHidden(listing.listingSpinner());
            await bro.yaAssertListingHas(folderNameToBeMoved);
        });

        it('diskclient-6329, diskclient-6330: Переименование текущей папки', async function() {
            const bro = this.browser;

            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-6330' : 'diskclient-6329';

            const workFolderName = `tmp-${Date.now()}-work-folder`;
            const folderNameForRenaming = `tmp-${Date.now()}-folder`;
            const subfolderName = `tmp-${Date.now()}-subfolder`;
            const newFolderName = await bro.yaGetUniqResourceName();

            this.currentTest.ctx.items = [workFolderName];

            await bro.yaClientLoginFast('yndx-ufo-test-192');
            await bro.yaCreateFolder(workFolderName);
            await bro.yaGoToFolderAndWaitForListingSpinnerHide(workFolderName);
            await bro.yaCreateFolders([folderNameForRenaming], null);
            await bro.yaGoToFolderAndWaitForListingSpinnerHide(folderNameForRenaming);
            await bro.yaCreateFolders([subfolderName], null);

            if (isMobile) {
                await bro.yaWaitForVisible(listing.listing.head.header());
                await bro.click(listing.listing.head.header());
                await bro.yaWaitForVisible(popups.actionPopup.renameButton());
                await bro.click(popups.actionPopup.renameButton());
            } else {
                await bro.yaWaitForVisible(listing.listing.head.actionButton());
                await bro.click(listing.listing.head.actionButton());
                await bro.yaWaitForVisible(popups.actionPopup.renameButton());
                await bro.click(popups.actionPopup.renameButton());
            }

            await bro.yaSetResourceNameAndApply(newFolderName);
            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitForVisible(listing.listing.head.title().replace(':title', newFolderName));

            await bro.yaGoToFolderAndWaitForListingSpinnerHide(subfolderName);
            await bro.yaWaitForHidden(listing.listingBody.items());

            await bro.url(`/client/disk/${workFolderName}`);
            await bro.yaWaitForHidden(listing.listingSpinner());
            await bro.yaAssertListingHas(newFolderName);
        });

        it('diskclient-932, 5187: Удаление текущей папки из контекстного меню', async function() {
            this.currentTest.ctx.items = [];
            const prefix = `tmp-${Date.now()}-`;
            const folderNameForDeleting = prefix + 'work-folder';
            this.currentTest.ctx.items.push({ fileNames: folderNameForDeleting });
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            await bro.yaClientLoginFast('yndx-ufo-test-227');

            await bro.yaCreateFolder(folderNameForDeleting);
            await bro.yaOpenListingElement(folderNameForDeleting);

            if (isMobile) {
                await bro.yaWaitForVisible(listing.listing.head.header());
                await bro.click(listing.listing.head.header());
            } else {
                await bro.yaWaitForVisible(listing.listing.head.actionButton());
                await bro.click(listing.listing.head.actionButton());
            }

            await bro.pause(300);
            await bro.yaWaitForVisible(popups.actionPopup());
            await bro.click(popups.actionPopup.deleteButton());

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                folderNameForDeleting,
                consts.TEXT_NOTIFICATION_FOLDER_MOVED_TO_TRASH
            );

            await bro.yaAssertFolderOpened('Файлы');
            await bro.yaAssertListingHasNot(folderNameForDeleting);
        });

        hermione.auth.createAndLogin();
        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6331: Переименование файла через КМ с активным топбаром', async function() {
            const bro = this.browser;
            await bro.yaSkipWelcomePopup();

            this.testpalmId = 'diskclient-6331';

            const workFolderName = `tmp-${Date.now()}-work-folder`;
            const newFolderName = workFolderName + '_rename';

            this.currentTest.ctx.items = [workFolderName];

            await bro.yaCreateFolder(workFolderName);
            await bro.yaSelectResource(workFolderName);

            await bro.rightClick(listing.listing.item(workFolderName));
            await bro.yaWaitForVisible(popups.actionPopup.renameButton());
            await bro.click(popups.actionPopup.renameButton());

            await bro.yaSetResourceNameAndApply(newFolderName);
            await bro.yaAssertProgressBarAppeared();
            await bro.yaAssertListingHas(newFolderName);
        });
    });
});
