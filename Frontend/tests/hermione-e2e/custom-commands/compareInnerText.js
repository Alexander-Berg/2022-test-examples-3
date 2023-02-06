const assert = require('chai').assert;

/**
 * Сравнивает переданный текст с текстом элемента.
 *
 * @param {String} selector - селектор элемента
 * @param {String} text - текст, с которым будет сравниваться текст элемента
 * @param {String} [errorMessage]
 *
 * @returns {Promise}
 */

module.exports = function(selector, text, errorMessage) {
    return this
        .getText(selector)
        .then((elementText) => assert.strictEqual(elementText, text, errorMessage));
};
