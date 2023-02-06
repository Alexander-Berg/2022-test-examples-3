require('co-mocha');

const { expect } = require('chai');

const trialToQuestionsFactory = require('tests/factory/trialToQuestionsFactory');
const { TrialToQuestion } = require('db/postgres');
const dbHelper = require('tests/helpers/clear');

describe('trialToQuestion entity', () => {
    beforeEach(function *() {
        yield dbHelper.clear();
    });

    it('should update single statement', function *() {
        const trial = { id: 2 };
        const section = { id: 5 };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 0 },
            { trial, question: { id: 3 }, section }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 2, answered: 0 },
            { trial, question: { id: 4 }, section }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 0 },
            { trial: { id: 5 }, question: { id: 6 }, section }
        );

        const firstTrialToQuestion = yield TrialToQuestion.findOne({ where: { trialId: 2, seq: 1 } });

        firstTrialToQuestion.set('answered', 1);

        // Reset `where` condition
        yield TrialToQuestion.findAll({ where: { trialId: 2, answered: { $ne: 1 } } });

        // Save with new `where` condition
        yield firstTrialToQuestion.save();

        const actual = yield TrialToQuestion.count({ where: { trialId: 2, answered: { $ne: 1 } } });

        expect(actual).to.equal(1, 'One unanswered question');
    });
});
