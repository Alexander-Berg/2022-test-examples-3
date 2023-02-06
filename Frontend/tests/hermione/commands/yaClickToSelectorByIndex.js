/**
 * Выбираем все элементы подподающие под itemsSelector и в полученном массиве элементов кликаем в элемент номер index
 * @param itemsSelector - селектор
 * @param index - индекс элемента в массиве выбранного по itemsSelector по которому будет клик
 * @return {Promise<void>}
 */
module.exports = async function yaClickToSelectorByIndex(itemsSelector, index) {
    await this.waitForVisible(itemsSelector, 10_000);
    await this.execute(function(itemsSelector, index) {
        const items = document.querySelectorAll(itemsSelector);
        const item = items[index];
        item.click();
    }, itemsSelector, index);
};
