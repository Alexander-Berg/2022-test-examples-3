/**
 * Клик в элемент списка BottomSheet-а. Элемент определяется по названию
 * @param itemText - текст по которому необходимо кликнуть
 * @return {Promise<void>}
 */
module.exports = async function yaClickToBottomSheetItem(itemText) {
    const itemsSelector = '.bottom-sheet:not(.bottom-sheet_hidden) .list-item__name';
    await this.yaClickToSelectorByText(itemsSelector, itemText);
};
