'use strict';

let core;
const service = require('./mlp.js');

beforeEach(() => {
    core = {
        config: {
            services: {
                mlp: 'http://mlp'
            }
        },
        got: jest.fn().mockResolvedValue({}),
        auth: {
            get: () => ({
                uid: '12'
            })
        },
        req: {}
    };
});

test('идет в сервис mlp', async () => {
    await service(core, '/method', {});

    expect(core.got.mock.calls[0][0]).toEqual('http://mlp/method');
});

test('идет в сервис mlp с правильными опциями', async () => {
    const params = {
        subj: 'TEST_SUBJ',
        text: 'TEST_TEXT',
        letter_lang: 'TEST_LANG',
        mid: '42',
        reqId: 'TEST_REQUEST_ID'
    };

    await service(core, '/method', params);

    expect(core.got.mock.calls[0][1]).toMatchSnapshot();
});

test('идет в сервис mlp с опцией max_repl_len', async () => {
    const params = {
        subj: 'TEST_SUBJ',
        text: 'TEST_TEXT',
        letter_lang: 'TEST_LANG',
        mid: '42',
        reqId: 'TEST_REQUEST_ID',
        max_repl_len: '42,37,25'
    };

    await service(core, '/method', params);

    expect(core.got.mock.calls[0][1]).toMatchSnapshot();
});

test('идет в сервис mlp с опцией tid', async () => {
    const params = {
        subj: 'TEST_SUBJ',
        text: 'TEST_TEXT',
        letter_lang: 'TEST_LANG',
        mid: '42',
        tid: '42'
    };

    await service(core, '/method', params);

    expect(core.got.mock.calls[0][1]).toMatchSnapshot();
});

test('идет в сервис mlp с опцией types', async () => {
    const params = {
        subj: 'TEST_SUBJ',
        text: 'TEST_TEXT',
        letter_lang: 'TEST_LANG',
        mid: '42',
        types: [ 1, 2, 3 ]
    };

    await service(core, '/method', params);

    expect(core.got.mock.calls[0][1]).toMatchSnapshot();
});

test('идет в сервис mlp с опцией stid', async () => {
    const params = {
        subj: 'TEST_SUBJ',
        text: 'TEST_TEXT',
        letter_lang: 'TEST_LANG',
        mid: '42',
        stid: '42.mail:1.2:3'
    };

    await service(core, '/method', params);

    expect(core.got.mock.calls[0][1]).toMatchSnapshot();
});

describe('tvm', () => {
    beforeEach(() => {
        core.req.tvm = {
            tickets: {
                mlp: { ticket: 'tvm-service-ticket-mlp' }
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
            .toHaveProperty('x-ya-service-ticket', core.req.tvm.tickets.mlp.ticket);
    });
});
