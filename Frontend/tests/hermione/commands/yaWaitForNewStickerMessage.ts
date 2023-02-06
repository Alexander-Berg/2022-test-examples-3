/**
 * Ожидаем сообщение со стикером
 *
 * @param {Object} options
 * @param {String} [options.type=receive|send] - тип сообщения - входящее/исходящее
 * @param {Boolean} [options.waitForSend] - ожидать отправки сообщения на сервер
 * @param {Number} [options.timeout] - таймаут ожидания, по умолчанию 10 секунд
 *
 * @returns {Promise}
 */
module.exports = async function yaWaitForNewStickerMessage(options: IMessageOptions) {
    await this.yaWaitForNewMessage(PO.lastMessage.sticker(), '', 'Не найдено новое сообщение со стикером', options);
};
