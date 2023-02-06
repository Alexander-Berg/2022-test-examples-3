/**
 * Делает элемент статичным
 * @param {String} selector
 * @returns {Promise}
 */
module.exports = function unStaticElement(selector) {
    return this.execute(function(selector) {
        const elem = document.getElementById(selector);
        if (elem) {
            elem.remove();
        }
    }, selector);
};
