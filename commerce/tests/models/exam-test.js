require('co-mocha');

const Exam = require('models/exam');
const catchError = require('tests/helpers/catchError');
const dbHelper = require('tests/helpers/clear');

const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const trialTemplateToSectionsFactory = require('tests/factory/trialTemplateToSectionsFactory');
const trialTemplateAllowedFailsFactory = require('tests/factory/trialTemplateAllowedFailsFactory');
const answersFactory = require('tests/factory/answersFactory');
const categoriesFactory = require('tests/factory/categoriesFactory');

const examJSON = require('tests/models/data/json/exam.json');

const { expect } = require('chai');
const _ = require('lodash');

describe('Exam model', () => {
    describe('`findByIdentity`', () => {
        let trialTemplate;

        beforeEach(function *() {
            yield dbHelper.clear();

            const trialTemplateData = {
                id: 2,
                slug: 'testExam',
                isProctoring: false,
                clusterSlug: 'testCluster',
                rules: 'RULES',
                description: 'DESCRIPTION',
                ogDescription: 'OG',
                seoDescription: 'SEO'
            };
            const trialTemplateModel = yield trialTemplatesFactory.createWithRelations(trialTemplateData);

            trialTemplate = trialTemplateModel.toJSON();
        });

        it('should throw error when `identity` is invalid', function *() {
            const error = yield catchError.generator(Exam.findByIdentity.bind(Exam, '!$#'));

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('Exam identity is invalid');
            expect(error.options).to.deep.equal({ internalCode: '400_EII', identity: '!$#' });
        });

        it('should throw error when test not found', function *() {
            const error = yield catchError.generator(Exam.findByIdentity.bind(Exam, 321));

            expect(error.statusCode).to.equal(404);
            expect(error.message).to.equal('Test not found');
            expect(error.options).to.deep.equal({ internalCode: '404_TNF' });
        });

        it('should return instance of `Exam` by `id`', function *() {
            const actual = yield Exam.findByIdentity(2);

            expect(actual).to.be.an.instanceof(Exam);
        });

        it('should return instance of `Exam` by `slug`', function *() {
            const actual = yield Exam.findByIdentity('testExam');

            expect(actual).to.be.an.instanceof(Exam);
        });

        it('should return `countQuestions` equal 0 when not `trialTemplatesToSection` table', function *() {
            const exam = yield Exam.findByIdentity(2);
            const actual = exam.toJSON();

            expect(actual.questionsCount).to.equal(0);
        });

        it('should return `allowedFails` equal 0 when not `trialTemplatesToSection` table', function *() {
            const exam = yield Exam.findByIdentity(2);
            const actual = exam.toJSON();

            expect(actual.allowedFails).to.equal(0);
        });

        it('should return `sectionsCount` equal 0 when not `trialTemplatesToSection` table', function *() {
            const exam = yield Exam.findByIdentity(2);
            const actual = exam.toJSON();

            expect(actual.sectionsCount).to.equal(0);
        });

        it('should sum `questionsCount`', function *() {
            const type = { id: 3 };
            const service = { id: 4 };
            let section = { id: 5, code: 'first' };

            yield trialTemplateToSectionsFactory.createWithRelations(
                { categoryId: 1, quantity: 1 },
                { trialTemplate, section, type, service }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { categoryId: 2, quantity: 2 },
                { trialTemplate, section, type, service }
            );

            section = { id: 6, code: 'second' };
            yield trialTemplateToSectionsFactory.createWithRelations(
                { categoryId: 1, quantity: 3 },
                { trialTemplate, section, type, service }
            );

            const exam = yield Exam.findByIdentity(2);
            const actual = exam.toJSON();

            expect(actual.questionsCount).to.equal(6);
        });

        it('should sum `allowedFails`', function *() {
            let section = { id: 5, code: 'first' };

            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                { trialTemplate, section }
            );

            section = { id: 6, code: 'second' };
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 2 },
                { trialTemplate, section }
            );

            const exam = yield Exam.findByIdentity(2);
            const actual = exam.toJSON();

            expect(actual.allowedFails).to.equal(3);
        });

        it('should calculate `sectionsCount`', function *() {
            // First section
            let section = { id: 5, code: 'first' };

            yield trialTemplateToSectionsFactory.createWithRelations(
                { categoryId: 1, quantity: 2 },
                { trialTemplate, section }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { categoryId: 2, quantity: 1 },
                { trialTemplate, section }
            );

            section = { id: 6, code: 'second' };
            yield trialTemplateToSectionsFactory.createWithRelations(
                { categoryId: 1, quantity: 2 },
                { trialTemplate, section }
            );

            section = { id: 7, code: 'third' };
            yield trialTemplateToSectionsFactory.createWithRelations(
                { categoryId: 1, quantity: 0 },
                { trialTemplate, section }
            );

            const exam = yield Exam.findByIdentity(2);
            const actual = exam.toJSON();

            expect(actual.sectionsCount).to.equal(2);
        });

        // https://st.yandex-team.ru/EXPERTDEV-485
        it('should return `isProctoring` and `clusterSlug` fields', function *() {
            const exam = yield Exam.findByIdentity(2);
            const actual = exam.toJSON();

            expect(actual.isProctoring).to.be.false;
            expect(actual.clusterSlug).to.equal('testCluster');
        });

        it('should return `description`, `rules`, `seoDescription`, `ogDescription` fields', function *() {
            const exam = yield Exam.findByIdentity(2);
            const actual = exam.toJSON();

            expect(actual.rules).to.equal('RULES');
            expect(actual.description).to.equal('DESCRIPTION');
            expect(actual.seoDescription).to.equal('SEO');
            expect(actual.ogDescription).to.equal('OG');
        });
    });

    describe('`getFindCondition`', () => {
        it('should return condition with `id` when identity is number', () => {
            const actual = Exam.getFindCondition(123);

            expect(actual).to.deep.equal({ id: 123 });
        });

        it('should return condition with `slug` when identity is string', () => {
            const actual = Exam.getFindCondition('abc');

            expect(actual).to.deep.equal({ slug: 'abc' });
        });

        it('should throw error when identity is invalid', () => {
            const error = catchError.func(Exam.getFindCondition.bind(Exam, '!^*'));

            expect(error.message).to.equal('Exam identity is invalid');
            expect(error.statusCode).to.equal(400);
            expect(error.options).to.deep.equal({ internalCode: '400_EII', identity: '!^*' });
        });
    });

    describe('`findByCluster`', () => {
        const firstTrialTemplateData = {
            id: 2,
            slug: 'exam',
            isProctoring: false,
            clusterSlug: 'testCluster'
        };
        const secondTrialTemplateData = {
            id: 3,
            slug: 'exam2',
            isProctoring: true,
            clusterSlug: 'testCluster'
        };
        const examFields = ['id', 'slug', 'isProctoring', 'clusterSlug'];

        beforeEach(function *() {
            yield dbHelper.clear();

            yield trialTemplatesFactory.createWithRelations(firstTrialTemplateData);
            yield trialTemplatesFactory.createWithRelations(secondTrialTemplateData);
        });

        it('should throw 404 when exams by cluster not found', function *() {
            const error = yield catchError.generator(Exam.findByCluster.bind(Exam, 'not_exist'));

            expect(error.statusCode).to.equal(404);
            expect(error.message).to.equal('Exams by cluster not found');
            expect(error.options).to.deep.equal({ internalCode: '404_ECN', clusterSlug: 'not_exist' });
        });

        it('should return all exams by cluster', function *() {
            const actuals = yield Exam.findByCluster('testCluster');

            expect(actuals).to.have.length(2);

            const firstActual = actuals[0].toJSON();
            const secondActual = actuals[1].toJSON();

            expect(_.pick(firstActual, examFields)).to.deep.equal(firstTrialTemplateData);
            expect(_.pick(secondActual, examFields)).to.deep.equal(secondTrialTemplateData);
        });
    });

    describe('`getInfoByIds`', () => {
        beforeEach(function *() {
            yield dbHelper.clear();

            yield trialTemplatesFactory.createWithRelations({ id: 1, slug: 'first', language: 0 });
            yield trialTemplatesFactory.createWithRelations({ id: 2, slug: 'second', language: 1 });
        });

        it('should return exam info by ids', function *() {
            const actual = yield Exam.getInfoByIds([1, 2]);

            expect(actual).to.deep.equal({
                1: {
                    language: 'ru',
                    slug: 'first'
                },
                2: {
                    language: 'en',
                    slug: 'second'
                }
            });
        });

        it('should return empty object if exam with id not exist', function *() {
            const actual = yield Exam.getInfoByIds([3]);

            expect(actual).to.deep.equal({});
        });
    });

    describe('`updateSettings`', () => {
        beforeEach(dbHelper.clear);

        it('should update settings fields', function *() {
            yield trialTemplatesFactory.createWithRelations({
                id: 1,
                slug: 'test-old',
                title: 'Old title',
                timeLimit: 1000
            }, {});

            const exam = yield Exam.findByIdentity(1);

            yield exam.updateSettings({
                title: 'new title',
                timeLimit: 2000,
                slug: 'test-new'
            });

            expect(exam.get('title')).to.equal('new title');
            expect(exam.get('timeLimit')).to.equal(2000);
            expect(exam.get('slug')).to.equal('test-old');
        });
    });

    describe('`getExamData`', () => {
        const trialTemplate = { id: 2 };
        const firstSection = { id: 3, code: 'first', title: 'first section' };
        const secondSection = { id: 4, code: 'second', title: 'second section' };
        const service = { id: 5 };
        const firstQuestion = { id: 101, active: 1, text: 'question_101', type: 0, categoryId: 1, version: 1 };

        function *createBaseData() {
            // First category
            yield categoriesFactory.create({ id: 1, difficulty: 1, timeLimit: 10 });

            // Second category
            yield categoriesFactory.create({ id: 2, difficulty: 2, timeLimit: 20 });

            // First question
            yield answersFactory.createWithRelations(
                { id: 1000, text: 'correct_101', correct: 1, active: 1 },
                { question: firstQuestion, section: firstSection, service, trialTemplate }
            );
            yield answersFactory.createWithRelations(
                { id: 1001, text: 'incorrect_101', correct: 0, active: 1 },
                { question: firstQuestion, section: firstSection, service, trialTemplate }
            );

            // Second question
            const secondQuestion = { id: 102, active: 1, text: 'question_102', type: 0, categoryId: 2, version: 1 };

            yield answersFactory.createWithRelations(
                { id: 1002, text: 'incorrect_102', correct: 0, active: 1 },
                { question: secondQuestion, section: firstSection, service, trialTemplate }
            );
            yield answersFactory.createWithRelations(
                { id: 1003, text: 'correct_102', correct: 1, active: 1 },
                { question: secondQuestion, section: firstSection, service, trialTemplate }
            );

            // Third question
            const thirdQuestion = { id: 103, active: 1, text: 'question_103', type: 1, categoryId: 1, version: 1 };

            yield answersFactory.createWithRelations(
                { id: 1004, text: 'correct_103_1', correct: 1, active: 1 },
                { question: thirdQuestion, section: secondSection, service, trialTemplate }
            );
            yield answersFactory.createWithRelations(
                { id: 1005, text: 'correct_103_2', correct: 1, active: 1 },
                { question: thirdQuestion, section: secondSection, service, trialTemplate }
            );

            // Fourth question
            const fourthQuestion = { id: 104, active: 1, text: 'question_104', type: 0, categoryId: 1, version: 1 };

            yield answersFactory.createWithRelations(
                { id: 1006, text: 'correct_104', correct: 1, active: 1 },
                { question: fourthQuestion, section: secondSection, service, trialTemplate }
            );
            yield answersFactory.createWithRelations(
                { id: 1007, text: 'incorrect_104', correct: 0, active: 1 },
                { question: fourthQuestion, section: secondSection, service, trialTemplate }
            );

            // Allowed fails
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                { section: firstSection, trialTemplate, service }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 2 },
                { section: secondSection, trialTemplate, service }
            );

            // Quantity
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 2, categoryId: 1 },
                { section: firstSection, trialTemplate, service }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1, categoryId: 2 },
                { section: firstSection, trialTemplate, service }
            );
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 2, categoryId: 1 },
                { section: secondSection, trialTemplate, service }
            );
        }

        beforeEach(dbHelper.clear);

        it('should select data only for current exam', function *() {
            yield createBaseData();

            const otherTrialTemplate = { id: 3 };
            const otherService = { id: 6 };
            const otherSection = { id: 5, code: 'other', title: 'other section' };

            // Allowed fails
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                {
                    section: otherSection,
                    trialTemplate: otherTrialTemplate,
                    service: otherService
                }
            );

            // Quantity
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1, categoryId: 1 },
                {
                    section: otherSection,
                    trialTemplate: otherTrialTemplate,
                    service: otherService
                }
            );

            // Other question
            const otherQuestion = { id: 404, active: 1, text: 'question_404', type: 0, categoryId: 1 };

            yield answersFactory.createWithRelations(
                { id: 1008, text: 'correct_404', correct: 1, active: 1 },
                {
                    question: otherQuestion,
                    section: otherSection,
                    service: otherService,
                    trialTemplate: otherTrialTemplate
                }
            );

            const actual = yield Exam.getExamData(2);

            expect(actual).to.deep.equal(examJSON);
        });

        it('should select only sections where quantity > 0', function *() {
            yield createBaseData();

            const otherService = { id: 6 };
            const otherSection = { id: 5, code: 'other', title: 'other section' };

            // Allowed fails
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                {
                    section: otherSection,
                    trialTemplate,
                    service: otherService
                }
            );

            // Quantity
            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 0, categoryId: 1 },
                { section: otherSection, trialTemplate, service: otherService }
            );

            const actual = yield Exam.getExamData(2);

            expect(actual).to.deep.equal(examJSON);
        });

        it('should select only active questions', function *() {
            yield createBaseData();

            // Inactive question
            const inactiveQuestion = { id: 105, active: 0, text: 'question_105', type: 0, categoryId: 2 };

            yield answersFactory.createWithRelations(
                { id: 1010, text: 'correct_105', correct: 1, active: 1 },
                { question: inactiveQuestion, section: firstSection, service, trialTemplate }
            );

            const actual = yield Exam.getExamData(2);

            expect(actual).to.deep.equal(examJSON);
        });

        it('should select only active answers', function *() {
            yield createBaseData();

            // Inactive answer
            yield answersFactory.createWithRelations(
                { id: 1010, text: 'correct_101', correct: 1, active: 0 },
                { question: firstQuestion, section: firstSection, service, trialTemplate }
            );

            const actual = yield Exam.getExamData(2);

            expect(actual).to.deep.equal(examJSON);
        });

        it('should return fields with `[]` when exam is empty', function *() {
            yield trialTemplatesFactory.createWithRelations(trialTemplate);

            const actual = yield Exam.getExamData(2);

            expect(actual).to.deep.equal({
                sections: [],
                categories: [],
                trialTemplateAllowedFails: [],
                trialTemplateToSections: [],
                questions: [],
                answers: []
            });
        });
    });
});
