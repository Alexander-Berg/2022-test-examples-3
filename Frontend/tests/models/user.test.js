const assert = require('assert');
const nock = require('nock');
const catchError = require('catch-error-async');
const config = require('yandex-cfg');

const { nockTvmtool } = require('tests/mocks');

const User = require('models/user');

describe('User model', () => {
    describe('fetchBlackboxUser', () => {
        afterEach(nock.cleanAll);

        it('should request blackbox and return User', async() => {
            nock(`http://${config.blackbox.api}/`)
                .get('/blackbox')
                .query(data => data.sessionid === 'user-session-id' && data.method === 'sessionid')
                .reply(200, { status: { id: 0 }, login: 'zhigalov' });
            nockTvmtool();

            const actual = await User.fetchBlackboxUser({ sessionid: 'user-session-id' });

            assert(actual instanceof User);
            assert.equal(actual.login, 'zhigalov');
        });

        it('should throw error when blackbox returned error', async() => {
            nock(`http://${config.blackbox.api}/`)
                .get('/blackbox')
                .query(data => data.sessionid === 'user-session-id' && data.method === 'sessionid')
                .reply(200, { status: { id: -1 } });
            nockTvmtool();

            const error = await catchError(
                User.fetchBlackboxUser.bind(User), { sessionid: 'user-session-id' });

            assert.equal(error.message, 'Invalid user data');
            assert.equal(error.statusCode, 401);
            assert.deepEqual(error.options, {
                internalCode: '401_IUD',
                body: { status: { id: -1 } },
            });
        });
    });
});
