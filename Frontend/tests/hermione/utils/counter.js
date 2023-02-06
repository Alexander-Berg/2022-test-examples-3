const _ = require('lodash');
const BlockStat = require('./blockstat');
const POS_KEY = 'pos';
const VALID_POS_STRING_VALUES = ['top', 'upper', 'important'];
const IGNORED_COUNTER_FIELDS = ['ignoreVars', 'varPatterns', 'message'];
// Счётчики с неверным форматом секции vars
const DIRTY_COUNTERS_PREFIXES = ['/tech/timing', '/tech/raw-timing', '/tech/ajax', '/tech/perf'];
const IGNORE_VARS_FIELDS = [
    '-mc',
    '-js-ready',
];

/**
 * Декодирует счётчик: расшифровывает path и vars счётчика, применяя опциональную фильтрацию с исключением wildcard
 *
 * @param {CounterObject} counter - счётчик
 * @param {CounterOptions} [options] - доп.опции
 *
 * @returns {CounterObject}
 */
function decode(counter, options) {
    options || (options = {});

    if (counter.path) {
        counter.path = BlockStat.path(counter.path);

        if (DIRTY_COUNTERS_PREFIXES.some((pref) => counter.path.startsWith(pref))) {
            return counter;
        }
    }

    const ignoreVars = [].concat(ensureArray(options.ignoreVars)).map((v) => isNaN(v) ? v : BlockStat.token(v));

    if (!counter.vars) {
        return counter;
    }

    counter.vars = _.pickBy(BlockStat.vars(counter.vars), (value, key) => {
        // Исключаем wildcard
        if (value === '*') {
            return false;
        }

        // Исключаем ignore-лист
        return ignoreVars.indexOf(key) < 0;
    });

    return counter;
}

/**
 * Сравнить сработавший и ожидаемый счетчики
 *
 * @param {CounterObject} triggered - сработавшие счётчики
 * @param {CounterObject} expected - ожидаемые счётчики
 * @param {CounterOptions} [options] - доп.опции
 *
 * @returns {boolean}
 */
function compare(triggered, expected, options) {
    const cmp = options && options.softCompare || !process.env.HERMIONE_COUNTERS_STRICT ?
        _.isMatch :
        _.isEqual;

    // Пока баобабные данные приходят в специальном варсе, исключаем их из ожидаемого счётчика для этой проверки.
    expected = _.omit(expected, ['event', 'behaviour']);

    let triggeredData = _.pick(triggered, Object.keys(expected));

    // Поле -baobab-event-json проверяется отдельно
    triggeredData = _.omit(triggeredData, ['-baobab-event-json']);

    return cmp(triggeredData, expected);
}

/**
 * Вырезает служебные поля, которые должны игнорироваться при сравнении счётчиков
 *
 * @param {CounterObject} counter - счётчик
 *
 * @returns {CounterObject}
 */
function clean(counter) {
    counter = _.omit(counter, [].concat(
        IGNORED_COUNTER_FIELDS,
        counter.url && typeof counter.url !== 'string' ? 'url' : [],
    ));

    if (counter.vars) {
        counter.vars = _.omit(counter.vars, IGNORE_VARS_FIELDS);
    }

    return counter;
}

/**
 * Валидирует значение pos
 *
 * @param {string} val - номер позиции от 0 до 49
 *
 * @returns {boolean}
 */
function validatePosVal(val) {
    if (!val) {
        return false;
    }

    val = val.startsWith('p') ? val.slice(1) : val;

    const num = parseInt(val, 10);

    if (isNaN(num)) {
        return VALID_POS_STRING_VALUES.indexOf(val) !== -1;
    }

    return num >= 0 && num <= 49;
}

/**
 * @param {*} val - значение
 *
 * @returns {Array}
 */
function ensureArray(val) {
    if (typeof val === 'undefined') {
        return [];
    }

    if (!Array.isArray(val)) {
        return [val];
    }

    return val;
}

/**
 * @param {FoundCounter} counter - счётчик
 * @param {CounterOptions} [options] - опции
 *
 * @returns {string}
 */
function foundCounterToMessage(counter, options) {
    const txt = JSON.stringify(counter.expected);
    const allowMultipleTriggering = options && options.allowMultipleTriggering;

    if (counter.count === 0) {
        return `не сработал счетчик ${txt}`;
    }

    if (counter.count > 1 && !allowMultipleTriggering) {
        return `ожидаемый счетчик ${txt} сработал ${counter.count} раз вместо 1:\n` +
            JSON.stringify(counter.triggered, null, 4);
    }
}

module.exports = {
    /**
     * expose
     */
    decode,

    compare,

    /**
     * Подготавливает к сравнению сработавшие счетчики
     *
     * @param {CounterObject|CounterObject[]} triggered - сработавшие счётчики
     * @param {CounterObject[]} expected - ожидаемые счётчики
     * @param {CounterOptions} [options] - опции
     *
     * @returns {CounterObject[]}
     */
    prepareTriggeredCounters(triggered, expected, options) {
        options || (options = {});

        return ensureArray(triggered).map((tCounter) => {
            const decodedTriggered = decode(tCounter, options);
            const expectedWithSamePath = _.find(expected, (item) => item.path === decodedTriggered.path);

            if (!expectedWithSamePath) {
                return decodedTriggered;
            }

            if (decodedTriggered.vars) {
                decodedTriggered.vars = _.omit(decodedTriggered.vars, IGNORE_VARS_FIELDS);
            }

            // нас не просили явно проверять pos, поэтому мы только проверим его значение на валидность
            if (!(expectedWithSamePath.vars && expectedWithSamePath.vars[POS_KEY])) {
                const triggeredPos = decodedTriggered.vars && decodedTriggered.vars[POS_KEY];

                if (triggeredPos) {
                    this.checkPos(triggeredPos);
                    delete decodedTriggered.vars[POS_KEY];
                }
            }

            return decodedTriggered;
        });
    },

    /**
     * Подготавливает к сравнению ожидаемые счетчики
     *
     * @param {CounterObject|CounterObject[]} expected - ожидаемые счётчики
     * @param {CounterOptions} [options] - опции
     *
     * @returns {CounterObject[]}
     */
    prepareExpectedCounters(expected, options) {
        return ensureArray(expected).map((eCounter) => clean(decode(eCounter, options)));
    },

    /**
     * Проверяет валидность значения pos
     *
     * @param {string} posVal - значение pos
     *
     * @throws если значение невалидно
     */
    checkPos(posVal) {
        assert.isTrue(
            validatePosVal(posVal),
            `Значение ${POS_KEY} в vars должно быть в диапазоне p0-p49 или {,p}top, {,p}upper, {,p}important\n` +
            `Сработавшие счётчики имеют значение ${POS_KEY}=${posVal}`,
        );
    },

    /**
     * Проверяет формат переменных счетчика
     *
     * @param {CounterObject} counter - счётчик
     * @param {CounterOptions} [options] - опции
     */
    checkVars(counter, options) {
        if (!options || !options.varPatterns) {
            return;
        }

        const vars = counter.vars;

        assert.isDefined(vars, `В проверяемом счётчике ${counter.path} нет переменных`);

        Object.keys(options.varPatterns).forEach((varName) => {
            const varValue = vars[varName];
            const varPattern = options.varPatterns[varName];

            assert.isDefined(varValue, `У переменной ${varName} не задано значение`);

            assert.match(varValue, varPattern,
                `Переменная счетчика ${varName}=${varValue} не соответствует формату ${varPattern}`);
        });
    },

    /**
     * @param {CounterObject[]} triggered - сработавшие счетчики
     * @param {CounterObject[]} expected - ожидаемые счетчики
     *
     * @returns {FoundCounter[]}
     */
    findCounters(triggered, expected) {
        return expected.map((eCounter) => {
            const filtered = _.filter(triggered, (item) => compare(item, eCounter));

            return { triggered: filtered, count: filtered.length, expected: eCounter };
        });
    },

    /**
     * Проверяет количество срабатываний счетчика
     *
     * @param {FoundCounter[]} found - найденные счётчики
     * @param {CounterObject[]} triggered - сработавшие счетчики
     * @param {CounterOptions} [options] - опции
     *
     * @returns {boolean}
     */
    isValid(found, triggered, options) {
        options || (options = {});

        return options.allowMultipleTriggering ?
            this.expectedCountersTriggeredAtLeastOnce(found) :
            this.expectedCountersTriggeredOnce(found);
    },

    /**
     * Проверяет, что каждый ожидаемый счетчик сработал только один раз
     *
     * @param {FoundCounter[]} found - найденные счётчики
     *
     * @returns {boolean}
     */
    expectedCountersTriggeredOnce(found) {
        return found.every((fCounter) => fCounter.count === 1);
    },

    /**
     * Проверяет, что каждый ожидаемый счетчик сработал как минимум один раз
     *
     * @param {FoundCounter[]} found - найденные счётчики
     *
     * @returns {boolean}
     */
    expectedCountersTriggeredAtLeastOnce(found) {
        return found.every((fCounter) => fCounter.count >= 1);
    },

    /**
     * Формирует сообщение о падении
     *
     * @param {CounterObject[]} triggered - сработавшие счётчики
     * @param {FoundCounter[]} found - найденные счётчики
     * @param {string} [message] - кастомное сообщение
     * @param {CounterOptions} [options] - опции
     *
     * @returns {string}
     */
    getFailMessage(triggered, found, message, options) {
        const res = [];

        if (process.env.CLIENT_COUNTERS_VERBOSE || process.env.SERVER_COUNTERS_VERBOSE) {
            res.push(`все сработавшие счетчики: ${JSON.stringify(triggered)}`);
        }

        if (message) {
            res.push(message);
        }

        return res.concat(found.map((fCounter) => foundCounterToMessage(fCounter, options)).filter(Boolean)).join('\n');
    },

    /**
     * Валидирует и возвращает сработавшие счётчики из числа найденных
     *
     * @param {FoundCounter[]} found - найденные счётчики
     * @param {CounterOptions} [options] - опции
     *
     * @throws Если не сработала валидация по varPatterns
     *
     * @returns {CounterObject[]}
     */
    getValidTriggered(found, options) {
        return _.flatten(found.map((fCounter) => {
            fCounter.triggered.forEach((counter) => this.checkVars(counter, options));

            return fCounter.triggered;
        }));
    },
};

/**
 * @typedef {Object} CounterObject
 *
 * @property {string} path - путь счётчика в формате '/path/to/my/snippet' или '254.4.18.22.167'
 * @property {Object} [vars] - переменные счётчика в формате: {source: 'wizard', '-item': '0'}
 * @property {string} [url] - разобранный URL залогированного счётчика
 *
 * Для `yaCheckCounter`, `yaCheckServerCounter`
 * доступны все дополнительные поля из счетчиков.
 * Самое распространенное - `url` для редир-счётчиков
 * @see https://github.yandex-team.ru/search-interfaces/hermione-get-counters/blob/master/README.md#Технические-подробности
 */

/**
 * @typedef {Object} CounterOptions
 *
 * @property {string|string[]} [ignoreVars=[]] - Переменные (vars) счетчика, которые исключаются из сравнения
 * @property {Object<RegExp>} [varPatterns] - Регулярки для валидации значений переменных
 * @property {boolean} [allowMultipleTriggering=false] - Разрешить ли множественное срабатывание счетчика
 * @property {string} [reqid] - reqid, для которого необходимо найти срабатывание счетчиков
 * @property {boolean} [softCompare=false] - не строгое сравнение счетчиков
 */

/**
 * @typedef {Object} FoundCounter
 *
 * @property {CounterObject[]} triggered сработавшие счетчики
 * @property {CounterObject} expected ожидаемый счетчик
 * @property {Number} count сколько раз сработал счетчик
 */
