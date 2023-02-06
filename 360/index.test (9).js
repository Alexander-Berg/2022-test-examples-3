'use strict';

beforeEach(() => {
    jest.resetModules();
});

test('transforms global config', () => {
    jest.doMock('./global-config.js', () => require('./__mocks__/u2709-conf.json'));

    const globalConfig = require('./index.js');
    const result = globalConfig.get([]);

    expect(result).toMatchSnapshot();
});

test('handles empty config', () => {
    jest.doMock('./global-config.js', () => ({}));

    const globalConfig = require('./index.js');
    const result = globalConfig.get([]);

    expect(result).toMatchSnapshot();
});
