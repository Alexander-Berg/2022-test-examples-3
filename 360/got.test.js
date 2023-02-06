'use strict';

// const jest = require('jest');
const nock = require('nock');

const {
    EXTERNAL_ERROR,
    HTTP_ERROR,
    NO_DATA_ERROR,
    CUSTOM_ERROR,
    XML_PARSE_ERROR
} = require('../../lib/helpers/errors/index.js');

const SHOULD_THROW = (res) => {
    throw new Error('Should throw, but returns ' + res);
};

const Core = require('./index.js').default;

let req;
let reqMock;
let core;

beforeEach(() => {
    req = nock('http://example.com');
    reqMock = req.get('/test');
    core = new Core({
        headers: {
            'x-real-ip': '2a02:6b8::25'
        }
    }, {
        on: jest.fn()
    });

    jest.spyOn(core.yasm, 'sum');
});

afterEach(() => {
    nock.cleanAll();
});

describe('заголовок Content-Type', () => {
    it.each([
        [ 'объект', 'application/json', { value: 'test' } ],
        [ 'массив', 'application/json', [ 'array' ] ],
        [ 'строка', undefined, 'some string' ],
        [ 'Buffer', undefined, Buffer.from('some buffer') ],
    ])('%s -> %s', (_, type, body) => {
        const reqExpect = req
            .post('/test', body)
            .matchHeader('Content-Type', h => h === type)
            .reply(200, { body: 'ok' });

        return core.got('http://example.com/test', { json: true, body })
            .then(res => {
                expect(reqExpect.isDone()).toEqual(true);
                expect(res.body).toEqual('ok');
            });
    });
});

describe('обработка ошибок', () => {
    it('возвращает HTTP_ERROR при наличии response.body и делает 1 ретрай', () => {
        process.emit = jest.fn();
        const reqExpect = reqMock
            .matchHeader('x-request-attempt', (v) => v <= 1)
            .twice()
            .reply(504, {
                body: 'test'
            });

        return core.got('http://example.com/test', { json: true })
            .then(SHOULD_THROW)
            .catch((err) => {
                expect(reqExpect.isDone()).toEqual(true);
                expect(err).toBeInstanceOf(HTTP_ERROR);
                expect(err.error.code).not.toEqual(0);
                expect(err.body).toEqual({ body: 'test' });
                expect(process.emit).toHaveBeenCalledWith('yasm', {
                    name: 'http.attempt',
                    payload: { host: 'example.com', attempt: 0 }
                });
                expect(process.emit).toHaveBeenCalledWith('yasm', {
                    name: 'http.attempt',
                    payload: { host: 'example.com', attempt: 1 }
                });
            });
    });

    describe('возвращает HTTP_ERROR при ошибочном коде ответа и ', function() {

        it('делает 1 ретрай, если не указано иного', function() {
            process.emit = jest.fn();
            const reqExpect = reqMock
                .matchHeader('x-request-attempt', (v) => v <= 1)
                .twice()
                .reply(503);

            return core.got('http://example.com/test')
                .then(SHOULD_THROW)
                .catch((err) => {
                    expect(reqExpect.isDone()).toEqual(true);
                    expect(err).toBeInstanceOf(HTTP_ERROR);
                    expect(err.error.code).toEqual(503);
                    expect(process.emit).toHaveBeenCalledWith('yasm', {
                        name: 'http.attempt',
                        payload: { host: 'example.com', attempt: 0 }
                    });
                    expect(process.emit).toHaveBeenCalledWith('yasm', {
                        name: 'http.attempt',
                        payload: { host: 'example.com', attempt: 1 }
                    });
                });
        });

        it('не делает ретраи, если передан retryOnUnavailable=0', function() {
            const reqExpect = reqMock
                .once()
                .matchHeader('x-request-attempt', '0')
                .reply(503);

            return core.got('http://example.com/test', { retryOnUnavailable: 0 })
                .then(SHOULD_THROW)
                .catch((err) => {
                    expect(reqExpect.isDone()).toEqual(true);
                    expect(err).toBeInstanceOf(HTTP_ERROR);
                    expect(err.error.code).toEqual(503);
                });

        });

        it('делает retryOnUnavailable ретраев', function() {
            const reqExpect = reqMock
                .matchHeader('x-request-attempt', (v) => v <= 2)
                .thrice()
                .reply(503);

            return core.got('http://example.com/test', { retryOnUnavailable: 2 })
                .then(SHOULD_THROW)
                .catch((err) => {
                    expect(reqExpect.isDone()).toEqual(true);
                    expect(err).toBeInstanceOf(HTTP_ERROR);
                    expect(err.error.code).toEqual(503);
                });

        });

        it('не делает ретраи, если в ответе есть заголовок x-request-noretry', function() {
            const reqExpect = reqMock
                .matchHeader('x-request-attempt', '0')
                .once()
                .reply(503, '', {
                    'x-request-noretry': 'yes'
                });

            return core.got('http://example.com/test')
                .then(SHOULD_THROW)
                .catch((err) => {
                    expect(reqExpect.isDone()).toEqual(true);
                    expect(err).toBeInstanceOf(HTTP_ERROR);
                    expect(err.error.code).toEqual(503);
                });

        });
    });

    it('возвращает HTTP_ERROR при ошибочном коде ответа и делает 1 ретрай', () => {
        const reqExpect = reqMock.twice().reply(503);

        return core.got('http://example.com/test')
            .then(SHOULD_THROW)
            .catch((err) => {
                expect(reqExpect.isDone()).toEqual(true);
                expect(err).toBeInstanceOf(HTTP_ERROR);
                expect(err.error.code).toEqual(503);
            });
    });

    it('делает 1 рейтрай при ECONNREFUSED', () => {
        process.emit = jest.fn();
        const reqExpect = reqMock
            .twice()
            .matchHeader('x-request-attempt', (v) => v <= 1)
            .replyWithError({ code: 'ECONNREFUSED' });

        return core.got('http://example.com/test')
            .then(SHOULD_THROW)
            .catch((err) => {
                expect(reqExpect.isDone()).toEqual(true);
                expect(err).toBeInstanceOf(HTTP_ERROR);
                expect(err.error.code).toEqual(0);
                expect(process.emit).toHaveBeenCalledWith('yasm', {
                    name: 'http.attempt',
                    payload: { host: 'example.com', attempt: 0 }
                });
                expect(process.emit).toHaveBeenCalledWith('yasm', {
                    name: 'http.attempt',
                    payload: { host: 'example.com', attempt: 1 }
                });
            });
    });

    it('делает 1 ретрай при ECONNRESET', () => {
        process.emit = jest.fn();
        const reqExpect = reqMock
            .twice()
            .matchHeader('x-request-attempt', (v) => v <= 1)
            .replyWithError({ code: 'ECONNRESET' });

        return core.got('http://example.com/test')
            .then(SHOULD_THROW)
            .catch((err) => {
                expect(reqExpect.isDone()).toEqual(true);
                expect(err).toBeInstanceOf(HTTP_ERROR);
                expect(err.error.code).toEqual(0);
                expect(process.emit).toHaveBeenCalledWith('yasm', {
                    name: 'http.attempt',
                    payload: { host: 'example.com', attempt: 0 }
                });
                expect(process.emit).toHaveBeenCalledWith('yasm', {
                    name: 'http.attempt',
                    payload: { host: 'example.com', attempt: 1 }
                });
            });
    });

    it('делает ретрай при ETIMEDOUT', () => {
        const doNock = ({ timeout, attempt }) => nock('http://example.com', {
            reqheaders: {
                'x-request-timeout': timeout,
                'x-request-attempt': attempt
            }
        }).get('/test').replyWithError({ code: 'ETIMEDOUT' });

        const req1 = doNock({ timeout: 1000, attempt: 0 });
        const req2 = doNock({ timeout: 2000, attempt: 1 });

        process.emit = jest.fn();

        return core.got('http://example.com/test', { timeout: 1000, retryOnTimeout: 1 })
            .then(SHOULD_THROW)
            .catch((err) => {
                expect(req1.isDone()).toEqual(true);
                expect(req2.isDone()).toEqual(true);
                expect(err).toBeInstanceOf(HTTP_ERROR);
                expect(err.error.code).toEqual(0);
                expect(process.emit).toHaveBeenCalledWith('yasm', {
                    name: 'http.attempt',
                    payload: { host: 'example.com', attempt: 0 }
                });
                expect(process.emit).toHaveBeenCalledWith('yasm', {
                    name: 'http.attempt',
                    payload: { host: 'example.com', attempt: 1 }
                });
            });
    });

    it('делает ретрай при ESOCKETTIMEDOUT', () => {
        process.emit = jest.fn();
        const reqExpect = reqMock.thrice().replyWithError({ code: 'ESOCKETTIMEDOUT' });

        return core.got('http://example.com/test', { retryOnTimeout: 2 })
            .then(SHOULD_THROW)
            .catch((err) => {
                expect(reqExpect.isDone()).toEqual(true);
                expect(err).toBeInstanceOf(HTTP_ERROR);
                expect(err.error.code).toEqual(0);
                expect(process.emit).toHaveBeenCalledWith('yasm', {
                    name: 'http.attempt',
                    payload: { host: 'example.com', attempt: 0 }
                });
                expect(process.emit).toHaveBeenCalledWith('yasm', {
                    name: 'http.attempt',
                    payload: { host: 'example.com', attempt: 1 }
                });
                expect(process.emit).toHaveBeenCalledWith('yasm', {
                    name: 'http.attempt',
                    payload: { host: 'example.com', attempt: 2 }
                });
            });
    });

    it('возвращает NO_DATA_ERROR если в ответе не пришло данных', () => {
        reqMock.reply(200);

        return core.got('http://example.com/test')
            .then(SHOULD_THROW)
            .catch((err) => expect(err).toBeInstanceOf(NO_DATA_ERROR));
    });

    it('возвращает EXTERNAL_ERROR для ответа с телом ошибки', () => {
        reqMock.reply(200, {
            error: {
                code: 42,
                message: 'the answer'
            }
        });

        return core.got('http://example.com/test', { json: true })
            .then(SHOULD_THROW)
            .catch((err) => {
                expect(err).toBeInstanceOf(EXTERNAL_ERROR);
                expect(err.error.code).toEqual(42);
                expect(err.error.message).toEqual('the answer');
            });
    });

    it('возвращает EXTERNAL_ERROR для ответа с телом ошибки (POST)', () => {
        req.post('/test').reply(200, {
            error: {
                code: 42,
                message: 'the answer'
            }
        });

        return core.got('http://example.com/test', { json: true, body: 'string' })
            .then(SHOULD_THROW)
            .catch((err) => {
                expect(err).toBeInstanceOf(EXTERNAL_ERROR);
                expect(err.error.code).toEqual(42);
                expect(err.error.message).toEqual('the answer');
            });
    });

    it('возвращает XML_PARSE_ERROR для невалидного XML ответа', () => {
        reqMock.reply(200, 'not xml');

        return core.got('http://example.com/test')
            .then(SHOULD_THROW)
            .catch((err) => expect(err).toBeInstanceOf(XML_PARSE_ERROR));
    });

    it('сигналит в голован код ошибки', async () => {
        expect.assertions(1);
        reqMock.replyWithError({ code: 'EFAKECODE' });

        try {
            await core.got('http://example.com/test');
        } catch (e) {
            expect(core.yasm.sum).toBeCalledWith('duffman_http_request_error_EFAKECODE');
        }
    });
});

describe('обработка данных', () => {
    it('возвращает json структуру для json ответа', () => {
        reqMock.reply(200, {
            test: 'test'
        });

        return core.got('http://example.com/test', { json: true })
            .then((res) => {
                expect(res).toEqual({
                    test: 'test'
                });
            });
    });

    it('возвращает json структуру для xml ответа #1', () => {
        reqMock.reply(200, '<test>0</test>>');

        return core.got('http://example.com/test')
            .then((res) => expect(res).toEqual('0'));
    });

    it('возвращает json структуру для xml ответа #2', () => {
        reqMock.reply(200, '<test/>');

        return core.got('http://example.com/test')
            .then((res) => expect(res).toEqual('test'));
    });

    it('возвращает json структуру для xml ответа #3', () => {
        reqMock.reply(200, '<test attr="1"></test>');

        return core.got('http://example.com/test')
            .then((res) => {
                expect(res).toEqual({
                    $: {
                        attr: '1'
                    }
                });
            });
    });

    it('возвращает json структуру для xml ответа #4', () => {
        reqMock.reply(200, '<?xml version="1.0"?><status>ok</status>');

        return core.got('http://example.com/test')
            .then((res) => expect(res).toEqual('ok'));
    });

    describe('allowPlain=true', () => {
        it('игнорирует ParseError на GET-запрос', () => {
            reqMock.reply(200, 'ok');

            return core.got('http://example.com/test', { allowPlain: true, json: true })
                .then((res) => expect(res).toEqual('ok'));
        });

        it('игнорирует ParseError на POST-запрос со строкой', () => {
            const params = { p: 'p' };
            req.post('/test', params)
                .reply(200, 'ok');

            return core.got('http://example.com/test', {
                allowPlain: true,
                json: true,
                body: JSON.stringify(params)
            }).then((res) => expect(res).toEqual('ok'));
        });

        it('игнорирует ParseError на POST-запрос с объектом', () => {
            const params = { p: 'p' };
            req.post('/test', params)
                .reply(200, 'ok');

            return core.got('http://example.com/test', {
                allowPlain: true,
                json: true,
                body: params
            }).then((res) => expect(res).toEqual('ok'));
        });

        it('не применяет xml парсер', () => {
            reqMock.reply(200, 'ok');

            return core.got('http://example.com/test', { allowPlain: true })
                .then((res) => expect(res).toEqual('ok'));
        });
    });

    describe('allowEmpty=true', () => {
        it('должен не выдавать ошибок при пустом теле ответа', () => {
            const body = '';
            reqMock.reply(200, body);

            return core.got('http://example.com/test', { allowEmpty: true })
                .then((res) => expect(res).toEqual(body));
        });

        it('должен не выдавать ошибок при пустом теле ответа и json=true', async () => {
            const body = '';
            reqMock.reply(200, body);

            const res = await core.got('http://example.com/test', { json: true, allowEmpty: true });
            expect(res).toEqual(body);
        });

        it('должен вернуть body, если оно не пустое', () => {
            const body = { test: true };
            reqMock.reply(200, body);

            return core.got('http://example.com/test', { allowEmpty: true, json: true })
                .then((res) => expect(res).toEqual(body));
        });

        it('должен выдать ошибку, если тело не пустое и содержит ошибку', () => {
            const body = { error: { test: true } };

            reqMock.reply(200, body);

            return core.got('http://example.com/test', { allowEmpty: true, json: true })
                .catch((err) => {
                    expect(err).toBeInstanceOf(CUSTOM_ERROR);
                    expect(err.error).toEqual(body.error);
                });
        });

        it('должен распарсить xml, если в теле xml', () => {
            const body = '<?xml version="1.0"?><status>ok</status>';
            reqMock.reply(200, body);

            return core.got('http://example.com/test', { allowEmpty: true })
                .then((res) => expect(res).toEqual('ok'));
        });

        it('getRaw resolves with full response', () => {
            reqMock.reply(200, 'anything');

            return core.got('http://example.com/test', { getRaw: true })
                .then((res) => {
                    expect(res.statusCode).toEqual(200);
                    expect(res.body).toEqual('anything');
                });
        });

        it('getRaw resolves with full response for errors too', () => {
            reqMock.reply(500, { some: 'error' });

            return core.got('http://example.com/test', { getRaw: true, json: true })
                .then((res) => {
                    expect(res.statusCode).toEqual(500);
                    expect(res.body).toEqual({ some: 'error' });
                });
        });

        it('getHeaders resolves with headers', () => {
            reqMock.reply(200, 'body', { 'x-custom-header': 'custom value' });

            return core.got('http://example.com/test', { getHeaders: true })
                .then((res) => {
                    expect(res).toEqual({
                        'x-custom-header': 'custom value'
                    });
                });
        });
    });

    describe('игнор ошибок', () => {
        it('возвращает правильную ошибку для json ответа', () => {
            reqMock.reply(400, {
                error: {
                    code: 42,
                    message: 'test message',
                    reason: 'The Answer'
                }
            });

            return core.got('http://example.com/test', { json: true, dontHttpError: [ 400 ] })
                .then(SHOULD_THROW)
                .catch((res) => {
                    expect(res).toBeInstanceOf(CUSTOM_ERROR);
                    expect(res).toEqual({
                        error: {
                            code: 42,
                            message: 'test message',
                            reason: 'The Answer'
                        }
                    });
                });
        });
    });
});

describe('заголовки запроса по умолчанию', () => {
    it('выставляет заголовки по умолчанию', () => {
        const reqExpect = reqMock
            .matchHeader('user-agent', 'u2709-node')
            .matchHeader('x-real-ip', '2a02:6b8::25')
            .reply(200, '<ok/>');

        core.httpCommonArgs = (options) => {
            options.headers = {
                'user-agent': 'u2709-node',
                'x-real-ip': '2a02:6b8::25'
            };

            return options;
        };

        return core.got('http://example.com/test').then(() => {
            expect(reqExpect.isDone()).toEqual(true);
        });
    });

    it('не перезаписывает кастомные заголовки', () => {
        const reqExpect = reqMock
            .matchHeader('user-agent', 'u2709-node-x')
            .matchHeader('x-real-ip', '2a02:6b8::255')
            .reply(200, '<ok/>');

        return core.got('http://example.com/test', {
            headers: {
                'user-agent': 'u2709-node-x',
                'x-real-ip': '2a02:6b8::255'
            }
        }).then(() => {
            expect(reqExpect.isDone()).toEqual(true);
        });

    });

    describe('добавляет x-forwarded-for', () => {
        it('из x-real-ip', () => {
            const reqExpect = reqMock
                .matchHeader('x-forwarded-for', '2a02:6b8::25')
                .reply(200, '<ok/>');

            return core.got('http://example.com/test').then(() => {
                expect(reqExpect.isDone()).toEqual(true);
            });
        });

        it('из x-forwarded-for, если есть', () => {
            const reqExpect = reqMock
                .matchHeader('x-forwarded-for', '2a02:6b8::25, dead::beef')
                .reply(200, '<ok/>');

            core.httpCommonArgs = (options) => {
                options.headers = {
                    'x-forwarded-for': '2a02:6b8::25, dead::beef'
                };

                return options;
            };

            return core.got('http://example.com/test').then(() => {
                expect(reqExpect.isDone()).toEqual(true);
            });
        });
    });

    it('добавляет x-request-timeout', function() {
        const reqExpect = reqMock
            .matchHeader('x-request-timeout', 1000)
            .reply(200, '<ok/>');

        return core.got('http://example.com/test', { timeout: 1000 }).then(() => {
            expect(reqExpect.isDone()).toEqual(true);
        });
    });

    it('добавляет x-request-attempt', function() {
        const reqExpect = reqMock
            .matchHeader('x-request-attempt', 0)
            .reply(200, '<ok/>');

        return core.got('http://example.com/test').then(() => {
            expect(reqExpect.isDone()).toEqual(true);
        });
    });
});

let params;
let query;

describe('скрытые параметры', () => {
    beforeEach(() => {
        params = {
            login: 'test',
            password: 'secret'
        };
        query = { ...params };
        core.hideParamInLog(query, null, 'password');
    });

    it('в GET-параметрах', () => {
        reqMock.query(params)
            .reply(200, {
                test: 'test'
            });

        return core.got('http://example.com/test', {
            json: true,
            query: query
        }).then(() => {
            expect(JSON.stringify(query)).toEqual('{"login":"test"}');
        });
    });

    it('в теле запроса', () => {
        req.post('/test', params)
            .reply(200, {
                test: 'test'
            });

        return core.got('http://example.com/test', {
            json: true,
            body: query
        }).then(() => {
            expect(JSON.stringify(query)).toEqual('{"login":"test"}');
        });
    });

    it('как тело запроса', () => {
        const o = {
            body: Buffer.from('test')
        };
        core.hideParamInLog(o, null, 'body');

        req.post('/test', 'test')
            .reply(200, {
                test: 'test'
            });

        return core.got('http://example.com/test', {
            json: true,
            body: o.body
        }).then(() => {
            expect(JSON.stringify(o)).toEqual('{}');
        });
    });

    it('в объекте Params', () => {
        const params = Object.assign({}, core.params);
        core.hideParamInLog(core.params, null, '_ckey');

        reqMock.query(params)
            .reply(200, {
                test: 'test'
            });

        return core.got('http://example.com/test', {
            json: true,
            query: core.params
        }).then(() => {
            expect(JSON.stringify(core.params._ckey)).toBeUndefined();
        });
    });

    it('не должен ломать тело запроса', () => {
        const body = Buffer.from('test');

        const reqExpect = req.post('/test', 'test')
            .reply(200, {
                test: 'test'
            });

        return core.got('http://example.com/test', {
            json: true,
            body: body
        }).then(() => {
            expect(reqExpect.isDone()).toEqual(true);
        });
    });

});
