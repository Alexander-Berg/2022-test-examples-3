/**
 * Скролит указанный контейнер к нужной позиции
 *
 * @param {String} selector - селектор контейнера с overflow = scroll|auto
 * @param {Number} [offsetX=0] - значение скролла по горизонтали
 * @param {Number} [offsetY=0] - значение скролла по вертикали
 *
 * @returns {Promise}
 */
module.exports = function yaScrollElement(selector, offsetX = 0, offsetY = 0) {
    return this.execute(function(containerSelector, targetScrollLeft, targetScrollTop) {
        const container = document.querySelector(containerSelector);

        container.scrollLeft = targetScrollLeft; // скроллим по оси X
        container.scrollTop = targetScrollTop; // скроллим по оси Y
    }, selector, offsetX, offsetY);
};
