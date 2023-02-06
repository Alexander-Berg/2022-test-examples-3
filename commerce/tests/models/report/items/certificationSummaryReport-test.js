const { expect } = require('chai');
const moment = require('moment');
const _ = require('lodash');

const CertificationSummaryReport = require('models/report/items/certificationSummaryReport');

const certificatesFactory = require('tests/factory/certificatesFactory');
const trialsFactory = require('tests/factory/trialsFactory');

const dbHelper = require('tests/helpers/clear');

describe('Certification summary report', () => {
    const typeCert = { id: 1, code: 'cert' };

    beforeEach(function *() {
        yield dbHelper.clear();

        // trials with certs (September 2019)
        for (let i = 1; i < 3; i += 1) {
            yield certificatesFactory.createWithRelations({ id: i },
                {
                    type: typeCert,
                    trial: {
                        id: i,
                        started: new Date(2019, 8, 10),
                        passed: 1,
                        nullified: 0
                    }
                });
        }

        // trials with certs (October 2019)
        for (let i = 3; i < 5; i += 1) {
            yield certificatesFactory.createWithRelations({ id: i },
                {
                    type: typeCert,
                    trial: {
                        id: i,
                        started: new Date(2019, 9, 10),
                        passed: 1,
                        nullified: 0
                    }
                });
        }
    });

    it('should return array with zero-reports when there is no suitable trials', function *() {
        const query = {
            from: moment({ year: 2015, month: 3, date: 1 }).toDate(),
            to: moment({ year: 2015, month: 8, date: 30 }).toDate()
        };

        const actual = yield CertificationSummaryReport.apply(query);

        const expected = [
            { period: 'Апрель 2015', periodDate: '2015-04-01' },
            { period: 'Май 2015', periodDate: '2015-05-01' },
            { period: 'Июнь 2015', periodDate: '2015-06-01' },
            { period: 'Июль 2015', periodDate: '2015-07-01' },
            { period: 'Август 2015', periodDate: '2015-08-01' },
            { period: 'Сентябрь 2015', periodDate: '2015-09-01' }
        ]
            .map(data => _.assign({
                certificatesCount: 0,
                successRate: 0,
                trialsCount: 0
            }, { period: data.period, periodDate: data.periodDate }));

        expect(actual).to.deep.equal(expected);
    });

    it('should return correct certification data when suitable trials exists', function *() {
        const query = {
            from: moment({ year: 2019, month: 8, date: 1 }).toDate(),
            to: moment({ year: 2019, month: 8, date: 30 }).toDate()
        };

        const actual = yield CertificationSummaryReport.apply(query);

        const expected = [{
            certificatesCount: 2,
            period: 'Сентябрь 2019',
            periodDate: '2019-09-01',
            successRate: 100,
            trialsCount: 2
        }];

        expect(actual).to.deep.equal(expected);
    });

    it('should add empty reports if there is no trials in the middle/on the boundaries of the interval', function *() {
        const query = {
            from: moment({ year: 2019, month: 7, date: 1 }).toDate(),
            to: moment({ year: 2019, month: 9, date: 30 }).toDate()
        };

        const actual = yield CertificationSummaryReport.apply(query);

        const expected = [{
            certificatesCount: 0,
            period: 'Август 2019',
            periodDate: '2019-08-01',
            successRate: 0,
            trialsCount: 0
        },
        {
            certificatesCount: 2,
            period: 'Сентябрь 2019',
            periodDate: '2019-09-01',
            successRate: 100,
            trialsCount: 2
        },
        {
            certificatesCount: 2,
            period: 'Октябрь 2019',
            periodDate: '2019-10-01',
            successRate: 100,
            trialsCount: 2
        }];

        expect(actual).to.deep.equal(expected);
    });

    it('should correctly count data by different months', function *() {
        yield certificatesFactory.createWithRelations({ id: 100 },
            {
                type: typeCert,
                trial: {
                    id: 100,
                    started: new Date(2019, 10, 10),
                    passed: 1,
                    nullified: 0
                }
            });

        yield trialsFactory.createWithRelations({
            id: 101,
            started: new Date(2019, 10, 21),
            passed: 0,
            nullified: 0
        }, {
            type: typeCert,
            trialTemplate: {
                id: 102,
                slug: `template-102`
            }
        });

        const query = {
            from: moment({ year: 2019, month: 8, date: 1 }).toDate(),
            to: moment({ year: 2019, month: 10, date: 30 }).toDate()
        };

        const actual = yield CertificationSummaryReport.apply(query);

        const expected = [
            {
                certificatesCount: 2,
                period: 'Сентябрь 2019',
                periodDate: '2019-09-01',
                successRate: 100,
                trialsCount: 2
            },
            {
                certificatesCount: 2,
                period: 'Октябрь 2019',
                periodDate: '2019-10-01',
                successRate: 100,
                trialsCount: 2
            },
            {
                certificatesCount: 1,
                period: 'Ноябрь 2019',
                periodDate: '2019-11-01',
                successRate: 50,
                trialsCount: 2
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should not count nullified trials', function *() {
        // trial with certificate, but nullified
        yield certificatesFactory.createWithRelations({ id: 123 },
            {
                trial: {
                    id: 123,
                    started: new Date(2019, 9, 10),
                    passed: 1,
                    nullified: 1
                }
            });

        const query = {
            from: moment({ year: 2019, month: 9, date: 1 }).toDate(),
            to: moment({ year: 2019, month: 9, date: 30 }).toDate()
        };

        const actual = yield CertificationSummaryReport.apply(query);

        const expected = [{
            certificatesCount: 2,
            period: 'Октябрь 2019',
            periodDate: '2019-10-01',
            successRate: 100,
            trialsCount: 2
        }];

        expect(actual).to.deep.equal(expected);
    });

    it('should correctly count data when period has trials without certificates', function *() {
        // trials without certificates
        for (let i = 5; i < 7; i += 1) {
            yield trialsFactory.createWithRelations({ id: i, started: new Date(2019, 9, 21), passed: 0 }, {
                type: typeCert,
                trialTemplate: {
                    id: i,
                    slug: `template-${i}`
                }
            });
        }

        const query = {
            from: moment({ year: 2019, month: 9, date: 1 }).toDate(),
            to: moment({ year: 2019, month: 9, date: 30 }).toDate()
        };

        const actual = yield CertificationSummaryReport.apply(query);

        const expected = [{
            certificatesCount: 2,
            period: 'Октябрь 2019',
            periodDate: '2019-10-01',
            successRate: 50,
            trialsCount: 4
        }];

        expect(actual).to.deep.equal(expected);
    });

    it('should not count achievements or other types of certificates', function *() {
        yield certificatesFactory.createWithRelations({ id: 1056 },
            {
                trial: {
                    id: 1056,
                    started: new Date(2019, 9, 10),
                    passed: 1,
                    nullified: 0
                },
                type: {
                    id: 100500,
                    code: 'achievement'
                }
            });

        const query = {
            from: moment({ year: 2019, month: 9, date: 1 }).toDate(),
            to: moment({ year: 2019, month: 9, date: 30 }).toDate()
        };

        const actual = yield CertificationSummaryReport.apply(query);

        const expected = [{
            certificatesCount: 2,
            period: 'Октябрь 2019',
            periodDate: '2019-10-01',
            successRate: 100,
            trialsCount: 2
        }];

        expect(actual).to.deep.equal(expected);
    });
});
