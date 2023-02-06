'use strict';

const { CUSTOM_ERROR } = require('../helpers/errors/index.js');
const Core = require('./index').default;

let core;

beforeEach(() => {
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

    core = new Core(request, response);

    // эмулируем авторизованность
    core.auth.set({
        mdb: 'mdb1',
        suid: '34',
        timezone: 'Europe/Moscow',
        tz_offset: -180,
        uid: '12'
    });

    core.ckey.check = () => true;

    core.models = {
        'md-1': async () => ({
            'md-1-data': 'md1'
        }),
        'md-2': async () => ({
            'md-2-data': 'md2'
        }),
        'md-3': async () => ({
            'md-3-data': 'md3'
        }),
        'do-md-1': async () => ({
            'do-md1-data': 'do-md1'
        }),
        'do-md-2': async () => ({
            'do-md2-data': 'do-md2'
        }),
        'md-x': async (params) => ({
            'md-x-data': params.x
        }),
        'md-test-cache': async ({ connection_id: ignore, ...params }) => {
            void ignore;
            throw new CUSTOM_ERROR({
                'md-test-cache-data': JSON.stringify(params)
            });
        }
    };
});

test('возвращает модели в нужной последовательности', async () => {
    const result = await core.request([
        { name: 'md-1', params: {} },
        { name: 'md-2', params: {} },
        { name: 'md-3', params: {} }
    ]);

    expect(result).toEqual([
        { 'md-1-data': 'md1' },
        { 'md-2-data': 'md2' },
        { 'md-3-data': 'md3' }
    ]);
});

test('возвращает модели в правильной последовательность (do + regular)', async () => {
    const result = await core.request([
        { name: 'do-md-1', params: {} },
        { name: 'md-1', params: {} }
    ]);

    expect(result).toEqual([
        { 'do-md1-data': 'do-md1' },
        { 'md-1-data': 'md1' }
    ]);
});

test('возвращает модели в правильной последовательность (do + do + regular)', async () => {
    const result = await core.request([
        { name: 'do-md-1', params: {} },
        { name: 'do-md-2', params: {} },
        { name: 'md-1', params: {} }
    ]);

    expect(result).toEqual([
        { 'do-md1-data': 'do-md1' },
        { 'do-md2-data': 'do-md2' },
        { 'md-1-data': 'md1' }
    ]);
});

test('возвращает модели в правильной последовательность (regular + do + do)', async () => {
    const result = await core.request([
        { name: 'md-1', params: {} },
        { name: 'do-md-1', params: {} },
        { name: 'do-md-2', params: {} }
    ]);

    expect(result).toEqual([
        { 'md-1-data': 'md1' },
        { 'do-md1-data': 'do-md1' },
        { 'do-md2-data': 'do-md2' }
    ]);
});

test('не запрашивает повторно модель', async () => {
    await core.request([
        { name: 'do-md-1', params: {} },
        { name: 'md-1', params: {} },
        { name: 'md-2', params: {} }
    ]);

    core.models['md-1'] = async () => ({
        'md-1-data': 'md1-new'
    });

    const result = await core.request({ name: 'md-1', params: {} });

    expect(result).toEqual({
        'md-1-data': 'md1'
    });
});

test('запрашивает повторно модель с другими параметрами', async () => {
    await core.request([
        { name: 'do-md-1', params: {} },
        { name: 'md-1', params: {} },
        { name: 'md-2', params: {} }
    ]);

    core.models['md-1'] = async () => ({
        'md-1-data': 'md1-new'
    });

    const result = await core.request({ name: 'md-1', params: { p: 1 } });

    expect(result).toEqual({
        'md-1-data': 'md1-new'
    });
});

test('не запрашивает больше допустимого числа моделей', async () => {
    const result = await core.request(Array.from({ length: 16 }, (_, idx) => ({
        name: 'md-x',
        params: { x: idx + 1 }
    })));

    const expected = Array.from({ length: 15 }, (_, idx) => ({
        'md-x-data': idx + 1
    }));
    expected.push({ error: 'Too much requests for one session' });

    expect(result).toEqual(expected);
});

test('Если модель впервые запросили в unsafe моде и она зареджектилась, ' +
    'то при запросе в safe моде не должно быть реджекта', async () => {
    expect.assertions(4);
    const params = { test: 'unsafe' };

    try {
        await core.request('md-test-cache', params);
    } catch (err) {
        expect(err).toBeInstanceOf(CUSTOM_ERROR);
        expect(err.error).toEqual({ 'md-test-cache-data': JSON.stringify(params) });
    }

    const result = await core.request.safe('md-test-cache', params);
    expect(result).toBeInstanceOf(CUSTOM_ERROR);
    expect(result.error).toEqual({ 'md-test-cache-data': JSON.stringify(params) });
});

test('если модель впервые запросили в safe моде и вернула ошибку, ' +
    'то при запросе в unsafe моде должен быть реджект', async () => {
    expect.assertions(4);
    const params = { test: 'safe' };

    const result = await core.request.safe('md-test-cache', params);
    expect(result).toBeInstanceOf(CUSTOM_ERROR);
    expect(result.error).toEqual({ 'md-test-cache-data': JSON.stringify(params) });

    try {
        await core.request('md-test-cache', params);
    } catch (err) {
        expect(err).toBeInstanceOf(CUSTOM_ERROR);
        expect(err.error).toEqual({ 'md-test-cache-data': JSON.stringify(params) });
    }
});
