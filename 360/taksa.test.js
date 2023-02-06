'use strict';

let core;
const service = require('./taksa.js');

beforeEach(() => {
    core = {
        config: {
            services: {
                taksa: 'http://taksa'
            }
        },
        got: jest.fn().mockResolvedValue({}),
        req: {},
        auth: {
            get: () => ({
                uid: 'TEST_UID',
                mdb: 'TEST_MDB'
            })
        }
    };
});

test('идет в сервис taksa', async () => {
    await service(core, '/method');

    expect(core.got.mock.calls[0][0]).toEqual('http://taksa/method');
});

test('идет в сервис taksa с правильными опциями', async () => {
    const params = {
        foo: 'bar'
    };

    await service(core, '/method', params);

    expect(core.got.mock.calls[0]).toMatchSnapshot();
});

test('можно расширить query', async () => {
    const params = {
        foo: 'bar'
    };

    await service(core, '/method', params, { query: { sync: 'yes' } });

    expect(core.got.mock.calls[0][1]).toMatchSnapshot();
});

test('режим поиска', async () => {
    const params = {
        details: {
            'search-options': 'wtf'
        }
    };

    await service(core, '/method', params);

    expect(core.got.mock.calls[0]).toMatchSnapshot();
});

test('параметр mobile', async () => {
    const params = {
        foo: 'bar',
        mobile: true
    };

    await service(core, '/method', params);

    expect(core.got.mock.calls[0]).toMatchSnapshot();
});

describe('tvm', () => {
    beforeEach(() => {
        core.req.tvm = {
            tickets: {
                taksa: { ticket: 'tvm-service-ticket-taksa' }
            }
        };
    });

    it('должен добавить заголовок "x-ya-user-ticket", если есть tvm', async () => {
        await service(core, '/method', {}, {});

        expect(core.got.mock.calls[0][1].headers)
            .toHaveProperty('x-ya-user-ticket', core.auth.get().userTicket);
    });

    it('должен добавить заголовок "x-ya-service-ticket", если есть tvm', async () => {
        await service(core, '/method', {}, {});

        expect(core.got.mock.calls[0][1].headers)
            .toHaveProperty('x-ya-service-ticket', core.req.tvm.tickets.taksa.ticket);
    });
});
