/**
 * Функция для организации стабов/моков аяксовых запросов.
 * Реализована с помощью подмены оригинального метода open из XMLHttpRequest
 * Перехватывает запросы по подходящим урлам (задаются в опциях) и отдаёт необходимые данные без сетевого запроса
 * Все остальные запросы осуществляются как обычно
 * Каждый повторный вызов может добавлять новые доопределения аяксовых запросов
 *
 * @param {Object} options - параметры перехвата запросов
 * @param {Number} [options.status] - имитируемый код ответа (по умолчанию 200)
 * @param {Number} [options.timeout = 0] - задержка ответа (по умолчанию 0)
 * @param {Record<String, any>} [options.urlDataMap] - хеш,
 *      ключи — строки-регулярные выражения для поиска совпадающего урла (например, ^https?://bs.yandex.ru/page/[0-9]+)
 *      значение — замоканый ответ сервера. Если будет передана не строка, то значение будет конвертировано в строку с
 *          помощью JSON.stringify
 * @param {Record<String, String>} options.urlRedirectMap - хеш редиректов,
 *      ключи — строки-регулярные выражения для поиска совпадающего урла (например, ^https?://bs.yandex.ru/page/[0-9]+)
 *      значение — строка для применения в функции <String>.replace
 * @param {String[]} options.recordData - массив урлов, запросы по которым нужно отслеживать.
 *      Собирает объект параметров запроса в window.hermione['mock-xhr-records']
 *      элементы — строки-регулярные выражения для поиска совпадающего урла
 */
window.hermione.injectXhrMock = function(options) {
    var MOCK_RECORDS = 'mock-xhr-records';

    var i,
        key,
        dataRegexps,
        redirectRegexps,
        recordRegexps,
        dataMapKeys = Object.keys(options && options.urlDataMap || {}),
        redirectMapKeys = Object.keys(options && options.urlRedirectMap || {}),
        recordKeys = options && options.recordData || [];

    if (!dataMapKeys.length && !redirectMapKeys.length && !recordKeys.length) {
        return;
    }

    dataRegexps = generateRegExps(dataMapKeys);
    redirectRegexps = generateRegExps(redirectMapKeys);
    recordRegexps = generateRegExps(recordKeys);

    options = Object.assign({
        status: 200,
        timeout: 0,
        response: '',
    }, options);

    // сохраняем предыдущую версию функции,
    // так что каждый повторный вызов функции позволяет добавлять новые доопределения аяксовых запросов
    var originalOpen = window.XMLHttpRequest.prototype.open;

    window.XMLHttpRequest.prototype.open = function(method, url, async) {
        var i,
            regexp,
            key,
            newUrl,
            result,
            shouldRecord,
            regExpMatch;

        for (i = 0; (i < dataMapKeys.length) && !result; i++) {
            key = dataMapKeys[i];
            regexp = dataRegexps[key];

            if (regexp && (regExpMatch = regexp.exec(url))) {
                result = options.urlDataMap[key];

                if (typeof result === 'function') {
                    result = result(regExpMatch);
                }
            }
        }

        recordKeys.forEach(function(key) {
            regexp = recordRegexps[key];

            if (regexp && (regExpMatch = regexp.exec(url))) {
                shouldRecord = true;
            }
        });

        if (!result && !shouldRecord) {
            for (i = 0; i < redirectMapKeys.length; i++) {
                key = redirectMapKeys[i];
                regexp = redirectRegexps[key];

                if (regexp && regexp.test(url)) {
                    newUrl = url.replace(regexp, options.urlRedirectMap[key]);
                    // редиректим на новый урл
                    return originalOpen.call(this, method, newUrl);
                }
            }

            // если нет отображения урла в данные, то продолжаем обычное исполнение
            return originalOpen.apply(this, arguments);
        }

        Object.defineProperty(this, 'timeout', {
            enumerable: true,
            configurable: true,
            // если не переопределить, то в IE получаем InvalidStateError - там нельзя устанавливать таймаут
            //   до реального вызова open
            set: function() {},
        });

        Object.defineProperty(this, 'setRequestHeader', {
            enumerable: true,
            configurable: true,
            // если не переопределить, то в IE получаем InvalidStateError - там нельзя устанавливать таймаут
            //   до реального вызова open
            get: function() {
                return function() { };
            },
        });

        // Иначе подменяем функцию, возвращающую данные
        this.send = function(body) {
            var self = this;
            var rawResultIsString = typeof result === 'string';
            var responseResult = rawResultIsString ? result : JSON.stringify(result);
            var headers = {};

            if (!rawResultIsString) {
                // Заголовок необходим для jQuery обработчиков ajax запроса
                headers['content-type'] = 'application/json; charset=utf-8';
            }

            if (shouldRecord) {
                var records = window.hermione[MOCK_RECORDS];
                if (!records) {
                    records = [];
                    window.hermione[MOCK_RECORDS] = records;
                }

                records.push({
                    url: url,
                    method: method,
                    body: body,
                });
            }

            Object.defineProperty(self, 'getResponseHeader', {
                enumerable: true,
                configurable: true,
                get: function() {
                    return function(name) {
                        name = String(name).toLowerCase();

                        if (headers.hasOwnProperty(name)) {
                            return headers[name];
                        }

                        return null;
                    };
                },
            });
            Object.defineProperty(self, 'getAllResponseHeaders', {
                enumerable: true,
                configurable: true,
                get: function() {
                    return function() {
                        return Object.keys(headers).reduce(function(all, header) {
                            all.push(header + ': ' + headers[header] + '\r\n');
                            return all;
                        }, []).join('');
                    };
                },
            });
            Object.defineProperty(self, 'status', {
                enumerable: true,
                configurable: true,
                // просто значение установить нельзя, нужен именно геттер, иначе свойство не меняется
                get: function() {
                    return options.status;
                },
            });
            Object.defineProperty(self, 'response', {
                enumerable: true,
                configurable: true,
                get: function() {
                    return responseResult;
                },
            });
            Object.defineProperty(self, 'responseText', {
                enumerable: true,
                configurable: true,
                get: function() {
                    return responseResult;
                },
            });
            Object.defineProperty(self, 'readyState', {
                enumerable: true,
                configurable: true,
                get: function() {
                    return 4;
                },
            });

            if (async !== false) {
                setTimeout(function() {
                    if (typeof self.onload === 'function') {
                        self.onload();
                    } else if (typeof self.onreadystatechange === 'function') {
                        self.onreadystatechange();
                    }
                }, options.timeout);
            }
        };
    };

    function generateRegExps(keys) {
        var resultMap = {};

        for (i = 0; i < keys.length; i++) {
            key = keys[i];
            resultMap[key] = new RegExp(key, 'i');
        }

        return resultMap;
    }
};
