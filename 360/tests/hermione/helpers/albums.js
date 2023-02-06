const path = require('path');
const popups = require('../page-objects/client-popups');
const { NAVIGATION } = require('../config').consts;

/**
 * @param {string[]} args
 */
function addResourcesForCleanup(...args) {
    if (!this.currentTest.ctx.resources) {
        this.currentTest.ctx.resources = [];
    }
    this.currentTest.ctx.resources.push(...args);
}

/**
 * @param {Object} [options]
 * @param {string[]} [options.resourcesIds]
 * @param {boolean} [options.goToAlbum=true]
 * @param {boolean} [options.isPublic=false]
 * @param {boolean} [options.isFavorites]
 * @returns {Promise<{Object}>}
 */
async function createAlbumFromTestImages({ resourcesIds, goToAlbum = true, isPublic = false, isFavorites } = {}) {
    const bro = this.browser;
    const result = {};
    if (!resourcesIds) {
        await bro.url(NAVIGATION.disk.url);
        const tempFolderName = 'tmp-' + Date.now();
        await bro.yaCreateFolder(tempFolderName);

        addResourcesForCleanup.call(this, tempFolderName);

        await bro.yaOpenListingElement(tempFolderName);

        result.testImageFiles = await bro.yaUploadFiles([
            'test-image1.jpg',
            'test-image2.jpg',
            'test-image3.jpg'
        ], { uniq: true });
        result.resourcesIds = result.testImageFiles.map((filename) => `/disk/${tempFolderName}/${filename}`);
    } else {
        result.resourcesIds = resourcesIds;
        result.testImageFiles = resourcesIds.map((id) => path.basename(id));
    }

    const albumTitle = 'album-' + Date.now();

    const albumData = await bro.executeAsync((resourcesIds, title, isPublic, isFavorites, done) => {
        const params = {
            title,
            cover: 0,
            idsResources: JSON.stringify(resourcesIds)
        };
        if (!isPublic) {
            params.isPublic = 'false';
        }
        if (isFavorites) {
            params.albumType = 'favorites';
        }
        window.rawFetchModel('do-create-album', params).then((data) => {
            done(data);
        });
    }, result.resourcesIds, albumTitle, isPublic, isFavorites);

    if (!isFavorites) {
        this.currentTest.ctx.albumIds = this.currentTest.ctx.albumIds || [];
        this.currentTest.ctx.albumIds.push(albumData.id);
    }

    Object.assign(result, { albumData });

    if (goToAlbum) {
        await bro.url(`${NAVIGATION.albums.url}/${albumData.id}`);
        await bro.yaWaitAlbumItemsInViewportLoad();
    }

    return result;
}

/**
 * @param {string} [albumName]
 * @returns {Promise<string>}
 */
async function setNewAlbumName(albumName) {
    const bro = this.browser;
    const newAlbumTitle = albumName || `tmp-album-${Date.now()}`;
    this.currentTest.ctx.albumName = newAlbumTitle;
    await bro.yaWaitForVisible(popups.common.albumTitleDialog());
    await bro.yaSetValue(popups.common.albumTitleDialog.nameInput(), newAlbumTitle);
    await bro.click(popups.common.albumTitleDialog.submitButton());
    await bro.yaWaitForHidden(popups.common.albumTitleDialog());
    return newAlbumTitle;
}

/**
 * @returns {Promise<void>}
 */
async function cleanUpAlbums() {
    const bro = this.browser;
    const { resources, albumIds, excludePhotosFromFavorites } = this.currentTest.ctx;
    if (!resources && !albumIds && !excludePhotosFromFavorites) {
        return;
    }

    await bro.url('/client/disk');
    if (resources) {
        await bro.yaDeleteCompletely(resources, { safe: true, fast: true });
    }

    if (albumIds) {
        for (const albumId of albumIds) {
            await bro.executeAsync((id, done) => {
                ns.forcedRequest('do-remove-album', { id }).then(() => done());
            }, albumId);
        }
    }

    if (excludePhotosFromFavorites) {
        const resources = await bro.executeAsync((ids, done) => {
            window.rawFetchModel('getResources', { ids: JSON.stringify(ids) })
                .then((data) => done(data));
        }, excludePhotosFromFavorites);
        const resourceIds = resources.map(({ meta: { resource_id } }) => resource_id);
        const items = await bro.executeAsync((ids, done) => {
            window.rawFetchModel('albumFindInFavorites', { resourceIds: JSON.stringify(ids) })
                .then((data) => done(data));
        }, resourceIds);
        for (const item of items) {
            await bro.executeAsync((itemId, done) => {
                window.rawFetchModel('do-remove-resource-album', { itemId })
                    .then(() => done());
            }, item.item_id);
        }
    }
}

module.exports = {
    addResourcesForCleanup,
    cleanUpAlbums,
    createAlbumFromTestImages,
    setNewAlbumName
};
