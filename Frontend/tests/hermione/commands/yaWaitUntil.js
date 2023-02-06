/**
 *
 * Команда для того, чтобы в случае падения waitUntil бросать своё сообщение об ошибке.
 *
 * @param {String} message - Сообщение об ошибке
 * @param {Function|Promise} condition - Условие
 * @param {Number} [timeout] - Таймаут в миллисекундах
 * @param {Number} [interval] - Интервал между проверками условия
 *
 * @returns {Promise}
 *
 * @example
 * this.browser
 *  .yaGoUrl({ text: 'ip' })
 *  .yaWaitUntil('не дождались появления IPv4', function() {
 *      return this.execute(function() {
 *              return $('.z-fact__fact_version_v4').text().length !== 0;
 *          })
 *          .then(function(result) {
 *              return result.state === 'success' && result.value === true;
 *          });
 *  });
 */
module.exports = function(message, condition, timeout, interval) {
    return this
        .waitUntil(condition, timeout, interval)
        .catch(function(e) {
            e.message = message + '\n(Original error: ' + e.message + ')';
            throw e;
        });
};
