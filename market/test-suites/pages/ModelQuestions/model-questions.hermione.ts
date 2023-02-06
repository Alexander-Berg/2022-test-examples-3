'use strict';

import _ from 'lodash';
import {mergeSuites, makeSuite, PageObject} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import buildUrl from 'spec/lib/helpers/buildUrl';
import initialStateShort from 'spec/lib/page-mocks/questions.json';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

import initialState from '../Questions/initialState';

const userStory = makeUserStory(ROUTE_NAMES.MODEL_QUESTIONS);
const ModelQuestionsFilters = PageObject.get('ModelQuestionsFilters');
const ModelQuestions = PageObject.get('ModelQuestions');
const Question = PageObject.get('Question');
const BackLink = PageObject.get('BackLink');

const modelId = 1722193751;

export default makeSuite('Страница Вопросы о модели.', {
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [PERMISSIONS.questions.read, 0], 3301);
            const params = {
                vendor,
                routeParams: {vendor, modelId},
            };

            return makeSuite(`${user.description}.`, {
                story: userStory({
                    user,
                    params,
                    // @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580
                    onSetKadavrState({id}) {
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
                                suite: 'ModelQuestions',
                                params: {
                                    isModelNamesVisible: false,
                                    isModelLinksVisible: false,
                                },
                            },
                            {
                                suite: 'Link',
                                suiteName: 'Ссылка "Обратно к списку"',
                                meta: {
                                    issue: 'VNDFRONT-1774',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-441',
                                },
                                params: {
                                    url: `/vendors/${vendor}/questions`,
                                    caption: 'Обратно к списку',
                                    comparison: {
                                        skipHostname: true,
                                    },
                                },
                                pageObjects: {
                                    link() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('Link', this.browser, BackLink.root);
                                    },
                                },
                                hooks: {
                                    async beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.browser.vndOpenPage(ROUTE_NAMES.MODEL_QUESTIONS, {
                                            vendor,
                                            modelId,
                                            retPath: buildUrl(ROUTE_NAMES.QUESTIONS, {vendor}),
                                        });
                                    },
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
                                    initialItemsCount: 3,
                                    filteredItemsCount: 1,
                                    async setFilters() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.list.waitForLoading();

                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.radioGroup.clickItemByTitle('Неотвеченные');
                                    },
                                },
                            },
                            {
                                suite: 'Filters/__period',
                                meta: {
                                    issue: 'VNDFRONT-1774',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-440',
                                },
                                params: {
                                    fromParamName: 'from',
                                    fromParamValue: '2018-04-25',
                                    toParamName: 'to',
                                    toParamValue: '2018-07-01',
                                    initialItemsCount: 3,
                                    filteredItemsCount: 1,
                                },
                                pageObjects: {
                                    datePicker() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('DatePicker', ModelQuestionsFilters.range);
                                    },
                                },
                            },
                            {
                                suite: 'Filters/__radioGroup',
                                suiteName: 'Фильтр "Неотвеченные"',
                                meta: {
                                    issue: 'VNDFRONT-1774',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-437',
                                },
                                params: {
                                    initialItemsCount: 3,
                                    tabName: 'Неотвеченные',
                                    filteredItemsCount: 1,
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
                                    id: 'vendor_auto-438',
                                },
                                params: {
                                    initialItemsCount: 3,
                                    tabName: 'Отвеченные',
                                    filteredItemsCount: 2,
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
                                    expectedItemText: '24 апреля 2018 г.',
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
                                    title: 'Вопросы',
                                },
                            },
                            {
                                suite: 'Comment/deleteConfirmation',
                                suiteName: 'Подтверждение удаления ответа на вопрос о конкретной модели. ',
                                meta: {
                                    id: 'vendor_auto-562',
                                    issue: 'VNDFRONT-3435',
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
                                suite: 'Comment/deleteConfirmation',
                                suiteName: 'Подтверждение удаления комментария на ответ о конкретной модели. ',
                                meta: {
                                    id: 'vendor_auto-704',
                                    issue: 'VNDFRONT-3435',
                                    feature: 'Вопросы и ответы',
                                },
                                params: {
                                    messageText: 'Комментарий к ответу успешно удален.',
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
                                    list() {
                                        return this.createPageObject(
                                            'CommentsList',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.question,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.question.answerList,
                                        );
                                    },
                                    item() {
                                        return this.createPageObject(
                                            'Comment',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list.getItemByIndex(0),
                                        );
                                    },
                                    commentsList() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('CommentsList', this.item, this.item.list);
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
                                suiteName: 'Автоподстановка ответа на вопрос по конкретной модели. ',
                                meta: {
                                    issue: 'VNDFRONT-3453',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-732',
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
                                suiteName: 'Автоподстановка в комментарии на ответ по конкретной модели. ',
                                meta: {
                                    issue: 'VNDFRONT-3453',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-733',
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
                                suiteName: 'Сохранение ответа на вопрос по конкретной модели. ',
                                meta: {
                                    issue: 'VNDFRONT-3453',
                                    feature: 'Вопросы и ответы',
                                    id: 'vendor_auto-737',
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
