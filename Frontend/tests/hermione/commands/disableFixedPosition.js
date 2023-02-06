/**
 * Убирает position: fixed у указанного блока 
 * (этот способ решает проблему с некорректной склейкой скриншотов, когда указанный элемент не помещается во viewport, и криншоты склеиваются)
 * @param {String}  selector
 * @returns {Promise}
 */
 module.exports = function disableFixedPosition(selector = '.modal_visible_yes') {
    return this.execute(function(selector) {
        const style = document.createElement('style');
        style.id = selector;

        style.textContent = /* css */`${selector}
        {
            position: unset !important
        }`;
        document.head.appendChild(style);
    }, selector);
};
