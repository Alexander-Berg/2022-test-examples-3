'use strict';

jest.unmock('@yandex-int/duffman');
const { CUSTOM_ERROR } = require('@yandex-int/duffman').errors;
const ModelResponse = require('./model-response.js');

const request = { name: 'modelName', params: {} };
const result = { foo: 'bar' };

test('creates response', () => {
    const subject = new ModelResponse(request, result);

    expect(subject).toMatchSnapshot();
});

test('creates error response', () => {
    const subject = new ModelResponse(request, new CUSTOM_ERROR('error'));

    expect(subject).toMatchSnapshot();
});

test('adds extra', () => {
    const subject = new ModelResponse(request, result, {
        xRequestId: 'deadbeef',
        foo: 'bar'
    });

    expect(subject).toMatchSnapshot();
});
