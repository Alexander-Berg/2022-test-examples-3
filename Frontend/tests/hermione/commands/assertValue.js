const { assert } = require('chai');

/**
* Проверка значений поля формы
* @param {String} selector селектор
* @param {Object} expectedValue ожидаемый результат
* @returns {Object}
*/
module.exports = function assertValue(selector, expectedValue) {
    return this
        .getValue(selector)
        .then(value => assert.equal(value, expectedValue));
};
