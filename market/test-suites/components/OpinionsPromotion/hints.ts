'use strict';

import {importSuite, mergeSuites, makeSuite, PageObject} from 'ginny';

const ProductInfoLabel = PageObject.get('ProductInfoLabel');

/**
 * Тесты на подсказки страницы с отзывами за баллы
 *
 * @param {PageObject.PagedList} list - список товаров с отзывами за баллы
 * @param {PageObject.OpinionsPromotionAgitationStats} agitationStats - блок со статистикой
 */
export default makeSuite('Подсказки.', {
    issue: 'VNDFRONT-3984',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'Блок статистики.': mergeSuites(
            {
                async beforeEach() {
                    this.setPageObjects({
                        spinner() {
                            return this.createPageObject('SpinnerLevitan', this.agitationStats);
                        },
                    });

                    await this.browser.allure.runStep('Дожидаемся появления блока со статистикой', () =>
                        this.agitationStats.waitForExist(),
                    );

                    return this.browser.allure.runStep('Дожидаемся загрузки блока со статистикой', () =>
                        this.browser.waitUntil(
                            async () => {
                                const visible = await this.spinner.isVisible();

                                return visible === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Не удалось дождаться скрытия спиннера',
                        ),
                    );
                },
            },
            importSuite('Hint', {
                suiteName: 'Подсказка про целевое количество отзывов.',
                meta: {
                    feature: 'Отзывы за баллы',
                    id: 'vendor_auto-1311',
                    environment: 'kadavr',
                },
                params: {
                    text:
                        'Здесь учитываются только те отзывы, которые получили ваши товары с установленными ' +
                        'целями. Чтобы получить подробную статистику по собранным отзывам, сформируйте отчёт.',
                },
                pageObjects: {
                    hint() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('Hint', this.agitationStats.progressInfo);
                    },
                },
            }),
            importSuite('Hint', {
                suiteName: 'Подсказка у поля ожидаемых расходов.',
                meta: {
                    feature: 'Отзывы за баллы',
                    id: 'vendor_auto-1312',
                    environment: 'kadavr',
                },
                params: {
                    text:
                        'Столько вы потратите, если получите отзывы за баллы на все недавно проданные товары, ' +
                        'для которых настроен сбор.\n\nВ реальности не все покупатели захотят написать отзыв — ' +
                        'или напишут, но не за баллы. Поэтому фактические расходы могут оказаться ниже ' +
                        'ожидаемых. Полную детализацию вы найдёте в отчёте — создайте его с помощью серой ' +
                        'кнопки в углу.',
                },
                pageObjects: {
                    expectedExpensesLabel() {
                        return this.createPageObject(
                            'ProductInfoLabel',
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.agitationStats,
                            `${ProductInfoLabel.root}:nth-child(2)`,
                        );
                    },
                    hint() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('Hint', this.expectedExpensesLabel);
                    },
                },
            }),
        ),
        'Список.': mergeSuites(
            {
                async beforeEach() {
                    this.setPageObjects({
                        listHeader() {
                            return this.createPageObject('OpinionsPromotionListHeader', this.list);
                        },
                    });

                    await this.browser.allure.runStep('Дожидаемся появления таблицы с отзывами за баллы', () =>
                        this.list.waitForExist(),
                    );

                    return this.list.waitForLoading();
                },
            },
            importSuite('Hint', {
                suiteName: 'Подсказка у столбца «Баллы».',
                meta: {
                    feature: 'Отзывы за баллы',
                    id: 'vendor_auto-1321',
                    environment: 'kadavr',
                },
                params: {
                    text:
                        'Укажите, сколько баллов Плюса получит пользователь за отзыв. 1 балл = 1 рубль. ' +
                        'За каждый новый отзыв мы будем списывать с вашего счёта эту ' +
                        'сумму вместе с комиссией Маркета — она составляет 10%.',
                },
                pageObjects: {
                    hint() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('Hint', this.listHeader.cashbackHeader);
                    },
                },
            }),
            importSuite('Hint', {
                suiteName: 'Подсказка у столбца «Сколько отзывов на карточке».',
                meta: {
                    feature: 'Отзывы за баллы',
                    id: 'vendor_auto-1322',
                    environment: 'kadavr',
                },
                params: {
                    text: 'Учитываются все отзывы — в том числе за баллы Плюса.',
                },
                pageObjects: {
                    hint() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('Hint', this.listHeader.currentOpinionsCountHeader);
                    },
                },
            }),
            importSuite('Hint', {
                suiteName: 'Подсказка у столбца «Цель».',
                meta: {
                    feature: 'Отзывы за баллы',
                    id: 'vendor_auto-1323',
                    environment: 'kadavr',
                },
                params: {
                    text:
                        'Укажите, сколько отзывов вы хотите видеть на карточке товара — соберём за баллы ровно ' +
                        'столько, сколько не хватает до этого значения. Например, если на карточке уже есть ' +
                        '2 отзыва, а вы хотите, чтобы было 10, укажите в поле число 10.\n\nМожете оставить ' +
                        'поле пустым, и тогда отзывы за баллы будут собираться, пока вы не отключите услугу ' +
                        'или не установите конкретную цель.',
                },
                pageObjects: {
                    hint() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('Hint', this.listHeader.targetOpinionsCountHeader);
                    },
                },
            }),
            importSuite('Hint', {
                suiteName: 'Подсказка у столбца «Отзывы за баллы».',
                meta: {
                    feature: 'Отзывы за баллы',
                    id: 'vendor_auto-1324',
                    environment: 'kadavr',
                },
                params: {
                    text:
                        'Столько отзывов за баллы получили ваши товары. ' +
                        'Детализацию расходов вы найдёте в отчёте. ' +
                        'Создайте его с помощью серой кнопки в блоке «Статистика» и скачайте в формате Excel.',
                },
                pageObjects: {
                    hint() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('Hint', this.listHeader.paidOpinionsCollectedHeader);
                    },
                },
            }),
            importSuite('Hint', {
                suiteName: 'Подсказка у столбца «Как часто продаётся».',
                meta: {
                    feature: 'Отзывы за баллы',
                    id: 'vendor_auto-1325',
                    environment: 'kadavr',
                },
                params: {
                    text: 'Максимальное количество отзывов, которое может получить товар на данный момент',
                },
                pageObjects: {
                    hint() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('Hint', this.listHeader.agitationTempHeader);
                    },
                },
            }),
            importSuite('Tooltip', {
                suiteName: 'Тултип у значка «Цель достигнута».',
                meta: {
                    feature: 'Отзывы за баллы',
                    id: 'vendor_auto-1333',
                    environment: 'kadavr',
                },
                params: {
                    text:
                        'Цель выполнена. Сбор приостановлен\n' +
                        'Чтобы продолжить собирать отзывы за баллы для этого товара, поставьте новую цель — ' +
                        'она должна быть больше значения в колонке «Сколько отзывов на карточке». ' +
                        'Если оставить поле пустым, отзывы за баллы будут собираться бесконечно.',
                },
                pageObjects: {
                    item() {
                        return this.createPageObject(
                            'OpinionsPromotionListItem',
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.list,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.list.getItemByIndex(0),
                        );
                    },
                    targetElement() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.createPageObject('IconLevitan', this.item.targetOpinionsContainer);
                    },
                },
            }),
        ),
    },
});
