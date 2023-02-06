'use strict';

let core;
const service = require('./messenger.js');

beforeEach(() => {
    core = {
        config: {
            services: {
                messenger: 'http://messenger'
            }
        },
        got: jest.fn().mockResolvedValue({}),
        auth: {
            get: () => ({
                uid: '12'
            })
        },
        req: {
            tvm: {
                tickets: {
                    messenger: { ticket: 'tvm-service-ticket-messenger' }
                }
            }
        }
    };
});

test('идет в сервис messenger', async () => {
    await service(core, '/method', {});

    expect(core.got.mock.calls[0][0]).toEqual('http://messenger/method');
});

test('идет в сервис messenger с правильными опциями', async () => {
    const params = {
        login: 'example'
    };

    await service(core, '/method', params);

    expect(core.got.mock.calls[0][1]).toMatchSnapshot();
});
