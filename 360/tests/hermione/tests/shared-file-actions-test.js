const popups = require('../page-objects/client-popups');
const listing = require('../page-objects/client-content-listing').common;
const navigation = require('../page-objects/client-navigation');
const PageObjects = require('../page-objects/public');
const consts = require('../config').consts;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 * @typedef { import('@ps-int/ufo-hermione/types').Resource } Resource
 */

/**
 * @param {string} touchUser
 * @param {string} desktopUser
 * @param {Browser} bro
 * @param {Object} options
 *
 * @returns {Promise.<string>}
 */
async function loginAndUploadFile(touchUser, desktopUser, bro, options) {
    const user = await bro.yaIsMobile() ? touchUser : desktopUser;
    await bro.yaClientLoginFast(user);
    return bro.yaUploadFiles('test-file.txt', { uniq: true, ...options });
}

/**
 * @returns {Promise<void>}
 */
async function cleanFiles() {
    const listingResources = this.currentTest.ctx.listingResources || [];

    if (listingResources.length) {
        const bro = this.browser;
        await bro.url(consts.NAVIGATION.disk.url);
        await bro.yaDeleteCompletely(listingResources, { safe: true, fast: true });
    }
}

//  Часть тестов падало, т.к. один тест может выполняться параллельно в разных браузерах,
//  поэтому решили вообще оторвать десктопный хром в режиме эмуляции,
//  но есть кусок тестов, которые пока падают в Андроиде CHEMODAN-57678,
//  и chrome-phone нельзя совсем оторвать, но можно заигнорить на тестах, которые не падают в Андроид.
hermione.only.notIn('chrome-phone');
describe('Публикация файлов -> ', () => {
    afterEach(cleanFiles);

    hermione.skip.in('chrome-phone-6.0', 'Сломанный тест https://st.yandex-team.ru/CHEMODAN-68670');
    it('diskclient-1522, 726: Смоук: Удаление публичной ссылки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1521' : 'diskclient-642';

        const fileName = await loginAndUploadFile('yndx-ufo-test-74', 'yndx-ufo-test-24', bro);
        this.currentTest.ctx.listingResources = [fileName];

        await bro.yaSelectResource(fileName);
        const publicUrl = await bro.yaShareSelected();

        await bro.yaGetPublicUrlAndCloseTab();
        await bro.yaDeletePublicLinkInShareDialog();

        await bro.newWindow(publicUrl);
        await bro.yaWaitForVisible(PageObjects.error());
        await bro.close();
    });

    it('diskclient-5054, 876: Поделение заблокированным файлом из топбара', async function() {
        const blockedFileName = '2019-10-31 15-06-43.jpeg';
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5054' : 'diskclient-876';
        await bro.yaClientLoginFast('yndx-ufo-test-187');

        await bro.yaSelectResource(blockedFileName);
        if (isMobile) {
            await bro.yaExecuteClick(navigation.touch.modalCell());
            await bro.pause(500); //ждем пока скроется инфопопап
        }
        await bro.yaWaitActionBarDisplayed();
        await bro.click(isMobile ? popups.touch.actionBar.publishButton() : popups.desktop.actionBar.publishButton());
        await bro.yaWaitNotificationForResource(blockedFileName, consts.TEXT_NOTIFICATION_PUBLISH_ERROR);
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-898: Клик по иконке публичности', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-898';
        await bro.yaClientLoginFast('yndx-ufo-test-177');

        await bro.yaWaitForVisible(listing.listing());
        await bro.click(listing.listingBody.items.publicIconButton());
        await bro.yaWaitForVisible(popups.common.shareDialog.textInput());
        await bro.yaWaitActionBarDisplayed();
    });
});

describe('Публикация файлов после загрузки и удаление публичных файлов -> ', () => {
    afterEach(cleanFiles);

    it('diskclient-1187, 724: Удаление файла с публичной ссылкой', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1187' : 'diskclient-724';

        const fileName = await loginAndUploadFile('yndx-ufo-test-77', 'yndx-ufo-test-30', bro);
        this.currentTest.ctx.listingResources = [fileName];

        await bro.yaSelectResource(fileName);
        const publicUrl = await bro.yaShareSelected();
        await bro.yaGetPublicUrlAndCloseTab();
        await bro.keys('Escape'); // закрытие поделяшки
        await bro.yaWaitForHidden(popups.common.shareDialog());

        await bro.yaAssertListingHas(fileName);
        await bro.yaDeleteCompletely(fileName);
        this.currentTest.ctx.listingResources = [];

        await bro.newWindow(publicUrl);
        await bro.yaWaitForVisible(PageObjects.error());
        await bro.close();
    });

    it('diskclient-4799, 4738: Поделение ресурсом из загрузчика по кнопке', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4799' : 'diskclient-4738';

        const fileName = await loginAndUploadFile(
            'yndx-ufo-test-181',
            'yndx-ufo-test-180',
            bro,
            { closeUploader: false }
        );
        this.currentTest.ctx.listingResources = [fileName];

        const publishButtonSelector = isMobile ?
            popups.touch.uploader.publishButton() :
            popups.desktop.uploader.publishButton();

        await bro.yaWaitForVisible(publishButtonSelector);
        await bro.click(publishButtonSelector);
        await bro.yaWaitForVisible(popups.common.shareDialog());
        const publicUrl = await bro.getValue(popups.common.shareDialog.textInput());

        await bro.newWindow(publicUrl);
        await bro.yaWaitForVisible(PageObjects.publicMain());
        await bro.yaWaitForHidden(PageObjects.error());
        await bro.close();
    });

    it('diskclient-1188, 725: Восстановление файла с публичной ссылкой из Корзины', async function() {
        const bro = this.browser;
        bro.executionContext.timeout(100000);
        const isMobile = await bro.yaIsMobile();
        const env = isMobile ? 'touch' : 'desktop';
        this.testpalmId = isMobile ? 'diskclient-1188' : 'diskclient-725';

        const fileName = await loginAndUploadFile('yndx-ufo-test-142', 'yndx-ufo-test-141', bro);
        this.currentTest.ctx.listingResources = [fileName];

        await bro.yaSelectResource(fileName);
        const publicUrl = await bro.yaShareSelected();
        await bro.yaGetPublicUrlAndCloseTab();
        await bro.keys('Escape');

        await bro.yaSelectResource(fileName);
        await bro.yaDeleteSelected();
        await bro.yaAssertListingHasNot(fileName);
        await bro.yaWaitNotificationForResource(fileName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
        await bro.yaWaitActionBarHidden();

        await bro.yaOpenSection('trash');
        await bro.yaSelectResource(fileName);
        await bro.yaWaitActionBarDisplayed();
        await bro.click(popups[env].actionBar.restoreFromTrashButton());
        await bro.yaWaitNotificationForResource(fileName, consts.TEXT_NOTIFICATION_FILE_RESTORE);

        await bro.url(consts.NAVIGATION.disk.url);
        await bro.yaAssertListingHas(fileName);

        await bro.newWindow(publicUrl);
        await bro.yaWaitForVisible(PageObjects.publicMain(), 'Не дождались появления ресурса');
        await bro.yaWaitForHidden(PageObjects.error(), 'Увидели сообщение об ошибке');
        await bro.close();

        await bro.yaDeleteCompletely(fileName);
        this.currentTest.ctx.listingResources = [];
        await bro.yaAssertListingHasNot(fileName);
    });
});
