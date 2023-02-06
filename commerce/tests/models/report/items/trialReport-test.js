const { expect } = require('chai');

const TrialReport = require('models/report/items/trialReport');

const catchError = require('tests/helpers/catchError').generator;
const trialToQuestionsFactory = require('tests/factory/trialToQuestionsFactory');
const questionsFactory = require('tests/factory/questionsFactory');

describe('Trial report model', () => {
    const trialTemplate = {
        id: 13,
        slug: 'test',
        title: 'Test exam'
    };
    const user = { id: 7, login: 'm-smirnov' };

    beforeEach(require('tests/helpers/clear').clear);

    it('should throw error when trialId is not a number', function *() {
        const query = { trialId: 'abc' };
        const error = yield catchError(TrialReport.apply.bind(null, query, 'analyst'));

        expect(error.message).to.equal('Trial id not a number');
        expect(error.status).to.equal(400);
        expect(error.options).to.deep.equal({
            internalCode: '400_TNN',
            trialId: 'abc'
        });
    });

    it('should pick fields for analyst', function *() {
        const question = { id: 2, version: 1, text: 'question text' };
        const trial = { id: 3 };
        const section = { id: 3, title: 'Magic' };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            { question, trial, trialTemplate, user, section }
        );

        const actual = yield TrialReport.apply({ trialId: 3 }, 'analyst');
        const expected = {
            login: 'm-smirnov',
            exam: 'Test exam',
            section: 'Magic',
            seq: 1,
            answered: 1,
            correct: 1,
            text: 'question text'
        };

        expect(actual).to.deep.equal([expected]);
    });

    // EXPERTDEV-1093: Отчет "Получение данных о попытке" для асессора
    it('should pick fields for assessor', function *() {
        const question = { id: 2, version: 1, text: 'question text' };
        const trial = { id: 3 };
        const section = { id: 3, title: 'Magic' };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            { question, trial, trialTemplate, user, section }
        );

        const actual = yield TrialReport.apply({ trialId: 3 }, 'assessor');
        const expected = {
            login: 'm-smirnov',
            exam: 'Test exam',
            section: 'Magic',
            seq: 1,
            answered: 1,
            correct: 1
        };

        expect(actual).to.deep.equal([expected]);
    });

    it('should filter by trial', function *() {
        const question = { text: 'question text' };
        const section = { id: 1 };
        const category = { id: 1 };

        yield [
            { id: 1 },
            { id: 2 }
        ].map((trial, i) => trialToQuestionsFactory.createWithRelations(
            { id: i + 1, seq: i + 10 },
            { trial, question, section, category, trialTemplate, user }
        ));

        const query = { trialId: 2 };
        const actual = yield TrialReport.apply(query, 'analyst');

        expect(actual).to.have.length(1);
        expect(actual[0].seq).to.equal(11);
    });

    it('should order by seq', function *() {
        const trial = { id: 1 };
        const section = { id: 1 };
        const category = { id: 1 };

        yield [
            { id: 3, text: '3' },
            { id: 2, text: '2' },
            { id: 1, text: '1' }
        ].map((question, i) => trialToQuestionsFactory.createWithRelations(
            { seq: i + 1 },
            { trial, question, section, category, trialTemplate, user }
        ));

        const actual = yield TrialReport.apply({ trialId: 1 }, 'analyst');

        expect(actual).to.have.length(3);
        expect(actual[0].seq).to.equal(1);
        expect(actual[0].text).to.equal('3');
        expect(actual[1].seq).to.equal(2);
        expect(actual[1].text).to.equal('2');
        expect(actual[2].seq).to.equal(3);
        expect(actual[2].text).to.equal('1');
    });

    it('should return correct version of question', function *() {
        const trial = { id: 7 };
        const section = { id: 3 };
        const question = { id: 1, version: 1, text: 'one' };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            { trial, trialTemplate, user, section, question }
        );
        yield questionsFactory.createWithRelations(
            { id: 1, version: 2, text: 'two' },
            { section }
        );

        const actual = yield TrialReport.apply({ trialId: 7 }, 'analyst');

        expect(actual.length).to.equal(1);
        expect(actual[0].text).to.equal('one');
    });
});
