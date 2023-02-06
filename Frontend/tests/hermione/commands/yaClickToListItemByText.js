/**
 * Клик в элемент списка - ListItem. Элемент определяется по названию
 * @param itemText - текст по которому необходимо кликнуть
 * @return {Promise<void>}
 */
module.exports = async function yaClickToListItemByText(itemText) {
    const itemsSelector = '.list-item__name,.list-item__label';
    await this.yaClickToSelectorByText(itemsSelector, itemText);
};
