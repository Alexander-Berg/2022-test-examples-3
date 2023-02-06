const { getAlbums, deleteAlbums } = require('../helpers/browser');
const { yaFilterResources } = require('../helpers/filter-resources');

/**
 * @typedef { import('../helpers/types').CleanerHandler } CleanerHandler
 */

/**
 * Очистка альбомов (выборочная)
 *
 * @type {CleanerHandler}
 */
const albumsCleaner = async(bro, { filter }) => {
    const albums = await getAlbums(bro);
    const filteredAlbums = await yaFilterResources(albums, filter);

    if (!filteredAlbums.length) {
        bro.setMeta('albums', 'empty');
        return;
    }

    const deletedAlbums = await deleteAlbums(bro, filteredAlbums);

    bro.setMeta('albums', JSON.stringify({
        albums: deletedAlbums,
        count: deletedAlbums.length,
    }, null, 4));
};

module.exports = albumsCleaner;
