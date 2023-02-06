const { expect } = require('chai');
const _ = require('lodash');

const UserTrialsReport = require('models/report/items/userTrialsReport');

const catchError = require('tests/helpers/catchError').generator;
const certificatesFactory = require('tests/factory/certificatesFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');

describe('User trials report model', () => {
    beforeEach(require('tests/helpers/clear').clear);

    it('should throw error when login is invalid', function *() {
        const query = {};
        const error = yield catchError(UserTrialsReport.apply.bind(null, query));

        expect(error.message).to.equal('Login is invalid');
        expect(error.status).to.equal(400);
        expect(error.options).to.deep.equal({
            internalCode: '400_LII',
            searchedLogin: undefined
        });
    });

    it('should pick fields correctly when trial passed', function *() {
        const user = { login: 'mokhov' };
        const started = new Date(2017, 0, 25, 12, 0, 10);
        const finished = new Date(2017, 0, 25, 12, 0, 40);
        const dueDate = new Date(2017, 0, 26);
        const trial = { id: 5, started, finished, passed: 1, nullified: 0 };
        const trialTemplate = { title: 'direct', isProctoring: false };

        yield certificatesFactory.createWithRelations(
            { id: 1, dueDate, active: 1 },
            { user, trial, trialTemplate }
        );

        const actual = yield UserTrialsReport.apply({ login: 'mokhov' });

        const expected = {
            exam: 'direct',
            trialId: 5,
            started,
            finished,
            passed: 1,
            nullified: 0,
            certId: 1,
            certDueDate: dueDate,
            isCertDeactivated: 0,
            deactivateDate: '',
            deactivateReason: '',
            isProctoring: 0,
            proctoringAnswer: '?',
            isMetricsHigh: 0,
            isPendingSentToToloka: 0,
            autoTolokaVerdict: '?',
            isRevisionRequested: 0,
            revisionVerdict: '?',
            appealVerdict: '?',
            finalVerdict: 1
        };

        expect(actual).to.have.length(1);
        expect(actual[0]).to.deep.equal(expected);
    });

    it('should pick fields correctly when trial not passed', function *() {
        const user = { login: 'dotokoto' };
        const started = new Date(2017, 0, 25, 12, 0, 10);
        const finished = new Date(2017, 0, 25, 12, 0, 40);
        const trial = { id: 5, started, finished, passed: 0, nullified: 0 };
        const trialTemplate = { title: 'direct', isProctoring: false };

        yield trialsFactory.createWithRelations(
            trial,
            { user, trialTemplate }
        );

        const actual = yield UserTrialsReport.apply({ login: 'dotokoto' });

        const expected = {
            exam: 'direct',
            trialId: 5,
            started,
            finished,
            passed: 0,
            nullified: 0,
            certId: '',
            certDueDate: '',
            isCertDeactivated: '',
            deactivateDate: '',
            deactivateReason: '',
            isProctoring: 0,
            proctoringAnswer: '?',
            isMetricsHigh: 0,
            isPendingSentToToloka: 0,
            autoTolokaVerdict: '?',
            isRevisionRequested: 0,
            revisionVerdict: '?',
            appealVerdict: '?',
            finalVerdict: 0
        };

        expect(actual).to.deep.equal([expected]);
    });

    it('should return data about deactivate certificate', function *() {
        const user = { login: 'dimastark' };
        const started = new Date(2017, 0, 10);
        const finished = new Date(2017, 0, 10);
        const trial = { id: 5, started, finished, passed: 1, nullified: 0 };
        const trialTemplate = { title: 'direct', isProctoring: false };
        const dueDate = new Date(2017, 0, 26);
        const deactivateDate = new Date(2017, 0, 15);
        const deactivateReason = 'ban';

        yield certificatesFactory.createWithRelations(
            {
                id: 17,
                dueDate,
                active: 0,
                deactivateDate,
                deactivateReason
            },
            { user, trial, trialTemplate }
        );

        const actual = yield UserTrialsReport.apply({ login: 'dimastark' });

        const expected = {
            exam: 'direct',
            trialId: 5,
            started,
            finished,
            passed: 1,
            nullified: 0,
            certId: 17,
            certDueDate: dueDate,
            isCertDeactivated: 1,
            deactivateDate,
            deactivateReason,
            isProctoring: 0,
            proctoringAnswer: '?',
            isMetricsHigh: 0,
            isPendingSentToToloka: 0,
            autoTolokaVerdict: '?',
            isRevisionRequested: 0,
            revisionVerdict: '?',
            appealVerdict: '?',
            finalVerdict: 1
        };

        expect(actual).to.deep.equal([expected]);
    });

    it('should filter trials by login', function *() {
        const trial = {};

        yield [
            { id: 1, login: 'm-smirnov' },
            { id: 2, login: 'anyok' }
        ].map((user, i) => certificatesFactory.createWithRelations(
            { id: i + 1 },
            { trial, user }
        ));

        const actual = yield UserTrialsReport.apply({ login: 'anyok' });

        expect(actual).to.have.length(1);
        expect(actual[0].certId).to.equal(2);
    });

    it('should find trials for alias to login', function *() {
        const trial = {};

        yield [
            { id: 1, login: 'm-smirnov.Alekseevich' },
            { id: 2, login: 'zhigalov' }
        ].map((user, i) => certificatesFactory.createWithRelations(
            { id: i + 1 },
            { trial, user }
        ));

        const actual = yield UserTrialsReport.apply({ login: 'm.smirnov-alekseevich' });

        expect(actual).to.have.length(1);
        expect(actual[0].certId).to.equal(1);
    });

    it('should return trial without certificate', function *() {
        const user = { login: 'mokhov' };

        yield certificatesFactory.createWithRelations({}, { trial: { passed: 1 }, user });
        yield trialsFactory.createWithRelations({ passed: 0 }, { user });

        const actual = yield UserTrialsReport.apply({ login: 'mokhov' });

        expect(actual).to.have.length(2);
    });

    it('should return trials from different exams', function *() {
        const user = { login: 'mokhov' };

        yield [
            { id: 1, title: 'direct' },
            { id: 2, title: 'shim' }
        ].map((trialTemplate, i) => certificatesFactory.createWithRelations(
            { id: i + 1 },
            { user, trial: { id: i + 1, finished: new Date(2017, 0, i + 20) }, trialTemplate }
        ));

        const actual = yield UserTrialsReport.apply({ login: 'mokhov' });

        expect(actual).to.have.length(2);
        expect(actual[0].exam).to.equal('direct');
        expect(actual[1].exam).to.equal('shim');
    });

    it('should return `[]` when trial does not exist', function *() {
        const actual = yield UserTrialsReport.apply({ login: 'semenmakhaev' });

        expect(actual).to.deep.equal([]);
    });

    describe('with proctoring', () => {
        const proctoringFields = [
            'isProctoring',
            'proctoringAnswer',
            'isMetricsHigh',
            'isPendingSentToToloka',
            'autoTolokaVerdict',
            'isRevisionRequested',
            'revisionVerdict',
            'appealVerdict',
            'finalVerdict'
        ];
        const proctoringResponseTime = new Date(2018, 3, 1);
        const proConfirmedDate = new Date(2018, 3, 3);
        const proTrialTemplate = { isProctoring: true };
        const user = { login: 'panna-kotta' };

        it('should return proctoring data when trial not passed', function *() {
            const proTrial = { id: 3, passed: 0 };

            yield trialsFactory.createWithRelations(
                proTrial,
                { trialTemplate: proTrialTemplate, user }
            );

            yield proctoringResponsesFactory.createWithRelations(
                { source: 'proctoring', verdict: 'correct' },
                { trial: proTrial }
            );

            const actual = yield UserTrialsReport.apply(user);

            const expected = {
                isProctoring: 1,
                proctoringAnswer: 1,
                isMetricsHigh: 0,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 0
            };

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return correct proctoring data when proctoring answer is `correct`', function *() {
            const proTrial = { id: 23, passed: 1 };

            yield certificatesFactory.createWithRelations({},
                {
                    trial: proTrial,
                    trialTemplate: proTrialTemplate,
                    user
                }
            );

            yield proctoringResponsesFactory.createWithRelations(
                { source: 'proctoring', verdict: 'correct' },
                { trial: proTrial }
            );

            const actual = yield UserTrialsReport.apply(user);

            const expected = {
                isProctoring: 1,
                proctoringAnswer: 1,
                isMetricsHigh: 0,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 1
            };

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return correct proctoring data when proctoring answer is `failed`', function *() {
            const proTrial = { id: 23, passed: 1 };

            yield trialsFactory.createWithRelations(proTrial, { trialTemplate: proTrialTemplate, user });

            yield proctoringResponsesFactory.createWithRelations(
                { source: 'proctoring', verdict: 'failed' },
                { trial: proTrial }
            );

            const actual = yield UserTrialsReport.apply(user);

            const expected = {
                isProctoring: 1,
                proctoringAnswer: 0,
                isMetricsHigh: 0,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 0
            };

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return proctoring data when violation was not confirmed in toloka after `pending`', function *() {
            const proTrial = { id: 23, passed: 1 };

            yield certificatesFactory.createWithRelations({}, {
                trial: proTrial,
                trialTemplate: proTrialTemplate,
                user
            }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    source: 'proctoring',
                    verdict: 'pending',
                    time: proctoringResponseTime,
                    isSentToToloka: true
                },
                { trial: proTrial }
            );

            yield proctoringResponsesFactory.createWithRelations(
                { source: 'toloka', verdict: 'correct', time: proConfirmedDate },
                { trial: proTrial }
            );

            const expected = {
                isProctoring: 1,
                proctoringAnswer: '?',
                isMetricsHigh: 0,
                isPendingSentToToloka: 1,
                autoTolokaVerdict: 1,
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 1
            };

            const actual = yield UserTrialsReport.apply(user);

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return correct proctoring data when violation confirmed in toloka after `pending`', function *() {
            const proTrial = { id: 23, passed: 1 };

            yield trialsFactory.createWithRelations(
                proTrial,
                { trialTemplate: proTrialTemplate, user }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    source: 'proctoring',
                    verdict: 'pending',
                    time: proctoringResponseTime,
                    isSentToToloka: true
                },
                { trial: proTrial }
            );

            yield proctoringResponsesFactory.createWithRelations(
                { source: 'toloka', verdict: 'failed', time: proConfirmedDate },
                { trial: proTrial }
            );

            const expected = {
                isProctoring: 1,
                proctoringAnswer: '?',
                isMetricsHigh: 0,
                isPendingSentToToloka: 1,
                autoTolokaVerdict: 0,
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 0
            };

            const actual = yield UserTrialsReport.apply(user);

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return correct proctoring data when no response from toloka about pending trial', function *() {
            const proTrial = { id: 23, passed: 1 };

            yield trialsFactory.createWithRelations(
                proTrial,
                { trialTemplate: proTrialTemplate, user }
            );

            yield proctoringResponsesFactory.createWithRelations(
                { source: 'proctoring', verdict: 'pending', isSentToToloka: true },
                { trial: proTrial }
            );

            const expected = {
                isProctoring: 1,
                proctoringAnswer: '?',
                isMetricsHigh: 0,
                isPendingSentToToloka: 1,
                autoTolokaVerdict: '?',
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 0
            };

            const actual = yield UserTrialsReport.apply(user);

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return correct proctoring data when metrics from proctoring are high', function *() {
            const proTrial = { id: 23, passed: 1 };

            yield trialsFactory.createWithRelations(
                proTrial,
                { trialTemplate: proTrialTemplate, user }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    source: 'proctoring',
                    verdict: 'pending',
                    isSentToToloka: false,
                    time: new Date(1, 1, 1)
                },
                { trial: proTrial }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    source: 'metrics',
                    verdict: 'failed',
                    isSentToToloka: false,
                    time: new Date(2, 2, 2)
                },
                { trial: proTrial }
            );

            const expected = {
                isProctoring: 1,
                proctoringAnswer: '?',
                isMetricsHigh: 1,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 0
            };

            const actual = yield UserTrialsReport.apply(user);

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return correct proctoring data when crit-metrics was during the attempt', function *() {
            const proTrial = { id: 23, passed: 1 };

            yield trialsFactory.createWithRelations(
                proTrial,
                { trialTemplate: proTrialTemplate, user }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    source: 'proctoring',
                    verdict: 'failed',
                    isSentToToloka: false,
                    time: new Date(1, 1, 1)
                },
                { trial: proTrial }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    source: 'crit-metrics',
                    verdict: 'failed',
                    isSentToToloka: false,
                    time: new Date(2, 2, 2)
                },
                { trial: proTrial }
            );

            const expected = {
                isProctoring: 1,
                proctoringAnswer: 0,
                isMetricsHigh: 1,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 0
            };

            const actual = yield UserTrialsReport.apply(user);

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return correct proctoring data when user request revision', function *() {
            const proTrial = { id: 23, passed: 1 };

            yield trialsFactory.createWithRelations(
                proTrial,
                { trialTemplate: proTrialTemplate, user }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    source: 'proctoring',
                    verdict: 'failed',
                    isSentToToloka: false,
                    isRevisionRequested: true
                },
                { trial: proTrial }
            );

            const expected = {
                isProctoring: 1,
                proctoringAnswer: 0,
                isMetricsHigh: 0,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 1,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 0
            };

            const actual = yield UserTrialsReport.apply(user);

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return correct proctoring data when revision verdict was received', function *() {
            const proTrial = { id: 23, passed: 1 };

            yield trialsFactory.createWithRelations(
                proTrial,
                { trialTemplate: proTrialTemplate, user }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    source: 'proctoring',
                    verdict: 'failed',
                    isSentToToloka: true,
                    isRevisionRequested: true,
                    time: new Date(1, 1, 1)
                },
                { trial: proTrial }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    source: 'toloka-revision',
                    verdict: 'correct',
                    isSentToToloka: false,
                    isRevisionRequested: false,
                    time: new Date(2, 2, 2)
                },
                { trial: proTrial }
            );

            const expected = {
                isProctoring: 1,
                proctoringAnswer: 0,
                isMetricsHigh: 0,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 1,
                revisionVerdict: 1,
                appealVerdict: '?',
                finalVerdict: 1
            };

            const actual = yield UserTrialsReport.apply(user);

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return correct proctoring data when there was appeal', function *() {
            const proTrial = { id: 23, passed: 1 };

            yield trialsFactory.createWithRelations(
                proTrial,
                { trialTemplate: proTrialTemplate, user }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    source: 'proctoring',
                    verdict: 'failed',
                    isSentToToloka: true,
                    isRevisionRequested: true,
                    time: new Date(1, 1, 1)
                },
                { trial: proTrial }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    source: 'toloka-revision',
                    verdict: 'failed',
                    isSentToToloka: false,
                    isRevisionRequested: false,
                    time: new Date(2, 2, 2)
                },
                { trial: proTrial }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    source: 'appeal',
                    verdict: 'correct',
                    isSentToToloka: false,
                    isRevisionRequested: false,
                    time: new Date(3, 3, 3)
                },
                { trial: proTrial }
            );

            const expected = {
                isProctoring: 1,
                proctoringAnswer: 0,
                isMetricsHigh: 0,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 1,
                revisionVerdict: 0,
                appealVerdict: 1,
                finalVerdict: 1
            };

            const actual = yield UserTrialsReport.apply(user);

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });
    });
});
