'use strict';

import url from 'url';

import {makeSuite, importSuite, mergeSuites, makeCase} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';

/**
 * Тест на блок Opinion.
 * @param {PageObject.Opinion} item
 */
export default makeSuite('Отзыв.', {
    issue: 'VNDFRONT-1256',
    environment: 'testing',
    feature: 'Отзывы на модель',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления тестируемого отзыва', () => this.item.waitForExist());
            },
        },
        importSuite('Link', {
            meta: {
                id: 'vendor_auto-139',
                environment: 'kadavr',
            },
            suiteName: 'Ссылка "Отзыв на Маркете"',
            params: {
                external: true,
                target: '_blank',
                caption: 'Отзыв на Маркете',
                url: buildUrl('external:market-model-reviews', {modelId: '[0-9]+'}),
                comparison: {
                    mode: 'match',
                    skipProtocol: true,
                },
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.item, this.item.externalLink);
                },
            },
        }),
        {
            'Ссылка на конкретную модель.': {
                'Открывает страницу со списом отзывов': makeCase({
                    id: 'vendor_auto-140',
                    async test() {
                        const currentUrl = await this.browser.yaParseUrl();

                        // runStep требует обязательно функцию
                        await this.browser.allure.runStep(`Текущий URL ${currentUrl.path}`, () => Promise.resolve());

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        const newUrl = await this.browser.yaWaitForChangeUrl(() =>
                            this.browser.allure.runStep('Кликаем на конкретную модель', () =>
                                this.item.titleLink.click(),
                            ),
                        );

                        return this.browser.allure.runStep(
                            `URL поменялся на ${url.parse(newUrl).path} и фильтры сохранились`,
                            () =>
                                newUrl.should.be.link(
                                    `/vendors/[0-9]+/opinions/models/[0-9]+${currentUrl.search || ''}`,
                                    {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                    },
                                ),
                        );
                    },
                }),
            },
        },
        importSuite('AnswerForm', {
            meta: {
                id: 'vendor_auto-141',
                issue: 'VNDFRONT-1687',
                environment: 'kadavr',
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
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.item, this.item.commentsList);
                },
                answer() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(1));
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.exists = this.params.has(PERMISSIONS.opinions.write);
                },
            },
        }),
        importSuite('AnswerForm/cancel', {
            meta: {
                id: 'vendor_auto-159',
                issue: 'VNDFRONT-1789',
                environment: 'kadavr',
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
                    this.params.exists = this.params.has(PERMISSIONS.opinions.write);
                },
            },
        }),
        importSuite('Comment', {
            meta: {
                id: 'vendor_auto-142',
                issue: 'VNDFRONT-1697',
                environment: 'kadavr',
            },
            pageObjects: {
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.list.getItemByIndex(4));
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(0));
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.editable = this.params.has(PERMISSIONS.opinions.write);
                },
            },
        }),
        importSuite('Comment/cancel', {
            meta: {
                id: 'vendor_auto-160',
                issue: 'VNDFRONT-1794',
                environment: 'kadavr',
            },
            pageObjects: {
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.list.getItemByIndex(4));
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(0));
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.editable = this.params.has(PERMISSIONS.opinions.write);
                },
            },
        }),
        importSuite('Comment/delete', {
            meta: {
                id: 'vendor_auto-143',
                issue: 'VNDFRONT-1795',
                environment: 'kadavr',
            },
            params: {
                confirmText: 'Вы уверены, что хотите удалить комментарий?',
                messageText: 'Комментарий успешно удален.',
            },
            pageObjects: {
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.list.getItemByIndex(4));
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(1));
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.editable = this.params.has(PERMISSIONS.opinions.write);
                },
            },
        }),
        importSuite('Comment/deleteConfirmation', {
            suiteName: 'Подтверждение удаления ответа на отзыв',
            meta: {
                id: 'vendor_auto-559',
                issue: 'VNDFRONT-3435',
            },
            params: {
                messageText: 'Комментарий успешно удален.',
            },
            pageObjects: {
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.list.getItemByIndex(4));
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(1));
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.editable = this.params.has(PERMISSIONS.opinions.write);
                },
            },
        }),
        importSuite('Comment/badge', {
            suiteName: 'Бейдж производителя в ответе на отзыв.',
            meta: {
                id: 'vendor_auto-454',
                issue: 'VNDFRONT-1845',
            },
            pageObjects: {
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.list.getItemByIndex(0));
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
        }),
        importSuite('Comment/answerOnly', {
            meta: {
                id: 'vendor_auto-144',
                issue: 'VNDFRONT-3435',
            },
            pageObjects: {
                commentsList() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CommentsList', this.item);
                },
                comment() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Comment', this.commentsList, this.commentsList.getItemByIndex(0));
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.editable = this.params.has(PERMISSIONS.opinions.write);
                },
            },
        }),
        importSuite('AnswerForm', {
            suiteName: 'Невозможность комментирования в недоступной вендору категории.',
            meta: {
                id: 'vendor_auto-145',
                issue: 'VNDFRONT-1848',
            },
            params: {
                exists: false,
            },
            pageObjects: {
                toggle() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('AnswerFormToggle', this.list.getItemByIndex(3));
                },
            },
        }),
        importSuite('AnswerForm/autocompletion', {
            suiteName: 'Автоподстановка ответа на отзыв. ',
            meta: {
                id: 'vendor_auto-734',
                issue: 'VNDFRONT-3435',
            },
            pageObjects: {
                answerForm() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('AnswerForm', this.item, this.item.answerForm);
                },
                toggle() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('AnswerFormToggle', this.item, this.item.answerFormToggle);
                },
            },
            params: {
                expectedTemplateText: 'elena.fireraven, ',
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.exists = this.params.has(PERMISSIONS.opinions.write);
                },
            },
        }),
    ),
});
