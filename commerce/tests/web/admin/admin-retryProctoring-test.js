require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const nock = require('nock');
const { expect } = require('chai');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const nockProctorEdu = require('tests/helpers/proctorEdu');
const nockAvatars = require('tests/helpers/mdsServices').avatars;

const dbHelper = require('tests/helpers/clear');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const trialsFactory = require('tests/factory/trialsFactory');

const {
    Certificate,
    ProctoringResponses
} = require('db/postgres');

describe('Admin proctoring retry controller', () => {
    beforeEach(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
        nockAvatars.success();
    });

    afterEach(nock.cleanAll);

    beforeEach(dbHelper.clear);

    function *createAdmin() {
        const admin = { uid: 1234567890 };
        const role = { code: 'admin' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should return passed and proctoring verdict', function *() {
        yield createAdmin();

        const trial = { id: 1, expired: 0, openId: 'correct-open-id', passed: 0 };
        const trialTemplate = { id: 7, isProctoring: true };

        yield trialsFactory.createWithRelations(trial, { trialTemplate });

        nockProctorEdu.protocol({
            openId: trial.openId,
            response: { id: 'openId', evaluation: 99 }
        });

        yield request
            .post('/v1/admin/proctoring/1/retry')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({
                passed: 0,
                verdict: 'correct'
            })
            .end();
    });

    it('should save data to db', function *() {
        yield createAdmin();

        const trial = { id: 1, expired: 0, openId: 'correct-open-id', passed: 1 };
        const trialTemplate = { id: 7, isProctoring: true };

        yield trialsFactory.createWithRelations(trial, { trialTemplate });

        nockProctorEdu.protocol({
            openId: trial.openId,
            response: { id: 'openId', evaluation: 0 }
        });

        yield request
            .post('/v1/admin/proctoring/1/retry')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({
                passed: 1,
                verdict: 'failed'
            })
            .end();

        const proctoringResponse = yield ProctoringResponses.findOne({ where: { trialId: 1 } });

        expect(proctoringResponse.get('source')).to.equal('proctoring');
        expect(proctoringResponse.get('verdict')).to.equal('failed');
    });

    // EXPERTDEV-972: [API] Заваливать попытку по техническим метрикам при завершении попытки без возможности обжаловать
    it('should correct process metrics', function *() {
        yield createAdmin();

        const trial = { id: 1, openId: 'correct-open-id', passed: 1 };
        const trialTemplate = { id: 7, isProctoring: true };

        yield trialsFactory.createWithRelations(trial, { trialTemplate });

        nockProctorEdu.protocol({
            openId: trial.openId,
            response: {
                id: 'openId',
                evaluation: 70,
                averages: { b2: 0, c1: 0, c2: 26, c3: 3, c4: 12, c5: 0, m1: 0, m2: 29, n1: 0, s1: 100, s2: 0 }
            }
        });

        yield request
            .post('/v1/admin/proctoring/1/retry')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({
                passed: 1,
                verdict: 'failed'
            })
            .end();

        const actual = yield ProctoringResponses.findAll({
            attributes: ['source', 'verdict', 'evaluation', 'isLast'],
            order: [['time']],
            raw: true
        });

        expect(actual).to.deep.equal([
            {
                source: 'proctoring',
                verdict: 'pending',
                evaluation: 70,
                isLast: false
            },
            {
                source: 'metrics',
                verdict: 'failed',
                evaluation: null,
                isLast: true
            }
        ]);
    });

    it('should create cert if trial is passed and proctoring is correct', function *() {
        yield createAdmin();

        const trial = { id: 1, expired: 0, openId: 'correct-open-id', passed: 1 };
        const trialTemplate = { id: 7, isProctoring: true };

        yield trialsFactory.createWithRelations(trial, { trialTemplate });

        nockProctorEdu.protocol({
            openId: trial.openId,
            response: { id: 'openId', evaluation: 99 }
        });

        yield request
            .post('/v1/admin/proctoring/1/retry')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({
                passed: 1,
                verdict: 'correct'
            })
            .end();

        const cert = yield Certificate.findOne({ where: { trialId: 1 } });

        expect(cert.id).to.exist;
    });

    it('should throw 400 when attempt id is invalid', function *() {
        yield request
            .post(`/v1/admin/proctoring/abc/retry`)
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                message: 'Attempt id is invalid',
                internalCode: '400_AII',
                attemptId: 'abc'
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/admin/proctoring/1/retry')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user not admin', function *() {
        yield request
            .post('/v1/admin/proctoring/1/retry')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User is not admin',
                internalCode: '403_NAD'
            })
            .end();
    });

    it('should throw 404 when attempt not found', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/proctoring/1/retry')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect({
                message: 'Attempt not found',
                internalCode: '404_ATF'
            })
            .end();
    });

    it('should throw 424 if proctorEdu not send `evaluation`', function *() {
        yield createAdmin();

        const trial = { id: 1, expired: 0, openId: 'correct-open-id' };
        const trialTemplate = { id: 7, isProctoring: true };

        yield trialsFactory.createWithRelations(trial, { trialTemplate });

        nockProctorEdu.protocol({
            openId: trial.openId,
            response: { id: 'openId' }
        });

        yield request
            .post('/v1/admin/proctoring/1/retry')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(424)
            .expect({
                message: 'Proctoring not send evaluation',
                internalCode: '424_PNE'
            })
            .end();
    });
});
