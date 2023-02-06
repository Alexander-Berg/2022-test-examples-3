/**
 * Ожидаем системное сообщение с заданным текстом
 *
 * Проверяем текст последнего сообщения
 *
 * @param {String} text - текст сообщения
 * @param {Object} options
 * @param {String} [options.type=receive|send] - тип сообщения - входящее/исходящее
 * @param {Boolean} [options.waitForSend] - ожидать отправки сообщения на сервер
 * @param {Number} [options.timeout] - таймаут ожидания, по умолчанию 10 секунд
 *
 * @returns {Promise}
 */
module.exports = async function yaWaitForNewSystemMessage(text: string, options: IMessageOptions) {
    const errorMessage = `Не найдено новое системное сообщение с текстом "${text}"`;

    await this.yaWaitForNewMessage(PO.lastMessage.system(), text, errorMessage, options);
};
