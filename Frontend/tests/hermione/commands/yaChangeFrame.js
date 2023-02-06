/**
 * Переходит в frame указанного селектора
 *
 * @param {String} selector - Селектор элемента
 *
 * @returns {Promise}
 */

module.exports = function(selector) {
    return this
        .element(selector)
        .then(element => {
            return this.frame(element.value);
        });
};
