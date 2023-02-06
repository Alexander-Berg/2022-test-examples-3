const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const clientPopups = require('../page-objects/client-popups');
const clientContentListing = require('../page-objects/client-content-listing');
const albums = require('../page-objects/client-albums-page');
const { consts } = require('../config');

const FOLDER_NAME = 'TestFolder';
const DOCUMENT_NAME = 'TestDocument.docx';
const FILE_NAME = 'TestFile.jpg';

const SHARED_FOLDER_NAME = 'TestFolderShared';
const SHARED_FILE_NAME = 'TestFileShared.jpg';
const SHARED_DOCUMENT_NAME = 'TestDocumentShared.docx';

const shareDialogSelector = (isMobile) => isMobile ?
    clientPopups.touch.shareDialog() :
    clientPopups.desktop.shareDialog();

const publicIconSelector = (isMobile) => isMobile ?
    clientContentListing.touch.listingItemRowPublicLinkIcon() :
    clientContentListing.desktop.listingItemPublicLinkIcon();

describe('Общий диалог поделения -> ', () => {
    beforeEach(async function() {
        await this.browser.yaSkipWelcomePopup();
    });
    afterEach(async function() {
        const bro = this.browser;
        const items = this.currentTest.ctx.listingResources || [];
        if (items.length) {
            await bro.url(consts.NAVIGATION.disk.url);
            await bro.yaDeleteCompletely(items, { safe: true, fast: true });
        }
    });

    /**
     * @param {string} resourceName
     * @param {boolean} [byContextMenu]
     */
    async function publishAndOpenShareDialog(resourceName, byContextMenu = false) {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.yaSelectResource(resourceName);

        if (byContextMenu) {
            await bro.yaOpenActionPopup(resourceName);
            await bro.click(clientPopups.common.actionPopup.publishButton());
        } else {
            await bro.yaSelectResource(resourceName);
            await bro.yaCallActionInActionBar('publish');
        }

        await bro.yaWaitForVisible(shareDialogSelector(isMobile));
        await bro.pause(500);
    }

    /**
     * @param {boolean} [byButton]
     */
    async function closeShareDialog(byButton = true) {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        if (byButton) {
            if (isMobile) {
                // для тача - вместо клика на крестик - свайп вниз
                await bro.swipeDown(shareDialogSelector(isMobile), 400);
            } else {
                await bro.click(clientPopups.common.shareDialog.closeButton());
                await bro.yaWaitForHidden(clientPopups.common.confirmDialog());
            }
        } else {
            if (isMobile) {
                await bro.yaTapOnScreen(1, 1);
            } else {
                await bro.keys('Escape');
            }
        }

        await bro.yaWaitForHidden(clientPopups.common.confirmDialog());
    }

    /**
     * @param {string} testpalmId
     * @param {string} resourceName
     * @param {boolean} [withScreen]
     * @param {boolean} [publishByContextMenu]
     */
    async function testPublish(testpalmId, resourceName, withScreen = true, publishByContextMenu = false) {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = testpalmId;

        const workFolderName = 'tmp-' + Date.now();
        this.currentTest.ctx.listingResources = [workFolderName];
        await bro.yaCreateFolder(workFolderName);

        await bro.yaSelectResource(resourceName);
        await bro.yaCopySelected(workFolderName);
        await bro.yaAssertProgressBarDisappeared();
        await bro.yaGoToFolderAndWaitForListingSpinnerHide(workFolderName);

        await publishAndOpenShareDialog.call(this, resourceName, publishByContextMenu);

        if (withScreen) {
            await bro.yaAssertView(testpalmId, shareDialogSelector(isMobile), {
                ignoreElements: clientPopups.common.shareDialog.textInput()
            });
        }

        await bro.yaWaitForVisible(publicIconSelector(isMobile));
    }

    /**
     * @param {string} testpalmId
     * @param {string} [resourceName]
     */
    async function testInfo(testpalmId, resourceName = '') {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = testpalmId;

        // если имени ресурса нет - значит тест для альбома и диалог шаринга уже открыт
        if (resourceName) {
            await publishAndOpenShareDialog.call(this, resourceName);
        }

        if (!isMobile) {
            await bro.moveToObject(clientPopups.common.shareDialog.onlyViewInfoIcon());
            await bro.pause(1000);
            await bro.yaWaitForVisible(clientPopups.common.messageBox());
            await bro.pause(500);

            await bro.yaAssertView(testpalmId, [
                clientPopups.common.shareDialog.onlyViewInfoIcon(),
                clientPopups.common.messageBox(),
            ]);
        } else {
            await bro.pause(1000);
            await bro.click(clientPopups.common.shareDialog.onlyViewInfoIcon());
            await bro.yaWaitForVisible(clientPopups.common.confirmDialog());

            await bro.pause(500);
            await bro.yaAssertView(testpalmId, clientPopups.common.confirmDialog());
        }
    }

    hermione.auth.tus({ tags: 'diskclient-share-dialog', lock_duration: '40' });
    it('diskclient-6346, diskclient-6409: Опубликование папки', async function () {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-6409' : 'diskclient-6346';

        await testPublish.call(this, testpalmId, FOLDER_NAME);
    });

    hermione.auth.tus({ tags: 'diskclient-share-dialog', lock_duration: '40' });
    it('diskclient-6347, diskclient-6410: Опубликование файла', async function () {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-6410' : 'diskclient-6347';

        await testPublish.call(this, testpalmId, FILE_NAME);
    });

    hermione.auth.tus({ tags: 'diskclient-share-dialog', lock_duration: '40' });
    it('diskclient-6349, diskclient-6412: Опубликование документа', async function () {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-6412' : 'diskclient-6349';

        await testPublish.call(this, testpalmId, DOCUMENT_NAME);
    });

    it('diskclient-6350, diskclient-6397: [Попап поделения] Копирование ссылки по кнопке', async function () {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-557');

        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-6397' : 'diskclient-6350';
        this.testpalmId = testpalmId;

        await publishAndOpenShareDialog.call(this, SHARED_DOCUMENT_NAME);

        await bro.click(clientPopups.common.shareDialog.copyButton());
        await bro.pause(1000);
        await bro.yaAssertView(testpalmId, clientPopups.common.shareDialog.copyButton());
    });

    hermione.auth.tus({ tags: 'diskclient-share-dialog', lock_duration: '40' });
    it('diskclient-6352, diskclient-6399: [Попап поделения] Удаление публичной ссылки на документ', async function () {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-6397' : 'diskclient-6352';

        await testPublish.call(this, testpalmId, DOCUMENT_NAME, false);

        await bro.click(clientPopups.common.shareDialog.trashButton());
        await bro.yaWaitForVisible(clientPopups.common.confirmDialog());
        await bro.pause(500);
        await bro.click(clientPopups.common.confirmDialog[isMobile ? 'closeButton' : 'cancelButton']());
        await bro.yaWaitForHidden(clientPopups.common.confirmDialog());

        await bro.yaWaitForVisible(publicIconSelector(isMobile));

        await publishAndOpenShareDialog.call(this, DOCUMENT_NAME);

        await bro.click(clientPopups.common.shareDialog.trashButton());
        await bro.yaWaitForVisible(clientPopups.common.confirmDialog());
        await bro.pause(500);

        await bro.yaAssertView(`${testpalmId}-1`, clientPopups.common.confirmDialog());

        await bro.click(clientPopups.common.confirmDialog.submitButton());
        await bro.yaWaitForHidden(clientPopups.common.confirmDialog());

        await bro.yaWaitForHidden(publicIconSelector(isMobile));
    });

    hermione.skip.in('chrome-phone', 'https://st.yandex-team.ru/CHEMODAN-84271'); // не работают свайпы
    it('diskclient-6366, diskclient-6401: [Попап поделения] Закрытие диалога', async function () {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-557');

        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6401' : 'diskclient-6366';

        await publishAndOpenShareDialog.call(this, SHARED_DOCUMENT_NAME);
        await closeShareDialog.call(this);

        await publishAndOpenShareDialog.call(this, SHARED_DOCUMENT_NAME);
        await closeShareDialog.call(this, false);
    });

    hermione.skip.in('chrome-phone', 'https://st.yandex-team.ru/CHEMODAN-84272'); // мигает
    hermione.auth.tus({ tags: 'diskclient-share-dialog', lock_duration: '40' });
    it('diskclient-6351, diskclient-6406: [Попап поделения] Переключение типа доступа с просмотра на редактирование', async function () {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-6406' : 'diskclient-6351';
        this.testpalmId = testpalmId;

        await testPublish.call(this, testpalmId, DOCUMENT_NAME, false);

        await bro.click(clientPopups.common.shareDialog.radioBoxNotChecked());
        await bro.yaAssertView(testpalmId, shareDialogSelector(isMobile), {
            ignoreElements: clientPopups.common.shareDialog.textInput()
        });
    });

    hermione.auth.tus({ tags: 'diskclient-share-dialog', lock_duration: '40' });
    it('diskclient-6386, diskclient-6402: [Попап поделения] Переключение типа доступа с редактирования на просмотр', async function () {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-6402' : 'diskclient-6386';
        this.testpalmId = testpalmId;

        await testPublish.call(this, testpalmId, DOCUMENT_NAME, false);
        await bro.click(clientPopups.common.shareDialog.radioBoxNotChecked());
        await closeShareDialog.call(this, false);

        await publishAndOpenShareDialog.call(this, DOCUMENT_NAME);
        await bro.click(clientPopups.common.shareDialog.accessTypeButton());
        await bro.click(clientPopups.common.shareDialog.radioBoxNotChecked());
        await bro.pause(1000);
        await bro.yaAssertView(testpalmId, shareDialogSelector(isMobile), {
            ignoreElements: clientPopups.common.shareDialog.textInput()
        });
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83408');
    it('diskclient-6354, diskclient-6403: [Попап поделения] Информация о типе доступа к папке', async function () {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-557');
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-6403' : 'diskclient-6354';

        await testInfo.call(this, testpalmId, SHARED_FOLDER_NAME);
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83408');
    it('diskclient-6384, diskclient-6404: [Попап поделения] Информация о типе доступа к файлу', async function () {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-557');
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-6404' : 'diskclient-6384';

        await testInfo.call(this, testpalmId, SHARED_FILE_NAME);
    });

    hermione.skip.in(clientDesktopBrowsersList, 'https://st.yandex-team.ru/CHEMODAN-84034');
    it('diskclient-6385, diskclient-6405: [Попап поделения] Информация о типе доступа к альбому', async function () {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-558');
        await bro.url('/client/disk');
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-6405' : 'diskclient-6385';

        await bro.url('/client/albums/605deb1d4c9a04b36efc6fe2');

        await bro.yaWaitForVisible(albums.album2.publishButton());
        await bro.click(albums.album2.publishButton());

        await testInfo.call(this, testpalmId);
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-6429: [Попап поделения] QR-код', async function () {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-557');

        const testpalmId = 'diskclient-6429';
        this.testpalmId = testpalmId;

        await publishAndOpenShareDialog.call(this, SHARED_FILE_NAME);

        // клик по qr-коду
        await bro.click(clientPopups.desktop.shareDialog.lastButton());
        await bro.pause(500);
        await bro.yaAssertView(
            testpalmId,
            shareDialogSelector(false),
            { ignoreElements: [clientPopups.common.shareDialog.qrImage()] }
        );
    });

    hermione.auth.tus({ tags: 'diskclient-share-dialog', lock_duration: '40' });
    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-6388: Опубликование файла из КМ', async function () {
        const testpalmId = 'diskclient-6388';
        await testPublish.call(this, testpalmId, FILE_NAME, true, true);
    });
});
