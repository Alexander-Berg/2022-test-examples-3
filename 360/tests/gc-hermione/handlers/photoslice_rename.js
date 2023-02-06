const { getNameWithoutTimestamps } = require('../helpers/fix-resources');
const { photo } = require('../../hermione/page-objects/client-photo2-page').common;

/**
 * @typedef { import('../helpers/types').CleanerHandler } CleanerHandler
 */

/**
 * Коррекция имен первых N элементов фотосреза
 *
 * @type {CleanerHandler}
 */
module.exports = async function photosliceRename(bro) {
    const PHOTOS_COUNT_RANGE = 22;

    await bro.yaWaitForVisible(photo.item(), 10000);

    for (let i = 2; i < PHOTOS_COUNT_RANGE; i++) {
        const selector = `${photo.item()}:nth-child(${i})`;

        try {
            await bro.yaWaitForVisible(selector);
        } catch (error) {
            // `:nth-child(i)` может оказаться заголовком, а не фоткой
            // тогда просто перейдём на следующую фотку
            const isTitle = await bro.isVisible(`${photo.title()}:nth-child(${i})`);
            if (isTitle) {
                continue;
            } else {
                throw error;
            }
        }

        const fileName = await bro.yaGetPhotosliceItemName(selector);

        if (fileName.includes('_')) {
            await bro.selectAndRenamePhoto(selector, getNameWithoutTimestamps(fileName));
        }
    }
};
