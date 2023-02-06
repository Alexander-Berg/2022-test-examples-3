/**
 * Ожидаем форвард-сообщение с опционально заданным текстом
 *
 * @param {String} [text=''] - текст сообщения (пустая строка означает отсутствие подписи к форвард-сообщению)
 * @param {Object} options
 * @param {String} [options.type=receive|send] - тип сообщения - входящее/исходящее
 * @param {Boolean} [options.waitForSend] - ожидать отправки сообщения на сервер
 * @param {Number} [options.timeout] - таймаут ожидания, по умолчанию 10 секунд
 *
 * @returns {Promise}
 */
module.exports = async function yaWaitForNewForwardMessage(text: string = '', options = {} as IMessageOptions) {
    let selector = '.yamb-forwarded-messages + .yamb-message-content .yamb-message-text';
    let errorMessage = `Не найдено новое форвард-сообщение с подписью "${text}"`;

    if (options.reply) {
        selector = '[data-test-tag="message-reply"] .yamb-message-content';
        errorMessage = `Не найден новый реплай с подписью "${text}"`;
    }

    await this.yaWaitForNewMessage.call(this, selector, text, errorMessage, options);
};
