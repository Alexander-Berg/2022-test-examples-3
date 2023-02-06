'use strict';

let core;
const service = require('./staff.js');

beforeEach(() => {
    core = {
        config: {
            secrets: {
                staffToken: 'AAAABABA'
            },
            services: {
                staff: 'http://staff'
            }
        },
        got: jest.fn().mockResolvedValue({}),
        req: {}
    };
});

test('идет в сервис staff', async () => {
    await service(core, '/method');

    expect(core.got.mock.calls[0][0]).toEqual('http://staff/method');
});

test('идет в сервис staff с правильными опциями', async () => {
    const params = {
        foo: 'bar'
    };

    await service(core, '/method', params);

    expect(core.got.mock.calls[0][1]).toMatchSnapshot();
});
