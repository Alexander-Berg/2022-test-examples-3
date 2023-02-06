/**
 * Клик и задержка в 200ms.
 * i-fastlick отменяет второй клик, если он произошел раньше 200ms от первого
 *
 * @param {String} selector - Селектор элемента
 *
 * @returns {Promise}
 */
module.exports = async function (selector: string) {
    await this.click(selector);
    await this.pause(200);
};
