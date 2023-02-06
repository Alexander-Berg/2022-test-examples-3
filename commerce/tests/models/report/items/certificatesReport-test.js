const { expect } = require('chai');

const _ = require('lodash');
const ip = require('ip');

const CertificatesReport = require('models/report/items/certificatesReport');

const certificatesFactory = require('tests/factory/certificatesFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');

const BBHelper = require('tests/helpers/blackbox');
const nockBlackbox = BBHelper.nockExtSeveralUids;

const dbHelper = require('tests/helpers/clear');

describe('Certificates report', () => {
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

    beforeEach(dbHelper.clear);

    afterEach(BBHelper.cleanAll);

    const trialTemplate = {
        id: 1,
        slug: 'direct',
        title: 'Direct test',
        isProctoring: false
    };
    const user = {
        id: 2,
        uid: 1234,
        login: 'anyok'
    };
    const authType = { code: 'web' };

    it('should pick fields', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        const confirmedDate = new Date(2017, 0, 20);

        yield certificatesFactory.createWithRelations(
            { id: 4, confirmedDate, firstname: 'Anna', lastname: 'Bazhenova' },
            { user, trialTemplate, authType, trial: { passed: 1 } }
        );
        const query = { from: new Date(2017, 0, 15).toISOString() };

        const actual = yield CertificatesReport.apply(query);

        const expected = [
            {
                certId: 4,
                confirmedDate,
                authType: 'web',
                examTitle: 'Direct test',
                examSlug: 'direct',
                firstname: 'Anna',
                lastname: 'Bazhenova',
                login: 'anyok',
                email: 'email1@yandex.ru',
                isProctoring: 0,
                proctoringAnswer: '?',
                isMetricsHigh: 0,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 1
            }
        ];

        expect(actual).deep.equal(expected);
    });

    it('should filter by `from` param', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        yield [
            { id: 3, confirmedDate: new Date(2017, 0, 10) },
            { id: 4, confirmedDate: new Date(2017, 0, 20) }
        ].map(cert => certificatesFactory.createWithRelations(cert, { user, trialTemplate }));

        const query = { from: new Date(2017, 0, 15).toISOString() };
        const actual = yield CertificatesReport.apply(query);

        expect(actual).to.have.length(1);
        expect(actual[0].certId).to.equal(4);
    });

    it('should filter by `to` param', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        yield [
            { id: 3, confirmedDate: new Date(2017, 0, 10) },
            { id: 4, confirmedDate: new Date(2017, 0, 20) },
            { id: 5, confirmedDate: new Date(2017, 0, 25) }
        ].map(cert => certificatesFactory.createWithRelations(cert, { user, trialTemplate }));

        const query = {
            from: new Date(2017, 0, 15).toISOString(),
            to: new Date(2017, 0, 21).toISOString()
        };
        const actual = yield CertificatesReport.apply(query);

        expect(actual).to.have.length(1);
        expect(actual[0].certId).to.equal(4);
    });

    it('should filter by single `slug` param', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        yield [
            { slug: 'direct', title: 'Direct test' },
            { slug: 'hello', title: 'Hello test' }
        ].map((template, i) => certificatesFactory.createWithRelations(
            { id: i + 5, confirmedDate: new Date(2017, 0, 20) },
            { user, trialTemplate: template }
        ));

        const query = {
            from: new Date(2017, 0, 15).toISOString(),
            slug: 'direct'
        };
        const actual = yield CertificatesReport.apply(query);

        expect(actual).to.have.length(1);
        expect(actual[0].certId).to.equal(5);
    });

    it('should filter by multiple `slug` params', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        yield [
            { slug: 'direct', title: 'Direct test' },
            { slug: 'hello', title: 'Hello test' },
            { slug: 'shim', title: 'Shim test' }
        ].map((template, i) => certificatesFactory.createWithRelations(
            { id: i + 5, confirmedDate: new Date(2017, 0, i + 20) },
            { user, trialTemplate: template }
        ));

        const query = {
            from: new Date(2017, 0, 15).toISOString(),
            slug: ['direct', 'shim']
        };
        const actual = yield CertificatesReport.apply(query);

        expect(actual).to.have.length(2);
        expect(actual[0].certId).to.equal(5);
        expect(actual[1].certId).to.equal(7);
    });

    it('should filter by nullified trial', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        yield [
            { nullified: 0 },
            { nullified: 1 }
        ].map((trial, i) => certificatesFactory.createWithRelations(
            { id: i + 5, confirmedDate: new Date(2017, 0, 20) },
            { user, trialTemplate, trial }
        ));

        const query = {
            from: new Date(2017, 0, 15).toISOString(),
            slug: ['direct', 'shim']
        };
        const actual = yield CertificatesReport.apply(query);

        expect(actual).to.have.length(1);
        expect(actual[0].certId).to.equal(5);
    });

    it('should sort by `confirmedDate`', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        yield [
            { id: 3, confirmedDate: new Date(2017, 0, 22) },
            { id: 4, confirmedDate: new Date(2017, 0, 20) },
            { id: 5, confirmedDate: new Date(2017, 0, 21) }
        ].map(cert => certificatesFactory.createWithRelations(cert, { user, trialTemplate }));

        const query = { from: new Date(2017, 0, 15).toISOString() };
        const actual = yield CertificatesReport.apply(query);

        expect(actual).to.have.length(3);
        expect(actual[0].certId).to.equal(4);
        expect(actual[1].certId).to.equal(5);
        expect(actual[2].certId).to.equal(3);
    });

    it('should return `[]` when there no certificates', function *() {
        nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

        const query = {
            from: new Date(2017, 0, 15).toISOString(),
            slug: 'direct'
        };
        const actual = yield CertificatesReport.apply(query);

        expect(actual).to.deep.equal([]);
    });

    it('should return correct emails for several users', function *() {
        nockBlackbox({ uid: '1234,5678', userip: ip.address(), response: blackboxResponse });

        const otherUser = { id: 3, uid: 5678 };

        yield certificatesFactory.createWithRelations(
            { id: 4, confirmedDate: new Date(2017, 0, 20) },
            { user, trialTemplate, authType, trial: { passed: 1, nullified: 0 } }
        );
        yield certificatesFactory.createWithRelations(
            { id: 5, confirmedDate: new Date(2017, 0, 21) },
            { user: otherUser, trialTemplate, authType, trial: { passed: 1, nullified: 0 } }
        );

        const query = { from: new Date(2017, 0, 15).toISOString() };

        const actual = yield CertificatesReport.apply(query);

        expect(actual.length).to.equal(2);
        expect(_.map(actual, 'email')).to.deep.equal(['email1@yandex.ru', 'email2@yandex.ru']);
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
        const proDueDate = new Date(2019, 3, 3);
        const proTrialTemplate = { id: 17, title: 'Direct-pro', slug: 'direct-pro', isProctoring: true };
        const proTrial = { id: 23, passed: 1, nullified: 0 };

        const query = {
            from: new Date(2018, 1, 1).toISOString(),
            slug: 'direct-pro'
        };

        it('should return correct proctoring data when violation was not confirmed in toloka', function *() {
            nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

            yield certificatesFactory.createWithRelations(
                {
                    id: 71,
                    dueDate: proDueDate,
                    confirmedDate: proConfirmedDate
                },
                {
                    trial: proTrial,
                    trialTemplate: proTrialTemplate,
                    user,
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

            const actual = yield CertificatesReport.apply(query);

            expect(actual.length).to.be.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });

        it('should return correct proctoring data when proctoring response is `correct`', function *() {
            nockBlackbox({ uid: 1234, userip: ip.address(), response: blackboxResponse });

            yield certificatesFactory.createWithRelations(
                {
                    id: 71,
                    dueDate: proDueDate,
                    confirmedDate: proConfirmedDate
                },
                {
                    trial: proTrial,
                    trialTemplate: proTrialTemplate,
                    user,
                    authType
                }
            );

            yield proctoringResponsesFactory.createWithRelations(
                { source: 'proctoring', verdict: 'correct', time: proctoringResponseTime },
                { trial: proTrial }
            );

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

            const actual = yield CertificatesReport.apply(query);

            expect(actual.length).to.be.equal(1);
            expect(_.pick(actual[0], proctoringFields)).to.deep.equal(expected);
        });
    });
});
