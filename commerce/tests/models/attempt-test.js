require('co-mocha');

let Attempt = require('models/attempt');
const MdsModel = require('models/mds');

const catchError = require('tests/helpers/catchError').generator;
const dbHelper = require('tests/helpers/clear');
const { expect } = require('chai');
const mockery = require('mockery');
const moment = require('moment');
const nockAvatars = require('tests/helpers/mdsServices').avatars;
const nockProctorEdu = require('tests/helpers/proctorEdu');
const mockMailer = require('tests/helpers/mailer');

const _ = require('lodash');
const nock = require('nock');

const trialTemplateToSectionsFactory = require('tests/factory/trialTemplateToSectionsFactory');
const questionsFactory = require('tests/factory/questionsFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const certificatesFactory = require('tests/factory/certificatesFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const trialToQuestionsFactory = require('tests/factory/trialToQuestionsFactory');
const trialTemplateAllowedFailsFactory = require('tests/factory/trialTemplateAllowedFailsFactory');
const rolesFactory = require('tests/factory/rolesFactory');
const freezingFactory = require('tests/factory/freezingFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');
const usersFactory = require('tests/factory/usersFactory');
const banFactory = require('tests/factory/bansFactory');

const {
    Trial,
    TrialToQuestion,
    User,
    Certificate,
    ProctoringResponses
} = require('db/postgres');

describe('Attempt model', () => {
    beforeEach(dbHelper.clear);

    describe('`getInfo`', () => {
        const user = { id: 234, uid: 1234567890 };
        const authType = { id: 2, code: 'web' };
        let trialTemplate;
        let now;

        beforeEach(function *() {
            now = new Date();

            trialTemplate = {
                id: 2,
                delays: '1M, 2M, 3M',
                allowedFails: 1,
                timeLimit: 90000,
                isProctoring: false
            };

            yield trialsFactory.createWithRelations({
                id: 323,
                expired: 1,
                started: moment(now).subtract(2, 'year').subtract(1, 'hour').toDate(),
                finished: moment(now).subtract(2, 'year').toDate()
            }, { trialTemplate, user, authType });
        });

        it('should throw 400 when `examIdentity` is invalid', function *() {
            const error = yield catchError(Attempt.getInfo.bind(Attempt, '!#&', { authTypeCode: 'web' }));

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('Exam identity is invalid');
            expect(error.options).to.deep.equal({ internalCode: '400_EII', identity: '!#&' });
        });

        it('should throw 404 when exam not found', function *() {
            const error = yield catchError(Attempt.getInfo.bind(Attempt, 100500, { authTypeCode: 'web' }));

            expect(error.statusCode).to.equal(404);
            expect(error.message).to.equal('Exam not found');
            expect(error.options).to.deep.equal({ internalCode: '404_ENF' });
        });

        describe('`enabled` state', () => {
            it('when attempts not exists and service not frozen', function *() {
                const otherTrialTemplate = { id: 3, delays: '', isProctoring: false };

                yield trialTemplatesFactory.createWithRelations(otherTrialTemplate);

                const actual = yield Attempt.getInfo(3, {
                    uid: '1234567890',
                    authTypeCode: 'web',
                    login: 'ordinary'
                });

                expect(actual).to.deep.equal({ state: 'enabled', hasValidCert: false });
            });

            it('when delays is empty', function *() {
                const otherTrialTemplate = { id: 3, delays: '', isProctoring: false };

                yield trialsFactory.createWithRelations({
                    id: 9,
                    started: moment(now).subtract(1, 'hour'),
                    finished: moment(now).subtract(1, 'minute')
                }, { trialTemplate: otherTrialTemplate, user, authType });

                const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({ state: 'enabled', hasValidCert: false });
            });

            it('when delay attempts has expired', function *() {
                trialTemplate = { id: 3, delays: '1M, 2M, 3M', isProctoring: false };
                yield trialsFactory.createWithRelations({
                    id: 9,
                    started: moment(now).subtract(1, 'month').subtract(1, 'hour'),
                    finished: moment(now).subtract(1, 'month').subtract(1, 'minute')
                }, { trialTemplate, user, authType });

                const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({ state: 'enabled', hasValidCert: false });
            });

            it('when delay has expired after success attempts', function *() {
                yield trialsFactory.createWithRelations({
                    id: 9,
                    started: moment(now).subtract(1, 'year').subtract(1, 'hour'),
                    finished: moment(now).subtract(1, 'year').subtract(1, 'minute'),
                    passed: 1
                }, { trialTemplate, user, authType });
                yield trialsFactory.createWithRelations({
                    id: 10,
                    started: moment(now).subtract(1, 'month').subtract(1, 'hour'),
                    finished: moment(now).subtract(1, 'month').subtract(1, 'minute')
                }, { trialTemplate, user, authType });

                const actual = yield Attempt.getInfo(2, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({ state: 'enabled', hasValidCert: false });
            });

            it('when certificate will expire after 2 days', function *() {
                trialTemplate = { id: 3, periodBeforeCertificateReset: '3d', isProctoring: false };
                const trial = {
                    id: 9,
                    started: moment(now).subtract(1, 'month').subtract(1, 'hour'),
                    finished: moment(now).subtract(1, 'month').subtract(1, 'minute')
                };

                yield certificatesFactory.createWithRelations({
                    dueDate: moment(now).add(2, 'day'),
                    active: 1
                }, { trial, trialTemplate, user, authType });

                const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({ state: 'enabled', hasValidCert: true });
            });

            it('when nullified certificate has expired', function *() {
                const otherTrialTemplate = {
                    id: 3,
                    periodBeforeCertificateReset: '3d',
                    isProctoring: false
                };
                const trial = {
                    id: 9,
                    started: moment(now).subtract(2, 'month').subtract(1, 'hour'),
                    finished: moment(now).subtract(2, 'month'),
                    passed: 1
                };

                yield certificatesFactory.createWithRelations({
                    dueDate: moment(now).subtract(1, 'month'),
                    active: 0
                }, { trial, trialTemplate: otherTrialTemplate, user, authType });

                const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({ state: 'enabled', hasValidCert: false });
            });

            it('when attempt is nullified', function *() {
                trialTemplate = { id: 3, delays: '1y', isProctoring: false };
                const trial = {
                    id: 10,
                    started: moment(now).subtract(1, 'months').subtract(1, 'hour'),
                    finished: moment(now).subtract(1, 'months'),
                    nullified: 1
                };

                yield trialsFactory.createWithRelations(trial, { trialTemplate, user, authType });

                const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'enabled',
                    hasValidCert: false
                });
            });

            it('when certification was frozen and login has access', function *() {
                yield trialTemplatesFactory.createWithRelations({
                    id: 5,
                    isProctoring: false
                }, {});
                yield freezingFactory.createWithRelations({
                    id: 2,
                    frozenBy: 1234567890,
                    startTime: now,
                    finishTime: moment(now).add(2, 'hour').toDate()
                }, { trialTemplate: { id: 5 } });

                const actual = yield Attempt.getInfo(5, {
                    uid: '9876543210',
                    authTypeCode: 'web',
                    login: 'test1'
                });

                expect(actual).to.deep.equal({
                    state: 'enabled',
                    hasValidCert: false
                });
            });

            it('when user login is actual', function *() {
                const otherUser = { id: 456, uid: 768563683 };
                const globalUser = { id: 10, isBanned: false, actualLogin: 'actual' };

                yield usersFactory.createWithRelations(otherUser, { globalUser, authType });

                const actual = yield yield Attempt.getInfo(2, { uid: 768563683, authTypeCode: 'web', login: 'actual' });

                expect(actual).to.deep.equal({
                    state: 'enabled',
                    hasValidCert: false
                });
            });

            describe('with proctoring', () => {
                it('when delay is expired when proctoring verdict is `failed`', function *() {
                    trialTemplate = { id: 3, delays: '1M, 2M, 3M', isProctoring: true };
                    yield trialsFactory.createWithRelations({
                        id: 9,
                        started: moment(now).subtract(1, 'month').subtract(1, 'week'),
                        finished: moment(now).subtract(1, 'month').subtract(1, 'week'),
                        passed: 1
                    }, { trialTemplate, user, authType });
                    yield proctoringResponsesFactory.createWithRelations({
                        verdict: 'failed',
                        isLast: true,
                        time: moment(now).subtract(1, 'month').subtract(1, 'day')
                    }, { trial: { id: 9 } });

                    const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                    expect(actual).to.deep.equal({ state: 'enabled', hasValidCert: false });
                });

                it('when certificate will expire after 2 days', function *() {
                    trialTemplate = { id: 3, periodBeforeCertificateReset: '3d', isProctoring: true };
                    const trial = {
                        id: 9,
                        started: moment(now).subtract(1, 'month'),
                        finished: moment(now).subtract(1, 'month'),
                        passed: 1
                    };

                    yield certificatesFactory.createWithRelations({
                        dueDate: moment(now).add(2, 'day'),
                        active: 1
                    }, { trial, trialTemplate, user, authType });
                    yield proctoringResponsesFactory.create({
                        trialId: 9,
                        verdict: 'correct',
                        isLast: true,
                        time: moment(now).subtract(1, 'month')
                    });

                    const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                    expect(actual).to.deep.equal({ state: 'enabled', hasValidCert: true });
                });
            });
        });

        describe('`frozen` state', () => {
            it('when attempt is available, service is frozen and login has not access', function *() {
                const otherNow = new Date();
                const finishTime = moment(otherNow).add(2, 'hour').startOf('second').toDate();

                yield freezingFactory.createWithRelations({
                    id: 2,
                    frozenBy: 1234567890,
                    startTime: otherNow,
                    finishTime
                }, { trialTemplate: { id: 2 } });

                const actual = yield Attempt.getInfo(2, {
                    uid: '987654321',
                    authTypeCode: 'web',
                    login: 'ordinary'
                });
                const freezeFinishTime = moment(actual.freezeFinishTime).toDate();

                expect(actual.state).to.equal('frozen');
                expect(actual.hasValidCert).to.be.false;
                expect(freezeFinishTime).to.deep.equal(finishTime);
            });

            it('when attempt is available, service is frozen and login is not actual', function *() {
                const otherNow = new Date();
                const finishTime = moment(otherNow).add(2, 'hour').startOf('second').toDate();

                yield freezingFactory.createWithRelations({
                    id: 2,
                    frozenBy: 1234567890,
                    startTime: otherNow,
                    finishTime
                }, { trialTemplate: { id: 2 } });

                const otherUser = { id: 456, uid: 768563683 };
                const globalUser = { id: 10, isBanned: false, actualLogin: 'actual' };

                yield usersFactory.createWithRelations(otherUser, { globalUser, authType });

                const actual = yield Attempt.getInfo(2, {
                    uid: 768563683,
                    authTypeCode: 'web',
                    login: 'other'
                });

                expect(actual).to.deep.equal({
                    state: 'frozen',
                    hasValidCert: false,
                    freezeFinishTime: finishTime.toISOString()
                });
            });
        });

        describe('`in_progress` state', () => {
            it('when attempt was not finished for simple exam', function *() {
                yield trialsFactory.createWithRelations({
                    id: 324,
                    expired: 0,
                    started: moment(now).subtract(1, 'minute').toDate()
                }, { trialTemplate, user, authType });

                const actual = yield Attempt.getInfo(2, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'in_progress',
                    attemptId: 324,
                    openId: null,
                    hasValidCert: false
                });
            });

            it('when attempt was not finished for proctoring exam', function *() {
                trialTemplate = {
                    id: 2,
                    delays: '1M, 2M, 3M',
                    allowedFails: 1,
                    timeLimit: 90000,
                    isProctoring: true
                };

                yield trialsFactory.createWithRelations({
                    id: 324,
                    expired: 0,
                    started: moment(now).subtract(1, 'minute').toDate(),
                    openId: 'f81d4fae-7dec-11d0-a765-00a0c91e6bf6'
                }, { trialTemplate, user, authType });

                const actual = yield Attempt.getInfo(2, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'in_progress',
                    attemptId: 324,
                    openId: 'f81d4fae-7dec-11d0-a765-00a0c91e6bf6',
                    hasValidCert: false
                });
            });
        });

        describe('`pending` state', () => {
            it('when attempt with proctoring and proctoring is pending', function *() {
                trialTemplate = { id: 3, delays: '', isProctoring: true };
                yield trialsFactory.createWithRelations({
                    id: 325,
                    expired: 1,
                    passed: 1
                }, { trialTemplate, user, authType });
                yield proctoringResponsesFactory.create({
                    trialId: 325,
                    verdict: 'pending',
                    isLast: true
                });

                const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({ state: 'pending', hasValidCert: false });
            });
        });

        describe('`disabled` state', () => {
            it('should return `availabilityDate` after 1 month when cert expire after 2 month', function *() {
                trialTemplate = {
                    id: 3,
                    periodBeforeCertificateReset: '1m',
                    delays: '5m, 6m, 7m',
                    isProctoring: false
                };
                const trial = {
                    id: 10,
                    started: moment(now).subtract(1, 'months').subtract(1, 'hour'),
                    finished: moment(now).subtract(1, 'months'),
                    passed: 1
                };
                const dueDate = moment(now).add(2, 'month');

                yield certificatesFactory.createWithRelations({
                    dueDate,
                    active: 1
                }, { trial, trialTemplate, user, authType });

                const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'disabled',
                    availabilityDate: moment(dueDate).subtract(1, 'month').startOf('day').toDate(),
                    hasValidCert: true
                });
            });

            it('should return `availabilityDate` after 1 month after 1 attempt', function *() {
                trialTemplate = { id: 3, delays: '1M, 2M, 3M', isProctoring: false };
                yield trialsFactory.createWithRelations({
                    id: 9,
                    started: moment(now).subtract(1, 'hour'),
                    finished: moment(now)
                }, { trialTemplate, user, authType });

                const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'disabled',
                    availabilityDate: moment(now).add(1, 'month').startOf('day').toDate(),
                    hasValidCert: false
                });
            });

            it('should return `availabilityDate` after 3 months when sequenceNumber bigger `delay`', function *() {
                const trialBase = {
                    started: moment(now).subtract(1, 'hour'),
                    finished: moment(now)
                };

                yield trialsFactory.createWithRelations(
                    _.assign({ id: 9 }, trialBase),
                    { trialTemplate, user, authType }
                );
                yield trialsFactory.createWithRelations(
                    _.assign({ id: 10 }, trialBase),
                    { trialTemplate, user, authType }
                );
                yield trialsFactory.createWithRelations(
                    _.assign({ id: 11 }, trialBase),
                    { trialTemplate, user, authType }
                );

                const actual = yield Attempt.getInfo(2, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'disabled',
                    availabilityDate: moment(now).add(3, 'month').startOf('day').toDate(),
                    hasValidCert: false
                });
            });

            it('should return `availabilityDate` after 2 month when sequenceNumber equal 2', function *() {
                yield trialsFactory.createWithRelations({
                    id: 9,
                    started: moment(now).subtract(1, 'hour'),
                    finished: moment(now)
                }, { trialTemplate, user, authType });

                const actual = yield Attempt.getInfo(2, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'disabled',
                    availabilityDate: moment(now).add(2, 'month').startOf('day').toDate(),
                    hasValidCert: false
                });
            });

            it('should return `disable` without availabilityDate when allowedTriesCount limited', function *() {
                trialTemplate = { id: 3, allowedTriesCount: 1, isProctoring: false };
                yield trialsFactory.createWithRelations({
                    id: 9,
                    started: moment(now).subtract(1, 'hour'),
                    finished: moment(now)
                }, { trialTemplate, user, authType });

                const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'disabled',
                    hasValidCert: false
                });
            });

            it('should return `disabled` when last attempt not finished and exam delay not over', function *() {
                trialTemplate = { id: 234, delays: '1M, 1M, 1M', timeLimit: 900000, isProctoring: false };
                const otherNow = new Date();
                const otherUser = { uid: 9876543210 };
                const started = moment(otherNow).subtract(1, 'day').toDate();
                const notFinishedTrial = {
                    id: 23,
                    started,
                    expired: 0,
                    finished: null,
                    passed: 0
                };

                yield trialsFactory.createWithRelations(
                    notFinishedTrial,
                    { trialTemplate, user: otherUser, authType }
                );

                const actual = yield Attempt.getInfo(234, { uid: otherUser.uid, authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'disabled',
                    hasValidCert: false,
                    availabilityDate: moment(started).add(1, 'month').startOf('day').toDate()
                });
            });

            it('should return `disabled` when last trial with smaller sequenceNumber finished now', function *() {
                trialTemplate = {
                    id: 3,
                    delays: '1M',
                    validityPeriod: '1y',
                    periodBeforeCertificateReset: '1m',
                    isProctoring: false
                };

                const otherNow = new Date();
                const otherUser = { uid: 9876543210 };

                let trial = {
                    id: 23,
                    started: moment(otherNow).subtract(1, 'year').subtract(1, 'hour').toDate(),
                    finished: moment(otherNow).subtract(1, 'year').toDate(),
                    expired: 1,
                    passed: 1,
                    sequenceNumber: 2
                };

                yield certificatesFactory.createWithRelations({
                    dueDate: moment(otherNow).subtract(1, 'hour').toDate(),
                    active: 1
                }, { trial, trialTemplate, user: otherUser, authType });

                trial = {
                    id: 24,
                    started: moment(otherNow).subtract(1, 'month').toDate(),
                    finished: moment(otherNow).subtract(1, 'month').add(1, 'day').toDate(),
                    expired: 1,
                    passed: 1,
                    // EXPERTDEV-234: Два активных сертификата с разницей больше месяца
                    //                Номер попытки меньше, чем у предыдущей
                    sequenceNumber: 1
                };
                const dueDate = moment(otherNow).add(1, 'year').toDate();

                yield certificatesFactory.createWithRelations({
                    dueDate,
                    active: 1
                }, { trial, trialTemplate, user: otherUser, authType });

                const actual = yield Attempt.getInfo(3, { uid: otherUser.uid, authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'disabled',
                    hasValidCert: true,
                    availabilityDate: moment(dueDate).subtract(1, 'month').startOf('day').toDate()
                });
            });

            it('should return `true` in `hasValidCert` when user failed attempt with actual cert', function *() {
                trialTemplate = {
                    id: 5,
                    periodBeforeCertificateReset: '1M',
                    delays: '2M, 2M, 2M',
                    isProctoring: false
                };
                const successTrial = {
                    id: 8,
                    expired: 1,
                    nullified: 0,
                    passed: 1,
                    started: moment(now).subtract(5, 'month').subtract(1, 'hour'),
                    finished: moment(now).subtract(5, 'month')
                };

                yield certificatesFactory.createWithRelations({
                    dueDate: moment(now).add(1, 'month'),
                    active: 1
                }, { trial: successTrial, trialTemplate, user, authType });

                const failedTrial = {
                    id: 9,
                    expired: 1,
                    nullified: 0,
                    passed: 0,
                    started: moment(now).subtract(1, 'hour'),
                    finished: moment(now)
                };

                yield trialsFactory.createWithRelations(failedTrial, { trialTemplate, user, authType });

                const actual = yield Attempt.getInfo(5, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'disabled',
                    availabilityDate: moment(now).add(2, 'month').startOf('day').toDate(),
                    hasValidCert: true
                });
            });

            // EXPERTDEV-456: Исправить подсчет даты следующей попытки
            it('should return correct `availabilityDate` when `finished` much more than `started`', function *() {
                const otherTrialTemplate = { id: 666, delays: '1M, 2M, 3M', isProctoring: false };
                const started = moment(now).subtract(1, 'hour').toDate();

                yield trialsFactory.createWithRelations({
                    id: 9,
                    started,
                    finished: moment(now).add(3, 'day').toDate(),
                    nullified: 0,
                    passed: 0,
                    expired: 1
                }, { trialTemplate: otherTrialTemplate, user, authType });

                const actual = yield Attempt.getInfo(666, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'disabled',
                    availabilityDate: moment(started).add(1, 'month').startOf('day').toDate(),
                    hasValidCert: false
                });
            });

            it('should return correct `availabilityDate` when certificate has been deactivated', function *() {
                const otherTrialTemplate = {
                    id: 123,
                    periodBeforeCertificateReset: '1m',
                    validityPeriod: '1y',
                    delays: '1M, 2M, 3M',
                    isProctoring: false
                };
                const trial = {
                    id: 321,
                    started: moment(now).subtract(1, 'hour'),
                    finished: now,
                    nullified: 0,
                    passed: 1,
                    expired: 1
                };

                const dueDate = moment(now).add(1, 'year');

                yield certificatesFactory.createWithRelations({
                    dueDate,
                    active: 0
                }, { trial, trialTemplate: otherTrialTemplate, user, authType });

                const actual = yield Attempt.getInfo(123, { uid: '1234567890', authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'disabled',
                    availabilityDate: moment(dueDate).startOf('day').toDate(),
                    hasValidCert: false
                });
            });

            describe('with proctoring', () => {
                it('should return `availabilityDate` from trial started date when trial not passed', function *() {
                    trialTemplate = { id: 3, delays: '1M, 2M, 3M', isProctoring: true };
                    yield trialsFactory.createWithRelations({
                        id: 9,
                        started: moment(now).subtract(1, 'hour'),
                        finished: moment(now).subtract(1, 'hour'),
                        passed: 0
                    }, { trialTemplate, user, authType });

                    const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                    expect(actual).to.deep.equal({
                        state: 'disabled',
                        availabilityDate: moment(now).add(1, 'month').startOf('day').toDate(),
                        hasValidCert: false
                    });
                });

                it('should return `availabilityDate` from proctoring time' +
                    ' if proctoring verdict is `failed`', function *() {
                    trialTemplate = { id: 3, delays: '1M, 2M, 3M', isProctoring: true };
                    const verdictTime = moment(now).add(1, 'week');

                    yield trialsFactory.createWithRelations({
                        id: 9,
                        started: moment(now).subtract(1, 'hour'),
                        finished: moment(now),
                        passed: 1
                    }, { trialTemplate, user, authType });

                    yield proctoringResponsesFactory.create({
                        trialId: 9,
                        verdict: 'failed',
                        isLast: true,
                        time: verdictTime
                    });

                    const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                    expect(actual).to.deep.equal({
                        state: 'disabled',
                        availabilityDate: verdictTime.add(1, 'month').startOf('day').toDate(),
                        hasValidCert: false
                    });
                }
                );

                it('should return `availabilityDate` from proctoring time' +
                    ' if proctoring verdict is `pending` and trial not passed',
                function *() {
                    trialTemplate = { id: 3, delays: '1M, 2M, 3M', isProctoring: true };
                    const verdictTime = moment(now).add(1, 'week');

                    yield trialsFactory.createWithRelations({
                        id: 9,
                        started: moment(now).subtract(1, 'hour'),
                        finished: moment(now),
                        passed: 0
                    }, { trialTemplate, user, authType });

                    yield proctoringResponsesFactory.create({
                        trialId: 9,
                        verdict: 'pending',
                        isLast: true,
                        time: verdictTime
                    });

                    const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                    expect(actual).to.deep.equal({
                        state: 'disabled',
                        availabilityDate: verdictTime.add(1, 'month').startOf('day').toDate(),
                        hasValidCert: false
                    });
                }
                );

                it('should return `availabilityDate` from cert if proctoring verdict is `correct`', function *() {
                    trialTemplate = {
                        id: 3,
                        isProctoring: true,
                        periodBeforeCertificateReset: '1M'
                    };

                    const dueDate = moment(now).add(2, 'month');

                    yield trialsFactory.createWithRelations({
                        id: 9,
                        started: moment(now).subtract(5, 'month'),
                        finished: moment(now).subtract(5, 'month'),
                        expired: 1,
                        nullified: 0,
                        passed: 1
                    }, { trialTemplate, user, authType });

                    yield proctoringResponsesFactory.create({
                        trialId: 9,
                        verdict: 'correct',
                        isLast: true,
                        time: moment(now).add(1, 'week')
                    });

                    yield certificatesFactory.createWithRelations({
                        dueDate,
                        active: 1
                    }, { trial: { id: 9 }, trialTemplate, user, authType });

                    const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                    expect(actual).to.deep.equal({
                        state: 'disabled',
                        availabilityDate: moment(dueDate).subtract(1, 'month').startOf('day').toDate(),
                        hasValidCert: true
                    });
                });

                // EXPERTDEV-962: [API] Доработать получение availabilityDate
                it('should return `availabilityDate` from first `failed` verdict' +
                    'when revision returns `failed`', function *() {
                    trialTemplate = {
                        id: 3,
                        isProctoring: true,
                        delays: '3M'
                    };
                    const finished = moment(now).subtract(2, 'month');
                    const trial = {
                        id: 13,
                        passed: 1,
                        nullified: 0,
                        started: moment(finished).subtract(1, 'hour'),
                        finished,
                        expired: 1
                    };

                    yield proctoringResponsesFactory.createWithRelations(
                        {
                            verdict: 'failed',
                            source: 'proctoring',
                            isLast: false,
                            time: finished
                        },
                        { trial, trialTemplate, user, authType }
                    );
                    yield proctoringResponsesFactory.createWithRelations(
                        {
                            verdict: 'failed',
                            source: 'toloka-revision',
                            isLast: true,
                            time: moment(finished).add(1, 'month')
                        },
                        { trial, trialTemplate, user, authType }
                    );

                    const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });

                    expect(actual).to.deep.equal({
                        state: 'disabled',
                        availabilityDate: moment(finished).add(3, 'month').startOf('day').toDate(),
                        hasValidCert: false
                    });
                });

                // EXPERTDEV-962: [API] Доработать получение availabilityDate
                it('should return `availabilityDate` from `dueDate`' +
                    'when revision returns `correct`', function *() {
                    trialTemplate = {
                        id: 3,
                        isProctoring: true,
                        delays: '3m',
                        periodBeforeCertificateReset: '2m',
                        validityPeriod: '1y'
                    };
                    const finished = moment(now).subtract(2, 'month');
                    const trial = {
                        id: 13,
                        passed: 1,
                        nullified: 0,
                        started: moment(finished).subtract(1, 'hour'),
                        finished,
                        expired: 1
                    };

                    yield proctoringResponsesFactory.createWithRelations(
                        {
                            verdict: 'failed',
                            source: 'proctoring',
                            isLast: false,
                            time: finished
                        },
                        { trial, trialTemplate, user, authType }
                    );

                    const correctTime = moment(finished).add(1, 'month');
                    const dueDate = moment(correctTime).add(1, 'year');

                    yield proctoringResponsesFactory.createWithRelations(
                        {
                            verdict: 'correct',
                            source: 'toloka-revision',
                            isLast: true,
                            time: correctTime
                        },
                        { trial, trialTemplate, user, authType }
                    );
                    yield certificatesFactory.createWithRelations(
                        {
                            id: 19,
                            dueDate,
                            confirmedDate: correctTime,
                            active: 1
                        },
                        { trial, trialTemplate, user, authType }
                    );

                    const actual = yield Attempt.getInfo(3, { uid: '1234567890', authTypeCode: 'web' });
                    const availabilityDate = moment(dueDate)
                        .subtract(2, 'month')
                        .startOf('day')
                        .toDate();

                    expect(actual).to.deep.equal({
                        state: 'disabled',
                        availabilityDate,
                        hasValidCert: true
                    });
                });
            });
        });

        describe('`banned` state', () => {
            it('when global user is super banned', function *() {
                const otherUser = { id: 456, uid: 768563683 };
                const globalUser = { id: 10, isBanned: true };

                yield usersFactory.createWithRelations(otherUser, { globalUser, authType });

                yield banFactory.createWithRelations({
                    id: 2,
                    action: 'ban',
                    isLast: true,
                    expiredDate: null
                }, {
                    trialTemplate: { id: 2 },
                    globalUser,
                    admin: { id: 1234 }
                });

                const actual = yield yield Attempt.getInfo(2, { uid: 768563683, authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'banned',
                    hasValidCert: false,
                    expiredDate: null
                });
            });

            it('when global user is banned on any test', function *() {
                const otherUser = { id: 456, uid: 768563683 };
                const globalUser = { id: 10, isBanned: false };
                const expiredDate = moment().add(1, 'year').startOf('day').toDate();

                yield usersFactory.createWithRelations(otherUser, { globalUser, authType });

                yield banFactory.createWithRelations({
                    id: 2,
                    action: 'ban',
                    isLast: true,
                    expiredDate
                }, {
                    trialTemplate: { id: 2 },
                    globalUser: { id: 10 },
                    admin: { id: 1234 }
                });

                const actual = yield yield Attempt.getInfo(2, { uid: 768563683, authTypeCode: 'web' });

                expect(actual).to.deep.equal({
                    state: 'banned',
                    hasValidCert: false,
                    expiredDate
                });
            });
        });

        describe('`notActualLogin` state', () => {
            it('when user login is not actual', function *() {
                const otherUser = { id: 456, uid: 768563683 };
                const globalUser = { id: 10, isBanned: false, actualLogin: 'actual' };

                yield usersFactory.createWithRelations(otherUser, { globalUser, authType });

                const actual = yield yield Attempt.getInfo(2, {
                    uid: 768563683,
                    authTypeCode: 'web',
                    login: 'notActual'
                });

                expect(actual).to.deep.equal({ state: 'notActualLogin' });
            });
        });
    });

    describe('`create`', () => {
        let section;

        beforeEach(function *() {
            const trialTemplate = {
                id: 2,
                allowedFails: 0,
                timeLimit: 90000,
                slug: 'testExam',
                isProctoring: false
            };
            const user = { id: 23, uid: 1234567890 };
            const role = { id: 5, code: 'user-role' };
            const authType = { id: 1, code: 'web' };

            yield rolesFactory.create({ id: 1 });

            // First sections data
            section = { id: 3, code: 'first' };
            yield trialTemplateToSectionsFactory.createWithRelations(
                { categoryId: 1, quantity: 1 },
                { trialTemplate, section }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { categoryId: 2, quantity: 2 },
                { trialTemplate, section }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                { trialTemplate, section }
            );

            // Second sections data
            section = { id: 4, code: 'second' };
            yield trialTemplateToSectionsFactory.createWithRelations(
                { categoryId: 1, quantity: 3 },
                { trialTemplate, section }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 2 },
                { trialTemplate, section }
            );

            yield questionsFactory.createWithRelations({ id: 5, active: 1 }, { section });
            yield questionsFactory.createWithRelations({ id: 6, active: 0 }, { section });
            yield questionsFactory.createWithRelations({ id: 7, active: 1 }, { section });
            yield trialsFactory.createWithRelations(
                { sequenceNumber: 5 },
                { trialTemplate, user, role, authType }
            );
        });

        it('should throw 400 when `examIdentity` is invalid', function *() {
            const error = yield catchError(Attempt.create.bind(Attempt, { examIdentity: '!^*' }));

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('Exam identity is invalid');
            expect(error.options).to.deep.equal({ internalCode: '400_EII', identity: '!^*' });
        });

        it('should throw 404 when exam not found', function *() {
            const error = yield catchError(Attempt.create.bind(Attempt, { examIdentity: 100500 }));

            expect(error.statusCode).to.equal(404);
            expect(error.message).to.equal('Exam not found');
            expect(error.options).to.deep.equal({ internalCode: '404_ENF' });
        });

        it('should create trial by id', function *() {
            const userState = { uid: { value: '1234567890' } };
            const trial = yield Attempt.create({ examIdentity: 2 }, { user: userState }, 'web');

            const actual = yield Trial.findById(trial.id);
            const { examSlug } = trial.toJSON();

            expect(actual).to.exist;
            expect(actual.userId).to.equal(23);
            expect(actual.trialTemplateId).to.equal(2);
            expect(actual.allowedFails).to.equal(3);
            expect(actual.questionCount).to.equal(6);
            expect(actual.timeLimit).to.equal(90000);
            expect(actual.sequenceNumber).to.equal(6);
            expect(actual.started).to.be.a('date');
            expect(actual.openId).to.be.null;
            expect(examSlug).to.equal('testExam');
        });

        it('should create trial by slug', function *() {
            const userState = { uid: { value: '1234567890' } };
            const trial = yield Attempt.create({ examIdentity: 'testExam' }, { user: userState }, 'web');
            const actual = yield Trial.findById(trial.id);
            const { examSlug } = trial.toJSON();

            expect(actual).to.exist;
            expect(actual.userId).to.equal(23);
            expect(actual.trialTemplateId).to.equal(2);
            expect(actual.allowedFails).to.equal(3);
            expect(actual.questionCount).to.equal(6);
            expect(actual.timeLimit).to.equal(90000);
            expect(actual.sequenceNumber).to.equal(6);
            expect(actual.started).to.be.a('date');
            expect(actual.openId).to.be.null;
            expect(examSlug).to.equal('testExam');
        });

        it('should create trial questions', function *() {
            const userState = { uid: { value: '1234567890' } };
            const trial = yield Attempt.create({ examIdentity: 2 }, { user: userState }, 'web');
            const actual = yield TrialToQuestion.findAll({ where: { trialId: trial.id }, order: 'seq' });

            expect(actual).to.have.length(2);
            actual.map((trialQuestion, i) => {
                expect(trialQuestion.trialId).to.equal(trial.id);
                expect(trialQuestion.questionId).to.be.oneOf([5, 7]);
                expect(trialQuestion.seq).to.equal(i + 1);
            });
        });

        it('should create new user', function *() {
            const blackboxUser = {
                uid: { value: '1111111111' },
                login: 'new-user'
            };

            yield Attempt.create({ examIdentity: 2 }, { user: blackboxUser }, 'web');
            const actual = yield User.findOne({ where: { uid: 1111111111, authTypeId: 1 } });

            expect(actual.get('login')).to.equal('new-user');
        });

        it('should create first attempt', function *() {
            yield trialTemplatesFactory.createWithRelations(
                { id: 3 },
                section
            );

            const userState = { uid: { value: '1234567890' } };
            const trial = yield Attempt.create({ examIdentity: 3 }, { user: userState }, 'web');

            expect(trial.get('sequenceNumber')).to.equal(1);
        });

        it('should increment `sequenceNumber`', function *() {
            yield trialTemplatesFactory.createWithRelations(
                { id: 4, delays: '' },
                section
            );

            const dataToCreate = { examIdentity: 4 };
            const userState = { uid: { value: '1234567890' } };

            yield Attempt.create(dataToCreate, { user: userState }, 'web');
            const trial = yield Attempt.create(dataToCreate, { user: userState }, 'web');

            expect(trial.get('sequenceNumber')).to.equal(2);
        });

        it('should not update `roleId` field', function *() {
            const userState = { uid: { value: '1234567890' } };

            yield Attempt.create({ examIdentity: 2 }, { user: userState }, 'web');

            const userData = yield User.findOne({ where: { uid: 1234567890, authTypeId: 1 } });

            expect(userData.get('roleId')).to.equal(5);
        });

        describe('with proctoring', () => {
            const proTrialTemplate = { id: 5, slug: 'direct-pro', isProctoring: true };

            it('should save `openId` when it was transferred', function *() {
                yield trialTemplatesFactory.createWithRelations(proTrialTemplate);

                const userState = { uid: { value: '1234567890' } };
                const dataToCreate = { examIdentity: 5, openId: 'open-id-2018' };
                const trial = yield Attempt.create(dataToCreate, { user: userState }, 'web');

                const actual = yield Trial.findById(trial.id);

                expect(actual.openId).to.equal(dataToCreate.openId);
            });

            it('should throw 400 when exam with proctoring and openId is absent', function *() {
                yield trialTemplatesFactory.createWithRelations(proTrialTemplate);

                const userState = { uid: { value: '1234567890' } };
                const error = yield catchError(Attempt.create.bind(Attempt,
                    { examIdentity: 5 }, { user: userState }, 'web'));

                expect(error.statusCode).to.equal(400);
                expect(error.message).to.equal('Open id is absent');
                expect(error.options).to.deep.equal({ internalCode: '400_OIA' });
            });

            it('should throw 400 with unique constraint error when `openId` already exist', function *() {
                yield trialsFactory.createWithRelations(
                    { openId: 'open-id' },
                    { trialTemplate: proTrialTemplate }
                );

                const userState = { uid: { value: '1234567890' } };
                const error = yield catchError(Attempt.create.bind(Attempt,
                    { examIdentity: 5, openId: 'open-id' }, { user: userState }, 'web'));

                const errorDetails = {
                    message: 'open_id must be unique',
                    type: 'unique violation',
                    path: 'open_id',
                    value: 'open-id'
                };

                expect(error.statusCode).to.equal(400);
                expect(error.message).to.equal('Unique constraint error');
                expect(error.options).to.deep.equal({
                    details: errorDetails,
                    internalCode: '400_UCE'
                });
            });
        });
    });

    describe('`findById`', () => {
        it('should throw 404 when attempt not exists', function *() {
            const error = yield catchError(Attempt.findById.bind(Attempt, '1234'));

            expect(error.message).to.equal('Attempt not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_ATF' });
        });

        it('should return attempt', function *() {
            yield trialsFactory.createWithRelations({ id: 3 });

            const actual = yield Attempt.findById('3');

            expect(actual.get('id')).to.equal(3);
        });
    });

    describe('`isFinished`', () => {
        it('should return true when `expired`', () => {
            const attempt = new Attempt({
                expired: 1,
                started: new Date(),
                timeLimit: 1
            });
            const actual = attempt.isFinished();

            expect(actual).to.be.true;
        });

        it('should return true started long time ago', () => {
            const attempt = new Attempt({
                expired: 0,
                started: new Date(2010, 1, 1),
                timeLimit: 1
            });
            const actual = attempt.isFinished();

            expect(actual).to.be.true;
        });

        it('should return false for new attempt', () => {
            const attempt = new Attempt({
                expired: 0,
                started: new Date(),
                timeLimit: 100000
            });
            const actual = attempt.isFinished();

            expect(actual).to.be.false;
        });
    });

    describe('`finish`', () => {
        const section = { id: 1 };
        const trial = { id: 3, expired: 0, passed: 0 };
        const trialTemplate = { id: 2, isProctoring: false };
        let question;
        const user = {
            id: 14,
            firstname: 'Petr',
            lastname: 'Ivanov'
        };

        afterEach(nock.cleanAll);

        beforeEach(function *() {
            nockAvatars.success();

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 3 },
                { trialTemplate, section }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                { trialTemplate, section }
            );
            question = { id: 3 };
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 1, answered: 1, correct: 1 },
                { trial, question, section, trialTemplate, user }
            );
        });

        it('should set `expired` field to `1`', function *() {
            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            yield attempt.finish();

            const actual = yield Trial.findById(3);

            expect(actual.get('expired')).to.equal(1);
        });

        it('should set `finished` field', function *() {
            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            yield attempt.finish();

            const actual = yield Trial.findById(3);

            expect(actual.get('finished').getTime()).to.be.at.most(Date.now());
        });

        it('should set `passed` field to `1` when all answers in section are correct', function *() {
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 1 },
                { trial, question: { id: 4 }, section, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 3, answered: 1, correct: 1 },
                { trial, question: { id: 5 }, section, trialTemplate }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            yield attempt.finish();

            const actual = yield Trial.findById(3);

            expect(actual.get('passed')).to.equal(1);
        });

        it('should set `passed` field to `1` when incorrect answers in section less or equal then 1', function *() {
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 1 },
                { trial, question: { id: 4 }, section, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 3, answered: 1, correct: 0 },
                { trial, question: { id: 5 }, section, trialTemplate }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            yield attempt.finish();

            const actual = yield Trial.findById(3);

            expect(actual.get('passed')).to.equal(1);
        });

        it('should set `passed` field to `0` when incorrect answers in section more 1', function *() {
            const question1 = { id: 4 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 0 },
                { trial, question: question1, section, trialTemplate }
            );
            const question2 = { id: 5 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 3, answered: 1, correct: 0 },
                { trial, question: question2, section, trialTemplate }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            yield attempt.finish();

            const actual = yield Trial.findById(3);

            expect(actual.get('passed')).to.equal(0);
        });

        it('should set `passed` field to `1` when all sections are correct', function *() {
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 1 },
                { trial, question: { id: 4 }, section, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 3, answered: 1, correct: 1 },
                { trial, question: { id: 5 }, section, trialTemplate }
            );

            const secondSection = { id: 3, code: 'anotherCode' };

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 2 },
                { trialTemplate, section: secondSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                { trialTemplate, section: secondSection }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 1, answered: 1, correct: 1 },
                { trial, question: { id: 6 }, section: secondSection, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 0 },
                { trial, question: { id: 7 }, section: secondSection, trialTemplate }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            yield attempt.finish();

            const actual = yield Trial.findById(3);

            expect(actual.get('passed')).to.equal(1);
        });

        it('should set `passed` field to `0` when one of sections is incorrect', function *() {
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 1 },
                { trial, question: { id: 4 }, section, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 3, answered: 1, correct: 1 },
                { trial, question: { id: 5 }, section, trialTemplate }
            );

            const secondSection = { id: 3, code: 'anotherCode' };

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 2 },
                { trialTemplate, section: secondSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                { trialTemplate, section: secondSection }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 4, answered: 1, correct: 0 },
                { trial, question: { id: 6 }, section: secondSection, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 5, answered: 1, correct: 0 },
                { trial, question: { id: 7 }, section: secondSection, trialTemplate }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            yield attempt.finish();

            const actual = yield Trial.findById(3);

            expect(actual.get('passed')).to.equal(0);
        });

        it('should set `passed` field to `1` only in current attempt', function *() {
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 1 },
                { trial, question: { id: 4 }, section, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 3, answered: 1, correct: 1 },
                { trial, question: { id: 8 }, section, trialTemplate }
            );

            const oldTrial = {
                id: 2,
                expired: 1,
                started: new Date(),
                finished: new Date(),
                timeLimit: 90000,
                passed: 0
            };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 1, answered: 1, correct: 0 },
                { trial: oldTrial, question: { id: 5 }, section, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 0 },
                { trial: oldTrial, question: { id: 6 }, section, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 3, answered: 1, correct: 0 },
                { trial: oldTrial, question: { id: 7 }, section, trialTemplate }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            yield attempt.finish();

            const actualCurrentAttempt = yield Trial.findById(3);

            expect(actualCurrentAttempt.get('passed')).to.equal(1);

            const actualOldAttempt = yield Trial.findById(2);

            expect(actualOldAttempt.get('passed')).to.equal(0);
        });

        it('should create certificate for test without proctoring when attempt is passed', function *() {
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 1 },
                { trial, question: { id: 4 }, section, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 3, answered: 1, correct: 1 },
                { trial, question: { id: 5 }, section, trialTemplate }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            yield attempt.finish();

            const certificate = yield Certificate.findOne({ where: { trialId: 3 } });
            const actual = certificate.toJSON();

            expect(actual.trialId).to.equal(3);
            expect(actual.firstname).to.equal('Petr');
            expect(actual.lastname).to.equal('Ivanov');
            expect(actual.confirmedDate).to.be.a('date');
            expect(actual.dueDate).to.be.a('date');
            expect(actual.active).to.equal(1);
            expect(actual.confirmed).to.equal(1);
            expect(actual.imagePath).to.equal('603/1468925144742_555555');
        });

        describe('finish trial with proctoring', () => {
            const trialTemplatePro = { id: 7, isProctoring: true };
            const trialPro = { id: 17, expired: 0, openId: 'correct-open-id' };

            beforeEach(function *() {
                yield trialTemplateToSectionsFactory.createWithRelations(
                    { quantity: 2 },
                    { trialTemplate: trialTemplatePro, section }
                );
                yield trialTemplateAllowedFailsFactory.createWithRelations(
                    { allowedFails: 1 },
                    { trialTemplate: trialTemplatePro, section }
                );
                yield trialToQuestionsFactory.createWithRelations(
                    { seq: 1, answered: 1, correct: 0 },
                    {
                        trial: trialPro,
                        trialTemplate: trialTemplatePro,
                        question: { id: 27 },
                        section,
                        user
                    }
                );
            });

            it('should create certificate when attempt is passed with successful proctoring', function *() {
                nockProctorEdu.protocol({ openId: trialPro.openId, response: { evaluation: 93 } });

                yield trialToQuestionsFactory.createWithRelations(
                    { seq: 2, answered: 1, correct: 1 },
                    {
                        trial: trialPro,
                        trialTemplate: trialTemplatePro,
                        question: { id: 28 },
                        section
                    }
                );

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);

                yield attempt.finish();

                const actual = yield Certificate.count({ where: { trialId: 17 } });

                expect(actual).to.equal(1);
            });

            it('should write "correct" verdict to `proctoring_responses` when proctoring is passed', function *() {
                nockProctorEdu.protocol({ openId: trialPro.openId, response: { evaluation: 80 } });

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);
                const now = Date.now();

                yield attempt.finish();

                const found = yield ProctoringResponses.findAll({
                    attributes: [
                        'trialId',
                        'source',
                        'verdict',
                        'evaluation',
                        'time',
                        'isLast',
                        'isSentToToloka'
                    ],
                    raw: true
                });

                expect(found).to.have.length(1);

                const [actual] = found;

                expect(actual.time).to.be.above(now);

                delete actual.time;

                const expected = {
                    trialId: 17,
                    source: 'proctoring',
                    verdict: 'correct',
                    evaluation: 80,
                    isLast: true,
                    isSentToToloka: false
                };

                expect(actual).to.deep.equal(expected);
            });

            it('should write "failed" verdict to `proctoring_responses` when proctoring is not passed', function *() {
                nockProctorEdu.protocol({ openId: trialPro.openId, response: { evaluation: 0 } });

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);
                const now = Date.now();

                yield attempt.finish();

                const found = yield ProctoringResponses.findAll({
                    attributes: [
                        'trialId',
                        'source',
                        'verdict',
                        'evaluation',
                        'time',
                        'isLast',
                        'isSentToToloka'
                    ],
                    raw: true
                });

                expect(found).to.have.length(1);

                const [actual] = found;

                expect(actual.time).to.be.above(now);

                delete actual.time;

                const expected = {
                    trialId: 17,
                    source: 'proctoring',
                    verdict: 'failed',
                    evaluation: 0,
                    isLast: true,
                    isSentToToloka: false
                };

                expect(actual).to.deep.equal(expected);
            });

            it('should not create certificate when verdict is not "correct"', function *() {
                nockProctorEdu.protocol({ openId: trialPro.openId, response: { evaluation: 10 } });

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);

                yield attempt.finish();

                const actual = yield Certificate.count({ where: { trialId: 17 } });

                expect(actual).to.equal(0);
            });

            it('should write "pending" verdict to `proctoring_responses` when proctoring is pending', function *() {
                nockProctorEdu.protocol({ openId: trialPro.openId, response: { evaluation: 65 } });

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);
                const now = Date.now();

                yield attempt.finish();

                const found = yield ProctoringResponses.findAll({
                    attributes: [
                        'trialId',
                        'source',
                        'verdict',
                        'evaluation',
                        'time',
                        'isLast',
                        'isSentToToloka'
                    ],
                    raw: true
                });

                expect(found).to.have.length(1);

                const [actual] = found;

                expect(actual.time).to.be.above(now);

                delete actual.time;

                const expected = {
                    trialId: 17,
                    source: 'proctoring',
                    verdict: 'pending',
                    evaluation: 65,
                    isLast: true,
                    isSentToToloka: false
                };

                expect(actual).to.deep.equal(expected);
            });

            it('should write "pending" verdict when request to proctoring was failed', function *() {
                mockMailer();
                Attempt = require('models/attempt');

                nockProctorEdu.protocol({ openId: trialPro.openId, code: 500 });

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);
                const now = Date.now();

                yield attempt.finish();

                const found = yield ProctoringResponses.findAll({
                    attributes: [
                        'trialId',
                        'source',
                        'verdict',
                        'evaluation',
                        'time',
                        'isLast',
                        'isSentToToloka'
                    ],
                    raw: true
                });

                expect(found).to.have.length(1);

                const [actual] = found;

                expect(actual.time).to.be.above(now);

                delete actual.time;

                const expected = {
                    trialId: 17,
                    source: 'proctoring',
                    verdict: 'pending',
                    evaluation: null,
                    isLast: true,
                    isSentToToloka: false
                };

                expect(actual).to.deep.equal(expected);

                mockery.disable();
                mockery.deregisterAll();
            });

            // EXPERTDEV-972: [API] Заваливать попытку по техническим метрикам
            // при завершении попытки без возможности обжаловать
            it('should failed trial by metrics', function *() {
                nockProctorEdu.protocol({
                    openId: trialPro.openId,
                    response: {
                        evaluation: 30,
                        averages: { b2: 0, c1: 0, c2: 26, c3: 3, c4: 12, c5: 0, m1: 0, m2: 29, n1: 0, s1: 95, s2: 0 }
                    }
                });

                const attempt = yield Attempt.findById(17);

                yield attempt.finish();

                const actual = yield ProctoringResponses.findAll({
                    attributes: [
                        'trialId',
                        'source',
                        'verdict',
                        'evaluation',
                        'isLast',
                        'isSentToToloka',
                        'isRevisionRequested'
                    ],
                    order: [['time']],
                    raw: true
                });

                expect(actual).to.deep.equal([
                    {
                        trialId: 17,
                        source: 'proctoring',
                        verdict: 'failed',
                        evaluation: 30,
                        isLast: false,
                        isSentToToloka: false,
                        isRevisionRequested: false
                    },
                    {
                        trialId: 17,
                        source: 'metrics',
                        verdict: 'failed',
                        evaluation: null,
                        isLast: true,
                        isSentToToloka: false,
                        isRevisionRequested: false
                    }
                ]);
            });

            // EXPERTDEV-1086 [API] Завершать тест с новым флагом
            it('should failed trial by crit-metrics', function *() {
                nockProctorEdu.protocol({
                    openId: trialPro.openId,
                    response: {
                        evaluation: 30,
                        averages: { b2: 0, c1: 0, c2: 0, c3: 0, c4: 0, c5: 0, m1: 0, m2: 0, n1: 0, s1: 0, s2: 0 }
                    }
                });

                const attempt = yield Attempt.findById(17);

                yield attempt.finish(true);

                const actual = yield ProctoringResponses.findAll({
                    attributes: [
                        'trialId',
                        'source',
                        'verdict',
                        'evaluation',
                        'isLast',
                        'isSentToToloka',
                        'isRevisionRequested'
                    ],
                    order: [['time']],
                    raw: true
                });

                expect(actual).to.deep.equal([
                    {
                        trialId: 17,
                        source: 'proctoring',
                        verdict: 'failed',
                        evaluation: 30,
                        isLast: false,
                        isSentToToloka: false,
                        isRevisionRequested: false
                    },
                    {
                        trialId: 17,
                        source: 'crit-metrics',
                        verdict: 'failed',
                        evaluation: null,
                        isLast: true,
                        isSentToToloka: false,
                        isRevisionRequested: false
                    }
                ]);
            });
        });
    });

    describe('`getResult`', () => {
        const now = new Date();
        const trial = {
            id: 3,
            expired: 1,
            passed: 0,
            started: moment(now).subtract(1, 'months').subtract(1, 'hour'),
            finished: moment(now).subtract(1, 'months'),
            nullified: 0,
            timeLimit: 100000
        };
        const trialTemplate = {
            id: 2,
            timeLimit: 100000,
            periodBeforeCertificateReset: '1M',
            delays: '1M, 2M, 3M',
            isProctoring: false
        };
        const dueDate = moment(now).add(2, 'month');
        const certificate = {
            id: 18,
            firstname: 'Andrey',
            lastname: 'Petrov',
            dueDate,
            confirmedDate: moment(now),
            active: 1,
            imagePath: '603/7483573983_55555'
        };
        const service = {
            id: 19,
            code: 'direct',
            title: 'Yandex.Direct'
        };
        const type = {
            id: 13,
            code: 'cert',
            title: 'Certification'
        };
        const firstSection = {
            id: 5,
            code: 'show_strategy',
            title: 'Strategy'
        };
        const secondSection = {
            id: 6,
            code: 'keywords',
            title: 'Key words'
        };
        const user = {
            id: 14,
            uid: 1234567890,
            firstname: 'Petr',
            lastname: 'Ivanov'
        };
        const authType = { id: 2, code: 'web' };

        afterEach(nock.cleanAll);

        beforeEach(function *() {
            nockAvatars.success();

            yield trialsFactory.createWithRelations(
                trial,
                { trialTemplate, service, type, user, authType }
            );

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 2, categoryId: 1 },
                { trialTemplate, section: firstSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                { trialTemplate, section: firstSection }
            );

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 2, categoryId: 1 },
                { trialTemplate, section: secondSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 2 },
                { trialTemplate, section: secondSection }
            );

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 1 },
                {
                    trial,
                    question: { id: 1, categoryId: 1, version: 0 },
                    section: firstSection,
                    trialTemplate,
                    user,
                    authType
                }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 3, answered: 1, correct: 1 },
                {
                    trial,
                    question: { id: 2, categoryId: 1, version: 0 },
                    section: firstSection,
                    trialTemplate,
                    user,
                    authType
                }
            );
        });

        function *createQuestionsInSecondSection() {
            // Questions for second section and first category
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 4, answered: 1, correct: 1 },
                {
                    trial,
                    trialTemplate,
                    user,
                    authType,
                    question: { id: 3, categoryId: 1, version: 3 },
                    section: secondSection
                }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 5, answered: 1, correct: 0 },
                { trial, question: { id: 4, categoryId: 1 }, section: secondSection, trialTemplate, user, authType }
            );
            yield questionsFactory.createWithRelations(
                { id: 3, categoryId: 1, active: 0, version: 2 },
                { section: secondSection, trialTemplate }
            );

            // Relation for second section and second category
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 2, categoryId: 2 },
                { trialTemplate, section: secondSection }
            );
            // Relation for second section and second category to other trialTemplate
            const otherTrialTemplate = { id: 3, isProctoring: false };

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 2, categoryId: 2 },
                { trialTemplate: otherTrialTemplate, section: secondSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                { trialTemplate: otherTrialTemplate, section: secondSection }
            );

            // Questions for second section and second category
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 6, answered: 1, correct: 0 },
                { trial, question: { id: 5, categoryId: 2 }, section: secondSection, trialTemplate, user, authType }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 7, answered: 1, correct: 1 },
                { trial, question: { id: 6, categoryId: 2 }, section: secondSection, trialTemplate, user, authType }
            );
        }

        it('should return correct `totalCount`', function *() {
            yield createQuestionsInSecondSection();

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.total.totalCount).to.equal(6);
        });

        it('should return correct total `correctCount`', function *() {
            yield createQuestionsInSecondSection();

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.total.correctCount).to.equal(4);
        });

        it('should return `passed` = 1 in `total` when all sections passed', function *() {
            yield createQuestionsInSecondSection();

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.total.passed).to.equal(1);
        });

        it('should return `passed` = 1 in `total` when allowed fails missing', function *() {
            yield createQuestionsInSecondSection();

            // Third section without `allowedFails`
            const thirdSection = { id: 7, code: 'clicks', title: 'User clicks' };

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1, categoryId: 1 },
                { trialTemplate, section: thirdSection }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 8, answered: 1, correct: 1 },
                { trial, question: { id: 7, categoryId: 1 }, section: thirdSection, trialTemplate }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);
            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.total.passed).to.equal(1);
        });

        it('should return `passed` = 0 in `total` when one section not passed', function *() {
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1, categoryId: 2 },
                { trialTemplate, section: secondSection }
            );

            // Questions for second section
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 4, answered: 1, correct: 0 },
                { trial, question: { id: 3 }, section: secondSection, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 5, answered: 1, correct: 0 },
                { trial, question: { id: 4 }, section: secondSection, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 6, answered: 1, correct: 0 },
                { trial, question: { id: 5, categoryId: 2 }, section: secondSection, trialTemplate }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.total.passed).to.equal(0);
        });

        it('should return correct service data', function *() {
            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.service).to.deep.equal(service);
        });

        it('should return `finished`', function *() {
            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });
            const expected = moment(now).subtract(1, 'months').toDate();

            expect(actual.finished).to.deep.equal(expected);
        });

        it('should return `isNullified`', function *() {
            const otherTrialTemplate = {
                id: 7,
                isProctroring: false
            };
            const otherTrial = {
                id: 4,
                expired: 0,
                openId: 'correct-open-id',
                nullified: 1
            };

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 2 },
                { trialTemplate: otherTrialTemplate, section: firstSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                { trialTemplate: otherTrialTemplate, section: firstSection }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 1, answered: 1, correct: 0 },
                {
                    trial: otherTrial,
                    trialTemplate: otherTrialTemplate,
                    question: { id: 27 },
                    section: firstSection,
                    user
                }
            );

            const trialItem = yield Trial.findById(3);
            const otherTrialItem = yield Trial.findById(4);

            const attempt = new Attempt(trialItem);
            const otherAttempt = new Attempt(otherTrialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });
            const otherActual = yield otherAttempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.isNullified).to.be.false;
            expect(otherActual.isNullified).to.be.true;
        });

        it('should return `isProctoring`', function *() {
            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.isProctoring).to.be.false;
        });

        it('should return correct type data', function *() {
            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.type).to.deep.equal(type);
        });

        it('should return correct certificate data when certificate exists', function *() {
            yield certificatesFactory.createWithRelations(
                certificate,
                { trial, trialTemplate, service, type }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });
            const imagePath = MdsModel.getAvatarsPath('603/7483573983_55555');

            expect(actual.certId).to.equal(18);
            expect(actual.firstname).to.equal('Andrey');
            expect(actual.lastname).to.equal('Petrov');
            expect(actual.dueDate).to.deep.equal(moment(now).add(2, 'month').toDate());
            expect(actual.active).to.equal(1);
            expect(actual.imagePath).to.equal(imagePath);
        });

        it('should return "" in `imagePath` when this field contain null in db', function *() {
            const certificateWithNull = {
                id: 255,
                dueDate: moment(now).add(2, 'month'),
                confirmedDate: moment(now),
                active: 1,
                imagePath: null
            };
            const otherTrial = {
                id: 256,
                expired: 1,
                passed: 0,
                started: moment(now).subtract(1, 'hour'),
                finished: moment(now)
            };

            yield certificatesFactory.createWithRelations(
                certificateWithNull,
                { trial: otherTrial, trialTemplate, service, type, user, authType }
            );

            const trialItem = yield Trial.findById(256);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.imagePath).to.equal('');
        });

        it('should not return certificate data when certificate does not exist', function *() {
            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.certId).to.be.undefined;
            expect(actual.firstname).to.be.undefined;
            expect(actual.lastname).to.be.undefined;
            expect(actual.dueDate).to.be.undefined;
            expect(actual.active).to.be.undefined;
        });

        it('should return certificate data only for current attempt', function *() {
            const otherTrial = { id: 23 };

            yield certificatesFactory.createWithRelations({ id: 3 }, { trial: otherTrial, trialTemplate });
            yield certificatesFactory.createWithRelations({ id: 4 }, { trial: otherTrial, trialTemplate });
            yield certificatesFactory.createWithRelations({ id: 5 }, { trial, trialTemplate });

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.certId).to.equal(5);
        });

        it('should return `availabilityDate` field when certificate does not exist', function *() {
            const otherTrial = {
                id: 17,
                started: moment(now).subtract(1, 'hour'),
                finished: moment(now),
                expired: 1
            };

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate, user, authType }
            );

            const trialItem = yield Trial.findById(17);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.availabilityDate).to.deep.equal(
                moment(now).add(2, 'month').startOf('day').toDate()
            );
        });

        it('should return `availabilityDate` field when certificate exists', function *() {
            yield certificatesFactory.createWithRelations(
                certificate,
                { trial, trialTemplate, user, authType }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });
            const expectedAvailabilityDate = moment(dueDate)
                .subtract(1, 'month')
                .startOf('day')
                .toDate();

            expect(actual.availabilityDate).to.deep.equal(expectedAvailabilityDate);
        });

        it('should not return `availabilityDate` field when time between two attempts left', function *() {
            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.availabilityDate).to.be.undefined;
        });

        it('should return correct sections data', function *() {
            yield createQuestionsInSecondSection();

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.sections[0].code).to.equal('show_strategy');
            expect(actual.sections[0].title).to.equal('Strategy');
            expect(actual.sections[0].totalCount).to.equal(2);
            expect(actual.sections[0].correctCount).to.equal(2);
            expect(actual.sections[0].allowedFails).to.equal(1);
            expect(actual.sections[0].passed).to.equal(1);

            expect(actual.sections[1].code).to.equal('keywords');
            expect(actual.sections[1].title).to.equal('Key words');
            expect(actual.sections[1].totalCount).to.equal(4);
            expect(actual.sections[1].correctCount).to.equal(2);
            expect(actual.sections[1].allowedFails).to.equal(2);
            expect(actual.sections[1].passed).to.equal(1);
        });

        it('should return sections in correct order', function *() {
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 1, answered: 1, correct: 0 },
                { trial, question: { id: 3 }, section: secondSection, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 4, answered: 1, correct: 0 },
                { trial, question: { id: 4 }, section: secondSection, trialTemplate }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.sections[0].code).to.equal('keywords');
            expect(actual.sections[1].code).to.equal('show_strategy');
        });

        it('should finish attempt when attempt is not finished', function *() {
            yield Trial.update({ expired: 0, finished: null, passed: 0 }, { where: { id: trial.id } });
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 4, answered: 1, correct: 1 },
                { trial, question: { id: 3 }, section: secondSection, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 5, answered: 1, correct: 0 },
                { trial, question: { id: 4 }, section: secondSection, trialTemplate }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            const actualTrial = yield Trial.findById(3);

            expect(actualTrial.expired).to.equal(1);
            expect(actualTrial.finished).to.not.be.null;
            expect(actualTrial.passed).to.equal(1);
        });

        it('should create certificate when attempt is not finished', function *() {
            yield Trial.update({ expired: 0, finished: null, passed: 0 }, { where: { id: trial.id } });
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 4, answered: 1, correct: 1 },
                { trial, question: { id: 3 }, section: secondSection, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 5, answered: 1, correct: 0 },
                { trial, question: { id: 4 }, section: secondSection, trialTemplate }
            );

            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            const otherCertificate = yield Certificate.findOne({ where: { trialId: 3 } });
            const actual = otherCertificate.toJSON();

            expect(actual.trialId).to.equal(3);
            expect(actual.firstname).to.equal('Petr');
            expect(actual.lastname).to.equal('Ivanov');
            expect(actual.confirmedDate).to.be.a('date');
            expect(actual.dueDate).to.be.a('date');
            expect(actual.active).to.equal(1);
            expect(actual.confirmed).to.equal(1);
            expect(actual.imagePath).to.equal('603/1468925144742_555555');
        });

        it('should not create certificate when attempt already finished', function *() {
            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            const actual = yield Certificate.findAll();

            expect(actual).to.be.empty;
        });

        it('should return correct data for not active questions', function *() {
            const otherTrialTemplate = {
                id: 7,
                timeLimit: 100000,
                periodBeforeCertificateReset: '1M',
                delays: '1M, 2M, 3M',
                isProctoring: false
            };
            const otherTrial = {
                id: 13,
                nullified: 0,
                expired: 1,
                started: now,
                finished: now,
                timeLimit: 100000
            };
            const thirdSection = {
                id: 7,
                code: 'magic',
                title: 'Magic words'
            };

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate: otherTrialTemplate, user, authType, service, type }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 2, categoryId: 1 },
                { trialTemplate: otherTrialTemplate, section: firstSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 0 },
                { trialTemplate: otherTrialTemplate, section: firstSection }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1, categoryId: 1 },
                { trialTemplate: otherTrialTemplate, section: secondSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 0 },
                { trialTemplate: otherTrialTemplate, section: secondSection }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1, categoryId: 1 },
                { trialTemplate: otherTrialTemplate, section: thirdSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 0 },
                { trialTemplate: otherTrialTemplate, section: thirdSection }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 1, answered: 1, correct: 1 },
                { trial: otherTrial, question: { id: 1, version: 1, active: 0 }, section: firstSection }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 1 },
                { trial: otherTrial, question: { id: 2, version: 1, active: 0 }, section: firstSection }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 3, answered: 1, correct: 0 },
                { trial: otherTrial, question: { id: 3, version: 2, active: 0 }, section: secondSection }
            );
            yield questionsFactory.createWithRelations(
                { id: 1, version: 2, active: 0 },
                { section: firstSection }
            );
            yield questionsFactory.createWithRelations(
                { id: 3, version: 1, active: 0 },
                { section: thirdSection }
            );

            const trialItem = yield Trial.findById(13);
            const attempt = new Attempt(trialItem);
            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            const expected = {
                total: {
                    totalCount: 3,
                    correctCount: 2,
                    passed: 0
                },
                sections: [
                    {
                        code: 'show_strategy',
                        title: 'Strategy',
                        passed: 1,
                        correctCount: 2,
                        totalCount: 2,
                        allowedFails: 0
                    },
                    {
                        code: 'keywords',
                        title: 'Key words',
                        passed: 0,
                        correctCount: 0,
                        totalCount: 1,
                        allowedFails: 0
                    }
                ],
                service: {
                    id: 19,
                    code: 'direct',
                    title: 'Yandex.Direct'
                },
                type: {
                    id: 13,
                    code: 'cert',
                    title: 'Certification'
                },
                finished: now,
                availabilityDate: moment(now).add(1, 'month').startOf('day').toDate(),
                isProctoring: false,
                isNullified: false
            };

            expect(actual).to.deep.equal(expected);
        });

        it('should not return `proctoringStatus` in total info', function *() {
            const trialItem = yield Trial.findById(3);
            const attempt = new Attempt(trialItem);

            const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

            expect(actual.total.proctoringStatus).to.be.undefined;
        });

        describe('getResult with proctoring', () => {
            const trialTemplatePro = { id: 7, isProctoring: true };
            const trialPro = { id: 17, expired: 0, openId: 'correct-open-id' };

            beforeEach(function *() {
                yield trialTemplateToSectionsFactory.createWithRelations(
                    { quantity: 2 },
                    { trialTemplate: trialTemplatePro, section: firstSection }
                );
                yield trialTemplateAllowedFailsFactory.createWithRelations(
                    { allowedFails: 1 },
                    { trialTemplate: trialTemplatePro, section: firstSection }
                );
                yield trialToQuestionsFactory.createWithRelations(
                    { seq: 1, answered: 1, correct: 0 },
                    {
                        trial: trialPro,
                        trialTemplate: trialTemplatePro,
                        question: { id: 27 },
                        section: firstSection,
                        user
                    }
                );
            });

            it('should return proctoring data in total info', function *() {
                nockProctorEdu.protocol({ openId: trialPro.openId, response: { evaluation: 12 } });

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);

                const actual = yield attempt.getResult({ uid: 1234567890, authType: 'web' });

                expect(actual.total.lastVerdict).to.equal('failed');
                expect(actual.total.lastSource).to.equal('proctoring');
                expect(actual.total.isRevisionRequested).to.be.false;
            });

            it('should create certificate when attempt is passed and proctoring is ok', function *() {
                nockProctorEdu.protocol({ openId: trialPro.openId, response: { evaluation: 97 } });

                yield trialToQuestionsFactory.createWithRelations(
                    { seq: 2, answered: 1, correct: 1 },
                    {
                        trial: trialPro,
                        trialTemplate: trialTemplatePro,
                        question: { id: 28 },
                        section: firstSection
                    }
                );

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);

                const result = yield attempt.getResult({ uid: 1234567890, authType: 'web' });
                const certificates = yield Certificate.findAll({
                    where: { trialId: 17 },
                    attributes: ['id'],
                    raw: true
                });

                expect(certificates.length).to.equal(1);
                expect(result.certId).to.equal(certificates[0].id);
            });

            it('should write "correct" verdict to `proctoring_responses` when proctoring is passed', function *() {
                nockProctorEdu.protocol({ openId: trialPro.openId, response: { evaluation: 93 } });

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);
                const otherNow = Date.now();

                yield attempt.getResult({ uid: 1234567890, authType: 'web' });

                const found = yield ProctoringResponses.findAll({
                    attributes: [
                        'trialId',
                        'source',
                        'verdict',
                        'evaluation',
                        'time',
                        'isLast',
                        'isSentToToloka'
                    ],
                    raw: true
                });

                expect(found).to.have.length(1);

                const [actual] = found;

                expect(actual.time).to.be.above(otherNow);

                delete actual.time;

                const expected = {
                    trialId: 17,
                    source: 'proctoring',
                    verdict: 'correct',
                    evaluation: 93,
                    isLast: true,
                    isSentToToloka: false
                };

                expect(actual).to.deep.equal(expected);
            });

            it('should write "failed" verdict to `proctoring_responses` when proctoring is not passed', function *() {
                nockProctorEdu.protocol({ openId: trialPro.openId, response: { evaluation: 10 } });

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);
                const otherNow = Date.now();

                yield attempt.getResult({ uid: 1234567890, authType: 'web' });

                const found = yield ProctoringResponses.findAll({
                    attributes: [
                        'trialId',
                        'source',
                        'verdict',
                        'evaluation',
                        'time',
                        'isLast',
                        'isSentToToloka'
                    ],
                    raw: true
                });

                expect(found).to.have.length(1);

                const [actual] = found;

                expect(actual.time).to.be.above(otherNow);

                delete actual.time;

                const expected = {
                    trialId: 17,
                    source: 'proctoring',
                    verdict: 'failed',
                    evaluation: 10,
                    isLast: true,
                    isSentToToloka: false
                };

                expect(actual).to.deep.equal(expected);
            });

            it('should not create certificate when verdict is not "correct"', function *() {
                nockProctorEdu.protocol({ openId: trialPro.openId, response: { evaluation: 10 } });

                yield trialToQuestionsFactory.createWithRelations(
                    { seq: 2, answered: 1, correct: 1 },
                    {
                        trial: trialPro,
                        trialTemplate: trialTemplatePro,
                        question: { id: 28 },
                        section: firstSection
                    }
                );

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);

                yield attempt.getResult({ uid: 1234567890, authType: 'web' });

                const actual = yield Certificate.count({ where: { trialId: 17 } });

                expect(actual).to.equal(0);
            });

            it('should write "pending" verdict to `proctoring_responses` when proctoring is pending', function *() {
                nockProctorEdu.protocol({ openId: trialPro.openId, response: { evaluation: 65 } });

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);
                const otherNow = Date.now();

                yield attempt.getResult({ uid: 1234567890, authType: 'web' });

                const found = yield ProctoringResponses.findAll({
                    attributes: [
                        'trialId',
                        'source',
                        'verdict',
                        'evaluation',
                        'time',
                        'isLast',
                        'isSentToToloka'
                    ],
                    raw: true
                });

                expect(found).to.have.length(1);

                const [actual] = found;

                expect(actual.time).to.be.above(otherNow);

                delete actual.time;

                const expected = {
                    trialId: 17,
                    source: 'proctoring',
                    verdict: 'pending',
                    evaluation: 65,
                    isLast: true,
                    isSentToToloka: false
                };

                expect(actual).to.deep.equal(expected);
            });

            it('should write "pending" verdict when request to proctoring was failed', function *() {
                mockMailer();

                Attempt = require('models/attempt');

                nockProctorEdu.protocol({ openId: trialPro.openId, code: 500 });

                yield trialToQuestionsFactory.createWithRelations(
                    { seq: 2, answered: 1, correct: 1 },
                    {
                        trial: trialPro,
                        trialTemplate: trialTemplatePro,
                        question: { id: 28 },
                        section: firstSection
                    }
                );

                const trialItem = yield Trial.findById(17);
                const attempt = new Attempt(trialItem);
                const otherNow = Date.now();

                yield attempt.getResult({ uid: 1234567890, authType: 'web' });

                const found = yield ProctoringResponses.findAll({
                    attributes: [
                        'trialId',
                        'source',
                        'verdict',
                        'evaluation',
                        'time',
                        'isLast',
                        'isSentToToloka'
                    ],
                    raw: true
                });

                expect(found).to.have.length(1);

                const [actual] = found;

                expect(actual.time).to.be.above(otherNow);

                delete actual.time;

                const expected = {
                    trialId: 17,
                    source: 'proctoring',
                    verdict: 'pending',
                    evaluation: null,
                    isLast: true,
                    isSentToToloka: false
                };

                expect(actual).to.deep.equal(expected);

                mockery.disable();
                mockery.deregisterAll();
            });
        });
    });

    describe('getQuestionsCondition', () => {
        it('should return correct condition for trial', function *() {
            const trial = { id: 13 };
            const section = { id: 23 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 1 },
                { trial, question: { id: 1, version: 1 }, section }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2 },
                { trial, question: { id: 2, version: 2 }, section }
            );

            yield [
                { id: 1, version: 2 },
                { id: 2, version: 1 }
            ].map(question => questionsFactory.createWithRelations(question, { section }));

            const actual = yield Attempt.getQuestionsCondition(13);
            const expected = {
                $or: [
                    { id: 1, version: 1 },
                    { id: 2, version: 2 }
                ]
            };

            expect(actual).to.deep.equal(expected);
        });

        it('should return condition only for current trial', function *() {
            const trial = { id: 2 };
            const otherTrial = { id: 3 };
            const section = { id: 23 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 1 },
                { trial, question: { id: 1, version: 1 }, section }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2 },
                { trial: otherTrial, question: { id: 2, version: 2 }, section }
            );

            const actual = yield Attempt.getQuestionsCondition(2);
            const expected = { $or: [{ id: 1, version: 1 }] };

            expect(actual).to.deep.equal(expected);
        });
    });

    describe('getLastFailedTrials', () => {
        let now;
        let started;
        let finished;
        const user = { id: 234, uid: '1234567890' };
        const authType = { id: 2, code: 'web' };
        const trialTemplate = {
            id: 18,
            slug: 'direct',
            delays: '1M, 2M, 3M',
            allowedFails: 1,
            timeLimit: 90000
        };
        let trial;

        beforeEach(function *() {
            now = new Date();
            started = moment(now).subtract(2, 'day').subtract(1, 'hour').toDate();
            finished = moment(now).subtract(2, 'day').toDate();
            trial = {
                id: 67,
                expired: 1,
                passed: 0,
                started,
                finished,
                nullified: 0
            };

            yield trialsFactory.createWithRelations(trial, { trialTemplate, user, authType });
        });

        it('should return correct data for one exam and several trials', function *() {
            yield trialsFactory.createWithRelations({
                id: 3,
                passed: 1,
                expired: 1,
                finished: moment(now).subtract(2, 'year').toDate(),
                started: moment(now).subtract(2, 'year').subtract(1, 'hour').toDate(),
                nullified: 0
            }, { trialTemplate, user, authType });

            yield trialsFactory.createWithRelations({
                id: 5,
                passed: 0,
                finished: moment(now).subtract(1, 'year').toDate(),
                started: moment(now).subtract(1, 'year').subtract(1, 'hour').toDate(),
                nullified: 0
            }, { trialTemplate, user, authType });

            const actual = yield Attempt.getLastFailedTrials(
                ['direct'],
                { uid: user.uid, authTypeCode: authType.code }
            );

            const expected = {
                state: 'disabled',
                examSlug: 'direct',
                started,
                availabilityDate: moment(finished).add(2, 'month').startOf('day').toDate(),
                trialId: trial.id,
                passed: 0,
                source: undefined,
                sections: []
            };

            expect(actual.length).to.equal(1);
            expect(actual[0]).to.deep.equal(expected);
        });

        it('should return data only for current user', function *() {
            const otherAuthType = { code: 'telegram' };
            const otherUser = { id: 345, uid: '4682473642' };
            const otherTrial = {
                id: 23,
                expired: 1,
                passed: 0,
                finished: now,
                started: now,
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate, user: otherUser, authType: otherAuthType }
            );

            const actual = yield Attempt.getLastFailedTrials(
                ['direct'],
                { uid: otherUser.uid, authTypeCode: otherAuthType.code }
            );

            const expected = {
                state: 'disabled',
                examSlug: 'direct',
                started: now,
                availabilityDate: moment(now).add(1, 'month').startOf('day').toDate(),
                trialId: otherTrial.id,
                passed: 0,
                source: undefined,
                sections: []
            };

            expect(actual.length).to.equal(1);
            expect(actual[0]).to.deep.equal(expected);
        });

        it('should return data for not nullified trial', function *() {
            const otherTrial = {
                id: 73,
                expired: 1,
                passed: 1,
                finished: now,
                started: moment(now).subtract(1, 'hour').toDate(),
                nullified: 1
            };

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate, user, authType }
            );

            const actual = yield Attempt.getLastFailedTrials(
                ['direct'],
                { uid: user.uid, authTypeCode: authType.code }
            );

            const expected = {
                state: 'disabled',
                examSlug: 'direct',
                started,
                availabilityDate: moment(finished).add(1, 'month').startOf('day').toDate(),
                trialId: trial.id,
                passed: 0,
                source: undefined,
                sections: []
            };

            expect(actual.length).to.equal(1);
            expect(actual[0]).to.deep.equal(expected);
        });

        it('should return data for specific exams', function *() {
            const otherTrialTemplate = {
                id: 21,
                slug: 'metrika',
                delays: '1M, 2M, 3M',
                allowedFails: 1,
                timeLimit: 90000
            };
            const otherTrial = {
                id: 98,
                expired: 1,
                passed: 0,
                finished: now,
                started: moment(now).subtract(1, 'hour').toDate(),
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate: otherTrialTemplate, user, authType }
            );

            const actual = yield Attempt.getLastFailedTrials(
                ['metrika'],
                { uid: user.uid, authTypeCode: authType.code }
            );

            const expected = {
                state: 'disabled',
                examSlug: 'metrika',
                started: otherTrial.started,
                availabilityDate: moment(now).add(1, 'month').startOf('day').toDate(),
                trialId: otherTrial.id,
                passed: 0,
                source: undefined,
                sections: []
            };

            expect(actual.length).to.equal(1);
            expect(actual[0]).to.deep.equal(expected);
        });

        it('should return correct data for several exams', function *() {
            const secondTrialTemplate = {
                id: 21,
                slug: 'metrika',
                delays: '1M, 2M, 3M',
                allowedFails: 1,
                timeLimit: 3600000
            };
            const secondTrial = {
                id: 98,
                expired: 0,
                passed: 0,
                started: now,
                finished: null,
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                secondTrial,
                { trialTemplate: secondTrialTemplate, user, authType }
            );

            const thirdTrialTemplate = {
                id: 12,
                slug: 'market',
                delays: '1M, 2M, 3M',
                allowedFails: 1,
                timeLimit: 90000
            };
            const thirdTrial = {
                id: 89,
                expired: 1,
                passed: 1,
                started: moment(now).subtract(1, 'hour').toDate(),
                finished: now,
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                thirdTrial,
                { trialTemplate: thirdTrialTemplate, user, authType }
            );

            const fourthTrialTemplate = {
                id: 54,
                slug: 'rsya',
                delays: '1M, 2M, 3M',
                allowedFails: 1,
                timeLimit: 90000
            };
            const fourthTrial = {
                id: 19,
                expired: 1,
                passed: 0,
                started: moment(now).subtract(3, 'year').subtract(1, 'hour').toDate(),
                finished: moment(now).subtract(3, 'year').toDate(),
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                fourthTrial,
                { trialTemplate: fourthTrialTemplate, user, authType }
            );

            const actual = yield Attempt.getLastFailedTrials(
                ['direct', 'metrika', 'market', 'rsya'],
                { uid: user.uid, authTypeCode: authType.code }
            );

            const expected = [
                {
                    state: 'disabled',
                    examSlug: 'direct',
                    started,
                    availabilityDate: moment(finished).add(1, 'month').startOf('day').toDate(),
                    trialId: trial.id,
                    passed: 0,
                    source: undefined,
                    sections: []
                },
                {
                    state: 'enabled',
                    examSlug: 'rsya',
                    started: fourthTrial.started,
                    trialId: fourthTrial.id,
                    passed: 0,
                    source: undefined,
                    sections: []
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return trial when certification was frozen', function *() {
            const otherTrialTemplate = {
                id: 777,
                slug: 'market',
                delays: '1M, 2M, 3M',
                allowedFails: 1,
                timeLimit: 90000
            };
            const otherStarted = moment(now).subtract(3, 'year').toDate();
            const otherTrial = {
                id: 99,
                expired: 1,
                passed: 0,
                started: otherStarted,
                finished: otherStarted,
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate: otherTrialTemplate, user, authType }
            );

            yield freezingFactory.createWithRelations(
                { finishTime: moment(now).add(2, 'hour').toDate() },
                { trialTemplate: otherTrialTemplate }
            );

            const actual = yield Attempt.getLastFailedTrials(
                ['direct', 'market'],
                { uid: user.uid, authTypeCode: authType.code }
            );

            const expected = [
                {
                    state: 'disabled',
                    examSlug: 'direct',
                    started,
                    availabilityDate: moment(finished).add(1, 'month').startOf('day').toDate(),
                    trialId: trial.id,
                    passed: 0,
                    source: undefined,
                    sections: []
                },
                {
                    state: 'frozen',
                    examSlug: 'market',
                    started: otherStarted,
                    trialId: otherTrial.id,
                    passed: 0,
                    source: undefined,
                    sections: []
                }
            ];

            expect(actual.length).to.equal(2);
            expect(actual).to.deep.equal(expected);
        });

        it('should return `enabled` trial when certification was frozen and login has access', function *() {
            const otherTrialTemplate = {
                id: 777,
                slug: 'market',
                delays: '1M, 2M, 3M',
                allowedFails: 1,
                timeLimit: 90000
            };
            const otherStarted = moment(now).subtract(3, 'year').toDate();
            const otherTrial = {
                id: 14,
                expired: 1,
                passed: 0,
                started: otherStarted,
                finished: otherStarted,
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate: otherTrialTemplate, user, authType }
            );

            yield freezingFactory.createWithRelations(
                { finishTime: moment(now).add(2, 'hour').toDate() },
                { trialTemplate: otherTrialTemplate }
            );

            const actual = yield Attempt.getLastFailedTrials(
                ['direct', 'market'],
                {
                    uid: user.uid,
                    authTypeCode: authType.code,
                    login: 'test1'
                }
            );

            const expected = [
                {
                    state: 'disabled',
                    examSlug: 'direct',
                    started,
                    availabilityDate: moment(finished).add(1, 'month').startOf('day').toDate(),
                    trialId: trial.id,
                    passed: 0,
                    source: undefined,
                    sections: []
                },
                {
                    state: 'enabled',
                    examSlug: 'market',
                    started: otherStarted,
                    trialId: otherTrial.id,
                    passed: 0,
                    source: undefined,
                    sections: []
                }
            ];

            expect(actual.length).to.equal(2);
            expect(actual).to.deep.equal(expected);
        });

        it('should add reason for failed proctoring trial', function *() {
            const proctoringResponse = {
                source: 'toloka',
                verdict: 'failed',
                time: now,
                isLast: true
            };
            const otherTrialTemplate = {
                id: 321,
                delays: '1M',
                slug: 'direct_pro',
                isProctoring: true
            };
            const otherTrial = {
                id: 123,
                passed: 1,
                started
            };

            const availabilityDate = moment(now).add(1, 'month').startOf('day').toDate();

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate: otherTrialTemplate, user, authType }
            );
            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { trial: otherTrial }
            );

            const actual = yield Attempt.getLastFailedTrials(
                [otherTrialTemplate.slug],
                { uid: user.uid, authTypeCode: authType.code }
            );

            expect(actual).to.deep.equal([
                {
                    state: 'disabled',
                    examSlug: 'direct_pro',
                    started,
                    availabilityDate,
                    trialId: otherTrial.id,
                    source: 'toloka',
                    passed: 1,
                    sections: []
                }
            ]);
        });

        it('should add reason for failed proctoring trial when it is not passed', function *() {
            const proctoringResponse = {
                source: 'toloka',
                verdict: 'correct',
                time: now,
                isLast: true
            };
            const otherTrialTemplate = {
                id: 321,
                delays: '1M',
                slug: 'direct_pro',
                isProctoring: true
            };
            const otherTrial = {
                id: 123,
                passed: 0,
                started
            };

            const availabilityDate = moment(now).add(1, 'month').startOf('day').toDate();

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate: otherTrialTemplate, user, authType }
            );
            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { trial: otherTrial }
            );

            const actual = yield Attempt.getLastFailedTrials(
                [otherTrialTemplate.slug],
                { uid: user.uid, authTypeCode: authType.code }
            );

            expect(actual).to.deep.equal([
                {
                    state: 'disabled',
                    examSlug: 'direct_pro',
                    started,
                    availabilityDate,
                    trialId: otherTrial.id,
                    source: 'toloka',
                    passed: 0,
                    sections: []
                }
            ]);
        });

        it('should return no failed trials when there is no trials', function *() {
            const otherTrialTemplate = {
                id: 321,
                delays: '1M',
                slug: 'direct_pro',
                isProctoring: true
            };

            yield trialTemplatesFactory.createWithRelations(otherTrialTemplate);

            const actual = yield Attempt.getLastFailedTrials(
                [otherTrialTemplate.slug],
                { uid: user.uid, authTypeCode: authType.code }
            );

            expect(actual).to.deep.equal([]);
        });

        it('should return sections for failed trials', function *() {
            const firstSection = {
                id: 5,
                code: 'show_strategy',
                title: 'Strategy'
            };
            const secondSection = {
                id: 6,
                code: 'keywords',
                title: 'Key words'
            };

            yield trialsFactory.createWithRelations({
                id: 5,
                passed: 0,
                finished: moment(now).subtract(1, 'year').toDate(),
                started: moment(now).subtract(1, 'year').subtract(1, 'hour').toDate(),
                nullified: 0
            }, { trialTemplate, user, authType });

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1, categoryId: 1 },
                { trialTemplate, section: firstSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 0 },
                { trialTemplate, section: firstSection }
            );

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1, categoryId: 1 },
                { trialTemplate, section: secondSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 0 },
                { trialTemplate, section: secondSection }
            );

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 1 },
                {
                    trial,
                    question: { id: 1, categoryId: 1, version: 0 },
                    section: firstSection,
                    trialTemplate,
                    user,
                    authType
                }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 3, answered: 1, correct: 0 },
                {
                    trial,
                    question: { id: 2, categoryId: 1, version: 0 },
                    section: secondSection,
                    trialTemplate,
                    user,
                    authType
                }
            );

            const actual = yield Attempt.getLastFailedTrials(
                ['direct'],
                { uid: user.uid, authTypeCode: authType.code }
            );

            const expected = {
                state: 'disabled',
                examSlug: 'direct',
                started,
                availabilityDate: moment(finished).add(2, 'month').startOf('day').toDate(),
                trialId: trial.id,
                passed: 0,
                source: undefined,
                sections: [
                    {
                        allowedFails: 0,
                        code: 'show_strategy',
                        correctCount: 1,
                        passed: 1,
                        title: 'Strategy',
                        totalCount: 1
                    },
                    {
                        allowedFails: 0,
                        code: 'keywords',
                        correctCount: 0,
                        passed: 0,
                        title: 'Key words',
                        totalCount: 1
                    }
                ]
            };

            expect(actual).to.deep.equal([expected]);
        });
    });

    describe('getPendingTrials', () => {
        let now;
        let started;
        let finished;
        let trial;
        let proctoringResponse;

        const user = { id: 234, uid: '1234567890' };
        const authType = { id: 2, code: 'web' };
        const trialTemplate = {
            id: 18,
            slug: 'direct_pro',
            delays: '1M, 2M, 3M',
            timeLimit: 90000,
            isProctoring: true
        };

        beforeEach(function *() {
            now = new Date();
            started = moment(now).subtract(2, 'day').subtract(1, 'hour').toDate();
            finished = moment(now).subtract(2, 'day').toDate();
            trial = {
                id: 67,
                expired: 1,
                passed: 1,
                started,
                finished,
                nullified: 0
            };
            proctoringResponse = {
                trialId: 67,
                source: 'proctoring',
                verdict: 'pending',
                isLast: true
            };

            yield trialsFactory.createWithRelations(trial, { trialTemplate, user, authType });
            yield proctoringResponsesFactory.createWithRelations(proctoringResponse, { trial });
        });

        it('should return correct pending trials', function *() {
            const actual = yield Attempt.getPendingTrials(
                ['direct_pro'],
                { uid: user.uid, authTypeCode: authType.code }
            );

            const expected = [
                {
                    trialId: trial.id,
                    examSlug: 'direct_pro',
                    started
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return data only for current uid', function *() {
            const otherUser = { id: 345, uid: '4682473642' };
            const otherTrial = {
                id: 23,
                expired: 1,
                passed: 1,
                started: now,
                finished: now,
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate, user: otherUser, authType }
            );
            yield proctoringResponsesFactory.createWithRelations({
                trialId: 23,
                source: 'proctoring',
                verdict: 'pending',
                isLast: true
            }, { trial: { id: 23 } });

            const actual = yield Attempt.getPendingTrials(
                ['direct_pro'],
                { uid: otherUser.uid, authTypeCode: authType.code }
            );

            const expected = [
                {
                    examSlug: 'direct_pro',
                    started: now,
                    trialId: otherTrial.id
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return data only for current auth type', function *() {
            const otherAuthType = { id: 4, code: 'telegram' };
            const otherTrial = {
                id: 23,
                expired: 1,
                passed: 1,
                started: now,
                finished: now,
                nullified: 0
            };
            const otherUser = _.assign(user, { id: 567 });

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate, user: otherUser, authType: otherAuthType }
            );
            yield proctoringResponsesFactory.createWithRelations({
                trialId: 23,
                source: 'proctoring',
                verdict: 'pending',
                isLast: true
            }, { trial: { id: 23 } });

            const actual = yield Attempt.getPendingTrials(
                ['direct_pro'],
                { uid: user.uid, authTypeCode: otherAuthType.code }
            );

            const expected = [
                {
                    examSlug: 'direct_pro',
                    started: now,
                    trialId: otherTrial.id
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return data for not nullified trial', function *() {
            const otherTrialTemplate = {
                id: 21,
                slug: 'metrika_pro',
                delays: '1M, 2M, 3M',
                allowedFails: 1,
                timeLimit: 3600000,
                isProctoring: true
            };
            const otherTrial = {
                id: 73,
                expired: 1,
                passed: 1,
                finished: now,
                started: moment(now).subtract(1, 'hour').toDate(),
                nullified: 1
            };

            yield trialsFactory.createWithRelations(
                otherTrial,
                { otherTrialTemplate, user, authType }
            );
            yield proctoringResponsesFactory.createWithRelations({
                trialId: otherTrial.id,
                source: 'proctoring',
                verdict: 'pending',
                isLast: true
            }, { trial: { id: otherTrial.id } });

            const actual = yield Attempt.getPendingTrials(
                ['direct_pro', 'metrika_pro'],
                { uid: user.uid, authTypeCode: authType.code }
            );

            const expected = [
                {
                    examSlug: 'direct_pro',
                    started,
                    trialId: trial.id
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return data for specific exams', function *() {
            const otherTrialTemplate = {
                id: 21,
                slug: 'metrika_pro',
                delays: '1M, 2M, 3M',
                allowedFails: 1,
                timeLimit: 90000,
                isProctoring: true
            };
            const otherTrial = {
                id: 98,
                expired: 1,
                passed: 1,
                finished: now,
                started: moment(now).subtract(1, 'hour').toDate(),
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate: otherTrialTemplate, user, authType }
            );
            yield proctoringResponsesFactory.createWithRelations({
                trialId: otherTrial.id,
                source: 'proctoring',
                verdict: 'pending',
                isLast: true
            }, { trial: { id: otherTrial.id } });

            const actual = yield Attempt.getPendingTrials(
                ['metrika_pro'],
                { uid: user.uid, authTypeCode: authType.code }
            );

            const expected = [
                {
                    examSlug: 'metrika_pro',
                    started: otherTrial.started,
                    trialId: otherTrial.id
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return correct data for several exams', function *() {
            const secondTrialTemplate = {
                id: 21,
                slug: 'metrika_pro',
                delays: '1M, 2M, 3M',
                allowedFails: 1,
                timeLimit: 3600000,
                isProctoring: true
            };
            const disabledTrial = {
                id: 98,
                expired: 0,
                passed: 1,
                started: now,
                finished: null,
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                disabledTrial,
                { trialTemplate: secondTrialTemplate, user, authType }
            );
            yield proctoringResponsesFactory.createWithRelations({
                trialId: disabledTrial.id,
                source: 'proctoring',
                verdict: 'correct',
                isLast: true
            }, { trial: { id: disabledTrial.id } });

            const thirdTrialTemplate = {
                id: 12,
                slug: 'market_pro',
                delays: '1M, 2M, 3M',
                allowedFails: 0,
                timeLimit: 90000,
                isProctoring: true
            };
            const failedTrial = {
                id: 89,
                expired: 1,
                passed: 0,
                started: moment(now).subtract(1, 'hour').toDate(),
                finished: now,
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                failedTrial,
                { trialTemplate: thirdTrialTemplate, user, authType }
            );

            const actual = yield Attempt.getPendingTrials(
                ['direct_pro', 'metrika_pro', 'market_pro'],
                { uid: user.uid, authTypeCode: authType.code }
            );

            const expected = [
                {
                    examSlug: 'direct_pro',
                    started,
                    trialId: trial.id
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return trial when certification was frozen', function *() {
            yield freezingFactory.createWithRelations(
                { finishTime: moment(now).add(2, 'hour').toDate() },
                { trialTemplate }
            );

            const actual = yield Attempt.getPendingTrials(
                ['direct_pro'],
                { uid: user.uid, authTypeCode: authType.code }
            );

            const expected = [
                {
                    examSlug: 'direct_pro',
                    started,
                    trialId: trial.id
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should not return pending attempts when `isProctoring` is false', function *() {
            const otherTrialTemplate = {
                id: 777,
                slug: 'market',
                delays: '1M, 2M, 3M',
                allowedFails: 1,
                timeLimit: 90000,
                isProctoring: false
            };
            const otherStarted = moment(now).subtract(3, 'year').toDate();
            const otherTrial = {
                id: 14,
                expired: 1,
                passed: 1,
                started: otherStarted,
                finished: otherStarted,
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                otherTrial,
                { trialTemplate: otherTrialTemplate, user, authType }
            );

            const actual = yield Attempt.getPendingTrials(
                ['market'],
                { uid: user.uid, authTypeCode: authType.code }
            );

            expect(actual).to.have.length(0);
        });
    });

    describe('`getNewPendingTrials`', () => {
        let started = null;
        let otherStarted = null;
        let trial = null;
        let otherTrial = null;
        let proctoringResponse = null;
        const user = { id: 234, uid: 567 };

        beforeEach(() => {
            const now = new Date();

            started = moment(now).subtract(2, 'day').subtract(1, 'hour').toDate();
            otherStarted = moment(now).subtract(2, 'day').toDate();
            trial = {
                id: 67,
                started,
                passed: 1,
                nullified: 0,
                openId: 'trial-open-id',
                filesStatus: 'saved'
            };
            otherTrial = {
                id: 76,
                started: otherStarted,
                passed: 1,
                nullified: 0,
                openId: 'other-trial-open-id',
                filesStatus: 'saved'
            };
            proctoringResponse = {
                trialId: trial.id,
                source: 'proctoring',
                verdict: 'pending',
                isLast: true,
                isSentToToloka: false
            };
        });

        it('should return several pending trials', function *() {
            yield proctoringResponsesFactory.createWithRelations(proctoringResponse, { user, trial });

            const otherUser = { id: 657, uid: 2746 };

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isLast: true,
                isSentToToloka: false
            }, { trial: otherTrial, user: otherUser });

            const actual = yield Attempt.getNewPendingTrials();

            const expected = [
                {
                    id: trial.id,
                    userId: user.id,
                    userUid: user.uid,
                    started,
                    openId: trial.openId,
                    isRevision: false
                },
                {
                    id: otherTrial.id,
                    userId: otherUser.id,
                    userUid: otherUser.uid,
                    started: otherStarted,
                    openId: otherTrial.openId,
                    isRevision: false
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should not return trials when response source is not `proctoring`', function *() {
            yield proctoringResponsesFactory.createWithRelations(proctoringResponse, { user, trial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'appeal',
                verdict: 'pending',
                isLast: true,
                isSentToToloka: false
            }, { trial: otherTrial, user });

            const actual = yield Attempt.getNewPendingTrials();

            const expected = [
                {
                    id: trial.id,
                    userId: user.id,
                    userUid: user.uid,
                    started,
                    openId: trial.openId,
                    isRevision: false
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should not return trials when they are nullified', function *() {
            otherTrial.nullified = 1;

            yield proctoringResponsesFactory.createWithRelations(proctoringResponse, { user, trial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isLast: true,
                isSentToToloka: false
            }, { trial: otherTrial, user });

            const actual = yield Attempt.getNewPendingTrials();

            const expected = [
                {
                    id: trial.id,
                    userId: user.id,
                    userUid: user.uid,
                    started,
                    openId: trial.openId,
                    isRevision: false
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return only pending trials', function *() {
            yield proctoringResponsesFactory.createWithRelations(proctoringResponse, { user, trial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'failed',
                isLast: true,
                isSentToToloka: false
            }, { trial: otherTrial, user });

            const actual = yield Attempt.getNewPendingTrials();

            const expected = [
                {
                    id: trial.id,
                    userId: user.id,
                    userUid: user.uid,
                    started,
                    openId: trial.openId,
                    isRevision: false
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return trials than was not sent to Toloka', function *() {
            yield proctoringResponsesFactory.createWithRelations(proctoringResponse, { user, trial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isLast: true,
                isSentToToloka: true
            }, { trial: otherTrial, user });

            const actual = yield Attempt.getNewPendingTrials();

            const expected = [
                {
                    id: trial.id,
                    userId: user.id,
                    userUid: user.uid,
                    started,
                    openId: trial.openId,
                    isRevision: false
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should not return trials than was not passed', function *() {
            otherTrial.passed = 0;

            yield proctoringResponsesFactory.createWithRelations(proctoringResponse, { user, trial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isLast: true,
                isSentToToloka: false
            }, { trial: otherTrial, user });

            const actual = yield Attempt.getNewPendingTrials();

            const expected = [
                {
                    id: trial.id,
                    userId: user.id,
                    userUid: user.uid,
                    started,
                    openId: trial.openId,
                    isRevision: false
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should not return trials when video not saved', function *() {
            otherTrial.filesStatus = 'initial';

            yield proctoringResponsesFactory.createWithRelations(proctoringResponse, { user, trial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isLast: true,
                isSentToToloka: false
            }, { trial: otherTrial, user });

            const actual = yield Attempt.getNewPendingTrials();

            const expected = [
                {
                    id: trial.id,
                    userId: user.id,
                    userUid: user.uid,
                    started,
                    openId: trial.openId,
                    isRevision: false
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should not return trials when verdict has made', function *() {
            yield proctoringResponsesFactory.createWithRelations(proctoringResponse, { user, trial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isLast: false,
                isSentToToloka: false
            }, { trial: otherTrial, user });

            const actual = yield Attempt.getNewPendingTrials();

            const expected = [
                {
                    id: trial.id,
                    userId: user.id,
                    userUid: user.uid,
                    started,
                    openId: trial.openId,
                    isRevision: false
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return no trial when there is no pending trials', function *() {
            const actual = yield Attempt.getNewPendingTrials();

            expect(actual.map(entity => entity.dataValues)).to.deep.equal([]);
        });
    });

    describe('`nullify`', () => {
        const trial = { id: 123, nullified: 0 };
        const otherTrial = { id: 124, nullified: 0 };
        const thirdTrial = { id: 125, nullified: 0 };

        const options = {
            order: ['id'],
            attributes: ['nullified', 'nullifyReason'],
            raw: true
        };

        it('should nullify attempts by id', function *() {
            yield trialsFactory.createWithRelations(trial);
            yield trialsFactory.createWithRelations(otherTrial);
            yield trialsFactory.createWithRelations(thirdTrial);

            const actual = yield Attempt.nullify([123, 124], 'manual');
            const attempts = yield Trial.findAll(options);

            const expected = [
                { nullified: 1, nullifyReason: 'manual' },
                { nullified: 1, nullifyReason: 'manual' },
                { nullified: 0, nullifyReason: null }
            ];

            expect(actual).to.deep.equal([2]);
            expect(attempts).to.deep.equal(expected);
        });

        it('should work correctly when there is no attempt with a specified id', function *() {
            const actual = yield Attempt.nullify([125]);
            const attempt = yield Trial.findOne({ where: { id: 125 } });

            expect(actual).to.deep.equal([0]);
            expect(attempt).to.be.null;
        });
    });

    describe('`getTrialsWithInitialFilesStatus`', () => {
        it('should not get trial that does not expired', function *() {
            const trialTemplate = { isProctoring: true };

            yield trialsFactory.createWithRelations(
                { id: 14, openId: 'session1', filesStatus: 'initial', expired: 1 },
                { trialTemplate }
            );
            yield trialsFactory.createWithRelations(
                { id: 13, openId: 'session2', filesStatus: 'initial', expired: 1 },
                { trialTemplate }
            );
            yield trialsFactory.createWithRelations(
                { id: 15, openId: 'session3', filesStatus: 'initial', expired: 0 },
                { trialTemplate }
            );

            const actual = yield Attempt.getTrialsWithInitialFilesStatus();

            const expected = [
                { id: 13, openId: 'session2' },
                { id: 14, openId: 'session1' }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should not get trial when video saved to s3', function *() {
            yield trialsFactory.createWithRelations({
                id: 15,
                openId: 'session1',
                expired: 1,
                filesStatus: 'saved'
            }, { trialTemplate: { isProctoring: true } });

            yield trialsFactory.createWithRelations({
                id: 16,
                openId: 'session2',
                expired: 1,
                filesStatus: 'initial'
            }, { trialTemplate: { isProctoring: true } });

            const actual = yield Attempt.getTrialsWithInitialFilesStatus();

            expect(actual).to.deep.equal([{ id: 16, openId: 'session2' }]);
        });

        it('should return trial only for exam with proctoring', function *() {
            yield trialsFactory.createWithRelations(
                { id: 16, openId: 'session1', filesStatus: 'initial', expired: 1 },
                { trialTemplate: { isProctoring: true } }
            );
            yield trialsFactory.createWithRelations(
                { id: 17, openId: 'session2', filesStatus: 'initial', expired: 1 },
                { trialTemplate: { isProctoring: false } }
            );

            const actual = yield Attempt.getTrialsWithInitialFilesStatus();

            const expected = [{ id: 16, openId: 'session1' }];

            expect(actual).to.deep.equal(expected);
        });

        it('should return no trial when there is no video for upload', function *() {
            const actual = yield Attempt.getTrialsWithInitialFilesStatus();

            expect(actual).to.deep.equal([]);
        });

        it('should return limited trials', function *() {
            yield trialsFactory.createWithRelations(
                { id: 16, openId: 'session1', filesStatus: 'initial', expired: 1 },
                { trialTemplate: { isProctoring: true } }
            );
            yield trialsFactory.createWithRelations(
                { id: 17, openId: 'session2', filesStatus: 'initial', expired: 1 },
                { trialTemplate: { isProctoring: true } }
            );
            yield trialsFactory.createWithRelations(
                { id: 18, openId: 'session3', filesStatus: 'initial', expired: 1 },
                { trialTemplate: { isProctoring: true } }
            );

            const actual = yield Attempt.getTrialsWithInitialFilesStatus();

            const expected = [
                { id: 16, openId: 'session1' },
                { id: 17, openId: 'session2' }
            ];

            expect(actual).to.deep.equal(expected);
        });
    });

    describe('`setFilesStatusAndPdf`', () => {
        it('should successfully set `filesStatus` = `saved` and save `pdf` name', function *() {
            yield trialsFactory.createWithRelations(
                {
                    openId: 'session1',
                    filesStatus: 'loading',
                    pdf: null
                },
                {}
            );

            yield Attempt.setFilesStatusAndPdf('session1', { pdf: 'some-name.pdf', filesStatus: 'saved' });

            const trials = yield Trial.findAll({
                attributes: ['filesStatus', 'openId', 'pdf'],
                raw: true
            });

            const expected = { openId: 'session1', filesStatus: 'saved', pdf: 'some-name.pdf' };

            expect(trials).to.deep.equal([expected]);
        });

        it('should do nothing when there is no trial with specified `openId`', function *() {
            yield trialsFactory.createWithRelations(
                {
                    openId: 'session1',
                    filesStatus: 'initial',
                    pdf: null
                },
                {}
            );

            yield Attempt.setFilesStatusAndPdf('session2', { pdf: 'some-name.pdf', filesStatus: 'saved' });

            const trials = yield Trial.findAll({
                attributes: ['filesStatus', 'openId', 'pdf'],
                raw: true
            });

            expect(trials).to.deep.equal([{ openId: 'session1', filesStatus: 'initial', pdf: null }]);
        });

        // EXPERTDEV-896: [API] Починить загрузку файлов в s3 по крону
        it('should correct set `filesStatus` when there is no pdf report', function *() {
            yield trialsFactory.createWithRelations(
                {
                    openId: 'session1',
                    filesStatus: 'initial',
                    pdf: null
                },
                {}
            );

            yield Attempt.setFilesStatusAndPdf('session1', { filesStatus: 'saved' });

            const trials = yield Trial.findAll({
                attributes: ['filesStatus', 'openId', 'pdf'],
                raw: true
            });

            expect(trials).to.deep.equal([{ openId: 'session1', filesStatus: 'saved', pdf: null }]);
        });
    });

    describe(`isExpiredTrialExist`, () => {
        it('should return `true` when there is expired trial', () => {
            const started = moment(new Date()).subtract(1, 'month').toDate();
            const trial = { started };

            const actual = Attempt.isExpiredTrialExist([trial]);

            expect(actual).to.be.true;
        });

        it('should return `false` when there is no expired trial', () => {
            const trial = { started: new Date() };

            const actual = Attempt.isExpiredTrialExist([trial]);

            expect(actual).to.be.false;
        });
    });

    describe('getProctoringAnswer', () => {
        const trialTemplatePro = { id: 7, isProctoring: true };
        const trialPro = { id: 17, expired: 0, openId: 'correct-open-id' };
        const section = { id: 3, code: 'first' };

        beforeEach(function *() {
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 2 },
                { trialTemplate: trialTemplatePro, section }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                { trialTemplate: trialTemplatePro, section }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 1, answered: 1, correct: 0 },
                {
                    trial: trialPro,
                    trialTemplate: trialTemplatePro,
                    question: { id: 27 },
                    section
                }
            );
        });

        afterEach(nock.cleanAll);

        it('should return `correct` verdict if proctoring is passed', function *() {
            nockProctorEdu.protocol({
                openId: trialPro.openId,
                response: { evaluation: 100 }
            });
            const actual = yield Attempt.getProctoringAnswer(trialPro.openId);

            expect(actual).to.deep.equal({
                response: {
                    source: 'proctoring',
                    verdict: 'correct',
                    evaluation: 100
                },
                sessionData: { evaluation: 100 }
            });
        });

        it('should return `failed` verdict if proctoring is not passed', function *() {
            nockProctorEdu.protocol({
                openId: trialPro.openId,
                response: { evaluation: 0 }
            });
            const actual = yield Attempt.getProctoringAnswer(trialPro.openId);

            expect(actual).to.deep.equal({
                response: {
                    source: 'proctoring',
                    verdict: 'failed',
                    evaluation: 0
                },
                sessionData: { evaluation: 0 }
            });
        });

        it('should return `pending` verdict if proctoring in pending range', function *() {
            nockProctorEdu.protocol({
                openId: trialPro.openId,
                response: { evaluation: 60 }
            });
            const actual = yield Attempt.getProctoringAnswer(trialPro.openId);

            expect(actual).to.deep.equal({
                response: {
                    source: 'proctoring',
                    verdict: 'pending',
                    evaluation: 60
                },
                sessionData: { evaluation: 60 }
            });
        });

        it('should return `pending` verdict if answer has no evaluation', function *() {
            mockMailer();
            Attempt = require('models/attempt');

            nockProctorEdu.protocol({
                openId: trialPro.openId,
                response: {}
            });
            const actual = yield Attempt.getProctoringAnswer(trialPro.openId);

            expect(actual).to.deep.equal({
                response: {
                    source: 'proctoring',
                    verdict: 'pending',
                    evaluation: null
                },
                sessionData: undefined
            });

            mockery.disable();
            mockery.deregisterAll();
        });
    });

    describe('findByIds', () => {
        it('should find trials by ids and return requested fields', function *() {
            yield trialsFactory.createWithRelations({ id: 1, nullified: 0 });
            yield trialsFactory.createWithRelations({ id: 2 });
            yield trialsFactory.createWithRelations({ id: 3, nullified: 1 });

            let actual = yield Attempt.findByIds([1, 3], ['id', 'nullified']);

            actual = _.sortBy(actual, 'id');

            expect(actual).to.deep.equal([
                { id: 1, nullified: 0 },
                { id: 3, nullified: 1 }
            ]);
        });

        it('should return empty array if trials not found', function *() {
            const actual = yield Attempt.findByIds([1, 2, 3], ['id']);

            expect(actual).to.deep.equal([]);
        });
    });

    describe('`tryFindCertAndAppeal`', () => {
        const trial = { id: 7 };

        it('should return empty fields when there are no cert and appeal for trial', function *() {
            yield trialsFactory.createWithRelations(trial);

            const actual = yield Attempt.tryFindCertAndAppeal(7);

            expect(actual.certId).to.be.undefined;
            expect(actual.verdict).to.be.undefined;
        });

        it('should return correct certId', function *() {
            yield certificatesFactory.createWithRelations({ id: 13 }, { trial });

            const actual = yield Attempt.tryFindCertAndAppeal(7);

            expect(actual.certId).to.equal(13);
        });

        it('should return correct verdict when appeal for trial has been made', function *() {
            yield proctoringResponsesFactory.createWithRelations(
                { verdict: 'correct', source: 'appeal', isLast: true },
                { trial }
            );
            yield proctoringResponsesFactory.createWithRelations(
                { verdict: 'failed', source: 'appeal' },
                { trial }
            );
            yield proctoringResponsesFactory.createWithRelations(
                { verdict: 'pending', source: 'proctoring' },
                { trial }
            );

            const actual = yield Attempt.tryFindCertAndAppeal(7);

            expect(actual.verdict).to.equal('correct');
        });

        it('should return data for requested trial', function *() {
            yield certificatesFactory.createWithRelations({ id: 17 }, { trial });
            yield proctoringResponsesFactory.createWithRelations(
                { verdict: 'failed', source: 'appeal', isLast: true },
                { trial }
            );

            const otherTrial = { id: 8 };

            yield certificatesFactory.createWithRelations({ id: 18 }, { trial: otherTrial });
            yield proctoringResponsesFactory.createWithRelations(
                { verdict: 'correct', source: 'appeal', isLast: true },
                { trial: otherTrial }
            );

            const actual = yield Attempt.tryFindCertAndAppeal(7);

            expect(actual).to.deep.equal({
                verdict: 'failed',
                certId: 17
            });
        });
    });

    describe('getTrialsInfo', () => {
        const authType = { id: 2, code: 'web' };
        const firstStarted = new Date(2019, 7, 7);
        const secondStarted = new Date(2019, 10, 5);
        const thirdStarted = new Date(2017, 9, 19);

        it('should find trials by ids and return requested fields', function *() {
            const trialTemplate = {
                id: 10,
                slug: 'some-exam'
            };
            const otherTrialTemplate = {
                id: 11,
                slug: 'other-exam'
            };
            const firstUser = {
                id: 2020,
                uid: 1234567890,
                login: 'first-user'
            };
            const secondUser = {
                id: 3030,
                uid: 1234567,
                login: 'second-user'
            };

            yield usersFactory.createWithRelations(firstUser, { authType });
            yield usersFactory.createWithRelations(secondUser, { authType });

            yield trialsFactory.createWithRelations({
                id: 1,
                nullified: 0,
                started: firstStarted
            }, { trialTemplate, user: firstUser });
            yield trialsFactory.createWithRelations({
                id: 2,
                nullified: 1,
                started: secondStarted
            }, { trialTemplate, user: secondUser });
            yield trialsFactory.createWithRelations({
                id: 3,
                nullified: 0,
                started: thirdStarted
            }, { trialTemplate: otherTrialTemplate, user: firstUser });

            const actual = yield Attempt.getTrialsInfo([1, 2, 3, 4]);

            expect(actual).to.deep.equal([
                {
                    trialId: 1,
                    nullified: 0,
                    started: firstStarted,
                    login: 'first-user',
                    trialTemplateSlug: 'some-exam'
                },
                {
                    trialId: 2,
                    nullified: 1,
                    started: secondStarted,
                    login: 'second-user',
                    trialTemplateSlug: 'some-exam'
                },
                {
                    trialId: 3,
                    nullified: 0,
                    started: thirdStarted,
                    login: 'first-user',
                    trialTemplateSlug: 'other-exam'
                }
            ]);
        });

        it('should return empty array if trials not found', function *() {
            const actual = yield Attempt.getTrialsInfo([1, 2, 3]);

            expect(actual).to.deep.equal([]);
        });
    });

    describe('`getNewTrialsForRevision`', () => {
        const trial = {
            id: 1,
            passed: 1,
            nullified: 0,
            openId: 'norm',
            started: new Date(2019, 7, 7),
            filesStatus: 'saved'
        };
        const user = { id: 1 };
        const otherUser = { id: 2 };
        const authType = { code: 'web' };

        beforeEach(function *() {
            yield proctoringResponsesFactory.createWithRelations(
                { isSentToToloka: false, isRevisionRequested: true, isLast: true },
                { trial, user, authType }
            );
        });

        it('should return not nullified trials', function *() {
            const otherTrial = {
                id: 2,
                passed: 1,
                nullified: 1,
                openId: 'other',
                started: new Date(2019, 3, 5),
                filesStatus: 'saved'
            };

            yield proctoringResponsesFactory.createWithRelations(
                { isSentToToloka: false, isRevisionRequested: true, isLast: true },
                { trial: otherTrial, user: otherUser, authType }
            );

            const actual = yield Attempt.getNewTrialsForRevision();
            const expected = [
                {
                    id: 1,
                    openId: 'norm',
                    userId: 1,
                    started: trial.started,
                    isRevision: true
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return passed trials', function *() {
            const otherTrial = {
                id: 2,
                passed: 0,
                nullified: 0,
                openId: 'other',
                started: new Date(2019, 3, 5),
                filesStatus: 'saved'
            };

            yield proctoringResponsesFactory.createWithRelations(
                { isSentToToloka: false, isRevisionRequested: true, isLast: true },
                { trial: otherTrial, user: otherUser, authType }
            );

            const actual = yield Attempt.getNewTrialsForRevision();
            const expected = [
                {
                    id: 1,
                    openId: 'norm',
                    userId: 1,
                    started: trial.started,
                    isRevision: true
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return trials with saved data', function *() {
            const otherTrial = {
                id: 2,
                passed: 1,
                nullified: 0,
                openId: 'other',
                started: new Date(2019, 3, 5),
                filesStatus: 'initial'
            };

            yield proctoringResponsesFactory.createWithRelations(
                { isSentToToloka: false, isRevisionRequested: true, isLast: true },
                { trial: otherTrial, user: otherUser, authType }
            );

            const actual = yield Attempt.getNewTrialsForRevision();
            const expected = [
                {
                    id: 1,
                    openId: 'norm',
                    userId: 1,
                    started: trial.started,
                    isRevision: true
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return trials with request to revision', function *() {
            const otherTrial = {
                id: 2,
                passed: 1,
                nullified: 0,
                openId: 'other',
                started: new Date(2019, 3, 5),
                filesStatus: 'saved'
            };

            yield proctoringResponsesFactory.createWithRelations(
                { isSentToToloka: false, isRevisionRequested: false, isLast: true },
                { trial: otherTrial, user: otherUser, authType }
            );

            const actual = yield Attempt.getNewTrialsForRevision();
            const expected = [
                {
                    id: 1,
                    openId: 'norm',
                    userId: 1,
                    started: trial.started,
                    isRevision: true
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return not sent to Toloka trials', function *() {
            const otherTrial = {
                id: 2,
                passed: 1,
                nullified: 0,
                openId: 'other',
                started: new Date(2019, 3, 5),
                filesStatus: 'saved'
            };

            yield proctoringResponsesFactory.createWithRelations(
                { isSentToToloka: true, isRevisionRequested: true, isLast: true },
                { trial: otherTrial, user: otherUser, authType }
            );

            const actual = yield Attempt.getNewTrialsForRevision();
            const expected = [
                {
                    id: 1,
                    openId: 'norm',
                    userId: 1,
                    started: trial.started,
                    isRevision: true
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return trials where request to revision in last record', function *() {
            const otherTrial = {
                id: 2,
                passed: 1,
                nullified: 0,
                openId: 'other',
                started: new Date(2019, 3, 5),
                filesStatus: 'saved'
            };

            yield proctoringResponsesFactory.createWithRelations(
                { isSentToToloka: false, isRevisionRequested: true, isLast: false },
                { trial: otherTrial, user: otherUser, authType }
            );

            const actual = yield Attempt.getNewTrialsForRevision();
            const expected = [
                {
                    id: 1,
                    openId: 'norm',
                    userId: 1,
                    started: trial.started,
                    isRevision: true
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return no trial when there is no trials to revision', function *() {
            yield Trial.update({ passed: 0 }, { where: { id: trial.id } });

            const actual = yield Attempt.getNewTrialsForRevision();

            expect(actual).to.deep.equal([]);
        });
    });

    describe('`processProctoringAnswer`', () => {
        const trialTemplatePro = { id: 7, isProctoring: true };
        const trialPro = { id: 17, openId: 'correct-open-id' };

        beforeEach(function *() {
            yield trialsFactory.createWithRelations(trialPro, { trialTemplate: trialTemplatePro });
        });

        afterEach(nock.cleanAll);

        it('should create only one record when metrics are not high', function *() {
            nockProctorEdu.protocol({
                openId: trialPro.openId,
                response: {
                    evaluation: 100,
                    averages: { b2: 0, c1: 0, c2: 26, c3: 3, c4: 12, c5: 0, m1: 0, m2: 29, n1: 0, s1: 1, s2: 0 }
                }
            });

            const attempt = yield Attempt.findById(17);

            const actual = yield attempt.processProctoringAnswer();

            expect(_.pick(actual, [
                'evaluation',
                'isLast',
                'trialId',
                'source',
                'verdict'
            ])).to.deep.equal({
                evaluation: 100,
                isLast: true,
                trialId: 17,
                source: 'proctoring',
                verdict: 'correct'
            });

            const responses = yield ProctoringResponses.findAll({
                attributes: ['source', 'verdict', 'evaluation'],
                raw: true
            });

            expect(responses).to.deep.equal([{
                source: 'proctoring',
                verdict: 'correct',
                evaluation: 100
            }]);
        });

        it('should create additional record by `metrics` when metrics are high', function *() {
            nockProctorEdu.protocol({
                openId: trialPro.openId,
                response: {
                    evaluation: 70,
                    averages: { b2: 0, c1: 0, c2: 26, c3: 3, c4: 12, c5: 0, m1: 0, m2: 29, n1: 0, s1: 100, s2: 0 }
                }
            });

            const attempt = yield Attempt.findById(17);

            const actual = yield attempt.processProctoringAnswer();

            expect(_.pick(actual, [
                'evaluation',
                'isLast',
                'trialId',
                'source',
                'verdict'
            ])).to.deep.equal({
                evaluation: null,
                isLast: true,
                trialId: 17,
                source: 'metrics',
                verdict: 'failed'
            });

            const responses = yield ProctoringResponses.findAll({
                attributes: ['source', 'verdict', 'evaluation'],
                order: [['time']],
                raw: true
            });

            expect(responses).to.deep.equal([
                {
                    source: 'proctoring',
                    verdict: 'pending',
                    evaluation: 70
                },
                {
                    source: 'metrics',
                    verdict: 'failed',
                    evaluation: null
                }
            ]);
        });

        it('should correct process proctoring answer when proctoring does not send session data', function *() {
            nockProctorEdu.protocol({
                openId: trialPro.openId,
                response: {}
            });

            mockMailer();
            Attempt = require('models/attempt');

            const attempt = yield Attempt.findById(17);

            const actual = yield attempt.processProctoringAnswer();

            expect(_.pick(actual, [
                'evaluation',
                'isLast',
                'trialId',
                'source',
                'verdict'
            ])).to.deep.equal({
                evaluation: null,
                isLast: true,
                trialId: 17,
                source: 'proctoring',
                verdict: 'pending'
            });

            const responses = yield ProctoringResponses.findAll({
                attributes: ['source', 'verdict', 'evaluation'],
                raw: true
            });

            expect(responses).to.deep.equal([{
                source: 'proctoring',
                verdict: 'pending',
                evaluation: null
            }]);

            mockery.disable();
            mockery.deregisterAll();
        });
    });

    describe('`saveProctoringMetrics`', () => {
        beforeEach(function *() {
            yield trialsFactory.createWithRelations({ id: 323 });
            yield trialsFactory.createWithRelations({ id: 324 });
        });

        it('should save proctoring metrics', function *() {
            yield Attempt.saveProctoringMetrics(323, {
                critical: [
                    { metric: 'c4', duration: 3000 }
                ]
            });

            const expected = {
                id: 323,
                proctoringMetrics: {
                    critical: [
                        { metric: 'c4', duration: 3000 }
                    ]
                }
            };
            const actual = yield Trial.findById(323, {
                attributes: ['id', 'proctoringMetrics'],
                raw: true
            });

            expect(actual).to.deep.equal(expected);
        });

        it('should not change other attempt\'s proctoring metrics', function *() {
            yield Attempt.saveProctoringMetrics(323, {
                critical: [
                    { metric: 'c4', duration: 3000 }
                ]
            });

            const expected = {
                id: 324,
                proctoringMetrics: null
            };
            const actual = yield Trial.findById(324, {
                attributes: ['id', 'proctoringMetrics'],
                raw: true
            });

            expect(actual).to.deep.equal(expected);
        });
    });

    describe('`getProctoringMetrics`', () => {
        it('should get proctoring metrics', function *() {
            yield trialsFactory.createWithRelations({
                id: 323,
                proctoringMetrics: {
                    critical: [
                        { metric: 'c4', duration: 3000 }
                    ]
                }
            });
            yield trialsFactory.createWithRelations({
                id: 324,
                proctoringMetrics: {
                    critical: []
                }
            });

            const expected = {
                critical: [
                    { metric: 'c4', duration: 3000 }
                ]
            };
            const actual = yield Attempt.getProctoringMetrics(323);

            expect(actual).to.deep.equal(expected);
        });
    });

    describe('`isNullifiesByMetricsLimitExceeded`', () => {
        it('should return false if there is no nullified attempts since last week', function *() {
            const started = new Date(2019);

            yield trialsFactory.createWithRelations(
                { id: 2, nullified: 1, nullifyReason: 'metrics', started },
                { trialTemplate: { id: 1 }, user: { id: 1 } }
            );
            yield trialsFactory.createWithRelations(
                { id: 3, nullified: 1, nullifyReason: 'metrics', started },
                { trialTemplate: { id: 1 }, user: { id: 1 } }
            );

            const actual = yield Attempt.isNullifiesByMetricsLimitExceeded(1, 1);

            expect(actual).to.be.false;
        });

        it('should not count manually nullified attempts', function *() {
            const started = moment().subtract(1, 'day');

            yield trialsFactory.createWithRelations(
                { id: 1, nullified: 1, nullifyReason: 'metrics', started: started.toDate() },
                { trialTemplate: { id: 1 }, user: { id: 1 } }
            );
            yield trialsFactory.createWithRelations(
                { id: 2, nullified: 1, nullifyReason: 'metrics', started: started.subtract(1, 'day').toDate() },
                { trialTemplate: { id: 1 }, user: { id: 1 } }
            );
            yield trialsFactory.createWithRelations(
                { id: 3, nullified: 1, nullifyReason: 'manual', started: started.subtract(2, 'day').toDate() },
                { trialTemplate: { id: 1 }, user: { id: 1 } }
            );

            const actual = yield Attempt.isNullifiesByMetricsLimitExceeded(1, 1);

            expect(actual).to.be.false;
        });

        it('should correctly count nullified attempts with different finished times', function *() {
            const started = moment().subtract(1, 'day');

            yield trialsFactory.createWithRelations(
                { id: 1, nullified: 1, nullifyReason: 'metrics', started: started.toDate() },
                { trialTemplate: { id: 1 }, user: { id: 1 } }
            );
            yield trialsFactory.createWithRelations(
                { id: 2, nullified: 1, nullifyReason: 'metrics', started: started.subtract(1, 'day').toDate() },
                { trialTemplate: { id: 1 }, user: { id: 1 } }
            );
            yield trialsFactory.createWithRelations(
                { id: 3, nullified: 1, nullifyReason: 'metrics', started: started.subtract(10, 'day').toDate() },
                { trialTemplate: { id: 1 }, user: { id: 1 } }
            );

            const actual = yield Attempt.isNullifiesByMetricsLimitExceeded(1, 1);

            expect(actual).to.be.false;
        });

        it('should correctly count nullified attempts with different users', function *() {
            const started = moment().subtract(1, 'day');

            yield trialsFactory.createWithRelations(
                { id: 1, nullified: 1, nullifyReason: 'metrics', started: started.toDate() },
                { trialTemplate: { id: 1 }, user: { id: 1 } }
            );
            yield trialsFactory.createWithRelations(
                { id: 2, nullified: 1, nullifyReason: 'metrics', started: started.subtract(1, 'day').toDate() },
                { trialTemplate: { id: 1 }, user: { id: 1 } }
            );
            yield trialsFactory.createWithRelations(
                { id: 3, nullified: 1, nullifyReason: 'metrics', started: started.subtract(10, 'day').toDate() },
                { trialTemplate: { id: 1 }, user: { id: 2 } }
            );

            const actual = yield Attempt.isNullifiesByMetricsLimitExceeded(1, 1);

            expect(actual).to.be.false;
        });

        it('should correctly count nullified attempts with trialTemplates', function *() {
            const started = moment().subtract(1, 'day');

            yield trialsFactory.createWithRelations(
                { id: 1, nullified: 1, nullifyReason: 'metrics', started: started.toDate() },
                { trialTemplate: { id: 1 }, user: { id: 1 } }
            );
            yield trialsFactory.createWithRelations(
                { id: 2, nullified: 1, nullifyReason: 'metrics', started: started.subtract(1, 'day').toDate() },
                { trialTemplate: { id: 1 }, user: { id: 1 } }
            );
            yield trialsFactory.createWithRelations(
                { id: 3, nullified: 1, nullifyReason: 'metrics', started: started.subtract(10, 'day').toDate() },
                { trialTemplate: { id: 2 }, user: { id: 1 } }
            );

            const actual = yield Attempt.isNullifiesByMetricsLimitExceeded(1, 1);

            expect(actual).to.be.false;
        });
    });

    describe('`getAttemptInfo`', () => {
        it('should return data when trial is found', function *() {
            yield certificatesFactory.createWithRelations(
                { id: 555 },
                {
                    trial: { id: 123 },
                    trialTemplate: { id: 5, slug: 'some', language: 0 }
                }
            );
            yield trialsFactory.createWithRelations(
                { id: 456 },
                { trialTemplate: { id: 7, slug: 'any' } }
            );

            const actual = yield Attempt.getAttemptInfo(123);

            expect(actual.toJSON()).to.deep.equal({
                id: 123,
                certificates: [{ id: 555 }],
                trialTemplate: {
                    delays: [], // берется с помощью get() из entity trialTemplate
                    language: 'ru',
                    slug: 'some'
                }
            });
        });

        it('should not return cert data when trial is not passed', function *() {
            yield trialsFactory.createWithRelations(
                { id: 456 },
                { trialTemplate: { id: 7, slug: 'without-cert', language: 0 } }
            );

            const actual = yield Attempt.getAttemptInfo(456);

            expect(actual.toJSON()).to.deep.equal({
                id: 456,
                certificates: [],
                trialTemplate: {
                    delays: [], // берется с помощью get() из entity trialTemplate
                    language: 'ru',
                    slug: 'without-cert'
                }
            });
        });

        it('should return `null` when trial is not found', function *() {
            const actual = yield Attempt.getAttemptInfo(123);

            expect(actual).to.be.null;
        });
    });
});
