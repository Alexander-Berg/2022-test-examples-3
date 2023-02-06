'use strict';

const service = require('./smart-compose.js');

let core;

beforeEach(function() {
    core = {
        auth: {
            get: jest.fn(() => ({
                uid: 'test-uid',
                userTicket: 'TEST_USER_TICKET'
            }))
        },
        got: jest.fn(),
        config: {
            services: {
                'smart-compose': 'test-smart-compose'
            }
        },
        req: {}
    };
});

test('should calls core.got with correct url', function() {
    service(core, '/test-method', {}, {});

    expect(core.got).toHaveBeenCalledWith('test-smart-compose/test-method', expect.any(Object));
});

describe('should calls core.got with correct options', function() {
    it('if service params are passed', function() {
        service(core, '/test-method', { testParam: true }, {});

        expect(core.got).toHaveBeenCalledWith(
            expect.any(String),
            {
                method: 'post',
                body: '{"testParam":true}',
                json: true,
                query: {
                    uid: 'test-uid'
                },
                headers: {}
            }
        );
    });

    it('if additional options are passed', function() {
        service(core, '/test-method', {}, { testOption: true });

        expect(core.got).toHaveBeenCalledWith(
            expect.any(String),
            {
                method: 'post',
                body: '{}',
                testOption: true,
                json: true,
                query: {
                    uid: 'test-uid'
                },
                headers: {}
            }
        );
    });

    it('should not rewrite uid', function() {
        service(core, '/test-method', {}, { query: { uid: 'evil', valid: 'pass' } });

        expect(core.got).toHaveBeenCalledWith(
            expect.any(String),
            {
                method: 'post',
                body: '{}',
                json: true,
                query: {
                    uid: 'test-uid',
                    valid: 'pass'
                },
                headers: {}
            }
        );
    });

    describe('if there is tvm', () => {
        beforeEach(() => {
            core.req.tvm = {
                headers: {
                    'tvm-service-smart-compose': 'TEST_SERVICE_TICKET_SMART_COMPOSE'
                },
                tickets: {
                    'smart-compose': {
                        ticket: 'TEST_SERVICE_TICKET_SMART_COMPOSE'
                    }
                }
            };
        });

        it('should add tvm headers ', function() {
            service(core, '/test-method', {}, { testOption: true });

            expect(core.got).toHaveBeenCalledWith(
                expect.any(String),
                {
                    method: 'post',
                    body: '{}',
                    testOption: true,
                    json: true,
                    query: {
                        uid: 'test-uid'
                    },
                    headers: {
                        'x-ya-service-ticket': 'TEST_SERVICE_TICKET_SMART_COMPOSE',
                        'x-ya-user-ticket': 'TEST_USER_TICKET'
                    }
                }
            );
        });

        it('should append tvm headers', function() {
            const headers = {
                'x-test-header': 'test-header-value'
            };

            service(core, '/test-method', {}, { headers, testOption: true });

            expect(core.got.mock.calls[0][1].headers).toEqual({
                'x-test-header': 'test-header-value',
                'x-ya-service-ticket': 'TEST_SERVICE_TICKET_SMART_COMPOSE',
                'x-ya-user-ticket': 'TEST_USER_TICKET'
            });
        });
    });
});
