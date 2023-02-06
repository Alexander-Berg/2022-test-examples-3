require('co-mocha');

const { expect } = require('chai');
const moment = require('moment');

const AttemptAccessControl = require('accessControl/attempt');
const Attempt = require('models/attempt');

const catchGeneratorError = require('tests/helpers/catchError').generator;
const catchErrorFunc = require('tests/helpers/catchError').func;
const dbHelper = require('tests/helpers/clear');
const trialsFactory = require('tests/factory/trialsFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const usersFactory = require('tests/factory/usersFactory');
const freezingFactory = require('tests/factory/freezingFactory');
const globalUsersFactory = require('tests/factory/globalUsersFactory');

const { User } = require('db/postgres');

describe('AttemptAccessControl', () => {
    beforeEach(dbHelper.clear);

    describe('`hasAccessToCreateAttempt`', () => {
        let trialTemplate;
        const user = { id: 123, uid: 1234567890 };

        beforeEach(function *() {
            trialTemplate = {
                id: 2,
                timeLimit: 90000,
                delays: '',
                periodBeforeCertificateReset: '1m',
                slug: 'testExam'
            };
            yield trialTemplatesFactory.createWithRelations(trialTemplate, {});
            yield usersFactory.createWithRelations(user, { authType: { code: 'web' } });
        });

        it('should throw 401 when user not authorized', function *() {
            const sut = new AttemptAccessControl();
            const error = yield catchGeneratorError(sut.hasAccessToCreateAttempt.bind(sut, 2));

            expect(error.message).to.equal('User not authorized');
            expect(error.statusCode).to.equal(401);
            expect(error.options).to.deep.equal({ internalCode: '401_UNA' });
        });

        it('should throw 400 when exam identity is invalid', function *() {
            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });
            const error = yield catchGeneratorError(sut.hasAccessToCreateAttempt.bind(sut, '!@^'));

            expect(error.message).to.equal('Exam identity is invalid');
            expect(error.statusCode).to.equal(400);
            expect(error.options).to.deep.equal({ internalCode: '400_EII', identity: '!@^' });
        });

        it('should throw 404 when exam not found', function *() {
            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });
            const error = yield catchGeneratorError(sut.hasAccessToCreateAttempt.bind(sut, 100500));

            expect(error.message).to.equal('Exam not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_ENF' });
        });

        it('should throw 403 when delay has not expired', function *() {
            trialTemplate = {
                id: 3,
                timeLimit: 90000,
                delays: '1M'
            };
            const trial = {
                id: 4,
                userId: 123,
                started: new Date(),
                finished: new Date(),
                expired: 1
            };

            yield trialsFactory.createWithRelations(trial, { trialTemplate });

            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });
            const error = yield catchGeneratorError(sut.hasAccessToCreateAttempt.bind(sut, 3));

            expect(error.message).to.equal('Attempt does not available');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_ANA' });
        });

        it('should throw 403 when attempt is already started', function *() {
            const trial = { id: 4, userId: 123, started: new Date() };

            yield trialsFactory.createWithRelations(trial, { trialTemplate });

            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });
            const error = yield catchGeneratorError(sut.hasAccessToCreateAttempt.bind(sut, 2));

            expect(error.message).to.equal('Attempt is already started');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_AAS', attemptId: 4 });
        });

        it('should throw 403 when login is not actual', function *() {
            yield globalUsersFactory.create({ id: 7, isBanned: false, actualLogin: 'User.Login' });
            yield User.update({ globalUserId: 7 }, { where: { id: user.id } });

            const sut = new AttemptAccessControl({
                user: {
                    uid: { value: '1234567890' },
                    login: 'current'
                },
                authType: 'web'
            });
            const error = yield catchGeneratorError(sut.hasAccessToCreateAttempt.bind(sut, 2));

            expect(error.message).to.equal('Attempt does not available');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_ANA' });
        });

        it('should success by id', function *() {
            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });

            yield sut.hasAccessToCreateAttempt(2);
        });

        it('should success by slug', function *() {
            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });

            yield sut.hasAccessToCreateAttempt('testExam');
        });

        it('should success when frozen and login has access', function *() {
            const now = new Date();

            yield freezingFactory.createWithRelations({
                id: 2,
                frozenBy: 1234567890,
                startTime: now,
                finishTime: moment(now).add(2, 'hour').toDate()
            }, { trialTemplate: { id: 2 } });

            const sut = new AttemptAccessControl(
                {
                    user: {
                        uid: { value: '1234567890' },
                        login: 'test1'
                    },
                    authType: 'web'
                }
            );

            yield sut.hasAccessToCreateAttempt(2);
        });
    });

    describe('`hasAccessToQuestion`', () => {
        it('should throw 401 when user not authorized', function *() {
            const sut = new AttemptAccessControl();

            const error = yield catchGeneratorError(sut.hasAccessToQuestion.bind(sut, null));

            expect(error.message).to.equal('User not authorized');
            expect(error.statusCode).to.equal(401);
            expect(error.options).to.deep.equal({ internalCode: '401_UNA' });
        });

        it('should throw 403 when user not author of trial', function *() {
            yield usersFactory.createWithRelations({ id: 23, uid: 22222222 }, { authType: { code: 'web' } });

            const attempt = new Attempt({ userId: 45 });
            const sut = new AttemptAccessControl({ user: { uid: { value: '22222222' } }, authType: 'web' });

            const error = yield catchGeneratorError(sut.hasAccessToQuestion.bind(sut, attempt));

            expect(error.message).to.equal('Illegal user for attempt');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_IUA' });
        });

        it('should throw 403 when test finished', function *() {
            yield usersFactory.createWithRelations({ id: 23, uid: 1234567890 }, { authType: { id: 2, code: 'web' } });

            const attempt = new Attempt({ userId: 23, expired: 1, started: new Date() });
            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });

            const error = yield catchGeneratorError(sut.hasAccessToQuestion.bind(sut, attempt));

            expect(error.message).to.equal('Test finished');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_TFN' });
        });

        it('should throw 404 when user not found', function *() {
            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });
            const attempt = new Attempt({ id: 23 });
            const error = yield catchGeneratorError(sut.hasAccessToQuestion.bind(sut, attempt));

            expect(error.message).to.equal('User not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_UNF', uid: '1234567890', authType: 'web' });
        });

        it('should throw 404 when user has same uid but other authType', function *() {
            yield usersFactory.createWithRelations({ id: 23, uid: 1234567890 }, { authType: { code: 'telegram' } });

            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });
            const attempt = new Attempt({ userId: 23 });
            const error = yield catchGeneratorError(sut.hasAccessToQuestion.bind(sut, attempt));

            expect(error.message).to.equal('User not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_UNF', uid: '1234567890', authType: 'web' });
        });

        it('should success', function *() {
            yield usersFactory.createWithRelations({ id: 23, uid: 1234567890 }, { authType: { code: 'web' } });

            const attempt = new Attempt({
                userId: 23,
                expired: 0,
                started: new Date(),
                timeLimit: 100000
            });
            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });

            yield sut.hasAccessToQuestion(attempt);
        });
    });

    describe('`hasAccessToFinish`', () => {
        it('should throw 401 when user not authorized', function *() {
            const sut = new AttemptAccessControl();

            const error = yield catchGeneratorError(sut.hasAccessToFinish.bind(sut, null));

            expect(error.message).to.equal('User not authorized');
            expect(error.statusCode).to.equal(401);
            expect(error.options).to.deep.equal({ internalCode: '401_UNA' });
        });

        it('should throw 403 when user not author of trial', function *() {
            yield usersFactory.createWithRelations({ id: 23, uid: 22222222 }, { authType: { code: 'web' } });

            const attempt = new Attempt({ userId: 45 });
            const sut = new AttemptAccessControl({ user: { uid: { value: '22222222' } }, authType: 'web' });

            const error = yield catchGeneratorError(sut.hasAccessToFinish.bind(sut, attempt));

            expect(error.message).to.equal('Illegal user for attempt');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_IUA' });
        });

        it('should throw 403 when attempt have already finished', function *() {
            yield usersFactory.createWithRelations({ id: 23, uid: 1111111111 }, { authType: { code: 'web' } });

            const attempt = new Attempt({
                userId: 23,
                expired: 1
            });
            const sut = new AttemptAccessControl({ user: { uid: { value: '1111111111' } }, authType: 'web' });

            const error = yield catchGeneratorError(sut.hasAccessToFinish.bind(sut, attempt));

            expect(error.message).to.equal('Attempt have already finished');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_AAF' });
        });

        it('should throw 404 when user not found', function *() {
            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });
            const attempt = new Attempt({ id: 23 });
            const error = yield catchGeneratorError(sut.hasAccessToFinish.bind(sut, attempt));

            expect(error.message).to.equal('User not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_UNF', uid: '1234567890', authType: 'web' });
        });

        it('should throw 404 when user has same uid but other authType', function *() {
            yield usersFactory.createWithRelations({ id: 23, uid: 1234567890 }, { authType: { code: 'telegram' } });

            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });
            const attempt = new Attempt({ userId: 23 });
            const error = yield catchGeneratorError(sut.hasAccessToQuestion.bind(sut, attempt));

            expect(error.message).to.equal('User not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_UNF', uid: '1234567890', authType: 'web' });
        });

        it('should success', function *() {
            yield usersFactory.createWithRelations({ id: 23, uid: 1234567890 }, { authType: { code: 'web' } });

            const attempt = new Attempt({
                userId: 23,
                expired: 0,
                started: new Date(),
                timeLimit: 100000
            });
            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });

            yield sut.hasAccessToFinish(attempt);
        });
    });

    describe('`hasAccessToAttempt`', () => {
        it('should throw 401 when user not authorized', function *() {
            const sut = new AttemptAccessControl();

            const error = yield catchGeneratorError(sut.hasAccessToAttempt.bind(sut, null));

            expect(error.message).to.equal('User not authorized');
            expect(error.statusCode).to.equal(401);
            expect(error.options).to.deep.equal({ internalCode: '401_UNA' });
        });

        it('should throw 403 when user not author of trial', function *() {
            yield usersFactory.createWithRelations({ id: 23, uid: 22222222 }, { authType: { code: 'web' } });

            const attempt = new Attempt({ userId: 45 });
            const sut = new AttemptAccessControl({ user: { uid: { value: '22222222' } }, authType: 'web' });

            const error = yield catchGeneratorError(sut.hasAccessToAttempt.bind(sut, attempt));

            expect(error.message).to.equal('Illegal user for attempt');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_IUA' });
        });

        it('should throw 404 when user not found', function *() {
            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });
            const attempt = new Attempt({ id: 23 });
            const error = yield catchGeneratorError(sut.hasAccessToAttempt.bind(sut, attempt));

            expect(error.message).to.equal('User not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_UNF', uid: '1234567890', authType: 'web' });
        });

        it('should throw 404 when user has same uid but other authType', function *() {
            yield usersFactory.createWithRelations({ id: 23, uid: 1234567890 }, { authType: { code: 'telegram' } });

            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });
            const attempt = new Attempt({ userId: 23 });
            const error = yield catchGeneratorError(sut.hasAccessToAttempt.bind(sut, attempt));

            expect(error.message).to.equal('User not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_UNF', uid: '1234567890', authType: 'web' });
        });

        it('should success', function *() {
            yield usersFactory.createWithRelations({ id: 23, uid: 1234567890 }, { authType: { code: 'web' } });

            const attempt = new Attempt({
                userId: 23,
                expired: 1,
                started: new Date(),
                finish: new Date(),
                timeLimit: 100000
            });
            const sut = new AttemptAccessControl({ user: { uid: { value: '1234567890' } }, authType: 'web' });

            yield sut.hasAccessToAttempt(attempt);
        });
    });

    describe('`hasAccessForRevision`', () => {
        it('should do nothing when user can request revision', () => {
            AttemptAccessControl.hasAccessForRevision([
                {
                    trialId: 2,
                    source: 'proctoring',
                    verdict: 'failed',
                    isRevisionRequested: false,
                    isLast: true
                }
            ], 2, 1);
        });

        it('should throw 403 when attempt does not have verdicts', () => {
            const error = catchErrorFunc(AttemptAccessControl.hasAccessForRevision.bind(null, [], 2, 1));

            expect(error.message).to.equal('Attempt does not have verdicts');
            expect(error.status).to.equal(403);
            expect(error.options).to.deep.equal({
                attemptId: 2,
                internalCode: '403_AHV'
            });
        });

        it('should throw 403 when revision is already requested', () => {
            const responses = [
                {
                    trialId: 2,
                    source: 'proctoring',
                    verdict: 'pending',
                    isRevisionRequested: false
                },
                {
                    trialId: 2,
                    source: 'toloka',
                    verdict: 'failed',
                    isRevisionRequested: true,
                    isLast: true
                }
            ];

            const error = catchErrorFunc(AttemptAccessControl.hasAccessForRevision.bind(null, responses, 2, 1));

            expect(error.message).to.equal('Revision is already requested');
            expect(error.status).to.equal(403);
            expect(error.options).to.deep.equal({
                attemptId: 2,
                internalCode: '403_RAR'
            });
        });

        it('should throw 403 when last verdict is not failed', () => {
            const responses = [
                {
                    trialId: 2,
                    source: 'proctoring',
                    verdict: 'pending',
                    isRevisionRequested: false,
                    isLast: true
                }
            ];

            const error = catchErrorFunc(AttemptAccessControl.hasAccessForRevision.bind(null, responses, 2, 1));

            expect(error.message).to.equal('Revision is not available');
            expect(error.status).to.equal(403);
            expect(error.options).to.deep.equal({
                attemptId: 2,
                internalCode: '403_RNA'
            });
        });

        it('should throw 403 when source is not `proctoring` or `toloka`', () => {
            const responses = [
                {
                    trialId: 2,
                    source: 'appeal',
                    verdict: 'failed',
                    isRevisionRequested: false,
                    isLast: true
                }
            ];

            const error = catchErrorFunc(AttemptAccessControl.hasAccessForRevision.bind(null, responses, 2, 1));

            expect(error.message).to.equal('Revision is not available');
            expect(error.status).to.equal(403);
            expect(error.options).to.deep.equal({
                attemptId: 2,
                internalCode: '403_RNA'
            });
        });

        it('should throw 403 when attempt are not passed', () => {
            const responses = [
                {
                    trialId: 2,
                    source: 'proctoring',
                    verdict: 'failed',
                    isRevisionRequested: false,
                    isLast: true
                }
            ];

            const error = catchErrorFunc(AttemptAccessControl.hasAccessForRevision.bind(null, responses, 2, 0));

            expect(error.message).to.equal('Revision is not available');
            expect(error.status).to.equal(403);
            expect(error.options).to.deep.equal({
                attemptId: 2,
                internalCode: '403_RNA'
            });
        });
    });
});
