'use strict';

jest.unmock('@yandex-int/duffman');

const { CUSTOM_ERROR } = require('@yandex-int/duffman').errors;

const service = require('./calendar.js');

let core;

beforeEach(() => {
    core = {
        config: {
            services: {
                calendar: 'http://calendar'
            }
        },
        got: jest.fn(),
        auth: {
            get: () => ({
                uid: '12',
                userTicket: 'TEST_USER_TICKET'
            })
        },
        req: {}
    };
});

test('реджектится с ошибкой', () => {
    core.got.mockRejectedValueOnce({
        error: { name: 'event-already-exists' }
    });

    return service(core, '/some_method', {}, {})
        .then(
            () => Promise.reject('MUST REJECT'),
            (err) => {
                expect(err).toEqual({ error: { name: 'event-already-exists' } });
            }
        );
});

test('удаляет приватные поля для CUSTOM_ERROR', () => {
    core.got.mockRejectedValueOnce(
        new CUSTOM_ERROR({
            name: 'some secure name',
            stackTrace: 'some secure stack trace',
            other: 'dont worry about it'
        })
    );

    return service(core, '/some_method', {}, {})
        .then(
            () => Promise.reject('MUST REJECT'),
            (err) => {
                expect(err.error).toEqual({ other: 'dont worry about it' });
            }
        );
});

describe('общие параметры запроса', () => {
    describe(' GET (default)', () => {
        it('должен добавить общие параметры и uid пользователя', function() {
            core.got.mockResolvedValueOnce({});

            service(core, '/method', {
                someParamName: 'someParamValue'
            }, {});

            expect(core.got.mock.calls[0][0])
                .toEqual('http://calendar/method');

            expect(core.got.mock.calls[0][1])
                .toEqual({
                    headers: {},
                    json: true,
                    method: 'GET',
                    type: 'GET',
                    query: {
                        uid: '12',
                        someParamName: 'someParamValue'
                    }
                });
        });

        it('должен добавить общие параметры, но без uid-а', function() {
            core.got.mockResolvedValueOnce({});

            service(core, '/method', {
                someParamName: 'someParamValue'
            }, { extendWithUid: false });

            expect(core.got.mock.calls[0][0])
                .toEqual('http://calendar/method');

            expect(core.got.mock.calls[0][1])
                .toEqual({
                    headers: {},
                    json: true,
                    method: 'GET',
                    type: 'GET',
                    query: {
                        someParamName: 'someParamValue'
                    }
                });
        });
    });

    it(' POST –> должен добавить общие параметры для POST', function() {
        core.got.mockResolvedValueOnce({});

        service(core, '/method', { param: 'value' }, { type: 'POST' });

        expect(core.got.mock.calls[0][0])
            .toEqual('http://calendar/method');

        expect(core.got.mock.calls[0][1])
            .toEqual({
                body: { param: 'value' },
                headers: {},
                json: true,
                method: 'POST',
                type: 'POST'
            });
    });
});

test('должен прокинуть tvm заголовки', function() {
    core.req.tvm = {
        tickets: {
            calendar: { ticket: 'TEST_SERVICE_TICKET' }
        }
    };

    core.got.mockResolvedValueOnce({});

    service(core, '/method', {}, {});

    expect(core.got.mock.calls[0][0])
        .toEqual('http://calendar/method');

    expect(core.got.mock.calls[0][1])
        .toEqual({
            headers: {
                'x-ya-service-ticket': 'TEST_SERVICE_TICKET',
                'x-ya-user-ticket': 'TEST_USER_TICKET'
            },
            json: true,
            method: 'GET',
            type: 'GET',
            query: {
                uid: '12'
            }
        });
});
