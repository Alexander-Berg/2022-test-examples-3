require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const {
    Trial,
    TrialToQuestion,
    User
} = require('db/postgres');

const dbHelper = require('tests/helpers/clear');
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;
const trialToQuestionsFactory = require('tests/factory/trialToQuestionsFactory');
const answersFactory = require('tests/factory/answersFactory');
const usersFactory = require('tests/factory/usersFactory');

describe('Attempt answer controller', () => {
    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
    });

    after(() => {
        require('nock').cleanAll();
    });

    beforeEach(function *() {
        yield dbHelper.clear();

        const trial = {
            id: 42,
            started: new Date(),
            timeLimit: 90000,
            expired: 0
        };
        const user = { id: 1, uid: 1234567890 };
        const authType = { id: 2, code: 'web' };
        const question = { id: 3 };
        const section = { id: 4 };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 2, answered: 0, correct: 0 },
            { trial, question, section, user, authType }
        );
        yield answersFactory.createWithRelations(
            { id: 5, correct: 1 },
            { question, section }
        );
        yield answersFactory.createWithRelations(
            { id: 6, correct: 0 },
            { question, section }
        );
    });

    it('should return 204 when answer success', function *() {
        yield request
            .post('/v1/attempt/answer')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                attemptId: 42,
                seq: 2,
                answerId: 5
            })
            .expect(204)
            .end();

        const actual = yield TrialToQuestion.findOne({ where: { seq: 2, trialId: 42 } });

        expect(actual.answered).to.equal(1);
        expect(actual.correct).to.equal(1);
    });

    it('should not change `correct` field when answer wrong', function *() {
        yield request
            .post('/v1/attempt/answer')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                attemptId: 42,
                seq: 2,
                answerId: 6
            })
            .expect(204)
            .end();

        const actual = yield TrialToQuestion.findOne({ where: { seq: 2, trialId: 42 } });

        expect(actual.answered).to.equal(1);
        expect(actual.correct).to.equal(0);
    });

    it('should set `skip` flag when answerId is not defined', function *() {
        yield request
            .post('/v1/attempt/answer')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                attemptId: 42,
                seq: 2
            })
            .expect(204)
            .end();

        const actual = yield TrialToQuestion.findOne({ where: { seq: 2, trialId: 42 } });

        expect(actual.answered).to.equal(2);
    });

    it('should return 400 when attempt id is invalid', function *() {
        yield request
            .post('/v1/attempt/answer')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                attemptId: 'abc',
                seq: 2,
                answerId: 5
            })
            .expect(400)
            .expect({ message: 'Attempt id is invalid', internalCode: '400_AII', attemptId: 'abc' })
            .end();
    });

    it('should return 401 when user is not authorized', function *() {
        yield request
            .post('/v1/attempt/answer')
            .send({
                attemptId: 42,
                seq: 2,
                answerId: 5
            })
            .expect(401)
            .expect({ message: 'User not authorized', internalCode: '401_UNA' })
            .end();
    });

    it('should return 404 when attempt is not found', function *() {
        yield request
            .post('/v1/attempt/answer')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                attemptId: 100500,
                seq: 2,
                answerId: 5
            })
            .expect(404)
            .expect({ message: 'Attempt not found', internalCode: '404_ATF' })
            .end();
    });

    it('should return 403 when illegal user for attempt', function *() {
        yield usersFactory.createWithRelations({ id: 23, uid: 987654321 }, { role: { id: 1 } });
        yield Trial.update({ userId: 23 }, { where: { id: 42 } });

        yield request
            .post('/v1/attempt/answer')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                attemptId: 42,
                seq: 2,
                answerId: 5
            })
            .expect(403)
            .expect({ message: 'Illegal user for attempt', internalCode: '403_IUA' })
            .end();
    });

    it('should return 403 when test finished', function *() {
        yield Trial.update({ timeLimit: 1 }, { where: { id: 42 } });

        yield request
            .post('/v1/attempt/answer')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                attemptId: 42,
                seq: 2,
                answerId: 5
            })
            .expect(403)
            .expect({ message: 'Test finished', internalCode: '403_TFN' })
            .end();
    });

    it('should return 400 when question number is invalid', function *() {
        yield request
            .post('/v1/attempt/answer')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                attemptId: 42,
                seq: 'abc',
                answerId: 5
            })
            .expect(400)
            .expect({
                message: 'Question number is invalid',
                internalCode: '400_QNI',
                questionNumber: 'abc'
            })
            .end();
    });

    it('should return 404 when attempt question not found', function *() {
        yield request
            .post('/v1/attempt/answer')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                attemptId: 42,
                seq: 100500,
                answerId: 5
            })
            .expect(404)
            .expect({ message: 'Attempt question not found', internalCode: '404_QNF' })
            .end();
    });

    it('should return 403 when question is already answered', function *() {
        yield TrialToQuestion.update({ answered: 1 }, { where: { seq: 2, trialId: 42 } });

        yield request
            .post('/v1/attempt/answer')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                attemptId: 42,
                seq: 2,
                answerId: 5
            })
            .expect(403)
            .expect({ message: 'Question is already answered', internalCode: '403_QAA' })
            .end();
    });

    it('should return 400 when answer ID is invalid', function *() {
        yield request
            .post('/v1/attempt/answer')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                attemptId: 42,
                seq: 2,
                answerId: 'abc'
            })
            .expect(400)
            .expect({ message: 'Answer ID is invalid', internalCode: '400_ANI' })
            .end();
    });

    it('should throw 404 when user not found', function *() {
        yield User.update({ uid: 12345 }, { where: { id: 1 } });

        yield request
            .post('/v1/attempt/answer')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                attemptId: 42,
                seq: 2,
                answerId: 5
            })
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
