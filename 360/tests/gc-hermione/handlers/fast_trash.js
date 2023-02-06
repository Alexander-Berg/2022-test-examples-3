/**
 * @typedef { import('../helpers/types').CleanerHandler } CleanerHandler
 */

/**
 * Быстрая очистка корзины (кнопка Очистить корзину)
 *
 * @type {CleanerHandler}
 */
const fastTrashCleaner = async(bro) => {
    bro.setMeta('trash', 'fast');

    await bro.executeAsync((done) => {
        window.rawFetchModel('do-clean-trash', { }).then(done, done);
    });
};

module.exports = fastTrashCleaner;
