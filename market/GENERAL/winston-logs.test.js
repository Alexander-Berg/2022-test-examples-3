const { getLogger, noticeError } = require('./winston-logs');

describe('Test winston log functions', () => {
    test('Should get winston logger', () => {
        const fileName = 'sovetnik.log';
        const logName = 'sovetnik-log';
        const testTransport = getLogger(fileName, logName);
        expect(testTransport).toBeDefined();
    });

    test('Should fail - no fileName', () => {
        const fileName = '';
        const logName = 'sovetnik-log';
        let testLogger;
        let errorMessage;
        try {
            testLogger = getLogger(fileName, logName);
        } catch (e) {
            errorMessage = e.message;
        }
        expect(testLogger).toBeUndefined();
        expect(errorMessage).toMatch('logFileName is not defined');
    });

    test('Should fail - no logName', () => {
        const fileName = 'sovetnik.log';
        const logName = '';
        let testLogger;
        let errorMessage;
        try {
            testLogger = getLogger(fileName, logName);
        } catch (e) {
            errorMessage = e.message;
        }
        expect(testLogger).toBeUndefined();
        expect(errorMessage).toMatch('logName is not defined');
    });

    test('Should noticeError', () => {
        const req = {
            connection: { remoteAddress: '127.0.0.1' },
            cookies: { yandex_login: 'Test User' },
            headers: {
                referer: 'http://ya.ru',
                referrer: 'http://ya.ru',
            },
            query: {
                ref: 'http://ya.ru',
                affId: 1234,
            },
        };
        const error = {
            name: 'Test Error Name',
            message: 'Test Error Message',
        };

        let success;
        try {
            noticeError(error, req);
            success = true;
        } catch (e) {
            success = false;
        }
        expect(success).toBeTruthy();
    });
});
