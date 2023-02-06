const assert = require('assert');
const nock = require('nock');

const Heartbeat = require('models/heartbeat');
const cleanDb = require('tests/db/clean');

describe('Heartbeat model', () => {
    beforeEach(cleanDb);

    describe('getCheckLevel', () => {
        afterEach(nock.cleanAll);

        it('should return `ok` when all checks success', async() => {
            nock('https://some.api.yandex.net')
                .get('/ping')
                .reply(200);

            const checksByLevels = [
                {
                    level: 'crit',
                    checks: [{ type: 'db' }],
                },
                {
                    level: 'warn',
                    checks: [{ type: 'http', params: [{ host: 'some.api.yandex.net' }] }],
                },
            ];
            const actual = await Heartbeat.getCheckLevel(checksByLevels);

            assert.equal(actual, 'ok');
        });

        it('should return level name from first failed group', async() => {
            nock('https://some.api.yandex.net')
                .get('/ping')
                .reply(500);

            const checksByLevels = [
                {
                    level: 'crit',
                    checks: [{ type: 'db' }],
                },
                {
                    level: 'warn',
                    checks: [{ type: 'http', params: [{ host: 'some.api.yandex.net' }] }],
                },
            ];
            const actual = await Heartbeat.getCheckLevel(checksByLevels);

            assert.equal(actual, 'warn');
        });

        it('should not run next level checks when first level failed', async() => {
            const critRequest = nock('https://crit.api.yandex.net')
                .get('/ping')
                .reply(500);
            const warnRequest = nock('https://warn.api.yandex.net')
                .get('/ping')
                .reply(200);

            const checksByLevels = [
                {
                    level: 'crit',
                    checks: [{ type: 'http', params: [{ host: 'crit.api.yandex.net' }] }],
                },
                {
                    level: 'warn',
                    checks: [{ type: 'http', params: [{ host: 'warn.api.yandex.net' }] }],
                },
            ];
            const actual = await Heartbeat.getCheckLevel(checksByLevels);

            assert.equal(actual, 'crit');
            assert(critRequest.isDone(), 'Crit check should be called');
            assert(!warnRequest.isDone(), 'Warn check should not be called');
        });
    });
});
