'use strict';

beforeEach(() => {
    jest.resetModules();
});

test('loads global config', () => {
    jest.doMock('/etc/yamail/u2709-conf.json', () => ({ foo: 1 }), { virtual: true });

    const globalConfig = require('./global-config.js');

    expect(globalConfig).toEqual({ foo: 1 });
});

test('handles file absense', () => {
    jest.doMock('/etc/yamail/u2709-conf.json', () => {
        throw new Error('Cannot find module');
    }, { virtual: true });

    const globalConfig = require('./global-config.js');

    expect(globalConfig).toEqual({});
});
