const Logger = require('../../../src/server/logger');

describe('logger', () => {
    it('должен формировать правильную структуру сообщения', () => {
        const fields = { someField: 'someValue' };
        const login = 'username';
        const reqId = '12345';
        const msg = 'some msg';
        const level = 'WARNING';

        const expected = {
            msg,
            '@fields': Object.assign(
                {
                    reqId,
                    login,
                },
                fields,
            ),
            levelStr: level,
        };

        const logger = new Logger(reqId, login);
        const record = logger._getRecord(msg, fields, level);

        assert.deepEqual(JSON.parse(record), expected);
    });
});
