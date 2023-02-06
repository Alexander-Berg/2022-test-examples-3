'use strict';

const httpMock = require('node-mocks-http');
const TestCore = require('./helpers/test-core.js');

beforeEach(function() {
    // nock debugging
    // process.env.DEBUG = 'nock.*';

    this.sinon = require('sinon').createSandbox();
    this.nock = require('nock');
    this.nock.disableNetConnect();

    // Хэлпер метод для стаба свойств объекта
    this.stubProperty = (obj, name, value) => {
        this.__stubbedProps__ = this.__stubbedProps__ || [];

        if (name in obj) {
            this.sinon.stub(obj, name, value);
        } else {
            obj[name] = undefined;
            this.sinon.stub(obj, name, value);
            this.__stubbedProps__.push(() => {
                delete obj[name];
            });
        }
    };

    return stubMl(this);
});

afterEach(function() {
    this.sinon.restore();
    this.nock.cleanAll();

    clearTestContext(this);
});

/**
 * Стабим
 */
function stubMl(sandbox) {
    const request = httpMock.createRequest({
        headers: {
            'x-original-host': 'mail.yandex.ru',
            'x-original-uri': '/u2709/api/models',
            'x-real-ip': '2a02:6b8::25',
            'x-https-request': 'yes',
            'cookie': '123'
        },
        body: {
            _connection_id: '1',
            _ckey: 'Gb1ZeTCNDfadbGuBWOzOzQ=='
        }
    });

    const response = httpMock.createResponse();

    sandbox.core = new TestCore(request, response);

    // sinon не умеет стабить методы, доступ к которым происходит через get/set
    // Object.keys(Object.getPrototypeOf(sandbox.core.services)).forEach(function(key) {
    //     Object.defineProperty(sandbox.core.services, key, {
    //         value: sandbox.core.services[key],
    //         writable: true,
    //         configurable: true,
    //         enumerable: true
    //     });
    // });

    // эмулируем авторизованность
    sandbox.core.auth.set({
        mdb: 'mdb1',
        suid: '34',
        timezone: 'Europe/Moscow',
        tz_offset: -180,
        uid: '12',
        userTicket: 'tvm-user-ticket',
        users: []
    });
    sandbox.sinon.stub(sandbox.core.ckey, 'check');

    sandbox.CoreCtor = TestCore;
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
        if (Object.prototype.hasOwnProperty.call(context, property)) {
            delete context[property];
        }
    }
}
