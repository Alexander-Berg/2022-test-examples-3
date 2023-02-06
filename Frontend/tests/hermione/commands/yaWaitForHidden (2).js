/**
 * Обёртка над стандартной командой waitForVisible. Позволяет указывать произвольное сообщение об ошибке.
 * Чтобы дождаться скрытия элемента webdriver.io предлагает использовать waitForVisible(selector, timeout, true),
 * что выглядит непонятно.
 *
 * @param {String} selector - Селектор для элемента, исчезновение которого нужно ждать
 * @param {Number} [timeout] - Таймаут в миллисекундах
 * @param {String} [message] - Сообщение об ошибке
 *
 * @returns {Promise}
 */

module.exports = function yaWaitForHidden(selector, timeout, message) {
    if (typeof timeout === 'string') {
        message = timeout;
    }

    if (typeof timeout !== 'number') {
        timeout = this.options.waitforTimeout;
    }

    return this.waitForVisible(selector, timeout, true).catch(e => {
        if (message && e.type === 'WaitUntilTimeoutError') {
            throw new Error(message);
        }

        throw e;
    });
};
