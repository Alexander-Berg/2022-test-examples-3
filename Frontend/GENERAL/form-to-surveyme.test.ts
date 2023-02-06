import { Forms, FormsAPI } from './types';
import { Nulls } from './utils/api-empty-survey';
import * as FormToSurveyme from './form-to-surveyme';

const emptyPage: Forms.Page = {
    type: 'page',
    key: '',
    title: '',
    description: '',
    body: [],
};

const textQuestion: Forms.FieldText = {
    type: 'text',
    key: '',
    title: 'Текст',
    description: '',
    view: 'textinput',
    suggest: null,
};

const formData: Forms.Form = {
    key: '',
    description: '',
    uuid: 'a9ff57a2-84c0-4c90-8c36-72b57fbe0065',
    type: 'form',
    title: 'Form template',
    body: [
        emptyPage,
    ],
    stats: {
        enabled: true,
    },
};

describe('form-to-surveyme', () => {
    describe('#toApiFormat', () => {
        it('должен вернуть настройки формы в поле "surveys"', () => {
            const apiData = FormToSurveyme.toApiFormat(formData);

            expect(apiData.hasOwnProperty('survey')).toBe(true);
            expect(apiData.survey.name).toEqual(formData.title);
        });

        it('должен вернуть stats в поле "surveys"', () => {
            const apiData = FormToSurveyme.toApiFormat(formData);

            expect(apiData.survey.hasOwnProperty('stats')).toBe(true);
            expect(apiData.survey.stats?.enabled).toEqual(formData.stats?.enabled);
        });

        it('должен вернуть поле "questions" с преобразованным списком вопросов', () => {
            const questions: FormsAPI.QuestionInitial[] = [];
            const makeQuestions = jest.fn(() => questions);
            const apiData = FormToSurveyme.toApiFormat(formData, makeQuestions);

            expect(makeQuestions).toBeCalledTimes(1);
            expect(apiData.questions).toBe(questions);
        });

        it('должен вернуть поле "texts" с настройками идентичными Nulls.Texts', () => {
            const apiData = FormToSurveyme.toApiFormat(formData);

            expect(apiData.texts).toMatchObject(Nulls.Texts);
        });
    });

    describe('#makeQuestions', () => {
        it('должен вернуть пустой массив если нет вопросов', () => {
            const pages: Forms.Page[] = [
                emptyPage,
            ];

            const questionsApiData = FormToSurveyme.makeQuestions(pages);

            expect(questionsApiData.length).toBe(0);
        });

        it('должен вызвать вызвать функцию для получения данных специфичных для вопроса', () => {
            const pages: Forms.Page[] = [
                {
                    ...emptyPage,
                    body: [
                        textQuestion,
                        textQuestion,
                    ],
                },
            ];
            const makeApiData = jest.fn();

            FormToSurveyme.makeQuestions(pages, makeApiData);

            expect(makeApiData).toBeCalledTimes(pages[0].body.length);
        });

        it('должен проставить номер страницу и позицию вопроса', () => {
            const pages: Forms.Page[] = [
                {
                    ...emptyPage,
                    body: [
                        textQuestion,
                    ],
                },
            ];

            const questionsApiData = FormToSurveyme.makeQuestions(pages);

            expect(questionsApiData[0].page).toBe(1);
            expect(questionsApiData[0].position).toBe(1);
        });

        it('должен проставить позицию внутри страницы для нескольких вопросов', () => {
            const pages: Forms.Page[] = [
                {
                    ...emptyPage,
                    body: [
                        textQuestion,
                        textQuestion,
                    ],
                },
            ];

            const questionsApiData = FormToSurveyme.makeQuestions(pages);

            expect(questionsApiData[1].page).toBe(1);
            expect(questionsApiData[1].position).toBe(2);
        });

        it('должен проставить разные позиции для нескольких страниц', () => {
            const pages: Forms.Page[] = [
                {
                    ...emptyPage,
                    body: [
                        textQuestion,
                    ],
                },
                {
                    ...emptyPage,
                    body: [
                        textQuestion,
                    ],
                },
            ];

            const questionsApiData = FormToSurveyme.makeQuestions(pages);

            expect(questionsApiData[1].page).toBe(2);
            expect(questionsApiData[1].position).toBe(1);
        });
    });

    describe('#makeStats', () => {
        it('должен вернуть true', () => {
            const questionsApiData = FormToSurveyme.makeStats(formData.stats);
            expect(questionsApiData.enabled).toBe(formData.stats.enabled);
        });

        it('должен вернуть false', () => {
            const questionsApiData = FormToSurveyme.makeStats(undefined);
            expect(questionsApiData.enabled).toBe(false);
        });
    });
});
