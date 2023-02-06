/**
 * Команда для ожидания конкретного текста на элементе. Если элементов по селектору несколько, ищется текст хотя бы
 * в одном из них.
 *
 * @param {String} selector Селектор.
 * @param {String} text Значение.
 * @param {Number} [timeout] Таймаут в миллисекундах
 * @param {Boolean} [oneLine] Флаг, для преобразования многострочного текста в однострочный.
 * @param {String} delimiter Разделитель строк
 * @returns {Boolean} Возвращает `true` если текст найден
 */
module.exports = function waitForExactText(selector, text, timeout, oneLine = false, delimiter = ' ') {
    return this.waitUntil(async() => {
        await this.waitForVisible(selector, timeout);
        let elementText = await this.getText(selector);

        elementText = Array.isArray(elementText) ? elementText : [elementText];

        if (oneLine) {
            elementText = elementText.map(item => item.replace(/\s+/g, delimiter));
        }

        return elementText.includes(text);
    }, timeout, `Текст не соответствует искомому: ${text} , ${selector}`);
};
