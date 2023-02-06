(function(global) {
    var stubsStash = {},
        constsStubs = {
            dmParams: {
                campaign: 'm-campaign',
                group: 'm-group',
                banner: 'm-banner'
            },
            currencies: {
                KZT: {
                    MIN_AUTOBUDGET_AVG_CPA: 3,
                    MALICIOUS_MIN_CAMP_PRICE_BORDER: 5,
                    MAX_AUTOBUDGET_CLICKS_BUNDLE: 1300000000,
                    MIN_DAY_BUDGET: 1300,
                    LIST_ORDER: 6,
                    MAX_PRICE: 13000,
                    MAX_TOPAY_SUGGEST: 65000,
                    DEFAULT_AUTOBUDGET: 6500,
                    MIN_AUTOBUDGET_CLICKS_BUNDLE: 100,
                    MIN_PRICE: 1,
                    MIN_AUTOBUDGET_BID: 1,
                    MONEYMETER_TYPICAL_MIDDLE_SUM_INTERVAL_BEGIN: 4000,
                    ISO_NUM_CODE: 398,
                    MONEYMETER_TYPICAL_MIDDLE_SUM_INTERVAL_END: 8000,
                    MIN_PRICE_FOR_MFA: 5,
                    MIN_IMAGE_PRICE:10,
                    name: 'тенге',
                    BALANCE_CURRENCY_NAME: 'KZT',
                    MONEYMETER_MAX_MIDDLE_SUM: 65000,
                    full_name: 'казахстанские тенге',
                    MONEYMETER_MIDDLE_PRICE_MIN: 650,
                    AUCTION_STEP: 1,
                    MIN_CAMP_PRICE_BORDER: 91,
                    MIN_PAY: 1300,
                    DIRECT_DEFAULT_PAY: 6500,
                    MIN_TRANSFER_MONEY: 1300,
                    MIN_AUTOBUDGET_AVG_PRICE: 3,
                    MAX_AUTOBUDGET: 1300000000,
                    MIN_AUTOBUDGET: 1300,
                    MAX_AUTOBUDGET_BID: 13000,
                    MIN_CPC_CPA_PERFORMANCE: 10,
                    AUTOBUDGET_AVG_CPA_WARNING: 130000,
                    AUTOBUDGET_PAY_FOR_CONVERSION_AVG_CPA_WARNING: 5000
                },
                YND_FIXED: {
                    MIN_AUTOBUDGET_AVG_CPA: 0.03,
                    MALICIOUS_MIN_CAMP_PRICE_BORDER: 0.05,
                    MAX_AUTOBUDGET_CLICKS_BUNDLE: 1000000000,
                    MIN_DAY_BUDGET: 10,
                    LIST_ORDER: 1,
                    MAX_PRICE: 84,
                    MAX_TOPAY_SUGGEST: 500,
                    DEFAULT_AUTOBUDGET: 50,
                    MIN_AUTOBUDGET_CLICKS_BUNDLE: 100,
                    MIN_PRICE: 0.01,
                    MIN_AUTOBUDGET_BID: 0.01,
                    MONEYMETER_TYPICAL_MIDDLE_SUM_INTERVAL_BEGIN: 30,
                    ISO_NUM_CODE:null,
                    MONEYMETER_TYPICAL_MIDDLE_SUM_INTERVAL_END: 60,
                    MIN_PRICE_FOR_MFA: 0.05,
                    MIN_IMAGE_PRICE: 0.1,
                    name: 'у.е.',
                    BALANCE_CURRENCY_NAME: null,
                    MONEYMETER_MAX_MIDDLE_SUM: 500,
                    full_name: 'условные единицы',
                    MONEYMETER_MIDDLE_PRICE_MIN: 5,
                    AUCTION_STEP: 0.01,
                    MIN_CAMP_PRICE_BORDER: 0.7,
                    MIN_PAY: 10,
                    DIRECT_DEFAULT_PAY: 50,
                    MIN_TRANSFER_MONEY: 10,
                    MIN_AUTOBUDGET_AVG_PRICE: 0.03,
                    MAX_AUTOBUDGET: 10000000,
                    MIN_AUTOBUDGET: 10,
                    MAX_AUTOBUDGET_BID: 84,
                    MIN_CPC_CPA_PERFORMANCE: 0.1,
                    AUTOBUDGET_AVG_CPA_WARNING: 1000,
                    AUTOBUDGET_PAY_FOR_CONVERSION_AVG_CPA_WARNING: 5000
                },
                RUB: {
                    MIN_AUTOBUDGET_AVG_CPA: 0.9,
                    MALICIOUS_MIN_CAMP_PRICE_BORDER: 1.5,
                    MAX_AUTOBUDGET_CLICKS_BUNDLE: 1000000000,
                    MIN_DAY_BUDGET: 300,
                    LIST_ORDER: 2,
                    MAX_PRICE: 2500,
                    MAX_TOPAY_SUGGEST: 15000,
                    DEFAULT_AUTOBUDGET: 1500,
                    MIN_AUTOBUDGET_CLICKS_BUNDLE: 100,
                    MIN_PRICE: 0.3,
                    MIN_AUTOBUDGET_BID: 0.3,
                    MONEYMETER_TYPICAL_MIDDLE_SUM_INTERVAL_BEGIN: 800,
                    ISO_NUM_CODE: 643,
                    MONEYMETER_TYPICAL_MIDDLE_SUM_INTERVAL_END: 1600,
                    MIN_PRICE_FOR_MFA: 1.5,
                    MIN_IMAGE_PRICE: 3,
                    name: 'руб.',
                    BALANCE_CURRENCY_NAME: null,
                    MONEYMETER_MAX_MIDDLE_SUM: 13000,
                    full_name: 'российские рубли',
                    MONEYMETER_MIDDLE_PRICE_MIN: 150,
                    AUCTION_STEP: 0.1,
                    MIN_CAMP_PRICE_BORDER: 21,
                    MIN_PAY: 300,
                    DIRECT_DEFAULT_PAY: 1500,
                    MIN_TRANSFER_MONEY: 300,
                    MIN_AUTOBUDGET_AVG_PRICE: 0.9,
                    MAX_AUTOBUDGET: 300000000,
                    MIN_AUTOBUDGET: 300,
                    MAX_AUTOBUDGET_BID: 2500,
                    MIN_CPC_CPA_PERFORMANCE: 3,
                    AUTOBUDGET_AVG_CPA_WARNING: 30000,
                    AUTOBUDGET_PAY_FOR_CONVERSION_AVG_CPA_WARNING: 5000
                }
            }
        };

    u.register({

        /**
         * Возвращает DOM-дерево из bemjson
         * @param {BEMJSON} template bemjson
         * @returns {jQuery}
         */
        getDOMTree: function(template) {
            return $(BEMHTML.apply(template));
        },

        'attribution-model': {
            isNewPolicyEnabled: function () {
                return true;
            }
        },

        campaign: {
            isInternal: function() {
                return false;
            }
        },

        getHelpUrl: function() {
            return;
        },

        /**
         * Создание блока
         * @param {BEMJSON} template bemjson
         * @param {Object} [settings]
         * @param {String} [settings.sandboxElement] bemjson блока, в который будет помещён создаваемый блок
         * @param {Boolean} [settings.inject=false] помещать ли блок на страницу
         * @param {Boolean} [settings.hidden=true] будет ли показан блок при помещении на страницу
         * @returns {BEM.DOM}
         */
        createBlock: function(template, settings) {
            var domElem = BEM.DOM.init(u.getDOMTree(template)),
                sandboxElem;

            settings = u._.extend({
                sandboxElement: null,
                inject: false,
                hidden: true
            }, settings);

            if (settings.inject) {
                settings.hidden && domElem.css({ visibility: 'hidden', position: 'absolute', top: 0, left: -65555 });

                // при вставке в DOM блоков, domElem которых является td/tr
                // для ненарушения валидности нужно определять sandbox контейнер table/td
                // так же для отладки тестов может потребоваться поместить блок
                // в специальным образом подготовленный контейнер
                settings.sandboxElement && (sandboxElem = u.getDOMTree(settings.sandboxElement));

                domElem.appendTo(sandboxElem || document.body);

                sandboxElem && sandboxElem.appendTo(document.body);
            }

            return domElem.bem(template.block);
        },
        /**
         * Извлекает из запроса тело запроса
         * @param request
         * @returns {Object|String}
         */
        getRequestBody: function(request) {
            var requestBody = request.requestBody;

            if (request.method.toUpperCase() == 'POST') {
                requestBody = /^application\/json;*?/.test(request.requestHeaders['Content-Type']) ?
                    JSON.parse(requestBody) :
                    decodeURIComponent(requestBody)
            }

            return requestBody;
        },
        /**
         * Регистрирует ответы фейкового сервера по указанным настройкам роутинга.
         * Обычным образом неудобно фейкать сервер, когда в одном тест-кейсе происходит несколько запросов,
         * возникает путаница и как результат снижается поддерживаемость и надежность теста.
         * В этой утилите перед выполнением тест-кейсов происходит регистрирация обработчиков на возможные запросы
         * через `server.responseWith`, а далее в тест-кейсах достаточно ответить на запрос через `server.respond()`
         * @param {Object} server инстанс фэйкового сервера
         * @param {Array[]} routes массив массивов с параметрами роутинга
         * @example
         *  // в beforeEach
         *  u.respondJSONWithRoutes(sandbox.server, [
         *      ['POST', '/save', function(xhr) {
         *          var result = u.getRequestBody(xhr);
         *
         *          result.saved = true;
         *
         *          return result;
         *      }],
         *      ['GET', /\/get\/by-id/, function(xhr) {
         *          return {
         *              id: xhr.url.match(/[\?&]item_id=([^&]+)/)[1],
         *              data: {}
         *          };
         *      }]
         *  ]);
         *
         *  // в тест-кейсах для того, чтобы ответить на запросы инициированные тестируемым кодом
         *  sandbox.server.respond();
         */
        respondJSONWithRoutes: function(server, routes) {
            var headers = { 'Content-Type': 'application/json' };

            routes.map(u._.spread(function(method, url, body) {
                server.respondWith(method, url, function(xhr) {
                    var code = 200,
                        response;

                    try {
                        response = body.apply(this, arguments);
                    } catch (ex) {
                        code = 500;

                        response = { error: u._.pick(ex, 'message') };
                    }

                    xhr.respond(code, headers, JSON.stringify(response));
                });
            }));
        },

        /**
         * Производит проход по хэшу с модификаторами и вызывает итерируемую функцию
         * Если значение по ключу в хэше является массивом, то итератор вызывается для каждого из его значений
         * @param {Object} mods хэш модификаторов, например: { contents: 'yes', stat: ['clicks', 'ctr'] }
         * @param {Function} iteratee итерируемая функция
         * @param {Object} [context] контекст итерируемой функции
         */
        forOwnMods: function(mods, iteratee, context) {
            u._.forOwn(mods, function(modVal, modName) {
                (Array.isArray(modVal) ? modVal : [modVal]).forEach(function(modVal) {
                    iteratee.call(context, modVal, modName);
                });
            });
        },

        /**
         * Возвращает проинициализорованный экземпляр блока
         * @param {BEMJSON} template template bemjson
         * @param {Boolean} [dontAppendToBody] Флаг говорящий о том что блок в DOM добавлять не нужно
         * @returns {BEM}
         */
        getInitedBlock: function(template, dontAppendToBody) {
            var domElem = u.getDOMTree(template);

            dontAppendToBody || (domElem.appendTo('body'));

            BEM.DOM.init(domElem);

            return domElem.bem(template.block);
        },

        /**
         * Возвращает набор предупреждений из модерации
         * @returns {Object}
         */
        getAdWarningsForStub: function() {
            return {
                abortion: {
                    long_text: 'Есть противопоказания. Посоветуйтесь с врачом. Возможен вред здоровью.',
                    short_text: 'аборты'
                },
                pseudoweapon: {
                    long_text: 'Конструктивно сходные с оружием изделия',
                    short_text: 'не оружие'
                },
                dietarysuppl: {
                    long_text: 'Не является лекарством',
                    short_text: 'БАД'
                },
                tobacco: {
                    long_text: 'Курение вредит вашему здоровью',
                    short_text: 'табак'
                },
                med_services: {
                    long_text: 'Имеются противопоказания. Посоветуйтесь с врачом',
                    short_text: 'мед. услуги'
                },
                age: {
                    is_common_warn: true,
                    variants: [18, 16, 12, 6, 0],
                    'default': 18
                },
                alcohol: {
                    long_text: 'Чрезмерное потребление вредно.',
                    short_text: 'алкоголь'
                },
                baby_food: {
                    long_text: 'Проконсультируйтесь со специалистом. Для питания детей с %d месяцев',
                    short_text: 'детское питание',
                    variants: [12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1],
                    'default': 11
                },
                medicine: {
                    long_text: 'Есть противопоказания. Посоветуйтесь с врачом.',
                    short_text: 'медицина'
                }
            };
        },

        /**
         * Ожидает истинности предиката и выполняет колбек
         * @param {Function} predicate условия, которое нужно ожидать
         * @param {Function} callback колбек на выполнение условия
         * @param {Number} [retryCount] количество попыток
         * @throws 'wait time out' при совершении retryCount неудачных проверок
         */
        waitFor: function(predicate, callback, retryCount) {
            if (retryCount == undefined) retryCount = 1000;

            if (predicate() && retryCount > 0) {
                callback();
            } else {
                if (retryCount === 0) {
                    throw 'wait time out';
                }
                setTimeout(function() {
                    u.waitFor(predicate, callback, retryCount - 1);
                }, 0);
            }
        },

        /**
         * Ожидает выполнения функции spy и делает дня нее restore
         * @param {Sinon.spy} spy
         * @param {Function} callback
         * @param {Number} [retryCount]
         */
        waitForAndRestore: function(spy, callback, retryCount) {
            u.waitFor(function() {
                if (spy.called) {
                    spy.restore();

                    return true;
                } else {
                    return false;
                }
            }, callback, retryCount);
        },
        /**
         * Заменяет в объекте obj свойства соответствующими из stub
         * Восстановить предыдущие значения свойств можно вызвав restore(obj)
         *
         * @param {Object} obj модифицируемый объект
         * @param {Object} stubs хеш замещающий свойств
         */
        stub: function(obj, stubs) {
            var clone = {};

            Object.keys(stubs).forEach(function(key) {
                clone[key] = obj[key];
            });

            $.extend(obj, stubs);

            stubsStash[$.identify(obj)] = clone;
        },

        /**
         * Стабим необходимые для работы блока константы
         * @param {Array} constsNames массив с именами констант
         */
        stubConsts: function(constsNames) {
            sinon.stub(u, 'consts').callsFake(function(name) {
                if (~constsNames.indexOf(name)) return constsStubs[name];
            });
        },

        stubDMParams: function() {
            sinon.stub(u, 'consts').withArgs('dmParams').returns(constsStubs.dmParams);
        },

        restoreConsts: function() {
            //todo@heliarian сбросит стабы всех констант - нужны ли случаи, когда нужно сбрасывать только конкретные констранты
            u.consts.restore()
        },



        /**
         * Стабим u.consts('currencies') информацией по валютам KZT, RUB и YND_FIXED
         * стабим u.consts('fixed_currency_rates') информацией по курсу KZT, RUB по отношению к y.e.
         */
        stubCurrencies: function() {
            sinon.stub(u, 'consts').callsFake(function(name) {
                return u.getCurrenciesForStub(name);
            });

            u.currencies.init({
                currencies:u.consts('currencies'),
                pseudoCurrency: u.consts('pseudo_currency'),
                currencyTextsDescription: u.consts('currency_texts_description'),
                fixedCurrencyRates: u.consts('fixed_currency_rates'),
                locationNameI18N: iget2
            });
        },

        /**
         * Возвращает хэш со тестовыми данными по валютам
         * @returns {constsStubs.currencies|{KZT, YND_FIXED, RUB}}
         */
        getCurrenciesStub: function() {
            return constsStubs.currencies
        },

        /**
         * Возвращает данные по валютам
         * @param {'currencies'|'fixed_currency_rates'|'currencies'} name
         * @returns {Object}
         */
        getCurrenciesForStub: function(name) {
            switch (name) {
                case 'currencies':
                    return constsStubs.currencies;
                case 'fixed_currency_rates':
                    return {
                        YND_FIXED: {
                            KZT: {
                                0: {
                                    without_nds: 100,
                                    with_nds: 112
                                },
                                20150401: {
                                    without_nds: 130,
                                    with_nds: 145.6
                                }
                            },
                            RUB: {
                                20150401: {
                                    without_nds: 25.4237288135593,
                                    with_nds: 30
                                }
                            }
                        }
                    };
                case 'pseudo_currency':
                    return {
                        rate: 30,
                        name: 'руб.',
                        id: 'rub'
                    };
                case "PAY_FOR_CONVERSION_SUM_TO_AVG_CPA_MIN_RATIO":
                    return 20;
            }
        },

        /**
         * Восстанавливает значение BEM.blocks['i-global']._params.currencies и
         * BEM.blocks['i-global']._params.fixed_currency_rates
         */
        restoreCurrencies: function() {
            u.consts.restore();
        },

        /**
         * Восстанавливает оригинальное состояние объекта: до применения к нему stub
         * @param {Object} obj восстанавливаемый объект
         */
        restore: function(obj) {
            u._.extend(obj, stubsStash[$.identify(obj)]);
        },

        /**
         * Возвращает разобранное по ключам тело POST запроса
         * @param {String} str
         * @returns {Object}
         */
        parseRequestBody: function(str) {
            return str.split('&').reduce(function(params, param) {
                var paramSplit = param.split('=').map(function(value) {
                    return decodeURIComponent(value.replace('+', ' '));
                });

                params[paramSplit[0]] = paramSplit[1];

                return params;
            }, {});
        }
    });
}(window));
