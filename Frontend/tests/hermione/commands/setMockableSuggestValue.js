const { mockSuggest } = require('../helpers');

/**
 * Mок данных и выбор значения в саджесте
 * @param {Object} options
 * @param {Object} options.data - данные для мока
 * @param {Object} options.input - селектор поля ввода
 * @param {Object} options.items - селектор списка выпадающих элементов
 * @param {Object} options.item - селектор выбираемого элемента
 * @returns {Object}
 */
module.exports = function setMockableSuggestValue(options) {
    const {
        data, input, items, item, text
    } = options;

    return this
        .execute(mockSuggest, data)
        .setValue(input, text)
        .click(input)
        .waitForVisible(items)
        .waitForVisible(item)
        .click(item)
        .waitForHidden(items);
};
