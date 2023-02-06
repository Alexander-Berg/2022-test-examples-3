/**
 * @see https://github.yandex-team.ru/search-interfaces/frontend/blob/master/services/wiki/tests/hermione/commands/yaWaitForHidden.js
 */

/**
 * Обёртка над стандартной командой waitForVisible. Позволяет указывать произвольное сообщение об ошибке.
 * Чтобы дождаться скрытия элемента webdriver.io предлагает использовать waitForVisible(selector, timeout, true),
 * что выглядит непонятно.
 *
 * @param {string} selector - Селектор для элемента, исчезновение которого нужно ждать
 * @param {number} [timeout] - Таймаут в миллисекундах
 *
 * @returns {Promise}
 */
module.exports = function yaWaitForHidden(selector, timeout) {
  if (typeof timeout !== 'number') {
    timeout = this.options.waitforTimeout;
  }

  return this.waitForVisible(selector, timeout, true);
};
