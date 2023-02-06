const _ = require('lodash');
const fs = require('fs');
const webDriverIO = require('webdriverio');

/**
 * Парсит vars из строки a=b,c=d в объект {a: b, c: d}
 *
 * @param {String|Object} vars - варсы счётчка
 *
 * @returns {Object}
 */
function parseVarsToObject(vars) {
    return _.fromPairs(vars.split(',').map(v => v.split('=')));
}

module.exports = class BlockStat {
    /**
     * Возвращает полное название файла со словарём
     *
     * @returns {String}
     */
    static get file() {
        return 'blockstat.dict.json';
    }

    /**
     * Возвращает список пар токен,значение из словаря
     *
     * @returns {Array[]}
     */
    static get dictionary() {
        return BlockStat._dictionary ||
            (BlockStat._dictionary = JSON.parse(fs.readFileSync(BlockStat.file)));
    }

    /**
     * Устанавливает кастомный словарь
     *
     * @param {Array[]} dict - словарь
     *
     */
    static set dictionary(dict) {
        BlockStat._dictionary = dict;
    }

    /**
     * Возвращает обратную карту значение:токен из словаря
     *
     * @returns {Map}
     */
    static get valuesMap() {
        return BlockStat._valuesMap ||
            (BlockStat._valuesMap = new Map(BlockStat.dictionary.map(pair => pair.slice().reverse())));
    }

    /**
     * Приводит путь к раскодированному виду
     *
     * @param {String} path - путь счётчика
     *
     * @returns {String}
     */
    static path(path) {
        // технические счетчики могут отсутствовать в блокстат-словаре
        if (path.startsWith('/') || path === 'tech' || path.startsWith('tech.')) {
            return path;
        }

        return '/' + path.split('.').map(BlockStat.token).join('/');
    }

    /**
     * Приводит сложное значение к раскодированному виду, например
     * 143 => page
     * 28.1034.153 => ru/web3/search
     *
     * @param {String} complex - закодированный путь или токен
     *
     * @returns {String}
     */
    static complex(complex) {
        try {
            if (!complex.match(/^(\d{1,4}\.)*\d{1,4}$/)) {
                return complex;
            }

            return complex.split('.').map(BlockStat.token).join('/');
        } catch (e) {
            return complex;
        }
    }

    /**
     * Приводит vars'ы к раскодированному виду и превращает в объект, если необходимо
     * Если ключ начинается с '-', не расшифровываем через BlockStat словарь
     *
     * @param {String|Object} vars - варсы счётчика
     *
     * @returns {Object}
     */
    static vars(vars) {
        const parsed = typeof vars === 'string' ? parseVarsToObject(vars) : vars;

        return _.transform(parsed, (res, val, key) => {
            // Если ключ НЕ начинается с '-', то расшифровываем
            if (key.startsWith('-')) {
                // в vars может быть что угодно, так что действуем осторожно
                try {
                    res[key] = decodeURIComponent(val);
                } catch (e) {
                    res[key] = val;
                }
            } else {
                res[BlockStat.complex(key)] = BlockStat.complex(val);
            }
        });
    }

    /**
     * Возвращает токен из словаря по его закодированному значению
     *
     * @param {String} value - закодированное значение токена
     *
     * @throws {ErrorHandler} Исключение, если токен не был найден словаре по его значению
     *
     * @returns {String}
     */
    static token(value) {
        if (BlockStat.valuesMap.has(value)) {
            return BlockStat.valuesMap.get(value);
        }

        throw new webDriverIO.ErrorHandler('CommandError', `Токен для значения '${value}' не найден в словаре ` +
            `'${BlockStat.file}'.\n\nПроверьте ключ на опечатки и обновите словарь:\n\n\tmake blockstat.dict.json -B`);
    }
};
