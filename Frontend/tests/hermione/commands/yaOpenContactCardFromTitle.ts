/**
 * Сценарий открытия карточки контакта по нажатию на заголовок чата
 *
 * @returns {Promise}
 */
module.exports = async function () {
    await this.click(PO.chat.header());
    await this.yaWaitForVisible(PO.modal(), 'Не открылась карточка контакта');
};
