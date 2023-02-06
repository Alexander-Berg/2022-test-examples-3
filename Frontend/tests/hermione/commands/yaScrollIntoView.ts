/**
 * Скроллит к элементу, чтобы он попал во вьюпорт
 *
 * @param {String} selector - Селектор для элемента, к которому нужно скроллить
 *
 * @returns {Promise}
 */

module.exports = async function yaScrollIntoView(selector: string) {
    const item = await this.$(selector);
    await item.scrollIntoView();
};
