const cryptoGen = require('crypto');
const debugSelector = require('debug')('chat:hermione:selector');

/**
 * Получает селектор элемента с указанным текстовым содержимым из коллекции по селектору
 *
 * @param {string} [selector] - селектор коллекции элементов
 * @param {string} [containsText] - текстовое содержимое
 * @param {string} [customHashText] - кастомное содержимое
 *
 * @returns {Promise<string>} - селектор на элемент с указанным текстовым содержимым
 */
module.exports = async function (selector: string, containsText: string, customHashText: string) {
    debugSelector('Selector %o', selector);
    debugSelector('Contains text %o', containsText);

    const containsHash = cryptoGen.createHash('md5').update(customHashText || containsText).digest('hex');

    debugSelector('Hash: %s', containsHash);

    await this.execute(function (collectionSelector, containsTextFilter, containsHashFilter) {
        Array.prototype.filter.call(
            document.querySelectorAll(collectionSelector),
            (item) => containsTextFilter ? item.innerText.includes(containsTextFilter) : true,
        ).forEach((item) => {
            item.dataset.containsHash = containsHashFilter;
        });
    }, selector, containsText, containsHash);

    return `${selector}[data-contains-hash="${containsHash}"]`;
};
