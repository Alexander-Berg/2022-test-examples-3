'use strict';

let core;
const service = require('./scan.js');

beforeEach(() => {
    core = {
        config: {
            services: {
                scan: 'http://scan'
            }
        },
        got: jest.fn().mockResolvedValue({})
    };
});

test('идет в сервис scan', async () => {
    await service(core, '/method');

    expect(core.got.mock.calls[0][0]).toEqual('http://scan');
});

test('идет в сервис scan с правильными опциями', async () => {
    const params = {
        foo: 'bar'
    };

    await service(core, '/method', params);

    expect(core.got.mock.calls[0]).toMatchSnapshot();
});
