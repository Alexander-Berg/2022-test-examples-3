const _ = require('lodash');

const IGNORE_ATTRS = [
    'oldPath',
    'oldVars'
];

/**
 * Найти узел в объекте по пути path
 *
 * @param {String} path - ожидаемый path
 * @param {Object} object - object tree
 * @param {Object} attrs - ожидаемые attrs
 *
 * @returns {*}
 */
function query(path, object, attrs) {
    const parts = path.split('.');
    let findNodes = [];

    if (parts[0] === '*' || parts[0] === object.name) {
        const innerPath = parts.slice(1).join('.');

        // Достигли конца path
        if (!innerPath) {
            if (attrs) {
                return compareAttrs(object.attrs, attrs) ? [object] : [];
            }

            return [object];
        }

        for (let node of object.children || []) {
            findNodes = findNodes.concat(query(innerPath, node, attrs));
        }
    }

    return findNodes;
}

function compareAttrs(attrs, expectedAttrs) {
    const ignoreExpectedKeys = Object
        .keys(expectedAttrs)
        .filter(key => expectedAttrs[key] === '*');

    expectedAttrs = _.omit(expectedAttrs, ignoreExpectedKeys);
    attrs = _.omit(attrs, [].concat(IGNORE_ATTRS, ignoreExpectedKeys));

    return _.isEqual(attrs, expectedAttrs);
}

/**
 * Проверить, что счётчик соответствует баобабному формату
 *
 * @todo фейлить проверку после увеличения покрытия https://st.yandex-team.ru/SERP-63934
 *
 * @param {CounterObject} triggered - сработавшие счётчики
 * @param {BaobabDataAttrs} dataAttrs - data атрибуты Baobab'а
 * @param {CounterObject} expected - ожидаемые счётчики
 */
function validateCounter(triggered, dataAttrs, expected) {
    // проверяем нередиры (полный формат)
    if (_.get(triggered, 'vars.-baobab-event-json')) {
        let event;

        try {
            event = JSON.parse(triggered.vars['-baobab-event-json'])[0];
        } catch (e) {
            console.error(e);
            throw new Error(`Счетчик ${JSON.stringify(triggered)} содержит событие не в баобабном формате`);
        }

        if (event === undefined) {
            throw new Error(`Счетчик ${JSON.stringify(triggered)} содержит событие не в баобабном формате`);
        }

        validateFull(triggered, event, dataAttrs, expected);

    // проверяем редиры (сейчас отправляется только node id в параметре bu)
    } else if (triggered.bu) {
        validateId(triggered, triggered.bu, dataAttrs);
    }
}

/**
 * Проверка объекта с баобабным событием.
 *
 * @param {CounterObject} triggered - сработавшие счётчики
 * @param {BaobabEvent} event - Baobab событие
 * @param {BaobabDataAttrs} dataAttrs - data атрибуты Baobab'а
 * @param {CounterObject} expected - ожидаемые счётчики
 */
function validateFull(triggered, event, dataAttrs, expected) {
    validateId(triggered, event.id, dataAttrs);
    validateEvent(triggered, event, expected, dataAttrs);
    validateRestAttrs(triggered, event, expected);
}

/**
 * Проверяет равенство id из дата-атрибута и id, полученное на клиенте.
 *
 * @param {CounterObject} triggered - сработавшие счётчики
 * @param {String} id - id счётчика
 * @param {BaobabDataAttrs} dataAttrs - data атрибуты Baobab'а
 */
function validateId(triggered, id, dataAttrs) {
    if (!id) {
        throw new Error(`Счетчик ${JSON.stringify(triggered)} не содержит ID ноды баобаба`);
    }

    assert.equal(id, dataAttrs.node, `ID в счётчике на клиенте не совпадает с ID в data-атрибуте для счетчика ${JSON.stringify(triggered)}`);
}

/**
 * Проверяет корректность события, отправляемого с клиентским счётчиком.
 *
 * @param {CounterObject} triggered - сработавшие счётчики
 * @param {BaobabEvent} event - Baobab событие
 * @param {CounterObject} expected - ожидаемые счётчики
 * @param {BaobabDataAttrs} dataAttrs - data атрибуты Baobab'а
 */
function validateEvent(triggered, event, expected, dataAttrs) {
    const expectedEventName = expected.event || 'click';

    assert.equal(event.event, expectedEventName, `Счетчик ${JSON.stringify(triggered)} не содержит ожидаемый тип события баобаба`);

    if (dataAttrs.event) {
        assert.equal(event.event, dataAttrs.event, `Тип баобабного события на клиенте не совпадает с типом в data-атрибуте для счетчика ${JSON.stringify(triggered)}`);
    }
}

/**
 * Проверяет остальные поля из события, отправляемого с клиента.
 *
 * @param {CounterObject} triggered - сработавшие счётчики
 * @param {BaobabEvent} event - Baobab событие
 * @param {CounterObject} expected - ожидаемые счётчики
 */
function validateRestAttrs(triggered, event, expected) {
    if (expected.behaviour) {
        assert.deepEqual(event.behaviour, expected.behaviour, `Счетчик ${JSON.stringify(triggered)} содержит неправильный behaviour в баобабных данных`);
    }

    assert.isNumber(event.cts, `В счетчике ${JSON.stringify(triggered)} отсутствует cts в баобабных данных`);
}

module.exports = {
    query,
    validateCounter
};

/**
 * Значения data-атрибутов, которыми размечаются DOM-ноды в Баобабе.
 *
 * @typedef {Object} BaobabDataAttrs
 *
 * @property {String} node - data-log-node
 * @property {String} event - data-log-event
 */
/**
 * Распаршенная переменная -baobab-event-json
 *
 * @typedef {Object} BaobabEvent
 * @see https://wiki.yandex-team.ru/baobab/rfc/common
 *
 * @property {String} event - имя события
 * @property {String} id - id узла логирования
 * @property {Object} behaviour — объект с полем type=click|scroll|..., см. ссылку на Вики
 * @property {String} cts - таймстемп
 */
