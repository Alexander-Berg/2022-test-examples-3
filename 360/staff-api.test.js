'use strict';

let core;
const service = require('./staff-api.js');

beforeEach(() => {
    core = {
        config: {
            services: {
                'staff-api': 'http://staff-api'
            }
        },
        got: jest.fn().mockResolvedValue({}),
        auth: {
            get: () => ({ userTicket: 'user-ticket' })
        },
        req: {
            headers: {
                'cookie': 'cookie',
                'user-agent': 'user-agent'
            }
        }
    };
});

test('идет в сервис staff-api', async () => {
    await service(core, '/method', { a: 1 });

    expect(core.got).toHaveBeenCalledWith('http://staff-api/method', {
        json: true,
        query: {
            a: 1
        },
        headers: {
            'cookie': 'cookie',
            'user-agent': 'user-agent',
            'referer': 'https://mail.yandex-team.ru'
        }
    });
});

test('идет в сервис staff-api c TVM', async () => {
    core.req.tvm = {
        tickets: {
            'staff-api': {
                ticket: 'service-ticket'
            }
        }
    };
    await service(core, '/method', undefined, { headers: { 'x-test': 'test' } });

    expect(core.got).toHaveBeenCalledWith('http://staff-api/method', {
        json: true,
        query: {},
        headers: {
            'x-test': 'test',
            'x-ya-service-ticket': 'service-ticket',
            'x-ya-user-ticket': 'user-ticket'
        }
    });
});
