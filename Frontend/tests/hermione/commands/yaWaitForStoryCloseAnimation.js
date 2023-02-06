/**
 * Ожидает завершения анимации закрытия стори
 */
module.exports = async function yaWaitForStoryCloseAnimation() {
    await this.yaWaitForHidden('.story-modal_visible .stories-modal__item-wrapper_active', 2_000);
};
