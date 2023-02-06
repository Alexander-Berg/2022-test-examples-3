/**
 * Отключает JS-скролл блока fi-scroll
 * @returns {Promise}
 */
module.exports = function disableFiScrollTo() {
    return this.execute(() => {
        BEM.blocks['fi-scroll'].scrollTo = () => {};
    });
};
