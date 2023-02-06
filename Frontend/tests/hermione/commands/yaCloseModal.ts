/**
 * Закрывает модальное окно
 *
 * @returns {Promise}
 */

module.exports = async function yaCloseModal() {
    try {
        await this.click(PO.modal.toolbar.backButton());
    } catch (e) {
        await this.yaScrollIntoView(PO.modal.toolbar.closeButton());
        await this.click(PO.modal.toolbar.closeButton());
    }
};
