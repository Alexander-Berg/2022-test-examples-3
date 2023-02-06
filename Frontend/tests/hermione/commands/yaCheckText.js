/**
 * Проверяет, что текст внутри элемента удовлетворяет ожидаемому.
 *
 * @param {String} selector - Селектор элемента
 * @param {String} expectedText - ожидаемое значение
 * @param {String} [message] - Сообщение об ошибке
 *
 * @returns {Promise<Boolean>}
 */

module.exports = function(selector, expectedText, message) {
    return this
        .getText(selector)
        .then(value => {
            assert(value, expectedText, (message ? message + '. ' : '') +
            `Значение селектора ${selector} не соответствует ожидаемому!`);
        });
};
