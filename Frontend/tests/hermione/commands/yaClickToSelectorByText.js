/**
 * Клик в элемент на основе селектора. Например селектор - элемент меню
 * @param itemsSelector - селектор
 * @param itemText - текст по которому будет клик, может быть массивом строк
 * @param strict - строгое сравнение
 * @return {Promise<void>}
 */
module.exports = async function yaClickToSelectorByText(itemsSelector, itemText, strict = false) {
    if (typeof itemText === 'string') {
        itemText = [itemText];
    }

    await this.waitForVisible(itemsSelector, 10_000);
    await this.execute(function(itemsSelector, itemText, strict) {
        const items = document.querySelectorAll(itemsSelector);
        const item = [...items].find(item => {
            const text = item.innerText.replace(/\s+/g, ' ').toLowerCase().trim();

            return itemText.some(str => (strict ? str === text : text.startsWith(str)));
        });
        item.click();
    }, itemsSelector, itemText.map(str => str.replace(/\s+/g, ' ').toLowerCase().trim()), strict);
};
