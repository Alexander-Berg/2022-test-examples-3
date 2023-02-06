/**
 * Ожидает завершения анимации раскрытия стори
 */
module.exports = async function yaWaitForStoryOpenAnimation() {
    await this.yaWaitForVisible('.story-modal_visible .stories-modal__item-wrapper_active');
    await this.yaWaitForVisible('.story-modal__scalable[style*="transform: scale(1)"]', 2_000);
};
