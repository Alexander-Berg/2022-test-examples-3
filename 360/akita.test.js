'use strict';

const {
    CUSTOM_ERROR,
    HTTP_ERROR
} = require('@yandex-int/duffman').errors;

const {
    ApiError,
    requireInstance
} = require('../../../moe.js');

let akita;
const got = jest.fn();
const core = { got };

beforeAll(() => {
    akita = requireInstance(require.resolve('./akita.yaml'));
});

beforeEach(() => {
    got.mockReset();
});

test('akita happy path', async () => {
    got.mockResolvedValue('ok');
    const result = await akita.call({ core });
    expect(result).toBe('ok');
});

test('akita error AUTH_NO_AUTH', async () => {
    const error = new CUSTOM_ERROR();
    error.error = { code: 2001 };
    got.mockRejectedValue(error);

    expect.hasAssertions();
    try {
        await akita.call({ core });
    } catch (error) {
        expect(error).toBeInstanceOf(ApiError);
        expect(error).toHaveProperty('code', 'AUTH_NO_AUTH');
    }
});

test('akita error AUTH_UNKNOWN', async () => {
    const error = new CUSTOM_ERROR();
    error.error = { code: 13, message: 'oops', reason: 'fun' };
    got.mockRejectedValue(error);

    expect.hasAssertions();
    try {
        await akita.call({ core });
    } catch (error) {
        expect(error).toBeInstanceOf(ApiError);
        expect(error).toHaveProperty('code', 'AUTH_UNKNOWN');
        expect(error).toHaveProperty('message', 'oops');
        expect(error).toHaveProperty('data', 'fun');
    }
});

test('akita error AUTH_WRONG_GUARD', async () => {
    const error = new CUSTOM_ERROR();
    error.error = { code: 2021 };
    got.mockRejectedValue(error);

    expect.hasAssertions();
    try {
        await akita.call({ core });
    } catch (error) {
        expect(error).toBeInstanceOf(ApiError);
        expect(error).toHaveProperty('code', 'AUTH_WRONG_GUARD');
    }
});

test('akita http error', async () => {
    const error = new HTTP_ERROR();
    error.body = {};
    got.mockRejectedValue(error);

    expect.hasAssertions();
    try {
        await akita.call({ core });
    } catch (error) {
        expect(error).toBeInstanceOf(ApiError);
        expect(error).toHaveProperty('code', 'AUTH_UNKNOWN');
    }
});
