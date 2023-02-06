/**
 * Проверяет, что элемент невидим/отсутствует в данный момент.
 * Если скрытия не нужно ждать, лучше использовать yaShouldNotBeVisible, а не yaWaitForHidden.
 * Если по селектору выберется несколько элементов, выбросит ошибку.
 *
 * @param {String} selector - Селектор элемента
 * @param {String} message - Кастомное сообщение об ошибке
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
            assert.isFalse(isVisible, (message ? message + '. ' : '') + `Элемент с селектором ${selector} виден!`);
        });
};
