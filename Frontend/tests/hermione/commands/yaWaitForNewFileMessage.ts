/**
 * Ожидаем сообщение с файлом
 *
 * @param {Object} options
 * @param {String} [options.type=receive|send] - тип сообщения - входящее/исходящее
 * @param {Boolean} [options.waitForSend] - ожидать отправки сообщения на сервер
 * @param {Number} [options.timeout] - таймаут ожидания, по умолчанию 10 секунд
 * @param {Number} [options.fileName] - имя файла
 *
 * @returns {Promise}
 */
module.exports = async function yaWaitForNewFileMessage(options = {} as IMessageOptions) {
    const selector = options.fileName ? '.yamb-message-file .yamb-message-file__name' : '.yamb-message-file';

    await this.yaWaitForNewMessage(selector, options.fileName, 'Не найдено новое сообщение с файлом', options);
};
