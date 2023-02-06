/**
 * Делает элемент статичным
 * @param {String} selector
 * @returns {Promise}
 */
module.exports = function staticElement(selector) {
    return this.execute(function(selector) {
        const style = document.createElement('style');
        style.id = selector;

        style.textContent = /* css */`${selector}
        {
            position: static !important;
        }`;
        document.head.appendChild(style);
    }, selector);
};
