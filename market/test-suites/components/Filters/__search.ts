'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на фильтрацию элементов списка по тексту.
 * @param {PageObject.InputB2b} input - текстовое поле
 * @param {PageObject.PagedList} list - список
 * @param {PageObject.TextLevitan|PageObject.TextB2b} notFoundElement - текст пустой выдачи списка
 * @param {Object} params
 * @param {number} params.initialCount - изначальное количество элементов в списке
 * @param {number} params.expectedCount - ожидаемое количество элементов в списке после фильтрации
 * @param {string} params.queryParamName - имя query-параметра фильтра
 * @param {string} params.queryParamValue - значение query-параметра фильтра
 * @param {Function} [params.waitForLoading] - функция ожидания загрузки списка
 * @param {string} [params.notFoundText] - текст сообщения в пустой выдаче в результатах поиска
 * @param {Function} [params.afterFiltration] - функция дополнительных проверок после фильтрации
 */
export default makeSuite('Поиск по тексту.', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При вводе значения': {
            'фильтрует элементы списка': makeCase({
                async test() {
                    const {
                        initialCount,
                        expectedCount,
                        queryParamName,
                        queryParamValue,
                        waitForLoading,
                        notFoundText,
                        afterFiltration,
                    } = this.params;

                    await this.list
                        .getItemsCount()
                        .should.eventually.to.be.equal(initialCount, `Отображается элементов: "${initialCount}"`);

                    await this.browser
                        .vndWaitForChangeUrl(() => this.input.setValue(queryParamValue))
                        .should.eventually.be.link(
                            {
                                query: {
                                    [queryParamName]: queryParamValue,
                                },
                            },
                            {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                                skipPathname: true,
                            },
                        );

                    if (waitForLoading) {
                        await this.browser.allure.runStep('Ожидаем загрузки списка', () => waitForLoading.call(this));
                    } else {
                        await this.list.waitForLoading();
                    }

                    await this.list
                        .getItemsCount()
                        .should.eventually.to.be.equal(expectedCount, `Отображается элементов: "${expectedCount}"`);

                    if (notFoundText) {
                        await this.notFoundElement
                            .getText()
                            .should.eventually.to.be.equal(notFoundText, 'Текст сообщения корректный');
                    }

                    if (afterFiltration) {
                        await afterFiltration.call(this);
                    }
                },
            }),
        },
    },
});
