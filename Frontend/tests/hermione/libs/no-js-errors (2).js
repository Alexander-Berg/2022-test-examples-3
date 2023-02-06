const Expectation = require('./expectation-base');

function prepareErrorMessage(errors) {
    if (!Array.isArray(errors)) {
        return '';
    }

    return errors
        .map((err, i) => {
            const stackTrace = err.error && err.error.stack;
            let result = `${i + 1}. ${err.message}`;

            if (err.url) {
                result += '\n  в файле ' + err.url;
            }

            if (err.line && err.col) {
                result += `:${err.line}:${err.col}`;
            }

            if (stackTrace) {
                result += `:\n\t${stackTrace}`;
            }

            return result;
        })
        .join('\n');
}

module.exports = class NoJSErrorsExpectation extends Expectation {
    constructor(browser) {
        super(browser, 'Страница содержит ошибки в клиентском коде.');
    }

    /**
     * Проверяет, что в процессе загрузки не было клиентских ошибок.
     * Подписка на непойманные ошибки загружается в assets
     *
     * @override
     */
    // eslint-disable-next-line class-methods-use-this
    validateInBrowserContext() {
        return function () {
            var hermioneErrors = window['hermione-jserrors'];
            var isValid = Array.isArray(hermioneErrors) ? hermioneErrors.length === 0 : true;

            if (isValid) {
                return true;
            }

            window['hermione-jserrors'] = hermioneErrors.map(function (error) {
                if (error.message === 'Script error.') {
                    var message = 'Script Error.\n';
                    var scripts = Array.prototype.slice.call(document.querySelectorAll('script')).filter(function (item) {
                        return item.src;
                    });
                    var withOriginScripts = scripts
                        .filter(function (item) {
                            return item.crossOrigin;
                        })
                        .map(function (item) {
                            return item.src;
                        })
                        .join('\n');
                    var withoutOriginScripts = scripts
                        .filter(function (item) {
                            return !item.crossOrigin;
                        })
                        .map(function (item) {
                            return item.src;
                        })
                        .join('\n');

                    if (withoutOriginScripts.length) {
                        message += '\nОшибка возникла в одном из скриптов: \n[' + withoutOriginScripts + ']\n';
                    }

                    if (withOriginScripts) {
                        message += '\nЛибо один из хостов не выставил заголовок "Access-Control-Allow-Origin" ' +
                            'для скрипта из: \n[' + withOriginScripts + ']';
                    }

                    error.message = message;
                }

                return error;
            });

            return false;
        };
    }

    /**
     * Выбрасывает исключение со списком клиентских ошибок
     *
     * @param {Object} e - объект ошибки
     * @override
     */
    onFail(e) {
        return this.browser
            .execute(function () {
                return window['hermione-jserrors'];
            })
            .then((result) => Expectation.reThrow(e, this.message + '\n' + prepareErrorMessage(result.value)));
    }
};
