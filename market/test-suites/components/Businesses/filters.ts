'use strict';

import {mergeSuites, importSuite, makeSuite} from 'ginny';

/**
 * Тесты на фильтры страницы "Рекомендованные магазины"
 *
 * @param {PageObject.BusinessesList} list - таблица с магазинами
 */
export default makeSuite('Фильтры.', {
    feature: 'Рекомендованные бизнесы',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    input() {
                        return this.createPageObject('TextFieldLevitan');
                    },
                });

                await this.allure.runStep('Ожидаем появления списка бизнесов', () => this.list.waitForVisible());

                await this.allure.runStep('Ожидаем загрузки списка бизнесов', () => this.list.waitForLoading());
            },
        },
        importSuite('Filters/__search', {
            suiteName: 'Поиск по названию.',
            meta: {
                issue: 'VNDFRONT-4301',
                id: 'vendor_auto-1447',
                environment: 'kadavr',
            },
            params: {
                initialCount: 10,
                expectedCount: 1,
                queryParamName: 'text',
                queryParamValue: 'Мир банан',
            },
        }),
        importSuite('Filters/__search', {
            suiteName: 'Поиск несуществующего названия.',
            meta: {
                issue: 'VNDFRONT-4301',
                id: 'vendor_auto-1447',
                environment: 'kadavr',
            },
            params: {
                initialCount: 10,
                expectedCount: 0,
                queryParamName: 'text',
                queryParamValue: 'Ой всё',
                notFoundText: 'Ничего не найдено',
            },
            pageObjects: {
                notFoundElement() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('TextNextLevitan', this.list.notFound);
                },
            },
        }),
        importSuite('Filters/__pager', {
            suiteName: 'Пагинация списка бизнесов.',
            meta: {
                issue: 'VNDFRONT-4301',
                id: 'vendor_auto-1448',
                environment: 'kadavr',
            },
            params: {
                expectedPage: 2,
                initialItemsCount: 10,
                expectedItemsCount: 8,
            },
            pageObjects: {
                pager() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('PagerLevitan', this.list);
                },
            },
        }),
    ),
});
