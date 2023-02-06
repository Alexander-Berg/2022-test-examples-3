/**
 * Проверяет, сколько элементов по переданному селектору видимы на странице.
 *
 * @param {String} selector - Селектор элемента
 *
 * @returns {Promise<Number>}
 */
module.exports = function(selector) {
    return this
        .isVisible(selector)
        .then(visibilityStates => [].concat(visibilityStates).filter(Boolean).length);
};
