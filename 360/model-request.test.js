/* global a:true */ // eslint-disable-line no-unused-vars
'use strict';

const ModelRequest = require('./model-request.js');
const {
    HTTP_ERROR,
    CUSTOM_ERROR,
    UNKNOWN_ERROR,
    UNKNOWN_MODEL_ERROR
} = require('./helpers/errors/index.js');

const {
    NO_CKEY,
    NO_AUTH,
    DO_MODEL
} = require('./helpers/flags.js');

const Core = require('./core').default;

const s0 = Symbol();
const s1 = Symbol('model');
const s2 = Symbol('model');

let core;
const getRequest = (name, params, meta) => new ModelRequest(core, { name, params, meta }, 1);

beforeEach(() => {
    const request = {
        cookies: {},
        headers: {
            'x-original-host': 'mail.yandex.ru',
            'x-original-uri': '/u2709/api/models',
            'x-real-ip': '2a02:6b8::25'
        },
        query: {},
        body: {
            _connection_id: '1',
            _ckey: 'Gb1ZeTCNDfadbGuBWOzOzQ=='
        }
    };

    const response = {
        cookie: function() {},
        on: function() {}
    };

    core = new Core(request, response);

    // эмулируем авторизованность
    core.auth.set({
        mdb: 'mdb1',
        suid: '34',
        timezone: 'Europe/Moscow',
        tz_offset: -180,
        uid: '12'
    });

    core.ckey.check = () => true;

    core.models = {
        'do-ok': async () => ({}),
        [Symbol.for('do-ok2')]: async () => ({}),
        'do-error': () => Promise.reject('error'),
        'do-exception': () => { throw new Error('test'); },
        'echo-params': async (params) => params,
        'account-information': async () => ({}),
        'auth': async () => ({ auth: 'ok' }),
        'safe-method/v1': () => {},
        'unsafe-method/v1': () => {},
        [s0]: () => {},
        [s1]: () => {},
        [s2]: () => {},
    };
    core.models['account-information'][NO_CKEY] = true;
    core.models['auth'][NO_AUTH] = true;
    core.models['safe-method/v1'][Symbol.for('method')] = { isSafe: true };
    core.models['unsafe-method/v1'][Symbol.for('method')] = { isSafe: false };
    core.models[s0][DO_MODEL] = true;

    jest.spyOn(core.yasm, 'sum').mockImplementation(() => {});
    jest.spyOn(core.yasm, 'hist').mockImplementation(() => {});
    jest.spyOn(core.console, 'info').mockImplementation(() => {});
    jest.spyOn(core.console, 'error').mockImplementation(() => {});
});

describe('#action', () => {
    it('возвращает функцию если модель определена', () => {
        const request = getRequest('do-ok');
        expect(request.action).toBeInstanceOf(Function);
    });
    it('возвращает fallbackAction если модель не определена', () => {
        const request = getRequest('not-exist');
        expect(request.action).toBe(request.fallbackAction);
    });
});

describe('#sync', () => {
    it('возвращает true для do-хендлеров', () => {
        const request = getRequest('do-ok');
        expect(request.sync).toBe(true);
    });

    it('возвращает false для обычных хендлеров', () => {
        const request = getRequest('echo-params');
        expect(request.sync).toBe(false);
    });

    it('возвращает true для опасных методов', () => {
        const request = getRequest('unsafe-method/v1');
        expect(request.sync).toBe(true);
    });

    it('возвращает false для безопасных методов', () => {
        const request = getRequest('safe-method/v1');
        expect(request.sync).toBe(false);
    });

    it('возвращает true для флага DO_MODEL', () => {
        const request = getRequest(s0);
        expect(request.sync).toBe(true);
    });
});

describe('#sign', () => {
    it('разный у разных моделей', () => {
        const request1 = getRequest('model1', { p: 1 });
        const request2 = getRequest('model2', { p: 1 });
        expect(request1.sign).not.toEqual(request2.sign);
    });

    it('разный у разных моделей с одинаковым описанием', () => {
        const request1 = getRequest(s1, { p: 1 });
        const request2 = getRequest(s2, { p: 1 });
        expect(s1.description).toEqual(s2.description);
        expect(request1.sign).not.toEqual(request2.sign);
    });

    it('разный у одной модели с разными параметрами', () => {
        const request1 = getRequest('model1', { p: 1 });
        const request2 = getRequest('model1', { p: 2 });
        expect(request1.sign).not.toEqual(request2.sign);
    });

    it('одинаковый у одной модели с одинаковыми параметрами', () => {
        const request1 = getRequest('model1', { p: 1 });
        const request2 = getRequest('model1', { p: 1 });
        expect(request1.sign).toEqual(request2.sign);
    });

    it('одинаковый у одной модели с одинаковыми параметрами (symbol)', () => {
        const request1 = getRequest(s2, { p: 1 });
        const request2 = getRequest(s2, { p: 1 });
        expect(request1.sign).toEqual(request2.sign);
    });

    it('не зависит от порядка параметров', () => {
        const request1 = getRequest('model1', { p: 1, q: 2 });
        const request2 = getRequest('model1', { q: 2, p: 1 });
        expect(request1.sign).toEqual(request2.sign);
    });

    it('не зависит от параметров у do-модели', () => {
        const request = getRequest('do-ok', { param: 'param' });
        expect(request.sign).toMatch(/^do-ok\b/);
        expect(request.sign).not.toMatch('param');
    });

    it('разный у одной do-модели с одинаковыми параметрами', () => {
        const request1 = getRequest('do-ok', { p: 1 });
        const request2 = getRequest('do-ok', { p: 1 });
        expect(request1.sign).not.toEqual(request2.sign);
    });
});

describe('#run', () => {
    it('вызывает fallbackAction если модели не существует', async () => {
        expect.hasAssertions();
        try {
            await getRequest('not-exist').run();
        } catch (error) {
            expect(error).toBeInstanceOf(UNKNOWN_MODEL_ERROR);
            expect(error.error).toEqual({ model: 'not-exist', message: 'Model \'not-exist\' not found' });
            expect(core.console.error).toHaveBeenCalledTimes(2);
            expect(core.console.error).toHaveBeenCalledWith(
                'UNKNOWN_MODEL_ERROR', { message: 'Model \'not-exist\' not found' }
            );
            expect(core.yasm.sum).toHaveBeenCalledWith('duffman_unknown_model_error');
            expect(core.yasm.sum).toHaveBeenCalledTimes(1);
        }
    });

    it('если у core есть свой fallbackAction, то вызывает его', async () => {
        expect.hasAssertions();
        const params = { test: true };
        const name = 'not-exist';

        core.params._connection_id = undefined;
        core.fallbackAction = async function(modelName, modelParams) {
            this.console.error('MODEL_NOT_FOUND', { model: modelName, params: modelParams });

            throw new UNKNOWN_MODEL_ERROR(modelName);
        };
        jest.spyOn(core, 'fallbackAction');

        try {
            await getRequest(name, params).run();
        } catch (error) {
            expect(error).toBeInstanceOf(UNKNOWN_MODEL_ERROR);
            expect(core.fallbackAction).toHaveBeenCalledTimes(1);
            expect(core.console.error).toHaveBeenCalledTimes(2);
            expect(core.console.error).toHaveBeenCalledWith('MODEL_NOT_FOUND', { model: name, params });
            expect(core.yasm.sum).not.toHaveBeenCalled();
        }

    });

    it('не вызывает fallbackAction если модель существует', async () => {
        const request = getRequest(Symbol.for('do-ok2'));
        jest.spyOn(request, 'fallbackAction');

        const result = await request.run();

        expect(result).toEqual({});
        expect(request.fallbackAction).not.toHaveBeenCalled();
        expect(core.yasm.sum).toHaveBeenCalledTimes(2);
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_resolved');
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_resolved.do-ok2');
    });

    it('кидает исключение с UNKNOWN_ERROR если модель вернула reject', async () => {
        expect.hasAssertions();
        try {
            await getRequest('do-error').run();
        } catch (error) {
            expect(error.error).toBe('UNKNOWN_ERROR');
            expect(core.yasm.sum).toHaveBeenCalledTimes(2);
            expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected');
            expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected.do-error');
        }
    });

    it('кидает исключение с EXEC_ERROR если модель упала с exception', async () => {
        expect.hasAssertions();
        try {
            await getRequest('do-exception').run();
        } catch (error) {
            expect(error.error).toEqual('EXEC_ERROR');
            expect(core.yasm.sum).toHaveBeenCalledTimes(4);
            expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_exec_error');
            expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_exec_error.do-exception');
            expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected');
            expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected.do-exception');
            expect(core.console.error).toHaveBeenCalledTimes(2);
            expect(core.console.error).toHaveBeenCalledWith('MODEL_EXEC_ERROR', expect.anything());
            expect(core.console.error).toHaveBeenCalledWith('MODEL_REJECTED', expect.anything());
        }
    });

    it('возвращает NO_AUTH_ERROR если нет авторизации', async () => {
        core.auth.set(new CUSTOM_ERROR({
            code: 2001,
            message: 'not authenticated'
        }));

        const result = await getRequest('do-ok').run();

        expect(result.error.code).toEqual('AUTH_NO_AUTH');
        expect(core.yasm.sum).toHaveBeenCalledTimes(2);
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected_auth');
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected_auth.do-ok');
    });

    it('возвращает AUTH_UNKNOWN для неизвестной ошибки авторизации', async () => {
        core.auth.set({});

        const result = await getRequest('do-ok').run();

        expect(result.error.code).toEqual('AUTH_UNKNOWN');
        expect(core.yasm.sum).toHaveBeenCalledTimes(2);
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected_auth');
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected_auth.do-ok');
    });

    it('кидает исключение с http error если авторизация упала по таймауту', async () => {
        const data = core.auth.get();
        data.uid = null;
        data.error = new HTTP_ERROR({ statusCode: 0 });

        const result = await getRequest('do-ok').run();

        expect(result.error.code).toEqual(0);
        expect(core.yasm.sum).toHaveBeenCalledTimes(2);
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected_auth');
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected_auth.do-ok');
    });

    it('возвращает ответ если flag.NO_AUTH', async () => {
        const data = core.auth.get();
        data.uid = null;
        data.error = new CUSTOM_ERROR({ code: 2001 });

        const result = await getRequest('auth').run();

        expect(result).toEqual({ auth: 'ok' });
        expect(core.yasm.sum).toHaveBeenCalledTimes(2);
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_resolved');
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_resolved.auth');
    });

    it('возвращает BAD_CKEY_ERROR если неправильный ckey и запрашивали не ai', async () => {
        core.ckey.isError = true;

        const result = await getRequest('do-ok').run();

        expect(result.error).toEqual('ckey');
        expect(core.yasm.sum).toHaveBeenCalledTimes(2);
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected_ckey');
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected_ckey.do-ok');
    });

    it('не должен возвращать BAD_CKEY_ERROR если неправильный ckey и запрашивали ai', async () => {
        core.ckey.isError = true;

        const result = await getRequest('account-information').run();

        expect(result.error).not.toEqual('ckey');
        expect(core.yasm.sum).toHaveBeenCalledTimes(2);
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_resolved');
        expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_resolved.account-information');
    });

    it('логирует успешный вызов', async () => {
        const result = await getRequest('do-ok', { p: 1 }).run();

        expect(result).toEqual({});
        expect(core.console.info).toHaveBeenCalledWith('MODEL_RESOLVED', expect.objectContaining({
            name: 'do-ok',
            time: expect.stringMatching(/\d\.\d{3}$/),
            params: { p: 1 }
        }));
    });

    it('добавляет core.logModelResolved', async () => {
        jest.spyOn(core, 'logModelResolved').mockReturnValue({
            message: 'from stub',
            name: 'ignored'
        });

        const result = await getRequest('do-ok', { p: 1 }).run();

        expect(result).toEqual({});
        expect(core.logModelResolved).toHaveBeenCalledWith(
            'do-ok',
            { p: 1 },
            {},
        );
        expect(core.console.info).toHaveBeenCalledWith('MODEL_RESOLVED', expect.objectContaining({
            name: 'do-ok',
            params: { p: 1 },
            message: 'from stub'
        }));
    });

    it('добавляет stack к логу ошибки', async () => {
        expect.hasAssertions();

        core.models['do-native-error'] = async () => {
            throw new Error('error');
        };

        try {
            await getRequest('do-native-error').run();
        } catch (error) {
            expect(error).toBeInstanceOf(UNKNOWN_ERROR);
            expect(core.console.error).toHaveBeenCalledTimes(1);
            expect(core.console.error).toHaveBeenNthCalledWith(1, 'MODEL_REJECTED', expect.objectContaining({
                stack: expect.any(String)
            }));
            expect(core.yasm.sum).toHaveBeenCalledTimes(2);
            expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected');
            expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_rejected.do-native-error');
        }
    });

    it('добавляет core.logModelRejected к логу ошибки', async () => {
        expect.hasAssertions();

        core.models['do-native-error'] = async () => {
            throw new Error('error');
        };

        jest.spyOn(core, 'logModelRejected').mockReturnValue({
            message: 'from stub',
            name: 'ignored'
        });

        try {
            await getRequest('do-native-error').run();
        } catch {
            expect(core.logModelRejected).toHaveBeenCalledWith(
                'do-native-error',
                {},
                expect.any(Error)
            );
            expect(core.console.error).toHaveBeenCalledWith('MODEL_REJECTED', expect.objectContaining({
                name: 'do-native-error',
                params: {},
                message: 'from stub'
            }));
        }
    });

    describe('логгирует номер попытки с клиента', () => {
        it('для resolved', async () => {
            await getRequest('do-ok', { p: 1 }, { requestAttempt: 42 }).run();

            expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_request_attempt.do-ok.attempt_42');
            expect(core.console.info).toHaveBeenCalledWith('MODEL_RESOLVED', expect.objectContaining({
                request_attempt: 42
            }));
        });

        it('для rejected', async () => {
            expect.hasAssertions();

            try {
                await getRequest('do-error', { p: 1 }, { requestAttempt: 42 }).run();
            } catch {
                expect(core.yasm.sum).toHaveBeenCalledWith('duffman_model_request_attempt.do-error.attempt_42');
                expect(core.console.error).toHaveBeenCalledWith('MODEL_REJECTED', expect.objectContaining({
                    request_attempt: 42
                }));
            }
        });

        it('не передает попытку в параметры модели', async () => {
            const result = await getRequest('echo-params', { p: 1 }, { requestAttempt: 42 }).run();

            expect(result).toEqual({ p: 1 });
        });

        it('не учитывает странные параметры попытки', async () => {
            await getRequest('do-ok', {}, { requestAttempt: 'шта' }).run();

            expect(core.yasm.sum).not.toHaveBeenCalledWith(expect.stringMatching('duffman_model_request_attempt'));
            expect(core.console.info).toHaveBeenCalledWith('MODEL_RESOLVED', expect.objectContaining({
                request_attempt: 0
            }));
        });
    });
});
