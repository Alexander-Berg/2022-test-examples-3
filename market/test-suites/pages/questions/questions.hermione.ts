'use strict';

import _ from 'lodash';
import {mergeSuites, makeSuite, PageObject} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import initialStateShort from 'spec/lib/page-mocks/questions.json';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

import initialState from './initialState';
import modelsState from './models.json';

const userStory = makeUserStory(ROUTE_NAMES.QUESTIONS);
const ModelQuestionsFilters = PageObject.get('ModelQuestionsFilters');
const ModelQuestionsHeader = PageObject.get('ModelQuestionsHeader');
const ModelQuestions = PageObject.get('ModelQuestions');
const Question = PageObject.get('Question');

export default makeSuite('Страница Вопросы.', {
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [PERMISSIONS.questions.read, 0], 3301);
            const params = {
                vendor,
                routeParams: {vendor},
            };

            return makeSuite(`${user.description}.`, {
                story: userStory({
                    user,
                    params,
                    async onSetKadavrState({id}) {
                        switch (id) {
                            case 'vendor_auto-418':
                                // тест кейс на пэйджинг, пушим сгенерированный длинный список
                                return this.browser.setState('vendorsModelQuestions', initialState);

                            case 'vendor_auto-419':
                                // тест кейс на пустой список, не устанавливаем state
                                break;

                            case 'vendor_auto-512':
                                // кейс на ограничение бесплатных услуг
                                return this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        hasFreeProducts: false,
                                    },
                                ]);

                            // Фильтрация по моделям
                            case 'vendor_auto-410':
                                await this.browser.setState('vendorModels', modelsState);

                                return this.browser.setState('vendorsModelQuestions', initialStateShort);

                            default:
                                // Простой список из статичного файла по дефолту
                                return this.browser.setState('vendorsModelQuestions', initialStateShort);
                        }
                    },
                    pageObjects: {
                        logo: 'Logo',
                        footer: 'Footer',
                        modelQuestions: 'ModelQuestions',
                        // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                        radioGroup: ['RadioGroup', ModelQuestionsFilters.root],
                        // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                        resetFilters: ['ResetFilters', ModelQuestionsFilters.root],
                        list() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('ListContainer').setItemSelector(Question.root);
                        },
                    },
                    suites: {
                        common: [
                            {
                                suite: 'Filters/__period',
                                meta: {
                                    issue: 'VNDFRONT-1774',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-415',
                                },
                                params: {
                                    fromParamName: 'from',
                                    fromParamValue: '2018-04-16',
                                    toParamName: 'to',
                                    toParamValue: '2018-07-01',
                                    initialItemsCount: 5,
                                    filteredItemsCount: 3,
                                },
                                pageObjects: {
                                    datePicker() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('DatePicker', ModelQuestionsFilters.range);
                                    },
                                },
                            },
                            {
                                suite: 'SuggestFilter',
                                suiteName: 'Фильтр по моделям.',
                                meta: {
                                    issue: 'VNDFRONT-1774',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-410',
                                    environment: 'kadavr',
                                },
                                hooks: {
                                    beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.browser.allure.runStep('Ожидаем появления саджеста', () =>
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.suggest.waitForExist(),
                                        );
                                    },
                                },
                                params: {
                                    url: `/vendors/${vendor}/questions/[0-9]+`,
                                    text: 'Смартфон Samsung Galaxy S8',
                                },
                                pageObjects: {
                                    suggest() {
                                        return this.createPageObject('Suggest', ModelQuestionsHeader.root);
                                    },
                                    popup: 'PopupB2b',
                                },
                            },
                            {
                                suite: 'Filters/__radioGroup',
                                suiteName: 'Фильтр "Неотвеченные"',
                                meta: {
                                    issue: 'VNDFRONT-1774',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-412',
                                },
                                params: {
                                    initialItemsCount: 5,
                                    tabName: 'Неотвеченные',
                                    filteredItemsCount: 2,
                                    queryParamValue: 'no',
                                    queryParamName: 'answers',
                                },
                            },
                            {
                                suite: 'Filters/__radioGroup',
                                suiteName: 'Фильтр "Отвеченные"',
                                meta: {
                                    issue: 'VNDFRONT-1774',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-413',
                                },
                                params: {
                                    initialItemsCount: 5,
                                    tabName: 'Отвеченные',
                                    filteredItemsCount: 3,
                                    queryParamValue: 'yes',
                                    queryParamName: 'answers',
                                },
                            },
                            {
                                suite: 'Filters/__select',
                                suiteName: 'Сортировка старые/новые',
                                meta: {
                                    issue: 'VNDFRONT-1774',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-411',
                                },
                                params: {
                                    initialItemText: '30 июля 2018 г.',
                                    expectedItemText: '11 апреля 2018 г.',
                                    initialFilterText: 'новые',
                                    expectedFilterText: 'старые',
                                    queryParamName: 'sortBy',
                                    queryParamValue: 'ASC',
                                    async getItemText() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.firstQuestion.waitForExist();
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.firstQuestion.getCreationDate();
                                    },
                                },
                                hooks: {
                                    // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                                    beforeEach() {
                                        return Promise.all([
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.firstQuestion.waitForExist(),
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.select.waitForExist(),
                                        ]);
                                    },
                                },
                                pageObjects: {
                                    select() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('SelectB2b', ModelQuestionsFilters.sort);
                                    },
                                    firstQuestion() {
                                        return this.createPageObject(
                                            'Question',
                                            ModelQuestions.root,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            ModelQuestions.getItemByIndex(0),
                                        );
                                    },
                                    popup: 'PopupB2b',
                                },
                            },
                            {
                                suite: 'Filters/__reset',
                                meta: {
                                    id: 'vendor_auto-442',
                                    issue: 'VNDFRONT-1786',
                                    feature: 'Вопросы и ответы',
                                },
                                params: {
                                    initialItemsCount: 5,
                                    filteredItemsCount: 2,
                                    async setFilters() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.list.waitForLoading();

                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.radioGroup.clickItemByTitle('Неотвеченные');
                                    },
                                },
                            },
                            {
                                suite: 'ModelQuestions',
                                params: {
                                    isModelNamesVisible: true,
                                    isModelLinksVisible: true,
                                },
                            },
                            {
                                suite: 'RestrictPanel',
                                pageObjects: {
                                    panel() {
                                        return this.createPageObject('RestrictPanel');
                                    },
                                },
                            },
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Вопросы о товарах бренда',
                                },
                            },
                            {
                                suite: 'Comment/deleteConfirmation',
                                suiteName: 'Подтверждение удаления ответа на вопрос',
                                meta: {
                                    id: 'vendor_auto-561',
                                    issue: 'VNDFRONT-3435',
                                    environment: 'kadavr',
                                    feature: 'Вопросы и ответы',
                                },
                                params: {
                                    messageText: 'Ответ успешно удален.',
                                },
                                pageObjects: {
                                    question() {
                                        return this.createPageObject(
                                            'Question',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.modelQuestions,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.modelQuestions.getItemByIndex(1),
                                        );
                                    },
                                    commentsList() {
                                        return this.createPageObject(
                                            'CommentsList',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.question,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.question.answerList,
                                        );
                                    },
                                    comment() {
                                        return this.createPageObject(
                                            'Comment',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.commentsList,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.commentsList.getItemByIndex(1),
                                        );
                                    },
                                },
                                hooks: {
                                    async beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        this.params.editable = this.params.has(PERMISSIONS.questions.write);

                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.question.toggleAnswers();
                                    },
                                },
                            },
                            {
                                suite: 'AnswerForm/autocompletion',
                                suiteName: 'Автоподстановка ответа на вопрос. ',
                                meta: {
                                    issue: 'VNDFRONT-3453',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-730',
                                },
                                pageObjects: {
                                    item() {
                                        return this.createPageObject(
                                            'Question',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list.getItemByIndex(0),
                                        );
                                    },
                                    answerForm() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('AnswerForm', this.item, this.item.answerForm);
                                    },
                                    toggle() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('AnswerFormToggle', this.item);
                                    },
                                },
                                params: {
                                    expectedTemplateText: 'elena.fireraven, ',
                                },
                                hooks: {
                                    // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                                    beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        this.params.exists = this.params.has(PERMISSIONS.questions.write);
                                    },
                                },
                            },
                            {
                                suite: 'AnswerForm/autocompletion',
                                suiteName: 'Автоподстановка в комментарии на ответ на вопрос. ',
                                meta: {
                                    issue: 'VNDFRONT-3453',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-731',
                                },
                                params: {
                                    answerButtonText: 'Комментировать',
                                    expectedTemplateText: 'autotestopinions, ',
                                },
                                pageObjects: {
                                    question() {
                                        return this.createPageObject(
                                            'Question',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.modelQuestions,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.modelQuestions.getItemByIndex(2),
                                        );
                                    },
                                    commentsList() {
                                        return this.createPageObject(
                                            'CommentsList',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.question,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.question.answerList,
                                        );
                                    },
                                    comment() {
                                        return this.createPageObject(
                                            'Comment',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.commentsList,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.commentsList.getItemByIndex(0),
                                        );
                                    },
                                    answerForm() {
                                        return this.createPageObject(
                                            'AnswerForm',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.question,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.question.answerForm,
                                        );
                                    },
                                    toggle() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('AnswerFormToggle', this.comment);
                                    },
                                },
                                hooks: {
                                    async beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        this.params.exists = this.params.has(PERMISSIONS.questions.write);

                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.question.toggleAnswers();
                                    },
                                },
                            },
                            {
                                suite: 'Question/toggleAnswerList',
                                suiteName: 'Сохранение ответа на вопрос. ',
                                meta: {
                                    issue: 'VNDFRONT-3453',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-736',
                                },
                                pageObjects: {
                                    question() {
                                        return this.createPageObject(
                                            'Question',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list.getItemByIndex(1),
                                        );
                                    },
                                    answerForm() {
                                        return this.createPageObject(
                                            'AnswerForm',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.question,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.question.answerForm,
                                        );
                                    },
                                    toggle() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('AnswerFormToggle', this.question);
                                    },
                                    messages() {
                                        return this.createPageObject('Messages');
                                    },
                                },
                                params: {
                                    expectedTemplateText: 'Vasya.Pupkin, ',
                                    messageText: 'Ответ успешно добавлен.',
                                    text: '2 ответа',
                                },
                                hooks: {
                                    // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                                    beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        this.params.exists = this.params.has(PERMISSIONS.questions.write);
                                    },
                                },
                            },
                        ],
                    },
                }),
            });
        });

        return mergeSuites(...suites);
    })(),
});
