const assert = require('chai').assert;

const DEFAULT_WAIT_TIME = 10000;

/**
 * Ждет появление указанного элемента за определенное время, в случае неудачи ассертит ошибку
 *
 * @param {String}  selector  селектор элемента
 * @param {Number}  [ms]      таймаут ожидания в мс (если нет, используется дефолтный для waitForVisible)
 * @param {String}  message   сообщение об ошибке
 * @param {Boolean} reverse   должен быть скрыт
 * @returns {Promise<*>}
 */
module.exports = async function ywWaitForVisible(selector, ms = DEFAULT_WAIT_TIME, message, reverse) {
    if (typeof ms !== 'number') {
        message = ms;
        ms = null;
    }

    return this
        .waitForVisible(selector, ms, reverse)
        .catch(() => {
            assert.fail((message ? message + '. ' : '') +
                `Блок с селектором "${selector}" не ${reverse ? 'скрылся' : 'отобразился'}${ms ? ` после ${ms}мс` : ''}`
            );
        });
};
