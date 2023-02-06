/**
 * Клик в элемент списка - TabThemeChips. Элемент определяется по названию
 * @param itemText - текст по которому необходимо кликнуть
 * @return {Promise<void>}
 */
module.exports = async function yaClickTabThemeChipsByText(itemText) {
    const itemsSelector = '.chips .chips-item'; // кнопка бывает обернута в tooltip %(
    await this.yaClickToSelectorByText(itemsSelector, itemText);
};
