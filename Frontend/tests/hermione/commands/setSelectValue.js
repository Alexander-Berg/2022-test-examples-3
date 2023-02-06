/**
 * Выбор значения в селекте
 * @param {Object} options
 * @param {Object} options.block - селектор контрола
 * @param {Object} options.menu - селектор выпадающего меню
 * @param {Object} options.item - селектор выбираемого элемента из menu
 * @returns {Object}
 */
module.exports = function setSelectValue(options) {
    let {
        block,
        menu = '.popup2.popup2_visible_yes',
        item,
    } = options;

    return this
        .waitForVisible(block)
        .click(block)
        .waitForVisible(menu)
        .waitForVisible(item)
        .click(item)
        .waitForHidden(menu);
};
