'use strict';

jest.unmock('@yandex-int/duffman');
const ModelError = require('./model-error.js');

const { CUSTOM_ERROR } = require('@yandex-int/duffman').errors;

const request = { name: 'modelName', params: {} };
const result = new CUSTOM_ERROR('error message');

test('creates error response', () => {
    const subject = new ModelError(request, result);

    expect(subject).toMatchSnapshot();
});

test('adds extra', () => {
    const subject = new ModelError(request, result, {
        xRequestId: 'deadbeef'
    });

    expect(subject).toMatchSnapshot();
});
