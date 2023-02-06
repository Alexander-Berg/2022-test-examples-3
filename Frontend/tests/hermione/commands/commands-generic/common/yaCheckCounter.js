const _ = require('lodash');
const webDriverIO = require('webdriverio');
const firstOrDefault = require('../../utils/').firstOrDefault;
const helpers = require('../../utils/counter');
const baobabHelpers = require('../../utils/baobab');
const RETRY_INTERVAL = 300; // Время ожидания между повторными проверками счетчиков
const POLL_TIMEOUT = 5000; // Максимальное время, после которого проверка счетчиков будет считаться неудачной

/**
 * Проверяет срабатывание клиентских счетчиков (они же счетчики действий, они же редир-счетчики) по редир-логу
 *
 * @param {Function|String} action - Действие, которое необходимо выполнить для срабатывания счётчиков или счётчика
 *                                 Если action - строка, будет выполнен click(action)
 * @param {CounterObject|CounterObject[]} expected - счётчики, которые должны сработать
 * @param {String} [message] - Сообщение, если счётчик не сработал
 * @param {CounterOptions} [options] - опции
 *
 * @returns {Promise}
 */
module.exports = function(action, expected, message, options) {
    let baobabSelector;

    if (typeof action === 'string') {
        let selector = baobabSelector = action;
        action = () => this.click(selector);
    }

    if (!_.isFunction(action)) {
        webDriverIO.ErrorHandler('CommandError', 'Action должен быть типа Function');
    }

    if (_.isPlainObject(message)) {
        options = message;
        message = null;
    }

    if (!baobabSelector && options) {
        baobabSelector = options.baobabSelector;
    }

    expected = helpers.prepareExpectedCounters(expected, options);

    const baobabDataAttrs = {};

    const assertCountersPresent = async reqid => {
        let result;
        await this.assertCounters(
            reqid,
            {
                retryDelay: RETRY_INTERVAL,
                timeout: POLL_TIMEOUT
            },
            allTriggeredCounters => {
                const triggered = helpers.prepareTriggeredCounters(allTriggeredCounters.client, expected, options);
                const found = helpers.findCounters(triggered, expected, options);
                if (!helpers.isValid(found, triggered, options)) {
                    assert.fail(triggered, expected, helpers.getFailMessage(triggered, found, message, options));
                }
                result = helpers.getValidTriggered(found, options);

                if (baobabDataAttrs.node && !(options && options.ignoreBaobab)) {
                    baobabHelpers.validateCounter(result[0], baobabDataAttrs, expected[0]);
                }
            }
        );

        return result;
    };

    // используем Promise, чтобы action мог возвращать Promise или быть обычной функцией
    let reqid;

    return Promise.resolve()
        .then(() => options && options.reqid || this.yaGetReqId())
        .then(res => reqid = res)
        .then(() => {
            // Соответствие ноды в логах и на dom-элементе проверяем только на элементах
            // с атрибутом data-counter
            if (baobabSelector) {
                return this
                    .getAttribute(baobabSelector, 'data-counter')
                    .then(counter => firstOrDefault(counter) && this.getAttribute(baobabSelector, 'data-log-node'));
            }
        })
        .then(id => baobabDataAttrs.node = firstOrDefault(id))
        .then(() => baobabSelector && this.getAttribute(baobabSelector, 'data-log-event'))
        .then(event => baobabDataAttrs.event = firstOrDefault(event))
        .then(action.bind(this))
        .then(() => assertCountersPresent(reqid));
};
