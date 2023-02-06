const { expect } = require('chai');
const moment = require('moment');

const CertificatesDetailedReport = require('models/report/items/certificatesDetailedReport');

const certificatesFactory = require('tests/factory/certificatesFactory');
const servicesFactory = require('tests/factory/servicesFactory');

const dbHelper = require('tests/helpers/clear');

describe('Certificates detailed report', () => {
    const directProServiceData = {
        id: 1,
        code: 'direct_pro'
    };

    const metrikaServiceData = {
        id: 2,
        code: 'metrika'
    };

    const type = { id: 1, code: 'cert' };

    beforeEach(function *() {
        yield dbHelper.clear();

        yield servicesFactory.create(metrikaServiceData);

        yield certificatesFactory.createWithRelations({
            id: 1
        }, {
            trial: {
                id: 1,
                passed: 1,
                started: moment({ year: 2019, month: 8, date: 2 }).toDate(),
                nullified: 0
            },
            type,
            service: directProServiceData
        });
    });

    it('should return array with zero-reports when there is no suitable trials', function *() {
        const query = {
            from: moment({ year: 2015, month: 3, date: 1 }).toDate(),
            to: moment({ year: 2015, month: 4, date: 1 }).toDate()
        };

        const actual = yield CertificatesDetailedReport.apply(query);

        const expected = [
            {
                directProCertificatesCount: 0,
                directProSuccessRate: 0,
                directProTrialsCount: 0,
                metrikaCertificatesCount: 0,
                metrikaSuccessRate: 0,
                metrikaTrialsCount: 0,
                period: 'Апрель 2015',
                periodDate: '2015-04-01'
            },
            {
                directProCertificatesCount: 0,
                directProSuccessRate: 0,
                directProTrialsCount: 0,
                metrikaCertificatesCount: 0,
                metrikaSuccessRate: 0,
                metrikaTrialsCount: 0,
                period: 'Май 2015',
                periodDate: '2015-05-01'
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should return correct certification data when suitable trials exists', function *() {
        const query = {
            from: moment({ year: 2019, month: 8, date: 1 }).toDate(),
            to: moment({ year: 2019, month: 8, date: 19 }).toDate()
        };

        const actual = yield CertificatesDetailedReport.apply(query);

        const expected = [
            {
                directProCertificatesCount: 1,
                directProSuccessRate: 100,
                directProTrialsCount: 1,
                metrikaCertificatesCount: 0,
                metrikaSuccessRate: 0,
                metrikaTrialsCount: 0,
                period: 'Сентябрь 2019',
                periodDate: '2019-09-01'
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should add empty reports if there is no trials in the middle/on the boundaries of the interval', function *() {
        const query = {
            from: moment({ year: 2019, month: 7, date: 1 }).toDate(),
            to: moment({ year: 2019, month: 8, date: 19 }).toDate()
        };

        const actual = yield CertificatesDetailedReport.apply(query);

        const expected = [
            {
                directProCertificatesCount: 1,
                directProSuccessRate: 100,
                directProTrialsCount: 1,
                metrikaCertificatesCount: 0,
                metrikaSuccessRate: 0,
                metrikaTrialsCount: 0,
                period: 'Сентябрь 2019',
                periodDate: '2019-09-01'
            },
            {
                directProCertificatesCount: 0,
                directProSuccessRate: 0,
                directProTrialsCount: 0,
                metrikaCertificatesCount: 0,
                metrikaSuccessRate: 0,
                metrikaTrialsCount: 0,
                period: 'Август 2019',
                periodDate: '2019-08-01'
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should correctly count data by different months', function *() {
        const query = {
            from: moment({ year: 2019, month: 8, date: 1 }).toDate(),
            to: moment({ year: 2019, month: 9, date: 30 }).toDate()
        };

        yield certificatesFactory.createWithRelations({
            id: 2
        }, {
            trial: {
                id: 2,
                passed: 1,
                started: moment({ year: 2019, month: 9, date: 2 }).toDate(),
                nullified: 0
            },
            type,
            service: directProServiceData
        });

        const actual = yield CertificatesDetailedReport.apply(query);

        const expected = [
            {
                directProCertificatesCount: 1,
                directProSuccessRate: 100,
                directProTrialsCount: 1,
                metrikaCertificatesCount: 0,
                metrikaSuccessRate: 0,
                metrikaTrialsCount: 0,
                period: 'Сентябрь 2019',
                periodDate: '2019-09-01'
            },
            {
                directProCertificatesCount: 1,
                directProSuccessRate: 100,
                directProTrialsCount: 1,
                metrikaCertificatesCount: 0,
                metrikaSuccessRate: 0,
                metrikaTrialsCount: 0,
                period: 'Октябрь 2019',
                periodDate: '2019-10-01'
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should not count nullified trials', function *() {
        const query = {
            from: moment({ year: 2019, month: 9, date: 1 }).toDate(),
            to: moment({ year: 2019, month: 9, date: 30 }).toDate()
        };

        yield certificatesFactory.createWithRelations({
            id: 2
        }, {
            trial: {
                id: 2,
                passed: 1,
                started: moment({ year: 2019, month: 9, date: 2 }).toDate(),
                nullified: 1
            },
            type,
            service: directProServiceData
        });

        const actual = yield CertificatesDetailedReport.apply(query);

        const expected = [
            {
                directProCertificatesCount: 0,
                directProSuccessRate: 0,
                directProTrialsCount: 0,
                metrikaCertificatesCount: 0,
                metrikaSuccessRate: 0,
                metrikaTrialsCount: 0,
                period: 'Октябрь 2019',
                periodDate: '2019-10-01'
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should not count data for other services', function *() {
        const query = {
            from: moment({ year: 2019, month: 8, date: 1 }).toDate(),
            to: moment({ year: 2019, month: 8, date: 30 }).toDate()
        };

        yield certificatesFactory.createWithRelations({
            id: 2
        }, {
            trial: {
                id: 2,
                passed: 1,
                started: moment({ year: 2019, month: 8, date: 2 }).toDate(),
                nullified: 0
            },
            type,
            service: { id: 3, code: 'market' }
        });

        const actual = yield CertificatesDetailedReport.apply(query);

        const expected = [
            {
                directProCertificatesCount: 1,
                directProSuccessRate: 100,
                directProTrialsCount: 1,
                metrikaCertificatesCount: 0,
                metrikaSuccessRate: 0,
                metrikaTrialsCount: 0,
                period: 'Сентябрь 2019',
                periodDate: '2019-09-01'
            }
        ];

        expect(actual).to.deep.equal(expected);
    });
});
