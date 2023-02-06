'use strict';

jest.useFakeTimers('modern');
jest.setSystemTime(1580554500751);

const serviceSource = require('./service-source.js');

let core;

jest.mock('./get-location.js', () => () => 'FAKE_LOCATION');

describe('service source', () => {
    beforeEach(() => {
        core = {
            request: jest.fn().mockRejectedValue({}),
            service: () => jest.fn().mockReturnValue(),
            console: {
                error: jest.fn()
            },
            yasm: {
                sum: jest.fn()
            },
            auth: {
                get: () => ({
                    uid: 'FAKE_UID'
                })
            },
            req: {
                uatraits: {
                    BrowserName: 'FAKE_BROWSER',
                    BrowserVersion: 'FAKE_VERSION'
                },
                cookies: {
                    yandexuid: 'FAKE_YANDEXUID',
                    i: 'FAKE_ICOOKIE'
                }
            },
            config: {
                USER_IP: 'FAKE_IP'
            }
        };
    });

    it('throws without client', async () => {
        expect.assertions(1);
        try {
            await serviceSource(core);
        } catch (e) {
            expect(e.message).toBe('unknown client');
        }
    });

    it('throws with unknown client', async () => {
        expect.assertions(1);
        try {
            await serviceSource(core, 'whatever');
        } catch (e) {
            expect(e.message).toBe('unknown client');
        }
    });

    it('calls service for LIZA', async () => {
        core.request.mockResolvedValue({});

        await serviceSource(core, 'LIZA');

        expect(core.request.mock.calls).toMatchSnapshot();
    });

    it('calls service for TOUCH', async () => {
        core.request.mockResolvedValue({});

        await serviceSource(core, 'TOUCH');

        expect(core.request.mock.calls).toMatchSnapshot();
    });

    it('fuid param', async () => {
        core.request.mockResolvedValue({});

        await serviceSource(core, 'TOUCH', { fuid: 'FAKE_FUID' });

        expect(core.request.mock.calls).toMatchSnapshot();
    });

    it('yexp cookie', async () => {
        core.request.mockResolvedValue({});
        core.req.cookies.yexp = 'FAKE_YEXP';

        await serviceSource(core, 'TOUCH');

        expect(core.request.mock.calls).toMatchSnapshot();
    });

    it('resolves with expboxes and checkers, filters only MAIL handlers', async () => {
        core.request.mockResolvedValue({
            ExpBoxes: 'TEST-EXPBOXES',
            Handlers: [
                { HANDLER: 'MAIL' },
                { HANDLER: 'MAIL', CONTEXT: {} },
                { HANDLER: 'DISK' }
            ]
        });

        const result = await serviceSource(core, 'TOUCH');

        expect(result).toEqual({
            ExpBoxes: 'TEST-EXPBOXES',
            Handlers: [ { HANDLER: 'MAIL' }, { HANDLER: 'MAIL', CONTEXT: {} } ]
        });
    });

    it('resolves with null when model rejects', async () => {
        const result = await serviceSource(core, 'TOUCH');

        expect(result).toBeNull();
    });

    describe('если не удалось получить данные от сервиса', () => {
        it('должен логировать', async () => {
            await serviceSource(core, 'LIZA');

            expect(core.console.error).toBeCalledTimes(1);
            expect(core.console.error.mock.calls[0][0]).toBe('EXPERIMENTS_REQUEST_ERROR');
        });

        it('должен отправлять сигнал в голован', async () => {
            await serviceSource(core, 'LIZA');

            expect(core.yasm.sum).toBeCalledTimes(1);
            expect(core.yasm.sum.mock.calls[0][0]).toBe('model_experiments_request_error');
        });
    });

    it('coverage', async () => {
        delete core.service;
        const result = await serviceSource(core, 'TOUCH');

        expect(result).toBeNull();
    });
});
