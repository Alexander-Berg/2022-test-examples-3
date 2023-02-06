'use strict';

import {mergeSuites, importSuite, PageObject, makeSuite} from 'ginny';

const ModalMultiSelectModelsItem = PageObject.get('ModalMultiSelectModelsItem');
const ModalMultiSelect = PageObject.get('ModalMultiSelect');
const Filters = PageObject.get('Filters');

/**
 * Тесты на фильтры страницы "Бренд на Маркете"
 */
export default makeSuite('Фильтры.', {
    feature: 'Бренд на Маркете',
    params: {
        user: 'Пользователь',
    },
    story: {
        beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.setPageObjects({
                filters() {
                    return this.createPageObject('Filters');
                },
            });

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            return this.allure.runStep('Ожидаем появления блока общих фильтров', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.filters.waitForVisible(),
            );
        },
        'Бренды.': mergeSuites(
            {
                beforeEach() {
                    this.setPageObjects({
                        select() {
                            return this.createPageObject(
                                'ModalMultiSelect',
                                this.filters,
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                `${Filters.label()} ${ModalMultiSelect.root}`,
                            );
                        },
                        modal() {
                            return this.createPageObject('Modal').setListItemSelector(ModalMultiSelectModelsItem.root);
                        },
                    });
                },
            },
            importSuite('ModalMultiSelect/search', {
                suiteName: 'Поиск по названию.',
                meta: {
                    issue: 'VNDFRONT-4214',
                    id: 'vendor_auto-1400',
                    environment: 'kadavr',
                },
                params: {
                    initialItemsCount: 5,
                    expectedItemsCount: 1,
                    searchText: 'Xiaomi',
                },
            }),
            importSuite('ModalMultiSelect/search', {
                suiteName: 'Поиск несуществующего названия.',
                meta: {
                    issue: 'VNDFRONT-4214',
                    id: 'vendor_auto-1400',
                    environment: 'kadavr',
                },
                params: {
                    initialItemsCount: 5,
                    expectedItemsCount: 0,
                    searchText: 'Braun',
                    notFoundText: 'По вашему запросу ничего не найдено',
                },
                pageObjects: {
                    notFoundElement() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('TextB2b', this.modal.content);
                    },
                },
            }),
        ),
    },
});
