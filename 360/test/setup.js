'use strict';

beforeEach(function() {
    this.sinon = require('sinon').createSandbox();
    return stubMl(this);
});

afterEach(function() {
    this.sinon.restore();
    clearTestContext(this);
});

/**
 * Стабим
 */
function stubMl(sandbox) {
    const Core = require('../index.js').Core;

    const request = {
        cookies: {},
        headers: {
            'x-original-host': 'mail.yandex.ru',
            'x-original-uri': '/u2709/api/models',
            'x-real-ip': '2a02:6b8::25'
        },
        query: {},
        body: {
            _connection_id: '1',
            _ckey: 'Gb1ZeTCNDfadbGuBWOzOzQ=='
        }
    };

    const response = {
        cookie: function() {},
        on: function() {}
    };

    sandbox.core = new Core(request, response);

    // эмулируем авторизованность
    sandbox.core.auth.set({
        mdb: 'mdb1',
        suid: '34',
        timezone: 'Europe/Moscow',
        tz_offset: -180,
        uid: '12'
    });
    sandbox.sinon.stub(sandbox.core.ckey, 'check');
}

/**
 * Очищает контекст после тестов
 */
function clearTestContext(context) {
    if (!context || typeof context !== 'object') {
        return;
    }

    for (const property in context) {
        // eslint-disable-next-line no-prototype-builtins
        if (context.hasOwnProperty(property)) {
            delete context[property];
        }
    }
}
