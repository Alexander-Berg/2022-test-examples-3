/**
 * Выбираем один элемент подподающие под itemsSelector и скорлимся к элементу
 * @param {string} itemsSelector - селектор
 * @return {Promise<void>}
 */
module.exports = async function yaScrollToEndOnSelector(itemsSelector) {
    await this.waitForVisible(itemsSelector, 10_000);
    await this.execute(function(itemsSelector) {
        const item = document.querySelector(itemsSelector);
        if (item) {
            item.scrollTop = item.scrollHeight;
        }
    }, itemsSelector);
};
