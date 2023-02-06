'use strict';

const msearch = require('./msearch.js');

let core;

beforeEach(() => {
    core = {
        config: {
            services: {
                msearch: 'http://msearch'
            },
            USER_IP: '1.1.1'
        },
        got: jest.fn().mockResolvedValue({}),
        auth: {
            get: jest.fn().mockReturnValue({
                uid: '12',
                suid: '34',
                mdb: 'mdb1'
            })
        },
        req: {}
    };
});

describe('общие параметры запроса ->', () => {
    it('должен добавить общие параметры', () => {
        msearch(core, '/method', { param: 'value' }, {}, () => ({}));
        expect(core.got.mock.calls[0][0])
            .toEqual('http://msearch/api/async/mail/method');
        expect(core.got.mock.calls[0][1])
            .toEqual({
                headers: {},
                json: true,
                query: {
                    mdb: 'mdb1',
                    param: 'value',
                    remote_ip: '1.1.1',
                    suid: '34',
                    uid: '12'
                }
            });
    });

    it('должен добавить параметр timeout', () => {
        const def = jest.fn().mockReturnValue({
            queryParamFast: '3',
            timeoutFast: '4'
        });
        msearch(core, '/method', { twoSteps: '1' }, {}, def);
        expect(core.got.mock.calls[0][1])
            .toEqual({
                headers: {},
                json: true,
                timeout: '4',
                query: {
                    mdb: 'mdb1',
                    remote_ip: '1.1.1',
                    suid: '34',
                    uid: '12',
                    timeout: '3',
                    twoSteps: '1'
                }
            });
    });

    it('должен добавить параметр timeout если указан status', () => {
        const def = jest.fn().mockReturnValue({
            queryParamNormal: '1',
            timeoutNormal: '2'
        });
        msearch(core, '/method', { twoSteps: '1', status: '1' }, {}, def);
        expect(core.got.mock.calls[0][1])
            .toEqual({
                headers: {},
                json: true,
                timeout: '2',
                query: {
                    mdb: 'mdb1',
                    remote_ip: '1.1.1',
                    suid: '34',
                    uid: '12',
                    timeout: '1',
                    twoSteps: '1',
                    status: '1'
                }
            });
    });
});

describe('tvm headers', () => {
    const def = jest.fn().mockReturnValue({});

    beforeEach(() => {
        core.auth.get.mockReturnValue({ userTicket: 'TEST_TVM_USER_TICKET' });
        core.req.tvm = {
            tickets: {
                msearch: {
                    ticket: 'TEST_TVM_SERVICE_MSEARCH'
                }
            }
        };
    });

    it('добавляет заголовки', () => {
        msearch(core, '/method', {}, {}, def);

        expect(core.got.mock.calls[0][1].headers).toEqual({
            'x-ya-user-ticket': 'TEST_TVM_USER_TICKET',
            'x-ya-service-ticket': 'TEST_TVM_SERVICE_MSEARCH'
        });
    });

    it('дополняет заголовки', () => {
        const options = {
            headers: {
                'Content-type': 'application/json'
            }
        };

        msearch(core, '/method', {}, options, def);

        expect(core.got.mock.calls[0][1].headers).toEqual({
            'Content-type': 'application/json',
            'x-ya-user-ticket': 'TEST_TVM_USER_TICKET',
            'x-ya-service-ticket': 'TEST_TVM_SERVICE_MSEARCH'
        });
    });
});
