const {
    assert,
} = require('chai');

/**
 * Сравнивает текст внутри block с заданным значением text
 * @param {String} block - селектор элемента
 * @param {String} text - текст
 * @returns {Object}
 */
module.exports = function assertText(block, text) {
    return this
        .getText(block)
        .then(_text => assert.equal(_text, text));
};
