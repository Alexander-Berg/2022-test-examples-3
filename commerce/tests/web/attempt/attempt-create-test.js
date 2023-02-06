require('co-mocha');

const trialTemplateToSectionsFactory = require('tests/factory/trialTemplateToSectionsFactory');
const trialTemplateAllowedFailsFactory = require('tests/factory/trialTemplateAllowedFailsFactory');
const questionsFactory = require('tests/factory/questionsFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const usersFactory = require('tests/factory/usersFactory');
const banFactory = require('tests/factory/bansFactory');
const globalUsersFactory = require('tests/factory/globalUsersFactory');

const {
    TrialToQuestion,
    User
} = require('db/postgres');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;
const dbHelper = require('tests/helpers/clear');
const { expect } = require('chai');
const nock = require('nock');
const moment = require('moment');

describe('Attempt create controller', () => {
    let trial = null;
    let trialTemplate = null;
    const authType = { id: 2, code: 'web' };
    const user = { id: 23, uid: 1234567890 };

    beforeEach(function *() {
        yield dbHelper.clear();

        trialTemplate = { id: 2, delays: '', slug: 'testExam' };

        // First sections data
        let section = { id: 3, code: 'first' };

        yield trialTemplateToSectionsFactory.createWithRelations(
            { categoryId: 1, quantity: 1 },
            { trialTemplate, section }
        );
        yield trialTemplateToSectionsFactory.createWithRelations(
            { categoryId: 2, quantity: 1 },
            { trialTemplate, section }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 1 },
            { trialTemplate, section }
        );
        yield questionsFactory.createWithRelations({ id: 5, categoryId: 1 }, { section });
        yield questionsFactory.createWithRelations({ id: 6, categoryId: 2 }, { section });
        yield questionsFactory.createWithRelations({ id: 7, categoryId: 1 }, { section });

        // Second sections data
        section = { id: 4, code: 'second' };
        yield trialTemplateToSectionsFactory.createWithRelations(
            { categoryId: 1, quantity: 1 },
            { trialTemplate, section }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 1 },
            { trialTemplate, section }
        );

        yield questionsFactory.createWithRelations({ id: 8, categoryId: 1 }, { section });
        yield questionsFactory.createWithRelations({ id: 9, categoryId: 1 }, { section });

        const role = { id: 1 };

        yield usersFactory.createWithRelations(user, { role, authType });
    });

    afterEach(nock.cleanAll);

    it('should create attempt by exam id', function *() {
        nockBlackbox({ response: {
            uid: { value: '1234567890' },
            karma: { value: 0 }
        } });

        const res = yield request
            .post('/v1/attempt/2')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const attempt = res.body;

        expect(attempt.userId).to.equal(23);
        expect(attempt.trialTemplateId).to.equal(2);
        expect(attempt.questionCount).to.equal(3);
        expect(attempt.allowedFails).to.equal(2);
        expect(attempt.examSlug).to.equal('testExam');
        expect(attempt.openId).to.be.null;
    });

    it('should create attempt by exam slug', function *() {
        nockBlackbox({ response: {
            uid: { value: '1234567890' },
            karma: { value: 0 }
        } });

        const res = yield request
            .post('/v1/attempt/testExam')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const attempt = res.body;

        expect(attempt.userId).to.equal(23);
        expect(attempt.trialTemplateId).to.equal(2);
        expect(attempt.questionCount).to.equal(3);
        expect(attempt.allowedFails).to.equal(2);
        expect(attempt.examSlug).to.equal('testExam');
        expect(attempt.openId).to.be.null;
    });

    it('should create attempt questions', function *() {
        nockBlackbox({ response: {
            uid: { value: '1234567890' },
            karma: { value: 0 }
        } });

        const res = yield request
            .post('/v1/attempt/2')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const attempt = res.body;

        const actual = yield TrialToQuestion.findAll();

        expect(actual).to.have.length(3);
        actual.map(question => expect(question.trialId).to.equal(attempt.id));
    });

    it('should create new blackbox user', function *() {
        nockBlackbox({
            request: { sessionid: 'session_for_new_user' },
            response: {
                uid: { value: '1111111111' },
                login: 'new-user',
                karma: { value: 0 }
            }
        });

        yield request
            .post('/v1/attempt/2')
            .set('Cookie', ['Session_id=session_for_new_user'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();

        const actual = yield User.findOne({ where: { uid: 1111111111, authTypeId: 2 } });

        expect(actual.get('login')).to.equal('new-user');
    });

    it('should save correct `openId`', function *() {
        nockBlackbox({ response: {
            uid: { value: '1234567890' },
            karma: { value: 0 }
        } });

        const res = yield request
            .post('/v1/attempt/testExam')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ openId: 'f81d4fae-7dec-11d0-a765-00a0c91e6bf6' })
            .expect(200)
            .expect('Content-Type', /json/)
            .end();
        const attempt = res.body;

        expect(attempt.openId).to.equal('f81d4fae-7dec-11d0-a765-00a0c91e6bf6');
    });

    it('should throw 400 when `openId` is invalid', function *() {
        nockBlackbox({ response: {
            uid: { value: '1234567890' },
            karma: { value: 0 }
        } });

        yield request
            .post('/v1/attempt/direct-pro')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ openId: 'inv@lid!' })
            .expect(400)
            .expect('Content-Type', /json/)
            .expect({
                message: 'openId is invalid',
                internalCode: '400_OII',
                openId: 'inv@lid!'
            })
            .end();
    });

    it('should throw 400 when `examIdentity` is invalid', function *() {
        nockBlackbox({ response: {
            uid: { value: '1234567890' },
            karma: { value: 0 }
        } });

        yield request
            .post('/v1/attempt/!^*')
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

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/attempt/2')
            .expect(401)
            .expect('Content-Type', /json/)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when delay has not expired', function *() {
        nockBlackbox({ response: {
            uid: { value: '1234567890' },
            karma: { value: 0 }
        } });

        trial = {
            id: 9,
            userId: 23,
            started: new Date(),
            finished: new Date(),
            expired: 1,
            nullified: 0
        };
        trialTemplate = {
            id: 3,
            delays: '1M, 2M'
        };
        yield trialsFactory.createWithRelations(trial, { trialTemplate });

        yield request
            .post('/v1/attempt/3')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Attempt does not available',
                internalCode: '403_ANA'
            })
            .end();
    });

    it('should throw 403 when attempt already started (EXPERTDEV-46)', function *() {
        nockBlackbox({ response: {
            uid: { value: '1234567890' },
            karma: { value: 0 }
        } });

        trial = { id: 9, userId: 23, finished: null, expired: 0 };
        trialTemplate = { id: 3 };
        yield trialsFactory.createWithRelations(trial, { trialTemplate });

        yield request
            .post('/v1/attempt/3')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Attempt is already started',
                internalCode: '403_AAS',
                attemptId: 9
            })
            .end();
    });

    it('should throw 403 when user is spammer', function *() {
        nockBlackbox({ response: {
            uid: { value: 1234567890 },
            karma: { value: 100 }
        } });

        yield request
            .post('/v1/attempt/3')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect('Content-Type', /json/)
            .expect({
                message: 'User is spammer',
                internalCode: '403_UIS',
                uid: 1234567890
            })
            .end();
    });

    it('should throw 403 when user is banned', function *() {
        const globalUser = { id: 7, isBanned: false };

        yield globalUsersFactory.create(globalUser, { authType, user });
        yield User.update({ globalUserId: 7 }, {
            fields: ['globalUserId'],
            where: { id: user.id }
        });
        yield banFactory.createWithRelations({
            action: 'ban',
            isLast: true,
            expiredDate: moment().add(1, 'year').toDate()
        }, {
            trialTemplate,
            globalUser,
            admin: { id: 45 }
        });

        nockBlackbox({
            response: {
                uid: { value: 1234567890 },
                karma: { value: 0 }
            }
        });

        yield request
            .post('/v1/attempt/2')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect('Content-Type', /json/)
            .expect({
                message: 'User is banned',
                internalCode: '403_UIB',
                uid: 1234567890,
                authTypeCode: 'web'
            })
            .end();
    });

    it('should throw 404 when exam not found', function *() {
        nockBlackbox({ response: {
            uid: { value: '1234567890' },
            karma: { value: 0 }
        } });

        yield request
            .post('/v1/attempt/1234')
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
