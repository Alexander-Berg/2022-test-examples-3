const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { TEST_FOLDER_NAME } = require('../config').consts;
const popups = require('../page-objects/client-popups');
const listing = require('../page-objects/client-content-listing').common;
const consts = require('../config').consts;
const { NAVIGATION } = require('../config').consts;

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');

describe('Контекстное меню создания ресурса -> ', () => {
    beforeEach(async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-13');
        await bro.url(NAVIGATION.disk.url);
        await bro.yaDeleteAllResources();
    });

    /**
     * @param {Browser} bro
     */
    async function openContextMenu(bro) {
        await bro.rightClick(listing.listing());
        await bro.yaWaitForVisible(popups.desktop.contextMenuCreatePopup());
    }

    it('diskclient-1505: Cоздание папки c помощью контекстного меню', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1505';

        await openContextMenu(bro);
        await bro.click(popups.desktop.contextMenuCreatePopup.createDirectory());
        await bro.yaSetResourceNameAndApply(TEST_FOLDER_NAME);
        await bro.yaWaitNotificationForResource(TEST_FOLDER_NAME, consts.TEXT_NOTIFICATION_FOLDER_CREATED);
        await bro.yaAssertListingHas(TEST_FOLDER_NAME);
        await bro.yaWaitActionBarDisplayed();
    });

    it('diskclient-1601: assertView: Пункты контекстного меню создания ресурса', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1601';

        await openContextMenu(bro);
        await bro.assertView('diskclient-1601', popups.desktop.contextMenuCreatePopup());
    });
});

const navigation = require('../page-objects/client-navigation').desktop;
hermione.skip.notIn('', 'Сильно мигающий тест https://st.yandex-team.ru/CHEMODAN-62037');
describe('Cозданиe ресурса по кнопке + -> ', () => {
    hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
    it('diskclient-582: Смоук: Создание таблицы', async function() {
        const bro = this.browser;
        const newTableUrl = '/edit/disk/disk/Таблица.xlsx';
        const newTableName = 'Таблица.xlsx';
        this.testpalmId = 'diskclient-582';
        await bro.yaClientLoginFast('yndx-ufo-test-65');
        await bro.url(NAVIGATION.disk.url);
        await bro.yaDeleteAllResources();
        await bro.click(navigation.sidebarButtons.create());
        await bro.yaClickAndAssertNewTabUrl(popups.desktop.createPopup.createTable(), {
            linkShouldContain: newTableUrl,
            message: 'Страница создания таблицы не открылась'
        },
        6000);
        await bro.close();
        await bro.yaAssertListingHas(newTableName);
        await bro.yaCleanTrash();
    });
});
