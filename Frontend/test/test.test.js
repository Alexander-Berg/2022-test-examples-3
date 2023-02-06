/* eslint-disable no-unused-vars */
/* eslint-disable no-console */
/**
 * @file
 * Глобальные переменные и хелперы для тестов.
 */
var assert = chai.assert;
sinon.assert.expose(assert, { prefix: '' });

// Заглушки функций логгирования
['logSerpJsError', 'w', 'wb', 'wbt', 'rc'].forEach(function(name) {
    window[name] = $.noop;
});

// Заглушка для серверных BEMHTML шаблонов
window.RequestCtx = { GlobalContext: { expFlags: {}, bannerData: {} } };

/**
 * Застабить методы произвольного объекта
 * @param {Object} obj
 * @param {Object|String|Object[]|String[]} methods (name or { name: handler })
 *
 * @returns {Object} Хелперы для работы со стабами
 */
function stubObjectMethods(obj, methods) {
    methods = [].concat(methods);
    var stubs = {};

    return {
        init: function() {
            methods.forEach(this.add, this);
        },
        /**
         * @param {String|null} methodName освобождает конкретный метод.
         * Если вызывается без параметра освобождает все
         */
        restore: function(methodName) {
            var stub = stubs[methodName];

            if (methodName) {
                stub && stub.restore();
                return;
            }

            Object.keys(stubs).forEach(function(methodName) {
                stubs[methodName].restore();
            });
        },
        add: function(method) {
            switch (typeof method) {
                case 'object':
                    Object.keys(method).forEach(function(name) {
                        stubs[name] = sinon.stub(obj, name).callsFake(method[name]);
                    });
                    break;
                case 'string':
                    stubs[method] = sinon.stub(obj, method);
                    break;
            }
            return false;
        },
        get: function(name) {
            return stubs[name];
        }
    };
}

/**
 * Застабить методы прототипа объекта
 * @param {String} blockName
 * @param {Object|String[]|Object[]} methods (name or { name: handler })
 *
 * @returns {Object} Хелперы для работы со стабами
 */
function stubBlockPrototype(blockName, methods) {
    // stubBlockPrototype обычно вызывается вне тестов, внутри `describe` метода
    // при выкидывании исключения в этом методе, мока полностью игнорирует все тесты этого
    // `describe`, для того что бы показать такие падения мы замалчиваем такие исключения
    try {
        return stubObjectMethods(BEM.DOM.blocks[blockName].prototype, methods);
    } catch (e) {
        console.log('stubBlockPrototype: ошибка при попытке застабать методы ' + blockName, e);
    }
}

/**
 * Застабить статические методы объекта
 * @param {String} blockName
 * @param {Object|String|String[]|Object[]} methods (name or { name: handler })
 *
 * @returns {Object} Хелперы для работы со стабами
 */
function stubBlockStaticMethods(blockName, methods) {
    return stubObjectMethods(BEM.DOM.blocks[blockName], methods);
}

var stubModHandlers = (function() {
    /**
     * Создать стаб для обработчиков onSetMod
     *
     * @param {String} blockName
     * @param {Object} params
     * @param {Object} [params.onSetMod]
     * @param {Object} [params.onElemSetMod]
     *
     * @returns {Object} Хелперы для работы со стабами
     *
     * @example
     * var stubs = stubModHandlers('link', {
     *     onSetMod: { js: function() { console.log('js') } },
     *     onElemSetMod: {
     *         link: {
     *             disabled: {
     *                 yes: function() { console.log('link disabled!') }
     *             }
     *         }
     *     }
     * });
     *
     * // в before/beforeEach:
     * stubs.init();
     *
     * // в after/afterEach:
     * stubs.restore();
     */
    return function(blockName, params) {
        var stubs = {};

        if (params.onSetMod) {
            modFnsToProps(params.onSetMod, stubs);
        }

        if (params.onElemSetMod) {
            $.each(params.onElemSetMod, function(elemName, modFns) {
                modFnsToProps(modFns, stubs, elemName);
            });
        }

        return stubBlockPrototype(blockName, stubs);
    };

    // Функции скопированы из i-bem.js, необходимо следить за их актуальностью
    function modFnsToProps(modFns, props, elemName) {
        var hasOwn = Function.prototype.call.bind(Object.prototype.hasOwnProperty);
        if ($.isFunction(modFns)) {
            (props[buildModFnName(elemName, '*', '*')] = modFns);
            return;
        }

        var modName, modVal, modFn;
        for (modName in modFns) {
            /* istanbul ignore next: for-in guard */
            if (!hasOwn(modFns, modName)) {
                continue;
            }

            modFn = modFns[modName];
            if ($.isFunction(modFn)) {
                props[buildModFnName(elemName, modName, '*')] = modFn;
                continue;
            }

            for (modVal in modFn) {
                /* istanbul ignore next: for-in guard */
                if (!hasOwn(modFn, modVal)) {
                    continue;
                }
                props[buildModFnName(elemName, modName, modVal)] = modFn[modVal];
            }
        }
    }

    function buildModFnName(elemName, modName, modVal) {
        return (elemName ? '__elem_' + elemName : '') +
            '__mod' +
            (modName ? '_' + modName : '') +
            (modVal ? '_' + modVal : '');
    }
})();

/**
 * Хелпер для создания и вставки DOM-блока в body.
 * @param  {String} name
 * @param  {Bemjson} bemjson
 * @returns {BEM} Блок на DOM-элементе
 */
function buildDomBlock(name, bemjson) {
    var node = BEM.DOM.init($(BEMHTML.apply(bemjson)).appendTo('body'));
    Ya.asyncQueue.executeSync();
    return node.bem(name);
}

/**
 * Создать ответ сервера на AJAX-запрос
 * @param {Object} [respData] Дополнительные данные ответа
 * @returns {Object} Ответ с правдоподобной структурой
 */
function stubAjaxResponse(respData) {
    return $.extend(true, {
        // Напр. //betastatic.yastatic.net/web4/STATIC_GIT_HEAD/
        'static-host': BEM.blocks['serp-request'].getStaticHost(),
        status: 200,
        reqid: '123',
        'some-block': { some: 'data' }, // Данные блока some-block, так же пишутся данные других блоков
        serp: {
            params: { // Новые параметры i-global
                reqid: '123',
                clck: '//yandex.ru/clck/safeclick/data=DATA/sign=SIGN/keyno=0',
                query: 'Фото котов',
                staticVersion: BEM.blocks['serp-request'].getStaticVersion(), // Напр. STATIC_GIT_HEAD
                extraParams: { extra: 1 }, // Новые параметры i-global.serpRequestExtraParams
                bundles: 'a', // Закодированный список бандлов, SERP-29247,
                bmt: 'bundlesMetadata',
                amt: 'assetsMetadata'
            }
        }
    }, respData);
}
