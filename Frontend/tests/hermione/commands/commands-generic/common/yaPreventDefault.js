/**
 * Отменяет действие по умолчанию при клике на элемент.
 * Обычно используется при тестировании счетчиков на клик, чтобы не было переходов.
 * Если этого не сделать - то страница обновляется, и при этом отсутствует параметр testRunId
 *
 * @param {String} selector - Селектор элемента
 *
 * @returns {Promise<Boolean>}
 */

module.exports = function(selector) {
    return this.selectorExecute(selector, function(elements) {
        elements.forEach(node => {
            node.onclick = function(e) { e.preventDefault() };
        });
    });
};
