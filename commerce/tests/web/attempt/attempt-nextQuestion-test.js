require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;
const dbHelper = require('tests/helpers/clear');
const { expect } = require('chai');

const trialToQuestionsFactory = require('tests/factory/trialToQuestionsFactory');
const answersFactory = require('tests/factory/answersFactory');
const usersFactory = require('tests/factory/usersFactory');

const {
    Trial,
    User
} = require('db/postgres');

describe('Attempt next question', () => {
    const trial = {
        id: 3,
        expired: 0,
        questionCount: 2,
        timeLimit: 100000
    };
    const section = { id: 5, code: 'rules', title: 'Price rules' };
    const user = { id: 1, uid: 1234567890 };
    const authType = { id: 2, code: 'web' };
    let now = null;

    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
    });

    after(() => {
        require('nock').cleanAll();
    });

    beforeEach(function *() {
        yield dbHelper.clear();

        const trialTemplate = { id: 2, title: 'some exam', isProctoring: false, timeLimit: 456 };

        now = new Date();
        trial.started = now;
        const question = { id: 7, type: 0 };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 3, answered: 1 },
            { trial, trialTemplate, section, question, user, authType }
        );
    });

    it('should return next question', function *() {
        const question = { id: 8, text: 'question text' };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 2, answered: 0 },
            { trial, question, section, user, authType }
        );
        yield answersFactory.createWithRelations(
            { id: 2, text: 'Yep' },
            { question, section }
        );

        const res = yield request
            .get('/v1/attempt/3/nextQuestion')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const actual = res.body;

        expect(actual.seq).to.equal(2);
        expect(actual.questionId).to.equal(8);
        expect(actual.answered).to.equal('not_answered');
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
        expect(actualTrial.questionCount).to.equal(2);

        const actualExam = actual.exam;

        expect(actualExam.title).to.equal('some exam');
        expect(actualExam.isProctoring).to.be.false;
        expect(actualExam.timeLimit).to.equal(456);
    });

    it('should return same questions when try several time', function *() {
        const question = { id: 8, text: 'question text' };

        yield trialToQuestionsFactory.createWithRelations({
            seq: 2,
            answered: 0
        }, { trial, question, section, user, authType });
        yield answersFactory.createWithRelations(
            { id: 2, text: 'Yep' },
            { question, section }
        );

        let res = yield request
            .get('/v1/attempt/3/nextQuestion')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const firstActual = res.body;

        res = yield request
            .get('/v1/attempt/3/nextQuestion')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const secondActual = res.body;

        expect(firstActual).to.deep.equal(secondActual);
    });

    it('should return null when question not found', function *() {
        yield request
            .get('/v1/attempt/3/nextQuestion')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({})
            .end();
    });

    it('should throw 400 when `attemptId` is invalid', function *() {
        yield request
            .get('/v1/attempt/abc/nextQuestion')
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

    it('should throw 404 when attempt not found', function *() {
        yield request
            .get('/v1/attempt/1234/nextQuestion')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Attempt not found',
                internalCode: '404_ATF'
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .get('/v1/attempt/3/nextQuestion')
            .expect(401)
            .expect('Content-Type', /json/)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user not author of attempt', function *() {
        yield usersFactory.createWithRelations({ id: 23, uid: 11111111111 }, { role: { id: 1 } });
        yield Trial.update({ userId: 23 }, { where: { id: trial.id } });

        yield request
            .get('/v1/attempt/3/nextQuestion')
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
            .get('/v1/attempt/3/nextQuestion')
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
            .get('/v1/attempt/3/nextQuestion')
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
