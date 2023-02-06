const { expect } = require('chai');
const _ = require('lodash');

const catchError = require('tests/helpers/catchError').generator;
const CertificatesAggregationReport = require('models/report/items/certificatesAggregationReport');

const certificatesFactory = require('tests/factory/certificatesFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');

describe('Certificates aggregation report', () => {
    beforeEach(require('tests/helpers/clear').clear);

    const trialTemplate = { id: 1, slug: 'direct', isProctoring: false };
    const baseFields = [
        'slug',
        'from',
        'to',
        'total',
        'passed',
        'isProctoring',
        'certificatesCount'
    ];

    it('should throw error when `interval` is invalid', function *() {
        const query = {
            from: new Date().toISOString(),
            to: new Date().toISOString(),
            interval: 'Invalid interval'
        };
        const error = yield catchError(CertificatesAggregationReport.apply.bind(null, query));

        expect(error.message).to.equal('Interval is invalid');
        expect(error.status).to.equal(400);
        expect(error.options).to.deep.equal({
            interval: 'Invalid interval',
            internalCode: '400_III'
        });
    });

    it('should aggregate use `from` date', function *() {
        yield [10, 20].map(day => certificatesFactory.createWithRelations(
            { active: 1 },
            { trialTemplate, trial: { nullified: 0, started: new Date(2017, 0, day), passed: 1 } }
        ));

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };

        const actual = yield CertificatesAggregationReport.apply(query);
        const actualWithBaseFields = actual.map(row => _.pick(row, baseFields));

        const expected = [
            { from, to, slug: 'direct', passed: 1, total: 1, certificatesCount: 1, isProctoring: 0 }
        ];

        expect(actualWithBaseFields).to.deep.equal(expected);
    });

    it('should aggregate use `to` date', function *() {
        yield [10, 20].map(day => certificatesFactory.createWithRelations(
            { active: 1 },
            { trialTemplate, trial: { nullified: 0, started: new Date(2017, 0, day), passed: 1 } }
        ));

        const from = new Date(2017, 0, 5);
        const to = new Date(2017, 0, 15);
        const query = { from: from.toISOString(), to: to.toISOString() };

        const actual = yield CertificatesAggregationReport.apply(query);
        const actualWithBaseFields = actual.map(row => _.pick(row, baseFields));

        const expected = [
            { from, to, slug: 'direct', passed: 1, total: 1, certificatesCount: 1, isProctoring: 0 }
        ];

        expect(actualWithBaseFields).to.deep.equal(expected);
    });

    it('should aggregate by single slug', function *() {
        yield [
            { slug: 'direct' },
            { slug: 'shim' }
        ].map((template, i) => certificatesFactory.createWithRelations(
            { id: i + 1, active: 1 },
            { trialTemplate: template, trial: { started: new Date(2017, 0, 20), nullified: 0, passed: 1 } }
        ));

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString(), slug: 'direct' };
        const actual = yield CertificatesAggregationReport.apply(query);
        const actualWithBaseFields = actual.map(row => _.pick(row, baseFields));

        const expected = [
            { from, to, slug: 'direct', passed: 1, total: 1, certificatesCount: 1, isProctoring: 0 }
        ];

        expect(actualWithBaseFields).to.deep.equal(expected);
    });

    it('should aggregate by several slugs', function *() {
        yield [
            { slug: 'direct' },
            { slug: 'shim' },
            { slug: 'hello' }
        ].map((template, i) => certificatesFactory.createWithRelations(
            { id: i + 1, active: 1 },
            { trialTemplate: template, trial: { started: new Date(2017, 0, 20), nullified: 0, passed: 1 } }
        ));

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString(), slug: ['direct', 'hello'] };
        const actual = yield CertificatesAggregationReport.apply(query);
        const actualWithBaseFields = actual.map(row => _.pick(row, baseFields));

        const expected = [
            { from, to, slug: 'direct', passed: 1, total: 1, certificatesCount: 1, isProctoring: 0 },
            { from, to, slug: 'hello', passed: 1, total: 1, certificatesCount: 1, isProctoring: 0 }
        ];

        expect(actualWithBaseFields).to.deep.equal(expected);
    });

    it('should aggregate by all slugs', function *() {
        yield [
            { id: 1, slug: 'direct' },
            { id: 2, slug: 'shim' },
            { id: 3, slug: 'hello' }
        ].map((template, i) => certificatesFactory.createWithRelations(
            { id: i + 1, active: 1 },
            { trialTemplate: template, trial: { started: new Date(2017, 0, 20), nullified: 0, passed: 1 } }
        ));

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };
        const actual = yield CertificatesAggregationReport.apply(query);
        const actualWithBaseFields = actual.map(row => _.pick(row, baseFields));

        const expected = [
            { from, to, slug: 'direct', passed: 1, total: 1, certificatesCount: 1, isProctoring: 0 },
            { from, to, slug: 'hello', passed: 1, total: 1, certificatesCount: 1, isProctoring: 0 },
            { from, to, slug: 'shim', passed: 1, total: 1, certificatesCount: 1, isProctoring: 0 }
        ];

        expect(actualWithBaseFields).to.deep.equal(expected);
    });

    it('should aggregate total and passed', function *() {
        const started = new Date(2017, 0, 20);

        yield [
            { passed: 1 },
            { passed: 1 }
        ].map(trial => certificatesFactory.createWithRelations(
            { active: 1 },
            { trialTemplate, trial: { started, nullified: 0, passed: trial.passed } }
        ));

        yield trialsFactory.createWithRelations(
            { passed: 0, started, nullified: 0 },
            { trialTemplate }
        );

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };
        const actual = yield CertificatesAggregationReport.apply(query);
        const actualWithBaseFields = actual.map(row => _.pick(row, baseFields));

        const expected = [
            { from, to, slug: 'direct', passed: 2, total: 3, certificatesCount: 2, isProctoring: 0 }
        ];

        expect(actualWithBaseFields).to.deep.equal(expected);
    });

    it('should aggregate by several periods', function *() {
        yield [
            new Date(2016, 0, 10), // Q1
            new Date(2016, 0, 20), // Q1
            new Date(2016, 7, 15), // Q3
            new Date(2016, 10, 1) // Q4
        ].map(started => certificatesFactory.createWithRelations(
            { active: 1 },
            { trialTemplate, trial: { started, nullified: 0, passed: 1 } }
        ));

        const from = new Date(2016, 0, 1);
        const to = new Date(2017, 0, 1);
        const query = { from: from.toISOString(), to: to.toISOString(), interval: '3m' };

        const actual = yield CertificatesAggregationReport.apply(query);
        const actualWithBaseFields = actual.map(row => _.pick(row, baseFields));

        const expected = [
            {
                from: new Date(2016, 0, 1),
                to: new Date(2016, 3, 1),
                slug: 'direct',
                total: 2,
                passed: 2,
                certificatesCount: 2,
                isProctoring: 0
            },
            {
                from: new Date(2016, 3, 1),
                to: new Date(2016, 6, 1),
                slug: 'direct',
                total: 0,
                passed: 0,
                certificatesCount: 0,
                isProctoring: 0
            },
            {
                from: new Date(2016, 6, 1),
                to: new Date(2016, 9, 1),
                slug: 'direct',
                total: 1,
                passed: 1,
                certificatesCount: 1,
                isProctoring: 0
            },
            {
                from: new Date(2016, 9, 1),
                to: new Date(2017, 0, 1),
                slug: 'direct',
                total: 1,
                passed: 1,
                certificatesCount: 1,
                isProctoring: 0
            }
        ];

        expect(actualWithBaseFields).to.deep.equal(expected);
    });

    it('should aggregate passed trials for only active certificates', function *() {
        const started = new Date(2017, 0, 20);

        yield [
            { active: 0 },
            { active: 1 }
        ].map(cert => certificatesFactory.createWithRelations(
            cert,
            { trialTemplate, trial: { started, nullified: 0, passed: 1 } }
        ));

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };
        const actual = yield CertificatesAggregationReport.apply(query);
        const actualWithBaseFields = actual.map(row => _.pick(row, baseFields));

        const expected = [
            { from, to, slug: 'direct', passed: 2, total: 2, certificatesCount: 1, isProctoring: 0 }
        ];

        expect(actualWithBaseFields).to.deep.equal(expected);
    });

    it('should aggregate only not nullified trials', function *() {
        yield [
            { nullified: 1 },
            { nullified: 0 }
        ].map(trial => trialsFactory.createWithRelations(
            { passed: 0, nullified: trial.nullified, started: new Date(2017, 0, 20) },
            { trialTemplate }
        ));

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };
        const actual = yield CertificatesAggregationReport.apply(query);
        const actualWithBaseFields = actual.map(row => _.pick(row, baseFields));

        const expected = [
            { from, to, slug: 'direct', passed: 0, total: 1, certificatesCount: 0, isProctoring: 0 }
        ];

        expect(actualWithBaseFields).to.deep.equal(expected);
    });

    it('should aggregate by several periods with end of month', function *() {
        yield [
            new Date(2017, 0, 10),
            new Date(2017, 1, 20),
            new Date(2017, 2, 15),
            new Date(2017, 3, 5)
        ].map(started => certificatesFactory.createWithRelations(
            { active: 1 },
            { trialTemplate, trial: { started, nullified: 0, passed: 1 } }
        ));

        const from = new Date(2016, 11, 31);
        const to = new Date(2017, 3, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };
        const actual = yield CertificatesAggregationReport.apply(query);
        const actualWithBaseFields = actual.map(row => _.pick(row, baseFields));

        const expected = [
            {
                from: new Date(2016, 11, 31),
                to: new Date(2017, 0, 31),
                slug: 'direct',
                total: 1,
                passed: 1,
                certificatesCount: 1,
                isProctoring: 0
            },
            {
                from: new Date(2017, 0, 31),
                to: new Date(2017, 1, 28),
                slug: 'direct',
                total: 1,
                passed: 1,
                certificatesCount: 1,
                isProctoring: 0
            },
            {
                from: new Date(2017, 1, 28),
                to: new Date(2017, 2, 31),
                slug: 'direct',
                total: 1,
                passed: 1,
                certificatesCount: 1,
                isProctoring: 0
            },
            {
                from: new Date(2017, 2, 31),
                to: new Date(2017, 3, 1),
                slug: 'direct',
                total: 0,
                passed: 0,
                certificatesCount: 0,
                isProctoring: 0
            }
        ];

        expect(actualWithBaseFields).to.deep.equal(expected);
    });

    it('should return `0` in proctoring fields when exam without proctoring', function *() {
        yield certificatesFactory.createWithRelations(
            { id: 123, active: 1 },
            { trialTemplate, trial: { nullified: 0, started: new Date(2018, 1, 15), passed: 1 } }
        );

        const from = new Date(2018, 1, 1);
        const to = new Date(2018, 2, 1);
        const query = { from: from.toISOString(), to: to.toISOString() };

        const actual = yield CertificatesAggregationReport.apply(query);

        const expected = [
            {
                from,
                to,
                slug: 'direct',
                total: 1,
                passed: 1,
                isProctoring: 0,
                correctAnswersCount: 0,
                failedAnswersCount: 0,
                sentToToloka: 0,
                passedWithCorrect: 0,
                passedWithFailed: 0,
                certificatesCount: 1
            }
        ];

        expect(actual).to.deep.equal(expected);
    });

    it('should return `[]` when exam not exist', function *() {
        const from = new Date(2017, 0, 10);
        const to = new Date(2017, 1, 1);
        const query = { from: from.toISOString(), to: to.toISOString(), slug: 'no-exist' };
        const actual = yield CertificatesAggregationReport.apply(query);

        expect(actual).to.deep.equal([]);
    });

    describe('with proctoring', () => {
        const proTrialTemplate = { id: 2, slug: 'direct-pro', isProctoring: true };

        it('should return correct exam data when trial does not exist', function *() {
            yield trialTemplatesFactory.createWithRelations(proTrialTemplate);

            const from = new Date(2017, 0, 10);
            const to = new Date(2017, 1, 1);
            const query = { from: from.toISOString(), to: to.toISOString(), slug: 'direct-pro' };
            const actual = yield CertificatesAggregationReport.apply(query);

            const expected = [
                {
                    from,
                    to,
                    slug: 'direct-pro',
                    total: 0,
                    passed: 0,
                    isProctoring: 1,
                    correctAnswersCount: 0,
                    failedAnswersCount: 0,
                    sentToToloka: 0,
                    passedWithCorrect: 0,
                    passedWithFailed: 0,
                    certificatesCount: 0
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should aggregate `correctAnswersCount` and `passedWithCorrect`', function *() {
            const started = new Date(2017, 0, 20);

            yield [
                { id: 11, passed: 1 },
                { id: 22, passed: 0 }
            ].map(trial => trialsFactory.createWithRelations(
                { id: trial.id, started, nullified: 0, passed: trial.passed },
                { trialTemplate: proTrialTemplate }
            ));

            yield [11, 22].map(id => proctoringResponsesFactory.createWithRelations(
                { source: 'proctoring', verdict: 'correct' },
                { trial: { id } }
            ));

            yield certificatesFactory.createWithRelations({ id: 111, active: 1 }, { trial: { id: 11 } });

            const from = new Date(2017, 0, 10);
            const to = new Date(2017, 1, 1);
            const query = { from: from.toISOString(), to: to.toISOString(), slug: 'direct-pro' };
            const report = yield CertificatesAggregationReport.apply(query);
            const actual = report.map(period => _.pick(period, [
                'correctAnswersCount',
                'passedWithCorrect',
                'certificatesCount'
            ]));

            const expected = [
                { correctAnswersCount: 2, passedWithCorrect: 1, certificatesCount: 1 }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should aggregate `failedAnswersCount` and `passedWithFailed`', function *() {
            const started = new Date(2017, 0, 20);

            yield [
                { id: 11, passed: 1 },
                { id: 22, passed: 0 }
            ].map(trial => trialsFactory.createWithRelations(
                { id: trial.id, started, nullified: 0, passed: trial.passed },
                { trialTemplate: proTrialTemplate }
            ));

            yield [11, 22].map(id => proctoringResponsesFactory.createWithRelations(
                { source: 'proctoring', verdict: 'failed' },
                { trial: { id } }
            ));

            const from = new Date(2017, 0, 10);
            const to = new Date(2017, 1, 1);
            const query = { from: from.toISOString(), to: to.toISOString(), slug: 'direct-pro' };
            const report = yield CertificatesAggregationReport.apply(query);
            const actual = report.map(period => _.pick(period, [
                'failedAnswersCount',
                'passedWithFailed',
                'certificatesCount'
            ]));

            const expected = [
                { failedAnswersCount: 2, passedWithFailed: 1, certificatesCount: 0 }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should aggregate `sentToToloka`', function *() {
            const started = new Date(2017, 0, 20);

            yield [
                { isSentToToloka: true },
                { isSentToToloka: true },
                { isSentToToloka: false }
            ].map(response => proctoringResponsesFactory.createWithRelations(
                { isSentToToloka: response.isSentToToloka },
                { trial: { started, nullified: 0 }, trialTemplate: proTrialTemplate }
            ));

            const from = new Date(2017, 0, 10);
            const to = new Date(2017, 1, 1);
            const query = { from: from.toISOString(), to: to.toISOString(), slug: 'direct-pro' };
            const report = yield CertificatesAggregationReport.apply(query);
            const actual = report.map(period => _.pick(period, ['sentToToloka']));

            const expected = [
                { sentToToloka: 2 }
            ];

            expect(actual).to.deep.equal(expected);
        });
    });
});
