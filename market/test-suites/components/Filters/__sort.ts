'use strict';

import {stringify} from 'query-string';
import {mergeSuites, makeSuite, makeCase} from 'ginny';

/**
 * Тест на проверку работы фильтра-сортировки
 *
 * @param {PageObject} sortToggler - Кликабельный элемент с сортировкой
 * @param {PageObject.PagedList} list - Список сущностей
 * @param {Object} params
 * @param {Function} params.getItemText - Функция получения текста элемента списка
 * @param {string} params.sortByQueryParamName - Имя URL параметра для критерия сортировки
 * @param {string} params.sortByQueryParamValue - Значение URL параметра для критерия сортировки
 * @param {string} params.sortOrderQueryParamName - Имя URL параметра для направления сортировки
 * @param {string} params.sortOrderQueryParamValue - Значение URL параметра для направления сортировки
 * @param {string} [params.initialItemText] - Начальный текст элемента списка
 * @param {string} [params.expectedItemText] - Ожидаемый текст элемента списка
 * @param {Function} [params.waitForLoading] - Функция ожидания загрузки списка
 * @param {Function} [params.afterFiltration] - функция дополнительных проверок после фильтрации
 */
export default makeSuite('Сортировка списка.', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления переключателя сортировки', () =>
                    this.sortToggler.waitForExist(),
                );
            },
        },
        {
            'При переключении сортировки': {
                'список сортируется в правильном направлении': makeCase({
                    async test() {
                        const {
                            getItemText,
                            waitForLoading,
                            initialItemText,
                            afterFiltration,
                            expectedItemText,
                            sortByQueryParamName,
                            sortByQueryParamValue,
                            sortOrderQueryParamName,
                            sortOrderQueryParamValue,
                        } = this.params;

                        if (initialItemText) {
                            await this.browser.allure.runStep(
                                `Текст первого элемента в списке "${initialItemText}"`,
                                () => getItemText.call(this).should.eventually.be.equal(initialItemText),
                            );
                        }

                        const query = {
                            [sortByQueryParamName]: sortByQueryParamValue,
                            [sortOrderQueryParamName]: sortOrderQueryParamValue,
                        };

                        await this.browser.vndWaitForChangeUrl(() => this.sortToggler.click(), true);

                        await this.browser.allure.runStep(`В URL прокинулись параметры ${stringify(query)}`, () =>
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

                        if (afterFiltration) {
                            await afterFiltration.call(this);
                        }
                    },
                }),
            },
        },
    ),
});
