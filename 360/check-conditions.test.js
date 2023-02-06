'use strict';

const checkConditions = require('./check-conditions.js');

const conditions = require('./__mocks__/conditions.json');

let core;

beforeEach(() => {
    core = {
        console: {
            error: jest.fn()
        },
        yasm: {
            sum: jest.fn()
        },
        params: {}
    };
});

test('without params', () => {
    expect(checkConditions(core, conditions)).toMatchSnapshot();
});

test('with params', () => {
    core.params.params = {
        lang: { type: 'string', value: 'en' },
        applicationId: { type: 'string', value: 'ru.yandex.mail.beta' }
    };

    expect(checkConditions(core, conditions)).toMatchSnapshot();
});

test('with missing params', () => {
    core.params.params = {
        lang: { type: 'string', value: 'en' }
    };
    expect(checkConditions(core, conditions)).toMatchSnapshot();
    expect(core.console.error.mock.calls).toMatchSnapshot();
    expect(core.yasm.sum.mock.calls).toMatchSnapshot();
});
