/**
 * Проверяет, что элемент существует на странице в данный момент.
 *
 * @param {String} selector - Селектор элемента
 * @param {String} [message] - Сообщение об ошибке
 *
 * @returns {Promise<Boolean>}
 */

module.exports = function(selector, message) {
    return this
        .isExisting(selector)
        .then(isExisting => {
            assert.isTrue(isExisting, (message ? message + '. ' : '') +
                `Элемент с селектором ${selector} не существует!`);
        });
};
