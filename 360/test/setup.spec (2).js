'use strict';

/**
 * Очищает контекст после тестов
 * DARIA-37332
 */
const clearTestContext = function(context) {
    if (!context || typeof context !== 'object') {
        return;
    }

    for (var property in context) {
        if (context.hasOwnProperty(property)) {
            delete context[property];
        }
    }
};

// Заглушки для глобальных объектов (на window).
window.i18n = function() {};
window.Daria = {
    Config: {},
    IS_CORP: false,
    messages: {
        params4SimpleLabel: function() {},
        params4SimplePath: function() {},
        folder4MessagePage: function() {}
    },
    promoNow: () => {},
    isDevEnv: () => false,
    allowUnsubscribeFiltersPopup: () => {},
    memoizeById: () => {},
    hasFeature: () => {},
    hasRocksSuggest: () => {},
    memoizeClearCache: () => {},
    canShowYandexPlusButtonUpgrade: () => {},
    isSearchPage: () => {},
    isMessageOpenedInList: () => {},
    isMessagesListPage: () => {},
    addParamsToLocation: () => {},
    getLinkToMailPro: () => {},
    hasReactPsHeader: () => {},
    getFeatureParams: () => {},
    Constants: {
        DATASYNC: {}
    }
};
require('modules/common/js/components/html2text.js');

window.Jane = {
    c: function() {},
    Promo: {
        add: () => {}
    },
    watcher: {
        get: () => {}
    }
};
window.vow = {
    Promise: Promise
};
window.Vow = {
    Promise: Promise,
    resolve: Promise.resolve.bind(Promise),
    reject: Promise.reject.bind(Promise)
};

window.$ = function() {};

// импорты могут содержать всякие сайд-эффекты, рассчитывающие на наличие чего-то из окружения,
// поэтому импортируем тут
const { setupNoscript } = require('./setup-noscript');
const { setupApp } = require('./setup-app');

beforeEach(function() {
    window.requestAnimationFrame = function(cb) {
        cb();
    };

    this.sinon = sinon.sandbox.create();

    const requests = this.requests = [];
    this.xhr = this.sinon.useFakeXMLHttpRequest();
    this.xhr.onCreate = function(xhr) {
        requests.push(xhr);
    };

    this.sinon.stub(window, 'Ya').value({
        Rum: {
            logError: this.sinon.stub(),
            ERROR_LEVEL: {
                INFO: 'info',
                DEBUG: 'debug',
                WARN: 'warn',
                ERROR: 'error',
                FATAL: 'fatal'
            }
        }
    });

    setupNoscript.call(this);

    this.setupApp = setupApp;
});

afterEach(function() {
    this.sinon.restore();
    clearTestContext(this);

    delete window.requestAnimationFrame;
});
