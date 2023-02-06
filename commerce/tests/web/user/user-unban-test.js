require('co-mocha');

const api = require('api');
const { expect } = require('chai');
const nock = require('nock');
const request = require('co-supertest').agent(api.callback());

const dbHelper = require('tests/helpers/clear');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const banFactory = require('tests/factory/bansFactory');
const usersFactory = require('tests/factory/usersFactory');
const { Ban, GlobalUser } = require('db/postgres');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;

describe('User unban controller', () => {
    const authType = { code: 'web' };

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

    it('should unban user', function *() {
        yield createAdmin();

        const user = { id: 123, login: 'expert' };
        const globalUser = { id: 1234, actualLogin: 'expert' };

        yield usersFactory.createWithRelations(user, { globalUser, authType });

        yield banFactory.createWithRelations(
            {
                action: 'ban',
                reason: 'Kill him',
                userLogin: 'expert'
            },
            { globalUser, trialTemplate: { id: 1 }, admin: { id: 987 } });

        yield request
            .post('/v1/user/unban')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ login: 'expert', reason: 'User is norm', trialTemplateIds: [1] })
            .expect(201);

        const bans = yield Ban.findAll({
            order: [['id']],
            attributes: [
                'globalUserId',
                'adminId',
                'reason',
                'action',
                'trialTemplateId',
                'userLogin'
            ],
            raw: true
        });

        const expected = [
            {
                globalUserId: 1234,
                adminId: 987,
                reason: 'Kill him',
                action: 'ban',
                trialTemplateId: 1,
                userLogin: 'expert'
            },
            {
                globalUserId: 1234,
                adminId: 987,
                reason: 'User is norm',
                action: 'unban',
                trialTemplateId: 1,
                userLogin: 'expert'
            }
        ];

        expect(bans).to.deep.equal(expected);
    });

    it('should super unban user', function *() {
        yield createAdmin();

        const globalUser = { id: 2, isBanned: true };

        yield usersFactory.createWithRelations(
            { id: 14, login: 'banned-user' },
            { globalUser, authType }
        );

        yield banFactory.createWithRelations(
            {
                action: 'ban',
                reason: 'AXAX',
                userLogin: 'banned-user'
            },
            {
                globalUser,
                trialTemplate: { id: 1 },
                admin: { id: 13, login: 'hi' }
            });

        yield request
            .post('/v1/user/unban')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                login: 'banned-user',
                reason: 'Unban reason',
                isSuperUnban: true,
                trialTemplateIds: [1]
            })
            .expect(201);

        const actualBans = yield Ban.findAll({
            attributes: ['globalUserId', 'adminId', 'reason', 'trialTemplateId', 'userLogin'],
            order: [['startedDate']],
            raw: true
        });

        const actualGlobalUsers = yield GlobalUser.findAll({
            attributes: ['id', 'isBanned'],
            raw: true
        });

        const expectedBans = [
            {
                globalUserId: 2,
                adminId: 13,
                reason: 'AXAX',
                trialTemplateId: 1,
                userLogin: 'banned-user'
            },
            {
                globalUserId: 2,
                adminId: 987,
                reason: 'Unban reason',
                trialTemplateId: 1,
                userLogin: 'banned-user'
            }
        ];

        expect(actualGlobalUsers).to.deep.equal([{ id: 2, isBanned: false }]);
        expect(actualBans).to.deep.equal(expectedBans);
    });

    it('should return 400 when check by schema failed', function *() {
        yield createAdmin();

        yield request
            .post('/v1/user/unban')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ login: 12, reason: 'some reason' })
            .expect(400)
            .expect(({ body }) => {
                expect(body.message).to.equal('Unban check by schema failed');
                expect(body.internalCode).to.equal('400_CSF');
                expect(body.errors.length).to.equal(1);
                expect(body.errors[0].dataPath).to.equal('.login');
                expect(body.errors[0].message).to.equal('should be string');
            });
    });

    it('should return 404 when user not found', function *() {
        yield createAdmin();

        yield request
            .post('/v1/user/unban')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ login: 'not-exist', trialTemplateIds: [1] })
            .expect(404)
            .expect({
                message: 'User not found',
                internalCode: '404_UNF',
                login: 'not-exist'
            });
    });

    it('should return 404 when global user not found', function *() {
        yield createAdmin();

        yield usersFactory.createWithRelations({
            login: 'some-user',
            globalUserId: null
        }, { authType });

        yield request
            .post('/v1/user/unban')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ login: 'some-user', trialTemplateIds: [1] })
            .expect(404)
            .expect({
                message: 'Global user not found',
                internalCode: '404_GNF',
                userLogin: 'some-user'
            });
    });

    it('should return 400 when check by schema failed', function *() {
        yield createAdmin();

        yield request
            .post('/v1/user/unban')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ login: 12, reason: 'some reason' })
            .expect(400)
            .expect(({ body }) => {
                expect(body.message).to.equal('Unban check by schema failed');
                expect(body.internalCode).to.equal('400_CSF');
                expect(body.errors.length).to.equal(1);
                expect(body.errors[0].dataPath).to.equal('.login');
                expect(body.errors[0].message).to.equal('should be string');
            });
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/user/unban')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no support access', function *() {
        yield request
            .post('/v1/user/unban')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no support access',
                internalCode: '403_UNS'
            })
            .end();
    });
});
