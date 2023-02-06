const { NAVIGATION, TEXT_NOTIFICATION_COVER_CHANGED } = require('../config').consts;
const albums = require('../page-objects/client-albums-page');
const popups = require('../page-objects/client-popups');
const slider = require('../page-objects/slider').common;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { assert } = require('chai');

/**
 * Переход в список альбомов лиц
 *
 */
async function openAlbumsFaces() {
    const bro = this.browser;
    await bro.click(albums.albums2.faces());
    await bro.yaWaitForHidden(albums.albums2.shimmer());
    await bro.yaWaitPreviewsLoaded(albums.albums2.item.preview());
}

/**
 * Переход в альбом-лицо
 *
 * @param {string} [albumTitle]
 */
async function openAlbumFaces(albumTitle) {
    const bro = this.browser;
    await bro.click(albumTitle ? albums.albumByName().replace(':titleText', albumTitle) : albums.albums2.item());
    await bro.yaWaitPreviewsLoaded(albums.album2.item.preview());
}

/**
 * Показ мета-альбома лица в списке всех альбомов
 */
async function isShownAlbumFaces() {
    const bro = this.browser;
    await bro.yaWaitForHidden(albums.albums2.shimmer());
    await bro.yaWaitForVisible(albums.albums2.faces());
}

/**
 * Получить заголовок ресурса, который стоит на обложке альбома
 *
 * @param {string} albumId
 * @returns {string}
 */
async function getCoverResourceTitle(albumId) {
    const bro = this.browser;
    const albumCoverResourceTitle = await bro.executeAsync((albumId, done) => {
        window.rawFetchModel('getAlbum', { albumId }).then((album) => {
            const title = album.cover.object.id.match(/([^\/]+)$/)[1];
            done(title);
        }).catch((error) => done(error));
    }, albumId);

    return albumCoverResourceTitle;
}

/**
 * Получить заголовок ресурса, который не стоит на обложке альбома
 *
 * @param {string} albumCoverResourceTitle
 * @returns {string}
 */
async function getNotCoverResourceTitle(albumCoverResourceTitle) {
    const bro = this.browser;
    const photoTitle = await bro.execute((selector, albumCoverResourceTitle) => {
        let title;
        document.querySelectorAll(selector).forEach((element) => {
            if (element.title !== albumCoverResourceTitle && !title) {
                title = element.title;
            }
        });
        return title;
    }, albums.album2.item(), albumCoverResourceTitle);

    return photoTitle;
}

describe('Альбомы-Лица ->', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-763');
        await bro.url(NAVIGATION.albums.url);

        await isShownAlbumFaces.call(this);
    });

    it('diskclient-6235, diskclient-6269: Отображение альбома "Люди на фото"', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6269' : 'diskclient-6235';

        await openAlbumsFaces.call(this);
        await bro.yaAssertView(this.testpalmId, albums.albums2());
    });

    it('diskclient-6236, diskclient-6271: Переход в альбом "Люди на фото"', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6271' : 'diskclient-6236';

        await openAlbumsFaces.call(this);
        await openAlbumFaces.call(this);

        await bro.back();
        await bro.yaWaitPreviewsLoaded(albums.albums2.item.preview());

        await bro.back();
        await bro.yaWaitForVisible(albums.albums2.faces());
    });

    it('diskclient-6251, diskclient-6294: Указание имени альбома лица по клику на заголовок', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6294' : 'diskclient-6251';

        await openAlbumsFaces.call(this);
        await openAlbumFaces.call(this);

        await bro.click(albums.album2.headerName());

        await bro.yaWaitForVisible(popups.common.albumTitleDialog());
        await bro.click(popups.common.albumTitleDialog.closeButton());
        await bro.yaWaitForHidden(popups.common.albumTitleDialog());
    });

    it('diskclient-6332, diskclient-6333: Переход в один из альбомов из "Люди на фото"', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6333' : 'diskclient-6332';

        await openAlbumsFaces.call(this);
        await openAlbumFaces.call(this, 'Lady');

        await bro.yaWaitForHidden(albums.album2.headerName());
    });

    it('diskclient-6212, diskclient-6286: Просмотр файла в слайдере альбома лица', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6286' : 'diskclient-6212';

        await openAlbumsFaces.call(this);
        await openAlbumFaces.call(this, 'Lady');

        await bro.yaClick(albums.album2.itemByName().replace(':title', '2014-10-05 18-16-14.JPG'));

        await bro.waitForVisible(slider.contentSlider.previewImage());
        await bro.yaAssertView(this.testpalmId, slider.contentSlider());
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-74253');
    it('diskclient-faces-in-dialog-add-to-album: альбом Люди на фото в диалоге выбора альбома при добавлении фото в альбом', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'faces-in-dialog-add-to-album' : 'diskclient-faces-in-dialog-add-to-album';

        await bro.url(NAVIGATION.disk.url);
        await bro.yaSelectResource('Горы.jpg');
        await bro.yaCallActionInActionBar('addToAlbum', false);

        const albumFacesSelector = popups.common.selectAlbumDialog.albumByName().replace(':title', 'Люди на фото');
        await bro.yaClick(albumFacesSelector);
        await bro.yaWaitForVisible(popups.common.selectAlbumDialog.album());

        await bro.yaAssertView(this.testpalmId, popups.common.selectAlbumDialog());

        await bro.click(popups.common.selectAlbumDialog.title.backIcon());
        await bro.yaWaitForVisible(albumFacesSelector);
        await bro.click(popups.common.selectAlbumDialog.closeButton());
        await bro.yaWaitForHidden(popups.common.selectAlbumDialog());
    });
});

describe('Альбомы-лица(Модифицирующие) -> ', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.url(NAVIGATION.albums.url);
        await bro.yaSkipWelcomePopup();
        await bro.yaSkipFacesOnboarding();
        await isShownAlbumFaces.call(this);
    });

    hermione.only.in(clientDesktopBrowsersList);
    hermione.auth.tus({ tags: 'diskclient-album-face-actions', lock_duration: '60' });
    it('diskclient-6208: Указание имени альбома лица из списка', async function() {
        const bro = this.browser;

        await openAlbumsFaces.call(this);

        const albumSelector = albums.albums2.item();
        await bro.yaWaitForVisible(albumSelector);

        // ховер нужен, чтобы появилась кнопка с действиями
        await bro.moveToObject(albumSelector);
        await bro.click(albumSelector + ' ' + albums.albumSettingsButton());
        await bro.yaWaitForVisible(albums.albumSettingsPopup());
        await bro.pause(200);
        await bro.click(albums.albumSettingsPopup.rename());

        const newAlbumTitle = 'album_' + Date.now();

        await bro.yaWaitForVisible(popups.common.albumTitleDialog());
        await bro.yaSetValue(popups.common.albumTitleDialog.nameInput(), newAlbumTitle);
        await bro.click(popups.common.albumTitleDialog.submitButton());
        await bro.yaWaitForHidden(popups.common.albumTitleDialog());

        const renamedAlbumSelector = albums.albumByName().replace(':titleText', newAlbumTitle);
        await bro.yaWaitForVisible(renamedAlbumSelector);
        await bro.click(renamedAlbumSelector);

        await bro.yaWaitForVisible(albums.album2.title());
        assert.strictEqual(await bro.getText(albums.album2.title()), newAlbumTitle);
    });

    hermione.auth.tus({ tags: 'diskclient-album-face-actions', lock_duration: '60' });
    it('diskclient-6204, diskclient-6295: Переименование альбома лица', async function() {
        const bro = this.browser;

        await openAlbumsFaces.call(this);
        await openAlbumFaces.call(this);

        await bro.yaCallActionInAlbumActionsDropdown('rename');

        const newAlbumTitle = 'album_' + Date.now();

        await bro.yaWaitForVisible(popups.common.albumTitleDialog());
        await bro.yaSetValue(popups.common.albumTitleDialog.nameInput(), newAlbumTitle);
        await bro.click(popups.common.albumTitleDialog.submitButton());
        await bro.yaWaitForHidden(popups.common.albumTitleDialog());

        assert.strictEqual(await bro.getText(albums.album2.title()), newAlbumTitle);

        await bro.click(albums.album2.backButton());
        await bro.yaWaitForVisible(albums.albumByName().replace(':titleText', newAlbumTitle));

        await bro.back();
        await bro.yaWaitForVisible(albums.album2.title());
        assert.strictEqual(await bro.getText(albums.album2.title()), newAlbumTitle);
    });

    hermione.auth.tus({ tags: 'diskclient-album-face-actions', lock_duration: '60' });
    it('diskclient-6221, diskclient-6301: Смена обложки в альбоме лица', async function() {
        const bro = this.browser;

        await openAlbumsFaces.call(this);
        await openAlbumFaces.call(this);

        const currentUrl = await bro.getUrl();
        const albumId = currentUrl.match(/([^\/]+)$/)[1];

        const albumCoverResourceTitle = await getCoverResourceTitle.call(this, albumId);
        const photoTitle = await getNotCoverResourceTitle.call(this, albumCoverResourceTitle);

        await bro.yaSelectPhotosliceItem(albums.album2.itemByName().replace(':title', photoTitle), true);
        await bro.yaCallActionInActionBar('setAsCover', true);
        bro.yaWaitNotificationWithText(TEXT_NOTIFICATION_COVER_CHANGED);
    });
});
