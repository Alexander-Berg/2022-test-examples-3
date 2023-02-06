const { expect } = require('chai');
const moment = require('moment');
const _ = require('lodash');
const ip = require('ip');

const TrialsReport = require('models/report/items/trialsReport');
const certificatesFactory = require('tests/factory/certificatesFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');

const BBHelper = require('tests/helpers/blackbox');
const nockBlackbox = BBHelper.nockExtSeveralUids;

const dbHelper = require('tests/helpers/clear');
const catchError = require('tests/helpers/catchError').generator;

describe('Trials report', () => {
    const trialTemplate = { id: 1, slug: 'direct', isProctoring: false };
    const user = { id: 2, login: 'zhigalov', uid: 1234, firstname: 'Vasya', lastname: 'Pupkin' };
    const authType = { code: 'web' };
    let now;
    let trial;

    const blackboxResponse = {
        users: [
            {
                uid: { value: 1234 },
                'address-list': [
                    { address: 'email1@yandex.ru' }
                ]
            },
            {
                uid: { value: 5678 },
                'address-list': [
                    { address: 'email2@yandex.ru' }
                ]
            }
        ]
    };

    beforeEach(function *() {
        yield dbHelper.clear();

        now = new Date();
        trial = {
            id: 3,
            passed: 1,
            started: now,
            nullified: 0
        };

        yield certificatesFactory.createWithRelations(
            { id: 12 },
            { user, authType, trialTemplate, trial }
        );
    });

    afterEach(BBHelper.cleanAll);

    it('should pick fields correctly when trial passed', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        const query = {
            from: moment(now).subtract(2, 'hour').toDate(),
            login: 'zhigalov',
            slug: 'direct'
        };
        const actual = yield TrialsReport.apply(query);

        const expected = {
            login: 'zhigalov',
            firstname: 'Vasya',
            lastname: 'Pupkin',
            authType: 'web',
            email: 'email1@yandex.ru',
            trialId: 3,
            date: moment(now).format('DD.MM.YYYY'),
            passed: 1,
            isProctoring: 0,
            proctoringAnswer: '?',
            isMetricsHigh: 0,
            isPendingSentToToloka: 0,
            autoTolokaVerdict: '?',
            isRevisionRequested: 0,
            revisionVerdict: '?',
            appealVerdict: '?',
            finalVerdict: 1,
            certId: 12
        };

        expect(actual.length).to.equal(1);
        expect(actual[0]).to.deep.equal(expected);
    });

    it('should pick fields correctly when trial not passed', function *() {
        nockBlackbox({ uid: 5678, userip: ip.address(), response: blackboxResponse });

        const failedTrial = {
            id: 17,
            passed: 0,
            nullified: 0,
            started: now
        };

        yield trialsFactory.createWithRelations(
            failedTrial,
            {
                user: { login: 'anyok', uid: 5678, firstname: 'Any', lastname: 'Ok' },
                trialTemplate,
                authType
            }
        );

        const query = {
            from: moment(now).subtract(2, 'hour').toDate(),
            login: 'anyok',
            slug: 'direct'
        };
        const actual = yield TrialsReport.apply(query);

        const expected = {
            login: 'anyok',
            firstname: 'Any',
            lastname: 'Ok',
            authType: 'web',
            email: 'email2@yandex.ru',
            trialId: 17,
            date: moment(now).format('DD.MM.YYYY'),
            passed: 0,
            isProctoring: 0,
            proctoringAnswer: '?',
            isMetricsHigh: 0,
            isPendingSentToToloka: 0,
            autoTolokaVerdict: '?',
            isRevisionRequested: 0,
            revisionVerdict: '?',
            appealVerdict: '?',
            finalVerdict: 0,
            certId: ''
        };

        expect(actual.length).to.equal(1);
        expect(actual[0]).to.deep.equal(expected);
    });

    it('should filter by `from` param', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        const oldTrial = {
            id: 2,
            passed: 0,
            started: moment(now).subtract(3, 'year'),
            nullified: 0
        };

        yield trialsFactory.createWithRelations(
            oldTrial,
            { user, authType, trialTemplate }
        );

        const query = {
            from: moment(now).subtract(2, 'hour').toDate(),
            login: 'zhigalov',
            slug: 'direct'
        };
        const actual = yield TrialsReport.apply(query);

        const expected = {
            login: 'zhigalov',
            firstname: 'Vasya',
            lastname: 'Pupkin',
            authType: 'web',
            email: 'email1@yandex.ru',
            trialId: 3,
            date: moment(now).format('DD.MM.YYYY'),
            passed: 1,
            isProctoring: 0,
            proctoringAnswer: '?',
            isMetricsHigh: 0,
            isPendingSentToToloka: 0,
            autoTolokaVerdict: '?',
            isRevisionRequested: 0,
            revisionVerdict: '?',
            appealVerdict: '?',
            finalVerdict: 1,
            certId: 12
        };

        expect(actual.length).to.equal(1);
        expect(actual[0]).to.deep.equal(expected);
    });

    it('should filter by `to` param', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        const oldStarted = moment(now).subtract(3, 'year').toDate();
        const oldTrial = {
            id: 1,
            passed: 0,
            started: oldStarted,
            nullified: 0
        };

        yield trialsFactory.createWithRelations(
            oldTrial,
            { user, authType, trialTemplate }
        );

        const query = {
            from: moment(now).subtract(4, 'year').toDate(),
            to: moment(now).subtract(2, 'year').toDate(),
            login: 'zhigalov',
            slug: 'direct'
        };
        const actual = yield TrialsReport.apply(query);

        const expected = {
            login: 'zhigalov',
            firstname: 'Vasya',
            lastname: 'Pupkin',
            authType: 'web',
            email: 'email1@yandex.ru',
            trialId: 1,
            date: moment(oldStarted).format('DD.MM.YYYY'),
            passed: 0,
            isProctoring: 0,
            proctoringAnswer: '?',
            isMetricsHigh: 0,
            isPendingSentToToloka: 0,
            autoTolokaVerdict: '?',
            isRevisionRequested: 0,
            revisionVerdict: '?',
            appealVerdict: '?',
            finalVerdict: 0,
            certId: ''
        };

        expect(actual.length).to.equal(1);
        expect(actual[0]).to.deep.equal(expected);
    });

    it('should filter by `slug` param', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        const otherTrialTemplate = { id: 13, slug: 'market' };
        const otherTrial = {
            id: 4,
            passed: 0,
            started: moment(now).subtract(1, 'hour').toDate(),
            nullified: 0
        };

        yield trialsFactory.createWithRelations(
            otherTrial,
            { user, authType, trialTemplate: otherTrialTemplate }
        );

        const query = {
            from: moment(now).subtract(1, 'day').toDate(),
            to: now,
            login: 'zhigalov',
            slug: 'market'
        };
        const actual = yield TrialsReport.apply(query);

        const expected = {
            login: 'zhigalov',
            firstname: 'Vasya',
            lastname: 'Pupkin',
            authType: 'web',
            email: 'email1@yandex.ru',
            trialId: 4,
            date: moment(now).format('DD.MM.YYYY'),
            passed: 0,
            isProctoring: 0,
            proctoringAnswer: '?',
            isMetricsHigh: 0,
            isPendingSentToToloka: 0,
            autoTolokaVerdict: '?',
            isRevisionRequested: 0,
            revisionVerdict: '?',
            appealVerdict: '?',
            finalVerdict: 0,
            certId: ''
        };

        expect(actual.length).to.equal(1);
        expect(actual[0]).to.deep.equal(expected);
    });

    it('should find user by aliases of `login`', function *() {
        nockBlackbox({ uid: 5678, userip: ip.address(), response: blackboxResponse });

        const otherUser = { id: 3, login: 'm-smirnov', uid: 5678, firstname: 'Ivan', lastname: 'Ivanov' };
        const otherTrial = {
            id: 13,
            passed: 0,
            started: now,
            nullified: 0
        };

        yield trialsFactory.createWithRelations(
            otherTrial,
            { user: otherUser, authType, trialTemplate }
        );

        const query = {
            from: moment(now).subtract(1, 'day').toDate(),
            login: 'm.smirnov',
            slug: 'direct'
        };
        const actual = yield TrialsReport.apply(query);

        const expected = {
            login: 'm-smirnov',
            firstname: 'Ivan',
            lastname: 'Ivanov',
            authType: 'web',
            email: 'email2@yandex.ru',
            trialId: 13,
            date: moment(now).format('DD.MM.YYYY'),
            passed: 0,
            isProctoring: 0,
            proctoringAnswer: '?',
            isMetricsHigh: 0,
            isPendingSentToToloka: 0,
            autoTolokaVerdict: '?',
            isRevisionRequested: 0,
            revisionVerdict: '?',
            appealVerdict: '?',
            finalVerdict: 0,
            certId: ''
        };

        expect(actual.length).to.equal(1);
        expect(actual[0]).to.deep.equal(expected);
    });

    it('should filter by nullified trial', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        const nullifiedTrial = {
            id: 1,
            passed: 0,
            started: moment(now).subtract(10, 'day'),
            nullified: 1
        };

        yield trialsFactory.createWithRelations(
            nullifiedTrial,
            { user, authType, trialTemplate }
        );

        const query = {
            from: moment(now).subtract(1, 'year').toDate(),
            login: 'zhigalov',
            slug: 'direct'
        };
        const actual = yield TrialsReport.apply(query);

        const expected = {
            login: 'zhigalov',
            firstname: 'Vasya',
            lastname: 'Pupkin',
            authType: 'web',
            email: 'email1@yandex.ru',
            trialId: 3,
            date: moment(now).format('DD.MM.YYYY'),
            passed: 1,
            isProctoring: 0,
            proctoringAnswer: '?',
            isMetricsHigh: 0,
            isPendingSentToToloka: 0,
            autoTolokaVerdict: '?',
            isRevisionRequested: 0,
            revisionVerdict: '?',
            appealVerdict: '?',
            finalVerdict: 1,
            certId: 12
        };

        expect(actual.length).to.equal(1);
        expect(actual[0]).to.deep.equal(expected);
    });

    it('should sort by `login` and `date`', function *() {
        nockBlackbox({ uid: '5678,1234', userip: ip.address(), response: blackboxResponse });

        const otherUser = { id: 3, login: 'm-smirnov', uid: '5678', firstname: 'Ivan', lastname: 'Ivanov' };
        const oldStarted = moment(now).subtract(1, 'month').toDate();

        yield [
            { id: 13, passed: 0, started: oldStarted, nullified: 0 },
            { id: 14, passed: 0, started: now, nullified: 0 }
        ].map(otherTrial => trialsFactory.createWithRelations(
            otherTrial,
            { user: otherUser, authType, trialTemplate }
        ));

        const query = {
            from: moment(now).subtract(1, 'year').toDate(),
            login: ['zhigalov', 'm-smirnov'],
            slug: 'direct'
        };
        const actual = yield TrialsReport.apply(query);

        const expected = [
            {
                login: 'm-smirnov',
                firstname: 'Ivan',
                lastname: 'Ivanov',
                authType: 'web',
                email: 'email2@yandex.ru',
                trialId: 14,
                date: moment(now).format('DD.MM.YYYY'),
                passed: 0,
                isProctoring: 0,
                proctoringAnswer: '?',
                isMetricsHigh: 0,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 0,
                certId: ''
            },
            {
                login: 'm-smirnov',
                firstname: 'Ivan',
                lastname: 'Ivanov',
                authType: 'web',
                email: 'email2@yandex.ru',
                trialId: 13,
                date: moment(oldStarted).format('DD.MM.YYYY'),
                passed: 0,
                isProctoring: 0,
                proctoringAnswer: '?',
                isMetricsHigh: 0,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 0,
                certId: ''
            },
            {
                login: 'zhigalov',
                firstname: 'Vasya',
                lastname: 'Pupkin',
                authType: 'web',
                email: 'email1@yandex.ru',
                trialId: 3,
                date: moment(now).format('DD.MM.YYYY'),
                passed: 1,
                isProctoring: 0,
                proctoringAnswer: '?',
                isMetricsHigh: 0,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 1,
                certId: 12
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should filter by `login` param', function *() {
        nockBlackbox({ uid: 5678, userip: ip.address(), response: blackboxResponse });

        const otherUser = { id: 3, login: 'semenmakhaev', uid: 5678, firstname: 'Semen', lastname: 'Semenych' };
        const trialDate = moment(now).subtract(1, 'hour').toDate();
        const otherTrial = {
            id: 345,
            passed: 0,
            started: trialDate,
            nullified: 0
        };

        yield trialsFactory.createWithRelations(
            otherTrial,
            { user: otherUser, authType, trialTemplate }
        );

        const query = {
            from: moment(now).subtract(2, 'hour').toDate(),
            login: 'semenmakhaev',
            slug: 'direct'
        };
        const actual = yield TrialsReport.apply(query);

        const expected = {
            login: 'semenmakhaev',
            firstname: 'Semen',
            lastname: 'Semenych',
            authType: 'web',
            email: 'email2@yandex.ru',
            trialId: 345,
            date: moment(trialDate).format('DD.MM.YYYY'),
            passed: 0,
            isProctoring: 0,
            proctoringAnswer: '?',
            isMetricsHigh: 0,
            isPendingSentToToloka: 0,
            autoTolokaVerdict: '?',
            isRevisionRequested: 0,
            revisionVerdict: '?',
            appealVerdict: '?',
            finalVerdict: 0,
            certId: ''
        };

        expect(actual.length).to.equal(1);
        expect(actual[0]).to.deep.equal(expected);
    });

    it('should return data for all users when login is not specified', function *() {
        nockBlackbox({ uid: '5678,1234', userip: ip.address(), response: blackboxResponse });

        const otherUser = { id: 3, login: 'semenmakhaev', uid: 5678 };
        const trialDate = moment(now).subtract(1, 'hour').toDate();
        const otherTrial = {
            id: 345,
            passed: 0,
            started: trialDate,
            nullified: 0
        };

        yield trialsFactory.createWithRelations(
            otherTrial,
            { user: otherUser, authType, trialTemplate }
        );

        const query = {
            from: moment(now).subtract(2, 'hour').toDate(),
            slug: 'direct'
        };
        const actual = yield TrialsReport.apply(query);

        expect(actual.length).to.equal(2);
        expect(actual.map(report => report.login)).to.deep.equal(['semenmakhaev', 'zhigalov']);
    });

    // EXPERTDEV-716: Выгрузить email тех, кто пытался, но не получил сертификат
    it('should return correct emails for several users', function *() {
        nockBlackbox({ uid: '5678,1234', userip: ip.address(), response: blackboxResponse });

        const otherUser = { id: 3, login: 'dotokoto', uid: 5678 };
        const trialDate = moment(now).subtract(1, 'hour').toDate();
        const otherTrial = {
            id: 345,
            passed: 0,
            started: trialDate,
            nullified: 0
        };

        yield trialsFactory.createWithRelations(
            otherTrial,
            { user: otherUser, authType, trialTemplate }
        );

        const query = {
            from: moment(now).subtract(2, 'hour').toDate(),
            slug: 'direct'
        };

        const actual = yield TrialsReport.apply(query);

        expect(actual.length).to.equal(2);
        expect(_.map(actual, 'email')).to.deep.equal(['email2@yandex.ru', 'email1@yandex.ru']);
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
        const proTrialTemplate = { slug: 'direct-pro', isProctoring: true };
        const proUser = { login: 'sinseveria', uid: 1234 };
        const proQuery = {
            from: new Date(2018, 1, 1),
            login: 'sinseveria',
            slug: 'direct-pro'
        };

        it('should return proctoring data when trial not passed', function *() {
            nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

            const proTrial = {
                id: 23,
                passed: 0,
                nullified: 0,
                started: new Date(2018, 2, 2)
            };

            yield trialsFactory.createWithRelations(
                proTrial,
                { trialTemplate: proTrialTemplate, user: proUser, authType }
            );

            yield proctoringResponsesFactory.createWithRelations(
                { source: 'proctoring', verdict: 'correct' },
                { trial: proTrial }
            );

            const actual = yield TrialsReport.apply(proQuery);

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
            nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

            const proTrial = {
                id: 23,
                passed: 1,
                nullified: 0,
                started: new Date(2018, 2, 2)
            };

            yield certificatesFactory.createWithRelations({},
                {
                    trial: proTrial,
                    trialTemplate: proTrialTemplate,
                    user: proUser,
                    authType
                }
            );

            yield proctoringResponsesFactory.createWithRelations(
                { source: 'proctoring', verdict: 'correct' },
                { trial: proTrial }
            );

            const actual = yield TrialsReport.apply(proQuery);

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
            nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

            const proTrial = {
                id: 23,
                passed: 1,
                nullified: 0,
                started: new Date(2018, 2, 2)
            };

            yield trialsFactory.createWithRelations(
                proTrial,
                { trialTemplate: proTrialTemplate, user: proUser, authType }
            );

            yield proctoringResponsesFactory.createWithRelations(
                { source: 'proctoring', verdict: 'failed' },
                { trial: proTrial }
            );

            const actual = yield TrialsReport.apply(proQuery);

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

        it('should return correct proctoring data ' +
            'when violation was not confirmed in toloka after `pending`', function *() {
            nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

            const proTrial = {
                id: 23,
                passed: 1,
                nullified: 0,
                started: new Date(2018, 2, 2)
            };

            yield certificatesFactory.createWithRelations({},
                {
                    trial: proTrial,
                    trialTemplate: proTrialTemplate,
                    user: proUser,
                    authType
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

            const actual = yield TrialsReport.apply(proQuery);

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

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return correct proctoring data when violation confirmed in toloka after `pending`', function *() {
            nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

            const proTrial = {
                id: 23,
                passed: 1,
                nullified: 0,
                started: new Date(2018, 2, 2)
            };

            yield trialsFactory.createWithRelations(
                proTrial,
                { trialTemplate: proTrialTemplate, user: proUser, authType }
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

            const actual = yield TrialsReport.apply(proQuery);

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

            expect(actual.length).to.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return proctoring data when trial has not been sent to toloka yet', function *() {
            nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

            const proTrial = {
                id: 23,
                passed: 1,
                nullified: 0,
                started: new Date(2018, 2, 2)
            };

            yield trialsFactory.createWithRelations(
                proTrial,
                { trialTemplate: proTrialTemplate, user: proUser, authType }
            );

            yield proctoringResponsesFactory.createWithRelations(
                { source: 'proctoring', verdict: 'pending', isSentToToloka: false },
                { trial: proTrial }
            );

            const actual = yield TrialsReport.apply(proQuery);

            const expected = {
                isProctoring: 1,
                proctoringAnswer: '?',
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
    });

    it('should throw 400 when slug contains invalid characters', function *() {
        const query = {
            from: moment(now).subtract(1, 'year').toDate(),
            slug: 'inv@lid'
        };
        const error = yield catchError(TrialsReport.apply.bind(TrialsReport, query));

        expect(error.message).to.equal('Exam slug contains invalid characters');
        expect(error.status).to.equal(400);
        expect(error.options).to.deep.equal({
            internalCode: '400_EIC',
            slug: 'inv@lid'
        });
    });

    it('should throw 400 when login contains invalid characters', function *() {
        const query = {
            from: moment(now).subtract(1, 'year').toDate(),
            login: 'inv@lid',
            slug: 'direct'
        };
        const error = yield catchError(TrialsReport.apply.bind(TrialsReport, query));

        expect(error.message).to.equal('Login contains invalid characters');
        expect(error.status).to.equal(400);
        expect(error.options).to.deep.equal({
            internalCode: '400_LIC',
            login: 'inv@lid'
        });
    });
});
