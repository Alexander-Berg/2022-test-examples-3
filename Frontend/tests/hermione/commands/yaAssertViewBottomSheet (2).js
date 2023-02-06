/**
 * Снятие скриншота открытого меню - BottomSheet
 * @param screenshotName - название скриншота
 * @return {Promise<void>}
 */
module.exports = async function yaAssertViewBottomSheet(screenshotName) {
    const itemsSelector = '.bottom-sheet:not(.bottom-sheet_hidden) .bottom-sheet__content';
    await new Promise((resolve) => setTimeout(resolve, 500));
    await this.assertView(screenshotName, itemsSelector);
};
