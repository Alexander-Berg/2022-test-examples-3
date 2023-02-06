const { TEST_FOLDER_NAME, NAVIGATION } = require('../config').consts;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const popups = require('../page-objects/client-popups');
const listing = require('../page-objects/client-content-listing').common;
const { assert } = require('chai');

describe('invite-folder', () => {
    // hermione.only.in(clientDesktopBrowsersList, 'Доступ настраивается только на десктопе');
    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-84265');
    it('diskclient-6158: Проверка http запроса приглашения в папку', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-276');
        await bro.url(NAVIGATION.disk.url);
        await bro.yaSelectResource(TEST_FOLDER_NAME);
        await bro.yaWaitActionBarDisplayed();
        await bro.click(popups.common.actionBar.moreButton());
        await bro.yaWaitForVisible(popups.desktop.actionBarInviteButton());
        await bro.click(popups.desktop.actionBarInviteButton());
        await bro.yaWaitForVisible(popups.desktop.invitePopup());
        // Рандомный несуществующий адрес
        const email = 'yndx-05ce3c6f8b2b693087gg56f36744f800142ad409130f8a91e9594@yandex.ru';
        await bro.yaSetValue(popups.desktop.invitePopup.emailInput(), email);
        await bro.yaWaitForVisible(popups.desktop.invitePopup.inviteByEmailButton());
        await bro.setupInterceptor();
        await bro.click(popups.desktop.invitePopup.inviteByEmailButton());
        await bro.pause(10000);

        const requests = await bro.getRequest();
        const inviteFolderRequest = requests.find((request) => request.url.includes('do-invite-folder'));

        // Иногда в 1 запросе 2 модели (/models/?_m=do-status-operation,do-invite-folder) - смотрим do-invite-folder
        const {
            params: {
                path, rights, userid, service, locale
            }
        } = inviteFolderRequest.response.body.models.find((mdl) => mdl.model === 'do-invite-folder');

        assert.equal(rights, 660);
        assert.equal(userid, email);
        assert.equal(service, 'email');
        assert.equal(locale, 'ru');
        assert.equal(path, `/disk/${TEST_FOLDER_NAME}`);
    });

    it('diskclient-615, 1194: Принятие приглашения в общую папку', async function() {
        const bro = this.browser;
        bro.executionContext.timeout(120000);
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1194' : 'diskclient-615';
        const mobileUser = 'yndx-ufo-test-383';
        const desktopUser = 'yndx-ufo-test-382';
        const mobileGuest = 'yndx-ufo-test-390';
        const desktopGuest = 'yndx-ufo-test-389';
        const folderName = await bro.yaLoginAndCreateFolder(isMobile ? mobileUser : desktopUser);
        await bro.url(NAVIGATION.disk.url);

        // дергаем ручку приглашения в ОП
        await bro.execute((folderName, guest) => {
            window.rawFetchModel('do-invite-folder',
                {
                    path: '/disk/' + folderName,
                    userid: guest + '@yandex.ru',
                    rights: '660',
                    name: guest + '@yandex.ru',
                    service: 'email',
                    locale: 'ru',
                }).then();
        }, folderName, isMobile ? mobileGuest : desktopGuest);

        await bro.yaClientLoginFast(isMobile ? mobileGuest : desktopGuest);
        await bro.url(NAVIGATION.disk.url);

        await bro.yaWaitForVisible(listing.listing.invitesToFolders());
        const inviteAcceptButton = listing.invitesToFolderCertainAcceptButtonXpath().replace(/:titleText/g, folderName);
        await bro.yaScrollIntoView(inviteAcceptButton);
        await bro.click(inviteAcceptButton);
        await bro.yaWaitForHidden(inviteAcceptButton);
        await bro.yaScrollToEnd();
        await bro.yaAssertListingHas(folderName, 'У гостя не появилась общая папка');
        const folderSelector = listing.listingBodyItemsXpath().replace(/:titleText/g, folderName);
        await bro.yaScrollIntoView(folderSelector);
        const sharedIconSelector = folderSelector +
        '//*[contains(concat(" ", normalize-space(@class), " "), " file-icon_dir_shared ")]';
        assert(await bro.isVisible(sharedIconSelector), 'У общей папки неверная иконка');

        await bro.yaClientLoginFast(isMobile ? mobileUser : desktopUser);
        await bro.url(NAVIGATION.disk.url);
        await bro.yaDeleteCompletely(folderName);
    });

    hermione.only.in(clientDesktopBrowsersList, 'Доступ настраивается только на десктопе');
    it('diskclient-798: Создание ОП в разделе Общий доступ', async function() {
        const bro = this.browser;
        const folderName = await bro.yaGetUniqResourceName() + 'shared';
        this.currentTest.ctx.items = [folderName];

        await bro.yaClientLoginFast('yndx-ufo-test-538');
        await bro.yaOpenSection('shared');
        await bro.click(listing.listing.createSharedFolderButton());
        await bro.yaWaitForVisible(popups.common.selectFolderDialog());
        await bro.yaSetValue(popups.common.selectFolderDialog.nameInput(), folderName);
        await bro.click(popups.common.selectFolderDialog.submitButton());
        await bro.yaWaitForVisible(popups.desktop.invitePopup());

        const email = 'yndx-05ce3c6f8b2b693087gg56f36744f800142ad409130f8a91e9594@yandex.ru';
        await bro.yaSetValue(popups.desktop.invitePopup.emailInput(), email);
        await bro.yaWaitForVisible(popups.desktop.invitePopup.inviteByEmailButton());
        await bro.click(popups.desktop.invitePopup.inviteByEmailButton());
        await bro.click(popups.desktop.invitePopup.closeButton());
        await bro.yaWaitForHidden(popups.desktop.invitePopup());
        await bro.yaWaitForVisible(
            listing.listingBodyItemsInfoXpath().replace(/:titleText/g, folderName),
            10000
        );
        await bro.yaDeleteCompletely(folderName);
    });
});
