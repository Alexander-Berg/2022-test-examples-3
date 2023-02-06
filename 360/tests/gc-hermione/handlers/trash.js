const { NAVIGATION } = require('../../hermione/config').consts;
const { getResources, deleteResources } = require('../helpers/browser');
const { yaFilterResources } = require('../helpers/filter-resources');

/**
 * @typedef { import('../helpers/types').CleanerHandler } CleanerHandler
 */

/**
 * Очистка корзины (выборочная)
 *
 * @type {CleanerHandler}
 */
module.exports = async function trashCleaner(bro, { filter }) {
    await bro.url(NAVIGATION.trash.url);

    const resources = await getResources(bro);
    const filteredResources = await yaFilterResources(resources, filter);

    if (!filteredResources.length) {
        bro.setMeta('trash', 'empty');
        return;
    }

    const deletedResources = await deleteResources(bro, filteredResources);

    bro.setMeta('trash', JSON.stringify({
        files: deletedResources,
        count: deletedResources.length,
    }, null, 4));
};
