const { expect } = require('chai');
const moment = require('moment');
const ip = require('ip');

const certificatesFactory = require('tests/factory/certificatesFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');
const trialsFactory = require('tests/factory/trialsFactory');

const dbHelper = require('tests/helpers/clear');
const { generator: catchError } = require('tests/helpers/catchError');

const BBHelper = require('tests/helpers/blackbox');
const nockBlackbox = BBHelper.nockExtSeveralUids;

const Report = require('models/report');

describe('Report test', () => {
    beforeEach(dbHelper.clear);

    describe('`getReportData`', () => {
        it('should apply `certificate` report', function *() {
            yield certificatesFactory.createWithRelations({ id: 1 });

            const report = new Report('certificate', { certId: 1 });
            const { data } = yield report.getReportData(['analyst']);

            expect(data.certId).to.equal(1);
        });

        it('should return correct blankName', function *() {
            yield trialsFactory.createWithRelations({ id: 1 });

            const report = new Report('trial', { trialId: 1 });
            const { blankName } = yield report.getReportData(['developer', 'assessor']);

            expect(blankName).to.equal('trialAssessor');
        });

        it('should throw 400 for unknown type', function *() {
            const report = new Report('unknown');
            const error = yield catchError(report.getReportData.bind(report));

            expect(error.message).to.equal('No reporter for type');
            expect(error.status).to.equal(400);
            expect(error.options).to.deep.equal({
                internalCode: '400_NRF',
                type: 'unknown'
            });
        });

        it('should throw 403 when user has no access to report', function *() {
            const report = new Report('questions');
            const error = yield catchError(report.getReportData.bind(report, ['assessor']));

            expect(error.message).to.equal('User has no access to report');
            expect(error.status).to.equal(403);
            expect(error.options).to.deep.equal({
                internalCode: '403_HNA'
            });
        });
    });

    describe('`getReports`', () => {
        it('should return reports for analyst', () => {
            const actual = Report.getReports(['analyst']);
            const expected = [
                {
                    type: 'certificate',
                    description: 'Получение данных по номеру сертификата',
                    fields: [{ name: 'certId', type: 'number', required: true }]
                },
                {
                    type: 'certificates',
                    description: 'Получение всех сертификатов за указанный период',
                    fields: [
                        { name: 'from', type: 'date-from', required: true },
                        { name: 'to', type: 'date-to', required: false },
                        { name: 'slug', type: 'array', required: true }
                    ]
                },
                {
                    type: 'certificatesAggregation',
                    description: 'Сбор статистических данных по сертификатам за указанный период',
                    fields: [
                        { name: 'from', type: 'date-from', required: true },
                        { name: 'to', type: 'date-to', required: false },
                        { name: 'interval', type: 'select', required: false },
                        { name: 'slug', type: 'array', required: true }
                    ]
                },
                {
                    type: 'userTrials',
                    description: 'Получение попыток пользователя по логину',
                    fields: [{ name: 'login', type: 'string', required: true }]
                },
                {
                    type: 'trial',
                    description: 'Получение данных о попытке',
                    fields: [{ name: 'trialId', type: 'number', required: true }]
                },
                {
                    type: 'trials',
                    description: 'Получение данных о попытках за указанный период',
                    fields: [
                        { name: 'from', type: 'date-from', required: true },
                        { name: 'to', type: 'date-to', required: false },
                        { name: 'slug', type: 'select', required: true },
                        { name: 'login', type: 'array', required: false }
                    ]
                },
                {
                    type: 'questions',
                    description: 'Получение статистики по ответам на вопросы',
                    fields: [
                        { name: 'slug', type: 'string', required: true },
                        { name: 'from', type: 'date-from', required: true },
                        { name: 'to', type: 'date-to', required: false }
                    ]
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return reports for assessor', () => {
            const actual = Report.getReports(['assessor']);
            const expected = [
                {
                    type: 'certificate',
                    description: 'Получение данных по номеру сертификата',
                    fields: [{ name: 'certId', type: 'number', required: true }]
                },
                {
                    type: 'userTrials',
                    description: 'Получение попыток пользователя по логину',
                    fields: [{ name: 'login', type: 'string', required: true }]
                },
                {
                    type: 'trial',
                    description: 'Получение данных о попытке',
                    fields: [{ name: 'trialId', type: 'number', required: true }]
                },
                {
                    type: 'bansByLogin',
                    description: 'Получение банов пользователя по логину',
                    fields: [{ name: 'login', type: 'string', required: true }]
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return `[]` when user has no access to reports', () => {
            const actual = Report.getReports(['mere-mortal']);

            expect(actual).to.deep.equal([]);
        });
    });

    describe('`getStatReportsData`', () => {
        it('should return data for stat reports', function *() {
            const type = { id: 3, code: 'cert' };
            const authType = { id: 2, code: 'web' };

            yield trialsFactory.createWithRelations({
                id: 12,
                nullified: 0,
                started: new Date(2019, 5, 10)
            }, {
                trialTemplate: { id: 2, slug: 'metrika' },
                service: { id: 1, code: 'metrika' },
                type,
                user: { id: 11, uid: 123, yandexUid: 1111 },
                authType
            });

            yield certificatesFactory.createWithRelations({
                id: 44,
                active: 1,
                confirmedDate: new Date(2019, 5, 23, 7)
            }, {
                trial: {
                    id: 13,
                    nullified: 0,
                    started: new Date(2019, 5, 23)
                },
                trialTemplate: { id: 3, slug: 'direct-pro' },
                service: { id: 2, code: 'direct_pro' },
                type,
                user: { id: 22, uid: 456, yandexUid: 2222 },
                authType
            });

            const date = new Date(2019, 5, 17);
            const from = moment(date).startOf('month').toDate();
            const to = moment(date).endOf('month').toDate();

            const actual = yield Report.getStatReportsData(from, to);

            expect(actual).to.deep.equal({
                certificatesDetailed: [
                    {
                        period: 'Июнь 2019',
                        periodDate: '2019-06-01',
                        directProCertificatesCount: 1,
                        directProSuccessRate: 100,
                        directProTrialsCount: 1,
                        metrikaCertificatesCount: 0,
                        metrikaSuccessRate: 0,
                        metrikaTrialsCount: 1
                    }
                ],
                certificationSummary: [
                    {
                        period: 'Июнь 2019',
                        periodDate: '2019-06-01',
                        trialsCount: 2,
                        certificatesCount: 1,
                        successRate: 50
                    }
                ],
                loginsCertificatesSummary: [
                    {
                        period: 'Июнь 2019',
                        periodDate: '2019-06-01',
                        loginsWithCerts: 1,
                        loginsWithSeveralCerts: 0,
                        multiCertsRate: 0,
                        popularCertsCombination: ''
                    }
                ]
            });
        });
    });

    describe('`getComdepReportData`', () => {
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

        afterEach(BBHelper.cleanAll);

        it('should get report data and split it into chunks', function *() {
            const trialTemplate = { id: 1, slug: 'direct-pro' };
            const user = {
                id: 1,
                uid: 123,
                login: 'test',
                firstname: 'A',
                lastname: 'B'
            };

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'correct',
                    isLast: true,
                    isSentToToloka: false,
                    isRevisionRequested: false
                },
                {
                    trial: {
                        id: 1,
                        passed: 0,
                        nullified: 0,
                        started: new Date(2020, 0, 1),
                        expired: 1
                    },
                    trialTemplate,
                    user
                }
            );

            const successTrial = {
                id: 2,
                passed: 1,
                nullified: 0,
                started: new Date(2020, 1, 2),
                expired: 1
            };

            yield certificatesFactory.createWithRelations(
                { id: 35, active: 1 },
                {
                    trial: successTrial,
                    trialTemplate,
                    user
                }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'correct',
                    isLast: true,
                    isSentToToloka: false,
                    isRevisionRequested: false
                },
                {
                    trial: successTrial,
                    trialTemplate,
                    user
                }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'pending',
                    isLast: true,
                    isSentToToloka: false,
                    isRevisionRequested: false
                },
                {
                    trial: {
                        id: 3,
                        passed: 1,
                        nullified: 0,
                        started: new Date(2020, 2, 3),
                        expired: 1
                    },
                    trialTemplate,
                    user
                }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'failed',
                    isLast: true,
                    isSentToToloka: false,
                    isRevisionRequested: true
                },
                {
                    trial: {
                        id: 4,
                        passed: 1,
                        nullified: 0,
                        started: new Date(2020, 3, 4),
                        expired: 1
                    },
                    trialTemplate,
                    user
                }
            );

            yield proctoringResponsesFactory.createWithRelations(
                {
                    verdict: 'failed',
                    isLast: true,
                    isSentToToloka: false,
                    isRevisionRequested: false
                },
                {
                    trial: {
                        id: 5,
                        passed: 0,
                        nullified: 0,
                        started: new Date(2020, 4, 5),
                        expired: 1
                    },
                    trialTemplate,
                    user
                }
            );

            const actual = yield Report.getComdepReportData();

            expect(actual).to.deep.equal([
                [
                    {
                        login: 'test',
                        firstname: 'A',
                        lastname: 'B',
                        email: 'email@yandex.ru',
                        trialId: 1,
                        date: '01.01.2020',
                        finalVerdict: 'failure',
                        certId: ''
                    },
                    {
                        login: 'test',
                        firstname: 'A',
                        lastname: 'B',
                        email: 'email@yandex.ru',
                        trialId: 2,
                        date: '02.02.2020',
                        finalVerdict: 'success',
                        certId: '35'
                    }
                ],
                [
                    {
                        login: 'test',
                        firstname: 'A',
                        lastname: 'B',
                        email: 'email@yandex.ru',
                        trialId: 3,
                        date: '03.03.2020',
                        finalVerdict: 'verification',
                        certId: ''
                    },
                    {
                        login: 'test',
                        firstname: 'A',
                        lastname: 'B',
                        email: 'email@yandex.ru',
                        trialId: 4,
                        date: '04.04.2020',
                        finalVerdict: 'verification',
                        certId: ''
                    }
                ],
                [
                    {
                        login: 'test',
                        firstname: 'A',
                        lastname: 'B',
                        email: 'email@yandex.ru',
                        trialId: 5,
                        date: '05.05.2020',
                        finalVerdict: 'failure',
                        certId: ''
                    }
                ]
            ]);
        });
    });
});
