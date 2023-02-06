require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;
const dbHelper = require('tests/helpers/clear');
const { expect } = require('chai');
const nock = require('nock');

const trialToQuestionsFactory = require('tests/factory/trialToQuestionsFactory');
const answersFactory = require('tests/factory/answersFactory');
const usersFactory = require('tests/factory/usersFactory');

const {
    Trial,
    User
} = require('db/postgres');

describe('Attempt question controller', () => {
    const trialTemplate = { id: 2 };
    const trial = {
        id: 4,
        expired: 0,
        timeLimit: 100000
    };
    const user = { id: 1, uid: 1234567890 };
    const authType = { id: 2, code: 'web' };
    const section = { id: 1, code: 'rules', title: 'Price rules' };
    const question = { id: 5, text: 'question text', type: 0 };
    let now = null;

    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
    });

    after(nock.cleanAll);

    beforeEach(function *() {
        yield dbHelper.clear();

        now = new Date();
        trial.started = now;

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 3, answered: 1 },
            { trial, question, trialTemplate, section, user, authType }
        );
        yield answersFactory.createWithRelations(
            { id: 2, text: 'Yep' },
            { question, section }
        );
    });

    it('should return question', function *() {
        const res = yield request
            .get('/v1/attempt/4/3')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const actual = res.body;

        expect(actual.seq).to.equal(3);
        expect(actual.questionId).to.equal(5);
        expect(actual.answered).to.equal('answered');
        expect(actual.section).to.deep.equal({ code: 'rules', title: 'Price rules' });
        expect(actual.text).to.equal('question text');
        expect(actual.type).to.equal('one_answer');
        expect(actual.answers).to.have.length(1);
        expect(actual.answers[0]).to.deep.equal({ id: 2, text: 'Yep' });

        const actualTrial = actual.trial;
        const actualStartedTime = new Date(actualTrial.started).toString();

        expect(actualStartedTime).to.equal(now.toString());
        expect(actualTrial.countAnswered).to.equal(1);
        expect(actualTrial.timeLimit).to.equal(100000);
        expect(actualTrial.questionCount).to.equal(1);
    });

    it('should throw 400 when `attemptId` is invalid', function *() {
        yield request
            .get('/v1/attempt/abc/3')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Attempt id is invalid',
                internalCode: '400_AII',
                attemptId: 'abc'
            })
            .end();
    });

    it('should throw 400 when question number is invalid', function *() {
        yield request
            .get('/v1/attempt/4/abc')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Question number is invalid',
                internalCode: '400_QNI',
                questionNumber: 'abc'
            })
            .end();
    });

    it('should throw 404 when attempt not found', function *() {
        yield request
            .get('/v1/attempt/1234/3')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Attempt not found',
                internalCode: '404_ATF'
            })
            .end();
    });

    it('should throw 404 when question not found', function *() {
        yield request
            .get('/v1/attempt/4/1234')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Attempt question not found',
                internalCode: '404_QNF'
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .get('/v1/attempt/4/3')
            .expect(401)
            .expect('Content-Type', /json/)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user not author of attempt', function *() {
        yield usersFactory.createWithRelations(
            { id: 23, uid: 11111111111 },
            { role: { id: 1 }, authType }
        );
        yield Trial.update({ userId: 23 }, { where: { id: trial.id } });

        yield request
            .get('/v1/attempt/4/3')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Illegal user for attempt',
                internalCode: '403_IUA'
            })
            .end();
    });

    it('should throw 403 when test finished', function *() {
        yield Trial.update({ expired: 1 }, { where: { id: trial.id } });

        yield request
            .get('/v1/attempt/4/3')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Test finished',
                internalCode: '403_TFN'
            })
            .end();
    });

    it('should throw 404 when user not found', function *() {
        yield User.update({ uid: 12345 }, { where: { id: 1 } });

        yield request
            .get('/v1/attempt/4/3')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect('Content-Type', /json/)
            .expect({
                message: 'User not found',
                internalCode: '404_UNF',
                uid: 1234567890,
                authType: 'web'
            })
            .end();
    });
});
