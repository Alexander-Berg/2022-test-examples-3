'use strict';

const Config = require('../config.js');
const server = require('../index.js');
const ExtraCore = require('../routes/helpers/extra-core.js');

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
    const Core = ExtraCore;

    const request = {
        cookies: {},
        headers: {
            'x-original-host': 'mail.yandex.ru',
            'x-original-uri': '/u2709/api/models',
            'x-real-ip': '2a02:6b8::25',
            'x-https-request': 'yes',
            'cookie': '123'
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
    sandbox.coreConfigs = new Config(sandbox.core);

    Object.assign(sandbox.core, {
        services: server.services,
        models: server.models,
        config: sandbox.coreConfigs
    });

    // эмулируем авторизованность
    sandbox.core.auth.set({
        mdb: 'mdb1',
        suid: '34',
        timezone: 'Europe/Moscow',
        tz_offset: -180,
        uid: '12',
        users: []
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

    if (context.__stubbedProps__) {
        context.__stubbedProps__.forEach((restoreCallback) => restoreCallback());
        delete context.__stubbedProps__;
    }

    for (const property in context) {
        if (context.hasOwnProperty(property)) {
            delete context[property];
        }
    }
}
