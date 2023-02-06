/* eslint-disable max-len */
'use strict';

const { camelCase } = require('lodash');

const Core = require('./index.js').default;

const Auth = require('./auth.js').default;
const Ckey = require('./ckey.js');
const Console = require('./console.js');
const Params = require('./params.js');

const EXTENDABLE_CLASSES = [ Auth, Ckey, Console, Params ];

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

describe('в Core должны быть все extendable классы', () => {
    it.each(EXTENDABLE_CLASSES.map(c => [ c.name, c ]))('%s', (name, Class) => {
        expect(Core[name]).toBe(Class);
    });
});

describe('должен правильно инициализировать extendable классы если параметр classes не передан при создании экземляра core', () => {
    const core = new Core(request, response);
    it.each(EXTENDABLE_CLASSES.map(c => [ c.name, c ]))('%s', (name, Class) => {
        expect(core[camelCase(name)]).toBeInstanceOf(Class);
    });
});

describe('не должен ничего переопределять если в classes есть контрукторы, но не extendable классов', () => {
    const core = new Core(request, response, { Test: class { test() {} } });
    it.each(EXTENDABLE_CLASSES.map(c => [ c.name, c ]))('%s', (name, Class) => {
        expect(core[camelCase(name)]).toBeInstanceOf(Class);
    });
});

describe('#constructor', function() {
    it('должен переопределять extendable классы если параметр classes передан при создании экземляра core', () => {
        const AuthNew = class extends Auth { test() {} };
        const core = new Core(request, response, { Auth: AuthNew });

        expect(core.auth).toBeInstanceOf(Auth);
        expect(core.auth).toBeInstanceOf(AuthNew);
        expect(core.auth).toHaveProperty('test', expect.any(Function));
    });

    it('не должен переопределять если в classes есть конструктор extendable класса, но от этого класса не наследуется', () => {
        const AuthPlain = class { test() {} };
        const core = new Core(request, response, { Auth: AuthPlain });

        expect(core.auth).toBeInstanceOf(Auth);
        expect(core.auth).not.toBeInstanceOf(AuthPlain);
        expect(core.auth).not.toHaveProperty('test');
    });
});

describe('core.params in Ckey', () => {
    it('works', () => {
        expect.assertions(1);
        class CustomCkey extends Ckey {
            constructor(core, options) {
                super(core, options);
                expect(core.params).toEqual({ _connection_id: '1', _ckey: 'Gb1ZeTCNDfadbGuBWOzOzQ==' });
            }
        }
        new Core(request, response, { Ckey: CustomCkey });
    });
});
