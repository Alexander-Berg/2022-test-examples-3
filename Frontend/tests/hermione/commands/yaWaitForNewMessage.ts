declare interface IMessageOptions {
    type?: 'receive' | 'send',
    waitForSend?: boolean,
    timeout?: number,
    index?: number,
    reply?: boolean,
    fileName?: string,
    status?: string,
}

/**
 * Ожидаем сообщение
 *
 * @param {String} contentSelector - css-селектор контента сообщения
 * @param {String} contentText - проверка на ожидаемый текст сообщения (пустая строка означает отстутсиве проверки)
 * @param {String} errorMessage - сообщение, которое будет показано в случае ошибки
 * @param {Object} options
 * @param {String} [options.type=receive|send] - тип сообщения - входящее/исходящее
 * @param {Boolean} [options.waitForSend=false] - ожидать отправки сообщения на сервер
 * @param {Number} [options.timeout=10000] - таймаут ожидания, по умолчанию 10 секунд
 * @param {Number} [options.index=0] - индекс сообщения от 0 - последнее 1 - предпосленее и т.п.
 *
 * @returns {Promise}
 */

module.exports = async function yaWaitForNewMessage(
    contentSelector: string,
    contentText: string = '',
    errorMessage: string,
    options = {} as IMessageOptions
) {
    const { type = 'receive', waitForSend = false, timeout = 10000 } = options;

    if (type !== 'receive' && type !== 'send') {
        throw new Error(`Parameter "type" must be "receive" or "send", "${type}" given`);
    }

    if (type === 'receive') {
        // eslint-disable-next-line no-console
        console.warn('yaWaitForNewMessage: Ожидаем получение сообщения от собеседника');
    }

    const contentSelectorWithContains = function (text) {
        if (text) {
            return this.yaGetContainsSelector(contentSelector, text);
        }

        return contentSelector;
    }.bind(this);

    await this.waitUntil(async () => {
        const typeModifier = type === 'receive' ?
            ':not(.yamb-message-row_own)' :
            '';

        const selector = `.yamb-message-row${typeModifier}`;

        return this.execute(function (expectedSelector, messageSelector, waitForSendMessage, index = 0) {
            const getMessageInfoIcon = function (messageEl) {
                const messageIconEl = messageEl && messageEl.querySelector('.yamb-message-info svg');

                return messageIconEl && messageIconEl.querySelector('use').getAttribute('xlink:href').substr(6);
            };

            const messages = document.querySelectorAll(messageSelector);
            const lastMessage = messages[messages.length - 1 - index];
            const isMessageSent = getMessageInfoIcon(lastMessage) !== 'time';

            return Boolean(lastMessage && lastMessage.querySelector(expectedSelector)) &&
                (!waitForSendMessage || isMessageSent);
        }, await contentSelectorWithContains(contentText), selector, waitForSend, options.index);
    }, timeout, errorMessage);
};
