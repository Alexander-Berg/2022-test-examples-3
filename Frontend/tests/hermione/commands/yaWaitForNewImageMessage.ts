/**
 * Ожидаем сообщение с изображением
 *
 * @param {Object} options
 * @param {String} [options.type=receive|send] - тип сообщения - входящее/исходящее
 * @param {Boolean} [options.waitForSend] - ожидать отправки сообщения на сервер
 * @param {Number} [options.timeout] - таймаут ожидания, по умолчанию 10 секунд
 *
 * @returns {Promise}
 */
module.exports = async function yaWaitForNewImageMessage(options = {} as IMessageOptions) {
    const { status } = options;

    let selector = '.yamb-message-image';
    let errMessage = 'Не найдено новое сообщение с изображением';

    if (status) {
        selector = `[data-test-tag="message-image-status-${status}"]`;
        errMessage = `Не найдено новое сообщение с изображением в статусе ${status}`;
    }

    await this.yaWaitForNewMessage(selector, '', errMessage, options);
};
