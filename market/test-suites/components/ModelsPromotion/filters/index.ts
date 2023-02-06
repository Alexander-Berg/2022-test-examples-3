'use strict';

import {mergeSuites, importSuite, makeSuite, makeCase} from 'ginny';

import categoriesState from './categoriesState.json';

/**
 * Тесты на фильтры списка продвигаемых товаров
 * @param {PageObject.ListContainer} list - список товаров
 * @param {PageObject.Filters} filters - фильтры
 */
export default makeSuite('Фильтры.', {
    issue: 'VNDFRONT-2231',
    environment: 'kadavr',
    feature: 'Прогнозатор',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.allure.runStep('Ожидаем появления фильтров', () => this.filters.waitForExist());
            },
        },
        {
            'Диапазон ставок.': {
                'При установке начального значения': {
                    'модели фильтруются': makeCase({
                        id: 'vendor_auto-665',
                        async test() {
                            await this.filters.setTextRangeFromValue('0.2', 'Ставка от', 1);
                            await this.list.waitForLoading();

                            const expectedModelsCount = 3;

                            await this.browser.allure.runStep(
                                `Количество элементов в списке стало ${expectedModelsCount}`,
                                () => this.list.getItemsCount().should.eventually.be.equal(expectedModelsCount),
                            );
                        },
                    }),
                },
                'При установке конечного значения': {
                    'модели фильтруются': makeCase({
                        id: 'vendor_auto-666',
                        async test() {
                            await this.filters.setTextRangeToValue('0.1', 'Ставка до', 1);
                            await this.list.waitForLoading();

                            const expectedModelsCount = 18;

                            await this.browser.allure.runStep(
                                `Количество элементов в списке стало ${expectedModelsCount}`,
                                () => this.list.getItemsCount().should.eventually.be.equal(expectedModelsCount),
                            );
                        },
                    }),
                },
            },
            'Диапазон цен.': {
                'При установке начального значения': {
                    'модели фильтруются': makeCase({
                        id: 'vendor_auto-663',
                        async test() {
                            await this.filters.setTextRangeFromValue('7000', 'Цена от', 0);
                            await this.list.waitForLoading();

                            const expectedModelsCount = 6;

                            await this.browser.allure.runStep(
                                `Количество элементов в списке стало ${expectedModelsCount}`,
                                () => this.list.getItemsCount().should.eventually.be.equal(expectedModelsCount),
                            );
                        },
                    }),
                },
                'При установке конечного значения': {
                    'модели фильтруются': makeCase({
                        id: 'vendor_auto-664',
                        async test() {
                            await this.filters.setTextRangeToValue('8000', 'Цена до', 0);
                            await this.list.waitForLoading();

                            const expectedModelsCount = 12;

                            await this.browser.allure.runStep(
                                `Количество элементов в списке стало ${expectedModelsCount}`,
                                () => this.list.getItemsCount().should.eventually.be.equal(expectedModelsCount),
                            );
                        },
                    }),
                },
            },
        },
        importSuite('Filters/__select', {
            suiteName: 'Сортировка.',
            meta: {
                id: 'vendor_auto-667',
            },
            params: {
                initialFilterText: 'Сначала популярные',
                expectedFilterText: 'Сначала дорогие',
                initialItemText: 'Видеорегистратор CARCAM Q7',
                expectedItemText: 'Видеорегистратор CARCAM K1 Авто',
                queryParamName: 'sortBy',
                queryParamValue: 'expensive',
                async getItemText() {
                    // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                    await this.browser.allure.runStep('Ожидаем появления модели', () =>
                        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                        this.item.waitForExist(),
                    );

                    // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                    return this.link.getText();
                },
            },
            pageObjects: {
                select() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('SelectB2b', this.filters, this.filters.select(3));
                },
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('ModelsPromotionListItem', this.list, this.list.getItemByIndex(0));
                },
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.item.modelSnippet);
                },
                popup() {
                    return this.createPageObject('PopupB2b');
                },
            },
        }),
        importSuite('Filters/__search', {
            suiteName: 'Поиск товара.',
            meta: {
                id: 'vendor_auto-661',
            },
            params: {
                initialCount: 20,
                expectedCount: 15,
                queryParamName: 'modelNameParam',
                queryParamValue: 'Видеорегистратор CARCAM',
            },
            pageObjects: {
                input() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('InputB2b', this.filters, this.filters.text);
                },
            },
        }),
        importSuite('Filters/__search', {
            suiteName: 'Поиск несуществующего товара.',
            meta: {
                id: 'vendor_auto-658',
            },
            params: {
                initialCount: 20,
                expectedCount: 0,
                queryParamName: 'modelNameParam',
                queryParamValue: 'Abracadabra',
            },
            pageObjects: {
                input() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('InputB2b', this.filters, this.filters.text);
                },
            },
        }),
        importSuite('Filters/suggest', {
            suiteName: 'Категория.',
            meta: {
                id: 'vendor_auto-662',
                issue: 'VNDFRONT-3332',
            },
            params: {
                searchText: 'Радар-детекторы',
                initialCount: 20,
                expectedCount: 2,
                queryParamName: 'categoryId',
                queryParamValue: 90462,
            },
            pageObjects: {
                suggest() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Suggest', this.filters);
                },
                popup() {
                    return this.createPageObject('PopupB2b');
                },
            },
            hooks: {
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.browser.setState('vendorsCategories', categoriesState);
                },
            },
        }),
    ),
});
