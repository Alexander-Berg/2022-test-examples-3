/**
 * Проверяем галерею последнего сообщения
 *
 * @param {String} text - текст сообщения
 * @param {Object} options
 * @param {String} [options.type=receive|send] - тип сообщения - входящее/исходящее
 * @param {Boolean} [options.waitForSend] - ожидать отправки сообщения на сервер
 * @param {Number} [options.timeout] - таймаут ожидания, по умолчанию 10 секунд
 *
 * @returns {Promise}
 */
module.exports = async function yaWaitForNewGalleryMessage(text: string, options: IMessageOptions) {
    const selector = '.yamb-message-content .yamb-message-gallery .yamb-message-text .text';
    const errorMessage = `Не найдено новое сообщение - галерея с текстом "${text}"`;

    await this.yaWaitForNewMessage(selector, text, errorMessage, options);
};
