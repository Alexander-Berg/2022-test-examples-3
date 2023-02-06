'use strict';

import {mergeSuites, importSuite, PageObject, makeSuite} from 'ginny';
import {ContextWithParams} from 'ginny-helpers';

const ModalMultiSelect = PageObject.get('ModalMultiSelect');
const Filters = PageObject.get('Filters');

function waitForLoading(this: ContextWithParams) {
    return this.browser.waitUntil(
        async () => {
            // @ts-expect-error(TS2683) найдено в рамках VNDFRONT-4580
            const visible = await this.spinner.isVisible();

            return visible === false;
        },
        this.browser.options.waitforTimeout,
        'Не удалось дождаться скрытия спиннера',
    );
}

/**
 * Подробный отчёт по каждому товару (таблица)
 * @param {PageObject.StatisticsReport} report - блок отчёта
 * @param {PageObject.Filters} filters - общие фильтры
 */
export default makeSuite('Подробный отчёт по товарам.', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.setPageObjects({
                spinner() {
                    return this.createPageObject('Spinner', this.report);
                },
                list() {
                    return this.createPageObject('ModelsPromotionStatisticsTableReport', this.report);
                },
            });

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.allure.runStep('Ожидаем появления блока отчёта по товарам', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.report.waitForVisible(),
            );

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.allure.runStep('Ожидаем появления таблицы с товарами', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.list.waitForVisible(),
            );

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.allure.runStep('Ожидаем загрузки таблицы с товарами', () => waitForLoading.call(this));
        },
        'Фильтры.': mergeSuites(
            {
                beforeEach() {
                    this.setPageObjects({
                        popup() {
                            return this.createPageObject('PopupB2b');
                        },
                        modal() {
                            return this.createPageObject('Modal');
                        },
                    });

                    return this.browser.allure.runStep('Проверяем начальные значения итоговых показателей', () =>
                        this.list.totalRowMetrics.should.eventually.be.equal(
                            'Итого 1 008 888 3 890 7,92 7 616,1 125,41 52,12 347 0,69 550 75',
                            'Начальные показатели корректные',
                        ),
                    );
                },
            },
            importSuite('Filters/__pager', {
                meta: {
                    id: 'vendor_auto-1029',
                },
                params: {
                    waitForLoading,
                    expectedPage: 2,
                    initialItemsCount: 20,
                    expectedItemsCount: 5,
                    afterFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep('Проверяем значения итоговых показателей', () =>
                            // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                            this.list.totalRowMetrics.should.eventually.be.equal(
                                'Итого 1 008 888 3 890 7,92 7 616,1 125,41 52,12 347 0,69 550 75',
                                'Показатели не изменились',
                            ),
                        );
                    },
                },
                pageObjects: {
                    pager() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('PagerB2b', this.report);
                    },
                },
            }),
            importSuite('Filters/__search', {
                meta: {
                    id: 'vendor_auto-1027',
                },
                params: {
                    waitForLoading,
                    initialCount: 20,
                    expectedCount: 3,
                    queryParamName: 'q',
                    queryParamValue: 'OBD2',
                    afterFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep('Проверяем значения итоговых показателей', () =>
                            // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                            this.list.totalRowMetrics.should.eventually.be.equal(
                                'Итого 142 667 386 1,14 979,5 36,62 18,62 30 0,07 66 9',
                                'Показатели корректные',
                            ),
                        );
                    },
                },
                pageObjects: {
                    input() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('InputB2b', this.report);
                    },
                },
            }),
            importSuite('Filters/__sort', {
                meta: {
                    id: 'vendor_auto-1028',
                },
                params: {
                    waitForLoading,
                    getItemText() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.firstItem.getTitle();
                    },
                    sortByQueryParamName: 'sortBy',
                    sortByQueryParamValue: 'CLICKS_TOTAL',
                    sortOrderQueryParamName: 'sortOrder',
                    sortOrderQueryParamValue: 'ASC',
                    initialItemText: 'Видеорегистратор CARCAM Z6, 2 камеры',
                    expectedItemText: 'Умные часы CARCAM DZ09',
                    afterFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep('Проверяем значения итоговых показателей', () =>
                            // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                            this.list.totalRowMetrics.should.eventually.be.equal(
                                'Итого 1 008 888 3 890 7,92 7 616,1 125,41 52,12 347 0,69 550 75',
                                'Показатели не изменились',
                            ),
                        );
                    },
                },
                pageObjects: {
                    firstItem() {
                        return this.createPageObject(
                            'ModelsPromotionStatisticsTableReportItem',
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.list,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.list.getItemByIndex(0),
                        );
                    },
                    sortToggler() {
                        return this.createPageObject(
                            'Link',
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.list,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.list.getHeadCellByIndex(2),
                        );
                    },
                },
            }),
            importSuite('Filters/__multiSelect', {
                suiteName: 'Места показа.',
                meta: {
                    id: 'vendor_auto-1377',
                    issue: 'VNDFRONT-4054',
                },
                params: {
                    waitForLoading,
                    initialFilterText: 'Все 6',
                    expectedFilterText: 'Выбрано 1',
                    queryParamName: 'ppGroup',
                    queryParamValue: 'HORIZONTAL_INCUT',
                    selectItems: ['Карусель'],
                    initialItemsCount: 20,
                    expectedItemsCount: 2,
                    afterFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep('Проверяем значения итоговых показателей', () =>
                            // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                            this.list.totalRowMetrics.should.eventually.be.equal(
                                'Итого 91 613 213 0,46 746,1 36,88 13,57 24 0,05 44 6',
                                'Показатели корректные',
                            ),
                        );
                    },
                    allItems: [
                        'Рекламные места на поиске Маркета',
                        'Карусель',
                        'Рекламный блок на карточке товара',
                        'Блок «Популярные предложения»',
                        'Рекламные кампании',
                        'Другое',
                    ],
                    expectedAllItemsFilterText: 'Все 6',
                    queryParamValueAll: [
                        'HORIZONTAL_INCUT',
                        'SEARCH',
                        'KKM',
                        'PREMIUM',
                        'SUPER_HORIZONTAL_INCUT',
                        'OTHER',
                    ],
                    expectedAllItemsCount: 6,
                    afterAllFiltration(this: ContextWithParams) {
                        return this.browser.allure.runStep(
                            'Проверяем значения итоговых показателей при всех выбранных значениях фильтра',
                            () =>
                                // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                                this.list.totalRowMetrics.should.eventually.be.equal(
                                    'Итого 400 736 2 326 1,97 6 391,5 51,62 21,83 192 0,2 132 18',
                                    'Показатели корректные',
                                ),
                        );
                    },
                },
                pageObjects: {
                    select() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('MultiSelectB2b', this.filters.label(3));
                    },
                },
            }),
            importSuite('Filters/__multiSelect', {
                suiteName: 'Версия Маркета.',
                meta: {
                    id: 'vendor_auto-1372',
                    issue: 'VNDFRONT-4054',
                },
                params: {
                    waitForLoading,
                    initialFilterText: 'Все 3',
                    expectedFilterText: 'Мобильная',
                    queryParamName: 'platform',
                    queryParamValue: 'TOUCH',
                    selectItems: ['Мобильная'],
                    initialItemsCount: 20,
                    expectedItemsCount: 3,
                    afterFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep('Проверяем значения итоговых показателей', () =>
                            // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                            this.list.totalRowMetrics.should.eventually.be.equal(
                                'Итого 28 746 93 0,93 0 0 0 9 0,09 66 9',
                                'Показатели корректные',
                            ),
                        );
                    },
                    allItems: ['Десктопная', 'Мобильная', 'Приложение'],
                    expectedAllItemsFilterText: 'Все 3',
                    queryParamValueAll: ['TOUCH', 'DESKTOP', 'APPLICATION'],
                    expectedAllItemsCount: 5,
                    afterAllFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep(
                            'Проверяем значения итоговых показателей при всех выбранных значениях фильтра',
                            () =>
                                // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                                this.list.totalRowMetrics.should.eventually.be.equal(
                                    'Итого 91 443 432 2,19 0 0 0 75 0,25 110 15',
                                    'Показатели корректные',
                                ),
                        );
                    },
                },
                pageObjects: {
                    select() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('MultiSelectB2b', this.filters.label(1));
                    },
                },
            }),
            importSuite('Filters/__select', {
                suiteName: 'Товары продвигались.',
                meta: {
                    id: 'vendor_auto-1373',
                    issue: 'VNDFRONT-4054',
                },
                params: {
                    waitForLoading,
                    initialFilterText: 'Неважно',
                    expectedFilterText: 'Да',
                    queryParamName: 'promotionType',
                    queryParamValue: 'PROMOTED',
                    initialItemsCount: 20,
                    expectedItemsCount: 3,
                    afterFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep('Проверяем значения итоговых показателей', () =>
                            // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                            this.list.totalRowMetrics.should.eventually.be.equal(
                                'Итого 142 924 365 0,97 928,5 24,53 14,98 32 0,09 66 9',
                                'Показатели корректные',
                            ),
                        );
                    },
                },
                pageObjects: {
                    select() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('SelectB2b', this.filters.label(2));
                    },
                },
            }),
            importSuite('Filters/__select', {
                suiteName: 'Период.',
                meta: {
                    id: 'vendor_auto-1371',
                    issue: 'VNDFRONT-4054',
                },
                params: {
                    waitForLoading,
                    initialFilterText: 'За последние 30 дней',
                    expectedFilterText: 'За последние 7 дней',
                    queryParamName: 'period',
                    queryParamValue: 'WEEK',
                    initialItemsCount: 20,
                    expectedItemsCount: 8,
                    afterFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep('Проверяем значения итоговых показателей', () =>
                            // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                            this.list.totalRowMetrics.should.eventually.be.equal(
                                'Итого 518 317 2 356 2,01 6 391,5 51,62 21,83 192 0,2 176 24',
                                'Показатели корректные',
                            ),
                        );
                    },
                },
                pageObjects: {
                    select() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('SelectAdvanced', this.filters.label());
                    },
                },
            }),
            importSuite('Filters/modalMultiSelect', {
                suiteName: 'Группы.',
                meta: {
                    id: 'vendor_auto-1374',
                    issue: 'VNDFRONT-4054',
                },
                params: {
                    waitForLoading,
                    initialFilterText: 'Все 3',
                    expectedFilterText: 'Выбрано 1',
                    expectedAllItemsFilterText: 'Выбрано 3',
                    initialItemsCount: 20,
                    expectedItemsCount: 5,
                    expectedAllItemsCount: 8,
                    selectItems: ['Умные часы'],
                    allItems: ['Mi Band 5', 'Mi Bunny', 'Умные часы'],
                    afterFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep('Проверяем значения итоговых показателей', () =>
                            // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                            this.list.totalRowMetrics.should.eventually.be.equal(
                                'Итого 147 612 160 0,89 0 0 0 10 0,07 110 15',
                                'Показатели корректные',
                            ),
                        );
                    },
                    afterAllFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep(
                            'Проверяем значения итоговых показателей при всех выбранных категориях',
                            () =>
                                // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                                this.list.totalRowMetrics.should.eventually.be.equal(
                                    'Итого 286 214 494 1,61 746,1 36,88 13,57 55 0,17 176 24',
                                    'Показатели корректные',
                                ),
                        );
                    },
                },
                pageObjects: {
                    select() {
                        return this.createPageObject(
                            'ModalMultiSelect',
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.filters,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            `${Filters.label()} ${ModalMultiSelect.root}`,
                        );
                    },
                },
            }),
            importSuite('Filters/modalMultiSelect', {
                suiteName: 'Категории.',
                meta: {
                    id: 'vendor_auto-1375',
                    issue: 'VNDFRONT-4054',
                },
                params: {
                    waitForLoading,
                    initialFilterText: 'Все 2',
                    expectedFilterText: 'Выбрано 1',
                    expectedAllItemsFilterText: 'Выбрано 2',
                    initialItemsCount: 20,
                    expectedItemsCount: 2,
                    expectedAllItemsCount: 20,
                    selectItems: ['Телефоны / Умные часы и браслеты'],
                    allItems: ['Телефоны / Умные часы и браслеты', 'Видеотехника / Автомобильные видеорегистраторы'],
                    afterFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep('Проверяем значения итоговых показателей', () =>
                            // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                            this.list.totalRowMetrics.should.eventually.be.equal(
                                'Итого 48 260 117 0,62 135 15,2 6,43 9 0,05 44 6',
                                'Показатели корректные',
                            ),
                        );
                    },
                    afterAllFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep(
                            'Проверяем значения итоговых показателей при всех выбранных категориях',
                            () =>
                                // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                                this.list.totalRowMetrics.should.eventually.be.equal(
                                    'Итого 1 008 888 3 890 7,92 7 616,1 125,41 52,12 347 0,69 550 75',
                                    'Показатели корректные',
                                ),
                        );
                    },
                },
                pageObjects: {
                    select() {
                        return this.createPageObject(
                            'ModalMultiSelect',
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.filters,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            `${Filters.label(1)} ${ModalMultiSelect.root}`,
                        );
                    },
                },
            }),
            importSuite('Filters/modalMultiSelect', {
                suiteName: 'Товары.',
                meta: {
                    id: 'vendor_auto-1376',
                    issue: 'VNDFRONT-4054',
                },
                params: {
                    waitForLoading,
                    initialFilterText: 'Все 14',
                    expectedFilterText: 'Выбрано 1',
                    expectedAllItemsFilterText: 'Выбрано 3',
                    initialItemsCount: 20,
                    expectedItemsCount: 1,
                    expectedAllItemsCount: 3,
                    selectItems: ['Mi Band 5'],
                    allItems: ['Amazfit Bip S Lite', 'Smart bracelet xiaomi mi 2', 'Mi Band 5'],
                    afterFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep('Проверяем значения итоговых показателей', () =>
                            // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                            this.list.totalRowMetrics.should.eventually.be.equal(
                                'Итого 234 380 1 933 0,82 5 645,4 14,74 8,27 141 0,06 22 3',
                                'Показатели корректные',
                            ),
                        );
                    },
                    afterAllFiltration() {
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep(
                            'Проверяем значения итоговых показателей при всех выбранных категориях',
                            () =>
                                // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                                this.list.totalRowMetrics.should.eventually.be.equal(
                                    'Итого 267 833 2 081 1,66 5 780,4 29,94 14,69 154 0,14 66 9',
                                    'Показатели корректные',
                                ),
                        );
                    },
                },
                pageObjects: {
                    select() {
                        return this.createPageObject(
                            'ModalMultiSelect',
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.filters,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            `${Filters.label(2)} ${ModalMultiSelect.root}`,
                        );
                    },
                },
            }),
        ),
    },
});
