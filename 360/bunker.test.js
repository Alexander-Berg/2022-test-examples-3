'use strict';

let core;
const service = require('./bunker.js');

beforeEach(() => {
    core = {
        config: {
            services: {
                bunker: 'http://bunker'
            }
        },
        got: jest.fn().mockResolvedValue(''),
        req: {}
    };
    service.clearCache();
});

test('идет в сервис bunker', async () => {
    await service(core, '/status', {});
    expect(core.got).toHaveBeenCalledWith('http://bunker/mailfront/status', { json: true });
});

test('не идёт в сервис если кеш валидный', async () => {
    await service(core, '/status', { ttl: 1000 });
    await service(core, '/status', { ttl: 1000 });
    expect(core.got).toHaveBeenCalledTimes(1);
});

test('возвращает данные из кеша в случае ошибки', async () => {
    core.got.mockRejectedValue('error').mockResolvedValueOnce('ok');
    await service(core, '/status', {});
    const result = await service(core, '/status', {});
    expect(result).toBe('ok');
    expect(core.got).toHaveBeenCalledTimes(2);
});

test('кидает ошибку, если в кеше пусто', async () => {
    core.got.mockRejectedValue('error');
    expect.hasAssertions();
    try {
        await service(core, '/status', {});
    } catch (e) {
        expect(e).toBe('error');
    }
});
