/**
 * Проверяет, что значение элемента удовлетворяет ожидаемому.
 *
 * @param {String} selector - Селектор элемента
 * @param {String} expectedValue - ожидаемое значение
 * @param {String} [message] - Сообщение об ошибке
 *
 * @returns {Promise<Boolean>}
 */

module.exports = function(selector, expectedValue, message) {
    return this
        .getValue(selector)
        .then(value => {
            assert(value, expectedValue, (message ? message + '. ' : '') +
            `Значение селектора ${selector} не соответствует ожидаемому!`);
        });
};
