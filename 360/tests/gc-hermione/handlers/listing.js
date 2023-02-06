const { getResources, deleteResources } = require('../helpers/browser');
const { yaFilterResources } = require('../helpers/filter-resources');

/**
 * @typedef { import('../helpers/types').CleanerHandler } CleanerHandler
 */

/**
 * Очистка листинга (выборочная)
 *
 * @type {CleanerHandler}
 */
module.exports = async function listingCleaner(bro, { filter }) {
    const resources = await getResources(bro);
    const filteredResources = await yaFilterResources(resources, filter);

    if (!filteredResources.length) {
        bro.setMeta('listing', 'empty');
        return;
    }

    const deletedResources = await deleteResources(bro, filteredResources);

    bro.setMeta('listing', JSON.stringify({
        files: deletedResources,
        count: deletedResources.length,
    }, null, 4));
};
