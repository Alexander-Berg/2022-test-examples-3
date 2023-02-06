const { expect } = require('chai');

const catchError = require('tests/helpers/catchError').generator;
const BansAggregationReport = require('models/report/items/bansAggregationReport');

const bansFactory = require('tests/factory/bansFactory');
const certificatesFactory = require('tests/factory/certificatesFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');

describe('Bans aggregation report', () => {
    beforeEach(require('tests/helpers/clear').clear);

    it('should throw error when `interval` is invalid', function *() {
        const query = {
            from: new Date().toISOString(),
            to: new Date().toISOString(),
            interval: 'Invalid interval'
        };
        const error = yield catchError(BansAggregationReport.apply.bind(null, query));

        expect(error.message).to.equal('Interval is invalid');
        expect(error.status).to.equal(400);
        expect(error.options).to.deep.equal({
            interval: 'Invalid interval',
            internalCode: '400_III'
        });
    });

    it('should return default data when trials and bans do not exist', function *() {
        yield trialTemplatesFactory.createWithRelations({ slug: 'no-data' });

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };

        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'no-data',
                from,
                to,
                trialsCount: 0,
                certificatesCount: 0,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should aggregate trials use `from` date', function *() {
        const trialTemplate = { id: 1, slug: 'direct' };

        yield [10, 20].map(day => certificatesFactory.createWithRelations(
            { active: 1 },
            {
                trialTemplate,
                trial: { nullified: 0, started: new Date(2017, 0, day) }
            }
        ));

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };

        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'direct',
                from,
                to,
                trialsCount: 1,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should aggregate trials use `to` date', function *() {
        const trialTemplate = { id: 1, slug: 'direct' };

        yield [10, 20].map(day => certificatesFactory.createWithRelations(
            { active: 1 },
            {
                trialTemplate,
                trial: { nullified: 0, started: new Date(2017, 0, day) }
            }
        ));

        const from = new Date(2017, 0, 5);
        const to = new Date(2017, 0, 15);
        const query = { from: from.toISOString(), to: to.toISOString() };

        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'direct',
                from,
                to,
                trialsCount: 1,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should get only not nullified trials', function *() {
        const trialTemplate = { id: 1, slug: 'direct' };
        const started = new Date(2017, 0, 20);

        yield [0, 1].map(nullified => certificatesFactory.createWithRelations(
            { active: 1 },
            {
                trialTemplate,
                trial: { nullified, started }
            }
        ));

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };

        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'direct',
                from,
                to,
                trialsCount: 1,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should aggregate trials without certificates', function *() {
        const trialTemplate = { id: 1, slug: 'direct' };
        const started = new Date(2017, 0, 20);

        yield trialsFactory.createWithRelations({
            nullified: 0,
            started
        }, { trialTemplate });
        yield certificatesFactory.createWithRelations(
            { active: 1 },
            {
                trialTemplate,
                trial: { started, nullified: 0 }
            }
        );

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };

        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'direct',
                from,
                to,
                trialsCount: 2,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should filter trials by exams', function *() {
        const started = new Date(2017, 0, 20);

        yield [
            { id: 1, slug: 'direct' },
            { id: 2, slug: 'market' }
        ].map(trialTemplate => certificatesFactory.createWithRelations(
            { active: 1 },
            {
                trialTemplate,
                trial: { nullified: 0, started }
            }
        ));

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString(), slug: ['market'] };

        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'market',
                from,
                to,
                trialsCount: 1,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should aggregate trials by several slugs', function *() {
        const started = new Date(2017, 0, 20);

        yield [
            { id: 1, slug: 'direct' },
            { id: 2, slug: 'market' }
        ].map(trialTemplate => certificatesFactory.createWithRelations(
            { active: 1 },
            {
                trialTemplate,
                trial: { nullified: 0, started }
            }
        ));

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString(), slug: ['direct', 'market'] };

        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'direct',
                from,
                to,
                trialsCount: 1,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            },
            {
                examSlug: 'market',
                from,
                to,
                trialsCount: 1,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should aggregate trials by several periods', function *() {
        const trialTemplate = { id: 1, slug: 'direct' };

        yield [
            new Date(2016, 0, 10), // Q1
            new Date(2016, 0, 20), // Q1
            new Date(2016, 7, 15), // Q3
            new Date(2016, 10, 1) // Q4
        ].map(started => certificatesFactory.createWithRelations(
            { active: 1 },
            { trialTemplate, trial: { started, nullified: 0 } }
        ));

        const from = new Date(2016, 0, 1);
        const to = new Date(2017, 0, 1);
        const query = { from: from.toISOString(), to: to.toISOString(), interval: '3m' };

        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'direct',
                from: new Date(2016, 0, 1),
                to: new Date(2016, 3, 1),
                trialsCount: 2,
                certificatesCount: 2,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            },
            {
                examSlug: 'direct',
                from: new Date(2016, 3, 1),
                to: new Date(2016, 6, 1),
                trialsCount: 0,
                certificatesCount: 0,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            },
            {
                examSlug: 'direct',
                from: new Date(2016, 6, 1),
                to: new Date(2016, 9, 1),
                trialsCount: 1,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            },
            {
                examSlug: 'direct',
                from: new Date(2016, 9, 1),
                to: new Date(2017, 0, 1),
                trialsCount: 1,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should aggregate by several periods with end of month', function *() {
        const trialTemplate = { id: 1, slug: 'direct' };

        yield [
            new Date(2017, 0, 10),
            new Date(2017, 1, 20),
            new Date(2017, 2, 15),
            new Date(2017, 3, 5)
        ].map(started => certificatesFactory.createWithRelations(
            { active: 1 },
            { trialTemplate, trial: { started, nullified: 0 } }
        ));

        const from = new Date(2016, 11, 31);
        const to = new Date(2017, 3, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };
        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'direct',
                from: new Date(2016, 11, 31),
                to: new Date(2017, 0, 31),
                trialsCount: 1,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            },
            {
                examSlug: 'direct',
                from: new Date(2017, 0, 31),
                to: new Date(2017, 1, 28),
                trialsCount: 1,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            },
            {
                examSlug: 'direct',
                from: new Date(2017, 1, 28),
                to: new Date(2017, 2, 31),
                trialsCount: 1,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            },
            {
                examSlug: 'direct',
                from: new Date(2017, 2, 31),
                to: new Date(2017, 3, 1),
                trialsCount: 0,
                certificatesCount: 0,
                bansCount: 0,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should aggregate nullified certificates', function *() {
        const trialTemplate = { id: 1, slug: 'direct' };

        yield certificatesFactory.createWithRelations(
            { active: 1, deactivateReason: null, deactivateDate: null },
            { trialTemplate, trial: { started: new Date(2017, 0, 17), nullified: 0 } }
        );
        yield certificatesFactory.createWithRelations(
            { active: 0, deactivateReason: 'ban', deactivateDate: new Date(2017, 0, 20) },
            { trialTemplate, trial: { started: new Date(2017, 0, 0), nullified: 0 } }
        );
        yield certificatesFactory.createWithRelations(
            { active: 0, deactivateReason: 'rules', deactivateDate: new Date(2017, 0, 25) },
            { trialTemplate, trial: { started: new Date(2017, 0, 3), nullified: 0 } }
        );

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };
        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'direct',
                from,
                to,
                trialsCount: 1,
                certificatesCount: 1,
                bansCount: 0,
                nullifiedCertsByBansCount: 1,
                nullifiedOtherCertsCount: 1
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should filter nullified certificates by period', function *() {
        const trialTemplate = { id: 1, slug: 'direct' };

        yield certificatesFactory.createWithRelations(
            { active: 0, deactivateReason: 'ban', deactivateDate: new Date(2017, 0, 10) },
            { trialTemplate, trial: { started: new Date(2017, 0, 0), nullified: 0 } }
        );
        yield certificatesFactory.createWithRelations(
            { active: 0, deactivateReason: 'rules', deactivateDate: new Date(2017, 0, 20) },
            { trialTemplate, trial: { started: new Date(2017, 0, 0), nullified: 0 } }
        );

        const from = new Date(2017, 0, 5);
        const to = new Date(2017, 0, 15);
        const query = { from: from.toISOString(), to: to.toISOString() };
        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'direct',
                from,
                to,
                trialsCount: 0,
                certificatesCount: 0,
                bansCount: 0,
                nullifiedCertsByBansCount: 1,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should filter nullified certificates by exams', function *() {
        const deactivateDate = new Date(2017, 0, 10);

        yield certificatesFactory.createWithRelations(
            { active: 0, deactivateReason: 'ban', deactivateDate },
            {
                trialTemplate: { id: 1, slug: 'direct' },
                trial: { started: new Date(2017, 0, 0), nullified: 0 }
            }
        );
        yield certificatesFactory.createWithRelations(
            { active: 0, deactivateReason: 'ban', deactivateDate },
            {
                trialTemplate: { id: 2, slug: 'market' },
                trial: { started: new Date(2017, 0, 0), nullified: 0 }
            }
        );

        const from = new Date(2017, 0, 5);
        const to = new Date(2017, 0, 15);
        const query = { from: from.toISOString(), to: to.toISOString(), slug: 'market' };
        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'market',
                from,
                to,
                trialsCount: 0,
                certificatesCount: 0,
                bansCount: 0,
                nullifiedCertsByBansCount: 1,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should get nullified certificates with not nullified trials', function *() {
        const deactivateDate = new Date(2017, 0, 10);
        const trialTemplate = { id: 1, slug: 'direct' };

        yield certificatesFactory.createWithRelations(
            { active: 0, deactivateReason: 'ban', deactivateDate },
            {
                trialTemplate,
                trial: { started: new Date(2017, 0, 0), nullified: 0 }
            }
        );
        yield certificatesFactory.createWithRelations(
            { active: 0, deactivateReason: 'ban', deactivateDate },
            {
                trialTemplate,
                trial: { started: new Date(2017, 0, 0), nullified: 1 }
            }
        );

        const from = new Date(2017, 0, 5);
        const to = new Date(2017, 0, 15);
        const query = { from: from.toISOString(), to: to.toISOString() };
        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'direct',
                from,
                to,
                trialsCount: 0,
                certificatesCount: 0,
                bansCount: 0,
                nullifiedCertsByBansCount: 1,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should filter bans by exams', function *() {
        const startedDate = new Date(2017, 0, 10);
        const admin = { id: 123 };
        const globalUser = { id: 1 };

        yield bansFactory.createWithRelations({
            action: 'ban',
            startedDate
        }, {
            trialTemplate: { id: 1, slug: 'direct' },
            globalUser,
            admin
        });
        yield bansFactory.createWithRelations({
            action: 'ban',
            startedDate
        }, {
            trialTemplate: { id: 2, slug: 'market' },
            globalUser,
            admin
        });

        const from = new Date(2017, 0, 5);
        const to = new Date(2017, 0, 15);
        const query = { from: from.toISOString(), to: to.toISOString(), slug: 'market' };
        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'market',
                from,
                to,
                trialsCount: 0,
                certificatesCount: 0,
                bansCount: 1,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should filter bans by interval', function *() {
        const admin = { id: 123 };
        const globalUser = { id: 1 };
        const trialTemplate = { id: 1, slug: 'direct' };

        yield bansFactory.createWithRelations({
            action: 'ban',
            startedDate: new Date(2017, 0, 0)
        }, {
            trialTemplate,
            globalUser,
            admin
        });
        yield bansFactory.createWithRelations({
            action: 'ban',
            startedDate: new Date(2017, 0, 10)
        }, {
            trialTemplate,
            globalUser,
            admin
        });

        const from = new Date(2017, 0, 5);
        const to = new Date(2017, 0, 15);
        const query = { from: from.toISOString(), to: to.toISOString() };
        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'direct',
                from,
                to,
                trialsCount: 0,
                certificatesCount: 0,
                bansCount: 1,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should filter bans by action', function *() {
        const admin = { id: 123 };
        const globalUser = { id: 1 };
        const trialTemplate = { id: 1, slug: 'direct' };
        const startedDate = new Date(2017, 0, 10);

        yield bansFactory.createWithRelations({
            action: 'ban',
            startedDate
        }, {
            trialTemplate,
            globalUser,
            admin
        });
        yield bansFactory.createWithRelations({
            action: 'unban',
            startedDate
        }, {
            trialTemplate,
            globalUser,
            admin
        });

        const from = new Date(2017, 0, 5);
        const to = new Date(2017, 0, 15);
        const query = { from: from.toISOString(), to: to.toISOString() };
        const actual = yield BansAggregationReport.apply(query);

        const expected = [
            {
                examSlug: 'direct',
                from,
                to,
                trialsCount: 0,
                certificatesCount: 0,
                bansCount: 1,
                nullifiedCertsByBansCount: 0,
                nullifiedOtherCertsCount: 0
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should return `[]` when exam not exist', function *() {
        const from = new Date(2017, 0, 10);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString(), slug: 'no-exist' };
        const actual = yield BansAggregationReport.apply(query);

        expect(actual).to.deep.equal([]);
    });
});
