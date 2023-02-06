/**
 * Патчит стили HTML элемента
 * @param {String} selector
 * @param {Object} customStyle
 * @returns {Promise}
 */
module.exports = function patchStyle(selector, customStyle = {}) {
    return this.execute((selector, customStyle) => {
        const elem = document.querySelector(selector);
        if (elem) {
            Object.assign(
                elem.style,
                customStyle
            );
        }
    }, selector, customStyle);
};
