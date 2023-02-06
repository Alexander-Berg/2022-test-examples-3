const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const {
    common: {
        selectFolderPopup,
        selectFolderPopupListingItemInfoXpath
    }
} = require('../page-objects/client-popups');
const { assert } = require('chai');

const TEST_FILE_NAME = 'Горы.jpg';
const TEST_ID = '?test-id=217036 ';

hermione.skip.notIn('', 'судьба диалога решается – https://st.yandex-team.ru/CHEMODAN-70510');
describe('Диалог выбора папки -> ', () => {
    it('diskclient-6156, 6157: Диалог выбора папки в корне', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-6157';

        await bro.yaClientLoginFast('yndx-ufo-test-202');
        await bro.url(`/client/disk${TEST_ID}`);

        await bro.yaSelectResource(TEST_FILE_NAME);
        await bro.yaCallActionInActionBar('move');
        await bro.yaWaitForVisible(selectFolderPopup());
        await bro.pause(200); // animation-duration: .2s;

        await bro.yaSetModalDisplay(selectFolderPopup());
        await bro.yaAssertView(this.testpalmId, selectFolderPopup.content());
    });

    it('diskclient-6155, 865: Диалог выбора папки в папке', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-865';

        await bro.yaClientLoginFast('yndx-ufo-test-202');
        await bro.url(`/client/disk${TEST_ID}`);

        await bro.yaSelectResource(TEST_FILE_NAME);
        await bro.yaCallActionInActionBar('move');
        await bro.yaWaitForVisible(selectFolderPopup());
        await bro.yaWaitForHidden(selectFolderPopup.listing.spin());

        await bro.click(selectFolderPopupListingItemInfoXpath().replace(/:titleText/g, 'Папка какая-то'));
        await bro.yaWaitForHidden(selectFolderPopup.listing.spin());

        await bro.yaSetModalDisplay(selectFolderPopup());
        await bro.yaAssertView(this.testpalmId, selectFolderPopup.content());
    });

    it('diskclient-6153, 6152: Сброс скролла в диалоге выбора папки при переходе по папкам', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-204');
        await bro.url(`/client/disk${TEST_ID}`);

        await bro.yaSelectResource(TEST_FILE_NAME);
        await bro.yaCallActionInActionBar('move');
        await bro.yaWaitForVisible(selectFolderPopup());
        await bro.yaWaitForHidden(selectFolderPopup.listing.spin());

        const targetFolderSelector = selectFolderPopupListingItemInfoXpath().replace(/:titleText/g, 'target-folder');
        await bro.yaNativeScrollIntoView(targetFolderSelector);
        await bro.click(targetFolderSelector);

        // при первом заходе скролл всегда будет на верху, потому что папка ещё не загружена
        await bro.yaWaitForHidden(selectFolderPopup.listing.spin());

        await bro.click(selectFolderPopup.listing.currentItem());
        await bro.yaWaitForVisible(targetFolderSelector);
        await bro.yaNativeScrollIntoView(targetFolderSelector);
        await bro.click(targetFolderSelector);

        const inViewport = await bro.isVisibleWithinViewport(selectFolderPopup.listing.currentItem());
        assert(inViewport === true, 'Первый элемент в листинге не виден');
    });
});

hermione.skip.notIn('', 'судьба диалога решается – https://st.yandex-team.ru/CHEMODAN-70510');
describe('Диалог создания папки с выбором папки -> ', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-202');
        await bro.url(`/client/photo${TEST_ID}`);
    });

    hermione.only.in(clientDesktopBrowsersList, 'Диалог создания папки с выбором папки есть только на десктопах');
    it('diskclient-6149: Диалог создания папки с выбором', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-6149';

        await bro.yaOpenCreateDirectoryDialog(false);
        await bro.yaWaitForHidden(selectFolderPopup.listing.spin());

        await bro.yaSetModalDisplay(selectFolderPopup());
        await bro.yaAssertView(this.testpalmId, selectFolderPopup.content());
    });

    hermione.only.in(clientDesktopBrowsersList, 'Диалог создания папки с выбором папки есть только на десктопах');
    it('diskclient-6148: Диалог создания папки с выбором, внутри ReadOnly папки', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-6148';

        await bro.yaOpenCreateDirectoryDialog(false);
        await bro.yaWaitForHidden(selectFolderPopup.listing.spin());

        await bro.click(selectFolderPopupListingItemInfoXpath().replace(/:titleText/g, 'Общая'));
        await bro.yaWaitForHidden(selectFolderPopup.listing.spin());

        await bro.yaSetModalDisplay(selectFolderPopup());
        await bro.yaAssertView(this.testpalmId, selectFolderPopup.content());
    });
});
