/**
 * Выбор значения в саджесте
 * @param {Object} options
 * @param {Object} options.block - селектор контрола
 * @param {Object} options.menu - селектор выпадающего меню
 * @param {Object} options.text - вводимый в контрол текст
 * @param {Object} options.item - селектор выбираемого элемента из menu
 * @returns {Object}
 */
module.exports = function(options) {
    const {
        block, menu, text, item,
    } = options;

    return this
        .setValue(block, text)
        .waitForVisible(menu)
        .waitForVisible(item)
        .click(item)
        .waitForHidden(menu);
};
