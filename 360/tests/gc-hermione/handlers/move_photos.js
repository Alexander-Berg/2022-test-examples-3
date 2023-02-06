const { getResources } = require('../helpers/browser');
const { yaFilterResources } = require('../helpers/filter-resources');
const listing = require('../../hermione/page-objects/client-content-listing').common;

/**
 * @typedef { import('../helpers/types').CleanerHandler } CleanerHandler
 */

/**
 * Проверка директорий листинга на наличие фотографий + возврат фото в папку Photos
 *
 * @type {CleanerHandler}
 */
module.exports = async function movePhotos(bro, { filter }) {
    const resources = await getResources(bro);
    const filteredResources = await yaFilterResources(resources, filter);

    const filteredResourcesNames = filteredResources.map(({ name }) => name);

    for (const resourceName of filteredResourcesNames) {
        await bro.yaSelectResource(resourceName);

        if (await bro.yaSelectedResourceType() === 'dir') {
            await bro.yaOpenListingElement(resourceName);
            await bro.yaWaitForHidden(listing.listingSpinner());
            await bro.yaMoveBackToPhoto();
            await bro.yaOpenSection('disk');
        }
    }
};
