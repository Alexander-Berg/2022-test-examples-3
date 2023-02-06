const {
    assert,
} = require('chai');

/**
 * Сравнивает значение атрибута HTML-элемента с желаемым значением
 * @param {String} selector - селектор HTML-элемента
 * @param {String} attributeName - Имя атрибута
 * @param {String} attributeValue - Желаемое значение атрибута
 * @param {String} isRegex - является ли эталон регуляркой
 * @returns {Promise}
 */
module.exports = function assertAttribute(selector, attributeName, attributeValue, isRegex = false) {
    return this.getAttribute(selector, attributeName)
        .then(_attributeValue => {
            if (!isRegex) {
                return assert.equal(_attributeValue, encodeURI(attributeValue));
            }
            return assert.match(_attributeValue, attributeValue);
        });
};
