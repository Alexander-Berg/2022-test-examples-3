'use strict';

const DollarConfig = require('dollar-config');

const mockGlobalConfig = new DollarConfig({
    test: {
        url: 'http://test',
        options: {
            $switch: [
                'options.path', [
                    [
                        '/message',
                        {
                            timeout: 10000,
                            retryOnTimeout: 1
                        }
                    ],
                    [
                        '$default',
                        {
                            dnsCache: false,
                            timeout: 2000,
                            retryOnTimeout: 2
                        }
                    ]
                ]
            ]
        }
    }

});
jest.mock('../../config/global-config/index.js', () => mockGlobalConfig);

const BaseHttpService = require('./base-http-service.js');

let service;
let got;

beforeEach(() => {
    const config = new DollarConfig({
        url: 'http://test2',
        options: { timeout: 100 },
        errors: {}
    });
    service = new BaseHttpService(config, 'service/test.yaml');
    got = jest.fn().mockResolvedValue(42);
});

test('merge default options', async () => {
    await service.fetch({ core: { got }, headers: {} }, { path: '/path' });
    expect(got).toHaveBeenCalledWith('http://test/path', expect.objectContaining({
        _http_log: expect.any(Object),
        headers: expect.any(Object),
        dnsCache: false,
        timeout: 2000,
        retryOnTimeout: 2
    }));
});

test('merge method options', async () => {
    const options = {
        path: '/message',
        timeout: 5000
    };

    await service.fetch({ core: { got }, headers: {} }, options);
    expect(got).toHaveBeenCalledWith('http://test/message', expect.objectContaining({
        timeout: 5000,
        retryOnTimeout: 1
    }));
});

test('should not serialize Buffer', async () => {
    const ctx = { core: { got }, headers: {} };
    const buffer = Buffer.from('_BUFFER_');
    await service.call(ctx, { path: '/', body: buffer });
    await service.call(ctx, { path: '/', body: buffer, json: true });
    await service.call(ctx, { path: '/', body: buffer, form: true });
    await service.call(ctx, { path: '/', body: buffer, json: true, form: true });
    got.mock.calls.forEach(([ , options ]) => {
        expect(options.body).toBeInstanceOf(Buffer);
    });
});

test('coverage', async () => {
    const ctx = { core: { got }, headers: {} };
    await service.call(ctx, { path: '/', body: 12 });
    await service.call(ctx, { path: '/', body: {} });
    await service.call(ctx, { path: '/', body: {}, json: true });
    await service.call(ctx, { path: '/', body: {}, form: true });
    await service.call(ctx, { path: '/', body: {}, json: true, form: true });
    expect(1).toBe(1);
});

test('no global config', async () => {
    const config = new DollarConfig({
        url: 'http://test2',
        options: { timeout: 100 },
        errors: {}
    });
    const service = new BaseHttpService(config, 'service/test2.yaml');
    const got = jest.fn().mockResolvedValue(42);

    await service.fetch({ core: { got }, headers: {} }, { path: '/path' });
    expect(got).toHaveBeenCalledWith('http://test2/path', expect.objectContaining({
        _http_log: expect.any(Object),
        headers: expect.any(Object),
        timeout: 100
    }));
});

test('got error', async () => {
    got.mockRejectedValue(13);
    service.catch = (e) => e;
    expect(await service.fetch({ core: { got }, headers: {} }, { path: '/path' })).toBe(13);
});
