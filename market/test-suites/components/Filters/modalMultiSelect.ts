'use strict';

import {difference, isEmpty} from 'lodash';
import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на проверку работы фильтра множественного выбора в модальном окне
 * @param {PageObject.ModalMultiSelect} select Мультиселект в модальном окне
 * @param {PageObject.PagedList} list Список фильтруемых сущностей
 * @param {PageObject.Modal} modal Модальное окно
 * @param {Object} params
 * @param {string} params.initialFilterText Начальный текст тогглера
 * @param {string} params.expectedFilterText Ожидаемый текст тогглера
 * @param {string} [params.expectedAllItemsFilterText] Ожидаемый текст тогглера, когда выбраны все опции
 * @param {number} [params.initialItemsCount] Начальное количество элементов в списке
 * @param {number} [params.expectedItemsCount] Ожидаемое количество элементов в списке
 * @param {number} [params.expectedAllItemsCount] Ожидаемое количество элементов в списке, когда выбраны все опции
 * @param {string[]} params.selectItems Опции для выбора
 * @param {string[]} [params.allItems] Все опции
 * @param {Function} [params.waitForLoading] Функция ожидания загрузки списка
 * @param {Function} [params.afterFiltration] Функция дополнительных проверок после фильтрации
 * @param {Function} [params.afterAllFiltration] Функция дополнительных проверок после фильтрации по всем опциям
 */
export default makeSuite('Мультиселект в модальном окне.', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            return this.allure.runStep('Ожидаем появления кнопки открытия модального окна', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.select.waitForExist(),
            );
        },
        'При выборе значений': {
            'фильтрует список': makeCase({
                async test() {
                    const {
                        expectedAllItemsFilterText,
                        expectedAllItemsCount,
                        afterAllFiltration,
                        expectedItemsCount,
                        expectedFilterText,
                        initialItemsCount,
                        initialFilterText,
                        afterFiltration,
                        waitForLoading,
                        selectItems,
                        allItems,
                    } = this.params;

                    await this.select
                        .getText()
                        .should.eventually.be.equal(initialFilterText, `Текст тогглера "${initialFilterText}"`);

                    if (initialItemsCount) {
                        await this.list
                            .getItemsCount()
                            .should.eventually.be.equal(
                                initialItemsCount,
                                `Количество элементов в списке ${initialItemsCount}`,
                            );
                    }

                    await this.select.click();

                    await this.browser.allure.runStep('Ожидаем появления модального окна', () =>
                        this.modal.waitForVisible(),
                    );

                    await this.modal.waitForLoading();

                    await this.select.selectItems(selectItems);

                    await this.select.apply();

                    await this.modal.waitForHidden();

                    await this.select
                        .getText()
                        .should.eventually.be.equal(
                            expectedFilterText,
                            `Текст тогглера поменялся на "${expectedFilterText}"`,
                        );

                    if (waitForLoading) {
                        await this.browser.allure.runStep('Ожидаем загрузки списка', () => waitForLoading.call(this));
                    } else {
                        await this.list.waitForLoading();
                    }

                    if (expectedItemsCount) {
                        await this.list
                            .getItemsCount()
                            .should.eventually.be.equal(
                                expectedItemsCount,
                                `Количество элементов в списке стало ${expectedItemsCount}`,
                            );
                    }

                    if (afterFiltration) {
                        await afterFiltration.call(this);
                    }

                    // Выбор всех возможных значений
                    if (allItems) {
                        await this.select.click();

                        await this.browser.allure.runStep('Ожидаем появления модального окна', () =>
                            this.modal.waitForVisible(),
                        );

                        await this.modal.waitForLoading();

                        await this.select.selectItems(
                            // Оставляем только не выбранные ранее значения
                            isEmpty(selectItems) ? allItems : difference(allItems, selectItems),
                        );

                        await this.select.apply();

                        await this.modal.waitForHidden();

                        if (expectedAllItemsFilterText) {
                            await this.select
                                .getText()
                                .should.eventually.be.equal(
                                    expectedAllItemsFilterText,
                                    `Текст тогглера поменялся на "${expectedAllItemsFilterText}"`,
                                );
                        }

                        if (expectedAllItemsCount) {
                            if (waitForLoading) {
                                await this.browser.allure.runStep('Ожидаем загрузки списка', () =>
                                    waitForLoading.call(this),
                                );
                            } else {
                                await this.list.waitForLoading();
                            }

                            await this.list
                                .getItemsCount()
                                .should.eventually.be.equal(
                                    expectedAllItemsCount,
                                    `Количество элементов в списке стало ${expectedAllItemsCount}`,
                                );
                        }

                        if (afterAllFiltration) {
                            await afterAllFiltration.call(this);
                        }
                    }
                },
            }),
        },
    },
});
