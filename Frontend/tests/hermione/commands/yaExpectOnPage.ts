const stringifyExpectation = require('../libs/expectation-base').stringify;
const DEFAULT_TIMEOUT = 10000;

/**
 * Выполняется в контексте браузера
 * В теле функции нужно использовать только ES5 из-за плохой поддержки современного JS в браузерах
 *
 * @param {String[]} stringifiedExpectations - expectations
 *
 * @returns {Object}
 */
function checkExpectations(stringifiedExpectations: string[]) {
    return stringifiedExpectations.reduce(function (result, serializedMeta) {
        const meta = JSON.parse(serializedMeta);
        // eslint-disable-next-line no-new-func
        const check = new Function('return (' + meta.condition + ').apply(null, arguments)');

        result[meta.id] = check();

        return result;
    }, {});
}

interface ExpectationsConfig {
    create: Function,
}

/**
 * Выполняет проверки сразу после загрузки страницы
 *
 * @param {ExpectationsConfig} config - Конфигурация обязательных проверок, исполняемых в контексте браузера
 * @param {Number} [timeout=10000] - Время ожидания загрузки страницы в миллисекундах
 *
 * @returns {Promise}
 */
module.exports = async function yaExpectOnPage(config: ExpectationsConfig, timeout?: number) {
    const expectations = config.create(this);

    timeout = typeof timeout === 'number' ? timeout : DEFAULT_TIMEOUT;

    let fulfillResult;

    try {
        await this.waitUntil(async function () {
            fulfillResult = await this.execute(checkExpectations, expectations.map(stringifyExpectation));
            return expectations.every((expectation) => fulfillResult[expectation.id] === true);
        }, timeout, 'PageLoadTimeout');
    } catch (e) {
        if (e.message === 'PageLoadTimeout') {
            for (let i = 0; i < expectations.length; i++) {
                const expectation = expectations[i];

                if (!fulfillResult[expectation.id]) {
                    return expectation.onFail(e);
                }
            }
        }

        throw e;
    }
};
