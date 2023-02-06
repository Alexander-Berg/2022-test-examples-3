const { expect } = require('chai');
const moment = require('moment');
const dbHelper = require('tests/helpers/clear');

const QuestionsReport = require('models/report/items/questionsReport');

const catchError = require('tests/helpers/catchError').generator;
const trialToQuestionsFactory = require('tests/factory/trialToQuestionsFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');

describe('Questions report model', () => {
    const trialTemplate = {
        id: 13,
        slug: 'test'
    };

    beforeEach(dbHelper.clear);

    it('should pick fields', function *() {
        const started = new Date(2017, 1, 7);
        const question = { id: 1, version: 1, text: 'question text', type: 0, active: 1 };

        yield trialsFactory.createWithRelations({ id: 4, started }, { trialTemplate });

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            { question, trial: { id: 4 }, trialTemplate }
        );

        const from = moment(started).subtract(1, 'd').toDate();
        const to = moment(started).add(1, 'd').toDate();

        const actual = yield QuestionsReport.apply({ slug: 'test', from, to });

        const expected = {
            examSlug: 'test',
            questionId: 1,
            type: 'один',
            text: 'question text',
            askedCount: 1,
            correctCount: 1
        };

        expect(actual).to.deep.equal([expected]);
    });

    it('should filter by trial template', function *() {
        const started = new Date(2017, 1, 7);

        yield trialsFactory.createWithRelations({ id: 1, started }, { trialTemplate });

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            {
                question: { id: 1, active: 1 },
                trial: { id: 1 },
                trialTemplate,
                section: { id: 1, code: '1' }
            }
        );

        const otherTrialTemplate = {
            id: 14,
            slug: 'not_test'
        };

        yield trialsFactory.createWithRelations({ id: 2, started }, { trialTemplate: otherTrialTemplate });

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            {
                question: { id: 2, active: 1 },
                trial: { id: 2 },
                trialTemplate: otherTrialTemplate,
                section: { id: 2, code: '2' }
            }
        );

        const from = moment(started).subtract(1, 'd').toDate();
        const to = moment(started).add(1, 'd').toDate();

        const actual = yield QuestionsReport.apply({ slug: 'test', from, to });

        expect(actual).to.have.length(1);
        expect(actual[0].examSlug).to.equal('test');
        expect(actual[0].questionId).to.equal(1);
    });

    it('should filter by `from` param', function *() {
        const started = new Date(2017, 1, 7);
        const secondStarted = new Date(2016, 2, 7);
        const question = { id: 1, active: 1 };
        const section = { id: 1, code: '1' };

        yield trialsFactory.createWithRelations({ id: 1, started }, { trialTemplate });

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 0 },
            { question, trial: { id: 1 }, trialTemplate, section }
        );

        yield trialsFactory.createWithRelations({ id: 2, started: secondStarted }, { trialTemplate });

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 2, answered: 1, correct: 1 },
            { question, trial: { id: 2 }, trialTemplate, section }
        );

        const from = moment(started).subtract(1, 'd').toDate();

        const actual = yield QuestionsReport.apply({ slug: 'test', from });

        expect(actual).to.have.length(1);
        expect(actual[0].questionId).to.equal(1);
        expect(actual[0].askedCount).to.equal(1);
        expect(actual[0].correctCount).to.equal(0);
    });

    it('should filter by `to` param', function *() {
        const started = new Date(2017, 1, 7);
        const secondStarted = new Date(2017, 2, 7);
        const question = { id: 1, active: 1 };
        const section = { id: 1, code: '1' };

        yield trialsFactory.createWithRelations({ id: 1, started }, { trialTemplate });

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 0 },
            { question, trial: { id: 1 }, trialTemplate, section }
        );

        yield trialsFactory.createWithRelations({ id: 2, started: secondStarted }, { trialTemplate });

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 2, answered: 1, correct: 1 },
            { question, trial: { id: 2 }, trialTemplate, section }
        );

        const from = moment(started).subtract(1, 'd').toDate();
        const to = moment(started).add(1, 'd').toDate();

        const actual = yield QuestionsReport.apply({ slug: 'test', from, to });

        expect(actual).to.have.length(1);
        expect(actual[0].questionId).to.equal(1);
        expect(actual[0].askedCount).to.equal(1);
        expect(actual[0].correctCount).to.equal(0);
    });

    it('should select only active questions', function *() {
        const started = new Date(2017, 1, 7);
        const question = { id: 1, version: 1, text: 'question text', type: 0, active: 1 };
        const secondQuestion = { id: 2, active: 0, text: 'not active question' };
        const section = { id: 1, code: '1' };
        const trial = { id: 1, started };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 0 },
            { question, trial, trialTemplate, section }
        );

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 2, answered: 1, correct: 1 },
            { question: secondQuestion, trial, trialTemplate, section }
        );

        const from = moment(started).subtract(1, 'd').toDate();
        const to = moment(started).add(1, 'd').toDate();

        const actual = yield QuestionsReport.apply({ slug: 'test', from, to });

        expect(actual).to.have.length(1);
        expect(actual[0].questionId).to.equal(1);
        expect(actual[0].text).to.equal('question text');
    });

    it('should select only answered or skipped questions', function *() {
        const started = new Date(2017, 1, 7);
        const question = { id: 1, version: 1, text: 'question text', type: 0, active: 1 };
        const section = { id: 1, code: '1' };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            { question, trial: { id: 1, started }, trialTemplate, section }
        );

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 2, answered: 0, correct: 0 },
            { question, trial: { id: 2, started }, trialTemplate, section }
        );

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 3, answered: 3, correct: 0 },
            { question, trial: { id: 3, started }, trialTemplate, section }
        );

        const from = moment(started).subtract(1, 'd').toDate();
        const to = moment(started).add(1, 'd').toDate();

        const actual = yield QuestionsReport.apply({ slug: 'test', from, to });

        expect(actual).to.have.length(1);
        expect(actual[0].questionId).to.equal(1);
        expect(actual[0].askedCount).to.equal(2);
        expect(actual[0].correctCount).to.equal(1);
    });

    it('should count `askedCount` and `correctCount` for multiple trials and questions', function *() {
        const started = new Date(2017, 1, 7);
        let question = { id: 1, version: 1, text: 'first question text', type: 0, active: 1 };
        const section = { id: 1, code: '1' };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 0 },
            { question, trial: { id: 1, started }, trialTemplate, section }
        );

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 0 },
            { question, trial: { id: 2, started }, trialTemplate, section }
        );

        question = { id: 2, version: 4, text: 'second question text', type: 1, active: 1 };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 2, answered: 1, correct: 1 },
            { question, trial: { id: 1, started }, trialTemplate, section }
        );

        question = { id: 3, version: 1, text: 'third question text', type: 1, active: 1 };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 3, answered: 1, correct: 1 },
            { question, trial: { id: 1, started }, trialTemplate, section }
        );

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 3, answered: 1, correct: 0 },
            { question, trial: { id: 2, started }, trialTemplate, section }
        );

        const from = moment(started).subtract(1, 'd').toDate();
        const to = moment(started).add(1, 'd').toDate();

        const actual = yield QuestionsReport.apply({ slug: 'test', from, to });

        expect(actual).to.have.length(3);

        expect(actual[0].questionId).to.equal(1);
        expect(actual[0].askedCount).to.equal(2);
        expect(actual[0].correctCount).to.equal(0);

        expect(actual[1].questionId).to.equal(2);
        expect(actual[1].askedCount).to.equal(1);
        expect(actual[1].correctCount).to.equal(1);

        expect(actual[2].questionId).to.equal(3);
        expect(actual[2].askedCount).to.equal(2);
        expect(actual[2].correctCount).to.equal(1);
    });

    it('should throw 404 when trialTemplate not found', function *() {
        const started = new Date(2017, 1, 7);

        const from = moment(started).subtract(1, 'd').toDate();
        const to = moment(started).add(1, 'd').toDate();

        const cb = QuestionsReport.apply.bind(QuestionsReport, { slug: 'not-exist', from, to });
        const error = yield catchError(cb);

        expect(error.message).to.equal('Test not found');
        expect(error.status).to.equal(404);
        expect(error.options).to.deep.equal({
            internalCode: '404_TNF',
            slug: 'not-exist'
        });
    });

    it('should return empty array when trials not found', function *() {
        yield trialTemplatesFactory.createWithRelations({
            slug: 'test'
        }, {});

        const actual = yield QuestionsReport.apply({ slug: 'test', from: new Date() });

        expect(actual).to.deep.equal([]);
    });
});
