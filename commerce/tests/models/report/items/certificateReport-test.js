const { expect } = require('chai');
const _ = require('lodash');

const CertificateReport = require('models/report/items/certificateReport');

const certificatesFactory = require('tests/factory/certificatesFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');
const catchError = require('tests/helpers/catchError').generator;

describe('Certificate report model', () => {
    describe('`canApply`', () => {
        it('should return `true` when type is `certificate`', () => {
            const actual = CertificateReport.canApply('certificate');

            expect(actual).to.be.true;
        });

        it('should return `false` when type is not `certificate`', () => {
            const actual = CertificateReport.canApply('user');

            expect(actual).to.be.false;
        });
    });

    describe('`apply`', () => {
        beforeEach(require('tests/helpers/clear').clear);

        const dueDate = new Date(2017, 0, 1);
        const confirmedDate = new Date(2016, 6, 1);

        const trial = { passed: 1 };
        const user = { login: 'm-smirnov' };
        const trialTemplate = { id: 2, title: 'Direct', isProctoring: false };
        const role = { title: 'User' };
        const agency = { login: 'm.smirnov', manager: 'mokhov' };
        const authType = { code: 'web' };
        const expected = {
            firstname: 'Mike',
            lastname: 'Smirnov',
            login: 'm-smirnov',
            role: 'User',
            authType: 'web',
            examId: 2,
            examTitle: 'Direct',
            certId: 1,
            confirmedDate,
            dueDate,
            agencyLogin: 'm.smirnov',
            agencyManager: 'mokhov',
            isDeactivated: 0,
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

        it('should return data', function *() {
            yield certificatesFactory.createWithRelations(
                {
                    id: 1,
                    dueDate,
                    confirmedDate,
                    firstname: 'Mike',
                    lastname: 'Smirnov',
                    active: 1
                },
                { trial, user, trialTemplate, role, agency, authType }
            );

            const actual = yield CertificateReport.apply({ certId: 1 });

            expect(actual).to.deep.equal(expected);
        });

        it('should return data without agency', function *() {
            const userWithoutAgency = {
                login: 'm-smirnov',
                agencyId: null
            };

            yield certificatesFactory.createWithRelations(
                {
                    id: 1,
                    dueDate,
                    confirmedDate,
                    firstname: 'Mike',
                    lastname: 'Smirnov',
                    active: 1
                },
                { trial, user: userWithoutAgency, trialTemplate, role, authType }
            );

            const actual = yield CertificateReport.apply({ certId: 1 });
            const expectedWithoutAgency = _.assign({}, expected, {
                agencyLogin: '',
                agencyManager: ''
            });

            expect(actual).to.deep.equal(expectedWithoutAgency);
        });

        it('should return data about deactivate certificate', function *() {
            const deactivateDate = new Date(2018, 7, 1);
            const deactivateReason = 'ban';

            yield certificatesFactory.createWithRelations(
                {
                    id: 1,
                    dueDate,
                    confirmedDate,
                    firstname: 'Mike',
                    lastname: 'Smirnov',
                    active: 0,
                    deactivateDate,
                    deactivateReason
                },
                { trial, user, trialTemplate, role, agency, authType }
            );

            const deactivatedCert = _.assign({}, expected, {
                isDeactivated: 1,
                deactivateDate,
                deactivateReason
            });

            const actual = yield CertificateReport.apply({ certId: 1 });

            expect(actual).to.deep.equal(deactivatedCert);
        });

        it('should throw 400 when `certId` not number', function *() {
            const error = yield catchError(CertificateReport.apply.bind(null, { certId: 'not number' }));

            expect(error.message).to.equal('CertId is invalid');
            expect(error.status).to.equal(400);
            expect(error.options).to.deep.equal({
                internalCode: '400_CII',
                certId: 'not number'
            });
        });

        it('should throw 404 when certificate not found', function *() {
            const cb = CertificateReport.apply.bind(CertificateReport, { certId: 1 });
            const error = yield catchError(cb);

            expect(error.message).to.equal('Certificate not found');
            expect(error.status).to.equal(404);
            expect(error.options).to.deep.equal({
                internalCode: '404_CNF',
                certId: 1
            });
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
            const proTrialTemplate = { id: 17, title: 'Direct-pro', isProctoring: true };
            const proTrial = { id: 23, passed: 1 };

            it('should return correct proctoring data when violation was not confirmed in toloka', function *() {
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
                        role,
                        agency,
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

                const otherExpected = {
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

                const actual = yield CertificateReport.apply({ certId: 71 });

                expect(_.pick(actual, proctoringFields)).to.deep.equal(otherExpected);
            });

            it('should return correct proctoring data when proctoring response is `correct`', function *() {
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
                        role,
                        agency,
                        authType
                    }
                );

                yield proctoringResponsesFactory.createWithRelations(
                    {
                        source: 'proctoring',
                        verdict: 'correct',
                        time: proctoringResponseTime,
                        isSentToToloka: false
                    },
                    { trial: proTrial }
                );

                const otherExpected = {
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

                const actual = yield CertificateReport.apply({ certId: 71 });

                expect(_.pick(actual, proctoringFields)).to.deep.equal(otherExpected);
            });
        });
    });
});
