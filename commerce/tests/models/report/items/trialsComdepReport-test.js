const { expect } = require('chai');
const ip = require('ip');

const TrialsComdepReport = require('models/report/items/trialsComdepReport');

const BBHelper = require('tests/helpers/blackbox');
const nockBlackbox = BBHelper.nockExtSeveralUids;

const dbHelper = require('tests/helpers/clear');

const certificatesFactory = require('tests/factory/certificatesFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');

describe('`Trials Comdep Report`', () => {
    beforeEach(dbHelper.clear);

    afterEach(BBHelper.cleanAll);

    it('should return data for several trials', function *() {
        nockBlackbox({
            uid: '123,456',
            userip: ip.address(),
            response: {
                users: [
                    {
                        uid: { value: 123 },
                        'address-list': [
                            { address: 'email1@yandex.ru' }
                        ]
                    },
                    {
                        uid: { value: 456 },
                        'address-list': [
                            { address: 'email2@yandex.ru' }
                        ]
                    }
                ]
            }
        });

        const trialTemplate = { id: 1, slug: 'direct-pro' };
        const firstTrial = {
            id: 1,
            passed: 1,
            nullified: 0,
            started: new Date(2020, 1, 2),
            expired: 1
        };

        yield certificatesFactory.createWithRelations(
            { id: 1, active: 1 },
            {
                trial: firstTrial,
                trialTemplate,
                user: {
                    id: 5,
                    uid: 123,
                    login: 'first',
                    firstname: 'A',
                    lastname: 'B'
                }
            }
        );

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'correct',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            { trial: firstTrial }
        );

        const secondTrial = {
            id: 2,
            passed: 1,
            nullified: 0,
            started: new Date(2020, 2, 3),
            expired: 1
        };

        yield certificatesFactory.createWithRelations(
            { id: 2, active: 1 },
            {
                trial: secondTrial,
                trialTemplate,
                user: {
                    id: 6,
                    uid: 456,
                    login: 'second',
                    firstname: 'C',
                    lastname: 'D'
                }
            }
        );

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'correct',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            { trial: secondTrial }
        );

        const actual = yield TrialsComdepReport.apply();

        expect(actual).to.deep.equal([
            {
                login: 'first',
                firstname: 'A',
                lastname: 'B',
                email: 'email1@yandex.ru',
                trialId: 1,
                date: '02.02.2020',
                finalVerdict: 'success',
                certId: '1'
            },
            {
                login: 'second',
                firstname: 'C',
                lastname: 'D',
                email: 'email2@yandex.ru',
                trialId: 2,
                date: '03.03.2020',
                finalVerdict: 'success',
                certId: '2'
            }
        ]);
    });

    it('should return `""` in fields when they are empty', function *() {
        nockBlackbox({
            uid: '123',
            userip: ip.address(),
            response: {
                users: [
                    {
                        uid: { value: 123 },
                        'address-list': [
                            { address: 'email@yandex.ru' }
                        ]
                    }
                ]
            }
        });

        const trial = {
            id: 2,
            passed: 1,
            nullified: 0,
            started: new Date(2020, 2, 3),
            expired: 1
        };
        const user = {
            id: 6,
            uid: 123,
            login: 'empty',
            firstname: null,
            lastname: null
        };

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'failed',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            {
                trial,
                user,
                trialTemplate: { id: 2, slug: 'direct-pro' }
            }
        );

        const actual = yield TrialsComdepReport.apply();

        expect(actual).to.deep.equal([
            {
                login: 'empty',
                firstname: '',
                lastname: '',
                email: 'email@yandex.ru',
                trialId: 2,
                date: '03.03.2020',
                finalVerdict: 'failure',
                certId: ''
            }
        ]);
    });

    it('should filter by `from` date', function *() {
        nockBlackbox({
            uid: '123',
            userip: ip.address(),
            response: {
                users: [
                    {
                        uid: { value: 123 },
                        'address-list': [
                            { address: 'email@yandex.ru' }
                        ]
                    }
                ]
            }
        });

        const trialTemplate = { id: 1, slug: 'direct-pro' };
        const trial = {
            id: 1,
            passed: 1,
            nullified: 0,
            started: new Date(2020, 4, 5),
            expired: 1
        };

        yield certificatesFactory.createWithRelations(
            { id: 1, active: 1 },
            {
                trial,
                trialTemplate,
                user: {
                    id: 5,
                    uid: 123,
                    login: 'first',
                    firstname: 'A',
                    lastname: 'B'
                }
            }
        );

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'correct',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            { trial }
        );

        const otherTrial = {
            id: 2,
            passed: 1,
            nullified: 0,
            started: new Date(2019, 2, 3), // <-
            expired: 1
        };

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'failed',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            {
                trial: otherTrial,
                user: { id: 6 },
                trialTemplate
            }
        );

        const actual = yield TrialsComdepReport.apply();

        expect(actual).to.deep.equal([
            {
                login: 'first',
                firstname: 'A',
                lastname: 'B',
                email: 'email@yandex.ru',
                trialId: 1,
                date: '05.05.2020',
                finalVerdict: 'success',
                certId: '1'
            }
        ]);
    });

    it('should filter by `examSlugs`', function *() {
        nockBlackbox({
            uid: '123',
            userip: ip.address(),
            response: {
                users: [
                    {
                        uid: { value: 123 },
                        'address-list': [
                            { address: 'email@yandex.ru' }
                        ]
                    }
                ]
            }
        });

        const trial = {
            id: 1,
            passed: 1,
            nullified: 0,
            started: new Date(2020, 4, 5),
            expired: 1
        };

        yield certificatesFactory.createWithRelations(
            { id: 1, active: 1 },
            {
                trial,
                trialTemplate: { id: 1, slug: 'direct-pro' },
                user: {
                    id: 5,
                    uid: 123,
                    login: 'first',
                    firstname: 'A',
                    lastname: 'B'
                }
            }
        );

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'correct',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            { trial }
        );

        const otherTrial = {
            id: 2,
            passed: 1,
            nullified: 0,
            started: new Date(2029, 2, 3),
            expired: 1
        };

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'failed',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            {
                trial: otherTrial,
                user: { id: 6 },
                trialTemplate: { id: 2, slug: 'market' } // <-
            }
        );

        const actual = yield TrialsComdepReport.apply();

        expect(actual).to.deep.equal([
            {
                login: 'first',
                firstname: 'A',
                lastname: 'B',
                email: 'email@yandex.ru',
                trialId: 1,
                date: '05.05.2020',
                finalVerdict: 'success',
                certId: '1'
            }
        ]);
    });

    it('should return only not nullified trials', function *() {
        nockBlackbox({
            uid: '123',
            userip: ip.address(),
            response: {
                users: [
                    {
                        uid: { value: 123 },
                        'address-list': [
                            { address: 'email@yandex.ru' }
                        ]
                    }
                ]
            }
        });

        const trialTemplate = { id: 1, slug: 'direct-pro' };
        const trial = {
            id: 1,
            passed: 1,
            nullified: 0,
            started: new Date(2020, 4, 5),
            expired: 1
        };

        yield certificatesFactory.createWithRelations(
            { id: 1, active: 1 },
            {
                trial,
                trialTemplate,
                user: {
                    id: 5,
                    uid: 123,
                    login: 'first',
                    firstname: 'A',
                    lastname: 'B'
                }
            }
        );

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'correct',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            { trial }
        );

        const otherTrial = {
            id: 2,
            passed: 1,
            nullified: 1, // <-
            started: new Date(2020, 2, 3),
            expired: 1
        };

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'failed',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            {
                trial: otherTrial,
                user: { id: 6, uid: 456 },
                trialTemplate
            }
        );

        const actual = yield TrialsComdepReport.apply();

        expect(actual).to.deep.equal([
            {
                login: 'first',
                firstname: 'A',
                lastname: 'B',
                email: 'email@yandex.ru',
                trialId: 1,
                date: '05.05.2020',
                finalVerdict: 'success',
                certId: '1'
            }
        ]);
    });

    it('should return only expired trials', function *() {
        nockBlackbox({
            uid: '123',
            userip: ip.address(),
            response: {
                users: [
                    {
                        uid: { value: 123 },
                        'address-list': [
                            { address: 'email@yandex.ru' }
                        ]
                    }
                ]
            }
        });

        const trialTemplate = { id: 1, slug: 'direct-pro' };
        const trial = {
            id: 1,
            passed: 1,
            nullified: 0,
            started: new Date(2020, 4, 5),
            expired: 1
        };

        yield certificatesFactory.createWithRelations(
            { id: 1, active: 1 },
            {
                trial,
                trialTemplate,
                user: {
                    id: 5,
                    uid: 123,
                    login: 'first',
                    firstname: 'A',
                    lastname: 'B'
                }
            }
        );

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'correct',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            { trial }
        );

        const otherTrial = {
            id: 2,
            passed: 1,
            nullified: 0,
            started: new Date(2020, 2, 3),
            expired: 0 // <-
        };

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'failed',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            {
                trial: otherTrial,
                user: { id: 6, uid: 456 },
                trialTemplate
            }
        );

        const actual = yield TrialsComdepReport.apply();

        expect(actual).to.deep.equal([
            {
                login: 'first',
                firstname: 'A',
                lastname: 'B',
                email: 'email@yandex.ru',
                trialId: 1,
                date: '05.05.2020',
                finalVerdict: 'success',
                certId: '1'
            }
        ]);
    });

    it('it should return `[]` when there are no suitable trials', function *() {
        const trial = {
            id: 1,
            passed: 1,
            nullified: 1, // <-
            started: new Date(2020, 2, 3),
            expired: 1
        };

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'failed',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            {
                trial,
                user: { id: 6, uid: 123 },
                trialTemplate: { id: 1, slug: 'direct-pro' }
            }
        );

        const actual = yield TrialsComdepReport.apply();

        expect(actual).to.deep.equal([]);
    });

    describe('`finalVerdict`', () => {
        const user = {
            id: 5,
            uid: 123,
            login: 'test',
            firstname: 'A',
            lastname: 'B'
        };
        const trialTemplate = { id: 1, slug: 'direct-pro' };

        beforeEach(() => {
            nockBlackbox({
                uid: '123',
                userip: ip.address(),
                response: {
                    users: [
                        {
                            uid: { value: 123 },
                            'address-list': [
                                { address: 'email@yandex.ru' }
                            ]
                        }
                    ]
                }
            });
        });

        it('should return `verification` when trial was sent to Toloka', function *() {
            const trial = {
                id: 1,
                passed: 1,
                nullified: 0,
                started: new Date(2020, 2, 3),
                expired: 1
            };

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'failed',
                    isLast: true,
                    isSentToToloka: true, // <-
                    isRevisionRequested: false
                },
                { trial, user, trialTemplate }
            );

            const actual = yield TrialsComdepReport.apply();

            expect(actual).to.deep.equal([
                {
                    login: 'test',
                    firstname: 'A',
                    lastname: 'B',
                    email: 'email@yandex.ru',
                    trialId: 1,
                    date: '03.03.2020',
                    finalVerdict: 'verification',
                    certId: ''
                }
            ]);
        });

        it('should return `verification` when user requested a revision', function *() {
            const trial = {
                id: 1,
                passed: 1,
                nullified: 0,
                started: new Date(2020, 2, 3),
                expired: 1
            };

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'failed',
                    isLast: true,
                    isSentToToloka: false,
                    isRevisionRequested: true // <-
                },
                { trial, user, trialTemplate }
            );

            const actual = yield TrialsComdepReport.apply();

            expect(actual).to.deep.equal([
                {
                    login: 'test',
                    firstname: 'A',
                    lastname: 'B',
                    email: 'email@yandex.ru',
                    trialId: 1,
                    date: '03.03.2020',
                    finalVerdict: 'verification',
                    certId: ''
                }
            ]);
        });

        it('should return `verification` when verdict is pending', function *() {
            const trial = {
                id: 1,
                passed: 1,
                nullified: 0,
                started: new Date(2020, 2, 3),
                expired: 1
            };

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'pending', // <-
                    isLast: true,
                    isSentToToloka: false,
                    isRevisionRequested: false
                },
                { trial, user, trialTemplate }
            );

            const actual = yield TrialsComdepReport.apply();

            expect(actual).to.deep.equal([
                {
                    login: 'test',
                    firstname: 'A',
                    lastname: 'B',
                    email: 'email@yandex.ru',
                    trialId: 1,
                    date: '03.03.2020',
                    finalVerdict: 'verification',
                    certId: ''
                }
            ]);
        });

        it('should return `success` when trial is passed, verdict is correct and cert is active', function *() {
            const trial = {
                id: 1,
                passed: 1, // <-
                nullified: 0,
                started: new Date(2020, 2, 3),
                expired: 1
            };

            yield certificatesFactory.createWithRelations(
                { id: 12345, active: 1 }, // <-
                { trial, trialTemplate, user }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'correct', // <-
                    isLast: true,
                    isSentToToloka: false,
                    isRevisionRequested: false
                },
                { trial, user, trialTemplate }
            );

            const actual = yield TrialsComdepReport.apply();

            expect(actual).to.deep.equal([
                {
                    login: 'test',
                    firstname: 'A',
                    lastname: 'B',
                    email: 'email@yandex.ru',
                    trialId: 1,
                    date: '03.03.2020',
                    finalVerdict: 'success',
                    certId: '12345'
                }
            ]);
        });

        it('should return `failure` when certificate was deactivated', function *() {
            const trial = {
                id: 1,
                passed: 1,
                nullified: 0,
                started: new Date(2020, 4, 5),
                expired: 1
            };

            yield certificatesFactory.createWithRelations(
                { id: 1, active: 0 }, // <-
                { trial, trialTemplate, user }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'correct',
                    isLast: true,
                    isSentToToloka: false,
                    isRevisionRequested: false
                },
                { trial, trialTemplate, user }
            );

            const actual = yield TrialsComdepReport.apply();

            expect(actual).to.deep.equal([
                {
                    login: 'test',
                    firstname: 'A',
                    lastname: 'B',
                    email: 'email@yandex.ru',
                    trialId: 1,
                    date: '05.05.2020',
                    finalVerdict: 'failure',
                    certId: ''
                }
            ]);
        });

        it('should return `failure` when trial is not passed', function *() {
            const trial = {
                id: 1,
                passed: 0, // <-
                nullified: 0,
                started: new Date(2020, 2, 3),
                expired: 1
            };

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'correct',
                    isLast: true,
                    isSentToToloka: false,
                    isRevisionRequested: false
                },
                { trial, user, trialTemplate }
            );

            const actual = yield TrialsComdepReport.apply();

            expect(actual).to.deep.equal([
                {
                    login: 'test',
                    firstname: 'A',
                    lastname: 'B',
                    email: 'email@yandex.ru',
                    trialId: 1,
                    date: '03.03.2020',
                    finalVerdict: 'failure',
                    certId: ''
                }
            ]);
        });

        it('should return `failure` when trial is passed but verdict is failed', function *() {
            const trial = {
                id: 1,
                passed: 1,
                nullified: 0,
                started: new Date(2020, 2, 3),
                expired: 1
            };

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'failed', // <-
                    isLast: true,
                    isSentToToloka: false,
                    isRevisionRequested: false
                },
                { trial, user, trialTemplate }
            );

            const actual = yield TrialsComdepReport.apply();

            expect(actual).to.deep.equal([
                {
                    login: 'test',
                    firstname: 'A',
                    lastname: 'B',
                    email: 'email@yandex.ru',
                    trialId: 1,
                    date: '03.03.2020',
                    finalVerdict: 'failure',
                    certId: ''
                }
            ]);
        });

        it('should return correct `finalVerdict` when there are several responses', function *() {
            const trial = {
                id: 1,
                passed: 1,
                nullified: 0,
                started: new Date(2020, 2, 3),
                expired: 1
            };

            yield certificatesFactory.createWithRelations(
                { id: 123, active: 1 },
                { trial, trialTemplate, user }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'failed',
                    isLast: false,
                    isSentToToloka: true,
                    isRevisionRequested: true
                },
                { trial, user, trialTemplate }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'correct',
                    isLast: true, // <-
                    isSentToToloka: false,
                    isRevisionRequested: false
                },
                { trial, user, trialTemplate }
            );

            const actual = yield TrialsComdepReport.apply();

            expect(actual).to.deep.equal([
                {
                    login: 'test',
                    firstname: 'A',
                    lastname: 'B',
                    email: 'email@yandex.ru',
                    trialId: 1,
                    date: '03.03.2020',
                    finalVerdict: 'success',
                    certId: '123'
                }
            ]);
        });
    });
});
