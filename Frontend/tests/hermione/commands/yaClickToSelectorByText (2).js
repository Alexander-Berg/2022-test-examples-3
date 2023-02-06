/**
 * Клик в элемент на основе селектора. Например селектор - элемент меню
 * @param itemsSelector - селектор
 * @param itemText - текст по которому будет клик
 * @return {Promise<void>}
 */
module.exports = async function yaClickToSelectorByText(itemsSelector, itemText) {
    await this.waitForVisible(itemsSelector, 10_000);
    await this.execute(function(itemsSelector, itemText) {
        const adaptText = (text) => text.replace(/\s/g, ' ').toLowerCase().trimLeft();
        const items = document.querySelectorAll(itemsSelector);
        const item = [...items].find((item) => adaptText(item.innerText).startsWith(adaptText(itemText)));
        item.click();
    }, itemsSelector, itemText);
};
