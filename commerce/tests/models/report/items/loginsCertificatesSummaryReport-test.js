const { expect } = require('chai');
const moment = require('moment');

const LoginsReport = require('models/report/items/loginsCertificatesSummaryReport');

const certificatesFactory = require('tests/factory/certificatesFactory');

const dbHelper = require('tests/helpers/clear');

describe('Logins report', () => {
    beforeEach(function *() {
        yield dbHelper.clear();
    });

    const type = { id: 23, code: 'cert' };
    const trial = { nullified: 0 };
    const firstService = { id: 1, title: 'Direct' };
    const secondService = { id: 2, title: 'Metrica' };
    const firstUser = { id: 11, yandexUid: 1111 };
    const secondUser = { id: 22, yandexUid: 2222 };

    it('should group data by periods', function *() {
        yield certificatesFactory.createWithRelations({
            id: 2,
            confirmedDate: new Date(2019, 4, 13),
            active: 1
        }, { type, trial, service: firstService, user: firstUser });
        yield certificatesFactory.createWithRelations({
            id: 3,
            confirmedDate: new Date(2019, 4, 15),
            active: 1
        }, { type, trial, service: secondService, user: firstUser });
        yield certificatesFactory.createWithRelations({
            id: 4,
            confirmedDate: new Date(2019, 4, 23),
            active: 1
        }, { type, trial, service: secondService, user: secondUser });
        yield certificatesFactory.createWithRelations({
            id: 5,
            confirmedDate: new Date(2019, 5, 5),
            active: 1
        }, { type, trial, service: firstService, user: firstUser });

        const query = {
            from: new Date(2019, 4, 10).toISOString(),
            to: new Date(2019, 5, 10).toISOString()
        };

        const actual = yield LoginsReport.apply(query);

        expect(actual).to.deep.equal([
            {
                period: 'Май 2019',
                periodDate: '2019-05-01',
                loginsWithCerts: 2,
                loginsWithSeveralCerts: 1,
                multiCertsRate: 50,
                popularCertsCombination: 'Direct + Metrica'
            },
            {
                period: 'Июнь 2019',
                periodDate: '2019-06-01',
                loginsWithCerts: 1,
                loginsWithSeveralCerts: 0,
                multiCertsRate: 0,
                popularCertsCombination: ''
            }
        ]);
    });

    it('should should filter data by interval', function *() {
        yield certificatesFactory.createWithRelations({
            id: 2,
            confirmedDate: new Date(2019, 3, 13),
            active: 1
        }, { type, trial, service: firstService, user: firstUser });
        yield certificatesFactory.createWithRelations({
            id: 3,
            confirmedDate: new Date(2019, 4, 15),
            active: 1
        }, { type, trial, service: secondService, user: firstUser });
        yield certificatesFactory.createWithRelations({
            id: 4,
            confirmedDate: new Date(2019, 5, 23),
            active: 1
        }, { type, trial, service: secondService, user: secondUser });

        const query = {
            from: new Date(2019, 4, 10).toISOString(),
            to: new Date(2019, 4, 20).toISOString()
        };

        const actual = yield LoginsReport.apply(query);

        expect(actual).to.deep.equal([
            {
                period: 'Май 2019',
                periodDate: '2019-05-01',
                loginsWithCerts: 1,
                loginsWithSeveralCerts: 0,
                multiCertsRate: 0,
                popularCertsCombination: ''
            }
        ]);
    });

    it('should return correct popular combinations', function *() {
        const thirdUser = { id: 13, yandexUid: 3333 };
        const thirdService = { id: 3, title: 'Market' };
        const confirmedDate = new Date(2019, 4, 13);

        // first user has three certs: Direct, Metrica, Market
        yield certificatesFactory.createWithRelations({
            id: 1,
            confirmedDate,
            active: 1
        }, { type, trial, service: firstService, user: firstUser });
        yield certificatesFactory.createWithRelations({
            id: 2,
            confirmedDate,
            active: 1
        }, { type, trial, service: secondService, user: firstUser });
        yield certificatesFactory.createWithRelations({
            id: 3,
            confirmedDate,
            active: 1
        }, { type, trial, service: thirdService, user: firstUser });

        // second user has two certs: Direct, Market
        yield certificatesFactory.createWithRelations({
            id: 4,
            confirmedDate,
            active: 1
        }, { type, trial, service: firstService, user: secondUser });
        yield certificatesFactory.createWithRelations({
            id: 5,
            confirmedDate,
            active: 1
        }, { type, trial, service: thirdService, user: secondUser });

        // third user has one cert: Metrica
        yield certificatesFactory.createWithRelations({
            id: 6,
            confirmedDate,
            active: 1
        }, { type, trial, service: secondService, user: thirdUser });

        const query = {
            from: moment({ year: 2019, month: 4, date: 10 }).toDate(),
            to: moment({ year: 2019, month: 4, date: 20 }).toDate()
        };

        const actual = yield LoginsReport.apply(query);

        expect(actual).to.deep.equal([{
            period: 'Май 2019',
            periodDate: '2019-05-01',
            loginsWithCerts: 3,
            loginsWithSeveralCerts: 2,
            multiCertsRate: 67,
            popularCertsCombination: 'Direct + Market'
        }]);
    });

    it('should add empty reports if there is no trials in the middle/on the boundaries of the interval', function *() {
        yield certificatesFactory.createWithRelations({
            id: 2,
            confirmedDate: new Date(2019, 4, 13),
            active: 1
        }, { type, trial, service: firstService, user: firstUser });
        yield certificatesFactory.createWithRelations({
            id: 3,
            confirmedDate: new Date(2019, 6, 15),
            active: 1
        }, { type, trial, service: secondService, user: firstUser });

        const query = {
            from: new Date(2019, 3, 10).toISOString(),
            to: new Date(2019, 6, 25).toISOString()
        };

        const actual = yield LoginsReport.apply(query);

        expect(actual).to.deep.equal([
            {
                period: 'Апрель 2019',
                periodDate: '2019-04-01',
                loginsWithCerts: 0,
                loginsWithSeveralCerts: 0,
                multiCertsRate: 0,
                popularCertsCombination: ''
            },
            {
                period: 'Май 2019',
                periodDate: '2019-05-01',
                loginsWithCerts: 1,
                loginsWithSeveralCerts: 0,
                multiCertsRate: 0,
                popularCertsCombination: ''
            },
            {
                period: 'Июнь 2019',
                periodDate: '2019-06-01',
                loginsWithCerts: 0,
                loginsWithSeveralCerts: 0,
                multiCertsRate: 0,
                popularCertsCombination: ''
            },
            {
                period: 'Июль 2019',
                periodDate: '2019-07-01',
                loginsWithCerts: 1,
                loginsWithSeveralCerts: 0,
                multiCertsRate: 0,
                popularCertsCombination: ''
            }
        ]);
    });

    it('should return empty reports when there are no certificates', function *() {
        const query = {
            from: new Date(2019, 4, 10).toISOString(),
            to: new Date(2019, 5, 20).toISOString()
        };

        const actual = yield LoginsReport.apply(query);

        expect(actual).to.deep.equal([
            {
                period: 'Май 2019',
                periodDate: '2019-05-01',
                loginsWithCerts: 0,
                loginsWithSeveralCerts: 0,
                multiCertsRate: 0,
                popularCertsCombination: ''
            },
            {
                period: 'Июнь 2019',
                periodDate: '2019-06-01',
                loginsWithCerts: 0,
                loginsWithSeveralCerts: 0,
                multiCertsRate: 0,
                popularCertsCombination: ''
            }
        ]);
    });

    it('should not count nullified trials', function *() {
        yield certificatesFactory.createWithRelations({
            id: 2,
            confirmedDate: new Date(2019, 4, 13),
            active: 1
        }, { type, trial, service: firstService, user: firstUser });
        yield certificatesFactory.createWithRelations({
            id: 3,
            confirmedDate: new Date(2019, 4, 15),
            active: 1
        }, {
            type,
            trial: { nullified: 1 },
            service: secondService,
            user: firstUser
        });

        const query = {
            from: new Date(2019, 4, 10).toISOString(),
            to: new Date(2019, 4, 20).toISOString()
        };

        const actual = yield LoginsReport.apply(query);

        expect(actual).to.deep.equal([
            {
                period: 'Май 2019',
                periodDate: '2019-05-01',
                loginsWithCerts: 1,
                loginsWithSeveralCerts: 0,
                multiCertsRate: 0,
                popularCertsCombination: ''
            }
        ]);
    });

    it('should not count achievements or other types of certificates ', function *() {
        yield certificatesFactory.createWithRelations({
            id: 2,
            confirmedDate: new Date(2019, 4, 13),
            active: 1
        }, { type, trial, service: firstService, user: firstUser });
        yield certificatesFactory.createWithRelations({
            id: 3,
            confirmedDate: new Date(2019, 4, 15),
            active: 1
        }, {
            type: { id: 45, code: 'achievement' },
            trial,
            service: secondService,
            user: firstUser
        });

        const query = {
            from: new Date(2019, 4, 10).toISOString(),
            to: new Date(2019, 4, 20).toISOString()
        };

        const actual = yield LoginsReport.apply(query);

        expect(actual).to.deep.equal([
            {
                period: 'Май 2019',
                periodDate: '2019-05-01',
                loginsWithCerts: 1,
                loginsWithSeveralCerts: 0,
                multiCertsRate: 0,
                popularCertsCombination: ''
            }
        ]);
    });

    it('should not count users which does not have yandexUid', function *() {
        yield certificatesFactory.createWithRelations({
            id: 2,
            confirmedDate: new Date(2019, 4, 13),
            active: 1
        }, { type, trial, service: firstService, user: { id: 33, yandexUid: null } });

        const query = {
            from: new Date(2019, 4, 10).toISOString(),
            to: new Date(2019, 4, 20).toISOString()
        };

        const actual = yield LoginsReport.apply(query);

        expect(actual).to.deep.equal([
            {
                period: 'Май 2019',
                periodDate: '2019-05-01',
                loginsWithCerts: 0,
                loginsWithSeveralCerts: 0,
                multiCertsRate: 0,
                popularCertsCombination: ''
            }
        ]);
    });
});
