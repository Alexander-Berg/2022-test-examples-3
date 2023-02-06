/**
 * Ожидаем окончание анимации BottomSheet
 * @return {Promise<void>}
 */
module.exports = async function yaWaitBottomSheetAnimation() {
    await this.waitForVisible('.bottom-sheet__wrapper');
    await this.yaWaitForVisibleAndAssertErrorMessage(
        '.bottom-sheet__wrapper_animated',
        1500,
        'Не закончилась анимация BottomSheet',
        true
    );
};
