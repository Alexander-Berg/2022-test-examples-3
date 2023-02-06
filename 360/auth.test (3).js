'use strict';

const auth = require('./auth.js');
const { AUTH_ERROR, HTTP_ERROR, CUSTOM_ERROR } = require('@yandex-int/duffman').errors;

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        config: {
            sids: {
                telemost: '121',
                corp: '669'
            }
        },
        auth: {
            get: jest.fn()
        },
        req: {
            headers: {
                'x-https-request': 'yes'
            }
        },
        yasm: {
            sum: jest.fn()
        },
        console: {
            error: jest.fn()
        },
        service: () => mockService
    };
});

describe('без авторизации', () => {
    beforeEach(() => {
        delete core.req.headers.authorization;
    });

    it('возвращает AUTH_ERROR', async () => {
        try {
            await auth(null, core);
            throw new Error('MUST_REJECT');
        } catch (e) {
            expect(e).toBeInstanceOf(AUTH_ERROR);
        }
    });
});

describe('с авторизацией', () => {
    beforeEach(() => {
        core.req.headers.authorization = 'Oauth deadbeef1234567890';
    });

    it('дергает метод akita /auth, happy path', async () => {
        mockService.mockResolvedValueOnce({});

        await auth(null, core);

        expect(mockService).toHaveBeenCalledWith(
            '/auth',
            {
                sids_to_check: [ '121', '669' ]
            },
            {
                headers: {
                    'authorization': 'Oauth deadbeef1234567890',
                    'x-https-request': 'yes'
                },
                user: {}
            }
        );
    });

    it('дергает метод akita /auth, unhappy path', async () => {
        mockService.mockRejectedValueOnce({});

        try {
            await auth(null, core);
            throw new Error('MUST_REJECT');
        } catch (error) {
            expect(error).toBeInstanceOf(AUTH_ERROR);
            expect(mockService).toHaveBeenCalledWith(
                '/auth',
                {
                    sids_to_check: [ '121', '669' ]
                },
                {
                    headers: {
                        'authorization': 'Oauth deadbeef1234567890',
                        'x-https-request': 'yes'
                    },
                    user: {}
                }
            );
        }
    });
});

describe('errors', () => {
    beforeEach(() => {
        core.req.headers.authorization = 'Oauth deadbeef1234567890';
    });

    it('должен вернуть http ошибку, если такая случилась', async () => {
        const e = new HTTP_ERROR({});
        mockService.mockRejectedValueOnce(e);

        try {
            await auth(null, core);
            throw new Error('MUST REJECT');
        } catch (error) {
            expect(error).toEqual(new AUTH_ERROR(e));
        }
    });

    it('должен вернуть AUTH_UNKNOWN для неизвестной ошибки', async () => {
        const e = {
            error: {
                error: 'some'
            }
        };

        mockService.mockRejectedValueOnce(e);

        try {
            await auth(null, core);
            throw new Error('MUST REJECT');
        } catch (error) {
            expect(error).toEqual(new AUTH_ERROR(e));
        }
    });

    it('должен вернуть AUTH_NO_AUTH ошибку, если пришла ошибка с сервера', async () => {
        const e = new CUSTOM_ERROR({ code: 2001, message: 'not authenticated', reason: '' });
        mockService.mockRejectedValueOnce(e);

        try {
            await auth(null, core);
            throw new Error('MUST REJECT');
        } catch (error) {
            expect(error).toEqual(new AUTH_ERROR(e));
        }
    });
});
