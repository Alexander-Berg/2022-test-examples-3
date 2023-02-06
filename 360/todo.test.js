'use strict';

let core;
const service = require('./todo.js');

beforeEach(() => {
    core = {
        config: {
            services: {
                todo: 'http://calendar'
            },
            locale: 'TEST_LOCALE'
        },
        got: jest.fn().mockResolvedValue({}),
        auth: {
            get: () => ({
                uid: '12',
                userTicket: 'TEST_USER_TICKET'
            })
        },
        req: {}
    };
});

test('идет в сервис calendar', async () => {
    await service(core, 'method', {});

    expect(core.got.mock.calls[0][0]).toEqual('http://calendar/api/method');
});

test('идет в сервис calendar с правильными опциями', async () => {
    const params = {
        foo: 'bar'
    };

    await service(core, 'method', params);

    expect(core.got.mock.calls[0]).toMatchSnapshot();
});

test('когда сервис зафейлился', async () => {
    expect.assertions(1);

    core.got.mockRejectedValue('error');

    try {
        await service(core, 'method', {});
    } catch (err) {
        expect(err).toMatchSnapshot();
    }
});

test('когда сервис зафейлился с трейсом', async () => {
    expect.assertions(1);

    const error = {
        error: {
            message: 'foo',
            stackTrace: 'stack trace'
        }
    };

    core.got.mockRejectedValue(error);

    try {
        await service(core, 'method', {});
    } catch (err) {
        expect(err).toMatchSnapshot();
    }
});

describe('tvm', () => {
    beforeEach(() => {
        core.req.tvm = {
            tickets: {
                calendar: { ticket: 'TEST_SERVICE_TICKET' }
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
            .toHaveProperty('x-ya-service-ticket', core.req.tvm.tickets.calendar.ticket);
    });
});
