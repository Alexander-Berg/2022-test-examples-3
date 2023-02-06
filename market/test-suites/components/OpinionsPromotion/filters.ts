'use strict';

import {importSuite, mergeSuites, makeSuite, makeCase} from 'ginny';

import TextLevitan from 'spec/page-objects/TextLevitan';

/**
 * Тесты на фильтрацию списка товаров с отзывами за баллы
 *
 * @param {PageObject.PagedList} list - список товаров с отзывами за баллы
 * @param {PageObject.PopupB2b} popup - попап для выпадашек селектов
 * @param {PageObject.Filters} filters - фильтры для списка товаров
 */
export default makeSuite('Фильтрация товаров с отзывами за баллы.', {
    issue: 'VNDFRONT-3986',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.allure.runStep('Дожидаемся появления списка товаров', () =>
                    this.list.waitForExist(),
                );

                await this.list.waitForLoading();
            },
        },
        importSuite('Filters/__multiSelect', {
            suiteName: 'Фильтрация по категориям.',
            meta: {
                feature: 'Отзывы за баллы',
                id: 'vendor_auto-1313',
                environment: 'kadavr',
            },
            params: {
                initialFilterText: 'Все',
                expectedFilterText: 'Выбрано 1',
                queryParamName: 'categoryId',
                queryParamValue: '91491',
                initialItemsCount: 20,
                expectedItemsCount: 1,
                selectItems: ['Телефоны / Мобильные телефоны'],
                allItems: ['Компьютеры / Ноутбуки', 'Телефоны / Мобильные телефоны'],
                expectedAllItemsFilterText: 'Все',
                queryParamValueAll: ['91491', '91013'],
                expectedAllItemsCount: 20,
            },
            pageObjects: {
                select() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('MultiSelectB2b', this.filters.label());
                },
            },
        }),
        {
            'Фильтрация по признаку «Баллы назначены».': mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            select() {
                                return this.createPageObject('SelectAdvanced', this.filters.label(1));
                            },
                        });
                    },
                },
                importSuite('Filters/__select', {
                    suiteName: 'Товары с баллами',
                    meta: {
                        feature: 'Отзывы за баллы',
                        id: 'vendor_auto-1314',
                    },
                    params: {
                        initialFilterText: 'Неважно',
                        expectedFilterText: 'Да',
                        queryParamName: 'opinionsPromotion',
                        queryParamValue: 'ENABLED',
                        initialItemsCount: 20,
                        expectedItemsCount: 2,
                    },
                }),
                importSuite('Filters/__select', {
                    suiteName: 'Товары без баллов',
                    meta: {
                        feature: 'Отзывы за баллы',
                        id: 'vendor_auto-1314',
                    },
                    params: {
                        initialFilterText: 'Неважно',
                        expectedFilterText: 'Нет',
                        queryParamName: 'opinionsPromotion',
                        queryParamValue: 'DISABLED',
                        initialItemsCount: 20,
                        expectedItemsCount: 19,
                    },
                }),
            ),
        },
        {
            'Фильтрация по признаку «Цели назначены».': mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            select() {
                                return this.createPageObject('SelectAdvanced', this.filters.label(2));
                            },
                            opinionsPromotionSelect() {
                                return this.createPageObject('SelectAdvanced', this.filters.label(1));
                            },
                        });

                        await this.opinionsPromotionSelect.click();

                        await this.popup.waitForPopupShown();

                        await this.browser.vndWaitForChangeUrl(
                            () => this.opinionsPromotionSelect.selectItem('Да'),
                            true,
                        );
                    },
                },
                importSuite('Filters/__select', {
                    suiteName: 'Товары с целями',
                    meta: {
                        feature: 'Отзывы за баллы',
                        id: 'vendor_auto-1315',
                    },
                    params: {
                        initialFilterText: 'Неважно',
                        expectedFilterText: 'Да',
                        queryParamName: 'targetOpinions',
                        queryParamValue: 'YES',
                        initialItemsCount: 2,
                        expectedItemsCount: 1,
                    },
                }),
                importSuite('Filters/__select', {
                    suiteName: 'Товары без целей',
                    meta: {
                        feature: 'Отзывы за баллы',
                        id: 'vendor_auto-1315',
                    },
                    params: {
                        initialFilterText: 'Неважно',
                        expectedFilterText: 'Нет',
                        queryParamName: 'targetOpinions',
                        queryParamValue: 'NO',
                        initialItemsCount: 2,
                        expectedItemsCount: 1,
                    },
                }),
            ),
        },
        {
            Поиск: mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            input() {
                                return this.createPageObject('InputB2b', this.filters);
                            },
                        });
                    },
                },
                importSuite('Filters/__search', {
                    suiteName: 'по названию.',
                    meta: {
                        feature: 'Отзывы за баллы',
                        id: 'vendor_auto-1317',
                    },
                    params: {
                        initialCount: 20,
                        expectedCount: 1,
                        queryParamName: 'modelName',
                        queryParamValue: 'Смартфон ASUS P535',
                    },
                }),
                importSuite('Filters/__search', {
                    suiteName: 'с пустым результатом.',
                    meta: {
                        feature: 'Отзывы за баллы',
                        id: 'vendor_auto-1317',
                    },
                    params: {
                        initialCount: 20,
                        expectedCount: 0,
                        queryParamName: 'modelName',
                        queryParamValue: 'карма гнутая',
                        notFoundText: 'Ничего не найдено',
                    },
                    pageObjects: {
                        notFoundElement() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('TextLevitan', this.list, `${TextLevitan.root}:nth-child(2)`);
                        },
                    },
                }),
            ),
        },
        importSuite('Filters/__pager', {
            suiteName: 'Пагинация списка товаров с отзывами за баллы.',
            meta: {
                feature: 'Отзывы за баллы',
                id: 'vendor_auto-1318',
            },
            params: {
                expectedPage: 2,
                initialItemsCount: 20,
                expectedItemsCount: 1,
            },
            pageObjects: {
                pager() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('PagerB2b', this.list);
                },
            },
        }),
        {
            'При выборе в фильтре «Баллы назначены» значения отличного от «Да»': {
                'фильтр «Цели назначены»': {
                    'задизейблен и сброшен в значение «Неважно»': makeCase({
                        id: 'vendor_auto-1316',
                        async test() {
                            this.setPageObjects({
                                opinionsPromotionSelect() {
                                    return this.createPageObject('SelectAdvanced', this.filters.label(1));
                                },
                                targetOpinionsSelect() {
                                    return this.createPageObject('SelectAdvanced', this.filters.label(2));
                                },
                            });

                            // Проверяем значения по умолчанию в фильтрах Баллы и Цели
                            await this.opinionsPromotionSelect
                                .getText()
                                .should.eventually.be.equal(
                                    'Неважно',
                                    'Значение по умолчанию фильтра «Баллы назначены» корректное',
                                );

                            await this.targetOpinionsSelect
                                .getText()
                                .should.eventually.be.equal(
                                    'Неважно',
                                    'Значение по умолчанию фильтра «Цели назначены» корректное',
                                );

                            await this.browser
                                .yaSafeAction(this.targetOpinionsSelect.isEnabled(), true)
                                .should.eventually.equal(false, 'Фильтр «Цели назначены» задизейблен');

                            // Устанавлвиаем оба фильтра в «Да»
                            await this.opinionsPromotionSelect.click();

                            await this.popup.waitForPopupShown();

                            await this.browser.vndWaitForChangeUrl(
                                () => this.opinionsPromotionSelect.selectItem('Да'),
                                true,
                            );

                            await this.targetOpinionsSelect
                                .getText()
                                .should.eventually.be.equal('Неважно', 'Значение фильтра «Цели назначены» корректное');

                            await this.browser
                                .yaSafeAction(this.targetOpinionsSelect.isEnabled(), false)
                                .should.eventually.equal(true, 'Фильтр «Цели назначены» активен');

                            await this.targetOpinionsSelect.click();

                            await this.popup.waitForPopupShown();

                            await this.browser.vndWaitForChangeUrl(
                                () => this.targetOpinionsSelect.selectItem('Да'),
                                true,
                            );

                            // Устанавливаем фильтр «Баллы» в «Нет», ожидаем сброса и дизейбла фильтра по Целям
                            await this.opinionsPromotionSelect.click();

                            await this.popup.waitForPopupShown();

                            await this.browser.vndWaitForChangeUrl(
                                () => this.opinionsPromotionSelect.selectItem('Нет'),
                                true,
                            );

                            await this.targetOpinionsSelect
                                .getText()
                                .should.eventually.be.equal('Неважно', 'Значение фильтра «Цели назначены» корректное');

                            await this.browser
                                .yaSafeAction(this.targetOpinionsSelect.isEnabled(), true)
                                .should.eventually.equal(false, 'Фильтр «Цели назначены» задизейблен');

                            // Возвращаем оба фильтра в состоние «Да»
                            await this.opinionsPromotionSelect.click();

                            await this.popup.waitForPopupShown();

                            await this.browser.vndWaitForChangeUrl(
                                () => this.opinionsPromotionSelect.selectItem('Да'),
                                true,
                            );

                            await this.targetOpinionsSelect.click();

                            await this.popup.waitForPopupShown();

                            await this.browser.vndWaitForChangeUrl(
                                () => this.targetOpinionsSelect.selectItem('Да'),
                                true,
                            );

                            // Устанавливаем фильтр «Баллы» в «Неважно», ожидаем сброса и дизейбла фильтра по Целям
                            await this.opinionsPromotionSelect.click();

                            await this.popup.waitForPopupShown();

                            await this.browser.vndWaitForChangeUrl(
                                () => this.opinionsPromotionSelect.selectItem('Неважно'),
                                true,
                            );

                            await this.targetOpinionsSelect
                                .getText()
                                .should.eventually.be.equal('Неважно', 'Значение фильтра «Цели назначены» корректное');

                            await this.browser
                                .yaSafeAction(this.targetOpinionsSelect.isEnabled(), true)
                                .should.eventually.equal(false, 'Фильтр «Цели назначены» задизейблен');

                            // Устанавливаем фильтр «Баллы» в «Да», фильтр «Цели» в «Нет»
                            await this.opinionsPromotionSelect.click();

                            await this.popup.waitForPopupShown();

                            await this.browser.vndWaitForChangeUrl(
                                () => this.opinionsPromotionSelect.selectItem('Да'),
                                true,
                            );

                            await this.targetOpinionsSelect.click();

                            await this.popup.waitForPopupShown();

                            await this.browser.vndWaitForChangeUrl(
                                () => this.targetOpinionsSelect.selectItem('Нет'),
                                true,
                            );

                            // Устанавливаем фильтр «Баллы» в «Неважно», ожидаем сброса и дизейбла фильтра по Целям
                            await this.opinionsPromotionSelect.click();

                            await this.popup.waitForPopupShown();

                            await this.browser.vndWaitForChangeUrl(
                                () => this.opinionsPromotionSelect.selectItem('Неважно'),
                                true,
                            );

                            await this.targetOpinionsSelect
                                .getText()
                                .should.eventually.be.equal('Неважно', 'Значение фильтра «Цели назначены» корректное');

                            await this.browser
                                .yaSafeAction(this.targetOpinionsSelect.isEnabled(), true)
                                .should.eventually.equal(false, 'Фильтр «Цели назначены» задизейблен');
                        },
                    }),
                },
            },
        },
    ),
});
