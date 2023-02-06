const ErrorHandler = require('webdriverio').ErrorHandler;

/**
 * Обёртка над стандартной командой waitForVisible. Позволяет указывать произвольное сообщение об ошибке.
 *
 * @param {String} selector - Селектор для элемента, появление которого нужно ждать
 * @param {Number} [timeout] - Таймаут в миллисекундах
 * @param {String} [message] - Сообщение об ошибке
 * @param {Boolean} [reverse] - Если true - ждет сокрытия
 *
 * @returns {Promise}
 */

module.exports = function yaWaitForVisible(selector, timeout, message, reverse = false) {
    if (typeof timeout === 'string') {
        message = timeout;
    }

    if (typeof timeout !== 'number') {
        timeout = this.options.waitforTimeout;
    }

    return this.waitForVisible(selector, timeout, reverse)
        .catch(e => {
            if (message && e.type === 'WaitUntilTimeoutError') {
                throw new ErrorHandler('CommandError', message);
            }

            throw e;
        });
};
