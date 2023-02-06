const Excel = require('models/excel');
const { expect } = require('chai');
const fs = require('fs');

const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const answersFactory = require('tests/factory/answersFactory');
const questionsFactory = require('tests/factory/questionsFactory');
const trialTemplateToSections = require('tests/factory/trialTemplateToSectionsFactory');

const dbHelper = require('tests/helpers/clear');
const catchError = require('tests/helpers/catchError');

describe('Excel model', () => {
    const data = fs.readFileSync('tests/models/data/xlsx/base.xlsx');
    let excel;

    before(() => {
        excel = Excel.tryLoad(data);
    });

    describe('`constructor`', () => {
        it('should parse columns', () => {
            [
                'examId',
                'sectionsCode',
                'sectionTitle',
                'allowedFails',
                'categoryId',
                'quantity',
                'questionId',
                'questionActive',
                'questionText',
                'questionType',
                'answerId',
                'answerActive',
                'answerText',
                'answerCorrect'
            ].forEach(columnName => expect(excel.columns[columnName]).not.be.undefined);
        });

        it('should define worksheet', () => {
            expect(excel.worksheet).not.be.undefined;
        });
    });

    describe('`getValue`', () => {
        it('should return undefined when cell is empty', () => {
            const actual = excel.getValue('examId', 4);

            expect(actual).to.be.undefined;
        });

        it('should return cell value', () => {
            const actual = excel.getValue('examId', 3);

            expect(actual).to.equal(2);
        });

        it('should replace \r\n to " "', () => {
            const actual = excel.getValue('questionText', 14);

            expect(actual).to.equal('Second question from section1 and category2');
        });

        it('should throw error when invalid column name', () => {
            const error = catchError.func(excel.getValue.bind(excel, 'invalidColumnName'));

            expect(error.message).to.equal('Invalid column name');
            expect(error.statusCode).to.equal(400);
            expect(error.options).to.deep.equal({
                internalCode: '400_ICN',
                columnName: 'invalidColumnName'
            });
        });

        it('should throw error when value type is invalid', () => {
            const wrongData = fs.readFileSync('tests/models/data/xlsx/wrongCellValueType.xlsx');
            const wrongExcel = Excel.tryLoad(wrongData);
            const error = catchError.func(excel.getValue.bind(wrongExcel, 'quantity', 3));

            expect(error.message).to.equal('Invalid value type');
            expect(error.statusCode).to.equal(400);
            expect(error.options).to.deep.equal({
                internalCode: '400_IVT',
                columnName: 'quantity',
                value: 'two',
                rowNumber: 3
            });
        });
    });

    describe('`tryLoad`', () => {
        it('should success parse xlsx', () => {
            Excel.tryLoad(data);
        });

        it('should throw 400 when file format is invalid', () => {
            const otherData = fs.readFileSync('index.js');
            const error = catchError.func(Excel.tryLoad.bind(Excel, otherData));

            expect(error.message).to.equal('Parse failed');
            expect(error.statusCode).to.equal(400);
            expect(error.options).to.deep.equal({
                internalCode: '400_PFD',
                details: 'Unsupported file 114'
            });
        });
    });

    describe('`getOperations`', () => {
        beforeEach(function *() {
            yield dbHelper.clear();
        });
        describe('success', () => {
            let actual;
            const firstSection = { serviceId: 3, code: 'lit', title: 'literature' };
            const secondSection = { serviceId: 3, code: 'movie', title: 'movie' };

            const firstCategory = { id: 1, difficulty: 0, timeLimit: 0 };
            const secondCategory = { id: 2, difficulty: 0, timeLimit: 0 };

            beforeEach(function *() {
                const service = { id: 3, code: 'direct' };

                yield trialTemplatesFactory.createWithRelations(
                    { id: 2 },
                    { service }
                );

                const section = { id: 7 };

                let question = { id: 3, version: 2, text: 'old text' };

                yield answersFactory.createWithRelations({ id: 5, active: 1 }, { question, section });
                yield answersFactory.createWithRelations({ id: 6, active: 1 }, { question, section });
                yield answersFactory.createWithRelations({ id: 7, active: 1 }, { question, section });

                question = { id: 4, version: 5, text: 'old text too' };
                yield answersFactory.createWithRelations({ id: 8, active: 1 }, { question, section });
                yield answersFactory.createWithRelations({ id: 9, active: 1 }, { question, section });

                actual = yield excel.getOperations();
            });

            it('should return two sections', () => {
                expect(actual.sections).to.have.length(2);

                expect(actual.sections[0]).to.deep.equal(firstSection);
                expect(actual.sections[1]).to.deep.equal(secondSection);
            });

            it('should return two categories', () => {
                expect(actual.categories).to.have.length(2);

                expect(actual.categories[0]).to.deep.equal(firstCategory);
                expect(actual.categories[1]).to.deep.equal(secondCategory);
            });

            it('should return three trialTemplateToSection', () => {
                expect(actual.trialTemplateToSections).to.have.length(3);

                expect(actual.trialTemplateToSections[0]).to.deep.equal({
                    trialTemplateId: 2,
                    sectionId: firstSection,
                    categoryId: firstCategory,
                    quantity: 2
                });
                expect(actual.trialTemplateToSections[1]).to.deep.equal({
                    trialTemplateId: 2,
                    sectionId: firstSection,
                    categoryId: secondCategory,
                    quantity: 2
                });
                expect(actual.trialTemplateToSections[2]).to.deep.equal({
                    trialTemplateId: 2,
                    sectionId: secondSection,
                    categoryId: firstCategory,
                    quantity: 1
                });
            });

            it('should return two trialTemplateAllowedFails', () => {
                expect(actual.trialTemplateAllowedFails).to.have.length(2);

                expect(actual.trialTemplateAllowedFails[0]).to.deep.equal({
                    trialTemplateId: 2,
                    sectionId: firstSection,
                    allowedFails: 1
                });
                expect(actual.trialTemplateAllowedFails[1]).to.deep.equal({
                    trialTemplateId: 2,
                    sectionId: secondSection,
                    allowedFails: 0
                });
            });

            it('should return 6 questions', () => {
                expect(actual.questions).to.have.length(6);

                expect(actual.questions[0]).to.deep.equal({
                    id: 0,
                    active: 1,
                    sectionId: firstSection,
                    categoryId: firstCategory,
                    text: 'First question from section1 and category1',
                    type: 0
                });
                expect(actual.questions[1]).to.deep.equal({
                    id: 0,
                    active: 1,
                    sectionId: firstSection,
                    categoryId: firstCategory,
                    text: 'Second question from section1 and category1',
                    type: 0
                });
                expect(actual.questions[2]).to.deep.equal({
                    id: 0,
                    active: 1,
                    sectionId: firstSection,
                    categoryId: secondCategory,
                    text: 'First question from section1 and category2',
                    type: 0
                });
                expect(actual.questions[3]).to.deep.equal({
                    id: 0,
                    active: 1,
                    sectionId: firstSection,
                    categoryId: secondCategory,
                    text: 'Second question from section1 and category2',
                    type: 1
                });
                expect(actual.questions[4]).to.deep.equal({
                    id: 3,
                    active: 1,
                    sectionId: secondSection,
                    categoryId: firstCategory,
                    text: 'First question from section2 and category1',
                    type: 0
                });
                expect(actual.questions[5]).to.deep.equal({
                    id: 4,
                    active: 0,
                    sectionId: secondSection,
                    categoryId: firstCategory,
                    text: 'Second question from section2 and category1',
                    type: 0
                });
            });

            it('should return 20 answers', () => {
                expect(actual.answers).to.have.length(20);

                const expected = [
                    {
                        id: 0,
                        question: 'First question from section1 and category1',
                        text: 'incorrect_1_1',
                        correct: 0,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'First question from section1 and category1',
                        text: 'incorrect_1_2',
                        correct: 0,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'First question from section1 and category1',
                        text: 'incorrect_1_3',
                        correct: 0,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'First question from section1 and category1',
                        text: 'correct_1',
                        correct: 1,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'Second question from section1 and category1',
                        text: 'incorrect_2_1',
                        correct: 0,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'Second question from section1 and category1',
                        text: 'correct_2',
                        correct: 1,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'Second question from section1 and category1',
                        text: 'incorrect_2_2',
                        correct: 0,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'First question from section1 and category2',
                        text: 'correct_3',
                        correct: 1,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'First question from section1 and category2',
                        text: 'incorrect_3_1',
                        correct: 0,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'First question from section1 and category2',
                        text: 'incorrect_3_2',
                        correct: 0,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'First question from section1 and category2',
                        text: 'incorrect_3_3',
                        correct: 0,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'Second question from section1 and category2',
                        text: 'correct_4_1',
                        correct: 1,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'Second question from section1 and category2',
                        text: 'correct_4_2',
                        correct: 1,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'Second question from section1 and category2',
                        text: 'correct_4_3',
                        correct: 1,
                        active: 1
                    },
                    {
                        id: 0,
                        question: 'Second question from section1 and category2',
                        text: 'incorrect_4',
                        correct: 0,
                        active: 1
                    },
                    {
                        id: 5,
                        question: 'First question from section2 and category1',
                        text: 'incorrect_5_1',
                        correct: 0,
                        active: 1
                    },
                    {
                        id: 6,
                        question: 'First question from section2 and category1',
                        text: 'correct_5',
                        correct: 1,
                        active: 1
                    },
                    {
                        id: 7,
                        question: 'First question from section2 and category1',
                        text: 'incorrect_5_2',
                        correct: 0,
                        active: 1
                    },
                    {
                        id: 8,
                        question: 'Second question from section2 and category1',
                        text: 'correct_6',
                        correct: 1,
                        active: 1
                    },
                    {
                        id: 9,
                        question: 'Second question from section2 and category1',
                        text: 'incorrect_6',
                        correct: 0,
                        active: 1
                    }
                ];

                actual.answers.forEach((answer, i) => {
                    expect(answer.id).to.equal(expected[i].id);
                    expect(answer.questionId.text).to.equal(expected[i].question);
                    expect(answer.correct).to.equal(expected[i].correct);
                    expect(answer.text).to.equal(expected[i].text);
                    expect(answer.active).to.equal(expected[i].active);
                });
            });
        });

        it('should throw 400 when wrong count of correct answers', function *() {
            const section = { id: 21, code: 'lit' };
            const question = { id: 1 };

            yield trialTemplateToSections.createWithRelations({}, { trialTemplate: { id: 2 }, section });
            yield answersFactory.createWithRelations({ id: 11 }, { question, section });
            yield answersFactory.createWithRelations({ id: 12 }, { question, section });

            const wrongData = fs.readFileSync('tests/models/data/xlsx/wrongCorrectCount.xlsx');
            const wrongExcel = Excel.tryLoad(wrongData);
            const error = yield catchError.generator(wrongExcel.getOperations.bind(wrongExcel));

            expect(error.message).to.equal('Wrong number of correct answers');
            expect(error.statusCode).to.equal(400);
            expect(error.options).to.deep.equal({
                internalCode: '400_WCA',
                questionText: 'Third question from section1 and category1'
            });
        });

        it('should throw 404 when exam not found', function *() {
            const error = yield catchError.generator(excel.getOperations.bind(excel));

            expect(error.message).to.equal('Exam not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_ENF', id: 2 });
        });

        it('should throw 404 when question not found', function *() {
            yield trialTemplatesFactory.createWithRelations({ id: 2 });

            const error = yield catchError.generator(excel.getOperations.bind(excel));

            expect(error.message).to.equal('Question not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_QNF', id: 3 });
        });

        it('should throw 404 when answers not found', function *() {
            yield trialTemplatesFactory.createWithRelations({ id: 2 });
            yield questionsFactory.createWithRelations({ id: 3, version: 2, text: 'old text' });

            const error = yield catchError.generator(excel.getOperations.bind(excel));

            expect(error.message).to.equal('Answer not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_ANF', id: 5 });
        });
    });

    describe('`write`', () => {
        const path = 'tests/test.xlsx';

        function writeTestData() {
            const rows = require('tests/models/data/json/insertTestData.json');
            const blank = fs.readFileSync('templates/blank.xlsx');
            const otherExcel = Excel.tryLoad(blank);

            const otherData = otherExcel.write(rows);

            fs.writeFileSync(path, otherData);
        }

        afterEach(() => {
            fs.unlinkSync(path);
        });

        it('should write correct data', () => {
            writeTestData();

            const actualData = fs.readFileSync(path);
            const otherExcel = Excel.tryLoad(actualData);

            require('tests/models/data/js/selectTestSavedData').forEach((localData, i) => {
                for (const key in localData) {
                    if (!Object.prototype.hasOwnProperty.call(localData, key)) {
                        continue;
                    }

                    const actual = otherExcel.getValue(key, i + 3);

                    expect(actual).to.equal(localData[key]);
                }
            });
        });

        it('should calculate correct range', () => {
            writeTestData();

            const actualData = fs.readFileSync(path);
            const otherExcel = Excel.tryLoad(actualData);
            const actual = otherExcel.worksheet['!ref'];

            expect(actual).to.equal('A1:N10');
        });

        it('should write object', () => {
            const rows = require('tests/models/data/json/insertTestData.json');
            const blank = fs.readFileSync('templates/blank.xlsx');
            const writeExcel = Excel.tryLoad(blank);

            const otherData = writeExcel.write(rows[0]);

            fs.writeFileSync(path, otherData);

            const actualData = fs.readFileSync(path);
            const otherExcel = Excel.tryLoad(actualData);
            const actual = otherExcel.worksheet['!ref'];

            expect(actual).to.equal('A1:N3');
        });
    });

    describe('`setValue`', () => {
        beforeEach(() => {
            const blank = fs.readFileSync('templates/blank.xlsx');

            excel = Excel.tryLoad(blank);
        });

        it('should throw error when column name is invalid', () => {
            const error = catchError.func(excel.setValue.bind(excel, 'invalidColumnName'));

            expect(error.message).to.equal('Invalid column name');
            expect(error.statusCode).to.equal(400);
            expect(error.options).to.deep.equal({
                internalCode: '400_ICN',
                columnName: 'invalidColumnName'
            });
        });

        it('should write number', () => {
            excel.setValue('examId', 3, 123);
            const actual = excel.worksheet.A3;

            expect(actual).to.deep.equal({ v: 123, t: 'n' });
        });

        it('should write string', () => {
            excel.setValue('examId', 3, 'someString');
            const actual = excel.worksheet.A3;

            expect(actual).to.deep.equal({ v: 'someString', t: 's' });
        });
    });
});
