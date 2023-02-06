'use strict';

import {difference} from 'lodash';
import {stringify} from 'query-string';
import {mergeSuites, makeSuite, makeCase} from 'ginny';

/**
 * Тест на проверку работы фильтра-мультиселекта
 *
 * @param {PageObject.MultiSelectB2b} select - Мультиселект
 * @param {PageObject.PagedList} list - Список сущностей
 * @param {PageObject.PopupB2b} popup - Выпадающий список
 * @param {Object} params
 * @param {string} params.initialFilterText - Начальный текст фильтра
 * @param {string} params.expectedFilterText - Ожидаемый текст фильтра
 * @param {string} params.queryParamName - Имя URL параметра
 * @param {string} params.queryParamValue - Значение URL параметра
 * @param {string[]} params.selectItems - Опции для выбора
 * @param {number} [params.initialItemsCount] - Начальное количество элементов в списке
 * @param {number} [params.expectedItemsCount] - Ожидаемое количество элементов в списке
 * @param {string[]} [params.allItems] - Все опции мультиселекта
 * @param {string} [params.expectedAllItemsFilterText] - Ожидаемый текст фильтра, когда выбраны все опции
 * @param {string[]} [params.queryParamValueAll] - Ожидаемый текст фильтра, когда выбраны все опции
 * @param {number} [params.expectedAllItemsCount] - Ожидаемый текст фильтра, когда выбраны все опции
 * @param {Function} [params.waitForLoading] - Функция ожидания загрузки списка
 * @param {Function} [params.afterFiltration] - функция дополнительных проверок после фильтрации
 * @param {Function} [params.afterAllFiltration] - функция дополнительных проверок после фильтрации по всем опциям
 */
export default makeSuite('Мультиселект.', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления мультиселекта', () => this.select.waitForExist());
            },
        },
        {
            'При выборе значения': {
                'фильтрует список': makeCase({
                    async test() {
                        const {
                            selectItems,
                            initialFilterText,
                            expectedFilterText,
                            queryParamName,
                            queryParamValue,
                            initialItemsCount,
                            expectedItemsCount,
                            allItems,
                            expectedAllItemsFilterText,
                            queryParamValueAll,
                            expectedAllItemsCount,
                            waitForLoading,
                            afterFiltration,
                            afterAllFiltration,
                        } = this.params;

                        await this.select
                            .getText()
                            .should.eventually.be.equal(initialFilterText, `Текст селекта "${initialFilterText}"`);

                        if (initialItemsCount) {
                            await this.list
                                .getItemsCount()
                                .should.eventually.be.equal(
                                    initialItemsCount,
                                    `Количество элементов в списке ${initialItemsCount}`,
                                );
                        }

                        /*
                         * Скролим страницу вниз, чтобы выпадающее окно располагалось ближе к верхней границе экрана.
                         * Иначе вебдрайвер не может кликнуть на элемент выпадающего списка,
                         * скрытого за пределами экрана.
                         */
                        await this.browser.vndScrollToBottom();

                        await this.select.click();

                        await this.popup.waitForPopupShown();

                        const query = {[queryParamName]: queryParamValue};

                        await this.browser.vndWaitForChangeUrl(() => this.select.selectItems(selectItems), true);

                        await this.browser.allure.runStep(`В URL прокинулся параметр ${stringify(query)}`, () =>
                            this.browser.getUrl().should.eventually.be.link(
                                {query},
                                {
                                    mode: 'match',
                                    skipProtocol: true,
                                    skipHostname: true,
                                    skipPathname: true,
                                },
                            ),
                        );

                        await this.select
                            .getText()
                            .should.eventually.be.equal(
                                expectedFilterText,
                                `Текст селекта поменялся на "${expectedFilterText}"`,
                            );

                        if (waitForLoading) {
                            await this.browser.allure.runStep('Ожидаем загрузки списка', () =>
                                waitForLoading.call(this),
                            );
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

                        // проверка мультиселекта при выборе всех значений
                        if (allItems) {
                            // вычитаем уже выбранные ноды, иначе кликнем на них еще раз и они не будут выбраны
                            const selectOptions = selectItems ? difference(allItems, selectItems) : allItems;

                            await this.browser.vndWaitForChangeUrl(() => this.select.selectItems(selectOptions), true);

                            if (waitForLoading) {
                                await this.browser.allure.runStep('Ожидаем загрузки списка', () =>
                                    waitForLoading.call(this),
                                );
                            } else {
                                await this.list.waitForLoading();
                            }

                            // проверка query-параметров при выборе всех значений
                            if (queryParamValueAll) {
                                const queryAll = {[queryParamName]: queryParamValueAll};

                                await this.browser.allure.runStep(
                                    `В URL прокинулся параметр ${stringify(queryAll)}`,
                                    () =>
                                        this.browser.getUrl().should.eventually.be.link(
                                            {query: queryAll},
                                            {
                                                mode: 'eql', // Глубокое сравнение (нужно для массивов)
                                                skipProtocol: true,
                                                skipHostname: true,
                                                skipPathname: true,
                                            },
                                        ),
                                );
                            }

                            // проверка текста в мультиселекте при выборе всех значений
                            if (expectedAllItemsFilterText) {
                                await this.select
                                    .getText()
                                    .should.eventually.be.equal(
                                        expectedAllItemsFilterText,
                                        `Текст селекта поменялся на "${expectedAllItemsFilterText}"`,
                                    );
                            }

                            // проверка количества элементов в списке при выборе всех значений
                            if (expectedAllItemsCount) {
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
    ),
});
