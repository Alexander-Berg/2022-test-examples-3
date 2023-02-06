'use strict';

import {stringify} from 'query-string';
import {mergeSuites, makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.SelectB2b} select - Селект
 * @param {PageObject.PagedList} list - Список сущностей
 * @param {PageObject.PopupB2b} popup - Выпадающий список
 * @param {Object} params
 * @param {Function} params.getItemText - Функция получения текста элемента списка
 * @param {string} params.initialFilterText - Начальный текст фильтра
 * @param {string} params.expectedFilterText - Ожидаемый текст фильтра
 * @param {string} [params.initialItemText] - Начальный текст элемента списка
 * @param {string} [params.expectedItemText] - Ожидаемый текст элемента списка
 * @param {string} params.queryParamName - Имя URL параметра
 * @param {string} params.queryParamValue - Значение URL параметра
 * @param {number} [params.initialItemsCount] - Начальное количество элементов в списке
 * @param {number} [params.expectedItemsCount] - Ожидаемое количество элементов в списке
 * @param {Function} [params.waitForLoading] - Функция ожидания загрузки списка
 * @param {Function} [params.afterFiltration] - функция дополнительных проверок после фильтрации
 */
export default makeSuite('Селект.', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления селекта', () => this.select.waitForExist());
            },
        },
        {
            'При выборе значения': {
                'фильтрует список': makeCase({
                    async test() {
                        const {
                            getItemText,
                            initialFilterText,
                            expectedFilterText,
                            initialItemText,
                            expectedItemText,
                            queryParamName,
                            queryParamValue,
                            initialItemsCount,
                            expectedItemsCount,
                            waitForLoading,
                            afterFiltration,
                        } = this.params;

                        await this.browser.allure.runStep(`Текст селекта "${initialFilterText}"`, () =>
                            this.select.getText().should.eventually.be.equal(initialFilterText),
                        );

                        if (initialItemText) {
                            await this.browser.allure.runStep(
                                `Текст первого элемента в списке "${initialItemText}"`,
                                () => getItemText.call(this).should.eventually.be.equal(initialItemText),
                            );
                        }

                        if (initialItemsCount) {
                            await this.browser.allure.runStep(
                                `Количество элементов в списке ${initialItemsCount}`,
                                () => this.list.getItemsCount().should.eventually.be.equal(initialItemsCount),
                            );
                        }

                        const query = {[queryParamName]: queryParamValue};

                        await this.select.click();

                        await this.popup.waitForPopupShown();

                        await this.browser.vndWaitForChangeUrl(() => this.select.selectItem(expectedFilterText), true);

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

                        await this.browser.allure.runStep(`Текст селекта поменялся на "${expectedFilterText}"`, () =>
                            this.select.getText().should.eventually.be.equal(expectedFilterText),
                        );

                        if (waitForLoading) {
                            await this.browser.allure.runStep('Ожидаем загрузки списка', () =>
                                waitForLoading.call(this),
                            );
                        } else {
                            await this.list.waitForLoading();
                        }

                        if (expectedItemText) {
                            await this.browser.allure.runStep(
                                `Текст первого элемента в списке стал "${expectedItemText}"`,
                                () => getItemText.call(this).should.eventually.be.equal(expectedItemText),
                            );
                        }

                        if (expectedItemsCount) {
                            await this.browser.allure.runStep(
                                `Количество элементов в списке стало ${expectedItemsCount}`,
                                () => this.list.getItemsCount().should.eventually.be.equal(expectedItemsCount),
                            );
                        }

                        if (afterFiltration) {
                            await afterFiltration.call(this);
                        }
                    },
                }),
            },
        },
    ),
});
