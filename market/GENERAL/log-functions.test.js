const {
    replaceToken,
    sanitizeValue,
    getIp,
    getLogsFromQuery,
    makeLogString,
} = require('./log-functions');

describe('Test main log functions', () => {
    test('Should replace sencetive user token', () => {
        const stringWithToken = 'http://yandex.ru/?token=1as6s876f9876d987s69876d';
        expect(replaceToken(stringWithToken)).toMatch('http://yandex.ru/?token=TOKEN_WAS_HERE');
    });

    test('Should escape bad symbols', () => {
        const stringWithToken = 'Yandex\nMarket\tOne Love';
        expect(sanitizeValue(stringWithToken)).toMatch('Yandex\\nMarket\\tOne Love');
    });

    test('Should get Ip from Req', () => {
        const req = {
            headers: { 'x-forwarded-for': '127.0.0.1' },
            connection: { socket: { remoteAddress: '1.2.3.4' }, remoteAddress: '192.168.44.1' },
            socket: { remoteAddress: '8.8.8.8' },
        };
        expect(getIp(req)).toMatch('127.0.0.1');
    });

    test('Should get log fields from', () => {
        const req = {
            headers: { 'x-forwarded-for': '127.0.0.1' },
            cookies: { 'svt-user': '441066' },
            clid: 1234,
            aff_id: 12,
            v: 202108071400,
        };
        expect(getLogsFromQuery(req)).toMatchObject({
            clid: 1234,
            aff_id: 12,
            v: 202108071400,
        });
    });

    test('Should make tskv log string from object', () => {
        const logs = { key1: 'value1', key2: 'value2' };
        expect(makeLogString(logs)).toMatch('key1=value1\tkey2=value2');
    });
});
