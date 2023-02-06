require('co-mocha');

const api = require('api');
const { expect } = require('chai');
const nock = require('nock');
const request = require('co-supertest').agent(api.callback());

const dbHelper = require('tests/helpers/clear');

const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const usersFactory = require('tests/factory/usersFactory');
const bansFactory = require('tests/factory/bansFactory');

const { Ban, User, GlobalUser } = require('db/postgres');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;

describe('User associateUsers controller', () => {
    const authType = { id: 2, code: 'web' };

    beforeEach(function *() {
        nockBlackbox({ response: { login: 'admin', uid: { value: '1234567890' } } });

        yield dbHelper.clear();
    });

    afterEach(nock.cleanAll);

    function *createAdmin() {
        const admin = { id: 987, uid: 1234567890, login: 'admin' };
        const role = { code: 'admin' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should associate users', function *() {
        yield createAdmin();

        const firstUser = { id: 1, uid: 1010, login: 'first' };
        const secondUser = { id: 2, uid: 2020, login: 'second' };

        yield usersFactory.createWithRelations(firstUser, {
            globalUser: {
                id: 10,
                actualLogin: 'first',
                isBanned: false,
                isActive: true
            },
            authType
        });
        yield usersFactory.createWithRelations(secondUser, {
            globalUser: {
                id: 20,
                actualLogin: 'second',
                isBanned: false,
                isActive: true
            },
            authType
        });
        yield bansFactory.createWithRelations(
            {
                action: 'ban',
                startedDate: new Date(1, 1, 1),
                expiredDate: new Date(2, 2, 2),
                userLogin: 'second',
                reason: 'патаму шта',
                isLast: true
            },
            {
                trialTemplate: { id: 1 },
                globalUser: { id: 20 },
                admin: { id: 123 }
            }
        );

        yield request
            .post('/v1/user/associate')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ logins: ['first', 'second'] })
            .expect(204);

        const actualUsers = yield User.findAll({
            attributes: ['globalUserId'],
            raw: true
        });
        const actualGlobalUsers = yield GlobalUser.findAll({
            attributes: ['id', 'actualLogin', 'isActive', 'isBanned'],
            order: [['id']],
            raw: true
        });
        const actualBans = yield Ban.findAll({
            attributes: [
                'adminId',
                'trialTemplateId',
                'globalUserId',
                'action',
                'startedDate',
                'expiredDate',
                'reason',
                'userLogin',
                'isLast'
            ],
            order: [['globalUserId']],
            raw: true
        });

        expect(actualUsers).to.deep.equal([
            { globalUserId: 10 },
            { globalUserId: 10 }
        ]);
        expect(actualGlobalUsers).to.deep.equal([
            { id: 10, actualLogin: 'first', isActive: true, isBanned: false },
            { id: 20, actualLogin: 'second', isActive: false, isBanned: false }
        ]);
        expect(actualBans).to.deep.equal([
            {
                adminId: 123,
                trialTemplateId: 1,
                globalUserId: 10,
                action: 'ban',
                startedDate: new Date(1, 1, 1),
                expiredDate: new Date(2, 2, 2),
                userLogin: 'second',
                reason: 'патаму шта',
                isLast: true
            },
            {
                adminId: 123,
                trialTemplateId: 1,
                globalUserId: 20,
                action: 'ban',
                startedDate: new Date(1, 1, 1),
                expiredDate: new Date(2, 2, 2),
                userLogin: 'second',
                reason: 'патаму шта',
                isLast: true
            }
        ]);
    });

    it('should throw 400 when logins is not array', function *() {
        yield createAdmin();

        yield request
            .post('/v1/user/associate')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ logins: 'some' })
            .expect(400)
            .expect({
                message: 'Logins is not an array',
                internalCode: '400_LNA',
                logins: 'some'
            })
            .end();
    });

    it('should throw 400 when login is not a string', function *() {
        yield createAdmin();

        yield request
            .post('/v1/user/associate')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ logins: ['good', 1] })
            .expect(400)
            .expect({
                message: 'Login is not a string',
                internalCode: '400_LNS',
                logins: ['good', 1]
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/user/associate')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no support access', function *() {
        yield request
            .post('/v1/user/associate')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no support access',
                internalCode: '403_UNS'
            })
            .end();
    });

    it('should throw 404 when some users not found', function *() {
        yield createAdmin();

        yield usersFactory.createWithRelations({ login: 'user' }, { authType });

        yield request
            .post('/v1/user/associate')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ logins: ['user', 'not-user'] })
            .expect(404)
            .expect({
                message: 'Some users not found',
                internalCode: '404_SUN',
                logins: ['not-user']
            });
    });
});
