'use strict';

let core;
const service = require('./promocodes.js');

beforeEach(() => {
    core = {
        config: {
            services: {
                promocodes: 'http://promocodes'
            },
            USER_IP: '123.123.123.123'
        },
        got: jest.fn().mockResolvedValue({}),
        auth: {
            get: () => ({
                uid: '42'
            })
        }
    };
});

test('идет в сервис promocodes', async () => {
    await service(core, '/method', {});

    expect(core.got.mock.calls[0][0]).toEqual('http://promocodes/method');
});

test('идет в сервис promocodes с правильными опциями', async () => {
    const params = {
        tag: 'tag',
        device_id: 'device_id'
    };

    await service(core, '/assign', params);

    expect(core.got.mock.calls[0][1]).toMatchSnapshot();
});
