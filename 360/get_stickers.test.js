'use strict';

const method = require('./get_stickers.js');
const ApiError = require('../../../routes/helpers/api-error.js');

let core;

const stickersMock = [
    {
        mid: '1',
        tid: '2',
        fid: '3',
        created: 42,
        date: 43,
        type: 'FAKE_TYPE',
        foo: 'bar'
    },
    {
        mid: '41',
        tid: '42',
        fid: '43',
        created: 42,
        date: 43,
        type: 'FAKE_TYPE',
        foo: 'bar'
    }
];

beforeEach(() => {
    core = {
        params: {},
        request: jest.fn()
    };
});

test('вызывает stickers', async () => {
    core.params = { type: 'reply_later' };
    core.request.mockResolvedValueOnce({ stickers: stickersMock });

    await method.call(core);

    expect(core.request).toHaveBeenCalledWith('stickers', { type: 'reply_later' });
});

test('отвечает', async () => {
    core.params = { type: 'reply_later' };
    core.request.mockResolvedValueOnce({ stickers: stickersMock });

    const result = await method.call(core);

    expect(result).toMatchSnapshot();
});

test('проверяет схему', async () => {
    expect.assertions(3);

    try {
        await method.call(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('invalid params schema');
    }
});
