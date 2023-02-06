'use strict';

import {makeSuite, importSuite, mergeSuites, PageObject} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';

const Question = PageObject.get('Question');

/**
 * Тест на блок Question.
 * @param {PageObject.Question} item
 * @param {Object} params
 * @param {boolean} params.isModelNamesVisible - отображение названия моделей
 * @param {boolean} params.isModelLinksVisible - отображение ссылки на модель на Маркете
 */
export default makeSuite('Вопрос.', {
    environment: 'kadavr',
    feature: 'Вопросы и ответы',
    params: {
        user: 'Пользователь',
        isModelNamesVisible: 'Отображение названия модели',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.allure.runStep('Ожидаем появления тестируемого вопроса', () => this.item.waitForExist());
            },
        },
        importSuite('Link', {
            suiteName: 'Ссылка "Вопрос на Маркете"',
            meta: {
                id: 'vendor_auto-416',
                issue: 'VNDFRONT-1793',
                environment: 'kadavr',
            },
            params: {
                caption: 'Вопрос на Маркете',
                target: '_blank',
                url: buildUrl('external:market-model-questions', {modelId: '[0-9]+'}),
                external: true,
                comparison: {
                    mode: 'match',
                    skipPathname: true,
                    skipProtocol: true,
                },
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.item, this.item.externalLink);
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.exist = this.params.isModelLinksVisible;
                },
            },
        }),
        importSuite('AnswerForm', {
            suiteName: 'Ответ на вопрос.',
            meta: {
                id: 'vendor_auto-428',
                issue: 'VNDFRONT-1780',
            },
            params: {
                answerButtonText: 'Комментировать',
            },
            pageObjects: {
                root() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('AnswerForm', this.item);
                },
                toggle() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('AnswerFormToggle', this.item);
                },
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.item, this.item.answerList);
                },
                answer() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(0));
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.exists = this.params.has(PERMISSIONS.questions.write);
                },
            },
        }),
        importSuite('AnswerForm/cancel', {
            suiteName: 'Отмена ответа на вопрос.',
            meta: {
                id: 'vendor_auto-423',
                issue: 'VNDFRONT-1789',
            },
            pageObjects: {
                root() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('AnswerForm', this.item, this.item.answerForm);
                },
                toggle() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('AnswerFormToggle', this.item, this.item.answerFormToggle);
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.exists = this.params.has(PERMISSIONS.questions.write);
                },
            },
        }),
        importSuite('Comment', {
            suiteName: 'Редактирование ответа на вопрос.',
            meta: {
                id: 'vendor_auto-425',
                issue: 'VNDFRONT-1791',
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
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.question, this.question.answerList);
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(0));
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
        }),
        importSuite('Comment/cancel', {
            suiteName: 'Отмена редактирования ответа на вопрос.',
            meta: {
                id: 'vendor_auto-424',
                issue: 'VNDFRONT-1794',
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
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.question, this.question.answerList);
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(0));
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
        }),
        importSuite('Comment/delete', {
            suiteName: 'Удаление ответа на вопрос.',
            meta: {
                id: 'vendor_auto-426',
                issue: 'VNDFRONT-1795',
            },
            params: {
                confirmText: 'Вы уверены, что хотите удалить ответ?',
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
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.question, this.question.answerList);
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(1));
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
        }),
        importSuite('AnswerForm', {
            suiteName: 'Комментирование ответа.',
            meta: {
                id: 'vendor_auto-434',
                issue: 'VNDFRONT-1796',
            },
            params: {
                answerButtonVisible: false,
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
                list() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.question, this.question.answerList);
                },
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.list, this.list.getItemByIndex(1));
                },
                root() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('AnswerForm', this.item);
                },
                toggle() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('AnswerFormToggle', this.item);
                },
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.item, this.item.list);
                },
                answer() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(0));
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
        }),
        importSuite('AnswerForm/cancel', {
            suiteName: 'Отмена комментирования ответа.',
            meta: {
                id: 'vendor_auto-429',
                issue: 'VNDFRONT-1800',
            },
            params: {
                answerButtonText: 'Комментировать',
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
                list() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.question, this.question.answerList);
                },
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.list, this.list.getItemByIndex(1));
                },
                root() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('AnswerForm', this.item);
                },
                toggle() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('AnswerFormToggle', this.item);
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
        }),
        importSuite('Comment', {
            suiteName: 'Редактирование комментария.',
            meta: {
                id: 'vendor_auto-432',
                issue: 'VNDFRONT-1801',
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
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.question, this.question.answerList);
                },
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.list, this.list.getItemByIndex(0));
                },
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.item, this.item.list);
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(0));
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
        }),
        importSuite('Comment/cancel', {
            suiteName: 'Отмена редактирования комментария.',
            meta: {
                id: 'vendor_auto-431',
                issue: 'VNDFRONT-1803',
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
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.question, this.question.answerList);
                },
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.list, this.list.getItemByIndex(0));
                },
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.item, this.item.list);
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(0));
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
        }),
        importSuite('Comment/delete', {
            suiteName: 'Удаление комментария.',
            meta: {
                id: 'vendor_auto-433',
                issue: 'VNDFRONT-1805',
            },
            params: {
                confirmText: 'Вы уверены, что хотите удалить ответ?',
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
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.question, this.question.answerList);
                },
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.list, this.list.getItemByIndex(0));
                },
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.item, this.item.list);
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(1));
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
        }),
        importSuite('Comment/badge', {
            suiteName: 'Бейдж производителя в ответе на вопрос.',
            meta: {
                id: 'vendor_auto-451',
                issue: 'VNDFRONT-1754',
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
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.question, this.question.answerList);
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(0));
                },
                badge() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('VendorBadge', this.comment);
                },
            },
            hooks: {
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.question.toggleAnswers();
                },
            },
        }),
        importSuite('Comment/badge', {
            suiteName: 'Бейдж производителя в комментарии.',
            meta: {
                id: 'vendor_auto-452',
                issue: 'VNDFRONT-1754',
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
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.question, this.question.answerList);
                },
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.list, this.list.getItemByIndex(0));
                },
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.item, this.item.list);
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(0));
                },
                badge() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('VendorBadge', this.comment);
                },
            },
            hooks: {
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.question.toggleAnswers();
                },
            },
        }),
        importSuite('Question/toggle', {
            params: {
                text: '2 ответа',
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
            },
        }),
        importSuite('Link', {
            suiteName: 'Ссылка в названии модели',
            meta: {
                id: 'vendor_auto-416',
                issue: 'VNDFRONT-1793',
                environment: 'kadavr',
            },
            params: {
                caption: 'Смартфон Samsung Galaxy S8',
                comparison: {
                    skipHostname: true,
                    skipProtocol: true,
                },
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.item, Question.modelLink);
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {routeParams, isModelNamesVisible} = this.params;
                    const {vendor} = routeParams;
                    const modelId = 1722193751;

                    if (isModelNamesVisible) {
                        // моделька из первого вопроса в initialState.json
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.params.url = `/vendors/${vendor}/questions/${modelId}`;
                    } else {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.params.exist = false;
                    }
                },
            },
        }),
        importSuite('Question/answerReadonly', {
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
            },
            hooks: {
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.question.toggleAnswers();
                },
            },
        }),
        importSuite('Comment/withoutChanges', {
            suiteName: 'Редактирование ответа без изменений.',
            meta: {
                id: 'vendor_auto-448',
                issue: 'VNDFRONT-1818',
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
                commentList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.question, this.question.answerList);
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentList, this.commentList.getItemByIndex(0));
                },
                popup() {
                    return this.createPageObject('PopupB2b');
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
        }),
        importSuite('Comment/withoutChanges', {
            meta: {
                id: 'vendor_auto-449',
                issue: 'VNDFRONT-1818',
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
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.question, this.question.answerList);
                },
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.list, this.list.getItemByIndex(0));
                },
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.item, this.item.list);
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(0));
                },
                popup() {
                    return this.createPageObject('PopupB2b');
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
        }),
    ),
});
