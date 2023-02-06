require('co-mocha');

const _ = require('lodash');

const AttemptQuestion = require('models/attemptQuestion');
const catchError = require('tests/helpers/catchError').generator;
const dbHelper = require('tests/helpers/clear');
const { expect } = require('chai');

const trialToQuestionsFactory = require('tests/factory/trialToQuestionsFactory');
const answersFactory = require('tests/factory/answersFactory');
const questionsFactory = require('tests/factory/questionsFactory');

const { TrialToQuestion } = require('db/postgres');

describe('Attempt question model', () => {
    beforeEach(function *() {
        yield dbHelper.clear();
    });

    describe('`findByNumber`', () => {
        it('should throw 404 when question not exists', function *() {
            const error = yield catchError(AttemptQuestion.findByNumber.bind(AttemptQuestion, '1234', '1234'));

            expect(error.message).to.equal('Attempt question not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_QNF' });
        });

        it('should return question', function *() {
            const question = { id: 2, text: 'question example text' };
            const trial = { id: 7 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 8 },
                { trial, question }
            );

            const actual = yield AttemptQuestion.findByNumber('7', '8');

            expect(actual.get('attemptQuestion.seq')).to.equal(8);
            expect(actual.get('attemptQuestion.question.text')).to.equal('question example text');
        });

        it('should calculate `countAnswered` when question was answered', function *() {
            const trial = { id: 7 };
            const section = { id: 5 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 8, answered: 1 },
                { trial, question: { id: 2 }, section }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 9, answered: 1 },
                { trial, question: { id: 3 }, section }
            );

            const actual = yield AttemptQuestion.findByNumber('7', '8');

            expect(actual.get('countAnswered')).to.equal(2);
        });

        it('should `countAnswered` equal 0 when questions were not answered or skipped', function *() {
            const trial = { id: 7 };
            const section = { id: 5 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 8, answered: 0 },
                { trial, question: { id: 2 }, section }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 7, answered: 2 },
                { trial, question: { id: 3 }, section }
            );

            const actual = yield AttemptQuestion.findByNumber('7', '8');

            expect(actual.get('countAnswered')).to.equal(0);
        });

        it('should return [] in `answers` field when question does not have answers', function *() {
            const question = { id: 3, text: 'other question', version: 2 };
            const trial = { id: 7 };
            const section = { id: 3 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 4 },
                { trial, section, question }
            );

            const actual = yield AttemptQuestion.findByNumber('7', '4');
            const answers = actual.get('attemptQuestion.question.answers');

            expect(answers).to.deep.equal([]);
        });

        it('should not select answers for old question versions', function *() {
            const question = { id: 3, text: 'other question', version: 2 };
            const trial = { id: 7 };
            const section = { id: 14 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 12 },
                { trial, section, question }
            );
            yield answersFactory.createWithRelations(
                { id: 5, active: 0 },
                { question, section }
            );
            yield answersFactory.createWithRelations(
                { id: 6, active: 1 },
                { question, section }
            );

            yield answersFactory.createWithRelations(
                { id: 7, active: 1 },
                { question: { id: 3, version: 1, active: 0 }, section }
            );

            const actual = yield AttemptQuestion.findByNumber('7', '12');
            const answers = _.sortBy(actual.get('attemptQuestion.question.answers'), 'id');

            expect(answers).to.have.length(2);
            expect(answers[0].get('id')).to.equal(5);
            expect(answers[1].get('id')).to.equal(6);
        });
    });

    describe('`findNext`', () => {
        let trial = null;
        let section = null;
        const trialTemplate = { isProctoring: true, timeLimit: 567, title: 'Hello' };

        beforeEach(function *() {
            trial = { id: 2 };
            section = { id: 5 };
            const question = { id: 4 };

            yield trialToQuestionsFactory.createWithRelations(
                { answered: 0, seq: 7 },
                { trial, question, section, trialTemplate }
            );
        });

        it('should return empty answer when question not found', function *() {
            const actual = yield AttemptQuestion.findNext('1234');

            expect(actual.toJSON()).to.deep.equal({});
        });

        it('should filter by `trialId`', function *() {
            yield trialToQuestionsFactory.createWithRelations({}, { trial: { id: 3 }, section });

            const actual = yield AttemptQuestion.findNext('2');

            expect(actual.get('attemptQuestion.trialId')).to.equal(2);
        });

        it('should filter by `answered`', function *() {
            yield trialToQuestionsFactory.createWithRelations(
                { answered: 2, seq: 6 },
                { trial, section }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { answered: 1, seq: 5 },
                { trial, section }
            );

            const actual = yield AttemptQuestion.findNext('2');

            expect(actual.get('attemptQuestion.questionId')).to.equal(4);
        });

        it('should sort by question number', function *() {
            const question = { id: 5 };

            yield trialToQuestionsFactory.createWithRelations(
                { answered: 0, seq: 6 },
                { trial, question, section }
            );

            const actual = yield AttemptQuestion.findNext('2');

            expect(actual.get('attemptQuestion.questionId')).to.equal(5);
        });

        it('should sort by `answered`', function *() {
            const question = { id: 5 };

            yield trialToQuestionsFactory.createWithRelations(
                { answered: 2, seq: 6 },
                { trial, question, section }
            );

            const actual = yield AttemptQuestion.findNext('2');

            expect(actual.get('attemptQuestion.questionId')).to.equal(4);
        });

        it('should calculate `countAnswered` when question was answered', function *() {
            yield trialToQuestionsFactory.createWithRelations(
                { answered: 1, seq: 6 },
                { trial, question: { id: 5 }, section }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { answered: 1, seq: 5 },
                { trial, question: { id: 6 }, section }
            );

            const actual = yield AttemptQuestion.findNext('2');

            expect(actual.get('countAnswered')).to.equal(2);
        });

        it('should `countAnswered` equal 0 when questions were not answered or skipped', function *() {
            yield trialToQuestionsFactory.createWithRelations(
                { answered: 0, seq: 6 },
                { trial, question: { id: 5 }, section }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { answered: 2, seq: 5 },
                { trial, question: { id: 6 }, section }
            );

            const actual = yield AttemptQuestion.findNext('2');

            expect(actual.get('countAnswered')).to.equal(0);
        });

        it('should return [] in `answers` when question does not have answers', function *() {
            const otherTrial = { id: 14 };
            const question = { id: 8 };

            yield trialToQuestionsFactory.createWithRelations(
                { answered: 0, seq: 7 },
                { trial: otherTrial, question, section }
            );

            const actual = yield AttemptQuestion.findNext('14');
            const answers = actual.get('attemptQuestion.question.answers');

            expect(answers).to.deep.equal([]);
        });

        it('should not select answers for old question version', function *() {
            const otherTrial = { id: 14 };
            const question = { id: 8, version: 2 };

            yield trialToQuestionsFactory.createWithRelations(
                { answered: 0, seq: 7 },
                { trial: otherTrial, question, section }
            );
            yield answersFactory.createWithRelations(
                { id: 6, active: 0 },
                { question, section }
            );
            yield answersFactory.createWithRelations(
                { id: 9, active: 1 },
                { question, section }
            );
            yield answersFactory.createWithRelations(
                { id: 10, active: 1 },
                { question: { id: 8, version: 1 }, section }
            );

            const actual = yield AttemptQuestion.findNext('14');
            let answers = actual.get('attemptQuestion.question.answers');

            answers = _.sortBy(answers, 'id');

            expect(answers).to.have.length(2);
            expect(answers[0].get('id')).to.equal(6);
            expect(answers[1].get('id')).to.equal(9);
        });

        it('should return correct question data', function *() {
            const oldSection = { id: 150, code: 'old' };
            const newSection = { id: 186, code: 'new' };
            const oldQuestion = { id: 3540, version: 2, categoryId: 1, text: 'Old text' };
            const actualQuestion = { id: 3540, version: 14, categoryId: 2, text: 'New text' };
            const currentTrial = { id: 3 };

            yield trialToQuestionsFactory.createWithRelations(
                { answered: 0, seq: 1 },
                { trial: currentTrial, question: actualQuestion, section: newSection }
            );

            yield questionsFactory.createWithRelations(
                oldQuestion,
                { section: oldSection }
            );

            const actual = yield AttemptQuestion.findNext('3');

            expect(actual.get('attemptQuestion.question.text')).to.equal('New text');
        });

        it('should return correct exam data', function *() {
            const actual = yield AttemptQuestion.findNext('2');
            const exam = actual.get('attemptQuestion.trial.trialTemplate');

            expect(exam.get('title')).to.equal('Hello');
            expect(exam.get('timeLimit')).to.equal(567);
            expect(exam.get('isProctoring')).to.equal(true);
        });
    });

    describe('`toJSON`', () => {
        it('should return empty object when data not defined', () => {
            const attemptQuestion = new AttemptQuestion();
            const actual = attemptQuestion.toJSON();

            expect(actual).to.deep.equal({});
        });

        it('should pick fields', function *() {
            const trialTemplate = { id: 8, title: 'someTitle', isProctoring: false, timeLimit: 235 };
            const trial = { id: 3, timeLimit: 100000, started: new Date() };
            const question = { id: 2, text: 'question example text', type: 0 };
            const section = { id: 5, code: 'rules', title: 'Price rules' };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 7, answered: 0, type: 0 },
                { trial, question, section, trialTemplate }
            );
            yield answersFactory.createWithRelations(
                { id: 1, text: 'Yes', correct: 1 },
                { question, section }
            );
            yield answersFactory.createWithRelations(
                { id: 2, text: 'No', correct: 0 },
                { question, section }
            );

            const attemptQuestion = yield AttemptQuestion.findByNumber('3', '7');
            const actual = attemptQuestion.toJSON();

            expect(actual.id).to.be.undefined;
            expect(actual.seq).to.equal(7);
            expect(actual.questionId).to.equal(2);
            expect(actual.answered).to.equal('not_answered');
            expect(actual.section).to.deep.equal({ code: 'rules', title: 'Price rules' });
            expect(actual.text).to.equal('question example text');
            expect(actual.type).to.equal('one_answer');

            actual.answers.sort((answer1, answer2) => answer1.id - answer2.id);

            expect(actual.answers).to.have.length(2);
            expect(actual.answers[0].id).to.equal(1);
            expect(actual.answers[0].text).to.equal('Yes');
            expect(actual.answers[1].id).to.equal(2);
            expect(actual.answers[1].text).to.equal('No');

            const actualTrial = actual.trial;

            expect(actualTrial.started).to.be.a('date');
            expect(actualTrial.timeLimit).to.equal(100000);
            expect(actualTrial.questionCount).to.equal(1);
            expect(actualTrial.countAnswered).to.equal(0);

            expect(actual.exam).to.deep.equal({ title: 'someTitle', isProctoring: false, timeLimit: 235 });
        });
    });

    describe('`answer`', () => {
        beforeEach(function *() {
            yield dbHelper.clear();
            const trial = { id: 3, started: new Date(), timeLimit: 90000 };
            const question = { id: 3 };
            const section = { id: 4 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 0, correct: 0 },
                { trial, question, section }
            );
            yield answersFactory.createWithRelations(
                { id: 5, correct: 1 },
                { question, section }
            );
            yield answersFactory.createWithRelations(
                { id: 6, correct: 0 },
                { question, section }
            );
        });

        it('should set `correct` field to `1` if correct answer', function *() {
            const attemptQuestion = yield AttemptQuestion.findByNumber('3', '2');

            yield attemptQuestion.answer(5);
            const model = attemptQuestion._data.attemptQuestion;
            const actual = attemptQuestion.toJSON();

            expect(model.correct).to.equal(1);
            expect(actual.answered).to.equal('answered');
        });

        it('should set `correct` field to `0` if not correct answer', function *() {
            const attemptQuestion = yield AttemptQuestion.findByNumber('3', '2');

            yield attemptQuestion.answer(6);
            const model = attemptQuestion._data.attemptQuestion;
            const actual = attemptQuestion.toJSON();

            expect(model.correct).to.equal(0);
            expect(actual.answered).to.equal('answered');
        });

        it('should set `correct` field to `1` if all answers are correct when multiple answers', function *() {
            const question = { id: 3 };
            const section = { id: 4 };

            yield answersFactory.createWithRelations(
                { id: 7, correct: 1 },
                { question, section }
            );

            const attemptQuestion = yield AttemptQuestion.findByNumber('3', '2');

            yield attemptQuestion.answer([5, 7]);
            const model = attemptQuestion._data.attemptQuestion;
            const actual = attemptQuestion.toJSON();

            expect(model.correct).to.equal(1);
            expect(actual.answered).to.equal('answered');
        });

        it('should set `correct` field to `0` if one answer is incorrect when multiple answers', function *() {
            const attemptQuestion = yield AttemptQuestion.findByNumber('3', '2');

            yield attemptQuestion.answer([5, 6]);
            const model = attemptQuestion._data.attemptQuestion;
            const actual = attemptQuestion.toJSON();

            expect(model.correct).to.equal(0);
            expect(actual.answered).to.equal('answered');
        });

        it('should set `correct` field to `0` if user chose not all correct answers', function *() {
            const question = { id: 3 };
            const section = { id: 4 };

            yield answersFactory.createWithRelations(
                { id: 7, correct: 1 },
                { question, section }
            );
            yield answersFactory.createWithRelations(
                { id: 8, correct: 1 },
                { question, section }
            );
            const attemptQuestion = yield AttemptQuestion.findByNumber('3', '2');

            yield attemptQuestion.answer([5, 7]);
            const model = attemptQuestion._data.attemptQuestion;
            const actual = attemptQuestion.toJSON();

            expect(model.correct).to.equal(0);
            expect(actual.answered).to.equal('answered');
        });

        it('should set `answered` to `skipped` if skip question', function *() {
            const attemptQuestionModel = yield AttemptQuestion.findByNumber('3', '2');
            const attemptQuestion = attemptQuestionModel.get('attemptQuestion');

            expect(attemptQuestion.get('answered')).to.equal(0);

            yield attemptQuestionModel.answer();

            const actual = attemptQuestionModel.toJSON();
            const attemptQuestionData = yield TrialToQuestion.findOne({
                where: { trialId: 3, seq: 2 }
            });

            expect(attemptQuestionData.get('answered')).to.equal(2);
            expect(actual.answered).to.equal('skipped');
        });

        it('should set `answered` to `skipped` if skip question second time', function *() {
            yield TrialToQuestion.update({ answered: 2 }, { where: { trialId: 3, seq: 2 } });
            const attemptQuestionModel = yield AttemptQuestion.findByNumber('3', '2');

            yield attemptQuestionModel.answer();

            const actual = attemptQuestionModel.toJSON();
            const attemptQuestionData = yield TrialToQuestion.findOne({
                where: { trialId: 3, seq: 2 }
            });

            expect(attemptQuestionData.get('answered')).to.equal(3);
            expect(actual.answered).to.equal('skipped');
        });

        it('should success answer to `skipped` question', function *() {
            const attemptQuestion = yield AttemptQuestion.findByNumber('3', '2');

            yield attemptQuestion.answer();
            yield attemptQuestion.answer(5);
            const actual = attemptQuestion.toJSON();

            expect(actual.answered).to.equal('answered');
        });

        it('should return 403 when question is already answered', function *() {
            const attemptQuestion = yield AttemptQuestion.findByNumber('3', '2');
            const model = attemptQuestion._data.attemptQuestion;

            model.answered = 1;
            const error = yield catchError(attemptQuestion.answer.bind(attemptQuestion, 6));

            expect(error.message).to.equal('Question is already answered');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_QAA' });
        });

        it('should return 400 when answer ID is invalid', function *() {
            const attemptQuestion = yield AttemptQuestion.findByNumber('3', '2');
            const error = yield catchError(attemptQuestion.answer.bind(attemptQuestion, 'abc'));

            expect(error.message).to.equal('Answer ID is invalid');
            expect(error.statusCode).to.equal(400);
            expect(error.options).to.deep.equal({ internalCode: '400_ANI' });
        });

        it('should return 400 when one of answers ID is invalid when multiple answers', function *() {
            const attemptQuestion = yield AttemptQuestion.findByNumber('3', '2');
            const error = yield catchError(attemptQuestion.answer.bind(attemptQuestion, [5, 'abc']));

            expect(error.message).to.equal('Answer ID is invalid');
            expect(error.statusCode).to.equal(400);
            expect(error.options).to.deep.equal({ internalCode: '400_ANI' });
        });
    });
});
