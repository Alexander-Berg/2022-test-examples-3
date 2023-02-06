const INITIAL_DELAY = 100; // Время перед первой проверкой метрки
const RETRY_INTERVAL = 300; // Время ожидания между повторными проверками метрик
const POLL_TIMEOUT = 30 * 1000; // Максимальное время, после которого проверка метрик будет считаться неудачной

/**
 * @param {Object.<String, Number>} expectedMetrics - ожидаемые метрики
 *
 * @returns {Promise}
 */
module.exports = function(expectedMetrics) {
    return this.checkMetrics(
        expectedMetrics,
        { initialDelay: INITIAL_DELAY, retryDelay: RETRY_INTERVAL, timeout: POLL_TIMEOUT }
    );
};
