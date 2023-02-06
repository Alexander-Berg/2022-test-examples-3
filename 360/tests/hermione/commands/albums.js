const albums = require('../page-objects/client-albums-page');
const photos = require('../page-objects/client-photo2-page');

/**
 * Ждёт пока прогрузятся все превью во вьюпорте в альбоме
 *
 * @this Browser
 * @returns {Promise<void>}
 */
const yaWaitAlbumItemsInViewportLoad = async function() {
    await this.yaWaitPreviewsLoaded(albums.album2.item.preview(), true, 5000);
};

/**
 * Ждёт пока появится элементы или заглушка в альбоме
 *
 * @returns {Promise<void>}
 */
const yaWaitAlbumLoaded = async function() {
    await this.yaWaitForHidden(albums.album2.spin());
    try {
        await this.yaWaitForVisible(albums.album2.item(), 2000);
    } catch (error) {
        await this.yaWaitForVisible(albums.album2.stub());
    }
};

/**
 * @param {string[]} fileNames
 * @returns {Promise<void>}
 */
const yaAssertPhotosInAlbum = async function(fileNames) {
    for (const name of fileNames) {
        await this.yaWaitForVisible(albums.album2.itemByName().replace(':title', name));
    }
};

/**
 * @param {string[]} fileNames
 * @param {boolean} [scrollIntoViewport]
 * @returns {Promise<void>}
 */
const yaSelectPhotosAndCreateAlbum = async function(fileNames, scrollIntoViewport = false) {
    await this.yaWaitPhotoSliceItemsInViewportLoad();
    await this.yaWaitPhotosliceWithToolbarOpened();
    for (const photo of fileNames) {
        const selector = photos.common.photo.itemByName().replace(':title', photo);
        if (scrollIntoViewport) {
            await this.yaScrollIntoView(selector);
        }
        await this.click(selector);
    }
    await this.click(photos.common.addToAlbumBar.submitButton());
};

/**
 * @param {string} action
 * @param {boolean} [waitForHideDropdown=true]
 * @returns {Promise<void>}
 */
const yaCallActionInAlbumActionsDropdown = async function(action, waitForHideDropdown = true) {
    await this.yaWaitForVisible(albums.album2.actionsMoreButton());
    await this.click(albums.album2.actionsMoreButton());
    await this.yaWaitForVisible(albums.album2ActionsDropdown());
    await this.pause(200);
    const actionButtonSelector = albums.album2ActionsDropdown[`${action}Button`]();

    await this.yaWaitForVisible(actionButtonSelector);
    await this.click(actionButtonSelector);
    if (waitForHideDropdown) {
        await this.yaWaitForHidden(albums.album2ActionsDropdown());
    }
};

/**
 * Удаляет альбом с заданным именем
 *
 * @param {string} name
 * @returns {Promise<void>}
 */
const yaDeleteAlbumByName = async function(name) {
    const albumSelector = albums.albums2.personal.album();

    await this.yaOpenSection('albums');
    await this.yaWaitForVisible(albumSelector);

    await this.executeAsync((albumName, done) => {
        window.rawFetchModel('albums', { offset: 0, amount: 40 }).then((data) => {
            const album = data.find((album) => album.title === albumName);
            if (album) {
                const albumId = album.id;
                ns.forcedRequest('do-remove-album', { id: albumId })
                    .then(() => done(albumId), (error) => done(error));
            } else {
                return done();
            }
        }).catch((error) => done(error));
    }, name);
};

module.exports = {
    common: {
        yaWaitAlbumItemsInViewportLoad,
        yaWaitAlbumLoaded,
        yaAssertPhotosInAlbum,
        yaSelectPhotosAndCreateAlbum,
        yaCallActionInAlbumActionsDropdown,
        yaDeleteAlbumByName
    }
};
