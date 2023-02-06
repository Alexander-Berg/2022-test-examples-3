require('co-mocha');

const { expect } = require('chai');

const dbHelper = require('tests/helpers/clear');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const answersFactory = require('tests/factory/answersFactory');
const questionsFactory = require('tests/factory/questionsFactory');
const trialTemplateAllowedFailsFactory = require('tests/factory/trialTemplateAllowedFailsFactory');
const trialTemplateToSectionsFactory = require('tests/factory/trialTemplateToSectionsFactory');

const Loader = require('models/loader');
const db = require('db/postgres');
const catchError = require('tests/helpers/catchError');
const _ = require('lodash');

describe('Loader model', () => {
    describe('`upsert`', () => {
        let data;

        function *prepareDB() {
            data = require('tests/models/data/json/base.json');

            yield dbHelper.clear();

            const trialTemplate = { id: 2 };

            yield trialTemplatesFactory.createWithRelations(
                trialTemplate,
                { service: { id: 3 } }
            );

            const category = { id: 1 };
            let section = { id: 7, serviceId: 3, code: 'movie', title: 'old title' };

            // First question
            let question = { id: 3, version: 5, text: 'old text' };

            yield answersFactory.createWithRelations({ id: 5, active: 1 }, { question, section, category });
            yield answersFactory.createWithRelations({ id: 6, active: 1 }, { question, section, category });
            yield answersFactory.createWithRelations({ id: 7, active: 1 }, { question, section, category });

            // Second question
            yield questionsFactory.createWithRelations(
                { id: 4, version: 1, text: 'too old text', active: 0 },
                { section, category }
            );
            question = { id: 4, version: 2, text: 'Second question from section2 and category1', active: 1 };
            yield answersFactory.createWithRelations({ id: 8, active: 1 }, { question, section, category });
            yield answersFactory.createWithRelations({ id: 9, active: 1 }, { question, section, category });

            // Third question
            question = { id: 5, version: 3, text: 'Not changed text' };
            yield answersFactory.createWithRelations(
                {
                    id: 10,
                    active: 1,
                    text: 'correct_7',
                    correct: 1
                },
                {
                    question,
                    section,
                    category
                }
            );
            yield answersFactory.createWithRelations(
                {
                    id: 11,
                    active: 1,
                    text: 'incorrect_7',
                    correct: 0
                },
                {
                    question,
                    section,
                    category
                }
            );

            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { id: 2, allowedFails: 2 },
                { trialTemplate, section }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { id: 2, quantity: 2 },
                { trialTemplate, section, category }
            );

            // old section
            section = { id: 100, serviceId: 3, code: 'old_section' };
            yield trialTemplateToSectionsFactory.createWithRelations(
                { id: 101, quantity: 5 },
                { trialTemplate, section, category }
            );
        }

        describe('success', () => {
            before(function *() {
                yield prepareDB();

                const loader = new Loader(_.cloneDeep(data));

                yield loader.upsert();
            });

            describe('sections', () => {
                it('should create `lit` sections', function *() {
                    const actual = yield db.Section.findAll({ where: { code: 'lit' } });

                    expect(actual).to.have.length(1);
                    expect(actual[0].serviceId).to.equal(3);
                    expect(actual[0].code).to.equal('lit');
                    expect(actual[0].title).to.equal('literature');
                });

                it('should update `movie` sections', function *() {
                    const actual = yield db.Section.findAll({ where: { code: 'movie' } });
                    const expected = { id: 7, serviceId: 3, code: 'movie', title: 'movie' };

                    expect(actual).to.have.length(1);
                    expect(actual[0].toJSON()).to.deep.equal(expected);
                });

                it('should exists three sections', function *() {
                    const actual = yield db.Section.count();

                    expect(actual).to.equal(3);
                });
            });

            describe('categories', () => {
                it('create second category', function *() {
                    const actual = yield db.Category.findById(2);

                    expect(actual.toJSON()).to.deep.equal({ id: 2, difficulty: 0, timeLimit: 0 });
                });

                it('should exists only two categories', function *() {
                    const actual = yield db.Category.count();

                    expect(actual).to.equal(2);
                });
            });

            describe('trialTemplateAllowedFails', () => {
                it('should create trialTemplateAllowedFails for first section', function *() {
                    const actual = yield db.TrialTemplateAllowedFails.findAll({ where: { id: { $ne: 2 } } });

                    expect(actual).to.have.length(1);
                    expect(actual[0].trialTemplateId).to.equal(2);
                    expect(actual[0].allowedFails).to.equal(1);
                });

                it('should update trialTemplateAllowedFails for second section', function *() {
                    const actual = yield db.TrialTemplateAllowedFails.findById(2, {
                        attributes: ['id', 'trialTemplateId', 'sectionId', 'allowedFails']
                    });
                    const expected = { id: 2, trialTemplateId: 2, sectionId: 7, allowedFails: 0 };

                    expect(actual.toJSON()).to.deep.equal(expected);
                });

                it('should exists only two trialTemplateAllowedFails', function *() {
                    const actual = yield db.TrialTemplateAllowedFails.count();

                    expect(actual).to.equal(2);
                });
            });

            describe('trialTemplateToSections', () => {
                it('should create two trialTemplateToSections', function *() {
                    const actual = yield db.TrialTemplateToSection.findAll({ where: { id: { $notIn: [2, 101] } } });

                    expect(actual).to.have.length(2);
                    [
                        { trialTemplateId: 2, quantity: 2 },
                        { trialTemplateId: 2, quantity: 2 }
                    ].forEach((expected, i) => {
                        expect(actual[i].trialTemplateId).to.equal(expected.trialTemplateId);
                        expect(actual[i].quantity).to.equal(expected.quantity);
                    });
                });

                it('should update trialTemplateToSections', function *() {
                    const actual = yield db.TrialTemplateToSection.findById(2, {
                        attributes: ['id', 'trialTemplateId', 'sectionId', 'categoryId', 'quantity']
                    });
                    const expected = { id: 2, trialTemplateId: 2, sectionId: 7, categoryId: 1, quantity: 1 };

                    expect(actual.toJSON()).to.deep.equal(expected);

                });

                it('should exists only four trialTemplateToSections', function *() {
                    const actual = yield db.TrialTemplateToSection.count();

                    expect(actual).to.equal(4);
                });

                it('should nullify unused trialTemplateToSections', function *() {
                    const actual = yield db.TrialTemplateToSection.findById(101, {
                        attributes: ['id', 'trialTemplateId', 'sectionId', 'categoryId', 'quantity']
                    });
                    const expected = { id: 101, trialTemplateId: 2, sectionId: 100, categoryId: 1, quantity: 0 };

                    expect(actual.toJSON()).to.deep.equal(expected);
                });
            });

            describe('questions', () => {
                it('should create 4 questions', function *() {
                    const actual = yield db.Question.findAll({
                        order: ['id', 'active'],
                        where: { id: { $notIn: [3, 4, 5] } }
                    });

                    expect(actual).to.have.length(4);
                    [
                        { version: 0, active: 1, text: 'First question from section1 and category1', type: 0 },
                        { version: 0, active: 1, text: 'Second question from section1 and category1', type: 0 },
                        { version: 0, active: 1, text: 'First question from section1 and category2', type: 0 },
                        { version: 0, active: 1, text: 'Second question from section1 and category2', type: 1 }

                    ].forEach((expected, i) => {
                        expect(actual[i].version).to.equal(expected.version);
                        expect(actual[i].active).to.equal(expected.active);
                        expect(actual[i].text).to.equal(expected.text);
                        expect(actual[i].type).to.equal(expected.type);
                    });
                });

                it('should not update question version if question not changed', function *() {
                    const actual = yield db.Question.findAll({
                        where: { id: 5 },
                        attributes: ['id', 'version', 'active', 'text', 'type', 'categoryId', 'sectionId']
                    });

                    const expected = {
                        id: 5,
                        version: 3,
                        active: 1,
                        text: 'Not changed text',
                        type: 0,
                        sectionId: 7,
                        categoryId: 1
                    };

                    expect(actual).to.have.length(1);

                    expect(actual[0].toJSON()).to.deep.equal(expected);
                });

                it('should update active question', function *() {
                    const actual = yield db.Question.findAll({
                        order: ['id', 'active'],
                        where: { id: 3 },
                        attributes: ['id', 'version', 'active', 'text', 'type', 'categoryId', 'sectionId']
                    });

                    expect(actual).to.have.length(2);
                    [
                        {
                            id: 3,
                            version: 5,
                            active: 0,
                            text: 'old text',
                            type: 0,
                            sectionId: 7,
                            categoryId: 1
                        },
                        {
                            id: 3,
                            version: 6,
                            active: 1,
                            text: 'First question from section2 and category1',
                            type: 0,
                            sectionId: 7,
                            categoryId: 1
                        }

                    ].forEach((expected, i) => {
                        expect(actual[i].toJSON()).to.deep.equal(expected);
                    });
                });

                it('should inactive question', function *() {
                    const actual = yield db.Question.findAll({ where: { id: 4 } });

                    expect(actual).to.have.length(2);
                    actual.forEach(item => expect(item.active).to.equal(0));
                });
            });
        });

        describe('answers success', () => {
            function *prepareDBForAnswersTest() {
                data = require('tests/models/xlsx/answersTest.json');

                yield dbHelper.clear();

                const trialTemplate = { id: 2 };

                yield trialTemplatesFactory.createWithRelations(
                    trialTemplate,
                    { service: { id: 1 } }
                );

                const category = { id: 1 };
                let section = { id: 1, serviceId: 1, code: 'lit', title: 'literature' };

                // для смены текста вопроса и удаления и добавления ответа
                let question = { id: 6, version: 5, text: 'old text' };

                yield answersFactory.createWithRelations(
                    {
                        id: 16,
                        active: 1,
                        text: 'correct_6',
                        correct: 1
                    },
                    {
                        question,
                        section,
                        category
                    }
                );
                yield answersFactory.createWithRelations(
                    {
                        id: 17,
                        active: 1,
                        text: 'incorrect_6', correct: 0 }, { question, section, category });

                section = { id: 2, serviceId: 1, code: 'movie', title: 'movie' };

                // для деактивации ответа
                question = { id: 1, version: 2, text: 'First question from section2 and category2' };
                yield answersFactory.createWithRelations(
                    {
                        id: 5,
                        active: 1,
                        text: 'incorrect_3_1',
                        correct: 0
                    },
                    {
                        question,
                        section,
                        category
                    }
                );
                yield answersFactory.createWithRelations(
                    {
                        id: 6,
                        active: 1,
                        text: 'correct_3',
                        correct: 1
                    },
                    {
                        question,
                        section,
                        category
                    }
                );
                yield answersFactory.createWithRelations(
                    {
                        id: 7,
                        active: 1,
                        text: 'incorrect_3_2',
                        correct: 0
                    },
                    {
                        question,
                        section,
                        category
                    }
                );

                // для деактивации вопроса
                question = { id: 2, version: 4, text: 'Second question from section2 and category2' };
                yield answersFactory.createWithRelations(
                    {
                        id: 8,
                        active: 1,
                        text: 'correct_4',
                        correct: 1
                    },
                    {
                        question,
                        section,
                        category
                    }
                );
                yield answersFactory.createWithRelations(
                    {
                        id: 9,
                        active: 1,
                        text: 'incorrect_4',
                        correct: 0
                    },
                    {
                        question,
                        section,
                        category
                    }
                );

                // для добавления новых ответов
                question = { id: 3, version: 2, text: 'Third question from section2 and category2' };
                yield answersFactory.createWithRelations(
                    {
                        id: 10,
                        active: 1,
                        text: 'correct_5',
                        correct: 1
                    },
                    {
                        question,
                        section,
                        category
                    });
                yield answersFactory.createWithRelations(
                    {
                        id: 11,
                        active: 1,
                        text: 'incorrect_5',
                        correct: 0
                    },
                    {
                        question,
                        section,
                        category
                    }
                );

                // для смены текста вопроса
                question = { id: 4, version: 2, text: 'Question old text' };
                yield answersFactory.createWithRelations(
                    {
                        id: 12,
                        active: 1,
                        text: 'correct_6',
                        correct: 1
                    },
                    {
                        question,
                        section,
                        category
                    }
                );
                yield answersFactory.createWithRelations(
                    {
                        id: 13,
                        active: 1,
                        text: 'incorrect_6',
                        correct: 0
                    },
                    {
                        question,
                        section,
                        category
                    }
                );

                // ничего не поменялось
                question = { id: 5, version: 7, text: 'Fifth question from section2 and category2' };
                yield answersFactory.createWithRelations(
                    {
                        id: 14,
                        active: 1,
                        text: 'correct_7',
                        correct: 1
                    },
                    {
                        question,
                        section,
                        category
                    }
                );
                yield answersFactory.createWithRelations(
                    {
                        id: 15,
                        active: 1,
                        text: 'incorrect_7',
                        correct: 0
                    },
                    {
                        question,
                        section,
                        category
                    }
                );

                // для смены текста ответа
                question = { id: 7, version: 1, text: 'Answers text changed', active: 1 };
                yield answersFactory.createWithRelations(
                    {
                        id: 18,
                        active: 1,
                        text: 'old_text',
                        correct: 1
                    },
                    {
                        question,
                        section,
                        category
                    }
                );
                yield answersFactory.createWithRelations(
                    {
                        id: 19,
                        active: 1,
                        text: 'not_changed_text',
                        correct: 0
                    },
                    {
                        question,
                        section,
                        category
                    }
                );

                // для смены корректности ответа
                question = { id: 8, version: 1, text: 'Answers correct changed', active: 1, type: 1 };
                yield answersFactory.createWithRelations(
                    {
                        id: 20,
                        active: 1,
                        text: 'changed_correct',
                        correct: 0
                    },
                    {
                        question,
                        section,
                        category
                    }
                );
                yield answersFactory.createWithRelations(
                    {
                        id: 21,
                        active: 1,
                        text: 'not_changed_correct',
                        correct: 1
                    },
                    {
                        question,
                        section,
                        category
                    }
                );
            }

            before(function *() {
                yield prepareDBForAnswersTest();

                const loader = new Loader(_.cloneDeep(data));

                yield loader.upsert();
            });

            function *_findAnswers(condition) {
                return yield db.Answer.findAll({
                    order: 'id',
                    include: {
                        model: db.Question,
                        as: 'question',
                        attributes: ['text']
                    },
                    where: condition
                });
            }

            function _assertAnswer(answer, expected) {
                const actual = answer.toJSON();

                expect(actual.correct).to.equal(expected.correct);
                expect(actual.questionVersion).to.equal(expected.version);
                expect(actual.text).to.equal(expected.text);
                expect(actual.question.text).to.equal(expected.question);
                expect(actual.active).to.equal(expected.active);

                if (expected.id) {
                    expect(actual.id).to.equal(expected.id);
                }
            }

            it('should create answers for new question', function *() {
                const answers = yield _findAnswers({ questionId: { $notBetween: [1, 8] } });

                expect(answers).to.have.length(4);

                [
                    {
                        question: 'Other question from section1 and category1',
                        version: 0,
                        correct: 0,
                        text: 'incorrect_1_1',
                        active: 1
                    },
                    {
                        question: 'Other question from section1 and category1',
                        version: 0,
                        correct: 0,
                        text: 'incorrect_1_2',
                        active: 1
                    },
                    {
                        question: 'Other question from section1 and category1',
                        version: 0,
                        correct: 0,
                        text: 'incorrect_1_3',
                        active: 1
                    },
                    {
                        question: 'Other question from section1 and category1',
                        version: 0,
                        correct: 1,
                        text: 'correct_1',
                        active: 1
                    }
                ].forEach((expected, i) => {
                    _assertAnswer(answers[i], expected);
                });
            });

            it('should deactivate answer for question and update question version', function *() {
                const answers = yield _findAnswers({ questionId: 1 });

                expect(answers).to.have.length(5);

                [
                    {
                        id: 5,
                        question: 'First question from section2 and category2',
                        version: 2,
                        correct: 0,
                        text: 'incorrect_3_1',
                        active: 0
                    },
                    {
                        id: 6,
                        question: 'First question from section2 and category2',
                        version: 2,
                        correct: 1,
                        text: 'correct_3',
                        active: 0
                    },
                    {
                        id: 7,
                        question: 'First question from section2 and category2',
                        version: 2,
                        correct: 0,
                        text: 'incorrect_3_2',
                        active: 0
                    },

                    {
                        question: 'First question from section2 and category2',
                        version: 3,
                        correct: 1,
                        text: 'correct_3',
                        active: 1
                    },
                    {
                        question: 'First question from section2 and category2',
                        version: 3,
                        correct: 0,
                        text: 'incorrect_3_2',
                        active: 1
                    }
                ].forEach((expected, i) => {
                    _assertAnswer(answers[i], expected);
                });
            });

            it('should deactivate all answers for deactivated question', function *() {
                const answers = yield _findAnswers({ questionId: 2 });

                expect(answers).to.have.length(2);

                [
                    {
                        id: 8,
                        question: 'Second question from section2 and category2',
                        version: 4,
                        correct: 1,
                        text: 'correct_4',
                        active: 0
                    },
                    {
                        id: 9,
                        question: 'Second question from section2 and category2',
                        version: 4,
                        correct: 0,
                        text: 'incorrect_4',
                        active: 0
                    }
                ].forEach((expected, i) => {
                    _assertAnswer(answers[i], expected);
                });
            });

            it('should add new answers and update question version', function *() {
                const answers = yield _findAnswers({ questionId: 3 });

                expect(answers).to.have.length(6);

                [
                    {
                        id: 10,
                        question: 'Third question from section2 and category2',
                        version: 2,
                        correct: 1,
                        text: 'correct_5',
                        active: 0
                    },
                    {
                        id: 11,
                        question: 'Third question from section2 and category2',
                        version: 2,
                        correct: 0,
                        text: 'incorrect_5',
                        active: 0
                    },

                    {
                        question: 'Third question from section2 and category2',
                        version: 3,
                        correct: 1,
                        text: 'correct_5',
                        active: 1
                    },
                    {
                        question: 'Third question from section2 and category2',
                        version: 3,
                        correct: 0,
                        text: 'incorrect_5',
                        active: 1
                    },
                    {
                        question: 'Third question from section2 and category2',
                        version: 3,
                        correct: 0,
                        text: 'incorrect_5_1',
                        active: 1
                    },
                    {
                        question: 'Third question from section2 and category2',
                        version: 3,
                        correct: 0,
                        text: 'incorrect_5_2',
                        active: 1
                    }
                ].forEach((expected, i) => {
                    _assertAnswer(answers[i], expected);
                });
            });

            it('should create answers for new question version if question text changed', function *() {
                const answers = yield _findAnswers({ questionId: 4 });

                expect(answers).to.have.length(4);

                [
                    {
                        id: 12,
                        question: 'Question old text',
                        version: 2,
                        correct: 1,
                        text: 'correct_6',
                        active: 0
                    },
                    {
                        id: 13,
                        question: 'Question old text',
                        version: 2,
                        correct: 0,
                        text: 'incorrect_6',
                        active: 0
                    },

                    {
                        question: 'Fourth question from section2 and category2',
                        version: 3,
                        correct: 1,
                        text: 'correct_6',
                        active: 1
                    },
                    {
                        question: 'Fourth question from section2 and category2',
                        version: 3,
                        correct: 0,
                        text: 'incorrect_6',
                        active: 1
                    }

                ].forEach((expected, i) => {
                    _assertAnswer(answers[i], expected);
                });
            });

            it('should not update answers versions if answers and question not changed', function *() {
                const answers = yield _findAnswers({ questionId: 5 });

                expect(answers).to.have.length(2);

                [
                    {
                        id: 14,
                        question: 'Fifth question from section2 and category2',
                        version: 7,
                        correct: 1,
                        text: 'correct_7',
                        active: 1
                    },
                    {
                        id: 15,
                        question: 'Fifth question from section2 and category2',
                        version: 7,
                        correct: 0,
                        text: 'incorrect_7',
                        active: 1
                    }

                ].forEach((expected, i) => {
                    _assertAnswer(answers[i], expected);
                });
            });

            it('should update question version only once if question answers changed', function *() {
                const answers = yield _findAnswers({ questionId: 6 });

                expect(answers).to.have.length(4);

                [
                    {
                        id: 16,
                        question: 'old text',
                        version: 5,
                        correct: 1,
                        text: 'correct_6',
                        active: 0
                    },
                    {
                        id: 17,
                        question: 'old text',
                        version: 5,
                        correct: 0,
                        text: 'incorrect_6',
                        active: 0
                    },

                    {
                        question: 'First question from section1 and category1',
                        version: 6,
                        correct: 1,
                        text: 'correct_6',
                        active: 1
                    },
                    {
                        question: 'First question from section1 and category1',
                        version: 6,
                        correct: 0,
                        text: 'incorrect_6_new',
                        active: 1
                    }

                ].forEach((expected, i) => {
                    _assertAnswer(answers[i], expected);
                });
            });

            it('should update question version if answer text changed', function *() {
                const answers = yield _findAnswers({ questionId: 7 });

                expect(answers).to.have.length(4);

                [
                    {
                        id: 18,
                        question: 'Answers text changed',
                        version: 1,
                        correct: 1,
                        text: 'old_text',
                        active: 0
                    },
                    {
                        id: 19,
                        question: 'Answers text changed',
                        version: 1,
                        correct: 0,
                        text: 'not_changed_text',
                        active: 0
                    },

                    {
                        question: 'Answers text changed',
                        version: 2,
                        correct: 1,
                        text: 'new_text',
                        active: 1
                    },
                    {
                        question: 'Answers text changed',
                        version: 2,
                        correct: 0,
                        text: 'not_changed_text',
                        active: 1
                    }

                ].forEach((expected, i) => {
                    _assertAnswer(answers[i], expected);
                });
            });

            it('should update question version if answer`s correctness changed', function *() {
                const answers = yield _findAnswers({ questionId: 8 });

                expect(answers).to.have.length(4);

                [
                    {
                        id: 20,
                        question: 'Answers correct changed',
                        version: 1,
                        correct: 0,
                        text: 'changed_correct',
                        active: 0
                    },
                    {
                        id: 21,
                        question: 'Answers correct changed',
                        version: 1,
                        correct: 1,
                        text: 'not_changed_correct',
                        active: 0
                    },

                    {
                        question: 'Answers correct changed',
                        version: 2,
                        correct: 1,
                        text: 'changed_correct',
                        active: 1
                    },
                    {
                        question: 'Answers correct changed',
                        version: 2,
                        correct: 1,
                        text: 'not_changed_correct',
                        active: 1
                    }

                ].forEach((expected, i) => {
                    _assertAnswer(answers[i], expected);
                });
            });
        });

        describe('errors', () => {
            before(function *() {
                yield prepareDB();

                const loader = new Loader(_.cloneDeep(data));

                yield loader.upsert();
            });

            it('should throw 400 when data is absent', function *() {
                const loader = new Loader({});

                const error = yield catchError.generator(loader.upsert.bind(loader));

                expect(error.message).to.equal('Data is absent');
                expect(error.statusCode).to.equal(400);
                expect(error.options).to.deep.equal({ internalCode: '400_DIA', schema: 'sections' });
            });

            it('should throw 400 when answers is absent', function *() {
                yield prepareDB();
                const loaderData = _.cloneDeep(data);

                delete loaderData.answers;

                const loader = new Loader(loaderData);

                const error = yield catchError.generator(loader.upsert.bind(loader));

                expect(error.message).to.equal('Answers is absent');
                expect(error.statusCode).to.equal(400);
                expect(error.options).to.deep.equal({ internalCode: '400_AIA' });
            });

            it('should throw 404 when question not found', function *() {
                yield prepareDB();
                yield db.Answer.destroy({ where: {} });
                yield db.Question.destroy({ where: {} });

                const loader = new Loader(_.cloneDeep(data));

                const error = yield catchError.generator(loader.upsert.bind(loader));

                expect(error.message).to.equal('Question not found');
                expect(error.statusCode).to.equal(404);
                expect(error.options).to.deep.equal({ internalCode: '404_QNF', id: 3 });
            });

            it('should throw 400 if question has no active answers', function *() {
                yield prepareDB();
                const loaderData = _.cloneDeep(data);

                const category = { id: 1 };
                const section = { id: 7, serviceId: 3, code: 'movie', title: 'old title' };
                const question = { id: 100, version: 1, text: 'question without answers' };

                yield answersFactory.createWithRelations({ id: 100, active: 0 }, { question, section, category });
                yield answersFactory.createWithRelations({ id: 101, active: 0 }, { question, section, category });

                loaderData.questions.push({
                    id: 100,
                    active: 1,
                    sectionId: {
                        serviceId: 3,
                        code: 'movie',
                        title: 'movie'
                    },
                    categoryId: {
                        id: 1,
                        difficulty: 0,
                        timeLimit: 0
                    },
                    text: 'question without answers',
                    type: 0
                });

                loaderData.answers.push(
                    {
                        id: 100,
                        questionId: {
                            id: 100,
                            active: 1,
                            sectionId: {
                                serviceId: 3,
                                code: 'movie',
                                title: 'movie'
                            },
                            categoryId: {
                                id: 1,
                                difficulty: 0,
                                timeLimit: 0
                            },
                            text: 'question without answers',
                            type: 0
                        },
                        correct: 1,
                        text: 'correct',
                        active: 0
                    },
                    {
                        id: 101,
                        questionId: {
                            id: 100,
                            active: 1,
                            sectionId: {
                                serviceId: 3,
                                code: 'movie',
                                title: 'movie'
                            },
                            categoryId: {
                                id: 1,
                                difficulty: 0,
                                timeLimit: 0
                            },
                            text: 'question without answers',
                            type: 0
                        },
                        correct: 0,
                        text: 'incorrect',
                        active: 0
                    }
                );

                const loader = new Loader(loaderData);

                const error = yield catchError.generator(loader.upsert.bind(loader));

                expect(error.message).to.equal('Question has no active answers');
                expect(error.statusCode).to.equal(400);
                expect(error.options).to.deep.equal({ internalCode: '400_QNA', questionId: '100' });
            });

            it('should throw 404 when answers not found', function *() {
                yield prepareDB();
                yield db.Answer.destroy({ where: {} });

                const loader = new Loader(_.cloneDeep(data));

                const error = yield catchError.generator(loader.upsert.bind(loader));

                expect(error.message).to.equal('Answers not found');
                expect(error.statusCode).to.equal(404);
                expect(error.options).to.deep.equal({ internalCode: '404_ANF', answerIds: [5, 6, 7] });
            });
        });
    });

    describe('`select`', () => {
        const firstSection = { id: 3, code: 'first', title: 'first section' };
        const secondSection = { id: 4, code: 'second', title: 'second section' };

        beforeEach(function *() {
            yield dbHelper.clear();
        });

        function *createBaseData(trialTemplate) {
            // First question
            let firstQuestion = { id: 100, active: 0, text: 'old text', type: 0, categoryId: 1, version: 1 };

            yield questionsFactory.createWithRelations(firstQuestion, { section: firstSection });

            firstQuestion = { id: 100, active: 1, text: 'question_100', type: 0, categoryId: 1, version: 2 };
            yield answersFactory.createWithRelations(
                { id: 1000, text: 'correct_100', correct: 1, active: 1 },
                { question: firstQuestion, section: firstSection }
            );
            yield answersFactory.createWithRelations(
                { id: 1001, text: 'incorrect_100', correct: 0, active: 1 },
                { question: firstQuestion, section: firstSection }
            );

            // Second question
            const secondQuestion = { id: 101, active: 0, text: 'question_101', type: 0, categoryId: 1 };

            yield answersFactory.createWithRelations(
                { id: 1002, text: 'incorrect_101', correct: 0, active: 1 },
                { question: secondQuestion, section: firstSection }
            );
            yield answersFactory.createWithRelations(
                { id: 1003, text: 'correct_101', correct: 1, active: 1 },
                { question: secondQuestion, section: firstSection }
            );

            // Third question
            const thirdQuestion = { id: 102, active: 1, text: 'question_102', type: 1, categoryId: 2 };

            yield answersFactory.createWithRelations(
                { id: 1004, text: 'correct_102_1', correct: 1, active: 1 },
                { question: thirdQuestion, section: firstSection }
            );
            yield answersFactory.createWithRelations(
                { id: 1005, text: 'correct_102_2', correct: 1, active: 1 },
                { question: thirdQuestion, section: firstSection }
            );

            // Fourth question
            const fourthQuestion = { id: 103, active: 1, text: 'question_103', type: 0, categoryId: 1 };

            yield answersFactory.createWithRelations(
                { id: 1006, text: 'correct_103', correct: 1, active: 1 },
                { question: fourthQuestion, section: secondSection }
            );
            yield answersFactory.createWithRelations(
                { id: 1007, text: 'incorrect_103', correct: 0, active: 1 },
                { question: fourthQuestion, section: secondSection }
            );

            // Allowed fails
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                { section: firstSection, trialTemplate }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 2 },
                { section: secondSection, trialTemplate }
            );

            // Quantity fails
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 3, categoryId: 1 },
                { section: firstSection, trialTemplate }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 4, categoryId: 2 },
                { section: firstSection, trialTemplate }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 5, categoryId: 1 },
                { section: secondSection, trialTemplate }
            );
        }

        it('should select exam questions', function *() {
            yield createBaseData({ id: 2 });

            const loader = new Loader();
            const actual = yield loader.select(2);

            expect(actual).to.have.length(6);
            require('tests/models/data/json/selectTestData')
                .forEach((expected, i) => expect(actual[i]).to.deep.equal(expected));
        });

        it('should not select other questions', function *() {
            yield createBaseData({ id: 2 });
            const trialTemplate = { id: 3 };
            const section = { id: 5, code: 'other', title: 'other section' };

            // Allowed fails
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 10 },
                { section, trialTemplate }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 10 },
                { section: firstSection, trialTemplate }
            );

            // Quantity fails
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 12, categoryId: 1 },
                { section, trialTemplate }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 13, categoryId: 2 },
                { section, trialTemplate }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 13, categoryId: 3 },
                { section: firstSection, trialTemplate }
            );

            const loader = new Loader();
            const actual = yield loader.select(3);

            expect(actual).to.have.length(0);
        });

        it('should not select inactive answers', function *() {
            const trialTemplate = { id: 5 };
            const section = { id: 5, code: 'old', title: 'old section' };

            // Allowed fails
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 10 },
                { section, trialTemplate }
            );

            // Question
            const question = { id: 100, active: 1, text: 'text', type: 0, categoryId: 1, version: 1 };

            yield questionsFactory.createWithRelations(question, { section });

            // Answers
            yield answersFactory.createWithRelations(
                { id: 1000, text: 'correct_1000', correct: 1, active: 1 },
                { question, section }
            );
            yield answersFactory.createWithRelations(
                { id: 1001, text: 'incorrect_1001', correct: 0, active: 0 },
                { question, section }
            );
            yield answersFactory.createWithRelations(
                { id: 1002, text: 'incorrect_1002', correct: 0, active: 1 },
                { question, section }
            );

            // Quantity fails
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 12, categoryId: 1 },
                { section, trialTemplate }
            );

            const loader = new Loader();
            const actual = yield loader.select(5);

            expect(actual).to.have.length(2);
        });
    });

    describe('`checkExamId`', () => {
        it('should do nothing if exam ids match', () => {
            const loader = new Loader({
                trialTemplateAllowedFails: [{ trialTemplateId: 1 }]
            });

            loader.checkExamId(1);
        });

        it('should throw 400 if exam ids not match', () => {
            const loader = new Loader({
                trialTemplateAllowedFails: [{ trialTemplateId: 1 }]
            });

            const error = catchError.func(loader.checkExamId.bind(loader, 2));

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('Exam ids not match');
            expect(error.options).to.deep.equal({
                internalCode: '400_ENM',
                trialTemplateId: 1,
                examId: 2
            });
        });
    });
});
