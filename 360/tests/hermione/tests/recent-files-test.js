const { NAVIGATION } = require('../config').consts;
const page = require('../page-objects/client-content-listing').common;
const popups = require('../page-objects/client-popups');
const listing = require('../page-objects/client-content-listing').common;
const navigation = require('../page-objects/client-navigation');
const PageObjects = require('../page-objects/public');
const { docs } = require('../config');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 * @typedef { import('@ps-int/ufo-hermione/types').Resource } Resource
 */

const listingItem = (n) => `${page.listingBody.items()}:nth-of-type(${n})`;

const doTest = ({ type, name, touchId, desktopId, assertUrlHas }) => {
    describe(`Последние файлы ${type} ->`, () => {
        it(`diskclient-${touchId}, ${desktopId}: Смоук: Открытие в DV`, async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? `diskclient-${touchId}` : `diskclient-${desktopId}`;

            await bro.yaClientLoginFast('yndx-ufo-test-09');
            await bro.yaOpenSection('recent');
            await bro.yaAssertListingHas(name);
            await bro.yaOpenLinkInNewTab(page.listingBodyItemsInfoXpath().replace(/:titleText/g, name), {
                doDoubleClick: true,
                assertUrlHas: type === 'pdf' && isMobile ? 'docviewer' : assertUrlHas
            });
        });
    });
};

docs.forEach(doTest);

describe('Последние файлы ->', () => {
    it('diskclient-1434, 1600: Смоук: Подгрузка порций в последних файлах', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1434' : 'diskclient-1600';

        await bro.yaClientLoginFast('yndx-ufo-test-10');
        await bro.url(NAVIGATION.recent.url);
        await bro.yaWaitForHidden(page.listingSpinner());
        await bro.yaScrollToEnd();
        await bro.yaWaitForVisible(listingItem(229));
    });
});

describe('Поделение в Последних файлах ->', () => {
    afterEach(async function() {
        const listingResources = this.currentTest.ctx.listingResources || [];

        if (listingResources.length) {
            const bro = this.browser;
            await bro.yaOpenSection('recent');
            await bro.yaDeleteCompletely(listingResources, { safe: true, fast: true });
        }
    });
    /**
     * @param {string} touchUser
     * @param {string} desktopUser
     * @param {Browser} bro
     * @returns {Promise<string|string[]>} filenames
     */
    async function loginAndUploadFiles(touchUser, desktopUser, bro) {
        const user = await bro.yaIsMobile() ? touchUser : desktopUser;
        await bro.yaClientLoginFast(user);
        await bro.url(NAVIGATION.disk.url);
        return bro.yaUploadFiles('test-file.txt', { uniq: true });
    }

    it('diskclient-5060, 671: assertView: Создание публичной ссылки на файл из раздела Последние', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-5060' : 'diskclient-671';
        this.testpalmId = testpalmId;

        const testFile = await loginAndUploadFiles('yndx-ufo-test-189', 'yndx-ufo-test-188', bro);
        this.currentTest.ctx.listingResources = [testFile];

        await bro.yaWaitForVisible(listing.listingBody());
        await bro.yaOpenSection('recent');
        await bro.yaSelectResource(testFile);
        await bro.yaWaitActionBarDisplayed();
        await bro.click(isMobile ? popups.touch.actionBar.publishButton() : popups.desktop.actionBar.publishButton());
        await bro.yaWaitForVisible(popups.common.shareDialog());

        if (isMobile) {
            await bro.yaExecuteClick(navigation.touch.modalCell());
            await bro.pause(500); //ждем пока скроется попап поделения
        } else {
            await bro.click(popups.common.shareDialog.closeButton());
        }
    });

    it('diskclient-5037, 673: Удаление публичной ссылки на файл из раздела Последние', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5037' : 'diskclient-673';

        const testFile = await loginAndUploadFiles('yndx-ufo-test-183', 'yndx-ufo-test-182', bro);
        this.currentTest.ctx.listingResources = [testFile];

        await bro.yaWaitForVisible(listing.listingBody());
        await bro.yaOpenSection('recent');
        await bro.yaSelectResource(testFile);
        await bro.yaWaitActionBarDisplayed();
        await bro.click(isMobile ? popups.touch.actionBar.publishButton() : popups.desktop.actionBar.publishButton());
        await bro.yaWaitForVisible(popups.common.shareDialog());

        await bro.yaGetPublicUrlAndCloseTab();
        const publicUrl = await bro.getValue(popups.common.shareDialog.textInput());
        await bro.yaDeletePublicLinkInShareDialog();

        await bro.yaDeleteCompletely(testFile);
        this.currentTest.ctx.listingResources = [];

        await bro.newWindow(publicUrl);
        await bro.yaWaitForVisible(PageObjects.error());
    });
});
