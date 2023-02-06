const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { TEST_FOLDER_NAME, NAVIGATION } = require('../config').consts;
const popups = require('../page-objects/client-popups');
const fileListing = require('../page-objects/client-content-listing').common;
const listing = require('../page-objects/client-content-listing').common;
const consts = require('../config').consts;
const PageObjects = require('../page-objects/public');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

describe('Действия с общими папками -> ', () => {
    hermione.skip.in('chrome-phone', 'https://st.yandex-team.ru/CHEMODAN-84274'); // мигает
    it('diskclient-1395, 1712: Смоук: Общие папки. Переименование', async function() {
        const bro = this.browser;
        const newFolderName = await bro.yaGetUniqResourceName();
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1712' : 'diskclient-1395';
        const user = isMobile ? 'yndx-ufo-test-58' : 'yndx-ufo-test-08';

        await bro.yaClientLoginFast(user);
        await bro.yaOpenSection('shared');

        await bro.yaWaitForVisible(fileListing.listing.firstFile());
        await (
            isMobile ?
                bro.yaLongPress(fileListing.listing.firstFile()) :
                bro.click(fileListing.listing.firstFile())
        );
        await bro.yaWaitActionBarDisplayed();
        if (isMobile) {
            await bro.click(popups.common.actionBar.moreButton());
            await bro.yaWaitForVisible(popups.common.actionBarMorePopup.renameButton());
            await bro.click(popups.common.actionBarMorePopup.renameButton());
        } else {
            await bro.click(popups.common.actionBar.renameButton());
        }
        await bro.yaSetResourceNameAndApply(newFolderName);
        await bro.yaAssertListingHas(newFolderName);
    });

    it('diskclient-1960, 815: Нельзя создать публичную ссылку на ОП (Только просмотр)', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1960' : 'diskclient-815';
        await bro.yaClientLoginFast('yndx-ufo-test-60');
        await bro.yaWaitForVisible(fileListing.listing.firstFile());
        await bro.yaSelectResource('ONLYREAD');
        await bro.yaWaitActionBarDisplayed();
        await bro.yaWaitForHidden(isMobile ?
            popups.touch.actionBar.publishButton() :
            popups.desktop.actionBar.publishButton()
        );
    });

    hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
    it('diskclient-3507: Открытие попапа общего доступа при переходе из ПО', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-59');
        await bro.url(NAVIGATION.disk.url + '|access-folder/disk/sfsdf');
        await bro.yaWaitForVisible(popups.common.accessPopup.suggestInput());
        await bro.yaWaitForHidden(popups.common.accessPopup.spinner());
    });
});

describe('Действия с общими папками -> ', () => {
    afterEach(async function() {
        const bro = this.browser;

        await bro.url(NAVIGATION.disk.url);
        await bro.yaDeleteResource(TEST_FOLDER_NAME);
    });

    hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
    it('diskclient-1499: Смоук: assertView: создание ОП в разделе Общий доступ', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-59');
        await bro.yaOpenSection('shared');
        await bro.yaWaitForVisible(fileListing.listing.createSharedFolderButton());
        await bro.click(fileListing.listing.createSharedFolderButton());
        await bro.yaWaitForVisible(popups.common.selectFolderDialog());
        await bro.yaSetValue(popups.common.selectFolderDialog.nameInput(), TEST_FOLDER_NAME);
        await bro.yaWaitForVisible(popups.common.selectFolderDialog.submitButton());
        await bro.click(popups.common.selectFolderDialog.submitButton());
        await bro.yaWaitForHidden(popups.common.selectFolderDialog(), 'Диалог создания общей папки не исчез');
        await bro.yaWaitForVisible(popups.common.accessPopup.suggestInput());
        await bro.yaWaitForHidden(popups.common.accessPopup.spinner());
        await bro.assertView('diskclient-1499', popups.common.accessPopup());
    });
});

describe('Публикация папок -> ', () => {
    afterEach(async function() {
        const bro = this.browser;
        const resource = this.currentTest.ctx.resource;
        if (resource) {
            await bro.yaDeleteCompletely(resource, { safe: true, fast: true });
        }
    });

    it('diskclient-5061, 5039: assertView: Создание публичной ссылки на папку', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-5061' : 'diskclient-5039';
        this.testpalmId = testpalmId;
        const folderName =
            await bro.yaLoginAndCreateFolder(isMobile ? 'yndx-ufo-test-191' : 'yndx-ufo-test-190');
        this.currentTest.ctx.resource = folderName;

        await bro.yaSelectResource(folderName);
        await bro.yaShareSelected();
        await bro.pause(500);
        await bro.assertView(
            testpalmId,
            isMobile ? popups.touch.shareDialog() : popups.desktop.shareDialog(),
            { ignoreElements: popups.common.shareDialog.textInput() });
        await bro.keys('Escape');
    });

    it('diskclient-5036, 896: Удаление публичной ссылки на папку', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5036' : 'diskclient-896';
        const folderName =
            await bro.yaLoginAndCreateFolder(isMobile ? 'yndx-ufo-test-179' : 'yndx-ufo-test-178');
        this.currentTest.ctx.resource = folderName;

        await bro.yaSelectResource(folderName);
        const publicUrl = await bro.yaShareSelected();
        await bro.yaGetPublicUrlAndCloseTab();
        await bro.yaDeletePublicLinkInShareDialog();

        await bro.newWindow(publicUrl);
        await bro.yaWaitForVisible(PageObjects.error());
        await bro.close();
        await bro.keys('Escape');
    });

    hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
    it('diskclient-736: Удаление публичной ссылки в разделе Общий доступ', async function() {
        const bro = this.browser;
        const folderName = await bro.yaLoginAndCreateFolder('yndx-ufo-test-222');
        this.currentTest.ctx.resource = folderName;

        await bro.yaSelectResource(folderName);
        await bro.yaShareSelected();
        await bro.url(NAVIGATION.shared.url);
        await bro.yaWaitForVisible(listing.listingBody.items.publicIconButton());
        await bro.click(listing.listingBody.items.publicIconButton());
        await bro.yaWaitForVisible(popups.common.shareDialog());
        await bro.yaDeletePublicLinkInShareDialog();
    });
});

//Данный тест пока постоянно падает в Андроиде, ждем CHEMODAN-57678
hermione.only.notIn('chrome-phone-6.0');
describe('Публикация папки переполненным пользователем -> ', () => {
    it('diskclient-2030, 2029: Создание публичной ссылки переполненным пользователем', async function() {
        const folderName = 'Не удалять';
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-2030' : 'diskclient-2029';
        await bro.yaClientLoginFast('yndx-ufo-test-184');

        await bro.yaWaitForVisible(listing.listingBody());
        await bro.yaSelectResource(folderName);
        await bro.yaWaitActionBarDisplayed();
        await bro.click(isMobile ? popups.touch.actionBar.publishButton() : popups.desktop.actionBar.publishButton());
        await bro.yaWaitNotificationForResource(folderName, consts.TEXT_NOTIFICATION_FOLDER_PUBLISH_NO_SPACE);
    });
});
