require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const nock = require('nock');
const moment = require('moment');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');

const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const bansFactory = require('tests/factory/bansFactory');
const usersFactory = require('tests/factory/usersFactory');

describe('Admin getUserAssociations controller', () => {
    beforeEach(function *() {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });

        yield dbHelper.clear();
    });

    afterEach(nock.cleanAll);

    function *createAdmin() {
        const admin = { uid: 1234567890 };
        const role = { code: 'admin' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should return associated users and global user info', function *() {
        yield createAdmin();

        const authType = { id: 2, code: 'web' };
        const startedDate = new Date();
        const expiredDate = moment(startedDate).add(1, 'month').toDate();
        const globalUser = { id: 10, isBanned: false, actualLogin: 'user-1@ya.ru' };

        yield usersFactory.createWithRelations({ id: 1, uid: 1, login: 'user-1@ya.ru' }, { globalUser, authType });
        yield usersFactory.createWithRelations({ id: 2, uid: 2, login: 'user-2' }, { globalUser, authType });

        yield bansFactory.createWithRelations({
            id: 1,
            action: 'ban',
            reason: 'cheater',
            startedDate,
            expiredDate,
            userLogin: 'user-1@ya.ru',
            isLast: true
        }, {
            admin: { login: 'anyok' },
            trialTemplate: { id: 1 },
            globalUser
        });

        const actual = yield request
            .get('/v1/admin/user/associations?login=user-1@ya.ru')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200);

        expect(actual.body).to.deep.equal({
            logins: ['user-1@ya.ru', 'user-2'],
            globalUserInfo: {
                actualLogin: 'user-1@ya.ru',
                isBanned: false,
                bans: [{
                    action: 'ban',
                    admin: { login: 'anyok' },
                    reason: 'cheater',
                    trialTemplateId: 1,
                    startedDate: startedDate.toISOString(),
                    expiredDate: expiredDate.toISOString(),
                    userLogin: 'user-1@ya.ru',
                    isLast: true
                }]
            }
        });
    });

    it('should return empty data if user has no global id', function *() {
        yield createAdmin();

        const authType = { id: 2, code: 'web' };

        yield usersFactory.createWithRelations({
            id: 1,
            login: 'user',
            globalUserId: null
        }, { authType });

        const actual = yield request
            .get('/v1/admin/user/associations?login=user')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200);

        expect(actual.body).to.deep.equal({
            logins: [],
            globalUserInfo: {}
        });
    });

    it('should throw 400 when login is not a string', function *() {
        yield createAdmin();

        yield request
            .get('/v1/admin/user/associations')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({})
            .expect(400)
            .expect({
                message: 'Login should be a string',
                internalCode: '400_LSS'
            });
    });

    it('should throw 404 when user not found', function *() {
        yield createAdmin();

        yield request
            .get('/v1/admin/user/associations?login=not-exist')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect({
                message: 'User not found',
                internalCode: '404_UNF',
                login: 'not-exist'
            });
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .get('/v1/admin/user/associations')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no support access', function *() {
        yield request
            .get('/v1/admin/user/associations')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no support access',
                internalCode: '403_UNS'
            })
            .end();
    });
});
