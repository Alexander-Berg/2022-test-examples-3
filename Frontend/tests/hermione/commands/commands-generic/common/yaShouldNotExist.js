/**
 * Проверяет, что элемент не существует на странице в данный момент.
 * Если пропадания ноды из DOM не нужно ждать, лучше использовать yaShouldNotExist, а не waitForExist.
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
            assert.isFalse(isExisting, (message ? message + '. ' : '') +
                `Элемент с селектором ${selector} существует!`);
        });
};
