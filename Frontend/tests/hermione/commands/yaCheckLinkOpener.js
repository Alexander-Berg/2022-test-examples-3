/**
 * Команда для базовых проверок ссылки
 *
 * 1. Атрибут href ссылки не пуст и не состоит из пробельных символов
 * 2. Ссылка открывается с правильным атрибутом 'target'
 * 3. По ссылке можно кликнуть
 *
 * @param {String} selector
 * @param {String} [message=Параметры ссылки содержат неверные значения] - кастомное базовое сообщение об ошибке,
 *   которое будет дополнено информацией о конкретной ошибке
 * @param {Object} [params]
 * @param {String} [params.target=_blank|_self] - открывать ссылку в новой вкладке/оставаться в текущей вкладке
 * @param {Array} [params.clickCoords] - координаты клика, есть надо кликать не в центр элемента
 *
 * @returns {Promise.<Url>} - разобранный URL
 */
module.exports = function(selector, message, params) {
    if (typeof message !== 'string') {
        params = message;
        message = 'Параметры ссылки содержат неверные значения';
    }

    params = params || {};

    return this.yaCheckLink2({
        selector,
        message,
        target: params.target,
        clickCoords: params.clickCoords,
    });
};
