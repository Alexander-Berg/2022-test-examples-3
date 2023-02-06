require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const nock = require('nock');

const config = require('yandex-config');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const nockAvatars = require('tests/helpers/mdsServices').avatars;
const dbHelper = require('tests/helpers/clear');

const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');

const { ProctoringResponses, Certificate } = require('db/postgres');

describe('Admin appeal controller', () => {
    beforeEach(function *() {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
        nockAvatars.success();

        nock(config.sender.host)
            .post(/\/api\/0\/sales\/transactional\/[A-Z0-9-]+\/send/)
            .query(true)
            .times(Infinity)
            .reply(200, {});

        yield dbHelper.clear();
    });

    afterEach(nock.cleanAll);

    function *createAdmin() {
        const admin = { uid: 1234567890 };
        const role = { code: 'admin' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should write to proctoring responses', function *() {
        yield createAdmin();

        yield proctoringResponsesFactory.createWithRelations(
            { source: 'proctoring', verdict: 'pending', isLast: true },
            { trial: { id: 7 } }
        );

        yield request
            .post('/v1/admin/appeal')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptId: 7, verdict: 'failed', email: 'dotokoto@yandex-team.ru' })
            .expect(204)
            .end();

        const responses = yield ProctoringResponses.findAll({
            attributes: ['trialId', 'source', 'verdict', 'isLast'],
            order: [['time']],
            raw: true
        });

        const expected = [
            {
                trialId: 7,
                source: 'proctoring',
                verdict: 'pending',
                isLast: false
            },
            {
                trialId: 7,
                source: 'appeal',
                verdict: 'failed',
                isLast: true
            }
        ];

        expect(responses).to.deep.equal(expected);
    });

    it('should create certificate when verdict is `correct`', function *() {
        yield createAdmin();

        yield trialsFactory.createWithRelations({ id: 7, passed: 1 });

        yield request
            .post('/v1/admin/appeal')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptId: 7, verdict: 'correct', email: 'semenmakhaev@yandex-team.ru' })
            .expect(204)
            .end();

        const actual = yield Certificate.findAll({ attributes: ['trialId'], raw: true });

        expect(actual).to.deep.equal([{ trialId: 7 }]);
    });

    it('should not create certificate when verdict is `failed`', function *() {
        yield createAdmin();

        yield trialsFactory.createWithRelations({ id: 7, passed: 1 });

        yield request
            .post('/v1/admin/appeal')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptId: 7, verdict: 'failed', email: 'sinseveria@yandex-team.ru' })
            .expect(204)
            .end();

        const actual = yield Certificate.findAll();

        expect(actual).to.deep.equal([]);
    });

    it('should not create certificate when attempt not passed', function *() {
        yield createAdmin();

        yield trialsFactory.createWithRelations({ id: 7, passed: 0 });

        yield request
            .post('/v1/admin/appeal')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptId: 7, verdict: 'correct', email: 'sinseveria@yandex-team.ru' })
            .expect(204)
            .end();

        const actual = yield Certificate.findAll();

        expect(actual).to.deep.equal([]);
    });

    it('should throw 400 when attempt id is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/appeal')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptId: 'abc', verdict: 'failed' })
            .expect(400)
            .expect({
                message: 'Attempt id is invalid',
                internalCode: '400_AII',
                attemptId: 'abc'
            })
            .end();
    });

    it('should throw 400 when verdict is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/appeal')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptId: 7, verdict: 'pending' })
            .expect(400)
            .expect({
                message: 'Verdict is invalid',
                internalCode: '400_VII',
                verdict: 'pending'
            })
            .end();
    });

    it('should throw 400 when email is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/appeal')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptId: 7, verdict: 'correct', email: 'inv@lid' })
            .expect(400)
            .expect({
                message: 'User email is invalid',
                internalCode: '400_UEI',
                email: 'inv@lid'
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/admin/appeal')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no access to revise video', function *() {
        yield request
            .post('/v1/admin/appeal')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no access to revise video',
                internalCode: '403_NRV'
            })
            .end();
    });

    it('should throw 403 when appeal has already been made', function *() {
        yield createAdmin();

        yield proctoringResponsesFactory.createWithRelations(
            { source: 'appeal', verdict: 'correct', isLast: true },
            { trial: { id: 13, passed: 1 } }
        );

        yield request
            .post('/v1/admin/appeal')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptId: 13, verdict: 'correct', email: 'dotokoto@ya.ru' })
            .expect(403)
            .expect({
                message: 'The appeal has already been made',
                internalCode: '403_AAM'
            })
            .end();

        const certificates = yield Certificate.findAll();

        expect(certificates).to.deep.equal([]);
    });

    it('should throw 404 when attempt not found', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/appeal')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptId: 7, verdict: 'correct', email: 'm-smirnov@yandex-team.ru' })
            .expect(404)
            .expect({
                message: 'Attempt not found',
                internalCode: '404_ATF'
            })
            .end();
    });
});
