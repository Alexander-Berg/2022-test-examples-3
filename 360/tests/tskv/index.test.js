const tskv = require('../../tskv');
const ask = require('asker-as-promised');

import { popFnCalls } from '../helpers.js';

const mockLogger = {
    log: jest.fn()
};

const logger = tskv(mockLogger);
const loggerWithExtraParams = tskv(mockLogger, (req) => ({
    from: `${req.ip} (${req.hostname})`
}));
const TEST_COMPONENT = 'test component';

const mockedReq = {
    ycrid: 'test-ycrid',
    hostname: 'disk.yandex.ru',
    ip: '127.0.0.1',
    user: {
        id: 123,
        login: 'test-tskv-logger (!!!DONT LOG LOGIN!!!)'
    },
    method: 'POST',
    originalUrl: '/alala?param=value'
};
const mockedResponseMeta = {
    time: {
        total: 50
    },
    options: {
        port: 443,
        protocol: 'https:',
        hostname: 'mpfs.yandex.net',
        method: 'POST',
        path: '/handle-path?with=param1&also-with=param2',
        query: {
            with: 'param1',
            'also-with': 'param2'
        },
        body: {
            bodyParam1: 'body-value-1',
            bodyParam2: 'body-value-2'
        }
    }
};

describe('tskv', () => {
    it('логирование исключения', () => {
        logger.log({}, TEST_COMPONENT, {}, new Error('test tskv error'));
        const logCalls = popFnCalls(mockLogger.log);
        expect(logCalls.length).toEqual(1);
        expect(logCalls[0].length).toEqual(1);
        const logParams = logCalls[0][0];

        expect(logParams.level).toEqual('error');
        expect(logParams.component).toEqual(TEST_COMPONENT);
        expect(logParams.errorCode).toEqual('UNKNOWN_ERROR');
        expect(logParams.message).toEqual('UNKNOWN_ERROR Unknown error. test tskv error.');
        expect(logParams.stack).toMatch(/^UNKNOWN_ERROR Terror: Unknown error\n    .+ensureError/);
        expect(logParams.uid).toEqual(0);
        expect(logParams.login).toBeUndefined();

        // удалим переменные поля
        delete logParams.unixtime;
        delete logParams.stack;
        expect(logParams).toMatchSnapshot();
    });

    it('логирование исключения с запросом и параметрами', () => {
        logger.log(mockedReq, TEST_COMPONENT, {
            query: {
                param: 'value'
            },
            serviceBody: {
                serviceBodyParam1: 'service-body-value-1',
                serviceBodyParam2: 'service-body-value-2'
            },
            justSomeAdditionalLogOption1: 1,
            justSomeAdditionalLogOption2: 'ddddd',
            justSomeAdditionalLogOption3: null
        }, new Error('test one more tskv error'));

        const logCalls = popFnCalls(mockLogger.log);
        expect(logCalls.length).toEqual(1);
        expect(logCalls[0].length).toEqual(1);
        const logParams = logCalls[0][0];

        expect(logParams.level).toEqual('error');
        expect(logParams.component).toEqual(TEST_COMPONENT);
        expect(logParams.errorCode).toEqual('UNKNOWN_ERROR');
        expect(logParams.message).toEqual('UNKNOWN_ERROR Unknown error. test one more tskv error.');
        expect(logParams.stack).toMatch(/^UNKNOWN_ERROR Terror: Unknown error\n    .+ensureError/);
        expect(logParams.uid).toEqual(123);
        // не логируем логин, требование СИБ
        expect(logParams.login).toBeUndefined();

        // удалим переменные поля
        delete logParams.unixtime;
        delete logParams.stack;
        expect(logParams).toMatchSnapshot();
    });

    it('логирование AskerError', () => {
        logger.log({}, TEST_COMPONENT, {},
            ask.Error.createError(
                ask.Error.CODES.UNEXPECTED_STATUS_CODE,
                {
                    statusCode: 500,
                    url: 'https://blackbox.yandex.ru/some/handle/?sessionid=a590f',
                    time: {
                        total: 100
                    }
                })
        );
        const logCalls = popFnCalls(mockLogger.log);
        expect(logCalls.length).toEqual(1);
        expect(logCalls[0].length).toEqual(1);
        const logParams = logCalls[0][0];

        expect(logParams.level).toEqual('error');
        expect(logParams.component).toEqual(TEST_COMPONENT);
        expect(logParams.errorCode).toEqual('UNEXPECTED_STATUS_CODE');
        expect(logParams.time).toEqual(100);
        expect(logParams.statusCode).toEqual(500);
        expect(logParams.errorUrl).toEqual('https://blackbox.yandex.ru/some/handle/?sessionid=a590f');
        expect(logParams.uid).toEqual(0);
        expect(logParams.login).toBeUndefined();

        // удалим переменные поля
        delete logParams.unixtime;
        delete logParams.stack;
        expect(logParams).toMatchSnapshot();
    });

    it('логирование какой-то информации', () => {
        logger.log(mockedReq, TEST_COMPONENT, {
            action: 'react-script-load-time',
            time: 999999
        });

        const logCalls = popFnCalls(mockLogger.log);
        expect(logCalls.length).toEqual(1);
        expect(logCalls[0].length).toEqual(1);
        const logParams = logCalls[0][0];

        expect(logParams.level).toEqual('info');
        expect(logParams.component).toEqual(TEST_COMPONENT);
        expect(logParams.uid).toEqual(123);
        // не логируем логин, требование СИБ
        expect(logParams.login).toBeUndefined();

        // удалим переменные поля
        delete logParams.unixtime;
        expect(logParams).toMatchSnapshot();
    });

    it('логирование предупреждения (level=warn)', () => {
        logger.log(mockedReq, TEST_COMPONENT, {
            level: 'warn'
        });

        const logCalls = popFnCalls(mockLogger.log);
        expect(logCalls.length).toEqual(1);
        expect(logCalls[0].length).toEqual(1);
        const logParams = logCalls[0][0];

        expect(logParams.level).toEqual('warn');
        expect(logParams.component).toEqual(TEST_COMPONENT);
        expect(logParams.uid).toEqual(123);
        // не логируем логин, требование СИБ
        expect(logParams.login).toBeUndefined();

        // удалим переменные поля
        delete logParams.unixtime;
        expect(logParams).toMatchSnapshot();
    });

    it('логирование крита (level=crit)', () => {
        logger.log(mockedReq, TEST_COMPONENT, {
            level: 'crit'
        }, new Error('ААА! Всё упало!'));

        const logCalls = popFnCalls(mockLogger.log);
        expect(logCalls.length).toEqual(1);
        expect(logCalls[0].length).toEqual(1);
        const logParams = logCalls[0][0];

        expect(logParams.level).toEqual('crit');
        expect(logParams.component).toEqual(TEST_COMPONENT);
        expect(logParams.message).toEqual('UNKNOWN_ERROR Unknown error. ААА! Всё упало!.');
        expect(logParams.uid).toEqual(123);
        // не логируем логин, требование СИБ
        expect(logParams.login).toBeUndefined();

        // удалим переменные поля
        delete logParams.unixtime;
        delete logParams.stack;
        expect(logParams).toMatchSnapshot();
    });

    it('логирование с дополнительными параметрами', () => {
        loggerWithExtraParams.log(mockedReq, TEST_COMPONENT, {
            id: 'a72821db-735f-46c0-aab7-da1fc2f37a48'
        });

        const logCalls = popFnCalls(mockLogger.log);
        expect(logCalls.length).toEqual(1);
        expect(logCalls[0].length).toEqual(1);
        const logParams = logCalls[0][0];

        expect(logParams.level).toEqual('info');
        expect(logParams.component).toEqual(TEST_COMPONENT);
        expect(logParams.uid).toEqual(123);
        // не логируем логин, требование СИБ
        expect(logParams.login).toBeUndefined();

        // удалим переменные поля
        delete logParams.unixtime;
        expect(logParams).toMatchSnapshot();
    });

    it('логирование с постобработкой', () => {
        logger.log(mockedReq, TEST_COMPONENT, {
            id: 'a72821db-735f-46c0-aab7-da1fc2f37a48'
        }, new Error('Request https://somebackend/do?login=me&password=secret failed'), (params) => {
            return Object.assign(params, {
                ip: params.ip.replace(/\d/g, 'X'),
                message: params.message.replace(/password=[^\s]+/g, 'password=XXX'),
                stack: params.stack.replace(/password=[^\s]+/g, 'password=XXX').replace(/\s+at[^\n]+/g, '')
            });
        });

        const logCalls = popFnCalls(mockLogger.log);
        expect(logCalls.length).toEqual(1);
        expect(logCalls[0].length).toEqual(1);
        const logParams = logCalls[0][0];

        expect(logParams.level).toEqual('error');
        expect(logParams.component).toEqual(TEST_COMPONENT);
        expect(logParams.uid).toEqual(123);
        // не логируем логин, требование СИБ
        expect(logParams.login).toBeUndefined();

        // удалим переменные поля
        delete logParams.unixtime;
        expect(logParams).toMatchSnapshot();
    });

    it('логирование сетевого запроса (успешного, с метаданными)', () => {
        logger.logAskerResponse(mockedReq, TEST_COMPONENT, {
            statusCode: 200,
            data: new Buffer('response data is here'),
            meta: mockedResponseMeta
        });

        const logCalls = popFnCalls(mockLogger.log);
        expect(logCalls.length).toEqual(1);
        expect(logCalls[0].length).toEqual(1);
        const logParams = logCalls[0][0];

        expect(logParams.level).toEqual('info');
        expect(logParams.component).toEqual(TEST_COMPONENT);
        expect(logParams.statusCode).toEqual(200);
        expect(logParams.time).toEqual(50);
        expect(logParams.serviceHost).toEqual('mpfs.yandex.net');
        expect(logParams.servicePath).toEqual('/handle-path');
        expect(logParams.uid).toEqual(123);
        // не логируем логин, требование СИБ
        expect(logParams.login).toBeUndefined();

        // удалим переменные поля
        delete logParams.unixtime;
        expect(logParams).toMatchSnapshot();
    });

    it('логирование сетевого запроса (успешного, без метаданных)', () => {
        logger.logAskerResponse(mockedReq, TEST_COMPONENT, {
            statusCode: 301
        });

        const logCalls = popFnCalls(mockLogger.log);
        expect(logCalls.length).toEqual(1);
        expect(logCalls[0].length).toEqual(1);
        const logParams = logCalls[0][0];

        expect(logParams.level).toEqual('info');
        expect(logParams.component).toEqual(TEST_COMPONENT);
        expect(logParams.statusCode).toEqual(301);
        expect(logParams.time).toBeUndefined();
        expect(logParams.serviceHost).toBeUndefined();
        expect(logParams.servicePath).toBeUndefined();
        expect(logParams.uid).toEqual(123);
        // не логируем логин, требование СИБ
        expect(logParams.login).toBeUndefined();

        // удалим переменные поля
        delete logParams.unixtime;
        expect(logParams).toMatchSnapshot();
    });

    it('логирование сетевого запроса (упавшего, с метаданными)', () => {
        logger.logAskerResponse(mockedReq, TEST_COMPONENT, {
            statusCode: 400,
            data: new Buffer('bad bad request'),
            meta: mockedResponseMeta
        });

        const logCalls = popFnCalls(mockLogger.log);
        expect(logCalls.length).toEqual(1);
        expect(logCalls[0].length).toEqual(1);
        const logParams = logCalls[0][0];

        expect(logParams.level).toEqual('warn');
        expect(logParams.component).toEqual(TEST_COMPONENT);
        expect(logParams.statusCode).toEqual(400);
        expect(logParams.time).toEqual(50);
        expect(logParams.serviceHost).toEqual('mpfs.yandex.net');
        expect(logParams.servicePath).toEqual('/handle-path');
        expect(logParams.responseData).toEqual('bad bad request');
        expect(logParams.uid).toEqual(123);
        // не логируем логин, требование СИБ
        expect(logParams.login).toBeUndefined();

        // удалим переменные поля
        delete logParams.unixtime;
        expect(logParams).toMatchSnapshot();
    });
});
