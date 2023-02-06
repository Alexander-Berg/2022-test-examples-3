/**
 * Ожидаем реплай-сообщение с заданным текстом
 *
 * @param {String} [text] - текст сообщения
 * @param {Object} options
 * @param {String} [options.type=receive|send] - тип сообщения - входящее/исходящее
 * @param {Boolean} [options.waitForSend] - ожидать отправки сообщения на сервер
 * @param {Number} [options.timeout] - таймаут ожидания, по умолчанию 10 секунд
 *
 * @returns {Promise}
 */
module.exports = async function yaWaitForNewReplyMessage(text: string, options: IMessageOptions) {
    const selector = '.yamb-message-reply + .yamb-message-content .yamb-message-text';
    const errorMessage = `Не найдено новое реплай-сообщение с ответом "${text}"`;

    await this.yaWaitForNewMessage(selector, text, errorMessage, options);
};
