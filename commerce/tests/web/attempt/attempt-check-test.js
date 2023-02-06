require('co-mocha');

const trialTemplateToSectionsFactory = require('tests/factory/trialTemplateToSectionsFactory');
const questionsFactory = require('tests/factory/questionsFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const certificatesFactory = require('tests/factory/certificatesFactory');
const freezingFactory = require('tests/factory/freezingFactory');
const usersFactory = require('tests/factory/usersFactory');

const api = require('api');
const moment = require('moment');
const request = require('co-supertest').agent(api.callback());
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;
const dbHelper = require('tests/helpers/clear');
const { expect } = require('chai');

describe('Attempt check controller', () => {
    const user = { id: 23, uid: 1234567890 };
    const authType = { id: 2, code: 'web' };
    let trial = null;
    let trialTemplate = null;
    let now = null;

    beforeEach(function *() {
        yield dbHelper.clear();

        const section = { id: 3 };

        trialTemplate = { id: 2, delay: '1M, 2M, 3M', slug: 'testExam' };
        now = new Date();
        trial = {
            id: 8,
            started: moment(now).subtract(1, 'hour'),
            finished: now
        };
        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 2 },
            { trialTemplate, section, trial }
        );

        yield questionsFactory.createWithRelations({ id: 5 }, { section });
        yield questionsFactory.createWithRelations({ id: 6 }, { section });
        yield questionsFactory.createWithRelations({ id: 7 }, { section });
    });

    before(() => {
        nockBlackbox({
            response: {
                uid: { value: '1234567890' },
                attributes: {
                    27: 'Vasya',
                    28: 'Pupkin'
                },
                karma: { value: 0 },
                login: 'Current.Login'
            }
        });
    });

    after(() => {
        require('nock').cleanAll();
    });

    it('should return state `in_progress` and `attemptId` when attempt not finished', function *() {
        trial = { id: 9, started: new Date() };
        yield trialsFactory.createWithRelations(trial, { trialTemplate, user, authType });
        const expected = {
            state: 'in_progress',
            attemptId: 9,
            openId: null,
            hasValidCert: false,
            firstname: 'Vasya',
            lastname: 'Pupkin'
        };

        let res = yield request
            .get('/v1/attempt/2/check')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();

        expect(res.body).to.deep.equal(expected);

        res = yield request
            .get('/v1/attempt/testExam/check')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        expect(res.body).to.deep.equal(expected);
    });

    it('should return state `disabled` and `availabilityDate` when certificate is active', function *() {
        trialTemplate = { id: 2, periodBeforeCertificateReset: '1m' };
        const dueDate = moment(now).add(2, 'months');

        yield certificatesFactory.createWithRelations({
            dueDate,
            active: 1
        }, { trial, trialTemplate, user, authType });

        const res = yield request
            .get('/v1/attempt/2/check')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const actual = res.body;

        const availabilityDate = `${moment(dueDate)
            .subtract(1, 'month')
            .startOf('day')
            .utc()
            .format('YYYY-MM-DDTHH:mm:ss.SSS')}Z`;

        expect(actual).to.deep.equal({
            state: 'disabled',
            availabilityDate,
            hasValidCert: true,
            firstname: 'Vasya',
            lastname: 'Pupkin'
        });
    });

    it('should return state `disabled` and `availabilityDate` when attempt delay has not expired', function *() {
        yield trialsFactory.createWithRelations(trial, { trialTemplate, user, authType });

        const res = yield request
            .get('/v1/attempt/2/check')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const actual = res.body;

        const availabilityDate = `${moment(now)
            .add(1, 'month')
            .startOf('day')
            .utc()
            .format('YYYY-MM-DDTHH:mm:ss.SSS')}Z`;

        expect(actual).to.deep.equal({
            state: 'disabled',
            availabilityDate,
            hasValidCert: false,
            firstname: 'Vasya',
            lastname: 'Pupkin'
        });
    });

    it('should return state `enabled` when not attempts', function *() {
        trialTemplate = { id: 3 };
        const section = { id: 3 };

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 2 },
            { trialTemplate, section }
        );

        const res = yield request
            .get('/v1/attempt/3/check')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const actual = res.body;

        expect(actual).to.deep.equal({
            state: 'enabled',
            hasValidCert: false,
            firstname: 'Vasya',
            lastname: 'Pupkin'
        });
    });

    it('should return state `enabled` when attempt delay has expired', function *() {
        trial = {
            id: 9,
            started: moment(now).subtract(1, 'month').subtract(1, 'hour'),
            finished: moment(now).subtract(1, 'month')
        };
        yield trialsFactory.createWithRelations(trial, { trialTemplate, user, authType });

        const res = yield request
            .get('/v1/attempt/2/check')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const actual = res.body;

        expect(actual).to.deep.equal({
            state: 'enabled',
            hasValidCert: false,
            firstname: 'Vasya',
            lastname: 'Pupkin'
        });
    });

    it('should return state `enabled` when certificate expires', function *() {
        trial = {
            id: 10,
            started: moment(now).subtract(2, 'months').subtract(1, 'hour'),
            finished: moment(now).subtract(2, 'months')
        };
        trialTemplate = { id: 3, periodBeforeCertificateReset: '1m', delay: '2M' };
        yield certificatesFactory.createWithRelations({
            dueDate: now,
            active: 1
        }, { trial, trialTemplate, user, authType });

        const res = yield request
            .get('/v1/attempt/3/check')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const actual = res.body;

        expect(actual).to.deep.equal({
            state: 'enabled',
            hasValidCert: false,
            firstname: 'Vasya',
            lastname: 'Pupkin'
        });
    });

    it('should return state `frozen` when attempt is available and service is frozen', function *() {
        const finishTime = moment(now).add(2, 'hour').toDate();

        trialTemplate = { id: 5, delay: '1M, 2M, 3M', slug: 'testExam' };

        yield freezingFactory.createWithRelations({
            id: 2,
            frozenBy: 1234567890,
            startTime: now,
            finishTime
        }, { trialTemplate });

        const res = yield request
            .get('/v1/attempt/5/check')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();

        const actual = res.body;
        const freezeFinishTime = moment(actual.freezeFinishTime).toDate();

        expect(actual.state).to.equal('frozen');
        expect(actual.hasValidCert).to.be.false;
        expect(actual.firstname).to.equal('Vasya');
        expect(actual.lastname).to.equal('Pupkin');
        expect(freezeFinishTime).to.deep.equal(finishTime);
    });

    it('should return `banned` state when global user is super banned', function *() {
        yield usersFactory.createWithRelations(
            user,
            { authType, globalUser: { id: 7, isBanned: true } }
        );

        const expected = {
            state: 'banned',
            hasValidCert: false,
            firstname: 'Vasya',
            lastname: 'Pupkin',
            expiredDate: null
        };

        yield request
            .get('/v1/attempt/2/check')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect(expected)
            .end();
    });

    it('should return `notActualLogin` state when login is not actual', function *() {
        const globalUser = { id: 7, isBanned: false, actualLogin: 'User.Login' };

        yield usersFactory.createWithRelations(
            user,
            { authType, globalUser }
        );

        const expected = {
            state: 'notActualLogin',
            firstname: 'Vasya',
            lastname: 'Pupkin'
        };

        yield request
            .get('/v1/attempt/2/check')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect(expected)
            .end();
    });

    it('should throw 400 when `examIdentity` is invalid', function *() {
        yield request
            .get('/v1/attempt/!^*/check')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Exam identity is invalid',
                internalCode: '400_EII',
                identity: '!^*'
            })
            .end();
    });

    it('should throw 404 when exam not exist', function *() {
        yield request
            .get('/v1/attempt/1234/check')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Exam not found',
                internalCode: '404_ENF'
            })
            .end();
    });
});
