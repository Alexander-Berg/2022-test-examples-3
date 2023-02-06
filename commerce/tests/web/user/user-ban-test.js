require('co-mocha');

const api = require('api');
const { expect } = require('chai');
const nock = require('nock');
const ip = require('ip');
const moment = require('moment');
const config = require('yandex-config');
const request = require('co-supertest').agent(api.callback());

const dbHelper = require('tests/helpers/clear');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const usersFactory = require('tests/factory/usersFactory');
const certificatesFactory = require('tests/factory/certificatesFactory');

const { Ban, GlobalUser, Certificate } = require('db/postgres');

const blackboxHelper = require('tests/helpers/blackbox');

describe('User ban controller', () => {
    const authType = { id: 2, code: 'web' };

    beforeEach(function *() {
        blackboxHelper.nockIntBlackbox({
            response: { login: 'admin', uid: { value: '1234567890' } }
        });

        blackboxHelper.nockExtSeveralUids({
            uid: '111,222',
            userip: ip.address(),
            response: {
                users: [
                    {
                        uid: { value: 111 },
                        'address-list': [
                            { address: 'example1@yandex.ru' }
                        ]
                    },
                    {
                        uid: { value: 222 },
                        'address-list': [
                            { address: 'example2@yandex.ru' }
                        ]
                    }
                ]
            }
        });

        nock(config.sender.host)
            .post(/\/api\/0\/sales\/transactional\/[A-Z0-9-]+\/send/)
            .query(true)
            .times(Infinity)
            .reply(200, {});

        yield dbHelper.clear();
    });

    afterEach(nock.cleanAll);

    function *createAdmin() {
        const admin = { id: 987, uid: 1234567890, login: 'admin' };
        const role = { code: 'admin' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should create and ban globalUser', function *() {
        yield createAdmin();

        yield usersFactory.createWithRelations({
            id: 123,
            uid: 111,
            login: 'banned-user'
        }, { authType });
        yield trialTemplatesFactory.createWithRelations({
            id: 1
        }, {});
        yield trialTemplatesFactory.createWithRelations({
            id: 2
        }, {});

        yield request
            .post('/v1/user/ban')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                login: 'banned-user',
                isSuperban: false,
                reason: 'Ban reason',
                trialTemplateIds: [1, 2]
            })
            .expect(201);

        const bans = yield Ban.findAll({
            attributes: ['globalUserId', 'adminId', 'reason', 'trialTemplateId', 'userLogin'],
            order: [['trialTemplateId']],
            raw: true
        });
        const globalUsers = yield GlobalUser.findAll({
            attributes: ['id', 'actualLogin', 'isBanned']
        });

        expect(globalUsers).to.have.length(1);
        expect(globalUsers[0].actualLogin).to.equal('banned-user');
        expect(globalUsers[0].isBanned).to.be.false;

        expect(bans).to.have.length(2);
        expect(bans[0].globalUserId).to.equal(globalUsers[0].id);
        expect(bans[0].adminId).to.equal(987);
        expect(bans[0].reason).to.equal('Ban reason');
        expect(bans[0].trialTemplateId).to.equal(1);
        expect(bans[0].userLogin).to.equal('banned-user');

        expect(bans[1].globalUserId).to.equal(globalUsers[0].id);
        expect(bans[1].adminId).to.equal(987);
        expect(bans[1].reason).to.equal('Ban reason');
        expect(bans[1].trialTemplateId).to.equal(2);
        expect(bans[1].userLogin).to.equal('banned-user');
    });

    it('should superban user', function *() {
        yield createAdmin();

        yield usersFactory.createWithRelations({
            id: 123,
            uid: 111,
            login: 'banned-user'
        }, { authType });
        yield trialTemplatesFactory.createWithRelations({
            id: 1
        }, {});

        yield request
            .post('/v1/user/ban')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                login: 'banned-user',
                reason: 'Ban reason',
                isSuperban: true,
                trialTemplateIds: [1]
            })
            .expect(201);

        const bans = yield Ban.findAll({
            attributes: ['globalUserId', 'adminId', 'reason', 'trialTemplateId', 'userLogin'],
            order: [['trialTemplateId']],
            raw: true
        });
        const globalUsers = yield GlobalUser.findAll({
            attributes: ['id', 'isBanned']
        });

        expect(globalUsers).to.have.length(1);
        expect(globalUsers[0].isBanned).to.be.true;

        expect(bans).to.have.length(1);
        expect(bans[0].globalUserId).to.equal(globalUsers[0].id);
        expect(bans[0].adminId).to.equal(987);
        expect(bans[0].reason).to.equal('Ban reason');
        expect(bans[0].trialTemplateId).to.equal(1);
        expect(bans[0].userLogin).to.equal('banned-user');
    });

    it('should nullify certificates', function *() {
        yield createAdmin();

        const firstUser = { id: 123, uid: 111, login: 'first-user' };
        const secondUser = { id: 456, uid: 222, login: 'second-user' };
        const dueDate = moment().add(1, 'year').toDate();
        const globalUser = { id: 10 };

        yield certificatesFactory.createWithRelations(
            { id: 23, active: 1, dueDate },
            {
                trial: { id: 1 },
                trialTemplate: { id: 3, language: 0 },
                service: { id: 1, code: 'direct' },
                user: firstUser,
                authType,
                globalUser
            }
        );
        yield certificatesFactory.createWithRelations(
            { id: 32, active: 1, dueDate },
            {
                trial: { id: 2 },
                trialTemplate: { id: 4, language: 0 },
                service: { id: 2, code: 'market' },
                user: secondUser,
                authType,
                globalUser
            }
        );

        yield request
            .post('/v1/user/ban')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                login: 'first-user',
                reason: 'Ban reason',
                isSuperban: false,
                trialTemplateIds: [3, 4]
            })
            .expect(201);

        const actualCerts = yield Certificate.findAll({
            attributes: ['id', 'active', 'deactivateReason'],
            order: [['id']],
            raw: true
        });

        expect(actualCerts).to.deep.equal([
            { id: 23, active: 0, deactivateReason: 'ban' },
            { id: 32, active: 0, deactivateReason: 'ban' }
        ]);
    });

    it('should throw 400 when trial template does not exist', function *() {
        yield createAdmin();

        yield usersFactory.createWithRelations({
            id: 123,
            login: 'banned-user'
        }, { authType });

        yield request
            .post('/v1/user/ban')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                login: 'banned-user',
                isSuperban: false,
                reason: 'Ban reason',
                trialTemplateIds: [1, 2]
            })
            .expect(400)
            .expect({
                message: 'insert or update on table "bans" violates foreign ' +
                    'key constraint "bans_trial_template_id_fkey"',
                internalCode: '400_FCE'
            });
    });

    it('should return 400 when check by schema failed', function *() {
        yield createAdmin();

        yield request
            .post('/v1/user/ban')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ login: 'test', isSuperban: false, reason: 'some reason', trialTemplateIds: 12 })
            .expect(400)
            .expect(({ body }) => {
                expect(body.message).to.equal('Ban check by schema failed');
                expect(body.internalCode).to.equal('400_CSF');
                expect(body.errors.length).to.equal(1);
                expect(body.errors[0].dataPath).to.equal('.trialTemplateIds');
                expect(body.errors[0].message).to.equal('should be array');
            });
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/user/ban')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no support access', function *() {
        yield request
            .post('/v1/user/ban')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no support access',
                internalCode: '403_UNS'
            })
            .end();
    });

    it('should throw 404 when user not found', function *() {
        yield createAdmin();

        yield request
            .post('/v1/user/ban')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                login: 'banned-user',
                isSuperban: false,
                reason: 'Ban reason',
                trialTemplateIds: [1]
            })
            .expect(404)
            .expect({
                message: 'User not found',
                internalCode: '404_UNF',
                login: 'banned-user'
            });
    });
});
