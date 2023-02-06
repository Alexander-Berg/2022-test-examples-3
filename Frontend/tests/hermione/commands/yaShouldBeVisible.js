/**
 * Проверяет, что элемент видим в данный момент. Если показа не нужно ждать, лучше использовать yaShouldBeVisible,
 * а не waitForVisible. Если по селектору выберется несколько элементов, выбросит ошибку.
 *
 * @param {String} selector - Селектор элемента
 * @param {String} [message] - Сообщение об ошибке
 *
 * @returns {Promise<Boolean>}
 */

module.exports = function(selector, message) {
    return this
        .isVisible(selector)
        .then(isVisible => {
            if (Array.isArray(isVisible)) {
                throw new Error('Найдено более одного элемента. ' +
                    `Пожалуйста, используйте более конкретный селектор. Исходный селектор - ${selector}`);
            }
            assert.isTrue(isVisible, (message ? message + '. ' : '') + `Элемент с селектором ${selector} не виден!`);
        });
};
