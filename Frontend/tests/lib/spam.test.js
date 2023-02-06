const assert = require('assert');
const nock = require('nock');

const isSpamCheck = require('lib/spam');
const { nockSpamCheckFail, nockSpamCheckSuccess } = require('tests/mocks');

describe('isSpamCheck', () => {
    afterEach(() => nock.cleanAll());

    const ip = '123.123.123.123';
    const realpath = 'http://test.com';
    const uid = '123';
    const fields = [{ slug: 'test', value: 'test' }];

    it('should return true if request return spam', async() => {
        nockSpamCheckFail();

        const isSpam = await isSpamCheck({ ip, realpath, uid, fields });

        assert(isSpam);
    });

    it('should return false if request return not spam', async() => {
        nockSpamCheckSuccess();

        const isSpam = await isSpamCheck({ ip, realpath, uid, fields });

        assert(!isSpam);
    });
});
