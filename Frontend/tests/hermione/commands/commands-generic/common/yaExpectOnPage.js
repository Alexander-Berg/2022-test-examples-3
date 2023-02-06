const stringifyExpectation = require('../../libs/expectation-base').stringify;
const DEFAULT_TIMEOUT = 10000;

/**
 * Выполняется в контексте браузера
 * В теле функции нужно использовать только ES5 из-за плохой поддержки современного JS в браузерах
 *
 * @param {String[]} stringifiedExpectations - expectations
 *
 * @returns {Object}
 */
function checkExpectations(stringifiedExpectations) {
    return stringifiedExpectations.reduce(function(result, serializedMeta) {
        let meta = JSON.parse(serializedMeta);
        // eslint-disable-next-line no-new-func
        let check = new Function('return (' + meta.condition + ').apply(null, arguments)');

        result[meta.id] = check();

        return result;
    }, {});
}

/**
 * Выполняет проверки сразу после загрузки страницы
 *
 * @param {ExpectationsConfig} config - Конфигурация обязательных проверок, исполняемых в контексте браузера
 * @param {Number} [timeout=10000] - Время ожидания загрузки страницы в миллисекундах
 *
 * @returns {Promise}
 */
module.exports = function yaExpectOnPage(config, timeout) {
    const expectations = config.create(this);

    timeout = typeof timeout === 'number' ? timeout : DEFAULT_TIMEOUT;

    let fulfillResult;

    return this.waitUntil(function() {
        return this.execute(checkExpectations, expectations.map(stringifyExpectation))
            .then(result => fulfillResult = result.value)
            .then(() => expectations.every(expectation => fulfillResult[expectation.id] === true));
    }, timeout, 'PageLoadTimeout')
        .catch(e => {
            if (e.message === 'PageLoadTimeout') {
                for (let i = 0; i < expectations.length; i++) {
                    let expectation = expectations[i];

                    if (!fulfillResult[expectation.id]) {
                        return expectation.onFail(e);
                    }
                }
            }

            throw e;
        });
};
