/**
 * Ожидаем сообщение с заданным текстом
 *
 * Проверяем текст последнего сообщения
 *
 * @param {String} text - текст сообщения
 * @param {Object} options
 * @param {String} [options.type=receive|send] - тип сообщения - входящее/исходящее
 * @param {Boolean} [options.waitForSend] - ожидать отправки сообщения на сервер
 * @param {Number} [options.timeout] - таймаут ожидания, по умолчанию 10 секунд
 * @param {Number} [options.index=0] - индекс сообщения от 0 - последнее 1 - предпосленее и т.п.
 *
 * @returns {Promise}
 */
module.exports = async function yaWaitForNewTextMessage(text: string, options: IMessageOptions) {
    const selector = '.yamb-message-content .yamb-message-text .text';
    const errorMessage = `Не найдено новое сообщение с текстом "${text}"`;

    await this.yaWaitForNewMessage(selector, text, errorMessage, options);
};
