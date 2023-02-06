'use strict';

jest.unmock('sendbernar');

const mockSendbernarResponse = jest.fn();
const mockRequest = jest.fn();
const mockSendbernar = ({
    request: {
        send_message: (common, userJournal, params) => ({
            ...common,
            ...userJournal,
            ...params,
            path: '/send_message'
        })
    },
    response: {
        send_message: mockSendbernarResponse
    }
});

jest.mock('sendbernar', () => mockSendbernar);

let core;
const service = require('./sendbernar.js');

beforeEach(() => {
    mockRequest.mockResolvedValue({ useNewSendbernar: false });
    core = {
        config: {
            services: {
                sendbernar: 'http://sendbernar'
            }
        },
        got: jest.fn().mockResolvedValue({}),
        auth: {
            get: () => ({
                uid: '12'
            })
        },
        request: mockRequest,
        req: {
            headers: {}
        },
        console: {
            error: jest.fn()
        },
        yasm: {
            sum: jest.fn()
        }
    };
});

describe('options', () => {
    it('значение caller берётся из config.SERVICE', async () => {
        mockSendbernarResponse.mockResolvedValueOnce({});
        core.config.SERVICE = 'TOUCH';

        await service(core, 'send_message', { message: { text: 'text' } }, {});

        expect(core.got).toHaveBeenCalledTimes(1);
        expect(core.got.mock.calls[0][1].caller).toEqual('TOUCH');
    });

    it('options.path используется в url и удаляется из options', async () => {
        mockSendbernarResponse.mockResolvedValueOnce({});

        await service(core, 'send_message', { message: { text: 'text' } }, { path: 'wtf' });

        expect(core.got).toHaveBeenCalledTimes(1);
        const args = core.got.mock.calls[0];
        expect(args[0]).toInclude('send_message');
        expect(args[1]).not.toHaveProperty('path');
    });
});

describe('retryable_error ->', () => {
    it('повторяет поход в сервис', async () => {
        core.got.mockResolvedValueOnce({});
        mockSendbernarResponse
            .mockResolvedValueOnce({ status: 'retryable_error' })
            .mockResolvedValueOnce({});

        await service(core, 'send_message', { message: { text: 'text' } }, {});

        expect(core.got).toHaveBeenCalledTimes(2);
        expect(core.got.mock.calls[0][0]).toInclude('send_message');
        expect(core.got.mock.calls[1][0]).toInclude('send_message');
    });
});

describe('tvm', () => {
    beforeEach(() => {
        core.req.tvm = {
            tickets: {
                sendbernar: { ticket: 'tvm-service-ticket-sendbernar' }
            }
        };
        mockSendbernarResponse.mockResolvedValueOnce({});
    });

    it('должен добавить заголовок "x-ya-user-ticket", если есть tvm', async () => {
        await service(core, 'send_message', { message: { text: 'text' } }, {});

        expect(core.got.mock.calls[0][1].headers)
            .toHaveProperty('x-ya-user-ticket', core.auth.get().userTicket);
    });

    it('должен добавить заголовок "x-ya-service-ticket", если есть tvm', async () => {
        await service(core, 'send_message', { message: { text: 'text' } }, {});

        expect(core.got.mock.calls[0][1].headers)
            .toHaveProperty('x-ya-service-ticket', core.req.tvm.tickets.sendbernar.ticket);
    });
});

test('должен залогировать и упасть на неизвестный метод', async () => {
    expect.assertions(2);

    try {
        await service(core, 'unknown_method', {}, {});
    } catch (e) {
        expect(e.message).toEqual('Unknown sendbernar method \'unknown_method\'');
        expect(core.console.error).toHaveBeenCalled();
    }
});
