const { NAVIGATION } = require('../../hermione/config').consts;
const { getResources } = require('../helpers/browser');

/**
 * @typedef { import('../helpers/types').CleanerHandler } CleanerHandler
 */

/**
 * Восстановление всех ресурсов из корзины
 *
 * @type {CleanerHandler}
 */
module.exports = async function restoreTrashCleaner(bro) {
    await bro.url(NAVIGATION.trash.url);

    const resources = await getResources(bro);

    if (!resources.length) {
        return;
    }

    await Promise.all(resources.map((resource) =>
        bro.executeAsync((resourceId, done) => {
            window.rawFetchModel('do-resource-restore', {
                id: resourceId
            }).then(done, done);
        }, resource.id)
    ));
};
